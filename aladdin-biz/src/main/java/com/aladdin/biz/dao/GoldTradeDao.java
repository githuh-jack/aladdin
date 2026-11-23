package com.aladdin.biz.dao;

import com.aladdin.biz.entity.GoldTrade;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 黄金买入流水DAO
 *
 * @author cles
 * @date 2026/11/23
 */
public interface GoldTradeDao extends BaseDao<GoldTrade> {

    /** 回放重建前物理删除，避免逻辑删除残留 */
    @Delete("DELETE FROM biz_gold_trade WHERE user_id = #{userId}")
    int deleteAllByUser(@Param("userId") Long userId);
}
