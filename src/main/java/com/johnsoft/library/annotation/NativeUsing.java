package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it means the method or field which be decorated
 * has been called or used in native method(JNI/NDK), any changes are likely to cause an error.
 * <br>
 *
 * 在方法或属性上标注此注解表示此方法或属性已在本地方法(JNI/NDK)中被调用或使用, 任何对其的修改(比如修改名称, 删除元素)都可能引发错误.
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Documented
public @interface NativeUsing
{
    String[] whoUsing();
}
