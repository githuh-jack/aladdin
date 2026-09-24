package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizFriendDao;
import com.aladdin.biz.dao.BizLetterDao;
import com.aladdin.biz.dao.BizStampDao;
import com.aladdin.biz.dao.BizUserDao;
import com.aladdin.biz.dao.BizUserEnvelopeItemDao;
import com.aladdin.biz.dao.BizUserStampItemDao;
import com.aladdin.biz.entity.BizLetter;
import com.aladdin.biz.entity.BizStamp;
import com.aladdin.biz.entity.BizUser;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
    private BizUserStampItemDao userStampItemDao;

    @Autowired
    private BizUserEnvelopeItemDao userEnvelopeItemDao;

    @Autowired
    private BizUserDao userDao;

    @Autowired
    private BizStampDao stampDao;

    @Autowired
    private com.aladdin.biz.service.RewardService rewardService;

    /**
     * 写信(草稿)或直接发送
     * body: {receiverId?, receiverNo?, receiverAddress?, title?, content, stampId?, envelopeId?, action: 'draft'|'send'}
     * 草稿：收件人/收件地址均可为空；发送：收件人与收件地址至少填一个，必须贴邮票(标题可不填)
     * receiverNo：按用户唯一编号寄信(6位)，优先于 receiverId
     */
    @PostMapping("/write")
    @Transactional(rollbackFor = Exception.class)
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
        // 按唯一编号寄信：编号解析为收件用户，优先于 receiverId
        Object receiverNoObj = body.get("receiverNo");
        if (receiverNoObj != null && !"".equals(receiverNoObj)) {
            BizUser receiver = userDao.selectByUserNo(Long.valueOf(receiverNoObj.toString().trim()));
            if (receiver == null) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "收件编号不存在，请核对后再寄");
            }
            letter.setReceiverId(receiver.getId());
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
            // 寄出必须贴邮票，并按邮票默认送达时间计算到达时间
            if (letter.getStampId() == null) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "请先贴上一枚邮票再寄出");
            }
            BizStamp stamp = stampDao.selectOneById(letter.getStampId());
            if (stamp == null) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "邮票不存在");
            }
            letter.setStatus(1);
            letter.setSendTime(LocalDateTime.now());
            int deliveryDays = stamp.getDeliveryDays() == null || stamp.getDeliveryDays() <= 0
                    ? 3 : stamp.getDeliveryDays();
            letter.setArrivalTime(letter.getSendTime().plusDays(deliveryDays));
        } else {
            letter.setStatus(0);
        }
        letter.setSys001(LocalDateTime.now());
        letter.setSys005(1);
        letter.setSys006("biz");
        letterDao.insert(letter);
        // 寄出后消耗票实例：每张邮票有使用状态，已使用的不能再次用于寄信；
        // 先落信件再消耗可回填 letter_id，消耗失败整体回滚
        if (letter.getStatus() != null && letter.getStatus() == 1) {
            if (userStampItemDao.consumeAvailable(userId, letter.getStampId(), letter.getId()) == 0) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该邮票可用数量不足");
            }
            if (letter.getEnvelopeId() != null
                    && userEnvelopeItemDao.consumeAvailable(userId, letter.getEnvelopeId(), letter.getId()) == 0) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该信封可用数量不足");
            }
            // 当日第一封信奖励 + 文竹生长(仅寄出的信)
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

    /**
     * 信箱统一列表 box=all|inbox|sent
     * all：收到的(仅已到达)+寄出的，未读置前；inbox/sent 同原收发件箱
     */
    @GetMapping("/list")
    public R<PageResult<BizLetter>> list(PageQuery pageQuery,
                                         @RequestParam(defaultValue = "all") String box) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        List<BizLetter> list;
        if ("inbox".equals(box)) {
            list = letterDao.selectInboxList(userId, null, pageQuery.getOffset(), pageQuery.getLimit());
        } else if ("sent".equals(box)) {
            list = letterDao.selectSentList(userId, null, pageQuery.getOffset(), pageQuery.getLimit());
        } else {
            list = letterDao.selectBoxList(userId, pageQuery.getOffset(), pageQuery.getLimit());
        }
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
        // 信件尚未到达：收件人不可提前查看(发件人可看自己在途的信)
        if (isReceiver && !isSender && letter.getArrivalTime() != null
                && letter.getArrivalTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "信件还在路上，尚未送达");
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
