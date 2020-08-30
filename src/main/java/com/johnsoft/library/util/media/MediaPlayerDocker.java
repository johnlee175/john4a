package com.johnsoft.library.util.media;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.johnsoft.library.util.DefaultTask;
import com.johnsoft.library.util.SimpleTaskExecutor;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

/**
 * MediaPlayer的一个简易封装: <br>
 * (1)可替换播放源; <br>
 * (2)一般播放结束后可分发事件回调({@link #registerOnCompletionListener(OnCompletionListener)}); <br>
 * (3)建议一个实例对应一个播放源, 且不要轻易release, 主要用于需要多次播放同一个播放源的情况; <br>
 *
 * @author John Kenrinus Lee
 * @version 2016-02-22
 */
public class MediaPlayerDocker implements MediaPlayer.OnCompletionListener {
    private final Set<OnCompletionListener> set = new LinkedHashSet<>();
    private MediaPlayer mp;

    public void prepare(Context context, int rawResId) throws IOException {
        prepare(context.getResources().openRawResourceFd(rawResId));
    }

    public void prepare(AssetFileDescriptor path) throws IOException {
        prepare(path, null);
    }

    public void prepare(Context context, Uri path) throws IOException {
        prepare(path, context);
    }

    public void prepare(String path) throws IOException {
        prepare(path, null);
    }

    private void prepare(Object path, Context context) throws IOException {
        if (mp == null) {
            mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            if (path instanceof AssetFileDescriptor) {
                AssetFileDescriptor afd = (AssetFileDescriptor) path;
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            } else if (path instanceof String) {
                mp.setDataSource((String) path);
            } else if (path instanceof Uri) {
                mp.setDataSource(context, (Uri) path);
            } else {
                throw new IllegalArgumentException("the type of \"path\" is illegal");
            }
            mp.setOnCompletionListener(this);
            mp.prepare();
        }
    }

    /** 没有进行reset, 但会调用seekTo(0) */
    public void start() {
        if (mp != null) {
//            mp.reset();
            mp.seekTo(0);
            mp.start();
        }
    }

    public void stop() {
        if (mp != null) {
            mp.stop();
        }
    }

    public boolean isPlaying() {
        if (mp != null) {
            return mp.isPlaying();
        }
        return false;
    }

    /** 清理MediaPlayer资源, 并清理所有播放完成事件的观察者 */
    public void release() {
        if (mp != null) {
            mp.release();
            mp = null;
            set.clear();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        for (MediaPlayerDocker.OnCompletionListener onCompletionListener : set) {
            SimpleTaskExecutor.scheduleNow(new OnCompletionRunnable(this, onCompletionListener));
        }
    }

    public void registerOnCompletionListener(MediaPlayerDocker.OnCompletionListener l) {
        set.add(l);
    }

    public void unregisterOnCompletionListener(MediaPlayerDocker.OnCompletionListener l) {
        set.remove(l);
    }

    public interface OnCompletionListener {
        void onCompletion(MediaPlayerDocker thread);
    }

    private static class OnCompletionRunnable extends DefaultTask {
        private final MediaPlayerDocker thread;
        private final MediaPlayerDocker.OnCompletionListener listener;
        public OnCompletionRunnable(MediaPlayerDocker thread,
                                    MediaPlayerDocker.OnCompletionListener listener) {
            super("MediaPlayerDocker.OnCompletionRunnable");
            this.thread = thread;
            this.listener = listener;
        }

        @Override
        public void doTask() {
            if (listener != null) {
                listener.onCompletion(thread);
            }
        }
    }
}
