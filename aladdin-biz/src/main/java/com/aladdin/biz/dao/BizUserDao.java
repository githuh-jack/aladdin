package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizUser;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 传信纸船用户DAO
 *
 * @author cles
 * @date 2026/09/24
 */
public interface BizUserDao extends BaseDao<BizUser> {

    /** 管理员: 用户列表(账号/笔名/编号关键字) */
    @Select("<script>" +
            "SELECT id, username, nickname, real_name, email, phone, user_no, coins, credit_score, status, sys001, sys005 " +
            "FROM biz_user WHERE sys005 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>AND (username LIKE CONCAT('%',#{keyword},'%') " +
            "OR nickname LIKE CONCAT('%',#{keyword},'%') " +
            "OR user_no LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND status = #{status} </if>" +
            "ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizUser> selectAdminList(@Param("keyword") String keyword,
                                  @Param("status") Integer status,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_user WHERE sys005 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>AND (username LIKE CONCAT('%',#{keyword},'%') " +
            "OR nickname LIKE CONCAT('%',#{keyword},'%') " +
            "OR user_no LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND status = #{status} </if>" +
            "</script>")
    long countAdminList(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select("SELECT * FROM biz_user WHERE user_no = #{userNo} LIMIT 1")
    BizUser selectByUserNo(@Param("userNo") Long userNo);

    @Select("SELECT MAX(user_no) FROM biz_user")
    Long selectMaxUserNo();

    @Select("SELECT id FROM biz_user WHERE sys005 = 1 AND status = 1")
    List<Long> selectActiveUserIds();

    /** 每寄出一封信+1颗小星星 */
    @Update("UPDATE biz_user SET star_count = star_count + 1, sys002 = NOW() WHERE id = #{userId} AND sys005 = 1")
    int addStar(@Param("userId") Long userId);
}
