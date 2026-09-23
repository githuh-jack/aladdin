package com.aladdin.common.log;

import com.aladdin.common.log.entity.OperationLog;

public interface OperationLogService {

    void save(OperationLog operationLog);
}
