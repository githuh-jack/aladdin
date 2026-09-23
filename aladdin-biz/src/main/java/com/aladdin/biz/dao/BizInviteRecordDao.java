package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizInviteRecord;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 邀请记录DAO
 *
 * @author cles
 * @date 2026/09/18
 */
public interface BizInviteRecordDao extends BaseDao<BizInviteRecord> {

    /** 读取用户注册时填写的邀请码 */
    @Select("SELECT invite_code FROM sys_user WHERE id = #{userId}")
    String selectInviteCodeByUser(@Param("userId") Long userId);

    /** 按用户ID查昵称 */
    @Select("SELECT nickname FROM sys_user WHERE id = #{userId} AND sys005 = 1")
    Map<String, Object> selectNicknameByUserId(@Param("userId") Long userId);

    /** 我的邀请记录(关联被邀请人昵称) */
    @Select("SELECT r.id, r.invitee_id AS inviteeId, su.nickname AS inviteeName, " +
            "r.invite_code AS inviteCode, r.reward_coins AS rewardCoins, r.sys001 " +
            "FROM biz_invite_record r LEFT JOIN sys_user su ON r.invitee_id = su.id " +
            "WHERE r.sys005 = 1 AND r.inviter_id = #{inviterId} " +
            "ORDER BY r.sys001 DESC LIMIT 100")
    List<Map<String, Object>> selectByInviter(@Param("inviterId") Long inviterId);
}
