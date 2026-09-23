package com.aladdin.system.controller;

import com.aladdin.common.core.annotation.OpLog;
import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.core.utils.IpUtil;
import com.aladdin.common.core.utils.ServletUtil;
import com.aladdin.common.log.entity.LoginLog;
import com.aladdin.common.log.LoginLogService;
import com.aladdin.common.redis.RedisService;
import com.aladdin.common.security.service.LoginUserDetails;
import com.aladdin.common.security.service.TokenService;
import com.aladdin.system.entity.SysOpenApiApp;
import com.aladdin.system.entity.SysUser;
import com.aladdin.system.service.SysOpenApiAppService;
import com.aladdin.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Open-API控制器
 * <p>
 * 第三方对接接口，通过ak/sk+时间戳+签名进行认证
 * 第三方登录后与本地用户绑定，使用一致的Token体系
 *
 * @author cles
 * @date 2026/06/12
 */
@Slf4j
@RestController
@RequestMapping("/openapi")
public class OpenApiController {

    private static final long TIMESTAMP_EXPIRE_MS = 5 * 60 * 1000;
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final SysOpenApiAppService openApiAppService;
    private final SysUserService sysUserService;
    private final TokenService tokenService;
    private final RedisService redisService;
    private final LoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;

    public OpenApiController(SysOpenApiAppService openApiAppService, SysUserService sysUserService,
                             TokenService tokenService, RedisService redisService,
                             LoginLogService loginLogService, PasswordEncoder passwordEncoder) {
        this.openApiAppService = openApiAppService;
        this.sysUserService = sysUserService;
        this.tokenService = tokenService;
        this.redisService = redisService;
        this.loginLogService = loginLogService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 第三方登录接口
     * 请求头需携带: ak, timestamp, sign, nonce
     * sign = HMAC-SHA256(ak + timestamp + nonce, sk)
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String ak = request.getHeader("ak");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        String nonce = request.getHeader("nonce");

        // 参数校验
        if (ak == null || timestamp == null || sign == null || nonce == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST);
        }

        // 时间戳校验
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return R.fail(GlobalErrorCode.OPEN_API_EXPIRED_TIMESTAMP);
        }
        if (Math.abs(System.currentTimeMillis() - requestTime) > TIMESTAMP_EXPIRE_MS) {
            return R.fail(GlobalErrorCode.OPEN_API_EXPIRED_TIMESTAMP);
        }

        // nonce防重放
        String nonceKey = RedisKeyConstant.OPEN_API_NONCE + nonce;
        if (!redisService.setIfAbsent(nonceKey, "1", 10, java.util.concurrent.TimeUnit.MINUTES)) {
            return R.fail(GlobalErrorCode.OPEN_API_NONCE_DUPLICATE);
        }

        // 查找应用
        SysOpenApiApp app = openApiAppService.getByAccessKey(ak);
        if (app == null) {
            return R.fail(GlobalErrorCode.OPEN_API_INVALID_AK);
        }
        if (app.getStatus() != null && app.getStatus() == 0) {
            return R.fail(GlobalErrorCode.OPEN_API_APP_DISABLED);
        }

        // 签名校验
        String data = ak + timestamp + nonce;
        String expectedSign = hmacSha256(data, app.getSecretKey());
        if (!expectedSign.equals(sign)) {
            return R.fail(GlobalErrorCode.OPEN_API_INVALID_SK);
        }

        // 绑定用户校验
        if (app.getBindUserId() == null) {
            return R.fail(GlobalErrorCode.OPEN_API_USER_NOT_BOUND);
        }
        SysUser user = sysUserService.getById(app.getBindUserId());
        if (user == null || user.getStatus() != 1) {
            return R.fail(GlobalErrorCode.OPEN_API_USER_NOT_BOUND);
        }

        // 生成Token，与本地登录一致
        Set<String> permissions = sysUserService.getPermsByUserId(user.getId());
        Set<String> roleKeys = sysUserService.getRoleKeysByUserId(user.getId());
        Set<String> allPerms = new HashSet<>();
        for (String roleKey : roleKeys) {
            allPerms.add("ROLE_" + roleKey);
        }
        allPerms.addAll(permissions);

        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", allPerms);
        claims.put("openApiAppId", app.getId());
        String token = tokenService.createToken(user.getId(), user.getUsername(), claims);

        // 记录登录日志
        saveOpenApiLog(app.getAppName(), user.getId(), request);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        return R.ok("Open-API登录成功", result);
    }

    /**
     * 管理端：创建API应用
     */
    @PostMapping("/app")
    @OpLog(value = "创建API应用", type = "openapi")
    public R<SysOpenApiApp> createApp(@RequestBody SysOpenApiApp app) {
        app.setAccessKey(UUID.randomUUID().toString().replace("-", ""));
        app.setSecretKey(UUID.randomUUID().toString().replace("-", ""));
        return openApiAppService.save(app) ? R.ok(app) : R.fail();
    }

    /**
     * 管理端：绑定系统用户
     */
    @PostMapping("/app/bind")
    @OpLog(value = "绑定API应用用户", type = "openapi")
    public R<Void> bindUser(@RequestBody Map<String, Long> body) {
        Long appId = body.get("appId");
        Long userId = body.get("userId");
        SysOpenApiApp app = openApiAppService.getById(appId);
        if (app == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        app.setBindUserId(userId);
        return openApiAppService.updateById(app) ? R.ok() : R.fail();
    }

    /**
     * 管理端：获取应用列表
     */
    @GetMapping("/app/list")
    public R<List<SysOpenApiApp>> listApps() {
        return R.ok(openApiAppService.list());
    }

    /**
     * 管理端：删除应用
     */
    @DeleteMapping("/app/{id}")
    @OpLog(value = "删除API应用", type = "openapi")
    public R<Void> deleteApp(@PathVariable Long id) {
        return openApiAppService.removeById(id) ? R.ok() : R.fail();
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256签名失败", e);
        }
    }

    private void saveOpenApiLog(String appName, Long userId, HttpServletRequest request) {
        if (loginLogService == null) {
            return;
        }
        LoginLog loginLog = new LoginLog();
        loginLog.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        loginLog.setLoginName("[OpenAPI]" + appName);
        loginLog.setLoginType("3");
        loginLog.setCode(20000);
        loginLog.setMessage("Open-API登录成功");
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setUserId(userId.intValue());
        loginLog.setLoginIp(IpUtil.getIpAddr(request));
        try {
            loginLogService.save(loginLog);
        } catch (Exception e) {
            log.warn("保存Open-API登录日志失败: {}", e.getMessage());
        }
    }
}
