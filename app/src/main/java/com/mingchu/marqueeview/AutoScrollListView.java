package com.mingchu.marqueeview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.IntDef;
import android.support.v4.widget.ListViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;

public class AutoScrollListView extends ListView {

    public final static int SCROLL_UP = 0x00;

    public final static int SCROLL_DOWN = 0x01;

    @IntDef({SCROLL_DOWN, SCROLL_UP})
    public @interface ScrollOritation {
    }

    private static int DALY_TIME = 5000;  //延时滚动时间

    private LoopRunnable mLoopRunnable;

    private boolean mAnimating = false;

    private Scroller mScroller;

    private InnerAdapter mInnerAdapter;

    private ListAdapter mOutterAdapter;

    private InnerOnItemClickListener mInnerOnItemClickListener;

    private OnItemClickListener mOutterOnItemClickListener;

    private InnerOnItemLongClickListener mInnerOnItemLongClickListener;

    private OnItemLongClickListener mOutterOnItemLongClickListener;

    private boolean mAutoScroll = false;  //是否自动滚动

    /**
     * 滚动方向，默认向上滚动。
     */
    private int mScrollOrientation = SCROLL_UP;

    private float mMoveDistance = 0;  //移动的距离

    private float mPreX = 0;  //开始x位置

    private float mPreY = 0;  //开始y位置

    private boolean mIgnoreLongClick = false;  //是否忽略长按点击事件

    /**
     * 设置延时事件
     *
     * @param dalyTime 延时事件   单位: ms
     */
    public static void setDalyTime(int dalyTime) {
        DALY_TIME = dalyTime;
    }

    public AutoScrollListView(Context context) {
        this(context, null);
    }

    public AutoScrollListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLoopRunnable = new LoopRunnable();
        mScroller = new Scroller(context, new AccelerateInterpolator());
        mInnerAdapter = new InnerAdapter();
    }

    public AutoScrollListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAutoScroll && mOutterAdapter != null) {
            AutoScroll autoScroll = (AutoScroll) mOutterAdapter;
            int height = autoScroll.getListItemHeight(getContext()) * autoScroll.getVisiableCount()
                    + (autoScroll.getVisiableCount() - 1) * getDividerHeight();
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        mAutoScroll = adapter instanceof AutoScroll;
        mOutterAdapter = adapter;
        super.setAdapter(mInnerAdapter);
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        if (mInnerOnItemClickListener == null) {
            mInnerOnItemClickListener = new InnerOnItemClickListener();
        }
        mOutterOnItemClickListener = listener;
        super.setOnItemClickListener(mInnerOnItemClickListener);
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        if (mInnerOnItemLongClickListener == null) {
            mInnerOnItemLongClickListener = new InnerOnItemLongClickListener();
        }
        mOutterOnItemLongClickListener = listener;
        super.setOnItemLongClickListener(mInnerOnItemLongClickListener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i("AutoScrollListView", "onAttachedToWindow");
        postDelayed(mLoopRunnable, DALY_TIME);
        mAnimating = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i("AutoScrollListView", "onDetachedFromWindow");
        removeCallbacks(mLoopRunnable);
    }

    int preY = 0;

    @Override
    public void computeScroll() {
        Log.i("AutoScrollListView", "computeScroll");
        if (!mScroller.computeScrollOffset()) {
            Log.i("AutoScrollListView", "compute finish");
            if (mAnimating) {
                Log.i("AutoScrollListView", "compute ignore runnable");
                return;
            }
            Log.i("AutoScrollListView", "compute send runnable");
            removeCallbacks(mLoopRunnable);
            postDelayed(mLoopRunnable, DALY_TIME);
            mAnimating = true;
            preY = 0;
            checkPosition();
        } else {
            mAnimating = false;
            Log.i("AutoScrollListView", "compute not finish");
            int dY = mScroller.getCurrY() - preY;
            ListViewCompat.scrollListBy(this, dY);
            preY = mScroller.getCurrY();
            invalidate();
        }
    }

    private void checkPosition() {
        if (!mAutoScroll) return;
        int targetPosition = -1;
        int firstVisiblePosition = getFirstVisiblePosition();
        if (firstVisiblePosition == 0) {
            AutoScroll autoScroll = (AutoScroll) mInnerAdapter;
            targetPosition = mInnerAdapter.getCount() - autoScroll.getVisiableCount() * 2;
        }
        int lastVisiblePosition = getLastVisiblePosition();
        if (lastVisiblePosition == getCount() - 1) {
            AutoScroll autoScroll = (AutoScroll) mOutterAdapter;
            targetPosition = autoScroll.getVisiableCount();
        }
        if (targetPosition >= 0 && firstVisiblePosition != targetPosition) {
            setSelection(targetPosition);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mMoveDistance = 0;
            mPreX = ev.getX();
            mPreY = ev.getY();
            mIgnoreLongClick = false;
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            mMoveDistance += (Math.abs(ev.getX() - mPreX) + Math.abs(ev.getY() - mPreY));
            mPreX = ev.getX();
            mPreY = ev.getY();
            if (mMoveDistance > 20 || !mScroller.isFinished()) {
                mIgnoreLongClick = true;
            }
            return true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP
                || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (mMoveDistance > 20 || !mScroller.isFinished()) {
                ev.setAction(MotionEvent.ACTION_CANCEL);
            }
            mIgnoreLongClick = false;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 设置滚动的防线
     *
     * @param oritation 滚动方向，SCROLL_UP 向上，SCROLL_DOWN 向下。
     */
    public void setScrollOrientation(@ScrollOritation int oritation) {
        this.mScrollOrientation = oritation;
    }

    /**
     * 开始自动滚动
     */
    public void startAutoScroll() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        removeCallbacks(mLoopRunnable);
        mAnimating = false;
        post(mLoopRunnable);
    }

    /**
     * 停止自动滚动
     */
    public void stopAutoScroll() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        removeCallbacks(mLoopRunnable);
        mAnimating = false;
    }

    /**
     * 线程管理类
     */
    class LoopRunnable implements Runnable {

        @Override
        public void run() {
            Log.i("AutoScrollListView", "run");
            mAnimating = true;
            View childAt = getChildAt(0);
            int scrollHeight = childAt.getMeasuredHeight() + getDividerHeight();
            mScroller.startScroll(0, 0, 0, mScrollOrientation == SCROLL_UP ? scrollHeight : -scrollHeight);
            invalidate();
        }

    }

    class InnerAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mOutterAdapter == null ? 0 :
                    (mAutoScroll ? mOutterAdapter.getCount() + ((AutoScroll) mOutterAdapter).getVisiableCount() * 2 : mOutterAdapter.getCount());
        }

        @Override
        public Object getItem(int position) {
            return mOutterAdapter.getItem((int) getItemId(position));
        }

        @Override
        public long getItemId(int position) {
            if (mAutoScroll) {
                AutoScroll autoScroll = (AutoScroll) mOutterAdapter;
                int immovableCount = autoScroll.getVisiableCount();
                int outerCount = mOutterAdapter.getCount();
                if (position < immovableCount) {//第一组
                    return outerCount - immovableCount + position;
                } else if (position < immovableCount + outerCount) {//第二组
                    return position - immovableCount;
                } else {//第三组
                    return position - (immovableCount + outerCount);
                }
            } else {
                return position;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mOutterAdapter.getView((int) getItemId(position), convertView, parent);
        }

    }

    /**
     * 内部item的点击事件
     */
    class InnerOnItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            if (mOutterOnItemClickListener != null && mInnerAdapter != null) {
                mOutterOnItemClickListener.onItemClick(parent, view, (int) mInnerAdapter.getItemId(position), id);
            }
        }

    }

    class InnerOnItemLongClickListener implements OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                       int position, long id) {
            return mOutterOnItemLongClickListener != null && mInnerAdapter != null && !mIgnoreLongClick && mOutterOnItemLongClickListener.onItemLongClick(parent, view, (int) mInnerAdapter.getItemId(position), id);
        }

    }

    public interface AutoScroll {
        /**
         * 返回屏幕可见个数
         *
         * @return 可见个数
         */
        public int getVisiableCount();

        /**
         * 获取条目高度
         *
         * @return 高度
         */
        public int getListItemHeight(Context context);
    }
}
