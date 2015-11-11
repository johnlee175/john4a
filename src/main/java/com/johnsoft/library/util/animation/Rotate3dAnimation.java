package com.johnsoft.library.util.animation;

import com.johnsoft.library.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public abstract class Rotate3dAnimation extends Animation
{
    private Camera mCamera;

    private float mFromDegrees;
    private float mToDegrees;
    private int mPivotXType;
    private int mPivotYType;
    private float mPivotXValue;
    private float mPivotYValue;
    private float mPivotX;
    private float mPivotY;
    private float mCameraDistance;
    // 是否需要扭曲
    private boolean mReverse;

    public Rotate3dAnimation(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Rotate3dAnimation);

        mFromDegrees = a.getFloat(R.styleable.Rotate3dAnimation_fromDegrees, 0.0f);
        mToDegrees = a.getFloat(R.styleable.Rotate3dAnimation_toDegrees, 0.0f);

        Description d = Description.parseValue(a.peekValue(R.styleable.Rotate3dAnimation_pivotX));
        mPivotXType = d.type;
        mPivotXValue = d.value;

        d = Description.parseValue(a.peekValue(R.styleable.Rotate3dAnimation_pivotY));
        mPivotYType = d.type;
        mPivotYValue = d.value;
        
        mCameraDistance = a.getFloat(R.styleable.Rotate3dAnimation_cameraDistance, 0.0f);
        mReverse = a.getBoolean(R.styleable.Rotate3dAnimation_reverse, false);

        a.recycle();

        if (mPivotXType == ABSOLUTE)
        {
            mPivotX = mPivotXValue;
        }
        if (mPivotYType == ABSOLUTE)
        {
            mPivotY = mPivotYValue;
        }
    }

    public Rotate3dAnimation(float fromDegrees, float toDegrees, float pivotX, float pivotY, float cameraDistance, boolean reverse)
    {
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;
        mPivotXType = ABSOLUTE;
        mPivotYType = ABSOLUTE;
        mPivotX = mPivotXValue = pivotX;
        mPivotY = mPivotYValue = pivotY;
        mCameraDistance = cameraDistance;
        mReverse = reverse;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        super.initialize(width, height, parentWidth, parentHeight);
        mPivotX = resolveSize(mPivotXType, mPivotXValue, width, parentWidth);
        mPivotY = resolveSize(mPivotYType, mPivotYValue, height, parentHeight);
        mCamera = new Camera();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t)
    {
        final float fromDegrees = mFromDegrees, toDegrees = mToDegrees;
        final float degrees = fromDegrees + ((toDegrees - fromDegrees) * interpolatedTime);
        final Camera camera = mCamera;
        final Matrix matrix = t.getMatrix();
        camera.save();
        if (mReverse)
        {
            camera.translate(0.0f, 0.0f, mCameraDistance * interpolatedTime);
        } else
        {
            camera.translate(0.0f, 0.0f, mCameraDistance * (1.0f - interpolatedTime));
        }
        doRotate(camera, degrees);
        camera.getMatrix(matrix);
        camera.restore();
        
        final float scale = getScaleFactor();
//        final float centerX = mPivotX;
//        final float centerY = mPivotY;
        final float centerX = mPivotX * scale;
        final float centerY = mPivotY * scale;
        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);
    }
    
    protected abstract void doRotate(Camera camera, float degrees);

    protected static class Description
    {

        public int type;

        public float value;

        static Description parseValue(TypedValue value)
        {
            Description d = new Description();
            if (value == null)
            {
                d.type = ABSOLUTE;
                d.value = 0;
            } else
            {
                if (value.type == TypedValue.TYPE_FRACTION)
                {
                    d.type = (value.data & TypedValue.COMPLEX_UNIT_MASK) == TypedValue.COMPLEX_UNIT_FRACTION_PARENT ? RELATIVE_TO_PARENT : RELATIVE_TO_SELF;
                    d.value = TypedValue.complexToFloat(value.data);
                    return d;
                } else if (value.type == TypedValue.TYPE_FLOAT)
                {
                    d.type = ABSOLUTE;
                    d.value = value.getFloat();
                    return d;
                } else if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT)
                {
                    d.type = ABSOLUTE;
                    d.value = value.data;
                    return d;
                }
            }

            d.type = ABSOLUTE;
            d.value = 0.0f;

            return d;
        }
    }
}