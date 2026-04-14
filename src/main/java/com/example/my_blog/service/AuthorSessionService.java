package com.example.my_blog.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthorSessionService {

    private static final String AUTHOR_SESSION_KEY = "AUTHOR_AUTHENTICATED";

    private final PostService postService;
    private final int maxAttempts;
    private final long windowMillis;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public AuthorSessionService(
            PostService postService,
            @Value("${blog.author.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${blog.author.login-rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.postService = postService;
        this.maxAttempts = maxAttempts;
        this.windowMillis = Math.max(1, windowSeconds) * 1000L;
    }

    public void verifyAndLogin(String providedPassword, HttpServletRequest request) {
        String key = buildRateLimitKey(request);
        long now = System.currentTimeMillis();
        AttemptWindow window = attempts.computeIfAbsent(key, ignored -> new AttemptWindow());

        synchronized (window) {
            if (window.windowStartMillis == 0 || now - window.windowStartMillis > windowMillis) {
                window.windowStartMillis = now;
                window.failCount = 0;
            }

            if (window.failCount >= maxAttempts) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "尝试次数过多，请稍后再试。"
                );
            }

            if (!postService.matchesAuthorPassword(providedPassword)) {
                window.failCount++;
                if (window.failCount >= maxAttempts) {
                    throw new ResponseStatusException(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "尝试次数过多，请稍后再试。"
                    );
                }
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "作者密码错误。");
            }

            window.failCount = 0;
            window.windowStartMillis = now;
        }

        request.getSession(true).setAttribute(AUTHOR_SESSION_KEY, Boolean.TRUE);
    }

    public void requireAuthor(HttpServletRequest request) {
        if (!isAuthenticated(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请先进行作者登录。");
        }
    }

    public boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object value = session.getAttribute(AUTHOR_SESSION_KEY);
        return value instanceof Boolean bool && bool;
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private String buildRateLimitKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String sessionId = request.getSession(true).getId();
        return ip + ":" + sessionId;
    }

    private static final class AttemptWindow {
        private long windowStartMillis;
        private int failCount;
    }
}
