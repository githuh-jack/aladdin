package com.aladdin.biz.dao;

import com.aladdin.biz.entity.GoldPrice;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 黄金日行情DAO
 *
 * @author cles
 * @date 2026/11/23
 */
public interface GoldPriceDao extends BaseDao<GoldPrice> {

    @Select("SELECT * FROM biz_gold_price WHERE sys005 = 1 ORDER BY trade_date DESC LIMIT 1")
    GoldPrice selectLatest();

    @Select("SELECT MIN(trade_date) FROM biz_gold_price WHERE sys005 = 1")
    LocalDate selectEarliestDate();
}
