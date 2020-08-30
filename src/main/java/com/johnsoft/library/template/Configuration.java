package com.johnsoft.library.template;

import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author John Kenrinus Lee
 * @version 2016-06-16
 */
public final class Configuration implements Parcelable {
    /** server响应返回给调用者(JSON格式) */
    public static final String KEY_FORWARD_SERVER_RESPONSE = "forwardServerResponse";

    private final boolean forwardServerResponse;

    public Configuration() {
        this(new Builder());
    }

    private Configuration(Builder builder) {
        this.forwardServerResponse = builder.forwardServerResponse;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    private Configuration(Parcel in) {
        forwardServerResponse = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (forwardServerResponse ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Configuration> CREATOR = new Creator<Configuration>() {
        @Override
        public Configuration createFromParcel(Parcel in) {
            return new Configuration(in);
        }

        @Override
        public Configuration[] newArray(int size) {
            return new Configuration[size];
        }
    };

    public boolean isForwardServerResponse() {
        return forwardServerResponse;
    }

    public static final class Builder {
        private boolean forwardServerResponse;

        public Builder() {
            this.forwardServerResponse = Setting.FORWARD_SERVER_RESPONSE;
        }

        // 无论配置是来自网络还是本地, 或者ContentProvider组件,
        // 无论来自普通文件/.ini/.properties/sqlite/json/xml/SharedPreferences还是跨进程传参利器Bundle对象,
        // 既然是配置, 都有办法转化成Map对象
        public Builder(Map<String, Object> map) {
            this.forwardServerResponse = (boolean)map.get(KEY_FORWARD_SERVER_RESPONSE);
        }

        private Builder(Configuration configuration) {
            this.forwardServerResponse = configuration.forwardServerResponse;
        }

        public Builder setForwardServerResponse(boolean forwardServerResponse) {
            this.forwardServerResponse = forwardServerResponse;
            return this;
        }

        public final Configuration build() {
            return new Configuration(this);
        }
    }
}
