package com.aladdin.biz.service;

import com.aladdin.biz.dao.BizRedeemCodeDao;
import com.aladdin.biz.dao.BizRedeemLogDao;
import com.aladdin.biz.dao.BizStampDao;
import com.aladdin.biz.dao.BizUserStampItemDao;
import com.aladdin.biz.entity.BizRedeemCode;
import com.aladdin.biz.entity.BizRedeemLog;
import com.aladdin.biz.entity.BizStamp;
import com.aladdin.biz.entity.BizUserStampItem;
import com.aladdin.common.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 兑换码服务：用户兑换铜钱/邮票(每人每码限一次，先插记录占位再原子累加次数)
 *
 * @author cles
 * @date 2026/09/29
 */
@Service
public class RedeemService {

    @Autowired
    private BizRedeemCodeDao redeemCodeDao;

    @Autowired
    private BizRedeemLogDao redeemLogDao;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private BizStampDao stampDao;

    @Autowired
    private BizUserStampItemDao userStampItemDao;

    /**
     * 兑换兑换码，返回 coinAmount(到账铜钱数) 与 stampName(获赠邮票名，可能为空)
     */
    @Transactional
    public Map<String, Object> redeem(Long userId, String code) {
        BizRedeemCode redeemCode = redeemCodeDao.selectByCode(code);
        if (redeemCode == null) {
            throw new BusinessException("兑换码不存在");
        }
        if (redeemCode.getStatus() == null || redeemCode.getStatus() != 1) {
            throw new BusinessException("兑换码已停用");
        }
        if (redeemLogDao.countByCodeAndUser(redeemCode.getId(), userId) > 0) {
            throw new BusinessException("您已兑换过该兑换码");
        }
        // 附赠邮票校验(配置缺失直接失败，避免扣次数后发不出邮票)
        BizStamp stamp = null;
        if (redeemCode.getStampId() != null) {
            stamp = stampDao.selectOneById(redeemCode.getStampId());
            if (stamp == null) {
                throw new BusinessException("该兑换码附赠的邮票不存在，请联系管理员");
            }
        }
        // 先插记录占位(uk_code_user 唯一键兜底并发重复兑换)
        BizRedeemLog log = new BizRedeemLog();
        log.setCodeId(redeemCode.getId());
        log.setCode(redeemCode.getCode());
        log.setUserId(userId);
        log.setCoinAmount(redeemCode.getCoinAmount());
        log.setStampId(stamp == null ? null : stamp.getId());
        log.setSys001(LocalDateTime.now());
        log.setSys005(1);
        log.setSys006("biz");
        try {
            redeemLogDao.insert(log);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("您已兑换过该兑换码");
        }
        // 原子累加已兑换次数，超限则整体回滚
        if (redeemCodeDao.incrementUsedCount(redeemCode.getId()) == 0) {
            throw new BusinessException("该兑换码兑换次数已用完");
        }
        rewardService.addCoins(userId, redeemCode.getCoinAmount(), "redeem", "兑换码兑换");
        if (stamp != null) {
            grantStamp(userId, stamp.getId());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("coinAmount", redeemCode.getCoinAmount());
        data.put("stampName", stamp == null ? null : stamp.getName());
        return data;
    }

    /** 赠送邮票入库(一条实例，编码回填，参照系统邮件附赠逻辑) */
    private void grantStamp(Long userId, Long stampId) {
        BizUserStampItem item = new BizUserStampItem();
        item.setUserId(userId);
        item.setStampId(stampId);
        item.setStatus(1);
        item.setSys001(LocalDateTime.now());
        item.setSys005(1);
        item.setSys006("biz");
        userStampItemDao.insert(item);
        userStampItemDao.fillStampCodes();
    }
}
