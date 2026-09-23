package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizCheckinRecord;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 每日签到记录DAO
 *
 * @author cles
 * @date 2026/09/20
 */
public interface BizCheckinRecordDao extends BaseDao<BizCheckinRecord> {

    @Select("SELECT COUNT(*) FROM biz_checkin_record WHERE sys005 = 1 AND user_id = #{userId} AND checkin_date = #{date}")
    long countByUserAndDate(@Param("userId") Long userId, @Param("date") java.time.LocalDate date);
}
