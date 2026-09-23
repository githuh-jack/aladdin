package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizConfigDao;
import com.aladdin.biz.entity.BizConfig;
import com.aladdin.common.core.domain.R;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统配置控制器（奖励金额/价格等，后台可改）
 *
 * @author cles
 * @date 2026/09/20
 */
@RestController
@RequestMapping({"/config", "/biz/config"})
public class ConfigController {

    @Autowired
    private BizConfigDao configDao;

    /** 配置列表(管理端) */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<List<BizConfig>> adminList() {
        return R.ok(configDao.selectListByQuery(QueryWrapper.create()
                .orderBy(new QueryColumn("config_key"), true)));
    }

    /** 修改配置值(管理端) */
    @PostMapping("/admin/save")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> adminSave(@RequestBody BizConfig body) {
        if (body.getId() == null) {
            return R.fail("缺少配置ID");
        }
        BizConfig config = configDao.selectOneById(body.getId());
        if (config == null) {
            return R.fail("配置不存在");
        }
        config.setConfigValue(body.getConfigValue() == null ? "" : body.getConfigValue().trim());
        config.setSys002(LocalDateTime.now());
        configDao.update(config);
        return R.ok();
    }
}
