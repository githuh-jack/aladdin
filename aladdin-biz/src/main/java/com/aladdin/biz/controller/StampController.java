package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizStampDao;
import com.aladdin.biz.dao.BizUserStampDao;
import com.aladdin.biz.entity.BizStamp;
import com.aladdin.biz.entity.BizUserStamp;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邮票控制器
 * 用户: 浏览上架邮票、查看我的邮票
 * 管理员: CRUD 邮票
 *
 * @author cles
 * @date 2026/09/15
 */
@RestController
@RequestMapping({"/stamp", "/biz/stamp"})
public class StampController {

    @Autowired
    private BizStampDao stampDao;

    @Autowired
    private BizUserStampDao userStampDao;

    /** 浏览上架邮票 */
    @GetMapping("/shop")
    public R<List<BizStamp>> shopList() {
        return R.ok(stampDao.selectOnShelfList());
    }

    /** 我的邮票 */
    @GetMapping("/mine")
    public R<List<BizUserStamp>> mine() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(userStampDao.selectMyStamps(userId));
    }

    /** 主题收藏进度(已使用+未使用均算) */
    @GetMapping("/collection")
    public R<List<Map<String, Object>>> collection() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        Map<String, Long> collected = new HashMap<>();
        for (Map<String, Object> row : stampDao.selectCollectedByTheme(userId)) {
            Object theme = row.get("theme");
            Object num = row.get("collected");
            if (theme != null && num != null) {
                collected.put(theme.toString(), Long.valueOf(num.toString()));
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : stampDao.selectThemeTotals()) {
            String theme = row.get("theme") == null ? "未分类" : row.get("theme").toString();
            long total = row.get("total") == null ? 0 : Long.parseLong(row.get("total").toString());
            Map<String, Object> item = new HashMap<>();
            item.put("theme", theme);
            item.put("total", total);
            item.put("collected", collected.getOrDefault(theme, 0L));
            result.add(item);
        }
        return R.ok(result);
    }

    /** 详情 */
    @GetMapping("/detail/{id}")
    public R<BizStamp> detail(@PathVariable Long id) {
        BizStamp stamp = stampDao.selectOneById(id);
        if (stamp == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        return R.ok(stamp);
    }

    // ===== 管理员接口 =====

    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:stamp:list') or hasRole('ROLE_admin')")
    public R<List<BizStamp>> adminList() {
        return R.ok(stampDao.selectAllList());
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasAuthority('biz:stamp:add') or hasRole('ROLE_admin')")
    public R<Void> add(@RequestBody BizStamp stamp) {
        stamp.setSys001(LocalDateTime.now());
        stamp.setSys005(1);
        stamp.setSys006("biz");
        stampDao.insert(stamp);
        return R.ok();
    }

    @PostMapping("/admin/edit")
    @PreAuthorize("hasAuthority('biz:stamp:edit') or hasRole('ROLE_admin')")
    public R<Void> edit(@RequestBody BizStamp stamp) {
        stamp.setSys002(LocalDateTime.now());
        stampDao.update(stamp);
        return R.ok();
    }

    @PostMapping("/admin/remove/{id}")
    @PreAuthorize("hasAuthority('biz:stamp:remove') or hasRole('ROLE_admin')")
    public R<Void> remove(@PathVariable Long id) {
        stampDao.deleteById(id);
        return R.ok();
    }
}
