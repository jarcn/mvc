package com.chenjia.mvc.annotaion;

import java.lang.annotation.*;

/**
 * @author chenjia@joyveb.com
 * @date 2020/11/3 10:44 上午
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JRequestMapping {
    String value() default "";
}
