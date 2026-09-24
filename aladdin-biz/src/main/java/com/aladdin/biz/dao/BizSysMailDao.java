package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizSysMail;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 系统邮件DAO
 *
 * @author cles
 * @date 2026/09/24
 */
public interface BizSysMailDao extends BaseDao<BizSysMail> {

    /** 后台发送记录(含送达/已领取人数) */
    @Select("SELECT m.*, s.name AS stamp_name, e.name AS envelope_name, " +
            "(SELECT COUNT(*) FROM biz_user_mail um WHERE um.mail_id = m.id AND um.sys005 = 1) AS recipient_count, " +
            "(SELECT COUNT(*) FROM biz_user_mail um WHERE um.mail_id = m.id AND um.sys005 = 1 AND um.claimed = 1) AS claimed_count " +
            "FROM biz_sys_mail m " +
            "LEFT JOIN biz_stamp s ON m.stamp_id = s.id " +
            "LEFT JOIN biz_envelope e ON m.envelope_id = e.id " +
            "WHERE m.sys005 = 1 ORDER BY m.id DESC")
    List<Map<String, Object>> selectAdminList();
}
