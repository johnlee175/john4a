package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it means the method which be decorated
 * can't be called in main ui thread
 * <br>
 * NOTICE: like as android.support.annotation.MainThread and android.support.annotation.UiThread
 * <br>
 * 在方法上标注此注解表示此方法不可以在主线程中被调用
 * <br>
 * 注意: 与android.support.annotation.BinderThread和android.support.annotation.WorkerThread类似
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Documented
public @interface OutMainThread
{
}
