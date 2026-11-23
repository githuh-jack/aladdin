package com.aladdin.biz.dao;

import com.aladdin.biz.entity.GoldDaily;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 黄金投资每日快照DAO
 *
 * @author cles
 * @date 2026/11/23
 */
public interface GoldDailyDao extends BaseDao<GoldDaily> {

    /** 回放重建前物理删除，避免逻辑删除与唯一键冲突 */
    @Delete("DELETE FROM biz_gold_daily WHERE user_id = #{userId}")
    int deleteAllByUser(@Param("userId") Long userId);
}
