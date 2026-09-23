package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizNote;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 话题DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizNoteDao extends BaseDao<BizNote> {

    String INTERACT_SELECT =
            "(SELECT COUNT(*) FROM biz_interaction i WHERE i.sys005 = 1 AND i.target_type = 'note' AND i.target_id = n.id AND i.action = 'like') AS like_count, " +
            "(SELECT COUNT(*) FROM biz_comment c WHERE c.sys005 = 1 AND c.target_type = 'note' AND c.target_id = n.id) AS comment_count, ";

    @Select("SELECT n.*, u.nickname AS user_name, " + INTERACT_SELECT +
            "0 AS liked FROM biz_note n " +
            "LEFT JOIN sys_user u ON n.user_id = u.id " +
            "WHERE n.id = #{id} AND n.sys005 = 1")
    BizNote selectDetailById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT n.*, u.nickname AS user_name, " + INTERACT_SELECT +
            "<if test='viewerId != null'>" +
            "EXISTS(SELECT 1 FROM biz_interaction i WHERE i.sys005 = 1 AND i.user_id = #{viewerId} AND i.target_type = 'note' AND i.target_id = n.id AND i.action = 'like') AS liked " +
            "</if>" +
            "<if test='viewerId == null'>0 AS liked </if>" +
            "FROM biz_note n " +
            "LEFT JOIN sys_user u ON n.user_id = u.id " +
            "WHERE n.sys005 = 1 " +
            "<if test='userId != null'>AND n.user_id = #{userId} </if>" +
            "<if test='isPublic != null'>AND n.is_public = #{isPublic} </if>" +
            "<if test='keyWord != null and keyWord != \"\"'>AND n.title LIKE CONCAT('%',#{keyWord},'%') </if>" +
            "ORDER BY n.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizNote> selectListWithUser(@Param("userId") Long userId,
                                     @Param("isPublic") Integer isPublic,
                                     @Param("keyWord") String keyWord,
                                     @Param("viewerId") Long viewerId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);
}
