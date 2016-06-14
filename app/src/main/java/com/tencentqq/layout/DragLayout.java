package com.tencentqq.layout;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.nineoldandroids.view.ViewHelper;

/**
 * Created by Anonymous on 2016/6/10.
 */
public class DragLayout extends FrameLayout {


    private static OnStatusDragListener mDragListener;
    private ViewDragHelper.Callback mCallbck;
    private ViewDragHelper mDragHelper;
    private View mfrontView;
    private View mLastView;
    public int mWidth;
    public int mHeight;
    private int mRange;
    public Status mStatus = Status.Close; //默认状态


    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context) {
        this(context, null);
    }


    private void init() {

        mCallbck = new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return true;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                if (child == mfrontView) {
                    left = fixLeft(left);
                }
                return left;
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);

                int newLeft = left;
                if (changedView == mLastView) {
                    newLeft = mfrontView.getLeft() + dx;
                }
                //修正
                newLeft = fixLeft(newLeft);
                if (changedView == mLastView) {
                    mLastView.layout(0, 0, mWidth, mHeight);
                    mfrontView.layout(newLeft, 0, newLeft + mWidth, mHeight);

                }
                // 更新状态,执行动画
                dispatchDragEvent(newLeft);
                // 为了兼容低版本, 每次修改值之后, 进行重绘
                invalidate();
            }


            @Override
            public int getViewHorizontalDragRange(View child) {
                return mRange;
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);

                // 判断执行 关闭/开启
                // 先考虑所有开启的情况,剩下的就都是关闭的情况
                if (xvel == 0 && mfrontView.getLeft() > mRange / 2.0f) {
                    //开启
                    open();
                } else if (xvel > 0) {
                    //开启
                    open();
                } else {
                    close();
                }
            }
        };

        //1.viewDragHelper初始化
        mDragHelper = ViewDragHelper.create(this, mCallbck);
    }

    public static enum Status{
        Drgging, Close, Open;
    }
    public interface OnStatusDragListener{
        void onDragging(float percent);
        void onClose();
        void onOpen();
    }
    public static void setOnStatusDragListener(OnStatusDragListener onDragListener) {
        DragLayout.mDragListener = onDragListener;
    }

    private void dispatchDragEvent(int newLeft) {
        //0.0f --> 1.0f
        float percent = newLeft * 1.0f / mRange;

        if (mDragListener != null){
            mDragListener.onDragging(percent);
        }

        Status perStatus = mStatus;
        mStatus = updataStatus(percent);
        if (perStatus != mStatus) {
            if (mStatus == Status.Close){
                if (mDragListener != null) {
                    mDragListener.onClose();
                }
            }else if (mStatus == Status.Open){
                if (mDragListener != null) {
                    mDragListener.onOpen();
                }
            }
        }
        animView(percent);
    }

    private Status updataStatus(float percent) {

        if (percent == 0f){
           return Status.Close;
        }else if (percent == 1.0f){
            return Status.Open;
        }

        return Status.Drgging;
    }

    private void open() {

        if (mDragHelper.smoothSlideViewTo(mfrontView, mRange, 0)) {
            ViewCompat.postInvalidateOnAnimation(this);

        }
    }

    private void close() {

        if (mDragHelper.smoothSlideViewTo(mfrontView, 0, 0)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void animView(float percent) {

        //1. 左面板: 缩放动画, 平移动画, 透明度动画
        ViewHelper.setScaleX(mLastView, evaluate(percent, 0.5f, 1.0f));
        ViewHelper.setScaleY(mLastView, evaluate(percent, 0.5f, 1.0f));
        // 平移动画: -mWidth / 2.0f -> 0.0f
        ViewHelper.setTranslationX(mLastView, evaluate(percent, -mWidth / 2.0f, 0.0f));
        // 透明度: 0.5 -> 1.0f
        ViewHelper.setAlpha(mLastView, evaluate(percent, 0.1f, 0.8f));
        //2. 主面板: 缩放动画
        ViewHelper.setScaleX(mfrontView, evaluate(percent, 1.0f, 0.7f));
        ViewHelper.setScaleY(mfrontView, evaluate(percent, 1.0f, 0.7f));

        //3. 背景动画: 亮度变化 (颜色变化)
        //getBackground().setColorFilter((Integer) evaluateColor(percent, Color.BLUE, Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);


    }

    /*估值器*/
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() < 2) {
            throw new IllegalArgumentException("DragLayout need two subclasses");
        }
        mLastView = getChildAt(0);
        mfrontView = getChildAt(1);

    }

    /**
     * 颜色估值器
     *
     * @param fraction
     * @param startValue
     * @param endValue
     * @return
     */
    public Object evaluateColor(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int) ((startA + (int) (fraction * (endA - startA))) << 24) |
                (int) ((startR + (int) (fraction * (endR - startR))) << 16) |
                (int) ((startG + (int) (fraction * (endG - startG))) << 8) |
                (int) ((startB + (int) (fraction * (endB - startB))));
    }

    private int fixLeft(int left) {

        if (left < 0) {
            return 0;
        } else if (left > mRange) {
            return mRange;
        }
        return left;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();

        //移动的距离
        mRange = (int) (mWidth * 0.6f);

    }

    //2.传递事件
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mDragHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
