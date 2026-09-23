package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysOpenApiApp;
import com.mybatisflex.core.query.QueryWrapper;

import static com.aladdin.system.entity.table.SysOpenApiAppTableDef.SYS_OPEN_API_APP;

/**
 * Open-API应用DAO
 *
 * @author cles
 * @date 2026/06/12
 */
public interface SysOpenApiAppDao extends BaseDao<SysOpenApiApp> {

    /**
     * 按 accessKey 查询(sys005=1 由 flex 自动追加)
     */
    default SysOpenApiApp selectByAccessKey(String accessKey) {
        return selectOneByQuery(QueryWrapper.create()
                .from(SYS_OPEN_API_APP)
                .where(SYS_OPEN_API_APP.ACCESS_KEY.eq(accessKey)));
    }
}
