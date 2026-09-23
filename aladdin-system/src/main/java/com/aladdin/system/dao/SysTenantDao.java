package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysTenant;
import com.mybatisflex.core.query.QueryWrapper;

import static com.aladdin.system.entity.table.SysTenantTableDef.SYS_TENANT;

/**
 * 租户DAO
 *
 * @author cles
 * @date 2026/06/12
 */
public interface SysTenantDao extends BaseDao<SysTenant> {

    /**
     * 按租户编码查询(sys005=1 由 flex 自动追加)
     */
    default SysTenant selectByTenantCode(String tenantCode) {
        return selectOneByQuery(QueryWrapper.create()
                .from(SYS_TENANT)
                .where(SYS_TENANT.TENANT_CODE.eq(tenantCode)));
    }
}
