package com.aladdin.system.pm.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import com.aladdin.system.pm.dao.PmDeptDao;
import com.aladdin.system.pm.dao.PmUserDao;
import com.aladdin.system.pm.entity.PmDept;
import com.aladdin.system.pm.entity.PmUser;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.aladdin.system.pm.entity.table.PmDeptTableDef.PM_DEPT;
import static com.aladdin.system.pm.entity.table.PmUserTableDef.PM_USER;

/**
 * 项目管理系统-组织域(部门/用户，管理员维护)
 *
 * @author cles
 * @date 2026/11/02
 */
@RestController
@RequestMapping("/pm")
public class PmOrgController {

    private final PmDeptDao pmDeptDao;
    private final PmUserDao pmUserDao;
    private final PmAuthHelper auth;
    private final PasswordEncoder passwordEncoder;

    public PmOrgController(PmDeptDao pmDeptDao, PmUserDao pmUserDao,
                           PmAuthHelper auth, PasswordEncoder passwordEncoder) {
        this.pmDeptDao = pmDeptDao;
        this.pmUserDao = pmUserDao;
        this.auth = auth;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== 部门 ====================

    @GetMapping("/dept/list")
    public R<List<PmDept>> deptList() {
        List<PmDept> list = pmDeptDao.selectListByQuery(
                QueryWrapper.create().orderBy(PM_DEPT.SORT, true));
        return R.ok(list);
    }

    @GetMapping("/dept/tree")
    public R<List<Map<String, Object>>> deptTree() {
        List<PmDept> list = pmDeptDao.selectListByQuery(QueryWrapper.create().orderBy(PM_DEPT.SORT, true));
        Map<Long, Map<String, Object>> nodeMap = new java.util.LinkedHashMap<>();
        for (PmDept dept : list) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", dept.getId());
            node.put("parentId", dept.getParentId());
            node.put("deptName", dept.getDeptName());
            node.put("leader", dept.getLeader());
            node.put("sort", dept.getSort());
            node.put("status", dept.getStatus());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(dept.getId(), node);
        }
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> node : nodeMap.values()) {
            Long parentId = (Long) node.get("parentId");
            Map<String, Object> parent = parentId == null ? null : nodeMap.get(parentId);
            if (parent != null) {
                ((List<Map<String, Object>>) parent.get("children")).add(node);
            } else {
                tree.add(node);
            }
        }
        return R.ok(tree);
    }

    @PostMapping("/dept")
    public R<Void> saveDept(@RequestBody PmDept dept) {
        auth.requireRole();
        dept.setId(null);
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        dept.setSys001(LocalDateTime.now());
        dept.setSys005(1);
        return pmDeptDao.insert(dept) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/dept/edit")
    public R<Void> editDept(@RequestBody PmDept dept) {
        auth.requireRole();
        return pmDeptDao.update(dept) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/dept/remove/{id}")
    public R<Void> removeDept(@PathVariable Long id) {
        auth.requireRole();
        return pmDeptDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 用户 ====================

    @GetMapping("/user/page")
    public R<PageResult<PmUser>> userPage(PageQuery pageQuery,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long deptId,
                                          @RequestParam(required = false) String role) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_USER.USERNAME.like(keyword).or(PM_USER.REAL_NAME.like(keyword)));
        }
        if (deptId != null) {
            wrapper.and(PM_USER.DEPT_ID.eq(deptId));
        }
        if (role != null && !role.isEmpty()) {
            wrapper.and(PM_USER.ROLE.eq(role));
        }
        wrapper.orderBy(PM_USER.ID, false);
        Page<PmUser> page = pmUserDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        List<PmUser> records = page.getRecords();
        fillDeptNames(records);
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), records));
    }

    @GetMapping("/user/list")
    public R<List<PmUser>> userList() {
        List<PmUser> list = pmUserDao.selectListByQuery(QueryWrapper.create()
                .where(PM_USER.STATUS.eq(1)).orderBy(PM_USER.ID, true));
        list.forEach(u -> u.setPassword(null));
        return R.ok(list);
    }

    @PostMapping("/user")
    public R<Void> saveUser(@RequestBody PmUser user) {
        auth.requireRole();
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }
        Long count = pmUserDao.selectCountByQuery(QueryWrapper.create()
                .where(PM_USER.USERNAME.eq(user.getUsername().trim())));
        if (count != null && count > 0) {
            return R.fail(GlobalErrorCode.DATA_DUPLICATE.getCode(), "用户名已存在");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "密码至少6位");
        }
        user.setUsername(user.getUsername().trim());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setSys001(LocalDateTime.now());
        user.setSys005(1);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        return pmUserDao.insert(user) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/user/edit")
    public R<Void> editUser(@RequestBody PmUser user) {
        auth.requireRole();
        PmUser db = pmUserDao.selectOneById(user.getId());
        if (db == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        // 不通过编辑修改密码
        user.setPassword(null);
        return pmUserDao.update(user) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/user/reset-pwd/{id}")
    public R<Void> resetPwd(@PathVariable Long id, @RequestBody Map<String, String> body) {
        auth.requireRole();
        String password = body.get("password");
        if (password == null || password.length() < 6) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "密码至少6位");
        }
        PmUser user = new PmUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        return pmUserDao.update(user) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/user/remove/{id}")
    public R<Void> removeUser(@PathVariable Long id) {
        auth.requireRole();
        return pmUserDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    private void fillDeptNames(List<PmUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> deptIds = users.stream().map(PmUser::getDeptId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = pmDeptDao.selectListByIds(deptIds).stream()
                .collect(Collectors.toMap(PmDept::getId, PmDept::getDeptName, (a, b) -> a));
        users.forEach(u -> u.setDeptName(nameMap.get(u.getDeptId())));
        users.forEach(u -> u.setPassword(null));
    }
}
