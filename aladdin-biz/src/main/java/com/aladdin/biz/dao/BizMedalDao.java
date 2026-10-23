package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizMedal;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 奖章定义DAO
 *
 * @author cles
 * @date 2026/10/14
 */
public interface BizMedalDao extends BaseDao<BizMedal> {

    /** 全部奖章(后台管理与用户展示共用，停用的排后) */
    @Select("SELECT * FROM biz_medal WHERE sys005 = 1 ORDER BY status DESC, id ASC")
    List<BizMedal> selectAllMedals();

    /** 处理举报选奖章用：启用中的奖章 */
    @Select("SELECT * FROM biz_medal WHERE sys005 = 1 AND status = 1 ORDER BY id ASC")
    List<BizMedal> selectEnabled();
}
