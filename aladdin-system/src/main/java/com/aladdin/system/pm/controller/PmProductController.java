package com.aladdin.system.pm.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.system.pm.dao.PmModuleDao;
import com.aladdin.system.pm.dao.PmPlanDao;
import com.aladdin.system.pm.dao.PmProductDao;
import com.aladdin.system.pm.dao.PmProgramDao;
import com.aladdin.system.pm.dao.PmReleaseDao;
import com.aladdin.system.pm.dao.PmStoryDao;
import com.aladdin.system.pm.entity.*;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.aladdin.system.pm.entity.table.PmModuleTableDef.PM_MODULE;
import static com.aladdin.system.pm.entity.table.PmPlanTableDef.PM_PLAN;
import static com.aladdin.system.pm.entity.table.PmProductTableDef.PM_PRODUCT;
import static com.aladdin.system.pm.entity.table.PmProgramTableDef.PM_PROGRAM;
import static com.aladdin.system.pm.entity.table.PmReleaseTableDef.PM_RELEASE;
import static com.aladdin.system.pm.entity.table.PmStoryTableDef.PM_STORY;

/**
 * 项目管理系统-产品域(产品/模块/计划/需求/发布)
 *
 * @author cles
 * @date 2026/11/02
 */
@RestController
@RequestMapping("/pm")
public class PmProductController {

    private final PmProductDao pmProductDao;
    private final PmModuleDao pmModuleDao;
    private final PmPlanDao pmPlanDao;
    private final PmStoryDao pmStoryDao;
    private final PmReleaseDao pmReleaseDao;
    private final PmProgramDao pmProgramDao;
    private final PmAuthHelper auth;

    public PmProductController(PmProductDao pmProductDao, PmModuleDao pmModuleDao, PmPlanDao pmPlanDao,
                               PmStoryDao pmStoryDao, PmReleaseDao pmReleaseDao, PmProgramDao pmProgramDao,
                               PmAuthHelper auth) {
        this.pmProductDao = pmProductDao;
        this.pmModuleDao = pmModuleDao;
        this.pmPlanDao = pmPlanDao;
        this.pmStoryDao = pmStoryDao;
        this.pmReleaseDao = pmReleaseDao;
        this.pmProgramDao = pmProgramDao;
        this.auth = auth;
    }

    // ==================== 产品 ====================

    @GetMapping("/product/page")
    public R<PageResult<PmProduct>> productPage(PageQuery pageQuery,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long programId) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_PRODUCT.NAME.like(keyword));
        }
        if (programId != null) {
            wrapper.and(PM_PRODUCT.PROGRAM_ID.eq(programId));
        }
        wrapper.orderBy(PM_PRODUCT.ID, false);
        Page<PmProduct> page = pmProductDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillProductDisplay(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    /** 产品列表(下拉用) */
    @GetMapping("/product/list")
    public R<List<PmProduct>> productList(@RequestParam(required = false) Long programId) {
        QueryWrapper wrapper = QueryWrapper.create().where(PM_PRODUCT.STATUS.eq(1));
        if (programId != null) {
            wrapper.and(PM_PRODUCT.PROGRAM_ID.eq(programId));
        }
        wrapper.orderBy(PM_PRODUCT.ID, true);
        return R.ok(pmProductDao.selectListByQuery(wrapper));
    }

    @PostMapping("/product")
    public R<Void> saveProduct(@RequestBody PmProduct product) {
        auth.requireRole("product");
        product.setId(null);
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getOwnerId() == null && auth.currentPmUserId() != null) {
            product.setOwnerId(auth.currentPmUserId());
        }
        product.setSys001(LocalDateTime.now());
        product.setSys005(1);
        return pmProductDao.insert(product) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/product/edit")
    public R<Void> editProduct(@RequestBody PmProduct product) {
        auth.requireRole("product");
        return pmProductDao.update(product) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/product/remove/{id}")
    public R<Void> removeProduct(@PathVariable Long id) {
        auth.requireRole("product");
        return pmProductDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 模块 ====================

    @GetMapping("/module/list")
    public R<List<PmModule>> moduleList(@RequestParam Long productId) {
        return R.ok(pmModuleDao.selectListByQuery(QueryWrapper.create()
                .where(PM_MODULE.PRODUCT_ID.eq(productId)).orderBy(PM_MODULE.SORT, true)));
    }

    @PostMapping("/module")
    public R<Void> saveModule(@RequestBody PmModule module) {
        auth.requireRole("product");
        module.setId(null);
        module.setSys001(LocalDateTime.now());
        module.setSys005(1);
        if (module.getParentId() == null) {
            module.setParentId(0L);
        }
        return pmModuleDao.insert(module) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/module/edit")
    public R<Void> editModule(@RequestBody PmModule module) {
        auth.requireRole("product");
        return pmModuleDao.update(module) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/module/remove/{id}")
    public R<Void> removeModule(@PathVariable Long id) {
        auth.requireRole("product");
        return pmModuleDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 计划 ====================

    @GetMapping("/plan/list")
    public R<List<PmPlan>> planList(@RequestParam Long productId) {
        return R.ok(pmPlanDao.selectListByQuery(QueryWrapper.create()
                .where(PM_PLAN.PRODUCT_ID.eq(productId)).orderBy(PM_PLAN.ID, false)));
    }

    @PostMapping("/plan")
    public R<Void> savePlan(@RequestBody PmPlan plan) {
        auth.requireRole("product");
        plan.setId(null);
        plan.setSys001(LocalDateTime.now());
        plan.setSys005(1);
        if (plan.getStatus() == null) {
            plan.setStatus(1);
        }
        return pmPlanDao.insert(plan) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/plan/edit")
    public R<Void> editPlan(@RequestBody PmPlan plan) {
        auth.requireRole("product");
        return pmPlanDao.update(plan) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/plan/remove/{id}")
    public R<Void> removePlan(@PathVariable Long id) {
        auth.requireRole("product");
        return pmPlanDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 需求 ====================

    @GetMapping("/story/page")
    public R<PageResult<PmStory>> storyPage(PageQuery pageQuery,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long productId,
                                            @RequestParam(required = false) Long moduleId,
                                            @RequestParam(required = false) Long planId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Integer priority) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_STORY.TITLE.like(keyword));
        }
        if (productId != null) {
            wrapper.and(PM_STORY.PRODUCT_ID.eq(productId));
        }
        if (moduleId != null) {
            wrapper.and(PM_STORY.MODULE_ID.eq(moduleId));
        }
        if (planId != null) {
            wrapper.and(PM_STORY.PLAN_ID.eq(planId));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.and(PM_STORY.STATUS.eq(status));
        }
        if (priority != null) {
            wrapper.and(PM_STORY.PRIORITY.eq(priority));
        }
        wrapper.orderBy(PM_STORY.ID, false);
        Page<PmStory> page = pmStoryDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillStories(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    @GetMapping("/story/list")
    public R<List<PmStory>> storyList(@RequestParam(required = false) Long productId,
                                      @RequestParam(required = false) String status) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (productId != null) {
            wrapper.and(PM_STORY.PRODUCT_ID.eq(productId));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.and(PM_STORY.STATUS.eq(status));
        }
        wrapper.orderBy(PM_STORY.ID, false);
        List<PmStory> list = pmStoryDao.selectListByQuery(wrapper);
        fillStories(list);
        return R.ok(list);
    }

    @PostMapping("/story")
    public R<Void> saveStory(@RequestBody PmStory story) {
        auth.requireRole("product");
        story.setId(null);
        if (story.getStatus() == null) {
            story.setStatus("draft");
        }
        story.setSys001(LocalDateTime.now());
        story.setSys005(1);
        return pmStoryDao.insert(story) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/story/edit")
    public R<Void> editStory(@RequestBody PmStory story) {
        auth.requireRole("product");
        return pmStoryDao.update(story) > 0 ? R.ok() : R.fail();
    }

    /** 需求状态变更: active激活 closed关闭(需关闭原因) */
    @PostMapping("/story/change-status/{id}")
    public R<Void> changeStoryStatus(@PathVariable Long id, @RequestBody PmStory body) {
        auth.requireRole("product");
        PmStory db = pmStoryDao.selectOneById(id);
        if (db == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        PmStory update = new PmStory();
        update.setId(id);
        update.setStatus(body.getStatus());
        update.setClosedReason(body.getClosedReason());
        return pmStoryDao.update(update) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/story/remove/{id}")
    public R<Void> removeStory(@PathVariable Long id) {
        auth.requireRole("product");
        return pmStoryDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 发布 ====================

    @GetMapping("/release/list")
    public R<List<PmRelease>> releaseList(@RequestParam(required = false) Long productId) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (productId != null) {
            wrapper.and(PM_RELEASE.PRODUCT_ID.eq(productId));
        }
        wrapper.orderBy(PM_RELEASE.ID, false);
        List<PmRelease> list = pmReleaseDao.selectListByQuery(wrapper);
        if (!list.isEmpty()) {
            Map<Long, String> nameMap = pmProductDao.selectListByIds(list.stream()
                    .map(PmRelease::getProductId).filter(Objects::nonNull).distinct().collect(Collectors.toList()))
                    .stream().collect(Collectors.toMap(PmProduct::getId, PmProduct::getName, (a, b) -> a));
            list.forEach(r -> r.setProductName(nameMap.get(r.getProductId())));
        }
        return R.ok(list);
    }

    @PostMapping("/release")
    public R<Void> saveRelease(@RequestBody PmRelease release) {
        auth.requireRole("product");
        release.setId(null);
        release.setSys001(LocalDateTime.now());
        release.setSys005(1);
        if (release.getStatus() == null) {
            release.setStatus(0);
        }
        return pmReleaseDao.insert(release) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/release/edit")
    public R<Void> editRelease(@RequestBody PmRelease release) {
        auth.requireRole("product");
        return pmReleaseDao.update(release) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/release/remove/{id}")
    public R<Void> removeRelease(@PathVariable Long id) {
        auth.requireRole("product");
        return pmReleaseDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 名称回填 ====================

    private void fillProductDisplay(List<PmProduct> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> programIds = products.stream()
                .map(PmProduct::getProgramId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> programMap = programIds.isEmpty() ? new HashMap<>()
                : pmProgramDao.selectListByIds(programIds).stream()
                .collect(Collectors.toMap(PmProgram::getId, PmProgram::getName, (a, b) -> a));
        products.forEach(p -> p.setProgramName(programMap.get(p.getProgramId())));
    }

    private void fillStories(List<PmStory> stories) {
        if (stories == null || stories.isEmpty()) {
            return;
        }
        List<Long> productIds = stories.stream()
                .map(PmStory::getProductId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> moduleIds = stories.stream()
                .map(PmStory::getModuleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> planIds = stories.stream()
                .map(PmStory::getPlanId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> productMap = productIds.isEmpty() ? new HashMap<>()
                : pmProductDao.selectListByIds(productIds).stream()
                .collect(Collectors.toMap(PmProduct::getId, PmProduct::getName, (a, b) -> a));
        Map<Long, String> moduleMap = moduleIds.isEmpty() ? new HashMap<>()
                : pmModuleDao.selectListByIds(moduleIds).stream()
                .collect(Collectors.toMap(PmModule::getId, PmModule::getName, (a, b) -> a));
        Map<Long, String> planMap = planIds.isEmpty() ? new HashMap<>()
                : pmPlanDao.selectListByIds(planIds).stream()
                .collect(Collectors.toMap(PmPlan::getId, PmPlan::getTitle, (a, b) -> a));
        stories.forEach(s -> {
            s.setProductName(productMap.get(s.getProductId()));
            s.setModuleName(moduleMap.get(s.getModuleId()));
            s.setPlanTitle(planMap.get(s.getPlanId()));
        });
    }
}
