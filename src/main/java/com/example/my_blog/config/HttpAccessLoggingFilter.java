package com.example.my_blog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * HTTP 访问日志：请求元数据 +（在链路执行后）请求体/响应体片段。
 * 密码等敏感字段在日志中会做脱敏；multipart 与二进制内容不输出原始字节。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HttpAccessLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpAccessLoggingFilter.class);

    private static final String MDC_REQUEST_ID = "requestId";

    private static final Pattern REDACT_PASSWORD = Pattern.compile(
            "(?i)(\"(?:password|token|secret|authorization)\"\\s*:\\s*)\"[^\"]*\""
    );
    private static final String REDACT_REPLACEMENT = "$1\"***\"";

    private final boolean enabled;
    private final List<String> excludePatterns;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final int maxBodyChars;
    private final int requestContentCacheLimit;

    public HttpAccessLoggingFilter(
            @Value("${app.http.access-log.enabled:true}") boolean enabled,
            @Value("${app.http.access-log.exclude-paths:/actuator/**,/error}") String excludePaths,
            @Value("${app.http.access-log.max-body-chars:4000}") int maxBodyChars,
            @Value("${app.http.access-log.request-content-cache-limit:524288}") int requestContentCacheLimit
    ) {
        this.enabled = enabled;
        this.excludePatterns = Arrays.stream(excludePaths.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        this.maxBodyChars = Math.max(256, maxBodyChars);
        this.requestContentCacheLimit = Math.max(4096, requestContentCacheLimit);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        for (String pattern : excludePatterns) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = compactUuid();
        MDC.put(MDC_REQUEST_ID, requestId);

        long startNanos = System.nanoTime();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String clientIp = resolveClientIp(request);
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String userAgent = truncate(request.getHeader("User-Agent"), 200);
        String contentType = request.getContentType();
        long contentLength = request.getContentLengthLong();
        String sessionId = request.getRequestedSessionId();

        // Spring Framework 7：缓存上限只能通过构造函数传入，已无 setContentCacheLimit
        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(request, requestContentCacheLimit);

        log.info(
                "┌── HTTP 请求 begin ── id={} {} {}{} ──",
                requestId,
                method,
                uri,
                query == null ? "" : "?" + query
        );
        log.info(
                "│  clientIp={}  forwardedFor={}  forwardedProto={}  sessionId={}",
                clientIp,
                nullToDash(request.getHeader("X-Forwarded-For")),
                forwardedProto == null ? "-" : forwardedProto,
                sessionId == null ? "-" : sessionId
        );
        log.info(
                "│  userAgent={}",
                userAgent == null ? "-" : userAgent
        );
        if (contentType != null || contentLength >= 0) {
            log.info(
                    "│  contentType={}  contentLength={}",
                    contentType == null ? "-" : contentType,
                    contentLength >= 0 ? contentLength : "-"
            );
        }

        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachingRequest, cachingResponse);
        } finally {
            int status = cachingResponse.getStatus();
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            String outcome = status >= 400 ? "FAIL" : "OK";
            log.info(
                    "└── HTTP 响应 end   ── id={} {} {}{} ── status={} {} {}ms ──",
                    requestId,
                    method,
                    uri,
                    query == null ? "" : "?" + query,
                    status,
                    outcome,
                    elapsedMs
            );

            logRequestBody(requestId, cachingRequest, contentType);
            logResponseBody(requestId, cachingResponse);

            cachingResponse.copyBodyToResponse();
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private void logRequestBody(String requestId, ContentCachingRequestWrapper request, String requestContentType) {
        if (requestContentType != null && requestContentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            log.info("│  requestBody id={} ── [multipart/form-data, raw body not logged]", requestId);
            return;
        }
        byte[] buf = request.getContentAsByteArray();
        if (buf == null || buf.length == 0) {
            log.info("│  requestBody id={} ── (empty)", requestId);
            return;
        }
        Charset charset = resolveHttpBodyCharsetForLog(requestContentType, request.getCharacterEncoding(), buf);
        String raw = new String(buf, charset);
        String safe = redactSensitive(raw);
        log.info(
                "│  requestBody(snippet) id={} ── {}",
                requestId,
                truncate(safe, maxBodyChars)
        );
    }

    private void logResponseBody(String requestId, ContentCachingResponseWrapper response) {
        String respType = response.getContentType();
        if (respType != null) {
            String lower = respType.toLowerCase(Locale.ROOT);
            if (lower.startsWith("image/")
                    || lower.startsWith("audio/")
                    || lower.startsWith("video/")
                    || lower.contains("octet-stream")) {
                byte[] buf = response.getContentAsByteArray();
                int n = buf == null ? 0 : buf.length;
                log.info("│  responseBody id={} ── [binary {} bytes, type={}]", requestId, n, respType);
                return;
            }
        }

        byte[] buf = response.getContentAsByteArray();
        if (buf == null || buf.length == 0) {
            log.info("│  responseBody id={} ── (empty)", requestId);
            return;
        }
        Charset charset = resolveHttpBodyCharsetForLog(respType, response.getCharacterEncoding(), buf);
        String raw = new String(buf, charset);
        String safe = redactSensitive(raw);
        if (statusIsError(response.getStatus())) {
            log.warn(
                    "│  responseBody(snippet) id={} ── {}",
                    requestId,
                    truncate(safe, maxBodyChars)
            );
        } else {
            log.info(
                    "│  responseBody(snippet) id={} ── {}",
                    requestId,
                    truncate(safe, maxBodyChars)
            );
        }
    }

    private static boolean statusIsError(int status) {
        return status >= 400;
    }

    private static Charset resolveCharset(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 将响应体字节解码为日志字符串时使用的字符集。
     * <p>
     * JSON 等响应体实际是 UTF-8，但 {@link jakarta.servlet.ServletResponse#getCharacterEncoding()} 在未显式设置时
     * 常为 ISO-8859-1（Servlet 默认），若按该编码解码会把中文打成乱码。优先解析 Content-Type 中的 charset，
     * 并对常见文本/JSON 类型在无 charset 时默认 UTF-8；若 Content-Type 缺失但字节像 JSON/UTF-8，则回退 UTF-8。
     */
    private static Charset resolveHttpBodyCharsetForLog(String contentType, String servletCharacterEncoding, byte[] buf) {
        Charset fromContentType = parseCharsetFromContentTypeHeader(contentType);
        if (fromContentType != null) {
            return fromContentType;
        }
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.contains("application/json")
                    || lower.contains("application/problem+json")
                    || lower.contains("application/xml")
                    || lower.contains("text/html")
                    || lower.contains("text/xml")
                    || lower.contains("text/plain")
                    || lower.contains("application/javascript")) {
                return StandardCharsets.UTF_8;
            }
        }
        Charset servletCharset = resolveCharset(servletCharacterEncoding);
        if (StandardCharsets.ISO_8859_1.equals(servletCharset) && looksLikeUtf8JsonOrText(buf)) {
            return StandardCharsets.UTF_8;
        }
        return servletCharset;
    }

    private static Charset parseCharsetFromContentTypeHeader(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        int idx = contentType.toLowerCase(Locale.ROOT).indexOf("charset=");
        if (idx < 0) {
            return null;
        }
        String rest = contentType.substring(idx + "charset=".length()).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            if (end > 1) {
                rest = rest.substring(1, end);
            }
        } else {
            int semi = rest.indexOf(';');
            if (semi >= 0) {
                rest = rest.substring(0, semi);
            }
        }
        rest = rest.trim();
        if (rest.isEmpty()) {
            return null;
        }
        try {
            return Charset.forName(rest);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 在 Content-Type 未标明 charset、但 Servlet 仍报告 ISO-8859-1 时，用简单启发式判断是否为 UTF-8 文本/JSON。
     */
    private static boolean looksLikeUtf8JsonOrText(byte[] buf) {
        if (buf == null || buf.length == 0) {
            return false;
        }
        byte b0 = buf[0];
        if (b0 == '{' || b0 == '[') {
            return true;
        }
        if (buf.length >= 3
                && (buf[0] & 0xFF) == 0xEF
                && (buf[1] & 0xFF) == 0xBB
                && (buf[2] & 0xFF) == 0xBF) {
            return true;
        }
        return false;
    }

    private static String redactSensitive(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        return REDACT_PASSWORD.matcher(raw).replaceAll(REDACT_REPLACEMENT);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }

    private static String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toLowerCase(Locale.ROOT);
    }
}
