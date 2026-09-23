package com.aladdin.common.security.filter;

import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.core.utils.IpUtil;
import com.aladdin.common.security.config.SecurityProperties;
import com.aladdin.common.redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * IP黑白名单过滤器
 * <p>
 * mode=off: 关闭黑白名单
 * mode=blacklist: 黑名单模式，黑名单中的IP无法访问
 * mode=whitelist: 白名单模式，仅白名单中的IP可以访问
 * mode=both: 黑白名单同时生效，黑名单优先
 *
 * @author cles
 * @date 2026/06/12
 */
public class IpListFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IpListFilter.class);

    private final RedisService redisService;
    private final SecurityProperties securityProperties;

    public IpListFilter(RedisService redisService, SecurityProperties securityProperties) {
        this.redisService = redisService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String mode = securityProperties.getIpList().getMode();
        if ("off".equalsIgnoreCase(mode)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = IpUtil.getIpAddr(request);

        // 黑名单检查（blacklist和both模式都检查）
        if ("blacklist".equalsIgnoreCase(mode) || "both".equalsIgnoreCase(mode)) {
            Set<Object> blacklist = redisService.sMembers(RedisKeyConstant.IP_BLACKLIST);
            if (blacklist != null && blacklist.contains(clientIp)) {
                log.warn("IP[{}]在黑名单中，拒绝访问", clientIp);
                writeForbiddenResponse(response, GlobalErrorCode.IP_BLACKLISTED);
                return;
            }
        }

        // 白名单检查（whitelist和both模式都检查）
        if ("whitelist".equalsIgnoreCase(mode) || "both".equalsIgnoreCase(mode)) {
            Set<Object> whitelist = redisService.sMembers(RedisKeyConstant.IP_WHITELIST);
            if (whitelist == null || whitelist.isEmpty() || !whitelist.contains(clientIp)) {
                log.warn("IP[{}]不在白名单中，拒绝访问", clientIp);
                writeForbiddenResponse(response, GlobalErrorCode.IP_NOT_IN_WHITELIST);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeForbiddenResponse(HttpServletResponse response, GlobalErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        Map<String, Object> result = new HashMap<>();
        result.put("code", errorCode.getCode());
        result.put("msg", errorCode.getMsg());
        result.put("data", null);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}
