package com.johnsoft.library.util;

public final class ClassUtils {
    private ClassUtils() {
    }

    public static boolean isCurrentMethodCalledFromThisClassOrSubclass() {
        try {
            // [0]是Thread#getStackTrace();
            // [1]是ClassUtils#isCurrentMethodCalledFromThisClassOrSubclass();
            // [2]是当前类的当前方法;
            // [3]是从哪个类的哪个方法调用的当前类的当前方法;
            final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            if (!elements[3].isNativeMethod()
                    && Class.forName(elements[2].getClassName())
                    .isAssignableFrom(Class.forName(elements[3].getClassName()))) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}