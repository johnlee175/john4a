package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it means the method which be decorated
 * is a callback method, shouldn't call it explicitly from external clients,
 * like method named "onXxx" in Android
 * <br>
 *
 * 在方法上标注此注解表示此方法是一个回调方法, 不期望也不应该在外部客户端显示调用, Android中另一个相似的约定是类似"onXxx"的方法
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Documented
public @interface Callback
{
}
