package com.studyroom.web.config;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.ResultCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentUser 参数解析器
 * <p>将 Controller 方法上的 {@link CurrentUser} 注解参数解析为 {@link LoginUser}。</p>
 *
 * @author studyroom
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && LoginUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        LoginUser user = AuthContextHolder.get();
        if (user == null && !annotation.anonymous()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }
}