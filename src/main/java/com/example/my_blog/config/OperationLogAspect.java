package com.example.my_blog.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    @Around(
            "execution(public * com.example.my_blog.controller..*(..)) " +
            "|| execution(public * com.example.my_blog.service..*(..))"
    )
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.info("START {}", signature);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("SUCCESS {} ({} ms)", signature, elapsed);
            return result;
        } catch (Throwable throwable) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("ERROR {} ({} ms): {}", signature, elapsed, throwable.getMessage(), throwable);
            throw throwable;
        }
    }
}
