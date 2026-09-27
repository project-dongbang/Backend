package com.dongbang.global.response;

import com.dongbang.global.response.code.BaseErrorCode;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorExamples.List.class)
public @interface ApiErrorExamples {
    Class<? extends BaseErrorCode> value();
    String[] names();

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        ApiErrorExamples[] value();
    }
}
