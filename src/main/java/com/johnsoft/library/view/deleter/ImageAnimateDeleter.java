package com.johnsoft.library.view.deleter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;

public abstract class ImageAnimateDeleter
{
    protected Context context;

    protected WindowManager windowManager;

    protected Handler handler;

    protected Paint paint;

    protected Bitmap sourceBitmap;

    protected int x, y, w, h;
    
    protected int delayBeforeHide;

    protected OnShownListener shownListener;

    protected OnHiddenListener hiddenListener;

    protected MiddleAnimation middleAnimation;

    public ImageAnimateDeleter(Context context)
    {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        this.paint = new Paint();
    }

    public final ImageAnimateDeleter setSource(Bitmap bitmap, int x, int y, int w, int h)
    {
        this.sourceBitmap = bitmap;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        return this;
    }
    
    public final ImageAnimateDeleter setMiddleAnimation(MiddleAnimation animation)
    {
        this.middleAnimation = animation;
        return this;
    }

    public final ImageAnimateDeleter setShownListener(OnShownListener shownListener)
    {
        this.shownListener = shownListener;
        return this;
    }

    public final ImageAnimateDeleter setHiddenListener(OnHiddenListener hiddenListener)
    {
        this.hiddenListener = hiddenListener;
        return this;
    }

    protected final WindowManager.LayoutParams newParams()
    {
        return newParams(x, y, w, h);
    }

    protected final WindowManager.LayoutParams newParams(int x, int y, int w, int h)
    {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h, x, y, WindowManager.LayoutParams.TYPE_APPLICATION, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGBA_8888);
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        return lp;
    }
    
    public final void postHide()
    {
        if(delayBeforeHide <= 0)
        {
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    hide();
                }
            });
        }
        else
        {
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    hide();
                }
            }, delayBeforeHide);
        }
    }

    public final void show()
    {
        if(middleAnimation != null)
            middleAnimation.setDeleterParams(this);
        if (shownListener != null)
            shownListener.beforeShown();
        int time = doShow();
        if(time == 0)
        {
            afterShow();
        }else{
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    afterShow();
                }
            }, time);
        }
    }

    private final void afterShow()
    {
        if (shownListener != null)
            shownListener.afterShown();
        if(middleAnimation != null)
            delayBeforeHide += middleAnimation.getDelayForAnimation();
            postHide();
    }

    /**
     * should use postHide instead
     */
    @Deprecated
    public final void hide()
    {
        if (hiddenListener != null)
            hiddenListener.beforeHidden();
        doHide();
        if (hiddenListener != null)
            hiddenListener.afterHidden();
        if(middleAnimation != null)
            middleAnimation.clearDeleterParams();
    }

    protected abstract int doShow();

    protected abstract void doHide();
    
    protected static abstract class MiddleAnimation
    {
        protected int x, y, w, h;
        protected ImageAnimateDeleter deleter;
        protected Context context;
        protected WindowManager windowManager;
        protected Handler handler;
        protected Bitmap sourceBitmap;
        
        private void setDeleterParams(ImageAnimateDeleter deleter)
        {
            this.deleter = deleter;
            this.x = deleter.x;
            this.y = deleter.y;
            this.w = deleter.w;
            this.h = deleter.h;
            this.context = deleter.context;
            this.windowManager = deleter.windowManager;
            this.handler = deleter.handler;
            this.sourceBitmap = deleter.sourceBitmap;
        }
        
        protected abstract int getDelayForAnimation();
        
        private void clearDeleterParams()
        {
            this.context = null;
            this.windowManager = null;
            this.handler = null;
            this.sourceBitmap = null;
        }
    }
    
    public interface OnShownListener
    {
        public void beforeShown();

        public void afterShown();
    }

    public interface OnHiddenListener
    {
        public void beforeHidden();

        public void afterHidden();
    }
}
