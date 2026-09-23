package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Feed 流扇出消息。
 * <p>
 * ts 由生产者在发博时取一次就固定下来，消费者拿它当 ZSet 的 score。
 * 因为 ZSet 对同一个 member 是覆盖写，所以同一条消息被重复消费时 score 不变，
 * 扇出天然幂等。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedFanoutMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 博文 id */
    private Long blogId;

    /** 作者 id，消费者据此反查粉丝列表 */
    private Long authorId;

    /** 发博时间戳，作为 feed ZSet 的 score */
    private Long ts;
}
