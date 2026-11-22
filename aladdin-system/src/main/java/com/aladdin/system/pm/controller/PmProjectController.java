package com.aladdin.system.pm.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.system.pm.dao.*;
import com.aladdin.system.pm.entity.*;
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

import static com.aladdin.system.pm.entity.table.PmProjectProductTableDef.PM_PROJECT_PRODUCT;
import static com.aladdin.system.pm.entity.table.PmProjectStoryTableDef.PM_PROJECT_STORY;
import static com.aladdin.system.pm.entity.table.PmProjectTableDef.PM_PROJECT;
import static com.aladdin.system.pm.entity.table.PmStoryTableDef.PM_STORY;
import static com.aladdin.system.pm.entity.table.PmTaskTableDef.PM_TASK;
import static com.aladdin.system.pm.entity.table.PmTeamTableDef.PM_TEAM;

/**
 * 项目管理系统-项目执行域(项目/团队/任务)
 *
 * @author cles
 * @date 2026/11/02
 */
@RestController
@RequestMapping("/pm")
public class PmProjectController {

    private final PmProjectDao pmProjectDao;
    private final PmProjectProductDao pmProjectProductDao;
    private final PmProjectStoryDao pmProjectStoryDao;
    private final PmTeamDao pmTeamDao;
    private final PmTaskDao pmTaskDao;
    private final PmUserDao pmUserDao;
    private final PmProgramDao pmProgramDao;
    private final PmStoryDao pmStoryDao;
    private final PmAuthHelper auth;

    public PmProjectController(PmProjectDao pmProjectDao, PmProjectProductDao pmProjectProductDao,
                               PmProjectStoryDao pmProjectStoryDao, PmTeamDao pmTeamDao, PmTaskDao pmTaskDao,
                               PmUserDao pmUserDao, PmProgramDao pmProgramDao, PmStoryDao pmStoryDao,
                               PmAuthHelper auth) {
        this.pmProjectDao = pmProjectDao;
        this.pmProjectProductDao = pmProjectProductDao;
        this.pmProjectStoryDao = pmProjectStoryDao;
        this.pmTeamDao = pmTeamDao;
        this.pmTaskDao = pmTaskDao;
        this.pmUserDao = pmUserDao;
        this.pmProgramDao = pmProgramDao;
        this.pmStoryDao = pmStoryDao;
        this.auth = auth;
    }

    // ==================== 项目 ====================

    @GetMapping("/project/page")
    public R<PageResult<PmProject>> page(PageQuery pageQuery,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long programId) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_PROJECT.NAME.like(keyword));
        }
        if (programId != null) {
            wrapper.and(PM_PROJECT.PROGRAM_ID.eq(programId));
        }
        wrapper.orderBy(PM_PROJECT.ID, false);
        Page<PmProject> page = pmProjectDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillProjects(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    @GetMapping("/project/list")
    public R<List<PmProject>> list() {
        List<PmProject> list = pmProjectDao.selectListByQuery(QueryWrapper.create()
                .where(PM_PROJECT.STATUS.eq(1)).orderBy(PM_PROJECT.ID, true));
        fillProjects(list);
        return R.ok(list);
    }

    @GetMapping("/project/detail/{id}")
    public R<PmProject> detail(@PathVariable Long id) {
        PmProject project = pmProjectDao.selectOneById(id);
        if (project == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        fillProjects(List.of(project));
        return R.ok(project);
    }

    /** 创建项目(含关联产品) */
    @PostMapping("/project")
    public R<Void> save(@RequestBody PmProject project) {
        auth.requireRole("manager");
        project.setId(null);
        project.setSys001(LocalDateTime.now());
        project.setSys005(1);
        if (project.getStatus() == null) {
            project.setStatus(1);
        }
        if (project.getManagerId() == null && auth.currentPmUserId() != null) {
            project.setManagerId(auth.currentPmUserId());
        }
        pmProjectDao.insert(project);
        saveProjectProducts(project.getId(), project.getProductIds());
        return R.ok();
    }

    @PostMapping("/project/edit")
    public R<Void> edit(@RequestBody PmProject project) {
        auth.requireRole("manager");
        pmProjectDao.update(project);
        if (project.getProductIds() != null) {
            pmProjectProductDao.deleteByQuery(QueryWrapper.create()
                    .where(PM_PROJECT_PRODUCT.PROJECT_ID.eq(project.getId())));
            saveProjectProducts(project.getId(), project.getProductIds());
        }
        return R.ok();
    }

    @PostMapping("/project/remove/{id}")
    public R<Void> remove(@PathVariable Long id) {
        auth.requireRole("manager");
        pmProjectDao.deleteById(id);
        pmProjectProductDao.deleteByQuery(QueryWrapper.create().where(PM_PROJECT_PRODUCT.PROJECT_ID.eq(id)));
        pmProjectStoryDao.deleteByQuery(QueryWrapper.create().where(PM_PROJECT_STORY.PROJECT_ID.eq(id)));
        pmTeamDao.deleteByQuery(QueryWrapper.create().where(PM_TEAM.PROJECT_ID.eq(id)));
        return R.ok();
    }

    /** 关联需求 */
    @PostMapping("/project/link-story")
    public R<Void> linkStory(@RequestBody Map<String, Object> body) {
        auth.requireRole("manager");
        Long projectId = Long.valueOf(String.valueOf(body.get("projectId")));
        @SuppressWarnings("unchecked")
        List<Number> storyIds = (List<Number>) body.get("storyIds");
        if (storyIds == null || storyIds.isEmpty()) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请选择需求");
        }
        for (Number storyId : storyIds) {
            Long sid = storyId.longValue();
            Long exists = pmProjectStoryDao.selectCountByQuery(QueryWrapper.create()
                    .where(PM_PROJECT_STORY.PROJECT_ID.eq(projectId)).and(PM_PROJECT_STORY.STORY_ID.eq(sid)));
            if (exists == null || exists == 0) {
                PmProjectStory link = new PmProjectStory();
                link.setProjectId(projectId);
                link.setStoryId(sid);
                pmProjectStoryDao.insert(link);
            }
        }
        return R.ok();
    }

    /** 项目已关联需求列表 */
    @GetMapping("/project/story-list")
    public R<List<PmStory>> projectStories(@RequestParam Long projectId) {
        List<PmProjectStory> links = pmProjectStoryDao.selectListByQuery(QueryWrapper.create()
                .where(PM_PROJECT_STORY.PROJECT_ID.eq(projectId)));
        if (links.isEmpty()) {
            return R.ok(List.of());
        }
        List<PmStory> stories = pmStoryDao.selectListByIds(links.stream()
                .map(PmProjectStory::getStoryId).distinct().collect(Collectors.toList()));
        stories.forEach(s -> {
            s.setProductName(null);
            s.setModuleName(null);
        });
        return R.ok(stories);
    }

    /** 移除项目关联需求 */
    @PostMapping("/project/unlink-story/{id}")
    public R<Void> unlinkStory(@PathVariable Long id) {
        auth.requireRole("manager");
        pmProjectStoryDao.deleteById(id);
        return R.ok();
    }

    // ==================== 团队 ====================

    @GetMapping("/team/list")
    public R<List<PmTeam>> teamList(@RequestParam Long projectId) {
        List<PmTeam> list = pmTeamDao.selectListByQuery(QueryWrapper.create()
                .where(PM_TEAM.PROJECT_ID.eq(projectId)).orderBy(PM_TEAM.ID, true));
        if (!list.isEmpty()) {
            Map<Long, PmUser> userMap = pmUserDao.selectListByIds(list.stream()
                    .map(PmTeam::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList()))
                    .stream().collect(Collectors.toMap(PmUser::getId, u -> u, (a, b) -> a));
            list.forEach(t -> {
                PmUser u = userMap.get(t.getUserId());
                if (u != null) {
                    t.setUserName(u.getUsername());
                    t.setRealName(u.getRealName());
                }
            });
        }
        return R.ok(list);
    }

    @PostMapping("/team")
    public R<Void> saveTeam(@RequestBody PmTeam team) {
        auth.requireRole("manager");
        team.setId(null);
        team.setSys001(LocalDateTime.now());
        team.setSys005(1);
        Long exists = pmTeamDao.selectCountByQuery(QueryWrapper.create()
                .where(PM_TEAM.PROJECT_ID.eq(team.getProjectId())).and(PM_TEAM.USER_ID.eq(team.getUserId())));
        if (exists != null && exists > 0) {
            return R.fail(GlobalErrorCode.DATA_DUPLICATE.getCode(), "该成员已在团队中");
        }
        return pmTeamDao.insert(team) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/team/remove/{id}")
    public R<Void> removeTeam(@PathVariable Long id) {
        auth.requireRole("manager");
        return pmTeamDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 任务 ====================

    @GetMapping("/task/page")
    public R<PageResult<PmTask>> taskPage(PageQuery pageQuery,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long projectId,
                                          @RequestParam(required = false) Long storyId,
                                          @RequestParam(required = false) Long assignedTo,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String type) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_TASK.NAME.like(keyword));
        }
        if (projectId != null) {
            wrapper.and(PM_TASK.PROJECT_ID.eq(projectId));
        }
        if (storyId != null) {
            wrapper.and(PM_TASK.STORY_ID.eq(storyId));
        }
        if (assignedTo != null) {
            wrapper.and(PM_TASK.ASSIGNED_TO.eq(assignedTo));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.and(PM_TASK.STATUS.eq(status));
        }
        if (type != null && !type.isEmpty()) {
            wrapper.and(PM_TASK.TYPE.eq(type));
        }
        wrapper.orderBy(PM_TASK.ID, false);
        Page<PmTask> page = pmTaskDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillTasks(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    /** 创建/分解任务(经理指派或从需求拆分；仅story拆分时可自动按项目-需求关联解析项目) */
    @PostMapping("/task")
    public R<Void> saveTask(@RequestBody PmTask task) {
        auth.requireRole("manager", "product");
        task.setId(null);
        task.setSys001(LocalDateTime.now());
        task.setSys005(1);
        if (task.getStatus() == null) {
            task.setStatus("wait");
        }
        if (task.getOpenedBy() == null) {
            task.setOpenedBy(auth.currentPmUserId());
        }
        if (task.getProjectId() == null && task.getStoryId() != null) {
            PmProjectStory link = pmProjectStoryDao.selectOneByQuery(QueryWrapper.create()
                    .where(PM_PROJECT_STORY.STORY_ID.eq(task.getStoryId())).limit(1));
            if (link != null) {
                task.setProjectId(link.getProjectId());
            }
        }
        return pmTaskDao.insert(task) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/task/edit")
    public R<Void> editTask(@RequestBody PmTask task) {
        auth.requireRole("manager", "dev");
        return pmTaskDao.update(task) > 0 ? R.ok() : R.fail();
    }

    /** 领取任务(研发/测试认领，任务指派给自己并开始) */
    @PostMapping("/task/claim/{id}")
    public R<Void> claimTask(@PathVariable Long id) {
        auth.requireRole("dev", "tester");
        PmTask task = pmTaskDao.selectOneById(id);
        if (task == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        PmTask update = new PmTask();
        update.setId(id);
        update.setAssignedTo(auth.currentPmUserId());
        if ("wait".equals(task.getStatus())) {
            update.setStatus("doing");
        }
        return pmTaskDao.update(update) > 0 ? R.ok() : R.fail();
    }

    /** 更新任务状态 wait/doing/done/closed(完成时记录完成时间) */
    @PostMapping("/task/status/{id}")
    public R<Void> taskStatus(@PathVariable Long id, @RequestParam String status) {
        auth.requireRole("dev", "tester", "manager");
        PmTask update = new PmTask();
        update.setId(id);
        update.setStatus(status);
        if ("done".equals(status)) {
            update.setFinishedTime(LocalDateTime.now());
        }
        return pmTaskDao.update(update) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/task/remove/{id}")
    public R<Void> removeTask(@PathVariable Long id) {
        auth.requireRole("manager");
        return pmTaskDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 私有 ====================

    private void saveProjectProducts(Long projectId, List<Long> productIds) {
        if (productIds == null) {
            return;
        }
        for (Long productId : productIds) {
            PmProjectProduct link = new PmProjectProduct();
            link.setProjectId(projectId);
            link.setProductId(productId);
            pmProjectProductDao.insert(link);
        }
    }

    private void fillProjects(List<PmProject> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        List<Long> programIds = projects.stream()
                .map(PmProject::getProgramId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> managerIds = projects.stream()
                .map(PmProject::getManagerId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> programMap = programIds.isEmpty() ? new HashMap<>()
                : pmProgramDao.selectListByIds(programIds).stream()
                .collect(Collectors.toMap(PmProgram::getId, PmProgram::getName, (a, b) -> a));
        Map<Long, PmUser> userMap = managerIds.isEmpty() ? new HashMap<>()
                : pmUserDao.selectListByIds(managerIds).stream()
                .collect(Collectors.toMap(PmUser::getId, u -> u, (a, b) -> a));
        projects.forEach(p -> {
            p.setProgramName(programMap.get(p.getProgramId()));
            PmUser manager = userMap.get(p.getManagerId());
            p.setManagerName(manager == null ? "" : manager.getRealName());
        });
    }

    private void fillTasks(List<PmTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<Long> assigneeIds = tasks.stream()
                .map(PmTask::getAssignedTo).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> storyIds = tasks.stream()
                .map(PmTask::getStoryId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, PmUser> userMap = assigneeIds.isEmpty() ? new HashMap<>()
                : pmUserDao.selectListByIds(assigneeIds).stream()
                .collect(Collectors.toMap(PmUser::getId, u -> u, (a, b) -> a));
        Map<Long, String> storyMap = storyIds.isEmpty() ? new HashMap<>()
                : pmStoryDao.selectListByIds(storyIds).stream()
                .collect(Collectors.toMap(PmStory::getId, PmStory::getTitle, (a, b) -> a));
        tasks.forEach(t -> {
            PmUser u = userMap.get(t.getAssignedTo());
            t.setAssignedToName(u == null ? "" : u.getRealName());
            t.setStoryTitle(storyMap.get(t.getStoryId()));
        });
    }
}
