package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 缓存重建线程池。用 ThreadPoolExecutor 显式声明参数（阿里规范禁止 Executors 的无界队列）。
    // 拒绝策略选 DiscardPolicy：任务被丢弃是安全的 —— 下一个请求发现逻辑过期，仍会触发重建。
    // 不能用 CallerRunsPolicy，那会让发起请求的用户线程去跑重建，把逻辑过期"永不阻塞"的设计抵消掉。
    // 线程设为 daemon：否则跑测试时非守护线程会让 JVM 不退出导致 mvn test 卡死。
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = new ThreadPoolExecutor(
            10, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r, "cache-rebuild");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardPolicy());

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        Shop shop = queryWithLogicalExpire(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 逻辑过期解决缓存击穿
     */
    private Shop queryWithLogicalExpire(Long id) {
        // 1. 从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 2. 未命中，直接返回（逻辑过期方案数据需预热，未预热视为不存在）
        if (StrUtil.isBlank(shopJson)) {
            return null;
        }
        // 3. 命中，先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 4. 判断逻辑过期时间是否已过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 5. 未过期，直接返回店铺信息
            return shop;
        }
        // 6. 已过期，需要缓存重建
        // 6.1 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2 判断是否获取锁成功
        if (isLock) {
            // 6.3 成功，开启独立线程，异步重建缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(lockKey);
                }
            });
        }
        // 6.4 返回过期的商铺信息
        return shop;
    }

    /**
     * 添加逻辑过期时间，并把数据写入redis
     */
    public void saveShop2Redis(Long id, Long expireSeconds) {
        // 1. 查询店铺数据
        Shop shop = getById(id);
        // 2. 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3. 写入Redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("商户id不能为空");
        }
        updateById(shop);
        // 逻辑过期方案下不能靠"删 Key"来失效：queryWithLogicalExpire 对未命中直接返回 null，
        // 删了这个店铺就会被一直判成"不存在"、且再也恢复不了。改成把 expireTime 置为当前时间
        // （isAfter 是严格大于，相等即视为过期），下个请求返回旧数据并抢锁触发异步重建。
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopJson)) {
            RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
            redisData.setExpireTime(LocalDateTime.now());
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
        } else {
            // 本来就没缓存（未预热）→ 直接建一份，否则这个店铺查不到
            saveShop2Redis(id, 20L);
        }
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if(x==null || y==null){
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        // 4. 解析结果
        if (results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size() <= from){
            //没有下一页了，返回空列表
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        //截取from-end的部分
        list.stream().skip(from).forEach(result -> {
            String shopIdstr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdstr));
            Distance distance = result.getDistance();
            distanceMap.put(shopIdstr, distance);
        });
        //根据id查询shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("order by FIELD(id, " + idStr + ")").list();
        for(Shop shop : shops){
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);

    }
}
