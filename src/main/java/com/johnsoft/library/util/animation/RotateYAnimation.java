package com.johnsoft.library.util.animation;

import android.content.Context;
import android.graphics.Camera;
import android.util.AttributeSet;

public class RotateYAnimation extends Rotate3dAnimation
{
    public RotateYAnimation(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public RotateYAnimation(float fromDegrees, float toDegrees, float pivotX, float pivotY, float cameraDistance, boolean reverse)
    {
        super(fromDegrees, toDegrees, pivotX, pivotY, cameraDistance, reverse);
    }

    @Override
    protected void doRotate(Camera camera, float degrees)
    {
        camera.rotateY(degrees);
    }
}