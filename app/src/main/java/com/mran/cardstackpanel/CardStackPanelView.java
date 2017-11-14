package com.mran.cardstackpanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.support.v4.widget.ViewDragHelper.STATE_DRAGGING;
import static android.support.v4.widget.ViewDragHelper.STATE_IDLE;
import static android.support.v4.widget.ViewDragHelper.STATE_SETTLING;

/**
 * Created by mran on 17-11-13.
 */

public class CardStackPanelView extends FrameLayout {


    public CardStackPanelView(@NonNull Context context) {
        super(context);

    }

    public CardStackPanelView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);

    }

    public CardStackPanelView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private ViewDragHelper viewDragHelper;
    int paddingLeft = 0;
    int paddingRight = 0;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (paddingLeft == 0) {
            paddingLeft = getPaddingLeft();
            paddingRight = getPaddingRight();
        }
        if (getChildCount() == 1) {
            setPadding(Math.min(paddingLeft, paddingRight), getPaddingTop(), Math.min(paddingLeft, paddingRight), getPaddingBottom());
        } else {
            setPadding(paddingLeft, getPaddingTop(), paddingRight, getPaddingBottom());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();


        if (count <= 3) {
            visibleViewCount = count;
        } else visibleViewCount = 3;
        super.onLayout(changed, left, top, right, bottom);
        if (itemInfoList.size() != count) {
            itemInfoList.clear();
            for (int i = 0; i < count; i++) {
                View view = getChildAt(i);
                ItemInfo itemInfo = new ItemInfo();
                itemInfo.position = count - i - 1;
                itemInfo.view = view;
                itemInfoList.add(itemInfo);
            }
        }
        if (count > 1)
            viewTrans(0, false);

    }


    @Override
    public void computeScroll() {
        if (viewDragHelper.continueSettling(true))
            ViewCompat.postInvalidateOnAnimation(this);
    }

    List<ItemInfo> itemInfoList;
    int currentLeft;//当前拖动视图的LEFT
    int currentTop;//当期拖动视图的TOP
    boolean dragAble = true;//能否拖动
    boolean changed = false;//是否发生视图改变
    int LEFT = 1;//左划出标记
    int RIGHT = 2;//右划出标记
    int lastDirection = 0;//记录上次的划出方向
    int direction = 0;//记录当前划出方向
    int visibleViewCount = 3;//可见视图的数量
    int ITEM_DEFAULT_INTERVAL = 50;
    float itemInterval;//item之间的水平间隔

    Context mContext;
    AttributeSet mAttributeSet;
    Animation animation;


    void init(Context context, AttributeSet attributeSet) {
        mContext = context;
        mAttributeSet = attributeSet;
        getAttrs();

        itemInfoList = new ArrayList<>();
        animation = new TranslateAnimation(-40, 0, 0, 0);

        animation.setDuration(100);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                dragAble = false;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animation.setFillAfter(true);
                animation.setFillEnabled(true);
                itemInfoList.get(itemInfoList.size() - visibleViewCount).view.clearAnimation();
                dragAble = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

        });

        viewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                if (releasedChild.getLeft() < -getWidth() / 5) {
                    changed = true;
                    viewDragHelper.settleCapturedViewAt(-getWidth(), currentTop);
                } else if (releasedChild.getRight() > getWidth() * 1.2) {
                    changed = true;
                    viewDragHelper.settleCapturedViewAt(getWidth(), currentTop);
                } else {
                    changed = false;
                    viewDragHelper.settleCapturedViewAt(currentLeft, currentTop);
                }
                invalidate();
                viewDragHelper.continueSettling(true);
            }

            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                currentLeft = child.getLeft();
                currentTop = child.getTop();
                return itemInfoList.size() > 1 && itemInfoList.get(itemInfoList.size() - 1).view == child && dragAble;
            }

            @Override
            public void onViewCaptured(View capturedChild, int activePointerId) {
                Log.d(TAG, "onViewCaptured: " + ((TextView) capturedChild).getText());
                super.onViewCaptured(capturedChild, activePointerId);
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                return left;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                return currentTop;
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                Log.d(TAG, "onViewPositionChanged: " + left + "dx^^" + dx);

                float deltaX;
                if (lastDirection != direction) {
                    Log.d(TAG, "onViewPositionChanged: " + left + "dx^^" + dx + "directionchange+currentx" + currentLeft);
                    deltaX = -Math.abs(left - currentLeft) / (float) (getWidth() + getPaddingLeft());
                    viewTrans(deltaX, true);
                    lastDirection = direction;
                    return;
                }
                lastDirection = direction;
                if (direction == RIGHT) {
                    deltaX = -dx / (float) (getWidth() - getPaddingLeft());
                } else {
                    deltaX = dx / (float) (getWidth() + getPaddingLeft());
                }

                viewTrans(deltaX, false);

            }

            @Override
            public void onViewDragStateChanged(int state) {
                switch (state) {
                    case STATE_IDLE:

                        Log.d(TAG, "onViewDragStateChanged: STATE_IDLE");
                        if (changed) {
                            addInEnd(itemInfoList.get(itemInfoList.size() - 1).view);
                            itemInfoList.get(itemInfoList.size() - visibleViewCount).view.startAnimation(animation);
                        } else dragAble = true;

                        break;
                    case STATE_DRAGGING:
                        dragAble = false;
                        Log.d(TAG, "onViewDragStateChanged: STATE_DRAGGING");
                        break;
                    case STATE_SETTLING:
                        dragAble = false;
                        Log.d(TAG, "onViewDragStateChanged: STATE_SETTLING");
                        break;
                }
            }
        });

    }

    void getAttrs() {
        TypedArray typedArray = mContext.obtainStyledAttributes(mAttributeSet, R.styleable.CardStackPanelView);
        itemInterval = typedArray.getDimension(R.styleable.CardStackPanelView_itemInterval, ITEM_DEFAULT_INTERVAL);
        visibleViewCount = typedArray.getInteger(R.styleable.CardStackPanelView_visibleItemCount, 3);

        typedArray.recycle();
    }

    private class ItemInfo {
        float position;
        View view;
    }

    private void viewTrans(float deltaPosition, boolean directionChange) {
        Log.d(TAG, "viewTrans: deltaPosition" + deltaPosition + "directions" + direction);


        if (itemInfoList.size() >= 2) {
            for (int i = itemInfoList.size() - 2; i >= 0; i--) {
                ItemInfo ii = itemInfoList.get(i);

                if (directionChange)
                    ii.position = itemInfoList.size() - i - 1 + deltaPosition;
                else
                    ii.position += deltaPosition;
                if (ii.position <= 0)
                    ii.position = 0;

                ii.view.setPivotX(ii.view.getWidth() - ii.view.getWidth() / 5);
                ii.view.setPivotY(ii.view.getHeight() / 2);
                ii.view.setScaleX((float) Math.pow(0.9, ii.position));
                ii.view.setScaleY((float) Math.pow(0.9, ii.position));
                ii.view.setTranslationX(ii.position * itemInterval);//50为偏移量
                if (i < itemInfoList.size() - visibleViewCount)
                    ii.view.setVisibility(INVISIBLE);
                else {
                    ii.view.setVisibility(VISIBLE);
                    ii.view.setAlpha(1);
                }
            }
            itemInfoList.get(itemInfoList.size() - 1).view.setAlpha(itemInfoList.get(itemInfoList.size() - 2).position);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private void addInEnd(View view) {

        itemInfoList.remove(itemInfoList.size() - 1);
        removeView(view);
        ItemInfo itemInfo = new ItemInfo();
        itemInfo.position = itemInfoList.size();
        itemInfo.view = view;
        itemInfoList.add(0, itemInfo);
        addView(view, 0);

    }

    float firstX;//首次touch的X坐标

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (viewDragHelper.shouldInterceptTouchEvent(ev))
            return true;

        return super.onInterceptTouchEvent(ev);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        if (x >= firstX) {
            direction = RIGHT;
            Log.d(TAG, "onTouchEvent: RIGHT");
        } else if (x < firstX) {
            direction = LEFT;
            Log.d(TAG, "onTouchEvent: LEFT");

        }
        viewDragHelper.processTouchEvent(event);


        switch (action) {
            case MotionEvent.ACTION_DOWN:
                firstX = x;
                return true;


        }
        return super.onTouchEvent(event);
    }
}
