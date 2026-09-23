package com.hmdp.mq;

import com.hmdp.config.RabbitMQConfig;
import com.hmdp.dto.FeedFanoutMessage;
import com.hmdp.entity.Follow;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;

/**
 * Feed 流扇出消费者：把「发博后推给所有粉丝」这件慢活儿从发博请求线程里搬出来。
 */
@Slf4j
@Component
public class FeedFanoutConsumer {

    /** 完成标记的存活时间。对账任务只看最近 10 分钟的博文，留 1 天远远够 */
    private static final Duration DONE_TTL = Duration.ofDays(1);

    @Resource
    private IFollowService followService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 特意不写 try/catch：异常直接抛出去，交给 application.yaml 里配的重试（3 次）和死信队列。
     * 这里一旦把异常吞掉，死信队列就永远不会触发。
     */
    @RabbitListener(queues = RabbitMQConfig.FEED_QUEUE)
    public void onBlogPublished(FeedFanoutMessage message) {
        List<Follow> fans = followService.query()
                .eq("follow_user_id", message.getAuthorId())
                .list();

        if (fans == null || fans.isEmpty()) {
            markDone(message.getBlogId());
            return;
        }

        final String member = message.getBlogId().toString();
        final double score = message.getTs().doubleValue();
        // 每个粉丝是一个独立的 key，所以不能用「一个 key 写多个 member」的批量接口，
        // 改用 pipeline 把这 N 条 ZADD 压成一次网络往返。
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) {
                ZSetOperations<String, String> zSet = operations.opsForZSet();
                for (Follow fan : fans) {
                    zSet.add(RedisConstants.FEED_KEY + fan.getUserId(), member, score);
                }
                return null;
            }
        });

        markDone(message.getBlogId());
        log.debug("博文 {} 已扇出给 {} 个粉丝", message.getBlogId(), fans.size());
    }

    /** 扇出成功才打标记，对账任务据此判断哪些博文漏推了 */
    private void markDone(Long blogId) {
        stringRedisTemplate.opsForValue()
                .set(RedisConstants.FEED_DONE_KEY + blogId, "1", DONE_TTL);
    }
}
