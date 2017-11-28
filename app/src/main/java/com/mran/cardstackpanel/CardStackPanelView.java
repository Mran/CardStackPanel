package com.mran.cardstackpanel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

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
        init(context);

    }

    public CardStackPanelView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private ViewDragHelper viewDragHelper;


    List<ItemInfo> itemInfoList;
    int currentLeft;//当前拖动视图的LEFT
    int currentTop;//当前拖动视图的TOP
    boolean changed = false;//是否发生视图改变
    int LEFT = 1;//左划出标记
    int RIGHT = 2;//右划出标记
    int TOP = 3;//左划出标记
    int BOTTOM = 4;//右划出标记
    int lastDirection = 0;//记录上次的划出方向
    int direction = 0;//记录当前划出方向
    int DEFAULT_VISIBLE_VIEW_COUNT = 3;
    int visibleViewCount = DEFAULT_VISIBLE_VIEW_COUNT;//可见视图的数量
    int ITEM_DEFAULT_INTERVAL = 50;//item之间的水平间隔
    float itemInterval = ITEM_DEFAULT_INTERVAL;//item之间的水平间隔

    long lastActionDownTime;
    long CLICK_OFFSET = 90;
    int lastLeft;
    int lastTop;
    float lastX = 0;

    public static final int V_TOP_TYPE = 0;
    public static final int V_BOTTOM_TYPE = 1;
    public static final int H_LEFT_TYPE = 2;
    public static final int H_RIGHT_TYPE = 3;

    int type = H_RIGHT_TYPE;
    Context mContext;
    boolean mIdled = true;

    View mCurrentAnimationView;


    @Override
    public void removeAllViews() {
        itemInfoList.clear();
        super.removeAllViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int cardHeight;
        int cardWidth;
        switch (type) {
            case V_TOP_TYPE:
                cardHeight = height - getPaddingTop();
                cardWidth = width;
                childMeasure(cardWidth, cardHeight);
                break;
            case V_BOTTOM_TYPE:
                cardHeight = height - getPaddingBottom();
                cardWidth = width;
                childMeasure(cardWidth, cardHeight);

                break;
            case H_LEFT_TYPE:
                cardHeight = height;
                cardWidth = (width - getPaddingLeft());
                childMeasure(cardWidth, cardHeight);

                break;
            case H_RIGHT_TYPE:
                cardHeight = height;
                cardWidth = (width - getPaddingRight());
                childMeasure(cardWidth, cardHeight);
                break;
        }

    }

    private void childMeasure(int cardWidth, int cardHeight) {

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(cardHeight, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();

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
        if (count > 1) {
            chooseTransWithType(0, false);
        }

    }


    @Override
    public void computeScroll() {
        if (viewDragHelper.continueSettling(true))
            ViewCompat.postInvalidateOnAnimation(this);
    }


    OnClickListener mOnClickListener;

    void init(Context context) {
        mContext = context;

        itemInfoList = new ArrayList<>();

        viewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {

                if (type == H_LEFT_TYPE || type == H_RIGHT_TYPE) {
                    if (xvel > 3500 || releasedChild.getRight() > getWidth() * 1.2) {//向右划出
                        changed = true;

                        viewDragHelper.settleCapturedViewAt(getWidth(), currentTop);
                    } else if (xvel < -3500 || releasedChild.getLeft() < -getWidth() / 5) {//向左划出
                        changed = true;
                        viewDragHelper.settleCapturedViewAt(-getWidth(), currentTop);
                    } else {//回到原来的位置
                        changed = false;
                        viewDragHelper.settleCapturedViewAt(currentLeft, currentTop);
                    }
                } else {
                    if (yvel < -3500 || releasedChild.getTop() < -getHeight() / 5) {//向上划出
                        changed = true;
                        viewDragHelper.settleCapturedViewAt(currentLeft, -getHeight());
                    } else if (yvel > 3500 || releasedChild.getBottom() > getHeight() * 1.2) {//向下划出
                        changed = true;
                        viewDragHelper.settleCapturedViewAt(currentLeft, getHeight());
                    } else {//回到原来的位置
                        changed = false;
                        viewDragHelper.settleCapturedViewAt(currentLeft, currentTop);
                    }

                }
                invalidate();
            }

            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                lastLeft = child.getLeft();
                lastTop = child.getTop();
                return itemInfoList.size() > 1 && (itemInfoList.get(itemInfoList.size() - 1).view == child || itemInfoList.get(itemInfoList.size() - 2).view == child);
            }

            @Override
            public void onViewCaptured(View capturedChild, int activePointerId) {
                if (mIdled) {
                    currentLeft = capturedChild.getLeft();
                    lastLeft = capturedChild.getLeft();
                    lastTop = capturedChild.getTop();
                    currentTop = capturedChild.getTop();
                }
                super.onViewCaptured(capturedChild, activePointerId);
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                if (type == H_LEFT_TYPE || type == H_RIGHT_TYPE)
                    return left;
                else return currentLeft;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                if (type == V_BOTTOM_TYPE || type == V_TOP_TYPE) {
                    if (top >= getHeight())
                        return getHeight();
                    else if (top <= -getHeight())
                        return -getHeight();
                    else
                        return top;
                } else return currentTop;
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                float deltaPosition;
                if (type == H_RIGHT_TYPE) {
                    float deltaX = left - lastLeft;
                    if (left > currentLeft) {
                        direction = RIGHT;
                    } else direction = LEFT;
                    if (lastDirection != direction) {
                        deltaPosition = -Math.abs(left - currentLeft) / (float) (getWidth());
                        chooseTransWithType(deltaPosition, true);
                        lastDirection = direction;
                        return;
                    }
                    lastDirection = direction;
                    if (direction == RIGHT) {
                        deltaPosition = -deltaX / (float) (getWidth());
                    } else {
                        deltaPosition = deltaX / (float) (getWidth());
                    }
                    chooseTransWithType(deltaPosition, false);
                    lastLeft = left;
                } else if (type == H_LEFT_TYPE) {
                    float deltaX = left - lastLeft;
                    if (left > currentLeft) {
                        direction = RIGHT;
                    } else direction = LEFT;
                    if (lastDirection != direction) {
                        deltaPosition = -Math.abs(left - currentLeft) / (float) (getWidth());
                        chooseTransWithType(deltaPosition, true);
                        lastDirection = direction;
                        return;
                    }
                    lastDirection = direction;
                    if (direction == RIGHT) {
                        deltaPosition = -deltaX / (float) (getWidth() - getPaddingLeft());
                    } else {
                        deltaPosition = deltaX / (float) (getWidth() + getPaddingLeft());
                    }
                    chooseTransWithType(deltaPosition, false);
                    lastLeft = left;
                } else if (type == V_TOP_TYPE) {

                    float deltaY = top - lastTop;
                    if (top >= currentTop) {
                        direction = BOTTOM;
                    } else direction = TOP;
                    if (lastDirection != direction) {
                        deltaPosition = -Math.abs(top - currentTop) / (float) (getHeight());
                        chooseTransWithType(deltaPosition, true);
                        lastDirection = direction;
                        return;
                    }
                    lastDirection = direction;
                    if (direction == TOP) {
                        deltaPosition = deltaY / (float) (getHeight() + getPaddingTop());
                    } else {
                        deltaPosition = -deltaY / (float) (getHeight() - getPaddingTop());
                    }
                    chooseTransWithType(deltaPosition, false);
                    lastTop = top;
                } else if (type == V_BOTTOM_TYPE) {

                    float deltaY = top - lastTop;
                    if (top >= currentTop) {
                        direction = BOTTOM;
                    } else direction = TOP;
                    if (lastDirection != direction) {
                        deltaPosition = -Math.abs(top - currentTop) / (float) (getHeight() );
                        chooseTransWithType(deltaPosition, true);
                        lastDirection = direction;
                        return;
                    }
                    lastDirection = direction;
                    if (direction == TOP) {
                        deltaPosition = deltaY / (float) (getHeight() );
                    } else {
                        deltaPosition = -deltaY / (float) (getHeight());
                    }
                    chooseTransWithType(deltaPosition, false);
                    lastTop = top;
                }
            }

            @Override
            public void onViewDragStateChanged(int state) {
                switch (state) {
                    case STATE_IDLE:
                        mIdled = true;
                        if (changed) {
                            addInEnd(itemInfoList.get(itemInfoList.size() - 1).view);
                            reInitState();
                        }

                        break;
                    case STATE_DRAGGING:
                        if (changed && !mIdled) {
                            addInEnd(itemInfoList.get(itemInfoList.size() - 1).view);
                            reInitState();
                        }
                        mIdled = false;
                        break;
                    case STATE_SETTLING:
                        if (changed) {

                            mCurrentAnimationView = itemInfoList.get(itemInfoList.size() - visibleViewCount - 1).view;
                            animationWithType(mCurrentAnimationView);
                        }
                        mIdled = false;
                        break;
                }
            }
        });

    }


    private void animationWithType(View currentAnimationView) {
        float startLocationX = 0;
        float transLocationX = 0;
        float startLocationY = 0;
        float transLocationY = 0;


        switch (type) {

            case V_TOP_TYPE:
                startLocationY = -(visibleViewCount - 2) * itemInterval;
                transLocationY = -itemInterval * (visibleViewCount - 1);
                break;
            case V_BOTTOM_TYPE:
                startLocationY = (visibleViewCount - 2) * itemInterval;
                transLocationY = itemInterval * (visibleViewCount - 1);
                break;
            case H_LEFT_TYPE:
                startLocationX = (visibleViewCount - 2) * itemInterval;
                transLocationX = -itemInterval * (visibleViewCount - 1);
                break;
            case H_RIGHT_TYPE:
                startLocationX = (visibleViewCount - 2) * itemInterval;
                transLocationX = itemInterval * (visibleViewCount - 1);
                break;
        }
        if (type == V_TOP_TYPE || type == V_BOTTOM_TYPE) {
            currentAnimationView.setTranslationY(startLocationY);
            currentAnimationView.animate().translationY(transLocationY).setDuration(200).start();
        } else {
            currentAnimationView.setTranslationX(startLocationX);
            currentAnimationView.animate().translationX(transLocationX).setDuration(200).start();
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        mOnClickListener = l;
    }


    private class ItemInfo {
        float position;
        View view;
    }

    private void changePosition(float deltaPosition, boolean directionChange) {
        for (int i = itemInfoList.size() - 1; i >= 0; i--) {
            ItemInfo ii = itemInfoList.get(i);
            if (directionChange)
                ii.position = itemInfoList.size() - i - 1 + deltaPosition;//方向发生改变，position调整计算方法
            else if (i <= itemInfoList.size() - 2) {//改变从第二层开始，改变position的值
                ii.position += deltaPosition;
            } else ii.position -= deltaPosition;
            if (ii.position < 0) {//避免position出现小于0的情况
                ii.position = 0;
            }
        }
    }

    private void viewHLeftTrans(float deltaPosition, boolean directionChange) {
        changePosition(deltaPosition, directionChange);

        if (itemInfoList.size() >= 2) {
            for (int i = itemInfoList.size() - 1; i >= 0; i--) {
                ItemInfo ii = itemInfoList.get(i);
                //修改锚点
                ii.view.setPivotX(0);
                ii.view.setPivotY(ii.view.getHeight() / 2);

                if (i <= itemInfoList.size() - 2) {//没有被拖动的view执行放大动画
                    if (i > itemInfoList.size() - visibleViewCount - 1)
                        ii.view.setTranslationX(-ii.position * itemInterval);
                    ii.view.setScaleX((float) Math.pow(0.9, ii.position));
                    ii.view.setScaleY((float) Math.pow(0.9, ii.position));
                }

                if (i < itemInfoList.size() - visibleViewCount)
                    if (i == itemInfoList.size() - visibleViewCount - 1 && viewDragHelper.getViewDragState() == STATE_SETTLING && changed) {
                        ii.view.setAlpha(1);
                        ii.view.setVisibility(VISIBLE);
                    } else {
                        ii.view.setAlpha(1);

                        ii.view.setVisibility(INVISIBLE);
                    }
                else {
                    ii.view.setVisibility(VISIBLE);
                    if (i != itemInfoList.size() - 1)
                        ii.view.setAlpha(1);
                    else {
                        if (viewDragHelper.getViewDragState() == STATE_DRAGGING)
                            ii.view.setAlpha(getNewAlpha(1 - ii.position));
                        else ii.view.setAlpha(1 - ii.position);

                    }
                }
            }
        }
    }


    private void viewHRightTrans(float deltaPosition, boolean directionChange) {
        changePosition(deltaPosition, directionChange);

        if (itemInfoList.size() >= 2) {
            for (int i = itemInfoList.size() - 1; i >= 0; i--) {
                ItemInfo ii = itemInfoList.get(i);
                //修改锚点
                ii.view.setPivotX(ii.view.getWidth());
                ii.view.setPivotY(ii.view.getHeight() / 2);

                if (i <= itemInfoList.size() - 2) {//没有被拖动的view执行放大动画
                    if (i > itemInfoList.size() - visibleViewCount - 1)
                        ii.view.setTranslationX(ii.position * itemInterval);
                    ii.view.setScaleX((float) Math.pow(0.9, ii.position));
                    ii.view.setScaleY((float) Math.pow(0.9, ii.position));
                }

                if (i < itemInfoList.size() - visibleViewCount)
                    if (i == itemInfoList.size() - visibleViewCount - 1 && viewDragHelper.getViewDragState() == STATE_SETTLING && changed) {
                        ii.view.setAlpha(1);
                        ii.view.setVisibility(VISIBLE);
                    } else {
                        ii.view.setAlpha(1);

                        ii.view.setVisibility(INVISIBLE);
                    }
                else {
                    ii.view.setVisibility(VISIBLE);
                    if (i != itemInfoList.size() - 1)
                        ii.view.setAlpha(1);
                    else {
                        if (viewDragHelper.getViewDragState() == STATE_DRAGGING)
                            ii.view.setAlpha(getNewAlpha(1 - ii.position));
                        else ii.view.setAlpha(1 - ii.position);

                    }
                }
            }
        }
    }

    private void viewVTopTrans(float deltaPosition, boolean directionChange) {
        changePosition(deltaPosition, directionChange);

        if (itemInfoList.size() >= 2) {
            for (int i = itemInfoList.size() - 1; i >= 0; i--) {
                ItemInfo ii = itemInfoList.get(i);
                //修改锚点
                ii.view.setPivotX(ii.view.getWidth() / 2);
                ii.view.setPivotY(0);

                if (i <= itemInfoList.size() - 2) {//没有被拖动的view执行放大动画
                    if (i > itemInfoList.size() - visibleViewCount - 1)
                        ii.view.setTranslationY(-ii.position * itemInterval);
                    ii.view.setScaleX((float) Math.pow(0.9, ii.position));
                    ii.view.setScaleY((float) Math.pow(0.9, ii.position));
                }

                if (i < itemInfoList.size() - visibleViewCount)
                    if (i == itemInfoList.size() - visibleViewCount - 1 && viewDragHelper.getViewDragState() == STATE_SETTLING && changed) {
                        ii.view.setAlpha(1);
                        ii.view.setVisibility(VISIBLE);
                    } else {
                        ii.view.setAlpha(1);

                        ii.view.setVisibility(INVISIBLE);
                    }
                else {
                    ii.view.setVisibility(VISIBLE);
                    if (i != itemInfoList.size() - 1)
                        ii.view.setAlpha(1);
                    else {
                        if (viewDragHelper.getViewDragState() == STATE_DRAGGING)
                            ii.view.setAlpha(getNewAlpha(1 - ii.position));
                        else ii.view.setAlpha(1 - ii.position);

                    }
                }
            }
        }
    }

    private void viewVBottomTrans(float deltaPosition, boolean directionChange) {
        changePosition(deltaPosition, directionChange);

        if (itemInfoList.size() >= 2) {
            for (int i = itemInfoList.size() - 1; i >= 0; i--) {
                ItemInfo ii = itemInfoList.get(i);
                //修改锚点
                ii.view.setPivotX(ii.view.getWidth() / 2);
                ii.view.setPivotY(ii.view.getHeight());

                if (i <= itemInfoList.size() - 2) {//没有被拖动的view执行放大动画
                    if (i > itemInfoList.size() - visibleViewCount - 1)
                        ii.view.setTranslationY(ii.position * itemInterval);
                    ii.view.setScaleX((float) Math.pow(0.9, ii.position));
                    ii.view.setScaleY((float) Math.pow(0.9, ii.position));
                }

                if (i < itemInfoList.size() - visibleViewCount)
                    if (i == itemInfoList.size() - visibleViewCount - 1 && viewDragHelper.getViewDragState() == STATE_SETTLING && changed) {
                        ii.view.setAlpha(1);
                        ii.view.setVisibility(VISIBLE);
                    } else {
                        ii.view.setAlpha(1);

                        ii.view.setVisibility(INVISIBLE);
                    }
                else {
                    ii.view.setVisibility(VISIBLE);
                    if (i != itemInfoList.size() - 1)
                        ii.view.setAlpha(1);
                    else {
                        if (viewDragHelper.getViewDragState() == STATE_DRAGGING)
                            ii.view.setAlpha(getNewAlpha(1 - ii.position));
                        else ii.view.setAlpha(1 - ii.position);

                    }
                }
            }
        }
    }

    private float getNewAlpha(float alpha) {
        return (float) (1.0 - Math.pow((1.0 - alpha), 2 * 4));
    }

    private void addInEnd(View view) {
        itemInfoList.remove(itemInfoList.size() - 1);
        ItemInfo itemInfo = new ItemInfo();
        itemInfo.position = itemInfoList.size();
        itemInfo.view = view;
        itemInfoList.add(0, itemInfo);
        removeView(view);
        addView(view, 0);


    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                interceptParentViewScrollHorizontally(getParent());
        }
        if (viewDragHelper.shouldInterceptTouchEvent(ev))
            return true;
        return super.onInterceptTouchEvent(ev);
    }

    private void interceptParentViewScrollHorizontally(ViewParent viewParent) {
        if (viewParent instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) viewParent;
            boolean canScroll = parent.canScrollHorizontally(1) || parent.canScrollHorizontally(-1);
            if (canScroll) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
            interceptParentViewScrollHorizontally(parent.getParent());
        }
    }

    public void setType(int type) {
        if (type < 0 || type > 3)
            return;
        this.type = type;
    }

    public void setVisibleViewCount(int visibleViewCount) {
        this.visibleViewCount = visibleViewCount;
        setPaddingWithType();

    }

    public void setItemInterval(int interval) {
        this.itemInterval = interval;
        setPaddingWithType();
    }



    private void setPaddingWithType() {
        int paddingWithType = (int) (itemInterval * (visibleViewCount - 1));
        switch (type) {

            case V_TOP_TYPE:
                setPadding(0, paddingWithType, 0, 0);
                break;
            case V_BOTTOM_TYPE:
                setPadding(0, 0, 0, paddingWithType);
                break;
            case H_LEFT_TYPE:
                setPadding(paddingWithType, 0, 0, 0);
                break;
            case H_RIGHT_TYPE:
                setPadding(0, 0, paddingWithType, 0);
                break;
        }

    }

    private void chooseTransWithType(float deltaPosition, boolean directionChange) {
        switch (type) {

            case V_TOP_TYPE:
                viewVTopTrans(deltaPosition, directionChange);
                break;
            case V_BOTTOM_TYPE:
                viewVBottomTrans(deltaPosition, directionChange);
                break;
            case H_LEFT_TYPE:
                viewHLeftTrans(deltaPosition, directionChange);
                break;
            case H_RIGHT_TYPE:
                viewHRightTrans(deltaPosition, directionChange);
                break;
        }

    }

    private void reInitState() {
        int childCount = getChildCount();
        for (int i = 0; i < itemInfoList.size(); i++) {
            ItemInfo itempTemInfo = itemInfoList.get(i);
            itempTemInfo.position = childCount - i - 1;
        }
        chooseTransWithType(0, false);

    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        viewDragHelper.processTouchEvent(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastActionDownTime = System.currentTimeMillis();
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                if (mOnClickListener != null)
                    if (System.currentTimeMillis() - lastActionDownTime < CLICK_OFFSET && Math.abs(event.getX() - lastX) < 20)
                        mOnClickListener.onClick(itemInfoList.get(itemInfoList.size() - 1).view);

        }
        return super.onTouchEvent(event);
    }
}
