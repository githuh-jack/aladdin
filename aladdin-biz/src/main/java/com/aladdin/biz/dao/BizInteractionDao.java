package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizInteraction;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 互动DAO(点赞/收藏)
 *
 * @author cles
 * @date 2026/09/16
 */
public interface BizInteractionDao extends BaseDao<BizInteraction> {

    @Select("SELECT * FROM biz_interaction WHERE sys005 = 1 " +
            "AND user_id = #{userId} AND target_type = #{targetType} " +
            "AND target_id = #{targetId} AND action = #{action} LIMIT 1")
    BizInteraction selectOne(@Param("userId") Long userId,
                             @Param("targetType") String targetType,
                             @Param("targetId") Long targetId,
                             @Param("action") String action);

    @Select("SELECT COUNT(*) FROM biz_interaction WHERE sys005 = 1 " +
            "AND target_type = #{targetType} AND target_id = #{targetId} AND action = #{action}")
    long countByTarget(@Param("targetType") String targetType,
                       @Param("targetId") Long targetId,
                       @Param("action") String action);
}
