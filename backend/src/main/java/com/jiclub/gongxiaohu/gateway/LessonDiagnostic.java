package com.jiclub.gongxiaohu.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课堂凭证链路的「验证诊断」记录器（对应 技术说明.md §6.7 待补抓包清单）。
 *
 * <p>目的：真实开课时，无需用户手工抓包，后端就把验证所需的证据结构化返回：
 * <ol>
 *   <li>checkin 实际发出的<b>全套请求头清单</b>（哪些有值、哪些缺失）</li>
 *   <li>响应头是否含 <code>Set-Auth</code> 及其指纹（长度/前缀，<b>不记明文</b>）</li>
 *   <li>业务码与三元组（socketJwt / identityId）是否齐全</li>
 *   <li>source 回退轨迹与每次结果</li>
 *   <li>耗时、HTTP 状态、失败分类</li>
 * </ol>
 *
 * <p><b>隐私边界</b>：只记录字段名、有无、长度与不可逆指纹；
 * 绝不记录 sessionid / csrftoken / pptJwt / socketJwt / identityId 的明文值。
 * 记录仅存内存环形缓冲（最近 {@value #MAX_RECORDS} 条），不落盘、不写日志文件，进程重启即失效。
 */
@Component
public class LessonDiagnostic {
    private static final Logger log = LoggerFactory.getLogger(LessonDiagnostic.class);
    private static final int MAX_RECORDS = 50;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    /** 单次 checkin 尝试的验证记录（全部为脱敏信息）。 */
    public record Record(
            String time,            // 本地时间 HH:mm:ss.SSS
            String lessonId,
            int source,
            String url,
            Map<String, String> requestHeaders,  // 头名 -> "有值" / "缺失" / 长度指纹
            List<String> missingHeaders,         // 缺失的关键头（排查 401 用）
            int httpStatus,
            int bizCode,
            String bizMessage,
            boolean setAuthPresent,
            String setAuthFingerprint,           // 如 "len=248,prefix=eyJhbG***"，无明文
            boolean socketJwtPresent,
            boolean identityIdPresent,
            String outcome,                     // SUCCESS / DYNAMIC_QR / AUTH_EXPIRED / LESSON_END / ...
            long elapsedMs
    ) {}

    /** §6.7 验证清单：每项是否已拿到证据。 */
    public record Checklist(
            boolean checkinHeadersCaptured,   // ① 全套请求头
            boolean setAuthCaptured,          // ② 原始 Set-Auth 证据（指纹）
            boolean tripleCaptured,           // ③ 三元组齐全
            boolean websocketHintCaptured,    // ④ socketJwt 存在性（URL 用法仍需抓包）
            boolean getTokenKnown,            // ⑤ get-token 归属已确认（静态结论，无需抓包）
            String note
    ) {}

    /** 诊断总览：最近记录 + 验证清单 + 提示。 */
    public record Report(
            int totalAttempts,
            List<Record> records,
            Checklist checklist,
            String privacyNote
    ) {}

    private final Deque<Record> records = new ArrayDeque<>();

    /** 记录一次 checkin 尝试。 */
    public synchronized void record(Record r) {
        records.addLast(r);
        while (records.size() > MAX_RECORDS) records.removeFirst();
        // 控制台只输出脱敏摘要，便于服务器 tail -f 实时观察
        log.info("[诊断] checkin source={} outcome={} http={} code={} setAuth={} triple=({},{}) 缺头={} 耗时={}ms",
                r.source(), r.outcome(), r.httpStatus(), r.bizCode(),
                r.setAuthPresent(), r.socketJwtPresent(), r.identityIdPresent(),
                r.missingHeaders(), r.elapsedMs());
    }

    /** 生成诊断报告（供 GET /api/v1/diagnostics 返回）。 */
    public synchronized Report report() {
        List<Record> snapshot = new ArrayList<>(records);
        boolean headers = snapshot.stream().anyMatch(r -> !r.requestHeaders().isEmpty());
        boolean setAuth = snapshot.stream().anyMatch(Record::setAuthPresent);
        boolean triple = snapshot.stream().anyMatch(r -> r.setAuthPresent()
                && r.socketJwtPresent() && r.identityIdPresent());
        boolean wsHint = snapshot.stream().anyMatch(Record::socketJwtPresent);
        String note;
        if (snapshot.isEmpty()) {
            note = "尚无 checkin 记录：请在真实开课时点击「进入课堂」触发一次凭证换取";
        } else if (!setAuth) {
            note = "尚未拿到 Set-Auth：优先检查 missingHeaders，补齐后重新导入 Cookie（/auth/import-cookie）";
        } else if (!triple) {
            note = "已拿到 Set-Auth，但 lessonToken / identityId 缺失：需按实际响应字段名调整解析（§6.7 ③）";
        } else {
            note = "三元组已齐全。WebSocket URL 中 socketJwt/identityId 的用法仍需抓包确认（§6.7 ④）";
        }
        return new Report(snapshot.size(), snapshot,
                new Checklist(headers, setAuth, triple, wsHint, true,
                        "get-token 已确认属 pc.js 教师端接口表，非学生进班凭据，不需要抓包"),
                "记录已脱敏：不含任何 Cookie / JWT 明文，仅内存保存最近 50 条");
    }

    public synchronized void clear() {
        records.clear();
    }

    /** 生成不可逆指纹：仅长度 + 掩码前缀，用于确认「拿到了」但不暴露内容。 */
    public static String fingerprint(String secret) {
        if (secret == null || secret.isBlank()) return "无";
        String prefix = secret.length() <= 4 ? "****" : secret.substring(0, 4) + "***";
        return "len=" + secret.length() + ",prefix=" + prefix;
    }

    /** 构造脱敏的请求头映射（值只记「有值/缺失」，敏感字段记长度指纹）。 */
    public static Map<String, String> headers(Map<String, String> raw, List<String> missing) {
        Map<String, String> out = new LinkedHashMap<>();
        raw.forEach((k, v) -> {
            boolean present = v != null && !v.isBlank();
            out.put(k, present ? "有值" : "缺失");
            if (!present) missing.add(k);
        });
        return out;
    }

    public static String now() {
        return TIME_FMT.format(Instant.now());
    }
}
