package com.aladdin.biz.log;

import com.aladdin.biz.dao.BizLoginLogDao;
import com.aladdin.biz.entity.BizLoginLog;
import com.aladdin.common.log.entity.LoginLog;
import com.aladdin.common.log.LoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录日志数据库实现：覆盖默认的Redis实现，登录日志落库 biz_login_log 供后台查看
 * (common-security 的 LoginLogService 为 @ConditionalOnMissingBean，本Bean存在时自动生效)
 *
 * @author cles
 * @date 2026/09/20
 */
@Slf4j
@Service
public class DbLoginLogServiceImpl implements LoginLogService {

    @Autowired
    private BizLoginLogDao loginLogDao;

    @Override
    public void save(LoginLog loginLog) {
        if (loginLog == null) {
            return;
        }
        try {
            BizLoginLog row = new BizLoginLog();
            row.setUserId(loginLog.getUserId() == null ? null : loginLog.getUserId().longValue());
            row.setUserName(loginLog.getUserName());
            row.setLoginName(loginLog.getLoginName());
            row.setLoginIp(loginLog.getLoginIp());
            row.setLoginLocation(loginLog.getLoginLocation());
            row.setLoginType(loginLog.getLoginType());
            row.setCode(loginLog.getCode());
            row.setMessage(loginLog.getMessage());
            row.setLoginTime(loginLog.getLoginTime() == null ? LocalDateTime.now() : loginLog.getLoginTime());
            row.setSys001(row.getLoginTime());
            row.setSys005(1);
            row.setSys006("biz");
            loginLogDao.insert(row);
        } catch (Exception e) {
            log.warn("登录日志落库失败: {}", e.getMessage());
        }
    }
}
