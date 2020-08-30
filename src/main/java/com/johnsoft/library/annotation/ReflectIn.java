package com.johnsoft.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it means the class and identifier using by reflect in the invocation
 * which be decorated is same with origin class and identifier defined some where.
 * Example: there is a class named "com.test.Test", it has a method named getName(),
 * now call the method of the class using by reflect at some where,
 * we mark the annotation to wish some tool can check the class and identifier name had real exist,
 * because of someone maybe had modified the method name from getName() to getSimpleName(),
 * and he doesn't know the reflection exists.
 * <br>
 *
 * 在调用上标注此注解表示反射的类和标识符与被反射的类和标识符一致.
 * 比如有一个com.test.Test类, 其中有一个实例方法名为getName(),
 * 现在在其他很多地方反射这个类并调用这个方法, 在调用处标注此注解要求工具验证是否有这么个类且有这么个方法名,
 * 因为有人可能修改了com.test.Test的getName()方法为getSimpleName(), 此时他可能不知道有其他代码在反射使用这个方法.
 * <br>
 *
 * @author John Kenrinus Lee
 * @version 2015-4-3
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.LOCAL_VARIABLE})
@Documented
public @interface ReflectIn
{
    String fullClassName();
    String identifierName();
}
