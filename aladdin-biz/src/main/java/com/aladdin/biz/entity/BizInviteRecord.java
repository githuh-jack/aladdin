package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邀请记录
 *
 * @author cles
 * @date 2026/09/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_invite_record")
public class BizInviteRecord extends BaseEntity {

    /** 邀请人ID */
    private Long inviterId;
    /** 被邀请人ID */
    private Long inviteeId;
    /** 使用的邀请码 */
    private String inviteCode;
    /** 邀请人获得的铜钱奖励 */
    private Integer rewardCoins;
}
