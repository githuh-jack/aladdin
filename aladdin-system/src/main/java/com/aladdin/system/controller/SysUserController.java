package com.aladdin.system.controller;

import com.aladdin.common.core.annotation.OpLog;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import com.aladdin.system.entity.SysRole;
import com.aladdin.system.entity.SysUser;
import com.aladdin.system.service.SysRoleService;
import com.aladdin.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统用户控制器
 *
 * @author cles
 * @date 2026/05/06
 */
@RestController
@RequestMapping({"/user", "/system/user"})
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final PasswordEncoder passwordEncoder;

    public SysUserController(SysUserService sysUserService, SysRoleService sysRoleService,
                             PasswordEncoder passwordEncoder) {
        this.sysUserService = sysUserService;
        this.sysRoleService = sysRoleService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<PageResult<SysUser>> list(PageQuery pageQuery,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) Long deptId) {
        return R.ok(sysUserService.listPage(pageQuery, username, status, deptId));
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getUserWithDeptById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return R.ok(user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @OpLog(value = "新增用户", type = "user")
    public R<Void> save(@RequestBody SysUser user) {
        // 检查用户名是否已存在
        if (sysUserService.getByUsername(user.getUsername()) != null) {
            throw new BusinessException(GlobalErrorCode.DATA_DUPLICATE, "用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 新用户默认需要强制修改密码
        user.setPwdForceChange(1);
        boolean saved = sysUserService.save(user);
        if (saved && user.getDeptIds() != null && !user.getDeptIds().isEmpty()) {
            sysUserService.saveUserDepts(user.getId(), user.getDeptIds());
        }
        return saved ? R.ok() : R.fail();
    }

    @PostMapping("/edit")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OpLog(value = "修改用户", type = "user")
    public R<Void> update(@RequestBody SysUser user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            SysUser existing = sysUserService.getById(user.getId());
            if (existing != null) {
                user.setPassword(existing.getPassword());
            }
        }
        boolean updated = sysUserService.updateById(user);
        // 多部门关联(传空数组则清空，不传则不动)
        if (updated && user.getDeptIds() != null) {
            sysUserService.saveUserDepts(user.getId(), user.getDeptIds());
        }
        return updated ? R.ok() : R.fail();
    }

    @PostMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('system:user:remove')")
    @OpLog(value = "删除用户", type = "user")
    public R<Void> remove(@PathVariable Long id) {
        return sysUserService.removeById(id) ? R.ok() : R.fail();
    }

    @GetMapping("/info")
    public R<Map<String, Object>> getCurrentUserInfo() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            return R.fail(GlobalErrorCode.UNAUTHORIZED);
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return R.fail(GlobalErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(null);

        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        Set<String> perms = sysUserService.getPermsByUserId(userId);
        List<String> roleKeys = roles.stream()
                .map(SysRole::getRoleKey)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("roles", roleKeys);
        data.put("desc", "");
        data.put("homePath", "/dashboard/analytics");
        return R.ok(data);
    }

    @GetMapping("/permissions")
    public R<Set<String>> getCurrentUserPermissions() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            return R.fail(GlobalErrorCode.UNAUTHORIZED);
        }
        Set<String> roleKeys = sysUserService.getRoleKeysByUserId(userId);
        Set<String> perms = sysUserService.getPermsByUserId(userId);
        Set<String> all = new HashSet<>();
        for (String roleKey : roleKeys) {
            all.add("ROLE_" + roleKey);
        }
        all.addAll(perms);
        return R.ok(all);
    }

    /**
     * 重置密码（管理员操作）
     */
    @PostMapping("/resetPassword")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OpLog(value = "重置用户密码", type = "password")
    public R<Void> resetPassword(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        String password = (String) body.get("password");
        if (password == null || password.isEmpty()) {
            password = "123456";
        }
        return sysUserService.resetPassword(id, passwordEncoder.encode(password)) ? R.ok() : R.fail();
    }

    /**
     * 修改密码（用户自己操作，需校验旧密码，不能与历史相同，记录日志）
     */
    @PostMapping("/changePassword")
    @OpLog(value = "修改密码", type = "password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            return R.fail(GlobalErrorCode.UNAUTHORIZED);
        }
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST);
        }
        return sysUserService.changePassword(userId, oldPassword, newPassword) ? R.ok() : R.fail();
    }

    /**
     * 修改个人信息
     */
    @PostMapping("/profile")
    @OpLog(value = "修改个人信息", type = "profile")
    public R<Void> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            return R.fail(GlobalErrorCode.UNAUTHORIZED);
        }
        return sysUserService.updateProfile(userId,
                body.get("realName") != null ? body.get("realName") : body.get("nickname"),
                body.get("email"),
                body.get("phone"), body.get("avatar")) ? R.ok() : R.fail();
    }

    @PostMapping("/changeStatus")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OpLog(value = "修改用户状态", type = "user")
    public R<Void> changeStatus(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        return sysUserService.updateStatus(id, status) ? R.ok() : R.fail();
    }
}
