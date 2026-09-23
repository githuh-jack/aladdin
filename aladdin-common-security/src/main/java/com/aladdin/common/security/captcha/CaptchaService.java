package com.aladdin.common.security.captcha;

import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.redis.RedisService;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 *
 * @author cles
 * @date 2026/05/06
 */
@Component
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = RedisKeyConstant.CAPTCHA;
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;

    private final RedisService redisService;

    public CaptchaService(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * 生成验证码
     */
    public Map<String, Object> generateCaptcha() {
        CaptchaGenerator.CaptchaResult result = CaptchaGenerator.generate();
        String uuid = UUID.randomUUID().toString().replace("-", "");

        redisService.set(CAPTCHA_PREFIX + uuid, result.getCode(), CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try {
            ImageIO.write(result.getImage(), "png", bos);
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
        String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());

        Map<String, Object> data = new HashMap<>();
        data.put("uuid", uuid);
        data.put("img", base64Image);
        return data;
    }

    /**
     * 校验验证码
     */
    public boolean validate(String uuid, String code) {
        if (uuid == null || code == null) {
            return false;
        }
        String cachedCode = redisService.get(CAPTCHA_PREFIX + uuid);
        if (cachedCode == null) {
            return false;
        }
        redisService.delete(CAPTCHA_PREFIX + uuid);
        return cachedCode.equalsIgnoreCase(code);
    }
}
