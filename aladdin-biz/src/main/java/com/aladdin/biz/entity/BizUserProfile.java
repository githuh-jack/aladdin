package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户资料
 *
 * @author cles
 * @date 2026/09/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_user_profile")
public class BizUserProfile extends BaseEntity {

    private Long userId;
    /** 邀请码 */
    private String inviteCode;
    /** 性别 0保密 1男 2女 */
    private Integer gender;
    /** 地区 */
    private String region;
    /** 地区是否保密 0公开 1保密 */
    private Integer regionSecret;
    /** 年龄 */
    private Integer age;
    /** 年龄是否保密 0公开 1保密 */
    private Integer ageSecret;
    /** 个性签名 */
    private String signature;
    /** 头像URL(审核通过后的生效头像) */
    private String avatarUrl;
    /** 头像状态 0无 1待审 2已通过 3已拒绝 */
    private Integer avatarStatus;
    /** 信用分 */
    private Integer creditScore;
    /** 一次性奖励标记位 1注册 2完善资料 */
    private Integer rewardFlags;
}
