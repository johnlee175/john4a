package com.johnsoft.library.annotation;

import java.lang.annotation.*;

/**
 * It is used to describe the major function modules,
 * minor(sub) branches modules and author(or who maintain the code),
 * <br>
 *
 * 它用于描述类型(或字段所属类型)所属的主功能模块, 次(子)分支模块, 以及作者(维护者)
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-11-3
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD})
@Documented
public @interface Module {
    String major();
    String minor();
    String author();
}
