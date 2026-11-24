package com.jiclub.gongxiaohu.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 行为审计日志（logger 名 {@code AUDIT}，输出到 logs/audit.log）。
 *
 * <p>与普通运行日志的分工：
 * <ul>
 *   <li>普通日志（app.log）：排查接口故障用，<b>学号脱敏</b>、不含凭证；</li>
 *   <li>审计日志（audit.log）：回答"谁在什么时候做了什么"，<b>学号明文</b>，
 *       但 token / Cookie / sessionid / pptJwt 一律不记录，只记指纹或长度。</li>
 * </ul>
 *
 * <p>每条记录统一格式 {@code 事件 键=值 ...}，便于 grep/awk：
 * <pre>
 * 2026-11-24 18:01:02.345 INFO  [tomcat-handler-3] AUDIT - LOGIN_OK method=qrcode userId=2023012345 ip=1.2.3.4 client=AndroidApp(okhttp)
 * </pre>
 */
public final class AuditLog {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private AuditLog() {
    }

    /** 登录成功：记录<b>明文学号</b>与来源渠道、IP、客户端。 */
    public static void loginOk(String method, String userId) {
        AUDIT.info("LOGIN_OK method={} userId={} ip={} client={}",
                nz(method), nz(userId), RequestContext.ip(), RequestContext.client());
    }

    /** 登录失败：account 只保留前 4 位，避免把错误输入的密码/完整账号写进日志。 */
    public static void loginFail(String method, String account, String reason) {
        AUDIT.info("LOGIN_FAIL method={} account={} reason={} ip={} client={}",
                nz(method), brief(account), nz(reason), RequestContext.ip(), RequestContext.client());
    }

    /** 主动登出。 */
    public static void logout(String userId) {
        AUDIT.info("LOGOUT userId={} ip={} client={}",
                nz(userId), RequestContext.ip(), RequestContext.client());
    }

    /** 进入课堂（用户主动点击触发）。 */
    public static void enterLesson(String userId, String courseId, String lessonId,
                                   String entryId, boolean credentialReady) {
        AUDIT.info("ENTER_LESSON userId={} courseId={} lessonId={} entryId={} credential={} ip={} client={}",
                nz(userId), nz(courseId), nz(lessonId), nz(entryId),
                credentialReady ? "ready" : "fallback", RequestContext.ip(), RequestContext.client());
    }

    private static String nz(String v) {
        return v == null || v.isBlank() ? "-" : v;
    }

    private static String brief(String v) {
        if (v == null || v.isBlank()) return "-";
        String s = v.trim();
        return s.length() <= 4 ? "****" : s.substring(0, 4) + "****";
    }
}
