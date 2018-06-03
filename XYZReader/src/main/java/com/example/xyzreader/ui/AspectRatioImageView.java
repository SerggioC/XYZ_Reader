package com.example.xyzreader.ui;

import android.content.Context;
import android.util.AttributeSet;

public class AspectRatioImageView extends android.support.v7.widget.AppCompatImageView {
    private float mAspectRatio = 1.5f; // Width:Height

    public AspectRatioImageView(Context context) {
        super(context);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) (MeasureSpec.getSize(widthMeasureSpec) / mAspectRatio);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightSpec);
//        int measuredWidth = getMeasuredWidth();
//        setMeasuredDimension(measuredWidth, (int) (measuredWidth / mAspectRatio));
    }
}
