package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 日记
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_diary")
public class BizDiary extends BaseEntity {

    private Long userId;
    private String title;
    private String content;
    private String weather;
    private String mood;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate writeDate;

    /** 0私密 1公开 */
    private Integer isPublic;
    /** 0正常 1异常(后台标记，异常不上广场) */
    private Integer abnormal;

    @com.mybatisflex.annotation.Column(ignore = true)
    private String userName;

    @com.mybatisflex.annotation.Column(ignore = true)
    private Long userNo;

    @com.mybatisflex.annotation.Column(ignore = true)
    private String userAvatar;
}
