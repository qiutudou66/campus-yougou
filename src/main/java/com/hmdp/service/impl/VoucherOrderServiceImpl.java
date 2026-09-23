package com.hmdp.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private RedisWorker redisWorker;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ApplicationContext applicationContext;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    // 静态代码块，初始化lua脚本
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 异步处理线程池。用 ThreadPoolExecutor 显式声明参数（阿里规范禁止 Executors 的无界队列）。
    // 这个池只在 @PostConstruct 里提交一次任务，队列给 1 就够；拒绝策略选 AbortPolicy ——
    // 提交失败说明消费者没起来，秒杀会整条链路失效，应该在启动时就抛出来而不是静默跑着。
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1),
            r -> new Thread(r, "seckill-order-handler"),
            new ThreadPoolExecutor.AbortPolicy());

    /** 异常后的退避时长，防止 Redis 不可达时消费线程空转刷日志 */
    private static final long ERROR_BACKOFF_MS = 1000L;

    //在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
    @PostConstruct
    private void init() {
        // 启动时确保消费者组存在（stream 不存在时需先手动 MKSTREAM 建一次）
        try {
            stringRedisTemplate.opsForStream().createGroup("stream.orders", "g1");
        } catch (Exception e) {
            log.warn("消费者组已存在，跳过创建: " + e.getMessage());
        }
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    // 用于线程池处理的任务
    // 当初始化完毕后，就会去从对列中去拿信息
    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";

        @Override
        public void run() {
            while (true) {
                //获取消息中的订单信息 XREADGROUP GROUP g1 c1 count 1 block 2000 STREAMS s1 >
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    // 2.判断订单信息是否为空
                    // 如果为null，说明没有消息，继续下一次循环
                    if(list == null || list.isEmpty()){
                        continue;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> recordValue = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(recordValue, new VoucherOrder(), true);

                    // 2.创建订单
                    handleVoucherOrder(voucherOrder);
                    //ACK 确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                    // Redis 不可达时 read 不会等满 2s 的 block 超时，而是立刻抛异常，
                    // 所以这里不退避的话这个 while 会以 CPU 极限速度空转刷日志（实测几分钟 6.5G）。
                    try {
                        Thread.sleep(ERROR_BACKOFF_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        private void handlePendingList() {
            while (true) {
                // 1.获取 PendingList 中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 STREAMS s1 0
                List<MapRecord<String, Object, Object>> list;
                try {
                    list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                } catch (Exception e) {
                    //读不动就退出本轮，别在 while 里空转
                    log.error("读取 PendingList 失败，退出本轮处理", e);
                    break;
                }
                // 2.判断订单信息是否为空，为空说明 pending 已经清完
                if (list == null || list.isEmpty()) {
                    break;
                }

                MapRecord<String, Object, Object> record = list.get(0);
                try {
                    Map<Object, Object> recordValue = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(recordValue, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    //处理失败不能就此卡住：下面照常 ACK，把这条丢掉并留下日志人工核对
                    log.error("处理 PendingList 消息失败，跳过该条，请人工核对：recordId="
                            + record.getId() + ", value=" + record.getValue(), e);
                }
                // 3.ACK 确认。成败都要 ACK：不 ACK 的话 from("0") 下一轮还会读到同一条，
                //   而它又永远处理不成功 —— 这就是原来的死循环。
                try {
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("ACK 失败，退出本轮处理：recordId=" + record.getId(), e);
                    break;
                }
            }
        }

    }


//    //异步处理线程池
//    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
//
//    //在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
//    @PostConstruct
//    private void init() {
//        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
//    }
//
//    // 用于线程池处理的任务
//    // 当初始化完毕后，就会去从对列中去拿信息
//    private class VoucherOrderHandler implements Runnable {
//
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
//                    // 2.判断订单信息是否为空
//                    // 如果为null，说明没有消息，继续下一次循环
//                    // 解析数据
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    // 2.创建订单
//                    handleVoucherOrder(voucherOrder);
//                } catch (Exception e) {
//                    log.error("处理订单异常", e);
//                }
//            }
//        }
//    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        //1.获取用户
        Long userId = voucherOrder.getUserId();
        // 2.创建锁对象
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        // 3.尝试获取锁
        boolean isLock = redisLock.tryLock();
        // 4.判断是否获得锁成功
        if (!isLock) {
            // 获取锁失败，直接返回失败或者重试
            log.error("不允许重复下单！");
            return;
        }
        try {
            //注意：createVoucherOrder 的 @Transactional 必须通过代理调用才生效。
            //后台线程里 AopContext 是空的（线程隔离），用 ApplicationContext.getBean 拿容器里的代理
            IVoucherOrderService proxy = applicationContext.getBean(IVoucherOrderService.class);
            proxy.createVoucherOrder(voucherOrder);
        } catch (DuplicateKeyException e) {
            //唯一索引 uk_user_voucher 拦下了重复下单，这是 Redis 判断失效时的最后一道防线。
            //必须在事务外层捕获：让 createVoucherOrder 的事务正常回滚，库存扣减一并撤销；
            //在这里吃掉异常，消息才能正常 ACK，不会掉进 pending 队列反复重试。
            log.error("唯一索引拦截重复下单，已忽略：userId=" + voucherOrder.getUserId()
                    + ", voucherId=" + voucherOrder.getVoucherId(), e);
        } finally {
            // 释放锁
            redisLock.unlock();
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1. 获取用户
        Long userId = UserHolder.getUser().getId();
        // 2.3.订单id
        long orderId = redisWorker.nextId("order");
        //执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.singletonList("seckill_voucher:" + voucherId.toString()),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        //判断结果
        int r = result.intValue();
        if(r != 0){
            //不为零，没有购买资格
            return Result.fail(r == 1 ? "库存不足！" : "没有购买资格！");
        }

//        // 为零把下单信息保存在阻塞对列
//        VoucherOrder voucherOrder = new VoucherOrder();
//
//        voucherOrder.setId(orderId);
//        // 2.4.用户id
//        voucherOrder.setUserId(userId);
//        // 2.5.代金券id
//        voucherOrder.setVoucherId(voucherId);
//        // 2.6.放入阻塞队列
//        orderTasks.add(voucherOrder);

        return Result.ok(orderId);

    }

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if(count > 0){
            log.error("用户已经购买过该优惠券！");
            return;
        }
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
               .gt("stock", 0).update();
        if(!success){
            log.error("库存不足！");
            return;
        }
        save(voucherOrder);

    }
}




