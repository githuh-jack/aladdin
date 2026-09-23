package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizUserEnvelope;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户持有信封DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizUserEnvelopeDao extends BaseDao<BizUserEnvelope> {

    @Select("SELECT ue.*, e.name AS envelope_name, e.image_url AS envelope_image_url " +
            "FROM biz_user_envelope ue " +
            "INNER JOIN biz_envelope e ON ue.envelope_id = e.id " +
            "WHERE ue.sys005 = 1 AND ue.user_id = #{userId} AND ue.count > 0 " +
            "ORDER BY ue.id DESC")
    List<BizUserEnvelope> selectMyEnvelopes(@Param("userId") Long userId);

    @Select("SELECT * FROM biz_user_envelope WHERE sys005 = 1 AND user_id = #{userId} AND envelope_id = #{envelopeId} LIMIT 1")
    BizUserEnvelope selectByUserAndEnvelope(@Param("userId") Long userId, @Param("envelopeId") Long envelopeId);

    @Update("UPDATE biz_user_envelope SET count = count + #{delta} WHERE user_id = #{userId} AND envelope_id = #{envelopeId}")
    int addCount(@Param("userId") Long userId, @Param("envelopeId") Long envelopeId, @Param("delta") int delta);
}
