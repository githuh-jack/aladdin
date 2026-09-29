package com.aladdin.common.core.constant;

/**
 * Redis键常量
 *
 * @author cles
 * @date 2026/04/30
 */
public interface RedisKeyConstant {

    /** 系统特有前缀 */
    String PREFIX = "ZM:";

    String LOGIN_TOKEN = PREFIX + "login:token:";
    String LOGIN_USER = PREFIX + "login:user:";
    String CAPTCHA = PREFIX + "captcha:";
    String RATE_LIMIT = PREFIX + "rate_limit:";
    String REPEAT_SUBMIT = PREFIX + "repeat_submit:";
    String SYS_CONFIG = PREFIX + "sys:config:";
    String SYS_DICT = PREFIX + "sys:dict:";

    /** 密码错误次数 */
    String PWD_ERROR_COUNT = PREFIX + "pwd:error:count:";
    /** 账号锁定 */
    String ACCOUNT_LOCK = PREFIX + "account:lock:";
    /** 密码历史记录 */
    String PWD_HISTORY = PREFIX + "pwd:history:";
    /** 黑名单 */
    String IP_BLACKLIST = PREFIX + "ip:blacklist";
    /** 白名单 */
    String IP_WHITELIST = PREFIX + "ip:whitelist";
    /** Open-API ak/sk */
    String OPEN_API_APP = PREFIX + "openapi:app:";
    /** Open-API nonce防重放 */
    String OPEN_API_NONCE = PREFIX + "openapi:nonce:";
    /** 邮箱验证码 */
    String EMAIL_CODE = PREFIX + "email:code:";
    /** 邮箱验证码发送频率限制 */
    String EMAIL_CODE_LIMIT = PREFIX + "email:code:limit:";
    /** 站内信未读数 */
    String MSG_UNREAD_COUNT = PREFIX + "msg:unread:";
    /** 租户缓存 */
    String TENANT = PREFIX + "tenant:";
}
