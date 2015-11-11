package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This note is to indicate the method, constructor, field, type for some special reasons for accessibility statement,
 * but it may indicate an internal state or its potential for non maintenance in subsequent releases,
 * so it should not be accessed or called explicitly on its external.
 *
 * 标明此注解是希望被标注的方法, 构造器, 属性, 类型因某种特殊原因声明为可访问,
 * 但其可能表示内部状态或其在后续版本中可能不予维护, 而不应在其外部显示访问或调用. <br>
 * @author John Kenrinus Lee
 * @version 2015-08-07
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.TYPE})
public @interface Hidden {
}
