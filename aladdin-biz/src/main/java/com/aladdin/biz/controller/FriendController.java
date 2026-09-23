package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizFriendDao;
import com.aladdin.biz.entity.BizFriend;
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
 * 好友控制器
 *
 * @author cles
 * @date 2026/09/15
 */
@RestController
@RequestMapping({"/friend", "/biz/friend"})
public class FriendController {

    @Autowired
    private BizFriendDao friendDao;

    /** 我的好友列表 */
    @GetMapping("/list")
    public R<List<BizFriend>> list() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(friendDao.selectMyFriends(userId));
    }

    /** 我收到的好友申请(待处理) */
    @GetMapping("/applies")
    public R<List<BizFriend>> applies() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(friendDao.selectPendingApplies(userId));
    }

    /** 申请添加好友 */
    @PostMapping("/apply")
    public R<Void> apply(@RequestBody Map<String, Object> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        Long friendId = Long.valueOf(body.get("friendId").toString());
        if (friendId.equals(userId)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "不能添加自己为好友");
        }
        // 检查是否已经是好友
        BizFriend existing = friendDao.selectFriendRelation(userId, friendId);
        if (existing != null) {
            throw new BusinessException(GlobalErrorCode.DATA_DUPLICATE, "已经是好友");
        }
        // 我拉黑了对方 / 对方拉黑了我，均不可申请
        if (friendDao.selectBlackRelation(userId, friendId) != null) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "你已将该用户拉黑，请先移出黑名单");
        }
        if (friendDao.selectBlackRelation(friendId, userId) != null) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "无法添加该用户");
        }
        BizFriend friend = new BizFriend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(0);
        friend.setApplyRemark((String) body.getOrDefault("remark", ""));
        friend.setSys001(LocalDateTime.now());
        friend.setSys005(1);
        friend.setSys006("biz");
        friendDao.insert(friend);
        return R.ok();
    }

    /** 通过/拒绝好友申请 */
    @PostMapping("/handle/{id}")
    public R<Void> handleApply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizFriend friend = friendDao.selectOneById(id);
        if (friend == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        // 只有被加方可以处理
        if (!friend.getFriendId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        Integer action = Integer.valueOf(body.get("action").toString());
        // 1通过 2拒绝
        friend.setStatus(action);
        if (action == 1) {
            friend.setAddTime(LocalDateTime.now());
        }
        friendDao.update(friend);
        // 通过后，反向也建立好友关系
        if (action == 1) {
            BizFriend reverse = new BizFriend();
            reverse.setUserId(userId);
            reverse.setFriendId(friend.getUserId());
            reverse.setStatus(1);
            reverse.setAddTime(LocalDateTime.now());
            reverse.setSys001(LocalDateTime.now());
            reverse.setSys005(1);
            reverse.setSys006("biz");
            friendDao.insert(reverse);
        }
        return R.ok();
    }

    /** 删除好友 */
    @PostMapping("/remove/{id}")
    public R<Void> remove(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizFriend friend = friendDao.selectOneById(id);
        if (friend == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (!friend.getUserId().equals(userId) && !friend.getFriendId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        friend.setStatus(3);
        friendDao.update(friend);
        return R.ok();
    }

    /** 我拉黑的用户列表 */
    @GetMapping("/blacklist")
    public R<List<BizFriend>> blacklist() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(friendDao.selectBlacklist(userId));
    }

    /** 拉黑用户(可拉黑好友或任意用户) */
    @PostMapping("/blacklist/{targetId}")
    public R<Void> blacklist(@PathVariable Long targetId) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        if (targetId.equals(userId)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "不能拉黑自己");
        }
        // 我方已有关系记录则置为拉黑，否则新建一条
        BizFriend mine = friendDao.selectMyRelationTo(userId, targetId);
        if (mine != null) {
            mine.setStatus(4);
            friendDao.update(mine);
        } else {
            BizFriend friend = new BizFriend();
            friend.setUserId(userId);
            friend.setFriendId(targetId);
            friend.setStatus(4);
            friend.setSys001(LocalDateTime.now());
            friend.setSys005(1);
            friend.setSys006("biz");
            friendDao.insert(friend);
        }
        // 对方的关系记录置为已删除，使其好友列表中不再显示我
        BizFriend reverse = friendDao.selectMyRelationTo(targetId, userId);
        if (reverse != null && reverse.getStatus() != null && reverse.getStatus() == 1) {
            reverse.setStatus(3);
            friendDao.update(reverse);
        }
        return R.ok();
    }

    /** 移出黑名单(移出后不再是好友) */
    @PostMapping("/unblacklist/{targetId}")
    public R<Void> unblacklist(@PathVariable Long targetId) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizFriend mine = friendDao.selectBlackRelation(userId, targetId);
        if (mine == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        mine.setStatus(3);
        friendDao.update(mine);
        return R.ok();
    }

    /** 管理员: 查询所有好友关系 */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:friend:list') or hasRole('ROLE_admin')")
    public R<List<BizFriend>> adminList() {
        return R.ok(friendDao.selectAllList());
    }
}
