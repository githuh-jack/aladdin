package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 互动(点赞/收藏)
 *
 * @author cles
 * @date 2026/09/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_interaction")
public class BizInteraction extends BaseEntity {

    private Long userId;
    /** thought/note */
    private String targetType;
    private Long targetId;
    /** like/fav */
    private String action;
}
