package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it means the parameter, the local variable, the field value, the method return value which be decorated
 * expert a value which between min and max
 * <br>
 *
 * 在方法参数上, 局部变量上, 字段上, 方法上标注此注解表示此参数值, 变量值, 字段值, 方法返回值期望取值范围在min和max之间
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
@Documented
public @interface ClampNumber
{
    public String min();
    public String max();
}
