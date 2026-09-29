package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 信件
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_letter")
public class BizLetter extends BaseEntity {

    /** 发信人ID */
    private Long senderId;
    /** 收信人ID(寄给地址时为空) */
    private Long receiverId;
    /** 收件地址(寄给非好友时填写) */
    private String receiverAddress;
    private String title;
    private String content;
    /** 0草稿 1已发送 2已读 3已删除 */
    private Integer status;
    /** 使用的邮票ID */
    private Long stampId;
    /** 使用的信封ID */
    private Long envelopeId;
    /** 0私密 1公开(可在信件广场展示) */
    private Integer isPublic;
    /** 0正常 1异常(后台标记，异常不上广场) */
    private Integer abnormal;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sendTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime readTime;

    /** 到达时间(寄出时间+邮票送达天数，未到时间收件人不可见) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime arrivalTime;

    /** 退回到达寄件人时间(退回操作+邮票送达天数，到点寄件人可见已退回) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime returnTime;

    /** 退回原因(未找到指定的用户/对方拒收/审核不合格) */
    private String returnReason;

    // ===== 展示用冗余字段（不映射DB列） =====
    @com.mybatisflex.annotation.Column(ignore = true)
    private String senderName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String senderAvatar;
    @com.mybatisflex.annotation.Column(ignore = true)
    private Long senderNo;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String receiverName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private Long receiverNo;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String stampName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String envelopeName;
}
