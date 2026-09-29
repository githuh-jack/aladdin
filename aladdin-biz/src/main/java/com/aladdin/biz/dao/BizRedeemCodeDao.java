package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizRedeemCode;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 兑换码DAO
 *
 * @author cles
 * @date 2026/09/29
 */
public interface BizRedeemCodeDao extends BaseDao<BizRedeemCode> {

    @Select("SELECT * FROM biz_redeem_code WHERE sys005 = 1 ORDER BY id DESC")
    List<BizRedeemCode> selectAllList();

    @Select("SELECT * FROM biz_redeem_code WHERE sys005 = 1 AND code = #{code} LIMIT 1")
    BizRedeemCode selectByCode(@Param("code") String code);

    /** 已兑换次数+1(返回受影响行数，0表示次数已用完或不存在) */
    @Update("UPDATE biz_redeem_code SET used_count = used_count + 1 " +
            "WHERE id = #{id} AND sys005 = 1 AND used_count < max_count")
    int incrementUsedCount(@Param("id") Long id);

    /** 清除赠送邮票(编辑时 stampId=0 触发，flex update 忽略 null 无法置空) */
    @Update("UPDATE biz_redeem_code SET stamp_id = NULL, sys002 = NOW() " +
            "WHERE id = #{id} AND sys005 = 1")
    int clearStamp(@Param("id") Long id);
}
