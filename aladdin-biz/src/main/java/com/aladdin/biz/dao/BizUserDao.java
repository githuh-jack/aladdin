package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizUser;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 传信纸船用户DAO
 *
 * @author cles
 * @date 2026/09/24
 */
public interface BizUserDao extends BaseDao<BizUser> {

    @Select("SELECT * FROM biz_user WHERE user_no = #{userNo} LIMIT 1")
    BizUser selectByUserNo(@Param("userNo") Long userNo);

    @Select("SELECT MAX(user_no) FROM biz_user")
    Long selectMaxUserNo();

    @Select("SELECT id FROM biz_user WHERE sys005 = 1 AND status = 1")
    List<Long> selectActiveUserIds();
}
