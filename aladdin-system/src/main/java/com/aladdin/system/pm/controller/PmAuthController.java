package com.aladdin.system.pm.controller;

import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import com.aladdin.common.security.service.TokenService;
import com.aladdin.system.pm.dao.PmDeptDao;
import com.aladdin.system.pm.dao.PmUserDao;
import com.aladdin.system.pm.entity.PmDept;
import com.aladdin.system.pm.entity.PmUser;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目管理系统-认证控制器(pm_user独立登录，与后台管理sys_user相互独立)
 * /pm/auth/login 加入安全白名单
 *
 * @author cles
 * @date 2026/11/02
 */
@RestController
@RequestMapping("/pm/auth")
public class PmAuthController {

    private final PmUserDao pmUserDao;
    private final PmDeptDao pmDeptDao;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public PmAuthController(PmUserDao pmUserDao, PmDeptDao pmDeptDao,
                            PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.pmUserDao = pmUserDao;
        this.pmDeptDao = pmDeptDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * pm用户登录：从 pm_user 校验，签发与 /auth/login 相同形状的token
     * subject = 200000 + pm_user.id，permissions 声明必填，避免过滤器按用户名回查 sys_user
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty() || password == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST);
        }
        PmUser user = pmUserDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("username").eq(username.trim())));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return R.fail(GlobalErrorCode.LOGIN_PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return R.fail(GlobalErrorCode.ACCOUNT_DISABLED);
        }
        String role = user.getRole() == null ? "dev" : user.getRole();
        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", List.of("pm:" + role));
        // pm_user id 从 200000 起，token subject 直接用真实id(不受偏移影响，偏移由id空间隔离保证)
        String token = tokenService.createToken(PmAuthHelper.PM_USER_ID_OFFSET + user.getId(), user.getUsername(), claims);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", token);
        data.put("id", PmAuthHelper.PM_USER_ID_OFFSET + user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("roles", Collections.singletonList(role));
        data.put("homePath", "/app/pm/program");
        return R.ok("登录成功", data);
    }

    /**
     * 当前登录pm用户信息
     */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            return R.fail(GlobalErrorCode.UNAUTHORIZED);
        }
        Map<String, Object> data = new HashMap<>();
        if (userId < PmAuthHelper.PM_USER_ID_OFFSET) {
            // 平台系统用户
            data.put("userId", userId);
            data.put("username", LoginService.getCurrentUsername());
            data.put("realName", "系统管理员");
            data.put("roles", Collections.singletonList("admin"));
            data.put("sysUser", true);
            return R.ok(data);
        }
        PmUser user = pmUserDao.selectOneById(userId - PmAuthHelper.PM_USER_ID_OFFSET);
        if (user == null) {
            return R.fail(GlobalErrorCode.UNAUTHORIZED);
        }
        data.put("userId", userId);
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        data.put("roles", Collections.singletonList(user.getRole()));
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        if (user.getDeptId() != null) {
            PmDept dept = pmDeptDao.selectOneById(user.getDeptId());
            data.put("deptName", dept == null ? "" : dept.getDeptName());
        }
        return R.ok(data);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        Long userId = LoginService.getCurrentUserId();
        if (userId != null) {
            tokenService.removeToken(userId);
        }
        return R.ok("登出成功", null);
    }
}
