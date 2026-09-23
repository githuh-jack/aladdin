package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizEnvelope;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 信封DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizEnvelopeDao extends BaseDao<BizEnvelope> {

    @Select("SELECT * FROM biz_envelope WHERE sys005 = 1 AND status = 1 ORDER BY id DESC")
    List<BizEnvelope> selectOnShelfList();

    @Select("SELECT * FROM biz_envelope WHERE sys005 = 1 ORDER BY id DESC")
    List<BizEnvelope> selectAllList();
}
