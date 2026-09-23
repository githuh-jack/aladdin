package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysMessageReceiver;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;

import java.time.LocalDateTime;
import java.util.List;

import static com.aladdin.system.entity.table.SysMessageReceiverTableDef.SYS_MESSAGE_RECEIVER;

/**
 * 站内信接收记录DAO
 *
 * @author cles
 * @date 2026/06/12
 */
public interface SysMessageReceiverDao extends BaseDao<SysMessageReceiver> {

    /**
     * 查询用户站内信(主表 sys005=1 由 flex 自动追加；LEFT JOIN 用字符串表名，避免被连接表逻辑删除条件过滤)
     * 原 SQL 中 m.title/content 等额外列本就未映射进实体，故不再显式 select
     */
    default List<SysMessageReceiver> selectByReceiverId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .from(SYS_MESSAGE_RECEIVER)
                .leftJoin("sys_message")
                .on(SYS_MESSAGE_RECEIVER.MESSAGE_ID.eq(new QueryColumn("sys_message", "id")))
                .where(SYS_MESSAGE_RECEIVER.RECEIVER_ID.eq(userId))
                .orderBy(SYS_MESSAGE_RECEIVER.SYS001.desc()));
    }

    default int countUnread(Long userId) {
        long count = selectCountByQuery(QueryWrapper.create()
                .from(SYS_MESSAGE_RECEIVER)
                .where(SYS_MESSAGE_RECEIVER.RECEIVER_ID.eq(userId))
                .and(SYS_MESSAGE_RECEIVER.READ_STATUS.eq(0)));
        return (int) count;
    }

    default int markAsRead(Long messageId, Long userId) {
        SysMessageReceiver entity = new SysMessageReceiver();
        entity.setReadStatus(1);
        entity.setReadTime(LocalDateTime.now());
        return updateByQuery(entity, true, QueryWrapper.create()
                .where(SYS_MESSAGE_RECEIVER.MESSAGE_ID.eq(messageId))
                .and(SYS_MESSAGE_RECEIVER.RECEIVER_ID.eq(userId)));
    }

    default int markAllAsRead(Long userId) {
        SysMessageReceiver entity = new SysMessageReceiver();
        entity.setReadStatus(1);
        entity.setReadTime(LocalDateTime.now());
        return updateByQuery(entity, true, QueryWrapper.create()
                .where(SYS_MESSAGE_RECEIVER.RECEIVER_ID.eq(userId))
                .and(SYS_MESSAGE_RECEIVER.READ_STATUS.eq(0)));
    }

    default int updateHandleStatus(Long messageId, Long userId, Integer handleStatus) {
        SysMessageReceiver entity = new SysMessageReceiver();
        entity.setHandleStatus(handleStatus);
        return updateByQuery(entity, true, QueryWrapper.create()
                .where(SYS_MESSAGE_RECEIVER.MESSAGE_ID.eq(messageId))
                .and(SYS_MESSAGE_RECEIVER.RECEIVER_ID.eq(userId)));
    }
}
