package com.aladdin.system.service.impl;

import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.db.base.BaseServiceImpl;
import com.aladdin.common.redis.RedisService;
import com.aladdin.system.dao.SysMessageDao;
import com.aladdin.system.dao.SysMessageReceiverDao;
import com.aladdin.system.entity.SysMessage;
import com.aladdin.system.entity.SysMessageReceiver;
import com.aladdin.system.service.SysMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内信服务实现
 *
 * @author cles
 * @date 2026/06/12
 */
@Service
public class SysMessageServiceImpl extends BaseServiceImpl<SysMessageDao, SysMessage> implements SysMessageService {

    private final SysMessageReceiverDao messageReceiverDao;
    private final RedisService redisService;

    public SysMessageServiceImpl(SysMessageReceiverDao messageReceiverDao, RedisService redisService) {
        this.messageReceiverDao = messageReceiverDao;
        this.redisService = redisService;
    }

    @Override
    @Transactional
    public void sendSingle(Long senderId, Long receiverId, String title, String content, Integer msgType) {
        SysMessage message = new SysMessage();
        message.setTitle(title);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setSendType(1);
        message.setSenderId(senderId);
        save(message);

        SysMessageReceiver receiver = new SysMessageReceiver();
        receiver.setMessageId(message.getId());
        receiver.setReceiverId(receiverId);
        receiver.setReadStatus(0);
        receiver.setHandleStatus(0);
        messageReceiverDao.insert(receiver);

        // 更新未读数缓存
        incrementUnreadCount(receiverId);
    }

    @Override
    @Transactional
    public void sendBatch(Long senderId, List<Long> receiverIds, String title, String content, Integer msgType) {
        SysMessage message = new SysMessage();
        message.setTitle(title);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setSendType(2);
        message.setSenderId(senderId);
        save(message);

        for (Long receiverId : receiverIds) {
            SysMessageReceiver receiver = new SysMessageReceiver();
            receiver.setMessageId(message.getId());
            receiver.setReceiverId(receiverId);
            receiver.setReadStatus(0);
            receiver.setHandleStatus(0);
            messageReceiverDao.insert(receiver);
            incrementUnreadCount(receiverId);
        }
    }

    @Override
    public List<SysMessageReceiver> getUserMessages(Long userId) {
        return messageReceiverDao.selectByReceiverId(userId);
    }

    @Override
    public int getUnreadCount(Long userId) {
        // 先从缓存取
        String key = RedisKeyConstant.MSG_UNREAD_COUNT + userId;
        Object cached = redisService.get(key);
        if (cached != null) {
            try {
                return Integer.parseInt(cached.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        int count = messageReceiverDao.countUnread(userId);
        redisService.set(key, count);
        return count;
    }

    @Override
    public boolean markAsRead(Long messageId, Long userId) {
        int rows = messageReceiverDao.markAsRead(messageId, userId);
        if (rows > 0) {
            decrementUnreadCount(userId);
        }
        return rows > 0;
    }

    @Override
    public boolean markAllAsRead(Long userId) {
        int rows = messageReceiverDao.markAllAsRead(userId);
        if (rows > 0) {
            // 清除未读数缓存
            redisService.set(RedisKeyConstant.MSG_UNREAD_COUNT + userId, 0);
        }
        return rows > 0;
    }

    @Override
    public boolean handleTodo(Long messageId, Long userId, Integer handleStatus) {
        return messageReceiverDao.updateHandleStatus(messageId, userId, handleStatus) > 0;
    }

    private void incrementUnreadCount(Long userId) {
        String key = RedisKeyConstant.MSG_UNREAD_COUNT + userId;
        redisService.increment(key);
    }

    private void decrementUnreadCount(Long userId) {
        String key = RedisKeyConstant.MSG_UNREAD_COUNT + userId;
        Long val = redisService.decrement(key);
        if (val != null && val < 0) {
            redisService.set(key, 0);
        }
    }
}
