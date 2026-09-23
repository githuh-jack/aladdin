package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置(奖励金额/价格等，后台可改)
 *
 * @author cles
 * @date 2026/09/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_config")
public class BizConfig extends BaseEntity {

    /** 配置键 */
    private String configKey;
    /** 配置值 */
    private String configValue;
    /** 说明 */
    private String remark;
}
