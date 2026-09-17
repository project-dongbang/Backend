package com.dongbang.global.security;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) &&
                Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long id) {
                return id;
            }
            if (principal instanceof String str) {
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException ignored) {}
            }
            if (principal instanceof UserDetails userDetails) {
                try {
                    return Long.parseLong(userDetails.getUsername());
                } catch (NumberFormatException ignored) {}
            }
        }

        CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
        if (annotation != null && annotation.required()) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }

        return null;
    }
}
