package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizLoginLogDao;
import com.aladdin.biz.dao.BizUserDao;
import com.aladdin.biz.entity.BizLoginLog;
import com.aladdin.biz.entity.BizUser;
import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.core.utils.IpUtil;
import com.aladdin.common.core.utils.ServletUtil;
import com.aladdin.common.redis.RedisService;
import com.aladdin.common.security.config.SecurityProperties;
import com.aladdin.common.security.service.LoginService;
import com.aladdin.common.security.service.LoginUserDetails;
import com.aladdin.common.security.service.TokenService;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 传信纸船认证控制器（用户注册/登录走 biz_user，与后台管理 sys_user 相互独立）
 *
 * @author cles
 * @date 2026/09/24
 */
@Slf4j
@RestController
@RequestMapping("/biz/auth")
public class PaperBoatAuthController {

    private static final String USER_ROLE = "user";

    @Autowired
    private BizUserDao bizUserDao;

    @Autowired
    private BizLoginLogDao bizLoginLogDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private com.aladdin.biz.service.RewardService rewardService;

    /**
     * 用户注册（传信纸船）
     * 入参：username / password / nickname（笔名，必填） / email（可选） / inviteCode（可选）
     */
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");
        String email = body.get("email");
        String inviteCode = body.get("inviteCode");
        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            return R.fail(GlobalErrorCode.BAD_REQUEST);
        }
        if (username.length() < 3 || username.length() > 30) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "用户名长度需在3-30之间");
        }
        if (password.length() < 6 || password.length() > 50) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "密码长度需在6-50之间");
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "笔名不能为空");
        }
        if (nickname.trim().length() > 30) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "笔名最多30个字符");
        }
        // 邀请码可选填：填写时仅校验格式
        if (inviteCode != null && !inviteCode.trim().isEmpty()
                && !inviteCode.trim().matches("[A-Za-z0-9]{4,16}")) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "邀请码需为4-16位字母或数字");
        }
        if (getByUsername(username) != null) {
            return R.fail(GlobalErrorCode.DATA_DUPLICATE.getCode(), "用户名已存在");
        }
        BizUser user = new BizUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname.trim());
        user.setInviteCode(inviteCode == null ? "" : inviteCode.trim().toUpperCase());
        user.setEmail(email == null ? "" : email);
        user.setPhone("");
        user.setAvatar("");
        user.setCoins(0);
        user.setCreditScore(100);
        user.setStatus(1);
        user.setSys001(LocalDateTime.now());
        user.setSys005(1);
        user.setSys006("biz");
        // 唯一编号：6位，从101322起，每个新用户在上一个最大编号基础上+3~100(唯一键冲突则重试)
        for (int i = 0; i < 5; i++) {
            Long maxNo = bizUserDao.selectMaxUserNo();
            long base = maxNo == null ? 101321L : maxNo;
            user.setUserNo(base + 3 + java.util.concurrent.ThreadLocalRandom.current().nextLong(98));
            try {
                bizUserDao.insert(user);
                break;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                if (i == 4) {
                    throw e;
                }
            }
        }
        // 注册奖励：发放欢迎铜钱(并创建 biz_coin 账户)
        rewardService.onRegistered(user.getId());
        log.info("传信纸船用户注册成功: {}", username);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        return R.ok("注册成功", data);
    }

    /**
     * 用户登录（传信纸船）：从 biz_user 校验，签发与 /auth/login 相同形状的token
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> loginBody) {
        String username = loginBody.get("username");
        String password = loginBody.get("password");
        if (username == null || password == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST);
        }
        // 检查账号是否被锁定
        String lockKey = RedisKeyConstant.ACCOUNT_LOCK + username;
        if (Boolean.TRUE.equals(redisService.hasKey(lockKey))) {
            insertLoginLog(null, username, "0", GlobalErrorCode.ACCOUNT_LOCKED_BY_RETRY.getCode(), "密码错误次数过多，账号已锁定");
            return R.fail(GlobalErrorCode.ACCOUNT_LOCKED_BY_RETRY);
        }
        BizUser user = getByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            handlePasswordError(username);
            insertLoginLog(user == null ? null : user.getId(), username, "0",
                    GlobalErrorCode.LOGIN_PASSWORD_ERROR.getCode(), GlobalErrorCode.LOGIN_PASSWORD_ERROR.getMsg());
            return R.fail(GlobalErrorCode.LOGIN_PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            insertLoginLog(user.getId(), username, "0", GlobalErrorCode.ACCOUNT_DISABLED.getCode(), GlobalErrorCode.ACCOUNT_DISABLED.getMsg());
            return R.fail(GlobalErrorCode.ACCOUNT_DISABLED);
        }
        // permissions 声明必填：避免 JwtAuthenticationFilter 按用户名回查 sys_user
        LoginUserDetails loginUser = new LoginUserDetails(user.getId(), user.getUsername(), user.getNickname(),
                user.getPassword(), user.getStatus(), Collections.singleton("ROLE_" + USER_ROLE),
                Collections.singleton(USER_ROLE));
        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", List.of("ROLE_" + USER_ROLE));
        String token = tokenService.createToken(loginUser.getUserId(), loginUser.getUsername(), claims);
        // 登录成功，清除错误计数
        redisService.delete(RedisKeyConstant.PWD_ERROR_COUNT + username);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", token);
        data.put("id", loginUser.getUserId());
        data.put("username", loginUser.getUsername());
        data.put("realName", loginUser.getNickname());
        data.put("roles", Collections.singletonList(USER_ROLE));
        data.put("homePath", "/home");
        insertLoginLog(user.getId(), username, "1", GlobalErrorCode.LOGIN_SUCCESS.getCode(), "登录成功");
        return R.ok("登录成功", data);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("登出成功", null);
    }

    /**
     * 当前登录用户信息（形状与原 /system/user/info 一致，供前端刷新用户态）
     */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizUser user = bizUserDao.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userNo", user.getUserNo());
        data.put("username", user.getUsername());
        data.put("realName", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("roles", Collections.singletonList(USER_ROLE));
        return R.ok(data);
    }

    private BizUser getByUsername(String username) {
        return bizUserDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("username").eq(username)));
    }

    /**
     * 处理密码错误，累加错误次数，超过限制则锁定
     */
    private void handlePasswordError(String username) {
        SecurityProperties.Password pwdConfig = securityProperties.getPassword();
        if (!pwdConfig.isEnabled()) {
            return;
        }
        String countKey = RedisKeyConstant.PWD_ERROR_COUNT + username;
        Long errorCount = redisService.increment(countKey);
        if (errorCount != null && errorCount >= pwdConfig.getMaxRetryCount()) {
            String lockKey = RedisKeyConstant.ACCOUNT_LOCK + username;
            redisService.set(lockKey, "1", pwdConfig.getLockDurationMinutes(), TimeUnit.MINUTES);
            redisService.delete(countKey);
            log.warn("传信纸船用户[{}]密码错误次数超限，账号锁定{}分钟", username, pwdConfig.getLockDurationMinutes());
        } else if (errorCount != null) {
            redisService.expire(countKey, pwdConfig.getLockDurationMinutes(), TimeUnit.MINUTES);
        }
    }

    /** 写传信纸船登录日志（biz_login_log，后台登录日志页可见） */
    private void insertLoginLog(Long userId, String loginName, String loginType, int code, String message) {
        try {
            BizLoginLog loginLog = new BizLoginLog();
            loginLog.setUserId(userId);
            loginLog.setUserName(loginName);
            loginLog.setLoginName(loginName);
            loginLog.setLoginType(loginType);
            loginLog.setCode(code);
            loginLog.setMessage(message);
            loginLog.setLoginTime(LocalDateTime.now());
            HttpServletRequest request = ServletUtil.getRequest();
            if (request != null) {
                loginLog.setLoginIp(IpUtil.getIpAddr(request));
            }
            loginLog.setSys005(1);
            loginLog.setSys006("biz");
            bizLoginLogDao.insert(loginLog);
        } catch (Exception e) {
            log.warn("保存传信纸船登录日志失败: {}", e.getMessage());
        }
    }
}
