package com.johnsoft.library.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * SimpleDateFormat是非线程安全的, 此类包装了SimpleDateFormat, 以每线程一个实例的方式应对并发而不失性能;<br>
 * 有getWide()和getUnsigned(), 其保证不会返回null, 分别返回"yyyy-MM-dd HH:mm:ss.SSS"和"yyyyMMddHHmmss"模式, 两者互不影响;<br>
 * 如果需要更改, 可以使用applyPattern()或者applyLocalizedPattern(), 但这样做会影响在此线程上使用此实例的其他用例;<br>
 * 所以如果一次性使用SimpleDateFormat而且模式不是上面两种, 不应使用此类的方法, 可以新建一个实例;<br>
 * @author John Kenrinus Lee
 * @version 2015-10-30
 */
public final class ConcurrentDateFormat {
    private static final ThreadLocal<SimpleDateFormat> POOL_WIDE = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        }
    };

    private static final ThreadLocal<SimpleDateFormat> POOL_UNSIGNED = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        }
    };

    private ConcurrentDateFormat() {}

    public static SimpleDateFormat getWide() {
        return POOL_WIDE.get();
    }

    public static SimpleDateFormat getUnsigned() {
        return POOL_UNSIGNED.get();
    }
}
