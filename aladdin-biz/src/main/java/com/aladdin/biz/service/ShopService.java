package com.aladdin.biz.service;

import com.aladdin.biz.dao.*;
import com.aladdin.biz.entity.*;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 商店服务：购买邮票/信封，事务性扣减铜钱、增加持有、记录订单和流水
 *
 * @author cles
 * @date 2026/09/15
 */
@Slf4j
@Service
public class ShopService {

    @Autowired private BizStampDao stampDao;
    @Autowired private BizEnvelopeDao envelopeDao;
    @Autowired private BizUserStampDao userStampDao;
    @Autowired private BizUserEnvelopeDao userEnvelopeDao;
    @Autowired private BizCoinDao coinDao;
    @Autowired private BizCoinLogDao coinLogDao;
    @Autowired private BizOrderDao orderDao;

    /**
     * 购买邮票/信封
     * @param itemType "stamp" or "envelope"
     * @param itemId   商品ID
     * @param quantity 购买数量
     */
    @Transactional(rollbackFor = Exception.class)
    public BizOrder buy(Long userId, String itemType, Long itemId, int quantity) {
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        if (quantity <= 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "购买数量必须大于0");
        }
        if (!"stamp".equals(itemType) && !"envelope".equals(itemType)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "商品类型无效");
        }

        String itemName;
        int unitPrice;
        int stock;
        if ("stamp".equals(itemType)) {
            BizStamp stamp = stampDao.selectOneById(itemId);
            if (stamp == null || stamp.getStatus() == null || stamp.getStatus() != 1) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "邮票不存在或已下架");
            }
            itemName = stamp.getName();
            unitPrice = stamp.getPrice();
            stock = stamp.getStock() == null ? 0 : stamp.getStock();
            if (stock > 0 && stock < quantity) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "库存不足");
            }
        } else {
            BizEnvelope envelope = envelopeDao.selectOneById(itemId);
            if (envelope == null || envelope.getStatus() == null || envelope.getStatus() != 1) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "信封不存在或已下架");
            }
            itemName = envelope.getName();
            unitPrice = envelope.getPrice();
            stock = envelope.getStock() == null ? 0 : envelope.getStock();
            if (stock > 0 && stock < quantity) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "库存不足");
            }
        }

        int totalPrice = unitPrice * quantity;

        // 1. 扣减铜钱余额(原子操作，DB层面防止并发超扣)
        int affected = coinDao.adjustBalance(userId, -totalPrice);
        if (affected == 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "铜钱不足或账户不存在");
        }
        int balanceAfter = coinDao.selectByUserId(userId).getBalance();

        // 2. 记录订单
        BizOrder order = new BizOrder();
        order.setUserId(userId);
        order.setItemType(itemType);
        order.setItemId(itemId);
        order.setItemName(itemName);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(totalPrice);
        order.setStatus(1);
        order.setSys001(LocalDateTime.now());
        order.setSys005(1);
        order.setSys006("biz");
        orderDao.insert(order);

        // 3. 记录铜钱流水
        BizCoinLog coinLog = new BizCoinLog();
        coinLog.setUserId(userId);
        coinLog.setChangeAmount(-totalPrice);
        coinLog.setBalanceAfter(balanceAfter);
        coinLog.setLogType("shop");
        coinLog.setRefId(order.getId());
        coinLog.setRemark("购买" + itemName + " x " + quantity);
        coinLog.setSys001(LocalDateTime.now());
        coinLog.setSys005(1);
        coinLog.setSys006("biz");
        coinLogDao.insert(coinLog);

        // 4. 增加用户持有量
        if ("stamp".equals(itemType)) {
            BizUserStamp existing = userStampDao.selectByUserAndStamp(userId, itemId);
            if (existing != null) {
                userStampDao.addCount(userId, itemId, quantity);
            } else {
                BizUserStamp us = new BizUserStamp();
                us.setUserId(userId);
                us.setStampId(itemId);
                us.setCount(quantity);
                us.setSys001(LocalDateTime.now());
                us.setSys005(1);
                us.setSys006("biz");
                userStampDao.insert(us);
            }
        } else {
            BizUserEnvelope existing = userEnvelopeDao.selectByUserAndEnvelope(userId, itemId);
            if (existing != null) {
                userEnvelopeDao.addCount(userId, itemId, quantity);
            } else {
                BizUserEnvelope ue = new BizUserEnvelope();
                ue.setUserId(userId);
                ue.setEnvelopeId(itemId);
                ue.setCount(quantity);
                ue.setSys001(LocalDateTime.now());
                ue.setSys005(1);
                ue.setSys006("biz");
                userEnvelopeDao.insert(ue);
            }
        }

        // 5. 扣减库存（-1表示不限）
        if (stock > 0) {
            if ("stamp".equals(itemType)) {
                BizStamp stampToUpdate = stampDao.selectOneById(itemId);
                stampToUpdate.setStock(stock - quantity);
                stampDao.update(stampToUpdate);
            } else {
                BizEnvelope envToUpdate = envelopeDao.selectOneById(itemId);
                envToUpdate.setStock(stock - quantity);
                envelopeDao.update(envToUpdate);
            }
        }

        log.info("用户[{}]购买 {} x{} 成功，订单ID={}", userId, itemName, quantity, order.getId());
        return order;
    }
}
