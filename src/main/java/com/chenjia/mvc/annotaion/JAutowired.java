package com.chenjia.mvc.annotaion;

import java.lang.annotation.*;

/**
 * @author chenjia@joyveb.com
 * @date 2020/11/3 10:41 上午
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JAutowired {
    String value() default "";
}

