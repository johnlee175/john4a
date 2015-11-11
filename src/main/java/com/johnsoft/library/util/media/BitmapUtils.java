package com.johnsoft.library.util.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.view.View;

/**
 * @author John Kenrinus Lee
 * @version 2015-04-14
 */
public class BitmapUtils
{
    public static final class Switcher
    {
        public static byte[] bitmap2byte(Bitmap pBitmap, Bitmap.CompressFormat pCompressFormat)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pBitmap.compress(pCompressFormat, 100, baos);
            return baos.toByteArray();
        }

        public static Bitmap byte2bitmap(byte[] pBytes)
        {
            return BitmapFactory.decodeByteArray(pBytes, 0, pBytes.length);
        }

        public static Drawable bitmap2drawable(Resources pResources, Bitmap pBitmap)
        {
            return new BitmapDrawable(pResources, pBitmap);
        }

        public static Bitmap drawable2bitmapByCast(Drawable drawable)
        {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            return bd.getBitmap();
        }

        public static Bitmap drawable2bitmapByCanvas(Drawable drawable)
        {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable
                            .getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                            : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }

        public static Bitmap drawable2bitmapByTypeCheck(Drawable drawable)
        {
            if (drawable instanceof BitmapDrawable)
                return ((BitmapDrawable) drawable).getBitmap();
            else if (drawable instanceof NinePatchDrawable)
            {
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable
                                .getIntrinsicHeight(),
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                : Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                drawable.draw(canvas);
                return bitmap;
            }
            return null;
        }

        public static Bitmap view2bitmapByCanvas(View view, int bitmapWidth, int bitmapHeight)
        {
            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bitmap));
            return bitmap;
        }

        public static Bitmap view2bitmapByCache(View view)
        {
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            view.buildDrawingCache();
            return view.getDrawingCache();
        }
    }

    public static final class Matrixer
    {
        public static Bitmap rotateBitmap(Bitmap pBitmap, float pDegree)
        {
            Matrix matrix = new Matrix();
            matrix.postRotate(pDegree);
            return Bitmap.createBitmap(pBitmap, 0, 0, pBitmap.getWidth(), pBitmap.getHeight(), matrix, true);
        }

        public static Bitmap resizeBitmap(Bitmap pBitmap, float pScale)
        {
            Matrix matrix = new Matrix();
            matrix.postScale(pScale, pScale);
            return Bitmap.createBitmap(pBitmap, 0, 0, pBitmap.getWidth(), pBitmap.getHeight(), matrix, true);
        }

        public static Bitmap resizeBitmap(Bitmap pBitmap, int pW, int pH)
        {
            Bitmap bitmapOrigin = pBitmap;
            int width = bitmapOrigin.getWidth();
            int height = bitmapOrigin.getHeight();
            float scaleWidth = ((float)pW) / width;
            float scaleHeight = ((float)pH) / height;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            return Bitmap.createBitmap(bitmapOrigin, 0, 0, width, height, matrix, true);
        }

        public static Bitmap reverseBitmap(Bitmap pBitmap, boolean pHorizontal)
        {
            float[] floats = null;
            if (pHorizontal)
            { // 水平反转
                floats = new float[] {-1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f};
            }
            else
            { // 垂直反转
                floats = new float[] {1f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 1f};
            }
            if (floats != null)
            {
                Matrix matrix = new Matrix();
                matrix.setValues(floats);
                return Bitmap.createBitmap(pBitmap, 0, 0, pBitmap.getWidth(), pBitmap.getHeight(), matrix, true);
            }
            return pBitmap;
        }
    }

    public static final class Compressor
    {
        public static final Bitmap compressBitmap(Bitmap image, Bitmap.CompressFormat format, int kb)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int options = 100;//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            image.compress(format, options, baos);
            while ((baos.toByteArray().length / 1024.0f) > kb)
            {//循环判断如果压缩后图片是否大于kb值的KB,大于继续压缩
                baos.reset();
                options -= 10;
                if(options <= 0)
                {
                    image.compress(Bitmap.CompressFormat.JPEG, 0, baos);
                    break;
                }
                image.compress(Bitmap.CompressFormat.JPEG, options, baos);
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return BitmapFactory.decodeStream(bais, null, null);
        }

        public static final Bitmap compressBitmap(String srcPath, float ww, float hh)
        {
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;//只读边,不读内容
            Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);

            newOpts.inJustDecodeBounds = false;
            int w = newOpts.outWidth;
            int h = newOpts.outHeight;
            //现在主流手机比较多是800*480分辨率
            if(ww <= 0)
                ww = 480f;
            if(hh <= 0)
                hh = 800f;
            int be = 1;  //be=1表示不缩放
            if (w > h && w > ww) {  //如果宽度大的话根据宽度固定大小缩放
                be = (int) (newOpts.outWidth / ww);
            } else if (w < h && h > hh) {  //如果高度高的话根据宽度固定大小缩放
                be = (int) (newOpts.outHeight / hh);
            }
            if (be <= 0)
                be = 1;

            newOpts.inSampleSize = be;//设置采样率
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_4444;//该模式是默认的,可不设
            newOpts.inPurgeable = true;// 同时设置才会有效
            newOpts.inInputShareable = true;//当系统内存不够时候图片自动被回收
            //      newOpts.inTempStorage = new byte[16 * 1024];

            bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
            return bitmap;
        }
    }

    public static final class Filter
    {
        public static Bitmap filter(Bitmap pBitmap, int pHue, int pSaturation, int pBrightness)
        {
            final Bitmap bitmap = Bitmap.createBitmap(pBitmap.getWidth(), pBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            final Paint paint = new Paint();
            paint.setAntiAlias(true);
            final ColorMatrix allMatrix = new ColorMatrix();
            final ColorMatrix hueMatrix = new ColorMatrix();
            final ColorMatrix saturationMatrix = new ColorMatrix();
            final ColorMatrix lightnessMatrix = new ColorMatrix();
            final int middleValue = 127;
            float hueValue = pHue * 1.0F / middleValue;
            float saturationValue = pSaturation * 1.0F / middleValue;
            float lightnessValue = (pBrightness - middleValue) * 1.0F / middleValue * 180;
            // 色相值
            hueMatrix.setScale(hueValue, hueValue, hueValue, 1);
            // 饱和度值, 最小可设为0, 此时对应的是灰度图, 为1表示饱和度不变, 大于1就显示过饱和
            saturationMatrix.setSaturation(saturationValue);
            // 亮度值, 就是色轮旋转的角度, 正值表示顺时针旋转, 负值表示逆时针旋转
            lightnessMatrix.setRotate(0, lightnessValue); // 控制让红色区在色轮上旋转的角度
            lightnessMatrix.setRotate(1, lightnessValue); // 控制让绿红色区在色轮上旋转的角度
            lightnessMatrix.setRotate(2, lightnessValue); // 控制让蓝色区在色轮上旋转的角度
            allMatrix.postConcat(hueMatrix); // 效果叠加
            allMatrix.postConcat(saturationMatrix); // 效果叠加
            allMatrix.postConcat(lightnessMatrix); // 效果叠加
            paint.setColorFilter(new ColorMatrixColorFilter(allMatrix));
            canvas.drawBitmap(pBitmap, 0, 0, paint);
            return bitmap;
        }
    }

    /**
     * 待处理的图片不要放到drawable下, 而是放在assets下比较好, 因为drawable下分辨率会影响图片,
     * 而且稍加改进, 可以使用BitmapFactory#decodeStream代替Bitmap#createBitmap, 此类只是个参考
     */
    public static final class Renderer
    {
        /** 怀旧 */
        public static Bitmap oldRemeber(Bitmap pBitmap)
        {
            final int width = pBitmap.getWidth();
            final int height = pBitmap.getHeight();
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            int pxColor = 0;
            int pxR = 0, pxG = 0, pxB = 0;
            int newR = 0, newG = 0, newB = 0;
            int[] pixels = new int[width * height];
            pBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int i = 0; i < height; ++i)
            {
                for (int j = 0; j < width; ++j)
                {
                    pxColor = pixels[width * i + j];
                    pxR = Color.red(pxColor);
                    pxG = Color.green(pxColor);
                    pxB = Color.blue(pxColor);
                    newR = (int)(0.393 * pxR + 0.769 * pxG + 0.189 * pxB);
                    newG = (int)(0.349 * pxR + 0.686 * pxG + 0.168 * pxB);
                    newB = (int)(0.272 * pxR + 0.534 * pxG + 0.131 * pxB);
                    int newColor = Color.argb(255, newR > 255 ? 255 : newR, newG > 255 ? 255 : newG, newB > 255 ? 255 : newB);
                    pixels[width * i + j] = newColor;
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        }

        /** 柔化 */
        public static Bitmap blur(Bitmap pBitmap)
        {
            final int width = pBitmap.getWidth();
            final int height = pBitmap.getHeight();
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            int pxColor = 0;
            int newR = 0, newG = 0, newB = 0;
            int newColor = 0;
            int[][] colors = new int[9][3];
            for (int i = 1, length = width - 1; i < length; ++i)
            {
                for (int j = 1, len = height - 1; j < len; ++j)
                {
                    for (int m = 0; m < 9; ++m)
                    {
                        int s = 0, p = 0;
                        switch (m)
                        {
                            case 0:
                                s = i - 1;
                                p = j - 1;
                                break;
                            case 1:
                                s = i;
                                p = j - 1;
                                break;
                            case 2:
                                s = i + 1;
                                p = j - 1;
                                break;
                            case 3:
                                s = i + 1;
                                p = j;
                                break;
                            case 4:
                                s = i + 1;
                                p = j + 1;
                                break;
                            case 5:
                                s = i;
                                p = j + 1;
                                break;
                            case 6:
                                s = i - 1;
                                p = j + 1;
                                break;
                            case 7:
                                s = i - 1;
                                p = j;
                                break;
                            case 8:
                                s = i;
                                p = j;
                                break;
                        }
                        pxColor = pBitmap.getPixel(s, p);
                        colors[m][0] = Color.red(pxColor);
                        colors[m][1] = Color.green(pxColor);
                        colors[m][2] = Color.blue(pxColor);
                    }
                    for (int n = 0; n < 9; ++n)
                    {
                        newR += colors[n][0];
                        newG += colors[n][1];
                        newB += colors[n][2];
                    }
                    newR = (int)(newR / 9F);
                    newG = (int)(newG / 9F);
                    newB = (int)(newB / 9F);
                    newR = Math.min(255, Math.max(0, newR));
                    newG = Math.min(255, Math.max(0, newG));
                    newB = Math.min(255, Math.max(0, newB));
                    newColor = Color.argb(255, newR, newG, newB);
                    bitmap.setPixel(i, j, newColor);
                    newR = newG = newB = 0;
                }
            }
            return bitmap;
        }

        /** 高斯模糊 */
        public static Bitmap blurAmeliorate(Bitmap pBitmap)
        {
            final int[] gauss = new int[] {1, 2, 1, 2, 4, 2, 1, 2, 1}; // 高斯矩阵
            final int width = pBitmap.getWidth();
            final int height = pBitmap.getHeight();
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            int pxR = 0, pxG = 0, pxB = 0;
            int pxColor = 0;
            int newR = 0, newG = 0, newB = 0;
            int delta = 16; // 值越小图片会越亮，越大则越暗
            int idx = 0;
            int[] pixels = new int[width * height];
            pBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int i = 1, length = height - 1; i < length; ++i)
            {
                for (int j = 1, len = width - 1; j < len; ++j)
                {
                    idx = 0;
                    for (int m = -1; m <= 1; ++m)
                    {
                        for (int n = -1; n <= 1; ++n)
                        {
                            pxColor = pixels[(i + m) * width + j + n];
                            pxR = Color.red(pxColor);
                            pxG = Color.green(pxColor);
                            pxB = Color.blue(pxColor);
                            newR += pxR * gauss[idx];
                            newG += pxG * gauss[idx];
                            newB += pxB * gauss[idx];
                            idx++;
                        }
                    }
                    newR /= delta;
                    newG /= delta;
                    newB /= delta;
                    newR = Math.min(255, Math.max(0, newR));
                    newG = Math.min(255, Math.max(0, newG));
                    newB = Math.min(255, Math.max(0, newB));
                    pixels[i * width + j] = Color.argb(255, newR, newG, newB);
                    newR = newG = newB = 0;
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        }

        /** 锐化 */
        private Bitmap sharpenAmeliorate(Bitmap pBitmap)
        {
            final int[] laplacian = new int[] {-1, -1, -1, -1, 9, -1, -1, -1, -1}; // 拉普拉斯矩阵
            final int width = pBitmap.getWidth();
            final int height = pBitmap.getHeight();
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            int pxR = 0, pxG = 0, pxB = 0;
            int pxColor = 0;
            int newR = 0, newG = 0, newB = 0;
            int idx = 0;
            float alpha = 0.3F;
            int[] pixels = new int[width * height];
            pBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int i = 1, length = height - 1; i < length; ++i)
            {
                for (int j = 1, len = width - 1; j < len; ++j)
                {
                    idx = 0;
                    for (int m = -1; m <= 1; ++m)
                    {
                        for (int n = -1; n <= 1; ++n)
                        {
                            pxColor = pixels[(i + n) * width + j + m];
                            pxR = Color.red(pxColor);
                            pxG = Color.green(pxColor);
                            pxB = Color.blue(pxColor);
                            newR += (int)(pxR * laplacian[idx] * alpha);
                            newG += (int)(pxG * laplacian[idx] * alpha);
                            newB += (int)(pxB * laplacian[idx] * alpha);
                            ++idx;
                        }
                    }
                    newR = Math.min(255, Math.max(0, newR));
                    newG = Math.min(255, Math.max(0, newG));
                    newB = Math.min(255, Math.max(0, newB));
                    pixels[i * width + j] = Color.argb(255, newR, newG, newB);
                    newR = newG = newB = 0;
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        }

        /** 浮雕 */
        public static Bitmap emboss(Bitmap pBitmap)
        {
            final int width = pBitmap.getWidth();
            final int height = pBitmap.getHeight();
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            int pxR = 0, pxG = 0, pxB = 0;
            int pxColor = 0;
            int newR = 0, newG = 0, newB = 0;
            int[] pixels = new int[width * height];
            pBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            int pos = 0;
            for (int i = 1, length = height - 1; i < length; ++i)
            {
                for (int j = 1, len = width - 1; j < len; ++j)
                {
                    pos = i * width + j;
                    pxColor = pixels[pos];
                    pxR = Color.red(pxColor);
                    pxG = Color.green(pxColor);
                    pxB = Color.blue(pxColor);
                    pxColor = pixels[pos + 1];
                    newR = Color.red(pxColor) - pxR + 127;
                    newG = Color.green(pxColor) - pxG + 127;
                    newB = Color.blue(pxColor) - pxB + 127;
                    newR = Math.min(255, Math.max(0, newR));
                    newG = Math.min(255, Math.max(0, newG));
                    newB = Math.min(255, Math.max(0, newB));
                    pixels[pos] = Color.argb(255, newR, newG, newB);
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        }

        /** 底片 */
        public static Bitmap film(Bitmap pBitmap)
        {
            final int maxValue = 255;
            final int width = pBitmap.getWidth();
            final int height = pBitmap.getHeight();
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            int pxR = 0, pxG = 0, pxB = 0;
            int pxColor = 0;
            int newR = 0, newG = 0, newB = 0;
            int[] pixels = new int[width * height];
            pBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            int pos = 0;
            for (int i = 1, length = height - 1; i < length; ++i)
            {
                for (int j = 1, len = width - 1; j < len; ++j)
                {
                    pos = i * width + j;
                    pxColor = pixels[pos];
                    pxR = Color.red(pxColor);
                    pxG = Color.green(pxColor);
                    pxB = Color.blue(pxColor);
                    newR = maxValue - pxR;
                    newG = maxValue - pxG;
                    newB = maxValue - pxB;
                    newR = Math.min(maxValue, Math.max(0, newR));
                    newG = Math.min(maxValue, Math.max(0, newG));
                    newB = Math.min(maxValue, Math.max(0, newB));
                    pixels[pos] = Color.argb(maxValue, newR, newG, newB);
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        }

        /** 光照 */
        public static Bitmap sunshine(Bitmap pBitmap)
        {
            final double strength = 150.0; // 光照强度100-150
            final int width = pBitmap.getWidth();
            final int height = pBitmap.getHeight();
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            int pxR = 0, pxG = 0, pxB = 0;
            int pxColor = 0;
            int newR = 0, newG = 0, newB = 0;
            int centerX = width >>> 2, centerY = height >>> 2;
            int radius = Math.min(centerX, centerY);
            int pos = 0;
            int[] pixels = new int[width * height];
            pBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int i = 1, length = height - 1; i < length; ++i)
            {
                for (int j = 1, len = width - 1; j < len; ++j)
                {
                    pos = i * width + j;
                    pxColor = pixels[pos];
                    pxR = Color.red(pxColor);
                    pxG = Color.green(pxColor);
                    pxB = Color.blue(pxColor);
                    newR = pxR;
                    newG = pxG;
                    newB = pxB;
                    //计算当前点到光照中心的距离, 平面坐标系中求两点之间的距离
                    int distance = (int)(Math.pow((centerY - i), 2) + Math.pow((centerX - j), 2));
                    if (distance < (radius * radius))
                    { //按照距离大小计算增加的光照值
                        int result = (int)(strength * (1.0 - Math.sqrt(distance) / radius));
                        newR = pxR + result;
                        newG = pxG + result;
                        newB = pxB + result;
                    }
                    newR = Math.min(255, Math.max(0, newR));
                    newG = Math.min(255, Math.max(0, newG));
                    newB = Math.min(255, Math.max(0, newB));
                    pixels[pos] = Color.argb(255, newR, newG, newB);
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        }

        /** 光晕 */
        public static Bitmap halo(Bitmap pBitmap, int pX, int pY, float pRadius)
        {
            final int[] gauss = new int[] {1, 2, 1, 2, 4, 2, 1, 2, 1}; // 高斯矩阵
            final int width = pBitmap.getWidth();
            final int height = pBitmap.getHeight();
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            int pxR = 0, pxG = 0, pxB = 0;
            int pxColor = 0;
            int newR = 0, newG = 0, newB = 0;
            int delta = 18; // 值越小图片会越亮，越大则越暗
            int idx = 0;
            int[] pixels = new int[width * height];
            pBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int i = 1, length = height - 1; i < length; ++i)
            {
                for (int j = 1, len = width - 1; j < len; ++j)
                {
                    idx = 0;
                    int distance = (int)(Math.pow(j - pX, 2) + Math.pow(i - pY, 2));
                    // 不是中心区域的点做模糊处理
                    if (distance > pRadius * pRadius)
                    {
                        for (int m = -1; m <= 1; ++m)
                        {
                            for (int n = -1; n <= 1; ++n)
                            {
                                pxColor = pixels[(i + m) * width + j + n];
                                pxR = Color.red(pxColor);
                                pxG = Color.green(pxColor);
                                pxB = Color.blue(pxColor);
                                newR += pxR * gauss[idx];
                                newG += pxG * gauss[idx];
                                newB += pxB * gauss[idx];
                                idx++;
                            }
                        }
                        newR /= delta;
                        newG /= delta;
                        newB /= delta;
                        newR = Math.min(255, Math.max(0, newR));
                        newG = Math.min(255, Math.max(0, newG));
                        newB = Math.min(255, Math.max(0, newB));
                        pixels[i * width + j] = Color.argb(255, newR, newG, newB);
                        newR = newG = newB = 0;
                    }
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        }
    }

    public static final class Chore
    {
        /**
         * Returns a Bitmap representing the thumbnail of the specified Bitmap. The
         * size of the thumbnail is defined by the dimension
         * android.R.dimen.launcher_application_icon_size.
         *
         * This method is not thread-safe and should be invoked on the UI thread
         * only.
         *
         * @param bitmap
         *            The bitmap to get a thumbnail of.
         * @param context
         *            The application's context.
         *
         * @return A thumbnail for the specified bitmap or the bitmap itself if the
         *         thumbnail could not be created.
         */
        public static Bitmap createBitmapThumbnail(Bitmap bitmap, Context context, Canvas canvas,
                                                   Paint paint)
        {
            int sIconWidth, sIconHeight;
            sIconWidth = sIconHeight = (int) context.getResources().getDimension(
                    android.R.dimen.app_icon_size);

            int width = sIconWidth, height = sIconHeight;

            final int bitmapWidth = bitmap.getWidth();
            final int bitmapHeight = bitmap.getHeight();

            if (width > 0 && height > 0)
            {
                if (width < bitmapWidth || height < bitmapHeight)
                {
                    final float ratio = (float) bitmapWidth / bitmapHeight;

                    if (bitmapWidth > bitmapHeight)
                    {
                        height = (int) (width / ratio);
                    }
                    else if (bitmapHeight > bitmapWidth)
                    {
                        width = (int) (height * ratio);
                    }

                    final Bitmap.Config c = (width == sIconWidth && height == sIconHeight) ? bitmap
                            .getConfig() : Bitmap.Config.ARGB_8888;
                    final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                    canvas.setBitmap(thumb);
                    paint.setDither(false);
                    paint.setFilterBitmap(true);
                    Rect sBounds = new Rect(), sOldBounds = new Rect();
                    sBounds.set((sIconWidth - width) / 2, (sIconHeight - height) / 2, width, height);
                    sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
                    canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);
                    return thumb;
                }
                else if (bitmapWidth < width || bitmapHeight < height)
                {
                    final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                    final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                    canvas.setBitmap(thumb);
                    paint.setDither(false);
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(bitmap, (sIconWidth - bitmapWidth) / 2,
                            (sIconHeight - bitmapHeight) / 2, paint);
                    return thumb;
                }
            }

            return bitmap;
        }

        public static final int TRANS_NONE = -1;
        public static final int TRANS_MIRROR = 0;
        public static final int TRANS_ROT90 = 1;
        public static final int TRANS_ROT180 = 2;
        public static final int TRANS_ROT270 = 4;
        public static final int TRANS_MIRROR_ROT90 = 8;
        public static final int TRANS_MIRROR_ROT180 = 16;
        public static final int TRANS_MIRROR_ROT270 = 32;
        public static final int VCENTER = 1;
        public static final int HCENTER = -1;
        public static final int BOTTOM = 0;
        public static final int RIGHT = 2;

        public void drawRegion(Bitmap image_src, int x_src, int y_src, int width, int height,
                               int transform, int x_dest, int y_dest, int anchor, Canvas canvas, Paint mPaint)
        {
            if ((anchor & VCENTER) != 0)
            {
                y_dest -= height / 2;
            }
            else if ((anchor & BOTTOM) != 0)
            {
                y_dest -= height;
            }
            if ((anchor & RIGHT) != 0)
            {
                x_dest -= width;
            }
            else if ((anchor & HCENTER) != 0)
            {
                x_dest -= width / 2;
            }
            Bitmap newMap = Bitmap.createBitmap(image_src, x_src, y_src, width, height);
            Matrix mMatrix = new Matrix();
            Matrix temp = new Matrix();
            float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1 };
            temp.setValues(mirrorY);

            switch (transform)
            {
                case TRANS_NONE:
                    break;
                case TRANS_ROT90:
                    mMatrix.setRotate(90, width / 2, height / 2);
                    break;
                case TRANS_ROT180:
                    mMatrix.setRotate(180, width / 2, height / 2);
                    break;
                case TRANS_ROT270:
                    mMatrix.setRotate(270, width / 2, height / 2);
                    break;
                case TRANS_MIRROR:
                    mMatrix.postConcat(temp);
                    break;
                case TRANS_MIRROR_ROT90:
                    mMatrix.postConcat(temp);
                    mMatrix.setRotate(90, width / 2, height / 2);
                    break;
                case TRANS_MIRROR_ROT180:
                    mMatrix.postConcat(temp);
                    mMatrix.setRotate(180, width / 2, height / 2);
                    break;
                case TRANS_MIRROR_ROT270:
                    mMatrix.postConcat(temp);
                    mMatrix.setRotate(270, width / 2, height / 2);
                    break;
            }
            mMatrix.setTranslate(x_dest, y_dest);
            canvas.drawBitmap(newMap, mMatrix, mPaint);
        }
    }
}
