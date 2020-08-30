package com.johnsoft.library.view;

import static android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

/**
 * Example:
 * <p><hr><pre><code>
 * {@code
 *  final RecordIndicator recordIndicator = RecordIndicator.getDefault(getApplicationContext());
 *  recordIndicator.setOnRecordStateListener(new RecordIndicator.OnRecordStateListener() {
 *      public void onRecordStateChanged(boolean recording) {
 *          if (recording) {
 *              recordIndicator.smoothSlidingTo(120, 700, 1000L, new AccelerateDecelerateInterpolator(), null);
 *          } else {
 *              recordIndicator.smoothSlidingTo(400, 120, 1000L, new AccelerateDecelerateInterpolator(), null);
 *          }
 *      }
 *  });
 *  recordIndicator.moveTo(30, 30).sizeTo(300, 300).show();
 * }
 * </code></pre><hr></p>
 *
 * file name: RecordIndicator.java
 * @author John Kenrinus Lee
 * @version 2017-01-15
 */
public class RecordIndicator extends View implements View.OnClickListener {
    private static RecordIndicator recordIndicator;

    /**
     * @param context can be application context
     * @return not RecordIndicator singleton
     */
    public static RecordIndicator getDefault(Context context) {
        if (recordIndicator == null) {
            recordIndicator = new RecordIndicator(context);
        }
        return recordIndicator;
    }

    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    private boolean isGone;
    private boolean isDismiss;

    private int alphaLowerBound;
    private int alphaUpperBound;
    private int touchSlop;

    private int kernelColor;
    private int outlineColor;

    private boolean recording;
    private OnRecordStateListener l;

    private int radialGradientRadius;

    private RadialGradient radialGradient;
    private PorterDuffColorFilter colorFilter;
    private Paint kernelPaint;
    private Paint outlinePaint;

    private int customAlpha;
    private float customScale;
    private int step;

    private float offsetX;
    private float offsetY;
    private boolean dragging;

    public RecordIndicator(Context context) {
        super(context);
        initDefaults();
    }

    public RecordIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDefaults();
    }

    private void initDefaults() {
        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_TOAST);
        windowParams.format = PixelFormat.RGBA_8888;
        windowParams.flags = FLAG_ALLOW_LOCK_WHILE_SCREEN_ON | FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL;
        isGone = true;
        isDismiss = true;

        alphaLowerBound = 60;
        alphaUpperBound = 180;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        kernelColor = 0XFFCC1122; // argb
        outlineColor = 0XFFFF0000; // argb

        recording = false;
        l = null;

        radialGradientRadius = 0;
        radialGradient = null;
        colorFilter = new PorterDuffColorFilter(0XE0F96EB3, PorterDuff.Mode.SRC_ATOP);
        kernelPaint = new Paint();
        kernelPaint.setAntiAlias(true);
        kernelPaint.setDither(true);
        kernelPaint.setHinting(Paint.HINTING_ON);
        kernelPaint.setStyle(Paint.Style.FILL);
        kernelPaint.setColor(kernelColor);
        kernelPaint.setAlpha(255);
        outlinePaint = new Paint(kernelPaint);
        outlinePaint.setColor(outlineColor);

        resetDrawParams();
        resetTouchEventParams();

        super.setOnClickListener(this);
    }

    public final Paint getKernelPaint() {
        return kernelPaint;
    }

    public final Paint getOutlinePaint() {
        return outlinePaint;
    }

    public final RecordIndicator setAlphaBound(int alphaLowerBound, int alphaUpperBound) {
        if (alphaLowerBound > alphaUpperBound) {
            alphaUpperBound = alphaLowerBound;
        }
        if (alphaLowerBound <= 255 && alphaLowerBound >= 0) {
            this.alphaLowerBound = alphaLowerBound;
        }
        if (alphaUpperBound <= 255 && alphaUpperBound >= 0) {
            this.alphaUpperBound = alphaUpperBound;
        }
        return this;
    }

    /** we hold it */
    @Override
    public final void setOnClickListener(OnClickListener l) {
    }

    public final void setOnRecordStateListener(OnRecordStateListener l) {
        this.l = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int viewW = getWidth();
        final int viewH = getHeight();
        final int halfW = (int) (viewW / 2.0F);
        final int halfH = (int) (viewH / 2.0F);
        final int quarterW = (int) (halfW / 2.0F);
        final int quarterH = (int) (halfH / 2.0F);
        final int radius = Math.min(quarterW, quarterH);
        final int alphaStep = (int) ((alphaUpperBound - alphaLowerBound) / 60.0F/* fps */);
        final float scaleStep = radius / 60.0F/* fps */;

        if (radialGradient == null || radius != radialGradientRadius) {
            radialGradient = new RadialGradient(halfW, halfH, radius, outlineColor, kernelColor,
                    Shader.TileMode.CLAMP);
            kernelPaint.setShader(radialGradient);
            radialGradientRadius = radius;
        }

        canvas.drawCircle(halfW, halfH, radius, kernelPaint);

        if (recording) {
            final float outRadius = radius + customScale;
            outlinePaint.setAlpha(customAlpha);
            canvas.drawCircle(halfW, halfH, outRadius, outlinePaint);
            customAlpha -= alphaStep;
            customScale += scaleStep;
            ++step;

            if (step == 60) {
                resetDrawParams();
            } else {
                if (customAlpha < alphaLowerBound) {
                    customAlpha = alphaLowerBound;
                }
                if (customScale > radius) {
                    customScale = radius;
                }
            }
            postInvalidateDelayed(16);
        } else {
            resetDrawParams();
        }
    }

    private void resetDrawParams() {
        customAlpha = alphaUpperBound;
        customScale = 0.0F;
        step = 0;
        outlinePaint.setAlpha(customAlpha);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                kernelPaint.setColorFilter(colorFilter);
                postInvalidate();
                offsetX = event.getRawX();
                offsetY = event.getRawY();
                super.onTouchEvent(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                final float rawX = event.getRawX();
                final float rawY = event.getRawY();
                final int diffX = (int) (rawX - offsetX);
                final int diffY = (int) (rawY - offsetY);
                if (Math.abs(diffX) > touchSlop || Math.abs(diffY) > touchSlop) {
                    dragging = true;
                }
                if (dragging) {
                    moveTo(windowParams.x + diffX, windowParams.y + diffY).show();
                    offsetX = rawX;
                    offsetY = rawY;
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                kernelPaint.setColorFilter(null);
                postInvalidate();
                if (!dragging) {
                    super.onTouchEvent(event);
                }
                resetTouchEventParams();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void resetTouchEventParams() {
        offsetX = offsetY = 0.0F;
        dragging = false;
    }

    @Override
    public void onClick(View v) {
        recording = !recording;
        postInvalidate();
        if (l != null) {
            l.onRecordStateChanged(recording);
        }
    }

    public interface OnRecordStateListener {
        void onRecordStateChanged(boolean recording);
    }

    public RecordIndicator moveTo(int x, int y) {
        windowParams.x = x;
        windowParams.y = y;
        return this;
    }

    public RecordIndicator sizeTo(int w, int h) {
        windowParams.width = w;
        windowParams.height = h;
        return this;
    }

    public RecordIndicator show() {
        if (!isShown()) {
            windowManager.addView(this, windowParams);
        } else {
            windowManager.updateViewLayout(this, windowParams);
        }
        isGone = false;
        isDismiss = false;
        return this;
    }

    public RecordIndicator hide() {
        if (!isGone) {
            isGone = true;
            setVisibility(GONE);
        }
        return this;
    }

    public void dismiss() {
        if (!isDismiss) {
            isDismiss = true;
            windowManager.removeViewImmediate(this);
        }
    }

    public void smoothSlidingTo(int x, int y, long duration,
                                 TimeInterpolator interpolator, Animator.AnimatorListener listener) {
        final int xOffset = windowParams.x;
        final int yOffset = windowParams.y;
        final int xDiff = x - xOffset;
        final int yDiff = y - yOffset;
        final ValueAnimator animator = ValueAnimator.ofFloat(0.0F, 1.0F);
        animator.setDuration(duration);
        if (interpolator != null) {
            animator.setInterpolator(interpolator);
        }
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                final float fraction = animator.getAnimatedFraction();
                moveTo(xOffset + (int) (xDiff * fraction), yOffset + (int) (yDiff * fraction)).show();
            }
        });
        animator.start();
    }

}
