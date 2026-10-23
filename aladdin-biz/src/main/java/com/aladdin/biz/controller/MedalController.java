package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizMedalDao;
import com.aladdin.biz.dao.BizUserMedalDao;
import com.aladdin.biz.entity.BizMedal;
import com.aladdin.biz.entity.BizUserMedal;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 奖章控制器
 * 用户：用户中心展示我的奖章(全部定义+是否获得)
 * 管理员：奖章定义管理(列表/新增/编辑)
 *
 * @author cles
 * @date 2026/10/14
 */
@RestController
@RequestMapping({"/medal", "/biz/medal"})
public class MedalController {

    @Autowired
    private BizMedalDao medalDao;

    @Autowired
    private BizUserMedalDao userMedalDao;

    /** 用户中心: 全部奖章+我是否获得(earned=true时earnedTime为获得时间) */
    @GetMapping("/my")
    public R<List<Map<String, Object>>> my() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        List<BizMedal> medals = medalDao.selectAllMedals();
        Map<Long, BizUserMedal> owned = new java.util.HashMap<>();
        for (BizUserMedal um : userMedalDao.selectByUserId(userId)) {
            owned.put(um.getMedalId(), um);
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>(medals.size());
        for (BizMedal m : medals) {
            BizUserMedal um = owned.get(m.getId());
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("medalId", m.getId());
            row.put("name", m.getName());
            row.put("icon", m.getIcon());
            row.put("description", m.getDescription());
            row.put("earned", um != null);
            row.put("earnedTime", um == null ? null : um.getSys001());
            result.add(row);
        }
        return R.ok(result);
    }

    // ===== 管理员接口 =====

    /** 管理员: 奖章定义列表 */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list') or hasRole('ROLE_admin')")
    public R<List<BizMedal>> adminList() {
        return R.ok(medalDao.selectAllMedals());
    }

    /** 管理员: 新增/编辑奖章定义 body: {id?, name, icon?, description?, status?} */
    @PostMapping("/admin/save")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> save(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null || "".equals(body.get("id").toString())
                ? null : Long.valueOf(body.get("id").toString());
        String name = body.get("name") == null ? "" : body.get("name").toString().trim();
        if (name.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "奖章名称不能为空");
        }
        if (name.length() > 50) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "奖章名称不能超过50字");
        }
        String icon = body.get("icon") == null ? "" : body.get("icon").toString().trim();
        String description = body.get("description") == null ? "" : body.get("description").toString().trim();
        BizMedal medal = new BizMedal();
        medal.setId(id);
        medal.setName(name);
        medal.setIcon(icon);
        medal.setDescription(description);
        if (body.get("status") != null) {
            medal.setStatus("0".equals(body.get("status").toString()) ? 0 : 1);
        }
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            medal.setSys001(now);
            medal.setSys005(1);
            medal.setSys006("biz");
            medalDao.insert(medal);
        } else {
            medal.setSys002(now);
            medalDao.update(medal);
        }
        return R.ok(id == null ? "奖章已新增" : "奖章已保存", null);
    }
}
