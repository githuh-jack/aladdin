package com.aladdin.system.pm.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.system.pm.dao.PmBugDao;
import com.aladdin.system.pm.dao.PmFileDao;
import com.aladdin.system.pm.dao.PmProjectDao;
import com.aladdin.system.pm.dao.PmStoryDao;
import com.aladdin.system.pm.dao.PmTaskDao;
import com.aladdin.system.pm.dao.PmUserDao;
import com.aladdin.system.pm.dao.PmWorklogDao;
import com.aladdin.system.pm.entity.PmBug;
import com.aladdin.system.pm.entity.PmFile;
import com.aladdin.system.pm.entity.PmProject;
import com.aladdin.system.pm.entity.PmStory;
import com.aladdin.system.pm.entity.PmTask;
import com.aladdin.system.pm.entity.PmUser;
import com.aladdin.system.pm.entity.PmWorklog;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.aladdin.system.pm.entity.table.PmBugTableDef.PM_BUG;
import static com.aladdin.system.pm.entity.table.PmFileTableDef.PM_FILE;
import static com.aladdin.system.pm.entity.table.PmTaskTableDef.PM_TASK;
import static com.aladdin.system.pm.entity.table.PmWorklogTableDef.PM_WORKLOG;

/**
 * 项目管理系统-工作台/统计/工时/项目文件
 *
 * @author cles
 * @date 2026/11/04
 */
@RestController
@RequestMapping("/pm")
public class PmStatController {

    private final PmBugDao pmBugDao;
    private final PmTaskDao pmTaskDao;
    private final PmWorklogDao pmWorklogDao;
    private final PmFileDao pmFileDao;
    private final PmUserDao pmUserDao;
    private final PmProjectDao pmProjectDao;
    private final PmStoryDao pmStoryDao;
    private final PmAuthHelper auth;

    public PmStatController(PmBugDao pmBugDao, PmTaskDao pmTaskDao, PmWorklogDao pmWorklogDao,
                            PmFileDao pmFileDao, PmUserDao pmUserDao, PmProjectDao pmProjectDao,
                            PmStoryDao pmStoryDao, PmAuthHelper auth) {
        this.pmBugDao = pmBugDao;
        this.pmTaskDao = pmTaskDao;
        this.pmWorklogDao = pmWorklogDao;
        this.pmFileDao = pmFileDao;
        this.pmUserDao = pmUserDao;
        this.pmProjectDao = pmProjectDao;
        this.pmStoryDao = pmStoryDao;
        this.auth = auth;
    }

    // ==================== 工作台 ====================

    /** 我的工作台：未完成任务 + 未完成Bug */
    @GetMapping("/workbench")
    public R<Map<String, Object>> workbench(@RequestParam(required = false) Long projectId) {
        Long me = auth.currentPmUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        if (me == null) {
            result.put("tasks", List.of());
            result.put("bugs", List.of());
            return R.ok(result);
        }
        QueryWrapper taskWrapper = QueryWrapper.create()
                .where(PM_TASK.ASSIGNED_TO.eq(me))
                .and(PM_TASK.STATUS.in("wait", "doing"))
                .orderBy(PM_TASK.ID, false);
        if (projectId != null) {
            taskWrapper.and(PM_TASK.PROJECT_ID.eq(projectId));
        }
        List<PmTask> tasks = pmTaskDao.selectListByQuery(taskWrapper);
        QueryWrapper bugWrapper = QueryWrapper.create()
                .where(PM_BUG.ASSIGNED_TO.eq(me))
                .and(PM_BUG.STATUS.in("active", "resolved"))
                .orderBy(PM_BUG.ID, false);
        if (projectId != null) {
            bugWrapper.and(PM_BUG.PROJECT_ID.eq(projectId));
        }
        List<PmBug> bugs = pmBugDao.selectListByQuery(bugWrapper);
        fillTasks(tasks);
        fillBugs(bugs);
        result.put("tasks", tasks);
        result.put("bugs", bugs);
        return R.ok(result);
    }

    // ==================== 统计(经理) ====================

    /** 按人汇总：任务数/预计工时/实际工时/缺陷完成情况 */
    @GetMapping("/stat/workload")
    public R<List<Map<String, Object>>> workload(@RequestParam(required = false) Long projectId) {
        QueryWrapper taskWrapper = QueryWrapper.create();
        QueryWrapper bugWrapper = QueryWrapper.create();
        QueryWrapper logWrapper = QueryWrapper.create();
        if (projectId != null) {
            taskWrapper.and(PM_TASK.PROJECT_ID.eq(projectId));
            bugWrapper.and(PM_BUG.PROJECT_ID.eq(projectId));
            logWrapper.and(PM_WORKLOG.PROJECT_ID.eq(projectId));
        }
        List<PmTask> tasks = pmTaskDao.selectListByQuery(taskWrapper);
        List<PmBug> bugs = pmBugDao.selectListByQuery(bugWrapper);
        List<PmWorklog> logs = pmWorklogDao.selectListByQuery(logWrapper);
        Set<Long> userIds = new java.util.HashSet<>();
        tasks.forEach(t -> {
            if (t.getAssignedTo() != null) {
                userIds.add(t.getAssignedTo());
            }
        });
        bugs.forEach(b -> {
            if (b.getAssignedTo() != null) {
                userIds.add(b.getAssignedTo());
            }
        });
        logs.forEach(l -> {
            if (l.getUserId() != null) {
                userIds.add(l.getUserId());
            }
        });
        Map<Long, PmUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : pmUserDao.selectListByIds(userIds).stream()
                .collect(Collectors.toMap(PmUser::getId, u -> u, (a, b) -> a));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long userId : userIds) {
            PmUser user = userMap.get(userId);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", userId);
            row.put("realName", user == null ? String.valueOf(userId) : user.getRealName());
            row.put("role", user == null ? "" : user.getRole());
            List<PmTask> myTasks = tasks.stream().filter(t -> userId.equals(t.getAssignedTo())).collect(Collectors.toList());
            row.put("taskTotal", myTasks.size());
            row.put("taskDone", myTasks.stream().filter(t -> "done".equals(t.getStatus()) || "closed".equals(t.getStatus())).count());
            row.put("estimateHours", myTasks.stream().map(PmTask::getEstimateHours)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("actualHours", logs.stream().filter(l -> userId.equals(l.getUserId()))
                    .map(PmWorklog::getHours).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            List<PmBug> myBugs = bugs.stream().filter(b -> userId.equals(b.getAssignedTo())).collect(Collectors.toList());
            row.put("bugTotal", myBugs.size());
            row.put("bugResolved", myBugs.stream().filter(b -> "resolved".equals(b.getStatus()) || "closed".equals(b.getStatus())).count());
            row.put("bugClosed", myBugs.stream().filter(b -> "closed".equals(b.getStatus())).count());
            row.put("bugActive", myBugs.stream().filter(b -> "active".equals(b.getStatus())).count());
            rows.add(row);
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("realName"))));
        return R.ok(rows);
    }

    /** 缺陷完成情况统计 */
    @GetMapping("/stat/bug")
    public R<Map<String, Object>> bugStat(@RequestParam(required = false) Long projectId) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (projectId != null) {
            wrapper.and(PM_BUG.PROJECT_ID.eq(projectId));
        }
        List<PmBug> bugs = pmBugDao.selectListByQuery(wrapper);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", bugs.size());
        result.put("active", bugs.stream().filter(b -> "active".equals(b.getStatus())).count());
        result.put("resolved", bugs.stream().filter(b -> "resolved".equals(b.getStatus())).count());
        result.put("closed", bugs.stream().filter(b -> "closed".equals(b.getStatus())).count());
        result.put("fixed", bugs.stream().filter(b -> "fixed".equals(b.getResolution())).count());
        result.put("bydesign", bugs.stream().filter(b -> "bydesign".equals(b.getResolution())).count());
        result.put("notrepro", bugs.stream().filter(b -> "notrepro".equals(b.getResolution())).count());
        result.put("duplicate", bugs.stream().filter(b -> "duplicate".equals(b.getResolution())).count());
        Map<Long, List<PmBug>> byUser = bugs.stream()
                .filter(b -> b.getAssignedTo() != null)
                .collect(Collectors.groupingBy(PmBug::getAssignedTo));
        List<Map<String, Object>> users = new ArrayList<>();
        Map<Long, PmUser> userMap = byUser.isEmpty() ? new HashMap<>()
                : pmUserDao.selectListByIds(byUser.keySet()).stream()
                .collect(Collectors.toMap(PmUser::getId, u -> u, (a, b) -> a));
        for (Map.Entry<Long, List<PmBug>> entry : byUser.entrySet()) {
            PmUser user = userMap.get(entry.getKey());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", entry.getKey());
            row.put("realName", user == null ? String.valueOf(entry.getKey()) : user.getRealName());
            row.put("total", entry.getValue().size());
            row.put("resolved", entry.getValue().stream().filter(b -> "resolved".equals(b.getStatus()) || "closed".equals(b.getStatus())).count());
            row.put("closed", entry.getValue().stream().filter(b -> "closed".equals(b.getStatus())).count());
            row.put("active", entry.getValue().stream().filter(b -> "active".equals(b.getStatus())).count());
            users.add(row);
        }
        users.sort(Comparator.comparing(r -> String.valueOf(r.get("realName"))));
        result.put("users", users);
        return R.ok(result);
    }

    /** 项目概览(项目详情聚合) */
    @GetMapping("/project/overview/{id}")
    public R<Map<String, Object>> projectOverview(@PathVariable Long id) {
        PmProject project = pmProjectDao.selectOneById(id);
        if (project == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", project);
        result.put("taskTotal", pmTaskDao.selectCountByQuery(QueryWrapper.create().where(PM_TASK.PROJECT_ID.eq(id))));
        result.put("taskDone", pmTaskDao.selectCountByQuery(QueryWrapper.create()
                .where(PM_TASK.PROJECT_ID.eq(id).and(PM_TASK.STATUS.in("done", "closed")))));
        result.put("bugTotal", pmBugDao.selectCountByQuery(QueryWrapper.create().where(PM_BUG.PROJECT_ID.eq(id))));
        result.put("bugActive", pmBugDao.selectCountByQuery(QueryWrapper.create()
                .where(PM_BUG.PROJECT_ID.eq(id).and(PM_BUG.STATUS.eq("active")))));
        result.put("fileCount", pmFileDao.selectCountByQuery(QueryWrapper.create().where(PM_FILE.PROJECT_ID.eq(id))));
        List<PmWorklog> logs = pmWorklogDao.selectListByQuery(QueryWrapper.create().where(PM_WORKLOG.PROJECT_ID.eq(id)));
        result.put("actualHours", logs.stream().map(PmWorklog::getHours)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        List<PmTask> tasks = pmTaskDao.selectListByQuery(QueryWrapper.create().where(PM_TASK.PROJECT_ID.eq(id)));
        result.put("estimateHours", tasks.stream().map(PmTask::getEstimateHours)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        return R.ok(result);
    }

    // ==================== 工时记录 ====================

    @GetMapping("/worklog/page")
    public R<PageResult<PmWorklog>> worklogPage(PageQuery pageQuery,
                                                @RequestParam(required = false) Long projectId,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) String month) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (projectId != null) {
            wrapper.and(PM_WORKLOG.PROJECT_ID.eq(projectId));
        }
        if (userId != null) {
            wrapper.and(PM_WORKLOG.USER_ID.eq(userId));
        }
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            YearMonth ym = YearMonth.parse(month);
            wrapper.and(PM_WORKLOG.WORK_DATE.ge(ym.atDay(1)))
                    .and(PM_WORKLOG.WORK_DATE.le(ym.atEndOfMonth()));
        }
        wrapper.orderBy(PM_WORKLOG.WORK_DATE, false);
        Page<PmWorklog> page = pmWorklogDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillWorklogs(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    /** 登记工时(本人；经理可代登) */
    @PostMapping("/worklog")
    public R<Void> saveWorklog(@RequestBody PmWorklog worklog) {
        auth.requireRole("dev", "tester", "manager", "product");
        worklog.setId(null);
        worklog.setSys001(java.time.LocalDateTime.now());
        worklog.setSys005(1);
        boolean isManagerRole;
        PmUser me = auth.currentPmUser();
        isManagerRole = me == null || "manager".equals(me.getRole()) || "admin".equals(me.getRole());
        if (worklog.getUserId() == null || !isManagerRole) {
            worklog.setUserId(auth.currentPmUserId());
        }
        if (worklog.getWorkDate() == null) {
            worklog.setWorkDate(LocalDate.now());
        }
        return pmWorklogDao.insert(worklog) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/worklog/remove/{id}")
    public R<Void> removeWorklog(@PathVariable Long id) {
        PmWorklog log = pmWorklogDao.selectOneById(id);
        if (log == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        PmUser me = auth.currentPmUser();
        boolean isManagerRole = me == null || "manager".equals(me.getRole()) || "admin".equals(me.getRole());
        if (!isManagerRole && (me == null || !me.getId().equals(log.getUserId()))) {
            return R.fail(GlobalErrorCode.FORBIDDEN.getCode(), "只能删除自己的工时记录");
        }
        return pmWorklogDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    /** 日历视图：某月按日汇总工时(userId空=本人) */
    @GetMapping("/worklog/calendar")
    public R<List<Map<String, Object>>> worklogCalendar(@RequestParam String month,
                                                        @RequestParam(required = false) Long projectId,
                                                        @RequestParam(required = false) Long userId) {
        if (!month.matches("\\d{4}-\\d{2}")) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "月份格式应为yyyy-MM");
        }
        Long target = userId != null ? userId : auth.currentPmUserId();
        YearMonth ym = YearMonth.parse(month);
        QueryWrapper wrapper = QueryWrapper.create()
                .where(PM_WORKLOG.USER_ID.eq(target))
                .and(PM_WORKLOG.WORK_DATE.ge(ym.atDay(1)))
                .and(PM_WORKLOG.WORK_DATE.le(ym.atEndOfMonth()));
        if (projectId != null) {
            wrapper.and(PM_WORKLOG.PROJECT_ID.eq(projectId));
        }
        List<PmWorklog> logs = pmWorklogDao.selectListByQuery(wrapper);
        Map<LocalDate, BigDecimal> byDate = logs.stream()
                .filter(l -> l.getWorkDate() != null)
                .collect(Collectors.groupingBy(PmWorklog::getWorkDate,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, l -> l.getHours() == null ? BigDecimal.ZERO : l.getHours(), BigDecimal::add)));
        List<Map<String, Object>> result = new ArrayList<>();
        byDate.forEach((date, hours) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            item.put("hours", hours);
            result.add(item);
        });
        return R.ok(result);
    }

    // ==================== 项目文件 ====================

    @GetMapping("/file/page")
    public R<PageResult<PmFile>> filePage(PageQuery pageQuery,
                                          @RequestParam(required = false) Long projectId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String fileCategory) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (projectId != null) {
            wrapper.and(PM_FILE.PROJECT_ID.eq(projectId));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_FILE.FILE_NAME.like(keyword));
        }
        if (fileCategory != null && !fileCategory.isEmpty()) {
            wrapper.and(PM_FILE.FILE_CATEGORY.eq(fileCategory));
        }
        wrapper.orderBy(PM_FILE.ID, false);
        Page<PmFile> page = pmFileDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillFiles(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    @PostMapping("/file")
    public R<Void> saveFile(@RequestBody PmFile file) {
        auth.requireRole("manager", "dev", "tester", "product");
        file.setId(null);
        file.setSys001(java.time.LocalDateTime.now());
        file.setSys005(1);
        file.setUploaderId(auth.currentPmUserId());
        return pmFileDao.insert(file) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/file/remove/{id}")
    public R<Void> removeFile(@PathVariable Long id) {
        PmFile file = pmFileDao.selectOneById(id);
        if (file == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        PmUser me = auth.currentPmUser();
        boolean isManagerRole = me == null || "manager".equals(me.getRole()) || "admin".equals(me.getRole());
        if (!isManagerRole && (me == null || !me.getId().equals(file.getUploaderId()))) {
            return R.fail(GlobalErrorCode.FORBIDDEN.getCode(), "只能删除自己上传的文件");
        }
        return pmFileDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 名称回填 ====================

    private Map<Long, PmUser> loadUserMap(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return pmUserDao.selectListByIds(ids.stream().distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(PmUser::getId, u -> u, (a, b) -> a));
    }

    private void fillTasks(List<PmTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Map<Long, PmUser> userMap = loadUserMap(tasks.stream()
                .map(PmTask::getAssignedTo).filter(Objects::nonNull).collect(Collectors.toList()));
        List<Long> storyIds = tasks.stream()
                .map(PmTask::getStoryId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> storyMap = storyIds.isEmpty() ? new HashMap<>()
                : pmStoryDao.selectListByIds(storyIds).stream()
                .collect(Collectors.toMap(PmStory::getId, PmStory::getTitle, (a, b) -> a));
        tasks.forEach(t -> {
            PmUser u = userMap.get(t.getAssignedTo());
            t.setAssignedToName(u == null ? "" : u.getRealName());
            t.setStoryTitle(storyMap.get(t.getStoryId()));
        });
    }

    private void fillBugs(List<PmBug> bugs) {
        if (bugs == null || bugs.isEmpty()) {
            return;
        }
        Map<Long, PmUser> userMap = loadUserMap(bugs.stream().flatMap(b -> java.util.stream.Stream.of(
                b.getOpenedBy(), b.getAssignedTo(), b.getResolvedBy()))
                .filter(Objects::nonNull).collect(Collectors.toList()));
        bugs.forEach(b -> {
            PmUser opened = userMap.get(b.getOpenedBy());
            PmUser assigned = userMap.get(b.getAssignedTo());
            PmUser resolved = userMap.get(b.getResolvedBy());
            b.setOpenedByName(opened == null ? "" : opened.getRealName());
            b.setAssignedToName(assigned == null ? "" : assigned.getRealName());
            b.setResolvedByName(resolved == null ? "" : resolved.getRealName());
        });
    }

    private void fillWorklogs(List<PmWorklog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        Map<Long, PmUser> userMap = loadUserMap(logs.stream()
                .map(PmWorklog::getUserId).filter(Objects::nonNull).collect(Collectors.toList()));
        List<Long> projectIds = logs.stream()
                .map(PmWorklog::getProjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> taskIds = logs.stream()
                .map(PmWorklog::getTaskId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> storyIds = logs.stream()
                .map(PmWorklog::getStoryId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> projectMap = projectIds.isEmpty() ? new HashMap<>()
                : pmProjectDao.selectListByIds(projectIds).stream()
                .collect(Collectors.toMap(PmProject::getId, PmProject::getName, (a, b) -> a));
        Map<Long, String> taskMap = taskIds.isEmpty() ? new HashMap<>()
                : pmTaskDao.selectListByIds(taskIds).stream()
                .collect(Collectors.toMap(PmTask::getId, PmTask::getName, (a, b) -> a));
        Map<Long, String> storyMap = storyIds.isEmpty() ? new HashMap<>()
                : pmStoryDao.selectListByIds(storyIds).stream()
                .collect(Collectors.toMap(PmStory::getId, PmStory::getTitle, (a, b) -> a));
        logs.forEach(l -> {
            PmUser u = userMap.get(l.getUserId());
            l.setUserName(u == null ? "" : u.getUsername());
            l.setRealName(u == null ? "" : u.getRealName());
            l.setProjectName(projectMap.get(l.getProjectId()));
            l.setTaskName(taskMap.get(l.getTaskId()));
            l.setStoryTitle(storyMap.get(l.getStoryId()));
        });
    }

    private void fillFiles(List<PmFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        Map<Long, PmUser> userMap = loadUserMap(files.stream()
                .map(PmFile::getUploaderId).filter(Objects::nonNull).collect(Collectors.toList()));
        List<Long> projectIds = files.stream()
                .map(PmFile::getProjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> projectMap = projectIds.isEmpty() ? new HashMap<>()
                : pmProjectDao.selectListByIds(projectIds).stream()
                .collect(Collectors.toMap(PmProject::getId, PmProject::getName, (a, b) -> a));
        files.forEach(f -> {
            PmUser u = userMap.get(f.getUploaderId());
            f.setUploaderName(u == null ? "" : u.getRealName());
            f.setProjectName(projectMap.get(f.getProjectId()));
        });
    }
}
