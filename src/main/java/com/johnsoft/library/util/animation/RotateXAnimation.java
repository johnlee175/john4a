package com.johnsoft.library.util.animation;

import android.content.Context;
import android.graphics.Camera;
import android.util.AttributeSet;

public class RotateXAnimation extends Rotate3dAnimation
{
    public RotateXAnimation(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public RotateXAnimation(float fromDegrees, float toDegrees, float pivotX, float pivotY, float cameraDistance, boolean reverse)
    {
        super(fromDegrees, toDegrees, pivotX, pivotY, cameraDistance, reverse);
    }

    @Override
    protected void doRotate(Camera camera, float degrees)
    {
        camera.rotateX(degrees);
    }
}