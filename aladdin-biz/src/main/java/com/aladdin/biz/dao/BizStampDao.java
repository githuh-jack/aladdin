package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizStamp;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 邮票DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizStampDao extends BaseDao<BizStamp> {

    /** 查询上架邮票 */
    @Select("SELECT * FROM biz_stamp WHERE sys005 = 1 AND status = 1 ORDER BY id DESC")
    List<BizStamp> selectOnShelfList();

    /** 查询全部(含下架) */
    @Select("SELECT * FROM biz_stamp WHERE sys005 = 1 ORDER BY id DESC")
    List<BizStamp> selectAllList();

    /** 各主题邮票总数(收藏进度用) */
    @Select("SELECT theme, COUNT(*) AS total FROM biz_stamp WHERE sys005 = 1 GROUP BY theme")
    List<java.util.Map<String, Object>> selectThemeTotals();

    /** 我收集的各主题邮票数(已使用+未使用均算) */
    @Select("SELECT s.theme, COUNT(*) AS collected FROM biz_user_stamp us " +
            "INNER JOIN biz_stamp s ON us.stamp_id = s.id " +
            "WHERE us.sys005 = 1 AND us.user_id = #{userId} " +
            "GROUP BY s.theme")
    List<java.util.Map<String, Object>> selectCollectedByTheme(@Param("userId") Long userId);
}
