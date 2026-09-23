package com.hmdp.mq;

import com.hmdp.config.RabbitMQConfig;
import com.hmdp.dto.FeedFanoutMessage;
import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Feed 扇出的对账兜底。
 * <p>
 * 异步化会引入一个新的失败点：博文入库成功、但消息没投出去（broker 挂了 / confirm 没回），
 * 这条博文就永远不会推给粉丝。这里定期把「最近发过、但没有扇出完成标记」的博文重投一次。
 * <p>
 * 重投是安全的：消费者的写入带固定 score，ZSet 对同 member 是覆盖，重复扇出没有副作用。
 */
@Slf4j
@Component
public class FeedFanoutReconcileTask {

    /** 只看最近这段时间的博文，再往前的认为对账窗口已过 */
    private static final int RECENT_MINUTES = 10;
    private static final int MAX_SCAN = 200;

    @Resource
    private IBlogService blogService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Scheduled(initialDelay = 60_000, fixedDelay = 5 * 60_000)
    public void republishMissedFanout() {
        // 时间比较整个放在 MySQL 里做（create_time 和 NOW() 同一个时钟），
        // 不跨 JDBC 边界传时间，避开驱动 serverTimezone=UTC 和 JVM 时区不一致的坑。
        List<Blog> recentBlogs = blogService.query()
                .apply("create_time >= DATE_SUB(NOW(), INTERVAL " + RECENT_MINUTES + " MINUTE)")
                .last("LIMIT " + MAX_SCAN)
                .list();

        int republished = 0;
        for (Blog blog : recentBlogs) {
            String doneKey = RedisConstants.FEED_DONE_KEY + blog.getId();
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(doneKey))) {
                continue;
            }
            long ts = System.currentTimeMillis();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.FEED_EXCHANGE,
                    RabbitMQConfig.FEED_ROUTING_KEY,
                    new FeedFanoutMessage(blog.getId(), blog.getUserId(), ts));
            republished++;
        }

        if (republished > 0) {
            log.warn("feed 扇出对账：扫描 {} 条，补发 {} 条", recentBlogs.size(), republished);
        }
    }
}
