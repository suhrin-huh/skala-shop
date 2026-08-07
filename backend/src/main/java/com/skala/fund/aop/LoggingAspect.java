package com.skala.fund.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 컨트롤러 실행 시간 측정 + 요청/응답 로깅.
 * 비밀번호와 토큰은 절대 원문으로 남기지 않는다.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /** JSON 이나 toString 출력 안에 섞여 나오는 민감 필드를 통째로 가린다. */
    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)(password|accessToken|refreshToken|token|secret|authorization)\\s*[=:]\\s*[^,)\\]}\\s]+");

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {
    }

    @Around("restController()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String endpoint = resolveEndpoint(joinPoint);
        long start = System.currentTimeMillis();

        log.info("{} 요청 - args={}", endpoint, mask(Arrays.toString(joinPoint.getArgs())));

        try {
            Object result = joinPoint.proceed();
            log.info("{} 실행시간: {}ms", endpoint, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.warn("{} 실패 ({}ms) - {}: {}", endpoint, System.currentTimeMillis() - start,
                    e.getClass().getSimpleName(), mask(e.getMessage()));
            throw e;
        }
    }

    private String resolveEndpoint(ProceedingJoinPoint joinPoint) {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            var request = servletAttributes.getRequest();
            return "[" + request.getMethod() + " " + request.getRequestURI() + "]";
        }
        // HTTP 컨텍스트 밖에서 호출된 경우(테스트 등)에는 메서드 시그니처로 대체한다.
        return "[" + joinPoint.getSignature().toShortString() + "]";
    }

    private String mask(String value) {
        if (value == null) {
            return null;
        }
        return SENSITIVE.matcher(value).replaceAll("$1=***");
    }
}
