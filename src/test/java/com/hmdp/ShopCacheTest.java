package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShopCacheTest {

    private static final Long SHOP_ID = 1L;

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clean() {
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + SHOP_ID);
        stringRedisTemplate.delete(RedisConstants.LOCK_SHOP_KEY + SHOP_ID);
    }

    @Test
    void queryById_logicalExpireFlow() throws Exception {
        // 1. 预热缓存：逻辑过期时间 3 秒
        shopService.saveShop2Redis(SHOP_ID, 3L);

        // 2. 未过期时查询，应命中缓存返回店铺
        mockMvc.perform(get("/shop/" + SHOP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("103茶餐厅"));

        // 3. 等逻辑过期
        Thread.sleep(3500);

        // 4. 过期后查询，应仍返回旧数据，并触发异步重建
        mockMvc.perform(get("/shop/" + SHOP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("103茶餐厅"));

        // 5. 等异步重建完成
        waitForCacheRefresh();

        // 6. 直接读 redis，确认重建后的逻辑过期时间已刷新到未来
        String json = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + SHOP_ID);
        assertNotNull(json);
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        assertNotNull(redisData.getExpireTime());
        assertTrue(redisData.getExpireTime().isAfter(LocalDateTime.now()));
    }

    private void waitForCacheRefresh() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            String json = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + SHOP_ID);
            RedisData redisData = JSONUtil.toBean(json, RedisData.class);
            if (redisData.getExpireTime() != null && redisData.getExpireTime().isAfter(LocalDateTime.now())) {
                return;
            }
            Thread.sleep(200);
        }
        fail("缓存重建未在预期时间内完成");
    }
}
