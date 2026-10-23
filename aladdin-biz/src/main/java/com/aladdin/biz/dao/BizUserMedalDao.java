package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizUserMedal;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户奖章DAO
 *
 * @author cles
 * @date 2026/10/14
 */
public interface BizUserMedalDao extends BaseDao<BizUserMedal> {

    /** 我的奖章(含奖章定义信息，获得时间=sys001) */
    @Select("SELECT um.*, m.name AS medal_name, m.icon AS medal_icon, m.description AS medal_description " +
            "FROM biz_user_medal um " +
            "INNER JOIN biz_medal m ON um.medal_id = m.id AND m.sys005 = 1 " +
            "WHERE um.sys005 = 1 AND um.user_id = #{userId} " +
            "ORDER BY um.id DESC")
    List<BizUserMedal> selectByUserId(@Param("userId") Long userId);

    /** 是否已拥有(防重复发放，唯一键兜底) */
    @Select("SELECT COUNT(*) FROM biz_user_medal WHERE sys005 = 1 AND user_id = #{userId} AND medal_id = #{medalId}")
    long countByUserAndMedal(@Param("userId") Long userId, @Param("medalId") Long medalId);
}
