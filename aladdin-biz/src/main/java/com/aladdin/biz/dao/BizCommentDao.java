package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizComment;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评论DAO
 *
 * @author cles
 * @date 2026/09/16
 */
public interface BizCommentDao extends BaseDao<BizComment> {

    @Select("SELECT c.*, u.nickname AS user_name FROM biz_comment c " +
            "LEFT JOIN biz_user u ON c.user_id = u.id " +
            "WHERE c.sys005 = 1 AND c.target_type = #{targetType} AND c.target_id = #{targetId} " +
            "ORDER BY c.id ASC")
    List<BizComment> selectByTarget(@Param("targetType") String targetType,
                                    @Param("targetId") Long targetId);
}
