package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 好友关系
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_friend")
public class BizFriend extends BaseEntity {

    /** 发起方ID */
    private Long userId;
    /** 被加方ID */
    private Long friendId;
    /** 0申请中 1已通过 2已拒绝 3已删除 */
    private Integer status;
    private String applyRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime addTime;

    @com.mybatisflex.annotation.Column(ignore = true)
    private String friendName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String friendAvatar;
}
