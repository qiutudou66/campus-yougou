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
import java.time.ZoneId;
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
        // 筛选条件整个放在 MySQL 里做（create_time 和 NOW() 同一个时钟），
        // 不把「当前时间」当参数传进去比，省得踩驱动 serverTimezone 和 JVM 时区不一致的坑。
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
            // score 必须用博文自己的发布时间，不能用「现在」：
            // 补发的是几分钟前的旧博文，取当前时间会把它顶到粉丝 feed 的最前面，打乱时间序。
            //
            // 时区警告：驱动对 LocalDateTime 不做任何换算，是【原样透传】MySQL 按会话时区
            // 渲染出的墙上时钟（实测：改会话时区，读出的字符串跟着变）。所以下面这行
            // .atZone(systemDefault()) 成立的前提是【MySQL 会话时区 == JVM 时区】——
            // 本机两边都是 Asia/Shanghai，实测 delta=0。注意 URL 里的 serverTimezone=UTC
            // 对这条 java.time 路径不起作用，别被它误导。
            // 一旦 MySQL 被搬到 UTC 环境（Docker 官方镜像、多数云服务器默认 UTC），这里会
            // 静默偏 -8h：补发的博文会沉到 feed 底部而不是顶部。要根治就改用
            // UNIX_TIMESTAMP(create_time)*1000 让 MySQL 自己算 epoch（TIMESTAMP 列该值不受会话时区影响）。
            long ts = blog.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
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
