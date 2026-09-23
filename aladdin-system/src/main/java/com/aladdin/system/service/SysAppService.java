package com.aladdin.system.service;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.db.base.BaseService;
import com.aladdin.system.entity.SysApp;

/**
 * 应用服务接口
 *
 * @author cles
 * @date 2026/09/22
 */
public interface SysAppService extends BaseService<SysApp> {

    /**
     * 分页查询应用
     */
    PageResult<SysApp> listPage(PageQuery pageQuery, String appName, Integer status);

    /**
     * 新增应用(应用编码唯一校验 + 自动创建同名根部门)
     */
    boolean createApp(SysApp app);

    /**
     * 修改应用(应用编码唯一校验)
     */
    boolean updateApp(SysApp app);

    /**
     * 按应用编码查询
     */
    SysApp getByAppCode(String appCode);

    /**
     * 初始化应用相关表(执行应用存储的DDL语句)
     *
     * @return 执行的语句条数
     */
    int initTables(Long appId);
}
