package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizCommentDao;
import com.aladdin.biz.entity.BizComment;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 评论控制器
 *
 * @author cles
 * @date 2026/09/16
 */
@RestController
@RequestMapping({"/comment", "/biz/comment"})
public class CommentController {

    private static final Set<String> TARGET_TYPES = Set.of("note");

    @Autowired
    private BizCommentDao commentDao;

    /** 评论列表 */
    @GetMapping("/list")
    public R<List<BizComment>> list(@RequestParam String targetType,
                                    @RequestParam Long targetId) {
        if (!TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "参数错误");
        }
        return R.ok(commentDao.selectByTarget(targetType, targetId));
    }

    /** 发表评论 */
    @PostMapping
    public R<Void> add(@RequestBody Map<String, Object> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        String targetType = String.valueOf(body.getOrDefault("targetType", ""));
        if (!TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "参数错误");
        }
        String content = String.valueOf(body.getOrDefault("content", "")).trim();
        if (content.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "评论内容不能为空");
        }
        Long targetId;
        try {
            targetId = Long.valueOf(body.get("targetId").toString());
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "参数错误");
        }
        BizComment comment = new BizComment();
        comment.setTargetType(targetType);
        comment.setTargetId(targetId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setSys001(LocalDateTime.now());
        comment.setSys005(1);
        comment.setSys006("biz");
        commentDao.insert(comment);
        return R.ok();
    }
}
