package com.aladdin.common.security.service;

import com.aladdin.common.core.context.UserContextHolder;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 登录服务
 *
 * @author cles
 * @date 2026/05/06
 */
@Component
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<SecurityUserDetailsService> userDetailsService;
    private final SecurityProperties securityProperties;

    public LoginService(TokenService tokenService,
                        PasswordEncoder passwordEncoder,
                        ObjectProvider<SecurityUserDetailsService> userDetailsService,
                        SecurityProperties securityProperties) {
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.securityProperties = securityProperties;
    }

    /**
     * 用户名密码登录
     */
    public Map<String, Object> login(String username, String password) {
        UserDetails userDetails;
        try {
            // UserDetailsService 实现在 system 模块；biz 独立运行无此 bean，登录调用将统一走"用户名或密码错误"
            userDetails = userDetailsService.getObject().loadUserByUsername(username);
        } catch (Exception e) {
            log.warn("用户不存在: {}", username);
            throw new BusinessException(GlobalErrorCode.LOGIN_PASSWORD_ERROR, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            log.warn("密码错误: {}", username);
            throw new BusinessException(GlobalErrorCode.LOGIN_PASSWORD_ERROR, "用户名或密码错误");
        }

        if (!userDetails.isEnabled()) {
            throw new BusinessException(GlobalErrorCode.ACCOUNT_DISABLED, "用户已被禁用");
        }

        LoginUserDetails loginUser = (LoginUserDetails) userDetails;
        Map<String, Object> claims = new HashMap<>();
        Set<String> permissions = new java.util.HashSet<>();
        loginUser.getAuthorities().forEach(a -> permissions.add(a.getAuthority()));
        claims.put("permissions", permissions);

        String token = tokenService.createToken(loginUser.getUserId(), loginUser.getUsername(), claims);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", loginUser.getUserId());
        result.put("username", loginUser.getUsername());
        return result;
    }

    /**
     * 退出登录
     */
    public void logout() {
        Long userId = getCurrentUserId();
        if (userId != null) {
            tokenService.removeToken(userId);
        }
        SecurityContextHolder.clearContext();
        UserContextHolder.clear();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserDetails) {
            return ((LoginUserDetails) principal).getUserId();
        }
        return null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserDetails) {
            return ((LoginUserDetails) principal).getUsername();
        }
        return authentication.getName();
    }
}
