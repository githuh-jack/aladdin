package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizBambooLog;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 文竹生长记录DAO
 *
 * @author cles
 * @date 2026/09/20
 */
public interface BizBambooLogDao extends BaseDao<BizBambooLog> {

    /** 生长记录列表(关联用户昵称) */
    @Select("<script>" +
            "SELECT l.*, u.nickname AS nick_name FROM biz_bamboo_log l " +
            "LEFT JOIN sys_user u ON l.user_id = u.id " +
            "WHERE l.sys005 = 1 " +
            "<if test='userId != null'> AND l.user_id = #{userId}</if>" +
            " ORDER BY l.sys001 DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String, Object>> selectLogsWithUser(@Param("userId") Long userId,
                                                 @Param("offset") long offset,
                                                 @Param("limit") long limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_bamboo_log l WHERE l.sys005 = 1 " +
            "<if test='userId != null'> AND l.user_id = #{userId}</if>" +
            "</script>")
    long countLogs(@Param("userId") Long userId);

    /** 用户当日日记生长次数(日记文竹每天最多1cm) */
    @Select("SELECT COUNT(*) FROM biz_bamboo_log WHERE sys005 = 1 AND user_id = #{userId} AND biz_type = 'diary' AND sys001 >= #{start}")
    long countDiaryGrowthToday(@Param("userId") Long userId, @Param("start") java.time.LocalDateTime start);
}
