package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizBamboo;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户文竹DAO
 *
 * @author cles
 * @date 2026/09/20
 */
public interface BizBambooDao extends BaseDao<BizBamboo> {

    @Select("SELECT * FROM biz_bamboo WHERE sys005 = 1 AND user_id = #{userId} LIMIT 1")
    BizBamboo selectByUserId(@Param("userId") Long userId);

    /** 文竹列表(关联用户昵称) */
    @Select("<script>" +
            "SELECT b.id, b.user_id AS userId, u.nickname AS nickName, b.height_cm AS heightCm, b.sys001 " +
            "FROM biz_bamboo b LEFT JOIN biz_user u ON b.user_id = u.id " +
            "WHERE b.sys005 = 1 " +
            "<if test='userId != null'> AND b.user_id = #{userId}</if>" +
            " ORDER BY b.height_cm DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    java.util.List<java.util.Map<String, Object>> selectListWithUser(@Param("userId") Long userId,
                                                                     @Param("offset") long offset,
                                                                     @Param("limit") long limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_bamboo b WHERE b.sys005 = 1 " +
            "<if test='userId != null'> AND b.user_id = #{userId}</if>" +
            "</script>")
    long countList(@Param("userId") Long userId);

    /** 长高(原子累加) */
    @org.apache.ibatis.annotations.Update("UPDATE biz_bamboo SET height_cm = height_cm + #{cm}, sys002 = NOW() " +
            "WHERE user_id = #{userId} AND sys005 = 1")
    int grow(@Param("userId") Long userId, @Param("cm") int cm);
}
