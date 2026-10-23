package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizFeedback;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 问题反馈DAO
 *
 * @author cles
 * @date 2026/10/08
 */
public interface BizFeedbackDao extends BaseDao<BizFeedback> {

    /** 我的反馈列表(最新在前) */
    @Select("SELECT id, content, contact, reply, status, coin_amount, handle_time, sys001 " +
            "FROM biz_feedback WHERE sys005 = 1 AND user_id = #{userId} ORDER BY id DESC LIMIT 100")
    List<BizFeedback> selectMyList(@Param("userId") Long userId);

    /** 管理员: 反馈列表(编号/笔名/内容关键字 + 状态筛选) */
    @Select("<script>" +
            "SELECT f.id, f.user_id, f.content, f.contact, f.reply, f.status, f.coin_amount, " +
            "f.handle_time, f.sys001, u.user_no, u.nickname AS user_name " +
            "FROM biz_feedback f " +
            "LEFT JOIN biz_user u ON f.user_id = u.id AND u.sys005 = 1 " +
            "WHERE f.sys005 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>AND (f.content LIKE CONCAT('%',#{keyword},'%') " +
            "OR u.nickname LIKE CONCAT('%',#{keyword},'%') " +
            "OR u.user_no LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND f.status = #{status} </if>" +
            "ORDER BY f.status ASC, f.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizFeedback> selectAdminList(@Param("keyword") String keyword,
                                      @Param("status") Integer status,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_feedback f " +
            "LEFT JOIN biz_user u ON f.user_id = u.id AND u.sys005 = 1 " +
            "WHERE f.sys005 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>AND (f.content LIKE CONCAT('%',#{keyword},'%') " +
            "OR u.nickname LIKE CONCAT('%',#{keyword},'%') " +
            "OR u.user_no LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND f.status = #{status} </if>" +
            "</script>")
    long countAdminList(@Param("keyword") String keyword, @Param("status") Integer status);
}
