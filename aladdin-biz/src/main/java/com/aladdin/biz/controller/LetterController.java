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
import java.util.Collection;
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
        // 按唯一编号寄信：编号解析为收件用户，优先于 receiverId；
        // 编号不存在时信件照常寄出，送达后按"未找到指定的用户"退回寄件人
        boolean receiverNotFound = false;
        Object receiverNoObj = body.get("receiverNo");
        if (receiverNoObj != null && !"".equals(receiverNoObj)) {
            BizUser receiver = userDao.selectByUserNo(Long.valueOf(receiverNoObj.toString().trim()));
            if (receiver == null) {
                receiverNotFound = true;
                letter.setReceiverAddress("编号" + receiverNoObj.toString().trim());
            } else {
                letter.setReceiverId(receiver.getId());
            }
        }
        Object receiverAddrObj = body.get("receiverAddress");
        if (receiverAddrObj != null && !receiverNotFound) {
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
        // 可见性：1公开(可在信件广场展示) 0私密，默认私密
        Object isPublicObj = body.get("isPublic");
        boolean isPublic = isPublicObj != null &&
                ("1".equals(isPublicObj.toString()) || "true".equalsIgnoreCase(isPublicObj.toString()));
        letter.setIsPublic(isPublic ? 1 : 0);
        String action = (String) body.getOrDefault("action", "draft");
        if ("send".equals(action)) {
            // 禁闭校验：举报处罚期间不能寄信(禁闭从领取处罚通知邮件起算)
            BizUser senderUser = userDao.selectOneById(userId);
            if (senderUser != null && senderUser.getConfineDays() != null && senderUser.getConfineDays() > 0
                    && senderUser.getConfineStart() != null) {
                LocalDateTime confineEnd = senderUser.getConfineStart().plusDays(senderUser.getConfineDays());
                if (LocalDateTime.now().isBefore(confineEnd)) {
                    long remainDays = (java.time.Duration.between(LocalDateTime.now(), confineEnd).toHours() + 23) / 24;
                    throw new BusinessException(GlobalErrorCode.BAD_REQUEST,
                            "您正处于禁闭中，还剩" + remainDays + "天不能寄信");
                }
            }
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
            // 收件编号不存在：到达后立即按"未找到指定的用户"退回(退回时间=到达时间)
            if (receiverNotFound) {
                letter.setReturnReason("未找到指定的用户");
                letter.setReturnTime(letter.getArrivalTime());
            }
            // 多选好友群发：receiverIds 非空时逐人寄一封，每人消耗一枚邮票(及一张信纸)
            List<Long> multiReceiverIds = parseReceiverIds(body.get("receiverIds"));
            if (!multiReceiverIds.isEmpty()) {
                if (multiReceiverIds.size() > 20) {
                    throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "单次最多寄给20位好友");
                }
                letter.setSys001(LocalDateTime.now());
                letter.setSys005(1);
                letter.setSys006("biz");
                boolean first = true;
                for (Long rid : multiReceiverIds) {
                    if (rid.equals(userId)) {
                        throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "不能给自己寄信");
                    }
                    if (friendDao.selectBlackRelation(rid, userId) != null) {
                        throw new BusinessException(GlobalErrorCode.FORBIDDEN, "名单中有已将你拉黑的好友，无法寄出");
                    }
                    BizLetter target = first ? letter : copyLetter(letter);
                    target.setReceiverId(rid);
                    letterDao.insert(target);
                    // 每封信各自消耗一枚邮票(及信纸)，不足则整体回滚
                    consumeLetterAssets(userId, target);
                    rewardService.onLetterSent(userId, target.getId(), target.getContent());
                    first = false;
                }
                return R.ok(letter);
            }
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
            consumeLetterAssets(userId, letter);
            // 当日第一封信奖励 + 小星星+1(仅寄出的信)
            rewardService.onLetterSent(userId, letter.getId(), letter.getContent());
        }
        return R.ok(letter);
    }

    /** 解析群发收件人ID列表(去重保序) */
    private static List<Long> parseReceiverIds(Object obj) {
        List<Long> ids = new java.util.ArrayList<>();
        if (!(obj instanceof Collection)) {
            return ids;
        }
        for (Object o : (Collection<?>) obj) {
            if (o == null || "".equals(o.toString())) continue;
            ids.add(Long.valueOf(o.toString()));
        }
        return ids.stream().distinct().collect(java.util.stream.Collectors.toList());
    }

    /** 复制群发信件模板(收件人外字段一致) */
    private static BizLetter copyLetter(BizLetter src) {
        BizLetter c = new BizLetter();
        c.setSenderId(src.getSenderId());
        c.setReceiverAddress(src.getReceiverAddress());
        c.setTitle(src.getTitle());
        c.setContent(src.getContent());
        c.setStatus(src.getStatus());
        c.setStampId(src.getStampId());
        c.setEnvelopeId(src.getEnvelopeId());
        c.setIsPublic(src.getIsPublic());
        c.setSendTime(src.getSendTime());
        c.setArrivalTime(src.getArrivalTime());
        c.setReturnReason(src.getReturnReason());
        c.setReturnTime(src.getReturnTime());
        c.setSys001(src.getSys001());
        c.setSys005(src.getSys005());
        c.setSys006(src.getSys006());
        return c;
    }

    /** 寄出消耗：一枚邮票(必须)与一张信纸(若贴了)，不足抛异常回滚 */
    private void consumeLetterAssets(Long userId, BizLetter letter) {
        if (userStampItemDao.consumeAvailable(userId, letter.getStampId(), letter.getId()) == 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该邮票可用数量不足");
        }
        if (letter.getEnvelopeId() != null
                && userEnvelopeItemDao.consumeAvailable(userId, letter.getEnvelopeId(), letter.getId()) == 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该信封可用数量不足");
        }
    }

    /** 我与好友的来往信件(双向，仅好友关系可查) */
    @GetMapping("/history/{friendId}")
    public R<List<BizLetter>> history(@PathVariable Long friendId) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        if (friendDao.selectFriendRelation(userId, friendId) == null) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "仅好友之间可以查看来往信件");
        }
        return R.ok(letterDao.selectHistory(userId, friendId));
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

    /**
     * 信件广场：设置公开的信件，最新20条(游客可浏览，仅返回公开信件内容)
     */
    @GetMapping("/public/list")
    public R<List<Map<String, Object>>> publicLetters() {
        return R.ok(letterDao.selectPublicLetters(20));
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
        // 已退回的信件：收件人(含拒收人)不可再查看，信件正退回寄件人
        if (isReceiver && !isSender && Integer.valueOf(4).equals(letter.getStatus())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该信件已退回寄件人");
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

    /**
     * 拒收(仅收件人，已到达且未读的信件)
     * 信件按原邮票送达时长退回寄件人，return_time 到点后寄件人可见"已退回"
     */
    @PostMapping("/refuse/{id}")
    public R<Void> refuse(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizLetter letter = letterDao.selectOneById(id);
        if (letter == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (!userId.equals(letter.getReceiverId())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "只有收件人可以拒收");
        }
        if (!Integer.valueOf(1).equals(letter.getStatus())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "仅未读信件可以拒收");
        }
        if (letter.getArrivalTime() != null && letter.getArrivalTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "信件还在路上，送达后才能拒收");
        }
        BizLetter update = new BizLetter();
        update.setId(id);
        update.setStatus(4);
        update.setReturnReason("对方拒收");
        update.setReturnTime(LocalDateTime.now().plusDays(resolveDeliveryDays(letter)));
        letterDao.update(update);
        return R.ok("已拒收，信件正在退回寄件人", null);
    }

    /** 按信件邮票取送达天数(邮票缺失或非法时默认3天) */
    private int resolveDeliveryDays(BizLetter letter) {
        if (letter.getStampId() != null) {
            BizStamp stamp = stampDao.selectOneById(letter.getStampId());
            if (stamp != null && stamp.getDeliveryDays() != null && stamp.getDeliveryDays() > 0) {
                return stamp.getDeliveryDays();
            }
        }
        return 3;
    }

    // ===== 管理员接口 =====

    /** 管理员: 查询所有信件(支持笔名/编号关键字) */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:letter:list') or hasRole('ROLE_admin')")
    public R<PageResult<BizLetter>> adminList(PageQuery pageQuery,
                                               @RequestParam(required = false) Long senderId,
                                               @RequestParam(required = false) Long receiverId,
                                               @RequestParam(required = false) String keyword) {
        List<BizLetter> list = letterDao.selectAdminList(senderId, receiverId, keyword,
                pageQuery.getOffset(), pageQuery.getLimit());
        long total = letterDao.countAdminList(senderId, receiverId, keyword);
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, list));
    }

    /** 管理员: 标记/取消异常信件(异常信件不会出现在信件广场) */
    @PostMapping("/admin/markAbnormal/{id}")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> adminMarkAbnormal(@PathVariable Long id,
                                     @RequestParam Integer abnormal) {
        BizLetter letter = letterDao.selectOneById(id);
        if (letter == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        BizLetter update = new BizLetter();
        update.setId(id);
        update.setAbnormal(abnormal != null && abnormal == 1 ? 1 : 0);
        letterDao.update(update);
        return R.ok();
    }

    /**
     * 管理员: 检查不合格退回信件
     * reason: 未找到指定的用户/对方拒收/审核不合格(默认审核不合格)
     * 信件按邮票送达时长退回寄件人，并标记异常不上广场
     */
    @PostMapping("/admin/return/{id}")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> adminReturn(@PathVariable Long id,
                               @RequestParam(required = false, defaultValue = "审核不合格") String reason) {
        BizLetter letter = letterDao.selectOneById(id);
        if (letter == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (letter.getStatus() == null || letter.getStatus() < 1 || letter.getStatus() > 2) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "仅已寄出的信件可以退回");
        }
        BizLetter update = new BizLetter();
        update.setId(id);
        update.setStatus(4);
        update.setReturnReason(reason);
        update.setReturnTime(LocalDateTime.now().plusDays(resolveDeliveryDays(letter)));
        update.setAbnormal(1);
        letterDao.update(update);
        return R.ok("已退回，信件正在退回寄件人", null);
    }
}
