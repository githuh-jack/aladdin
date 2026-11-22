package com.aladdin.system.pm.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.system.pm.dao.PmBugDao;
import com.aladdin.system.pm.dao.PmCaseDao;
import com.aladdin.system.pm.dao.PmCommitDao;
import com.aladdin.system.pm.dao.PmModuleDao;
import com.aladdin.system.pm.dao.PmProductDao;
import com.aladdin.system.pm.dao.PmTaskDao;
import com.aladdin.system.pm.dao.PmUserDao;
import com.aladdin.system.pm.entity.PmBug;
import com.aladdin.system.pm.entity.PmCase;
import com.aladdin.system.pm.entity.PmCommit;
import com.aladdin.system.pm.entity.PmModule;
import com.aladdin.system.pm.entity.PmProduct;
import com.aladdin.system.pm.entity.PmTask;
import com.aladdin.system.pm.entity.PmUser;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.aladdin.system.pm.entity.table.PmBugTableDef.PM_BUG;
import static com.aladdin.system.pm.entity.table.PmCaseTableDef.PM_CASE;
import static com.aladdin.system.pm.entity.table.PmCommitTableDef.PM_COMMIT;

/**
 * 项目管理系统-质量域(用例/缺陷/代码提交)
 *
 * @author cles
 * @date 2026/11/02
 */
@RestController
@RequestMapping("/pm")
public class PmQaController {

    private final PmCaseDao pmCaseDao;
    private final PmBugDao pmBugDao;
    private final PmCommitDao pmCommitDao;
    private final PmProductDao pmProductDao;
    private final PmModuleDao pmModuleDao;
    private final PmTaskDao pmTaskDao;
    private final PmUserDao pmUserDao;
    private final PmAuthHelper auth;

    public PmQaController(PmCaseDao pmCaseDao, PmBugDao pmBugDao, PmCommitDao pmCommitDao,
                          PmProductDao pmProductDao, PmModuleDao pmModuleDao, PmTaskDao pmTaskDao,
                          PmUserDao pmUserDao, PmAuthHelper auth) {
        this.pmCaseDao = pmCaseDao;
        this.pmBugDao = pmBugDao;
        this.pmCommitDao = pmCommitDao;
        this.pmProductDao = pmProductDao;
        this.pmModuleDao = pmModuleDao;
        this.pmTaskDao = pmTaskDao;
        this.pmUserDao = pmUserDao;
        this.auth = auth;
    }

    // ==================== 用例 ====================

    @GetMapping("/case/page")
    public R<PageResult<PmCase>> casePage(PageQuery pageQuery,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long productId,
                                          @RequestParam(required = false) Long moduleId,
                                          @RequestParam(required = false) String lastResult) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_CASE.TITLE.like(keyword));
        }
        if (productId != null) {
            wrapper.and(PM_CASE.PRODUCT_ID.eq(productId));
        }
        if (moduleId != null) {
            wrapper.and(PM_CASE.MODULE_ID.eq(moduleId));
        }
        if (lastResult != null && !lastResult.isEmpty()) {
            wrapper.and(PM_CASE.LAST_RESULT.eq(lastResult));
        }
        wrapper.orderBy(PM_CASE.ID, false);
        Page<PmCase> page = pmCaseDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillCases(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    @PostMapping("/case")
    public R<Void> saveCase(@RequestBody PmCase pmCase) {
        auth.requireRole("tester");
        pmCase.setId(null);
        pmCase.setSys001(LocalDateTime.now());
        pmCase.setSys005(1);
        if (pmCase.getLastResult() == null) {
            pmCase.setLastResult("none");
        }
        return pmCaseDao.insert(pmCase) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/case/edit")
    public R<Void> editCase(@RequestBody PmCase pmCase) {
        auth.requireRole("tester");
        return pmCaseDao.update(pmCase) > 0 ? R.ok() : R.fail();
    }

    /** 执行用例: pass/fail/blocked */
    @PostMapping("/case/execute/{id}")
    public R<Void> executeCase(@PathVariable Long id, @RequestParam String result) {
        auth.requireRole("tester");
        if (!List.of("pass", "fail", "blocked").contains(result)) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "非法的执行结果");
        }
        PmCase update = new PmCase();
        update.setId(id);
        update.setLastResult(result);
        update.setExecutedBy(auth.currentPmUserId());
        update.setExecutedTime(LocalDateTime.now());
        return pmCaseDao.update(update) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/case/remove/{id}")
    public R<Void> removeCase(@PathVariable Long id) {
        auth.requireRole("tester");
        return pmCaseDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 缺陷 ====================

    @GetMapping("/bug/page")
    public R<PageResult<PmBug>> bugPage(PageQuery pageQuery,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Long productId,
                                        @RequestParam(required = false) Long projectId,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Integer severity,
                                        @RequestParam(required = false) Long assignedTo) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_BUG.TITLE.like(keyword));
        }
        if (productId != null) {
            wrapper.and(PM_BUG.PRODUCT_ID.eq(productId));
        }
        if (projectId != null) {
            wrapper.and(PM_BUG.PROJECT_ID.eq(projectId));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.and(PM_BUG.STATUS.eq(status));
        }
        if (severity != null) {
            wrapper.and(PM_BUG.SEVERITY.eq(severity));
        }
        if (assignedTo != null) {
            wrapper.and(PM_BUG.ASSIGNED_TO.eq(assignedTo));
        }
        wrapper.orderBy(PM_BUG.ID, false);
        Page<PmBug> page = pmBugDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillBugs(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    /** 提交Bug(测试/经理/产品)，无需经理确认节点，可直接指定修复人 */
    @PostMapping("/bug")
    public R<Void> saveBug(@RequestBody PmBug bug) {
        auth.requireRole("tester", "manager", "product");
        bug.setId(null);
        bug.setSys001(LocalDateTime.now());
        bug.setSys005(1);
        bug.setStatus("active");
        bug.setOpenedBy(auth.currentPmUserId());
        return pmBugDao.insert(bug) > 0 ? R.ok() : R.fail();
    }

    /** 领取Bug(研发认领未指派的缺陷) */
    @PostMapping("/bug/claim/{id}")
    public R<Void> claimBug(@PathVariable Long id, @RequestParam(required = false) String remark) {
        auth.requireRole("dev");
        PmBug bug = pmBugDao.selectOneById(id);
        if (bug == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (bug.getAssignedTo() != null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "该缺陷已被认领");
        }
        PmBug update = new PmBug();
        update.setId(id);
        update.setAssignedTo(auth.currentPmUserId());
        update.setRemark(remark);
        return pmBugDao.update(update) > 0 ? R.ok() : R.fail();
    }

    /** 转发Bug给他人(附带说明，非必填) */
    @PostMapping("/bug/forward/{id}")
    public R<Void> forwardBug(@PathVariable Long id, @RequestParam Long toUserId,
                              @RequestParam(required = false) String remark) {
        auth.requireRole("dev", "tester", "manager");
        PmBug bug = pmBugDao.selectOneById(id);
        if (bug == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        PmBug update = new PmBug();
        update.setId(id);
        update.setAssignedTo(toUserId);
        update.setRemark(remark);
        return pmBugDao.update(update) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/bug/edit")
    public R<Void> editBug(@RequestBody PmBug bug) {
        auth.requireRole("tester", "dev");
        return pmBugDao.update(bug) > 0 ? R.ok() : R.fail();
    }

    /** 解决Bug(研发): fixed/bydesign/notrepro/duplicate，状态置resolved，可附解决说明 */
    @PostMapping("/bug/resolve/{id}")
    public R<Void> resolveBug(@PathVariable Long id, @RequestParam String resolution,
                              @RequestParam(required = false) String remark) {
        auth.requireRole("dev");
        if (!List.of("fixed", "bydesign", "notrepro", "duplicate").contains(resolution)) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "非法的解决方案");
        }
        PmBug update = new PmBug();
        update.setId(id);
        update.setResolution(resolution);
        update.setResolveRemark(remark);
        update.setStatus("resolved");
        update.setResolvedBy(auth.currentPmUserId());
        return pmBugDao.update(update) > 0 ? R.ok() : R.fail();
    }

    /** 验证Bug(测试): 验证通过关闭，验证不通过重新激活 */
    @PostMapping("/bug/verify/{id}")
    public R<Void> verifyBug(@PathVariable Long id, @RequestParam boolean pass) {
        auth.requireRole("tester");
        PmBug update = new PmBug();
        update.setId(id);
        if (pass) {
            update.setStatus("closed");
        } else {
            update.setStatus("active");
            update.setResolution(null);
        }
        return pmBugDao.update(update) > 0 ? R.ok() : R.fail();
    }

    /** 关闭Bug(测试) */
    @PostMapping("/bug/close/{id}")
    public R<Void> closeBug(@PathVariable Long id) {
        auth.requireRole("tester");
        PmBug update = new PmBug();
        update.setId(id);
        update.setStatus("closed");
        return pmBugDao.update(update) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/bug/remove/{id}")
    public R<Void> removeBug(@PathVariable Long id) {
        auth.requireRole("tester");
        return pmBugDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 代码提交 ====================

    @GetMapping("/commit/page")
    public R<PageResult<PmCommit>> commitPage(PageQuery pageQuery,
                                              @RequestParam(required = false) Long taskId,
                                              @RequestParam(required = false) Long userId) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (taskId != null) {
            wrapper.and(PM_COMMIT.TASK_ID.eq(taskId));
        }
        if (userId != null) {
            wrapper.and(PM_COMMIT.USER_ID.eq(userId));
        }
        wrapper.orderBy(PM_COMMIT.ID, false);
        Page<PmCommit> page = pmCommitDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillCommits(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    /** 提交代码(研发) */
    @PostMapping("/commit")
    public R<Void> saveCommit(@RequestBody PmCommit commit) {
        auth.requireRole("dev");
        commit.setId(null);
        commit.setSys001(LocalDateTime.now());
        commit.setSys005(1);
        commit.setUserId(auth.currentPmUserId());
        commit.setCommitTime(LocalDateTime.now());
        return pmCommitDao.insert(commit) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/commit/remove/{id}")
    public R<Void> removeCommit(@PathVariable Long id) {
        auth.requireRole("dev");
        return pmCommitDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 名称回填 ====================

    private Map<Long, PmUser> loadUserMap(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return pmUserDao.selectListByIds(ids.stream().distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(PmUser::getId, u -> u, (a, b) -> a));
    }

    private void fillCases(List<PmCase> cases) {
        if (cases == null || cases.isEmpty()) {
            return;
        }
        List<Long> productIds = cases.stream()
                .map(PmCase::getProductId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> moduleIds = cases.stream()
                .map(PmCase::getModuleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> productMap = productIds.isEmpty() ? new HashMap<>()
                : pmProductDao.selectListByIds(productIds).stream()
                .collect(Collectors.toMap(PmProduct::getId, PmProduct::getName, (a, b) -> a));
        Map<Long, String> moduleMap = moduleIds.isEmpty() ? new HashMap<>()
                : pmModuleDao.selectListByIds(moduleIds).stream()
                .collect(Collectors.toMap(PmModule::getId, PmModule::getName, (a, b) -> a));
        Map<Long, PmUser> userMap = loadUserMap(cases.stream()
                .map(PmCase::getExecutedBy).filter(Objects::nonNull).collect(Collectors.toList()));
        cases.forEach(c -> {
            c.setProductName(productMap.get(c.getProductId()));
            c.setModuleName(moduleMap.get(c.getModuleId()));
            PmUser u = userMap.get(c.getExecutedBy());
            c.setExecutedByName(u == null ? "" : u.getRealName());
        });
    }

    private void fillBugs(List<PmBug> bugs) {
        if (bugs == null || bugs.isEmpty()) {
            return;
        }
        List<Long> productIds = bugs.stream()
                .map(PmBug::getProductId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> productMap = productIds.isEmpty() ? new HashMap<>()
                : pmProductDao.selectListByIds(productIds).stream()
                .collect(Collectors.toMap(PmProduct::getId, PmProduct::getName, (a, b) -> a));
        Map<Long, PmUser> userMap = loadUserMap(bugs.stream().flatMap(b -> java.util.stream.Stream.of(
                b.getOpenedBy(), b.getAssignedTo(), b.getResolvedBy()))
                .filter(Objects::nonNull).collect(Collectors.toList()));
        bugs.forEach(b -> {
            b.setProductName(productMap.get(b.getProductId()));
            PmUser opened = userMap.get(b.getOpenedBy());
            PmUser assigned = userMap.get(b.getAssignedTo());
            PmUser resolved = userMap.get(b.getResolvedBy());
            b.setOpenedByName(opened == null ? "" : opened.getRealName());
            b.setAssignedToName(assigned == null ? "" : assigned.getRealName());
            b.setResolvedByName(resolved == null ? "" : resolved.getRealName());
        });
    }

    private void fillCommits(List<PmCommit> commits) {
        if (commits == null || commits.isEmpty()) {
            return;
        }
        Map<Long, PmUser> userMap = loadUserMap(commits.stream()
                .map(PmCommit::getUserId).filter(Objects::nonNull).collect(Collectors.toList()));
        List<Long> taskIds = commits.stream()
                .map(PmCommit::getTaskId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> taskMap = taskIds.isEmpty() ? new HashMap<>()
                : pmTaskDao.selectListByIds(taskIds).stream()
                .collect(Collectors.toMap(PmTask::getId, PmTask::getName, (a, b) -> a));
        commits.forEach(c -> {
            PmUser u = userMap.get(c.getUserId());
            c.setUserName(u == null ? "" : u.getRealName());
            c.setTaskName(taskMap.get(c.getTaskId()));
        });
    }
}
