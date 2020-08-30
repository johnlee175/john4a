/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.library.util.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Make sure android:layout_height != "wrap_content"
 *
 * @author John Kenrinus Lee
 * @version 2016-12-09
 */
public class SimpleVFlowLayout extends ViewGroup {

    public SimpleVFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        final int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        final int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        final int count = getChildCount();

        int width = 0, lineWidth = 0, childWidth;
        int height = 0, lineHeight = 0, childHeight;
        View child;
        MarginLayoutParams lp;
        for (int i = 0; i < count; ++i) {
            child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            lp = (MarginLayoutParams) child.getLayoutParams();
            childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            if (lineHeight + childHeight > sizeHeight) {
                width += lineWidth;
                lineWidth = childWidth;
                height = Math.max(lineHeight, childHeight);
                lineHeight = childHeight;
            } else {
                lineHeight += childHeight;
                lineWidth = Math.max(lineWidth, childWidth);
            }
            if (i == count - 1) {
                width += lineWidth;
                height = Math.max(lineHeight, childHeight);
            }
        }

        setMeasuredDimension(
                //                (modeWidth == MeasureSpec.EXACTLY) ? sizeWidth : width,
                Math.max(sizeWidth, width),
                (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight : height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        final int height = b - t;
        int maxLineWidth = 0;
        int left = 0, top = 0;
        int w, h, lc, tc, rc, bc;
        View child;
        MarginLayoutParams lp;
        for (int i = 0; i < count; ++i) {
            child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            lp = (MarginLayoutParams) child.getLayoutParams();
            w = child.getMeasuredWidth();
            h = child.getMeasuredHeight();

            lc = left + lp.leftMargin;
            tc = top + lp.topMargin;
            rc = lc + w;
            bc = tc + h;
            top = bc + lp.bottomMargin;
            if (top < height) {
                child.layout(lc, tc, rc, bc);
                maxLineWidth = Math.max(lp.leftMargin + w + lp.rightMargin, maxLineWidth);
            } else {
                top = 0;
                left += maxLineWidth;
                maxLineWidth = 0;
                lc = left + lp.leftMargin;
                tc = top + lp.topMargin;
                rc = lc + w;
                bc = tc + h;
                top = bc + lp.bottomMargin;
                child.layout(lc, tc, rc, bc);
                maxLineWidth = Math.max(lp.leftMargin + w + lp.rightMargin, maxLineWidth);
            }
        }
    }

}
