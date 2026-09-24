package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告(后台发布，用户登录后首页可见)
 *
 * @author cles
 * @date 2026/09/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_announcement")
public class BizAnnouncement extends BaseEntity {

    /** 公告标题 */
    private String title;
    /** 公告内容 */
    private String content;
    /** 0下架 1发布 */
    private Integer status;
}
