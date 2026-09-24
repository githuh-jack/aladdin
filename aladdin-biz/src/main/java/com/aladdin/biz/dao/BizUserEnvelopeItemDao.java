package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizUserEnvelopeItem;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 用户信封实例DAO
 *
 * @author cles
 * @date 2026/09/24
 */
public interface BizUserEnvelopeItemDao extends BaseDao<BizUserEnvelopeItem> {

    /** 我的信封聚合：按模板分组，可用/已用数量统计(编码不外显) */
    @Select("SELECT ei.envelope_id AS envelopeId, e.name AS envelopeName, e.image_url AS envelopeImageUrl, " +
            "SUM(CASE WHEN ei.status = 1 THEN 1 ELSE 0 END) AS available, " +
            "SUM(CASE WHEN ei.status = 2 THEN 1 ELSE 0 END) AS used " +
            "FROM biz_user_envelope_item ei " +
            "INNER JOIN biz_envelope e ON ei.envelope_id = e.id " +
            "WHERE ei.sys005 = 1 AND ei.user_id = #{userId} " +
            "GROUP BY ei.envelope_id, e.name, e.image_url " +
            "ORDER BY MAX(ei.id) DESC")
    List<Map<String, Object>> selectMyEnvelopeAgg(@Param("userId") Long userId);

    /** 批量插入后回填唯一编码(EN+12位序号)，唯一键防重 */
    @Update("UPDATE biz_user_envelope_item SET code = CONCAT('EN', LPAD(id, 12, '0')) WHERE code IS NULL")
    int fillEnvelopeCodes();

    /** 消耗一枚可用实例：置已使用并关联信件 */
    @Update("UPDATE biz_user_envelope_item SET status = 2, used_time = NOW(), letter_id = #{letterId} " +
            "WHERE sys005 = 1 AND user_id = #{userId} AND envelope_id = #{envelopeId} AND status = 1 " +
            "ORDER BY id LIMIT 1")
    int consumeAvailable(@Param("userId") Long userId, @Param("envelopeId") Long envelopeId,
                         @Param("letterId") Long letterId);
}
