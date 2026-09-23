package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizDiary;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 日记DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizDiaryDao extends BaseDao<BizDiary> {

    @Select("SELECT d.*, u.nickname AS user_name FROM biz_diary d " +
            "LEFT JOIN sys_user u ON d.user_id = u.id " +
            "WHERE d.id = #{id} AND d.sys005 = 1")
    BizDiary selectDetailById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT d.*, u.nickname AS user_name FROM biz_diary d " +
            "LEFT JOIN sys_user u ON d.user_id = u.id " +
            "WHERE d.sys005 = 1 " +
            "<if test='userId != null'>AND d.user_id = #{userId} </if>" +
            "<if test='isPublic != null'>AND d.is_public = #{isPublic} </if>" +
            "<if test='onlyToday'>AND d.write_date = CURDATE() </if>" +
            "<if test='keyWord != null and keyWord != \"\"'>AND d.title LIKE CONCAT('%',#{keyWord},'%') </if>" +
            "ORDER BY d.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizDiary> selectListWithUser(@Param("userId") Long userId,
                                      @Param("isPublic") Integer isPublic,
                                      @Param("keyWord") String keyWord,
                                      @Param("onlyToday") boolean onlyToday,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /** 用户当日新建日记数 */
    @Select("SELECT COUNT(*) FROM biz_diary WHERE sys005 = 1 AND user_id = #{userId} AND sys001 >= #{start}")
    long countToday(@Param("userId") Long userId, @Param("start") java.time.LocalDateTime start);
}
