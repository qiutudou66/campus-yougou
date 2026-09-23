package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feed 流扇出的消息拓扑。
 * <p>
 * feed.exchange --feed.publish--> feed.fanout.queue
 *                                        | 重试耗尽
 *                                        v
 *                                   feed.dlx --> feed.dlq.queue
 */
@Configuration
public class RabbitMQConfig {

    public static final String FEED_EXCHANGE = "feed.exchange";
    public static final String FEED_QUEUE = "feed.fanout.queue";
    public static final String FEED_ROUTING_KEY = "feed.publish";

    public static final String FEED_DLX = "feed.dlx";
    public static final String FEED_DLQ = "feed.dlq.queue";

    @Bean
    public DirectExchange feedExchange() {
        return new DirectExchange(FEED_EXCHANGE, true, false);
    }

    @Bean
    public Queue feedQueue() {
        return QueueBuilder.durable(FEED_QUEUE)
                .deadLetterExchange(FEED_DLX)
                .build();
    }

    @Bean
    public Binding feedBinding() {
        return BindingBuilder.bind(feedQueue()).to(feedExchange()).with(FEED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange feedDlx() {
        return new DirectExchange(FEED_DLX, true, false);
    }

    @Bean
    public Queue feedDlq() {
        return QueueBuilder.durable(FEED_DLQ).build();
    }

    /**
     * 死信沿用原路由键（没配 x-dead-letter-routing-key），所以这里用同一个 routing key 绑定。
     */
    @Bean
    public Binding feedDlqBinding() {
        return BindingBuilder.bind(feedDlq()).to(feedDlx()).with(FEED_ROUTING_KEY);
    }

    /**
     * 消息体走 JSON，别用默认的 JDK 序列化（那东西对端不是 Java 就读不了，也难看）。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
