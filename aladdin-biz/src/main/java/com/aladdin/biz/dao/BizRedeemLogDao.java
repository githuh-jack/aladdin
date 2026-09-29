package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizRedeemLog;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 兑换记录DAO
 *
 * @author cles
 * @date 2026/09/29
 */
public interface BizRedeemLogDao extends BaseDao<BizRedeemLog> {

    @Select("SELECT COUNT(*) FROM biz_redeem_log WHERE sys005 = 1 " +
            "AND code_id = #{codeId} AND user_id = #{userId}")
    long countByCodeAndUser(@Param("codeId") Long codeId, @Param("userId") Long userId);

    @Select("<script>" +
            "SELECT rl.*, u.nickname AS user_name, u.user_no AS user_no, s.name AS stamp_name FROM biz_redeem_log rl " +
            "LEFT JOIN biz_user u ON rl.user_id = u.id " +
            "LEFT JOIN biz_stamp s ON rl.stamp_id = s.id " +
            "WHERE rl.sys005 = 1 " +
            "<if test='userId != null'>AND rl.user_id = #{userId} </if>" +
            "<if test='code != null and code != \"\"'>AND rl.code LIKE CONCAT('%', #{code}, '%') </if>" +
            "ORDER BY rl.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizRedeemLog> selectAllWithUser(@Param("userId") Long userId,
                                         @Param("code") String code,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_redeem_log rl WHERE rl.sys005 = 1 " +
            "<if test='userId != null'>AND rl.user_id = #{userId} </if>" +
            "<if test='code != null and code != \"\"'>AND rl.code LIKE CONCAT('%', #{code}, '%') </if>" +
            "</script>")
    long countAll(@Param("userId") Long userId, @Param("code") String code);
}
