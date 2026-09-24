package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizUserStampItem;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 用户邮票实例DAO
 *
 * @author cles
 * @date 2026/09/24
 */
public interface BizUserStampItemDao extends BaseDao<BizUserStampItem> {

    /** 我的邮票聚合：按模板分组，可用/已用数量统计(编码不外显) */
    @Select("SELECT si.stamp_id AS stampId, s.name AS stampName, s.image_url AS stampImageUrl, " +
            "s.stamp_type AS stampType, s.theme AS stampTheme, s.series AS series, s.delivery_days AS deliveryDays, " +
            "SUM(CASE WHEN si.status = 1 THEN 1 ELSE 0 END) AS available, " +
            "SUM(CASE WHEN si.status = 2 THEN 1 ELSE 0 END) AS used " +
            "FROM biz_user_stamp_item si " +
            "INNER JOIN biz_stamp s ON si.stamp_id = s.id " +
            "WHERE si.sys005 = 1 AND si.user_id = #{userId} " +
            "GROUP BY si.stamp_id, s.name, s.image_url, s.stamp_type, s.theme, s.series, s.delivery_days " +
            "ORDER BY MAX(si.id) DESC")
    List<Map<String, Object>> selectMyStampAgg(@Param("userId") Long userId);

    /** 批量插入后回填唯一编码(ST+12位序号)，唯一键防重 */
    @Update("UPDATE biz_user_stamp_item SET code = CONCAT('ST', LPAD(id, 12, '0')) WHERE code IS NULL")
    int fillStampCodes();

    /** 消耗一枚可用实例(按模板取最早入手的一张)：置已使用并关联信件 */
    @Update("UPDATE biz_user_stamp_item SET status = 2, used_time = NOW(), letter_id = #{letterId} " +
            "WHERE sys005 = 1 AND user_id = #{userId} AND stamp_id = #{stampId} AND status = 1 " +
            "ORDER BY id LIMIT 1")
    int consumeAvailable(@Param("userId") Long userId, @Param("stampId") Long stampId,
                         @Param("letterId") Long letterId);
}
