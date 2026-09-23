package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizFriendDao;
import com.aladdin.biz.dao.BizLetterDao;
import com.aladdin.biz.dao.BizUserStampDao;
import com.aladdin.biz.entity.BizLetter;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 信件控制器
 * 用户：写信/发送/查看发件箱/查看收件箱/查看详情/标记已读
 * 管理员：查看所有信件
 *
 * @author cles
 * @date 2026/09/15
 */
@RestController
@RequestMapping({"/letter", "/biz/letter"})
public class LetterController {

    @Autowired
    private BizLetterDao letterDao;

    @Autowired
    private BizFriendDao friendDao;

    @Autowired
    private BizUserStampDao userStampDao;

    @Autowired
    private com.aladdin.biz.service.RewardService rewardService;

    /**
     * 写信(草稿)或直接发送
     * body: {receiverId?, receiverAddress?, title, content, stampId?, envelopeId?, action: 'draft'|'send'}
     * 草稿：收件人/收件地址均可为空；发送：收件人与收件地址至少填一个
     */
    @PostMapping("/write")
    public R<BizLetter> write(@RequestBody Map<String, Object> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizLetter letter = new BizLetter();
        letter.setSenderId(userId);
        Object receiverIdObj = body.get("receiverId");
        if (receiverIdObj != null && !"".equals(receiverIdObj)) {
            letter.setReceiverId(Long.valueOf(receiverIdObj.toString()));
        }
        Object receiverAddrObj = body.get("receiverAddress");
        if (receiverAddrObj != null) {
            letter.setReceiverAddress(((String) receiverAddrObj).trim());
        }
        letter.setTitle((String) body.getOrDefault("title", ""));
        letter.setContent((String) body.getOrDefault("content", ""));
        Object stampId = body.get("stampId");
        if (stampId != null && !"".equals(stampId)) {
            letter.setStampId(Long.valueOf(stampId.toString()));
        }
        Object envId = body.get("envelopeId");
        if (envId != null && !"".equals(envId)) {
            letter.setEnvelopeId(Long.valueOf(envId.toString()));
        }
        String action = (String) body.getOrDefault("action", "draft");
        if ("send".equals(action)) {
            // 随机模式：由系统随机挑选一位收信人
            boolean random = Boolean.parseBoolean(String.valueOf(body.getOrDefault("random", "false")));
            if (random && letter.getReceiverId() == null) {
                Long randomReceiver = letterDao.selectRandomReceiver(userId);
                if (randomReceiver == null) {
                    throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "暂无可随机寄送的用户");
                }
                letter.setReceiverId(randomReceiver);
            }
            // 发送时收件人与收件地址至少一个
            boolean hasReceiver = letter.getReceiverId() != null;
            boolean hasAddress = letter.getReceiverAddress() != null && !letter.getReceiverAddress().isEmpty();
            if (!hasReceiver && !hasAddress) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "发送前请填写收件人或收件地址");
            }
            // 收件人为真实用户时，校验对方是否已将我拉黑
            if (hasReceiver && friendDao.selectBlackRelation(letter.getReceiverId(), userId) != null) {
                throw new BusinessException(GlobalErrorCode.FORBIDDEN, "对方已将你拉黑，无法寄出");
            }
            // 寄出时消耗一枚可用邮票(可用-1 已使用+1)
            if (letter.getStampId() != null
                    && userStampDao.consumeStamp(userId, letter.getStampId()) == 0) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该邮票可用数量不足");
            }
            letter.setStatus(1);
            letter.setSendTime(LocalDateTime.now());
        } else {
            letter.setStatus(0);
        }
        letter.setSys001(LocalDateTime.now());
        letter.setSys005(1);
        letter.setSys006("biz");
        letterDao.insert(letter);
        // 寄出后：当日第一封信奖励 + 文竹生长(仅寄出的信)
        if (letter.getStatus() != null && letter.getStatus() == 1) {
            rewardService.onLetterSent(userId, letter.getId(), letter.getContent());
        }
        return R.ok(letter);
    }

    /** 发件箱 */
    @GetMapping("/sent")
    public R<PageResult<BizLetter>> sent(PageQuery pageQuery,
                                         @RequestParam(required = false) Integer status) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        List<BizLetter> list = letterDao.selectSentList(userId, status,
                pageQuery.getOffset(), pageQuery.getLimit());
        // 简化：不查总数，前端按 list 长度处理；MVP 足够
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), list.size(), list));
    }

    /** 收件箱 */
    @GetMapping("/inbox")
    public R<PageResult<BizLetter>> inbox(PageQuery pageQuery,
                                          @RequestParam(required = false) Integer status) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        List<BizLetter> list = letterDao.selectInboxList(userId, status,
                pageQuery.getOffset(), pageQuery.getLimit());
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), list.size(), list));
    }

    /**
     * 信件广场：最近写信人(仅头像与昵称，不含信件内容，游客可浏览)
     */
    @GetMapping("/public/writers")
    public R<List<Map<String, Object>>> publicWriters() {
        return R.ok(letterDao.selectRecentWriters(20));
    }

    /** 详情(若为收件人且未读，标记已读) */
    @GetMapping("/detail/{id}")
    public R<BizLetter> detail(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizLetter letter = letterDao.selectLetterDetail(id);
        if (letter == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        // 权限：发件人/收件人可见(地址信件仅发件人可见)；admin 角色由 AdminAwareAccessDecisionManager 放行
        boolean isSender = userId.equals(letter.getSenderId());
        boolean isReceiver = userId.equals(letter.getReceiverId());
        if (!isSender && !isReceiver) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        // 收件人查看时标记已读
        if (isReceiver && Integer.valueOf(1).equals(letter.getStatus())) {
            letterDao.updateStatusAndReadTime(id, 2, LocalDateTime.now());
            letter.setStatus(2);
            letter.setReadTime(LocalDateTime.now());
        }
        return R.ok(letter);
    }

    /** 删除(逻辑删除:置status=3) */
    @PostMapping("/remove/{id}")
    public R<Void> remove(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizLetter letter = letterDao.selectOneById(id);
        if (letter == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (!userId.equals(letter.getSenderId()) && !userId.equals(letter.getReceiverId())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        letter.setStatus(3);
        letterDao.update(letter);
        return R.ok();
    }

    // ===== 管理员接口 =====

    /** 管理员: 查询所有信件 */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:letter:list') or hasRole('ROLE_admin')")
    public R<PageResult<BizLetter>> adminList(PageQuery pageQuery,
                                               @RequestParam(required = false) Long senderId,
                                               @RequestParam(required = false) Long receiverId) {
        List<BizLetter> list = letterDao.selectAdminList(senderId, receiverId,
                pageQuery.getOffset(), pageQuery.getLimit());
        long total = letterDao.countAdminList(senderId, receiverId);
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, list));
    }
}
