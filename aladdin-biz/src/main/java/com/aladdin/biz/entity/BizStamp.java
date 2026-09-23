package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邮票
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_stamp")
public class BizStamp extends BaseEntity {

    private String name;
    private String description;
    private String imageUrl;
    /** 铜钱单价 */
    private Integer price;
    /** 库存, -1不限 */
    private Integer stock;
    /** 普通/稀有/限量 */
    private String stampType;
    /** 邮票主题(收藏分类) */
    private String theme;
    /** 0下架 1上架 */
    private Integer status;
}
