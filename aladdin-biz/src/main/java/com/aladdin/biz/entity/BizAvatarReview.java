package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 头像审核记录
 *
 * @author cles
 * @date 2026/09/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_avatar_review")
public class BizAvatarReview extends BaseEntity {

    private Long userId;
    /** 存储文件名 */
    private String fileName;
    /** 访问URL */
    private String imageUrl;
    /** 审核状态 0待审 1通过 2拒绝 */
    private Integer status;
    /** 审核备注 */
    private String remark;
}
