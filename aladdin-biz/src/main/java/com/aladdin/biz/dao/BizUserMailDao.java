package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizUserMail;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 用户系统邮件DAO
 *
 * @author cles
 * @date 2026/09/24
 */
public interface BizUserMailDao extends BaseDao<BizUserMail> {

    @Select("SELECT um.id, um.mail_id AS mailId, um.user_id AS userId, um.claimed, um.claim_time AS claimTime, " +
            "m.title, m.content, m.coin_amount AS coinAmount, m.stamp_id AS stampId, m.envelope_id AS envelopeId, " +
            "s.name AS stampName, e.name AS envelopeName, m.sys001 AS sendTime " +
            "FROM biz_user_mail um " +
            "INNER JOIN biz_sys_mail m ON um.mail_id = m.id " +
            "LEFT JOIN biz_stamp s ON m.stamp_id = s.id " +
            "LEFT JOIN biz_envelope e ON m.envelope_id = e.id " +
            "WHERE um.sys005 = 1 AND um.user_id = #{userId} ORDER BY um.id DESC")
    List<Map<String, Object>> selectMyMails(@Param("userId") Long userId);

    /** 领取附件(幂等锁)：仅未领取时置为已领取 */
    @Update("UPDATE biz_user_mail SET claimed = 1, claim_time = NOW() " +
            "WHERE sys005 = 1 AND id = #{id} AND user_id = #{userId} AND claimed = 0")
    int markClaimed(@Param("id") Long id, @Param("userId") Long userId);
}
