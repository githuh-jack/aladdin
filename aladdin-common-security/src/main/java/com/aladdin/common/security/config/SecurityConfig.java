package com.aladdin.common.security.config;

import com.aladdin.common.security.filter.IpListFilter;
import com.aladdin.common.security.filter.JwtAuthenticationFilter;
import com.aladdin.common.redis.RedisService;
import com.aladdin.common.security.service.SecurityUserDetailsService;
import com.aladdin.common.security.service.TokenService;
import com.aladdin.common.security.tenant.TenantContextFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Security配置
 *
 * @author cles
 * @date 2026/05/06
 */
@Configuration
@ConditionalOnProperty(prefix = "aladdin.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final TokenService tokenService;
    private final SecurityProperties securityProperties;
    private final SecurityUserDetailsService userDetailsService;
    private final RedisService redisService;

    public SecurityConfig(TokenService tokenService,
                          SecurityProperties securityProperties,
                          ObjectProvider<SecurityUserDetailsService> userDetailsServiceProvider,
                          RedisService redisService) {
        this.tokenService = tokenService;
        this.securityProperties = securityProperties;
        this.userDetailsService = userDetailsServiceProvider.getIfAvailable();
        this.redisService = redisService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (userDetailsService != null) {
            auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(securityProperties.getWhitelist()).permitAll()
                .antMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .antMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .headers().frameOptions().disable()
                .and()
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(ipListFilter(), JwtAuthenticationFilter.class)
                .addFilterAfter(tenantContextFilter(), JwtAuthenticationFilter.class);

        http.exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    Map<String, Object> result = new HashMap<>();
                    result.put("code", 401);
                    result.put("msg", "未认证，请先登录");
                    result.put("message", "未认证，请先登录");
                    result.put("error", "Unauthorized Exception");
                    result.put("data", null);
                    response.getWriter().write(new ObjectMapper().writeValueAsString(result));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    Map<String, Object> result = new HashMap<>();
                    result.put("code", 403);
                    result.put("msg", "无权限访问");
                    result.put("message", "无权限访问");
                    result.put("error", "Access Denied");
                    result.put("data", null);
                    response.getWriter().write(new ObjectMapper().writeValueAsString(result));
                });
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, securityProperties);
        if (userDetailsService != null) {
            filter.setUserDetailsService(userDetailsService);
        }
        return filter;
    }

    @Bean
    public IpListFilter ipListFilter() {
        return new IpListFilter(redisService, securityProperties);
    }

    @Bean
    public TenantContextFilter tenantContextFilter() {
        return new TenantContextFilter();
    }
}
