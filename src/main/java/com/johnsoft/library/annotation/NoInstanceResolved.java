package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it means the parameter which be decorated
 * will only be using this method call,
 * the class or it's instance which include this method
 * don't hold the parameter object reference
 * <br>
 *
 * 在参数上标注此注解表示此参数只会被此调用方法使用, 调用方法所在的类或实例不会保存参数所指对象的引用
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
@Documented
public @interface NoInstanceResolved
{
}
