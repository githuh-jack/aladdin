package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizCoin;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户铜钱账户DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizCoinDao extends BaseDao<BizCoin> {

    @Select("SELECT * FROM biz_coin WHERE sys005 = 1 AND user_id = #{userId} LIMIT 1")
    BizCoin selectByUserId(@Param("userId") Long userId);

    /** 余额扣减(返回受影响行数，0表示余额不足或账户不存在) */
    @Update("UPDATE biz_coin SET balance = balance + #{delta} " +
            "WHERE user_id = #{userId} AND sys005 = 1 AND (balance + #{delta}) >= 0")
    int adjustBalance(@Param("userId") Long userId, @Param("delta") int delta);
}
