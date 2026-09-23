package com.aladdin.system.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.log.entity.OperationLog;
import com.alibaba.fastjson2.JSON;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志控制器
 * 从Redis List中读取操作日志数据(倒序，最新在前)
 *
 * @author cles
 * @date 2026/07/03
 */
@RestController
@RequestMapping({"/operLog", "/system/operLog"})
public class SysOperLogController {

    private static final String OPERATION_LOG_KEY = "ZM:log:operation";

    private final StringRedisTemplate redisTemplate;

    public SysOperLogController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 分页查询操作日志
     *
     * @param pageQuery 分页参数
     * @param userName  用户名(模糊匹配，可为空)
     * @param code      状态码(可为空，20000成功/50000失败)
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:operlog:list')")
    public R<PageResult<OperationLog>> list(PageQuery pageQuery,
                                            @RequestParam(required = false) String userName,
                                            @RequestParam(required = false) Integer code) {
        Long total = redisTemplate.opsForList().size(OPERATION_LOG_KEY);
        if (total == null || total == 0) {
            return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), 0L, Collections.emptyList()));
        }

        // 读取全量数据后过滤(fastjson2解析)
        // Redis List按写入时间顺序存储，倒序读取使最新日志在前
        List<String> rawList = redisTemplate.opsForList().range(OPERATION_LOG_KEY, 0, -1);
        if (rawList == null) {
            rawList = Collections.emptyList();
        }
        Collections.reverse(rawList);

        List<OperationLog> all = rawList.stream()
                .map(s -> {
                    try {
                        return JSON.parseObject(s, OperationLog.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(log -> log != null)
                .filter(log -> userName == null || userName.isEmpty()
                        || (log.getUserName() != null && log.getUserName().contains(userName)))
                .filter(log -> code == null || (log.getCode() != null && log.getCode().equals(code)))
                .collect(Collectors.toList());

        long filteredTotal = all.size();
        int offset = (pageQuery.getPage() - 1) * pageQuery.getLimit();
        List<OperationLog> page;
        if (offset >= filteredTotal) {
            page = Collections.emptyList();
        } else {
            int to = (int) Math.min(offset + pageQuery.getLimit(), filteredTotal);
            page = all.subList(offset, to);
        }
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), filteredTotal, page));
    }
}
