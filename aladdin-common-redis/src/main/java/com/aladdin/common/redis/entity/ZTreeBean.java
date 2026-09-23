package com.aladdin.common.redis.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;

/**
 * Redis键树结构
 *
 * @author cles
 * @date 2026/05/07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZTreeBean {

    private String id;

    @JSONField(name = "pId")
    private String pId;

    private String name;

    private String pattern;

    private Integer index;

    private Integer page;

    private Long count;

    private String icon;

    private boolean checked = false;

    @JSONField(name = "isParent")
    private boolean isParent;

    private LinkedList<ZTreeBean> children;

    public ZTreeBean(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public ZTreeBean(String id, String pId, String name) {
        this.id = id;
        this.pId = pId;
        this.name = name;
    }
}
