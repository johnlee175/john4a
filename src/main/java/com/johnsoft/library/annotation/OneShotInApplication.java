package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it means the method which be decorated
 * can only be called in the only instance
 * where Application subclasses
 * and can only be called once
 * <br>
 *
 * 在方法上标注此注解表示此方法必须在Application子类的唯一实例上得到调用, 且仅调用一次
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Documented
public @interface OneShotInApplication
{
}
