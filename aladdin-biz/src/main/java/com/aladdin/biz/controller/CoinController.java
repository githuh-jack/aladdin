package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizCoinDao;
import com.aladdin.biz.dao.BizCoinLogDao;
import com.aladdin.biz.dao.BizOrderDao;
import com.aladdin.biz.entity.BizCoin;
import com.aladdin.biz.entity.BizCoinLog;
import com.aladdin.biz.entity.BizOrder;
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
 * 铜钱控制器
 * 用户: 查询余额、流水、订单
 * 管理员: 查询所有用户流水、订单，调整余额
 *
 * @author cles
 * @date 2026/09/15
 */
@RestController
@RequestMapping({"/coin", "/biz/coin"})
public class CoinController {

    @Autowired
    private BizCoinDao coinDao;

    @Autowired
    private BizCoinLogDao coinLogDao;

    @Autowired
    private BizOrderDao orderDao;

    /** 我的铜钱账户 */
    @GetMapping("/balance")
    public R<Map<String, Object>> balance() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizCoin coin = coinDao.selectByUserId(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("balance", coin == null ? 0 : coin.getBalance());
        return R.ok(data);
    }

    /** 我的铜钱流水 */
    @GetMapping("/logs")
    public R<PageResult<BizCoinLog>> logs(PageQuery pageQuery) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        List<BizCoinLog> list = coinLogDao.selectMyLogs(userId, pageQuery.getOffset(), pageQuery.getLimit());
        long total = coinLogDao.countMyLogs(userId);
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, list));
    }

    /** 我的订单 */
    @GetMapping("/orders")
    public R<List<BizOrder>> orders(PageQuery pageQuery) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(orderDao.selectListWithUser(userId, null, null,
                pageQuery.getOffset(), pageQuery.getLimit()));
    }

    // ===== 管理员接口 =====

    @GetMapping("/admin/logs")
    @PreAuthorize("hasAuthority('biz:coin:list') or hasRole('ROLE_admin')")
    public R<List<BizCoinLog>> adminLogs(PageQuery pageQuery,
                                          @RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) String logType) {
        return R.ok(coinLogDao.selectAllLogsWithUser(userId, logType,
                pageQuery.getOffset(), pageQuery.getLimit()));
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasAuthority('biz:order:list') or hasRole('ROLE_admin')")
    public R<List<BizOrder>> adminOrders(PageQuery pageQuery,
                                         @RequestParam(required = false) Long userId,
                                         @RequestParam(required = false) String itemType,
                                         @RequestParam(required = false) Integer status) {
        return R.ok(orderDao.selectListWithUser(userId, itemType, status,
                pageQuery.getOffset(), pageQuery.getLimit()));
    }

    /** 管理员调整用户铜钱 */
    @PostMapping("/admin/adjust")
    @PreAuthorize("hasAuthority('biz:coin:adjust') or hasRole('ROLE_admin')")
    public R<Void> adminAdjust(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Integer delta = Integer.valueOf(body.get("delta").toString());
        String remark = (String) body.getOrDefault("remark", "管理员调整");
        int affected = coinDao.adjustBalance(userId, delta);
        if (affected == 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "调整失败：账户不存在或调整后余额为负");
        }
        BizCoin coin = coinDao.selectByUserId(userId);
        BizCoinLog log = new BizCoinLog();
        log.setUserId(userId);
        log.setChangeAmount(delta);
        log.setBalanceAfter(coin.getBalance());
        log.setLogType("admin_adjust");
        log.setRemark(remark);
        log.setSys001(LocalDateTime.now());
        log.setSys005(1);
        log.setSys006("biz");
        coinLogDao.insert(log);
        return R.ok();
    }
}
