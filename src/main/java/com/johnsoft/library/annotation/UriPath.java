package com.johnsoft.library.annotation;

import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that an String parameter, field or method return value is expected
 * to be an URI/URL string or valid file path
 * <br>
 *
 * 在方法, 字段, 参数上标注此注解表示此方法返回值, 此字段值或参数值表示或期望是一个URI/URL格式字符串, 或一个有效的文件路径
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
@Documented
public @interface UriPath {

}
