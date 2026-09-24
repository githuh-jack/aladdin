package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizAnnouncement;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告DAO
 *
 * @author cles
 * @date 2026/09/24
 */
public interface BizAnnouncementDao extends BaseDao<BizAnnouncement> {

    @Select("SELECT * FROM biz_announcement WHERE sys005 = 1 ORDER BY id DESC")
    List<BizAnnouncement> selectAllList();

    @Select("SELECT * FROM biz_announcement WHERE sys005 = 1 AND status = 1 ORDER BY id DESC LIMIT 20")
    List<BizAnnouncement> selectPublished();
}
