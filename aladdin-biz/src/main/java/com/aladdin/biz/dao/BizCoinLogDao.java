package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizCoinLog;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 铜钱流水DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizCoinLogDao extends BaseDao<BizCoinLog> {

    /** 我的流水(仅最近一个月) */
    @Select("SELECT * FROM biz_coin_log WHERE sys005 = 1 AND user_id = #{userId} " +
            "AND sys001 >= DATE_SUB(NOW(), INTERVAL 1 MONTH) " +
            "ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<BizCoinLog> selectMyLogs(@Param("userId") Long userId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM biz_coin_log WHERE sys005 = 1 AND user_id = #{userId} " +
            "AND sys001 >= DATE_SUB(NOW(), INTERVAL 1 MONTH)")
    long countMyLogs(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT cl.*, u.nickname AS user_name, u.user_no AS user_no FROM biz_coin_log cl " +
            "LEFT JOIN biz_user u ON cl.user_id = u.id " +
            "WHERE cl.sys005 = 1 " +
            "<if test='userId != null'>AND cl.user_id = #{userId} </if>" +
            "<if test='logType != null and logType != \"\"'>AND cl.log_type = #{logType} </if>" +
            "ORDER BY cl.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizCoinLog> selectAllLogsWithUser(@Param("userId") Long userId,
                                           @Param("logType") String logType,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);
}
