package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_order")
public class BizOrder extends BaseEntity {

    private Long userId;
    /** stamp/envelope */
    private String itemType;
    private Long itemId;
    /** 冗余商品名 */
    private String itemName;
    private Integer quantity;
    private Integer unitPrice;
    private Integer totalPrice;
    /** 1已完成 2已退款 */
    private Integer status;

    @com.mybatisflex.annotation.Column(ignore = true)
    private String userName;
}
