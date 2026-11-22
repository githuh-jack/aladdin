package com.aladdin.system.pm.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.system.pm.dao.PmProgramDao;
import com.aladdin.system.pm.dao.PmStakeholderDao;
import com.aladdin.system.pm.dao.PmUserDao;
import com.aladdin.system.pm.entity.PmProgram;
import com.aladdin.system.pm.entity.PmStakeholder;
import com.aladdin.system.pm.entity.PmUser;
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

import static com.aladdin.system.pm.entity.table.PmProgramTableDef.PM_PROGRAM;
import static com.aladdin.system.pm.entity.table.PmStakeholderTableDef.PM_STAKEHOLDER;

/**
 * 项目管理系统-项目集域(项目集/干系人)
 *
 * @author cles
 * @date 2026/11/02
 */
@RestController
@RequestMapping("/pm")
public class PmProgramController {

    private final PmProgramDao pmProgramDao;
    private final PmStakeholderDao pmStakeholderDao;
    private final PmUserDao pmUserDao;
    private final PmAuthHelper auth;

    public PmProgramController(PmProgramDao pmProgramDao, PmStakeholderDao pmStakeholderDao,
                               PmUserDao pmUserDao, PmAuthHelper auth) {
        this.pmProgramDao = pmProgramDao;
        this.pmStakeholderDao = pmStakeholderDao;
        this.pmUserDao = pmUserDao;
        this.auth = auth;
    }

    @GetMapping("/program/page")
    public R<PageResult<PmProgram>> page(PageQuery pageQuery,
                                         @RequestParam(required = false) String keyword) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_PROGRAM.NAME.like(keyword));
        }
        wrapper.orderBy(PM_PROGRAM.ID, false);
        Page<PmProgram> page = pmProgramDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillOwners(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    /** 全部项目集(下拉用) */
    @GetMapping("/program/list")
    public R<List<PmProgram>> list() {
        List<PmProgram> list = pmProgramDao.selectListByQuery(QueryWrapper.create()
                .where(PM_PROGRAM.STATUS.eq(1)).orderBy(PM_PROGRAM.ID, true));
        fillOwners(list);
        return R.ok(list);
    }

    @GetMapping("/program/detail/{id}")
    public R<PmProgram> detail(@PathVariable Long id) {
        PmProgram program = pmProgramDao.selectOneById(id);
        if (program == null) {
            return R.fail(GlobalErrorCode.DATA_NOT_FOUND);
        }
        fillOwners(List.of(program));
        return R.ok(program);
    }

    @PostMapping("/program")
    public R<Void> save(@RequestBody PmProgram program) {
        auth.requireRole("owner");
        program.setId(null);
        if (program.getStatus() == null) {
            program.setStatus(1);
        }
        // 负责人默认为当前登录pm用户
        if (program.getOwnerId() == null && auth.currentPmUserId() != null) {
            program.setOwnerId(auth.currentPmUserId());
        }
        program.setSys001(LocalDateTime.now());
        program.setSys005(1);
        return pmProgramDao.insert(program) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/program/edit")
    public R<Void> edit(@RequestBody PmProgram program) {
        auth.requireRole("owner");
        return pmProgramDao.update(program) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/program/remove/{id}")
    public R<Void> remove(@PathVariable Long id) {
        auth.requireRole("owner");
        return pmProgramDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 干系人 ====================

    @GetMapping("/stakeholder/list")
    public R<List<PmStakeholder>> stakeholderList(@RequestParam Long programId) {
        return R.ok(pmStakeholderDao.selectListByQuery(QueryWrapper.create()
                .where(PM_STAKEHOLDER.PROGRAM_ID.eq(programId)).orderBy(PM_STAKEHOLDER.ID, true)));
    }

    @PostMapping("/stakeholder")
    public R<Void> saveStakeholder(@RequestBody PmStakeholder stakeholder) {
        auth.requireRole("owner");
        stakeholder.setId(null);
        stakeholder.setSys001(LocalDateTime.now());
        stakeholder.setSys005(1);
        return pmStakeholderDao.insert(stakeholder) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/stakeholder/remove/{id}")
    public R<Void> removeStakeholder(@PathVariable Long id) {
        auth.requireRole("owner");
        return pmStakeholderDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    private void fillOwners(List<PmProgram> programs) {
        if (programs == null || programs.isEmpty()) {
            return;
        }
        List<Long> ownerIds = programs.stream()
                .map(PmProgram::getOwnerId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, PmUser> userMap = ownerIds.isEmpty() ? new HashMap<>()
                : pmUserDao.selectListByIds(ownerIds).stream()
                .collect(Collectors.toMap(PmUser::getId, Function.identity(), (a, b) -> a));
        programs.forEach(p -> {
            PmUser owner = userMap.get(p.getOwnerId());
            p.setOwnerName(owner == null ? "" : owner.getRealName());
        });
    }
}
