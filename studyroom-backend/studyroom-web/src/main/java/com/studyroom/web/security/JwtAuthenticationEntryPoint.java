package com.studyroom.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.result.Result;
import com.studyroom.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未登录 / token 无效 → 401
 *
 * @author studyroom
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("[JwtAuthenticationEntryPoint] 401 unauthorized path={}, msg={}",
                request.getRequestURI(), authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.fail(ResultCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}