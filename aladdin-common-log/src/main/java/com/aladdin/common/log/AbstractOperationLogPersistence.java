package com.aladdin.common.log;

import com.aladdin.common.log.entity.OperationLog;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 操作日志持久化服务
 * <p>
 * 先写入内存缓冲，定时批量落库
 *
 * @author cles
 * @date 2026/05/06
 */
public abstract class AbstractOperationLogPersistence {

    private static final Logger log = LoggerFactory.getLogger(AbstractOperationLogPersistence.class);

    private static final int BATCH_SIZE = 100;
    private final List<OperationLog> buffer = new CopyOnWriteArrayList<>();

    public void addToBuffer(OperationLog operationLog) {
        buffer.add(operationLog);
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduledFlush() {
        if (!buffer.isEmpty()) {
            flush();
        }
    }

    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        List<OperationLog> toSave = new ArrayList<>(buffer);
        buffer.clear();
        try {
            batchSave(toSave);
            log.debug("操作日志批量持久化: {}条", toSave.size());
        } catch (Exception e) {
            log.error("操作日志持久化失败: {}", e.getMessage());
            buffer.addAll(toSave);
        }
    }

    /**
     * 批量保存，业务系统实现此方法
     */
    protected abstract void batchSave(List<OperationLog> logs);
}
