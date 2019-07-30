package com.johnsoft.library.view.deleter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import com.johnsoft.library.R;

import java.util.Random;

public class TwoSImageAnimateDeleterN extends ImageAnimateDeleter
{
    private ImageView iv;
    private ImageView iv_l;
    private ImageView iv_r;
    private WindowManager.LayoutParams lp_l;
    private WindowManager.LayoutParams lp_r;

    private Random random;
    
    private boolean shake;
    
    public TwoSImageAnimateDeleterN(Context context)
    {
        super(context);

        iv_l = new ImageView(context);
        iv_r = new ImageView(context);

        random = new Random();
    }
    
    public TwoSImageAnimateDeleterN setShake(boolean shake)
    {
        this.shake = shake;
        return this;
    }

    @Override
    protected void doHide()
    {
        windowManager.removeView(iv_l);
        windowManager.removeView(iv_r);
    }

    @Override
    protected int doShow()
    {
        if(shake)
        {
            final int duration = 450, swing = 4, count = 10;
            iv = new ImageView(context);
            iv.setImageBitmap(sourceBitmap);
            windowManager.addView(iv, newParams(x, y - swing, w, h + swing * 2));
            ObjectAnimator oa = ObjectAnimator.ofFloat(iv, "translationY", -swing, swing);
            oa.setRepeatCount(count);
            oa.setRepeatMode(ObjectAnimator.REVERSE);
            oa.setDuration((duration - 50) / count);
            oa.setInterpolator(new AccelerateInterpolator());
            oa.addListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    directShow();
                    if(iv != null)
                    {
                        iv.setVisibility(View.GONE);
                        if(windowManager != null)
                        {
                            windowManager.removeView(iv);
                        }
                    }
                }
            });
            oa.start();
            return duration;
        }else{
            directShow();
            return 0;
        }
    }
    
    private void directShow()
    {
        int w2 = w / 2;
        int w4 = w2 / 2;
        int[] array = createRandomArray(h, w4, w2 + w4);
        int size = array.length;
        Bitmap b = Bitmap.createBitmap(sourceBitmap);

        Bitmap tl = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas cl = new Canvas(tl);
        Paint ptl = new Paint();
        ptl.setStyle(Style.FILL);
        ptl.setColor(Color.RED);
        ptl.setAntiAlias(true);
        Path pl = new Path();
        pl.moveTo(0, 0);
        pl.lineTo(w2, 0);
        for (int i = 0; i != size; i += 2)
        {
            pl.lineTo(array[i], array[i + 1]);
        }
        pl.lineTo(w2, h);
        pl.lineTo(0, h);
        pl.close();
        cl.drawPath(pl, ptl);
        ptl.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        cl.drawBitmap(b, 0, 0, ptl);

        Bitmap tr = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas cr = new Canvas(tr);
        Paint ptr = new Paint();
        ptr.setStyle(Style.FILL);
        ptr.setColor(Color.RED);
        ptr.setAntiAlias(true);
        Path pr = new Path();
        pr.moveTo(w, 0);
        pr.lineTo(w2, 0);
        for (int i = 0; i != size; i += 2)
        {
            pr.lineTo(array[i], array[i + 1]);
        }
        pr.lineTo(w2, h);
        pr.lineTo(w, h);
        pr.close();
        cr.drawPath(pr, ptr);
        ptr.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        cr.drawBitmap(b, 0, 0, ptr);

        iv_l.setImageBitmap(tl);
        iv_r.setImageBitmap(tr);
        
        lp_l = newParams();
        lp_r = newParams();
        lp_l.windowAnimations = R.anim.n_two_s_image_animate_deleter_l;
        lp_r.windowAnimations = R.anim.n_two_s_image_animate_deleter_r;
        
        windowManager.addView(iv_l, lp_l);
        windowManager.addView(iv_r, lp_r);
    }

    private int[] createRandomArray(int h, int lw, int rw)
    {
        int x = random(2, 8);
        int size = x << 1;
        int[] array = new int[size];
        int k = h / x, vs = 0, ve = k;
        for (int i = 0; i != size; i += 2)
        {
            array[i] = random(lw, rw);
            array[i + 1] = random(vs, ve);
            vs += k;
            ve += k;
        }
        return array;
    }

    private int random(int min, int max)
    {
        int x;
        while (true)
        {
            if ((x = random.nextInt(max)) > min)
                break;
        }
        return x;
    }
}
