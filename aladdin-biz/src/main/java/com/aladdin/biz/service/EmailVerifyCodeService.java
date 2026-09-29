package com.aladdin.biz.service;

import com.aladdin.biz.dao.BizEmailSendDao;
import com.aladdin.biz.entity.BizEmailSendLog;
import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.utils.IpUtil;
import com.aladdin.common.core.utils.ServletUtil;
import com.aladdin.common.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务：生成验证码 → Redis暂存(带有效期) → SMTP发送 → 发送记录落库(biz_email_send_log)
 * SMTP配置见 aladdin-boot application.yml 的 biz.email.*（QQ邮箱需在网页版设置开启SMTP服务并填授权码）
 *
 * @author cles
 * @date 2026/09/28
 */
@Slf4j
@Service
public class EmailVerifyCodeService {

    @Autowired
    private BizEmailSendDao emailSendDao;

    @Autowired
    private RedisService redisService;

    /** SMTP服务器地址 */
    @Value("${biz.email.host:smtp.qq.com}")
    private String smtpHost;

    /** SMTP端口：QQ/163=465(SSL)；Outlook=587(STARTTLS) */
    @Value("${biz.email.port:465}")
    private int smtpPort;

    /** true=SSL(465，QQ/163)；false=STARTTLS(587，Outlook) */
    @Value("${biz.email.ssl:true}")
    private boolean smtpSsl;

    /** 发件邮箱 */
    @Value("${biz.email.username:}")
    private String fromEmail;

    /** SMTP授权码(不是邮箱登录密码) */
    @Value("${biz.email.password:}")
    private String authCode;

    /** 验证码有效期(分钟) */
    @Value("${biz.email.expire-minutes:5}")
    private int expireMinutes;

    /** 同邮箱重发间隔(秒) */
    @Value("${biz.email.resend-seconds:60}")
    private int resendSeconds;

    /**
     * 发送验证码：生成 → 暂存Redis → SMTP发送；无论成功失败均写一条 biz_email_send_log
     * 发送成功后验证码才缓存进Redis，并启动重发频率限制
     *
     * @param email 收件邮箱
     * @param scene 用途场景(register注册/bind绑定/reset重置密码等)
     */
    public void sendCode(String email, String scene) {
        if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new BusinessException("邮箱格式不正确");
        }
        email = email.trim().toLowerCase();
        // 重发频率限制：同邮箱 resendSeconds 秒内只允许一次
        String limitKey = RedisKeyConstant.EMAIL_CODE_LIMIT + email;
        if (Boolean.TRUE.equals(redisService.hasKey(limitKey))) {
            throw new BusinessException("发送过于频繁，请" + resendSeconds + "秒后再试");
        }
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();

        BizEmailSendLog record = new BizEmailSendLog();
        record.setEmail(email);
        record.setScene(scene == null ? "" : scene);
        record.setCode(code);
        record.setSendTime(now);
        record.setExpireTime(now.plusMinutes(expireMinutes));
        record.setClientIp(currentIp());
        try {
            doSend(email, code);
            record.setStatus(1);
            record.setFailReason("");
        } catch (Exception e) {
            log.warn("验证码邮件发送失败[{}]: {}", email, e.getMessage());
            record.setStatus(0);
            String reason = e.getMessage() == null ? "unknown" : e.getMessage();
            record.setFailReason(reason.length() > 250 ? reason.substring(0, 250) : reason);
        }
        record.setSys001(now);
        record.setSys005(1);
        record.setSys006("biz");
        emailSendDao.insert(record);

        if (record.getStatus() != null && record.getStatus() == 1) {
            // 验证码按 scene 隔离：注册/找回密码等场景互不可用
            // 读写必须走 string 模板(与 verify 的 getString 配对，redisTemplate 的 JSON 序列化会导致比对失败)
            redisService.setString(RedisKeyConstant.EMAIL_CODE + sceneKey(scene) + email, code, expireMinutes, TimeUnit.MINUTES);
            redisService.set(limitKey, "1", resendSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 校验验证码(校验通过即从Redis删除失效)
     *
     * @param email 收件邮箱
     * @param code  用户输入的验证码
     * @param scene 用途场景，须与发送时一致
     */
    public boolean verify(String email, String code, String scene) {
        if (email == null || code == null) {
            return false;
        }
        String key = RedisKeyConstant.EMAIL_CODE + sceneKey(scene) + email.trim().toLowerCase();
        String cached = redisService.getString(key);
        if (cached == null || !cached.equals(code.trim())) {
            return false;
        }
        redisService.deleteString(key);
        return true;
    }

    /** 场景键：空/默认场景归入register，避免空场景跨用 */
    private String sceneKey(String scene) {
        return (scene == null || scene.trim().isEmpty() ? "register" : scene.trim().toLowerCase()) + ":";
    }

    /** SMTP发送验证码邮件 */
    private void doSend(String email, String code) throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpHost);
        sender.setPort(smtpPort);
        sender.setUsername(fromEmail);
        sender.setPassword(authCode);
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (smtpSsl) {
            // QQ/163：465 端口直连 SSL
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            // Outlook/Hotmail：587 端口 STARTTLS 明文升级加密
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(email);
        helper.setSubject("【传信纸船】邮箱验证码");
        helper.setText(buildHtmlContent(code), true);
        sender.send(message);
    }

    /** 验证码邮件正文 */
    private String buildHtmlContent(String code) {
        return "<div style=\"max-width:420px;margin:0 auto;padding:24px;"
                + "font-family:'Microsoft YaHei',sans-serif;background:#faf6ec;"
                + "border:1px solid #e3d2ac;border-radius:12px;text-align:center;\">"
                + "<h2 style=\"color:#7a5b1f;margin:0 0 12px;\">传信纸船 · 邮箱验证码</h2>"
                + "<p style=\"color:#666;font-size:13px;\">您正在进行邮箱验证，本次验证码为：</p>"
                + "<div style=\"font-size:32px;font-weight:700;letter-spacing:8px;"
                + "color:#b98a3e;margin:16px 0;\">" + code + "</div>"
                + "<p style=\"color:#999;font-size:12px;\">验证码 " + expireMinutes
                + " 分钟内有效，请勿泄露给他人。若非本人操作，请忽略本邮件。</p>"
                + "</div>";
    }

    /** 当前请求IP(非Web上下文返回空串) */
    private String currentIp() {
        HttpServletRequest request = ServletUtil.getRequest();
        return request == null ? "" : IpUtil.getIpAddr(request);
    }
}
