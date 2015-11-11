package com.johnsoft.library.util.animation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Point;
import android.os.SystemClock;
import android.support.annotation.RawRes;
import android.util.AttributeSet;
import android.view.View;

/**
 * 播放gif格式图片动画的视图
 * @author John Kenrinus Lee
 * @version 2015-10-19
 */
public class MovieGifView extends View {
    private int playCount;
    private long movieStart;
    private Movie movie;
    private OnMovieFinishedListener onMovieFinishedListener;
    private FitPositionType fitPositionType;
    private Point startPoint;

    public MovieGifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        fitPositionType = FitPositionType.FIT_START;
    }

    /**
     * 设置资源
     */
    public void decodeRawResouce(@RawRes int gifResId) {
        decodeStream(getResources().openRawResource(gifResId));
    }

    /**
     * 设置资源
     */
    public void decodeAssets(String fileName) {
        try {
            decodeStream(getResources().getAssets().open(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置资源
     */
    public void decodeStream(InputStream inputStream) {
        movie = Movie.decodeStream(inputStream);
    }

    /**
     * 设置资源
     */
    public void decodeFile(File file) {
        decodeFile(file.getAbsolutePath());
    }

    /**
     * 设置资源
     */
    public void decodeFile(String filePath) {
        movie = Movie.decodeFile(filePath);
    }

    /**
     * 设置重复播放次数; playCount传入1, 即一共仅播放一次, 传入2, 一共播放两次, 当播放结束后回调OnMovieFinishedListener;
     * 如果小于1, 则将无限次不停重复播放下去, OnMovieFinishedListener会被忽略;
     * @param playCount 重复播放次数
     * @param listener 播放结束后回调此接口
     */
    public void setRepeatPlayCount(int playCount, OnMovieFinishedListener listener) {
        this.playCount = playCount;
        if (playCount > 0) {
            this.onMovieFinishedListener = listener;
        }
    }

    /**
     * 设置图片在视图中的位置, 目前仅支持三种位置类型
     * @param type 如果为FitPositionType.FIT_START则将图片左上角摆放在视图的(0,0)显示;
     *             如果为FitPositionType.FIT_END则将图片右下角摆放在视图的(width, height)显示;
     *             如果为FitPositionType.FIT_CENTER则将图片于水平和垂直居中在视图上显示;
     */
    public void setFitPositionType(FitPositionType type) {
        this.fitPositionType = type;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long currTime = SystemClock.uptimeMillis();
        if (movieStart == 0) {
            movieStart = currTime;
        }
        if (movie != null) {
            int duration = movie.duration();
            int relTime = (int)((currTime - movieStart) % duration);
            movie.setTime(relTime);
            if (startPoint == null) {
                parseFitPositionType();
            }
            movie.draw(canvas, startPoint.x, startPoint.y);
            if (playCount <= 0 || ((int)((currTime - movieStart) / duration) < playCount)) {
                invalidate();
            } else {
                if (onMovieFinishedListener != null) {
                    onMovieFinishedListener.onMovieFinished();
                }
            }
        }
    }

    private void parseFitPositionType() {
        final int mw = movie.width(), mh = movie.height();
        final int vw = getWidth(), vh = getHeight();
        switch (fitPositionType) {
            case FIT_CENTER:
                startPoint = new Point((int)((vw- mw) / 2.0f), (int)((vh - mh) / 2.0f));
                break;
            case FIT_END:
                startPoint = new Point(vw - mw, vh - mh);
                break;
            case FIT_START:
            default:
                startPoint = new Point();
        }
    }

    public interface OnMovieFinishedListener {
        void onMovieFinished();
    }

    public enum FitPositionType {
        FIT_START, FIT_END, FIT_CENTER
    }
}
