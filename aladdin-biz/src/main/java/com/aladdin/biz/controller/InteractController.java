package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizInteractionDao;
import com.aladdin.biz.entity.BizInteraction;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 互动控制器(点赞/收藏)
 *
 * @author cles
 * @date 2026/09/16
 */
@RestController
@RequestMapping({"/interact", "/biz/interact"})
public class InteractController {

    private static final Set<String> TARGET_TYPES = Set.of("note");
    private static final Set<String> ACTIONS = Set.of("like", "fav");

    @Autowired
    private BizInteractionDao interactionDao;

    /**
     * 切换点赞/收藏状态(已点则取消，未点则新增)
     * body: {targetType: 'thought'|'note', targetId, action: 'like'|'fav'}
     */
    @PostMapping("/toggle")
    public R<Map<String, Object>> toggle(@RequestBody Map<String, Object> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        String targetType = String.valueOf(body.getOrDefault("targetType", ""));
        String action = String.valueOf(body.getOrDefault("action", ""));
        if (!TARGET_TYPES.contains(targetType) || !ACTIONS.contains(action)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "参数错误");
        }
        Long targetId;
        try {
            targetId = Long.valueOf(body.get("targetId").toString());
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "参数错误");
        }
        BizInteraction existing = interactionDao.selectOne(userId, targetType, targetId, action);
        boolean active;
        if (existing != null) {
            interactionDao.deleteById(existing.getId());
            active = false;
        } else {
            BizInteraction item = new BizInteraction();
            item.setUserId(userId);
            item.setTargetType(targetType);
            item.setTargetId(targetId);
            item.setAction(action);
            item.setSys001(LocalDateTime.now());
            item.setSys005(1);
            item.setSys006("biz");
            interactionDao.insert(item);
            active = true;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("active", active);
        result.put("count", interactionDao.countByTarget(targetType, targetId, action));
        return R.ok(result);
    }
}
