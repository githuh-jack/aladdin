package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 传信纸船用户
 *
 * @author cles
 * @date 2026/09/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_user")
public class BizUser extends BaseEntity {

    private String username;
    private String password;
    /** 笔名 */
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    /** 注册时填写的邀请码 */
    private String inviteCode;
    /** 用户唯一编号(6位，从101322起，邮寄信件凭编号) */
    private Long userNo;
    /** 铜钱 */
    private Integer coins;
    /** 信用分 */
    private Integer creditScore;
    /** 小星星累计数(每寄出一封信+1) */
    private Integer starCount;
    /** 禁闭天数(0=无，举报处罚期间不能寄信) */
    private Integer confineDays;
    /** 禁闭起算时间(领取/查看处罚通知邮件时写入) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confineStart;
    /** 状态 1正常 0禁用 */
    private Integer status;
    private Long appId;
}
