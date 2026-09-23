package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 其他笔记
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_note")
public class BizNote extends BaseEntity {

    private Long userId;
    private String title;
    private String content;
    /** 笔记分类 */
    private String noteType;
    /** 0私密 1公开 */
    private Integer isPublic;

    @com.mybatisflex.annotation.Column(ignore = true)
    private String userName;

    // ===== 互动展示字段(不映射DB列) =====
    @com.mybatisflex.annotation.Column(ignore = true)
    private Long likeCount;
    @com.mybatisflex.annotation.Column(ignore = true)
    private Long commentCount;
    @com.mybatisflex.annotation.Column(ignore = true)
    private Boolean liked;
}
