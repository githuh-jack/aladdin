package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论
 *
 * @author cles
 * @date 2026/09/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_comment")
public class BizComment extends BaseEntity {

    /** thought/note */
    private String targetType;
    private Long targetId;
    private Long userId;
    private String content;

    // ===== 展示用字段(不映射DB列) =====
    @com.mybatisflex.annotation.Column(ignore = true)
    private String userName;
}
