package com.johnsoft.library.util.animation;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

/**
 * Frame animation utilities.
 * It can avoid OOM error and animate more smoothly to using ImageView load AnimationDrawable;
 *
 * @author John Kenrinus Lee
 * @version 2015-08-07
 */
public final class FramesAnimationUtils {
    private static final class Frame {
        byte[] bytes;
        int duration;
        Drawable drawable;
        boolean isReady = false;
    }

    private interface OnDrawableLoadedListener {
        void onDrawableLoaded(List<Frame> frames);
    }

    private static byte[] streamToBytes(final InputStream inputStream) {
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = null;
        try {
            bis = new BufferedInputStream(inputStream);
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = bis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    /* close silently */
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                   /* close silently */
                }
            }
        }
    }

    private static void loadFromXml(final int resourceId, final Context context,
                                    final OnDrawableLoadedListener onDrawableLoadedListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<Frame> frames = new ArrayList<>();
                final XmlResourceParser parser = context.getResources().getXml(resourceId);
                try {
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_DOCUMENT) {

                        } else if (eventType == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("item")) {
                                byte[] bytes = null;
                                int duration = 1000;
                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    if (parser.getAttributeName(i).equals("drawable")) {
                                        int resId = Integer.parseInt(parser.getAttributeValue(i).substring(1));
                                        bytes = streamToBytes(context.getResources().openRawResource(resId));
                                    } else if (parser.getAttributeName(i).equals("duration")) {
                                        duration = parser.getAttributeIntValue(i, 1000);
                                    }
                                }
                                final Frame frame = new Frame();
                                frame.bytes = bytes;
                                frame.duration = duration;
                                frames.add(frame);
                            }
                        } else if (eventType == XmlPullParser.END_TAG) {

                        } else if (eventType == XmlPullParser.TEXT) {

                        }
                        eventType = parser.next();
                    }
                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();
                }
                // Run on UI Thread
                new Handler(context.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (onDrawableLoadedListener != null) {
                            onDrawableLoadedListener.onDrawableLoaded(frames);
                        }
                    }
                });
            }
        }).run();
    }

    private static void animateRawManually(final List<Frame> frames, final ImageView imageView,
                                           final Runnable onComplete, final int frameNumber) {
        final Frame thisFrame = frames.get(frameNumber);
        if (frameNumber == 0) {
            thisFrame.drawable = new BitmapDrawable(imageView.getContext().getResources(), BitmapFactory
                    .decodeByteArray(thisFrame.bytes, 0, thisFrame.bytes.length));
        } else {
            final Frame previousFrame = frames.get(frameNumber - 1);
            ((BitmapDrawable) previousFrame.drawable).getBitmap().recycle();
            previousFrame.drawable = null;
            previousFrame.isReady = false;
        }
        imageView.setImageDrawable(thisFrame.drawable);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Make sure ImageView hasn't been changed to a different Image in this time
                if (imageView.getDrawable() == thisFrame.drawable) {
                    if (frameNumber + 1 < frames.size()) {
                        final Frame nextFrame = frames.get(frameNumber + 1);
                        if (nextFrame.isReady) {
                            // Animate next frame
                            animateRawManually(frames, imageView, onComplete, frameNumber + 1);
                        } else {
                            nextFrame.isReady = true;
                        }
                    } else {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                }
            }
        }, thisFrame.duration);
        // Load next frame
        if (frameNumber + 1 < frames.size()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Frame nextFrame = frames.get(frameNumber + 1);
                    nextFrame.drawable = new BitmapDrawable(imageView.getContext().getResources(),
                            BitmapFactory.decodeByteArray(nextFrame.bytes, 0, nextFrame.bytes.length));
                    if (nextFrame.isReady) {
                        // Animate next frame
                        animateRawManually(frames, imageView, onComplete, frameNumber + 1);
                    } else {
                        nextFrame.isReady = true;
                    }
                }
            }).run();
        }
    }

    private static void animateDrawableManually(final AnimationDrawable animationDrawable, final ImageView imageView,
                                                final Runnable onComplete, final int frameNumber) {
        final Drawable frame = animationDrawable.getFrame(frameNumber);
        imageView.setImageDrawable(frame);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Make sure ImageView hasn't been changed to a different Image in this time
                if (imageView.getDrawable() == frame) {
                    if (frameNumber + 1 < animationDrawable.getNumberOfFrames()) {
                        // Animate next frame
                        animateDrawableManually(animationDrawable, imageView, onComplete, frameNumber + 1);
                    } else {
                        // Animation complete
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                }
            }
        }, animationDrawable.getDuration(frameNumber));
    }

    /**
     * start animate by load raw xml manually
     * @param resourceId the animation-list drawable resource id
     * @param imageView the image view which show animation drawable
     * @param onStart execute before animate start
     * @param onComplete execute after animate complete
     */
    public static void animateRawManually(final int resourceId, final ImageView imageView, final Runnable onStart,
                                          final Runnable onComplete) {
        loadFromXml(resourceId, imageView.getContext(), new OnDrawableLoadedListener() {
            @Override
            public void onDrawableLoaded(List<Frame> frames) {
                if (onStart != null) {
                    onStart.run();
                }
                animateRawManually(frames, imageView, onComplete, 0);
            }
        });
    }

    /**
     * start animate by android.graphics.drawable.AnimationDrawable manually
     * @param resourceId the animation-list drawable resource id
     * @param imageView the image view which show animation drawable
     * @param onStart execute before animate start
     * @param onComplete execute after animate complete
     */
    public static void animateDrawableManually(final int resourceId, final ImageView imageView,
                                               final Runnable onStart, final Runnable onComplete) {
        final AnimationDrawable animationDrawable = new AnimationDrawable();
        final Resources resources = imageView.getContext().getResources();
        final XmlResourceParser parser = resources.getXml(resourceId);
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {

                } else if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("item")) {
                        Drawable drawable = null;
                        int duration = 1000;
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("drawable")) {
                                int resId = Integer.parseInt(parser.getAttributeValue(i).substring(1));
                                byte[] bytes = streamToBytes(resources.openRawResource(resId));
                                if (bytes != null) {
                                    drawable = new BitmapDrawable(resources,
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                                }
                            } else if (parser.getAttributeName(i).equals("duration")) {
                                duration = parser.getAttributeIntValue(i, 66);
                            }
                        }
                        if (drawable != null) {
                            animationDrawable.addFrame(drawable, duration);
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {

                } else if (eventType == XmlPullParser.TEXT) {

                }
                eventType = parser.next();
            }
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        if (onStart != null) {
            onStart.run();
        }
        animateDrawableManually(animationDrawable, imageView, onComplete, 0);
    }
}