package com.aladdin.biz.mail;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.util.Properties;

/**
 * SMTP 邮箱验证码发送测试（独立 main，不依赖 Spring 容器/数据库/后台结构）
 *
 * 使用前仅需修改下面常量：
 * 1. FROM_EMAIL：发件邮箱
 * 2. AUTH_CODE ：认证密码（Outlook=应用密码；QQ/163=SMTP授权码，都不是邮箱登录密码）
 * 3. TO_EMAIL  ：收件邮箱
 * 4. SMTP_HOST / SMTP_PORT / USE_SSL：按邮箱选择
 *
 * 各邮箱 SMTP 配置：
 * - Outlook/Hotmail：smtp.office365.com，587(STARTTLS)，USE_SSL=false；
 *   应用密码需先在 account.microsoft.com → 安全 → 高级安全选项 开启两步验证后创建；
 *   注意：微软已对个人账户逐步停用基础认证，若报 535 / CLIENT not authenticated
 *   说明该账户仅支持 OAuth2，此方式走不通，建议换 QQ/163 测试
 * - QQ邮箱  ：smtp.qq.com，465(SSL)，USE_SSL=true，授权码在 设置-账户-POP3/IMAP/SMTP 服务开启后生成
 * - 163邮箱 ：smtp.163.com，465(SSL)，USE_SSL=true，授权码在 设置-POP3/SMTP/IMAP 服务开启后生成
 *
 * @author cles
 * @date 2026/09/28
 */
public class MailVerifyCodeTest {

    /** 发件邮箱 */
    private static final String FROM_EMAIL = "yunxinxiaozhan@foxmail.com";
    /** 认证密码：QQ/163=SMTP授权码 / Outlook=应用密码（个人账户基础认证已被微软停用，走不通） */
//    private static final String AUTH_CODE = "ojwgqhxmkdfmeehx";
    private static final String AUTH_CODE = "smmjzjllkjvfdeid";
    /** 收件邮箱 */
    private static final String TO_EMAIL = "1446977718@qq.com";

    /** SMTP 服务器地址 */
    private static final String SMTP_HOST = "smtp.qq.com";
    /** SMTP 端口：QQ/163=465(SSL)；Outlook=587(STARTTLS) */
    private static final int SMTP_PORT = 465;
    /** 加密方式开关：true=SSL(465，QQ/163)；false=STARTTLS(587，Outlook/Hotmail) */
    private static final boolean USE_SSL = true;
    /** 验证码有效期(分钟)，仅作邮件文案提示 */
    private static final int EXPIRE_MINUTES = 5;

    public static void main(String[] args) throws Exception {
        String code = generateCode();
        sendVerifyCode(code);
        System.out.println("====================================");
        System.out.println("验证码已发送至: " + TO_EMAIL);
        System.out.println("本次验证码    : " + code);
        System.out.println("有效期        : " + EXPIRE_MINUTES + " 分钟");
        System.out.println("====================================");
    }

    /** 生成6位数字验证码 */
    private static String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    /** 通过 SMTP 发送验证码邮件 */
    private static void sendVerifyCode(String code) throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(SMTP_HOST);
        sender.setPort(SMTP_PORT);
        sender.setUsername(FROM_EMAIL);
        sender.setPassword(AUTH_CODE);
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (USE_SSL) {
            // QQ/163：465 端口直连 SSL
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            // Outlook/Hotmail：587 端口 STARTTLS 明文升级加密
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        // 调试排错时可改为 true 查看SMTP握手日志
        props.put("mail.debug", "false");

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(FROM_EMAIL);
        helper.setTo(TO_EMAIL);
        helper.setSubject("【传信纸船】邮箱验证码");
        helper.setText(buildHtmlContent(code), true);

        sender.send(message);
    }

    /** 验证码邮件正文 */
    private static String buildHtmlContent(String code) {
        return "<div style=\"max-width:420px;margin:0 auto;padding:24px;"
                + "font-family:'Microsoft YaHei',sans-serif;background:#faf6ec;"
                + "border:1px solid #e3d2ac;border-radius:12px;text-align:center;\">"
                + "<h2 style=\"color:#7a5b1f;margin:0 0 12px;\">传信纸船 · 邮箱验证码</h2>"
                + "<p style=\"color:#666;font-size:13px;\">您正在进行邮箱验证，本次验证码为：</p>"
                + "<div style=\"font-size:32px;font-weight:700;letter-spacing:8px;"
                + "color:#b98a3e;margin:16px 0;\">" + code + "</div>"
                + "<p style=\"color:#999;font-size:12px;\">验证码 " + EXPIRE_MINUTES
                + " 分钟内有效，请勿泄露给他人。若非本人操作，请忽略本邮件。</p>"
                + "</div>";
    }
}
