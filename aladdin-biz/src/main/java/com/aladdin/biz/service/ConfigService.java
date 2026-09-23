package com.aladdin.biz.service;

import com.aladdin.biz.dao.BizConfigDao;
import com.aladdin.biz.entity.BizConfig;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务（读取 biz_config，后台可改，缺失时用默认值）
 *
 * @author cles
 * @date 2026/09/20
 */
@Service
public class ConfigService {

    @Autowired
    private BizConfigDao configDao;

    /** 读取整型配置，缺失/非法时返回默认值 */
    public int getInt(String key, int defaultValue) {
        BizConfig config = configDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("config_key").eq(key)));
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 读取逗号分隔的整型数组配置(如文竹生长阈值)，缺失时返回默认值 */
    public int[] getIntArray(String key, int[] defaultValue) {
        BizConfig config = configDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("config_key").eq(key)));
        if (config == null || config.getConfigValue() == null || config.getConfigValue().trim().isEmpty()) {
            return defaultValue;
        }
        try {
            String[] parts = config.getConfigValue().trim().split("[,，]");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
            return result;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
