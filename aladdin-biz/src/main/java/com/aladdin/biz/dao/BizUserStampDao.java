package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizUserStamp;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户持有邮票DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizUserStampDao extends BaseDao<BizUserStamp> {

    @Select("SELECT us.*, s.name AS stamp_name, s.image_url AS stamp_image_url, s.stamp_type, s.theme AS stamp_theme " +
            "FROM biz_user_stamp us " +
            "INNER JOIN biz_stamp s ON us.stamp_id = s.id " +
            "WHERE us.sys005 = 1 AND us.user_id = #{userId} " +
            "ORDER BY us.id DESC")
    java.util.List<BizUserStamp> selectMyStamps(@Param("userId") Long userId);

    @Select("SELECT * FROM biz_user_stamp WHERE sys005 = 1 AND user_id = #{userId} AND stamp_id = #{stampId} LIMIT 1")
    BizUserStamp selectByUserAndStamp(@Param("userId") Long userId, @Param("stampId") Long stampId);

    @Update("UPDATE biz_user_stamp SET count = count + #{delta} WHERE user_id = #{userId} AND stamp_id = #{stampId}")
    int addCount(@Param("userId") Long userId, @Param("stampId") Long stampId, @Param("delta") int delta);

    /** 消耗一枚可用邮票：可用-1 已使用+1 */
    @Update("UPDATE biz_user_stamp SET count = count - 1, used_count = used_count + 1 " +
            "WHERE sys005 = 1 AND user_id = #{userId} AND stamp_id = #{stampId} AND count > 0")
    int consumeStamp(@Param("userId") Long userId, @Param("stampId") Long stampId);
}
