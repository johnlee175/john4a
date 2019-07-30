package com.johnsoft.library.view.deleter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.johnsoft.library.R;

public class SMiddleAnimationN extends ImageAnimateDeleter.MiddleAnimation
{
    private SView view;
    
    @Override
    protected int getDelayForAnimation()
    {
        int duration = 300;
        view = new SView(context);
        windowManager.addView(view, deleter.newParams());
        
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "downY", 0, h);
        animator.setDuration(duration - 50);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if(view != null)
                {
                    view.setVisibility(View.GONE);
                    if(windowManager != null)
                    {
                        windowManager.removeView(view);
                    }
                }
            }
        });
        animator.start();
        return duration;
    }
    
    private class SView extends View
    {
        private Paint mPaint;
        private Bitmap bitmap;
        private Rect srcRect;
        private Rect dstRect;
        private RectF maskRect;

        public SView(Context context)
        {
            super(context);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            bitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.ss)).getBitmap();
            srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            dstRect = new Rect(0, 0, w, h);
            maskRect = new RectF(0, 0, w, 0);
        }
        
        public void setDownY(float downY)
        {
            maskRect.bottom = downY;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            canvas.save();
            canvas.clipRect(maskRect);
            canvas.drawBitmap(bitmap, srcRect, dstRect, mPaint);
            canvas.restore();
        }
    }
}
