package com.aladdin.system.log;

import com.aladdin.common.log.entity.LoginLog;
import com.aladdin.common.log.LoginLogService;
import com.aladdin.system.dao.SysLoginLogDao;
import com.aladdin.system.entity.SysLoginLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录日志数据库实现(system)：登录日志落库 sys_login_log
 * (common-security 的 LoginLogService 为 @ConditionalOnMissingBean，本Bean存在时自动生效)
 * 仅 auth 进程加载(AuthApplication扫描com.aladdin.system)，biz 进程不受影响
 *
 * @author cles
 * @date 2026/09/22
 */
@Slf4j
@Primary
@Service
public class SysLoginLogDbService implements LoginLogService {

    @Autowired
    private SysLoginLogDao sysLoginLogDao;

    @Override
    public void save(LoginLog loginLog) {
        if (loginLog == null) {
            return;
        }
        try {
            LocalDateTime loginTime = loginLog.getLoginTime() == null ? LocalDateTime.now() : loginLog.getLoginTime();
            SysLoginLog record = new SysLoginLog();
            record.setTraceId(loginLog.getTraceId());
            record.setUserId(loginLog.getUserId() == null ? null : loginLog.getUserId().longValue());
            record.setUserName(loginLog.getUserName());
            record.setLoginName(loginLog.getLoginName());
            record.setLoginIp(loginLog.getLoginIp());
            record.setLoginLocation(loginLog.getLoginLocation());
            record.setLoginType(loginLog.getLoginType());
            record.setToken(loginLog.getToken());
            record.setSessionId(loginLog.getSessionId());
            record.setCode(loginLog.getCode());
            record.setMessage(loginLog.getMessage());
            record.setLoginTime(loginTime);
            record.setSys001(loginTime);
            record.setSys005(1);
            record.setSys006("system");
            sysLoginLogDao.insert(record);
        } catch (Exception e) {
            log.warn("登录日志落库失败: {}", e.getMessage());
        }
    }
}
