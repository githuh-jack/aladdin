package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizAvatarReview;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 头像审核记录DAO
 *
 * @author cles
 * @date 2026/09/18
 */
public interface BizAvatarReviewDao extends BaseDao<BizAvatarReview> {

    /**
     * 审核列表(关联用户昵称)
     */
    @Select("SELECT r.id, r.user_id AS userId, su.nickname AS userName, " +
            "r.image_url AS imageUrl, r.status, r.remark, r.sys001 " +
            "FROM biz_avatar_review r LEFT JOIN biz_user su ON r.user_id = su.id " +
            "WHERE r.sys005 = 1 AND (#{status} IS NULL OR r.status = #{status}) " +
            "ORDER BY r.sys001 DESC LIMIT 200")
    List<Map<String, Object>> selectReviewsWithUser(@Param("status") Integer status);
}
