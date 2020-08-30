package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it means the type which be decorated
 * this type of claim to be a single case
 * <br>
 *
 * 在类型上标注此注解表示此类型声称自己是单例
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface Singleton
{
}
