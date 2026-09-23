package com.aladdin.common.security.config;

import com.aladdin.common.security.service.LoginUserDetails;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.Set;

/**
 * 自定义访问决策管理器：
 * 包装原始的 AccessDecisionManager，admin 用户直接放行所有接口，
 * 其他用户走原始决策逻辑。
 *
 * @author cles
 */
public class AdminAwareAccessDecisionManager implements AccessDecisionManager {

    private final AccessDecisionManager delegate;

    public AdminAwareAccessDecisionManager(AccessDecisionManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void decide(Authentication authentication, Object object,
                       Collection<ConfigAttribute> configAttributes) throws AccessDeniedException {
        // admin 用户直接放行，跳过 @PreAuthorize 权限校验
        if (isAdmin(authentication)) {
            return;
        }
        delegate.decide(authentication, object, configAttributes);
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return delegate.supports(attribute);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return delegate.supports(clazz);
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserDetails) {
            LoginUserDetails userDetails = (LoginUserDetails) principal;
            // 用户名为 admin 直接放行
            if ("admin".equals(userDetails.getUsername())) {
                return true;
            }
            // 拥有 admin 角色也放行
            Set<String> roleKeys = userDetails.getRoleKeys();
            if (roleKeys != null && roleKeys.contains("admin")) {
                return true;
            }
        }
        return false;
    }
}
