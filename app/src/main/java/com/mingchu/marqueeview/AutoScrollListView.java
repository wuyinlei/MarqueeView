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
        if (mAutoScroll && mOutterAdapter != null) { //如果是自动滚动和当前的adapter不为空
            AutoScroll autoScroll = (AutoScroll) mOutterAdapter; //
            //获取到高度  也就是滚动的view的高度
            int height = autoScroll.getListItemHeight(getContext()) * autoScroll.getVisiableCount()
                    + (autoScroll.getVisiableCount() - 1) * getDividerHeight();
            //进行测量
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

        // 如果返回true，表示动画还没有结束
        // 因为前面startScroll，所以只有在startScroll完成时 才会为false
        if (!mScroller.computeScrollOffset()) {  //没有
            Log.i("AutoScrollListView", "compute finish");
            if (mAnimating) {
                Log.i("AutoScrollListView", "compute ignore runnable");
                return;
            }
            Log.i("AutoScrollListView", "compute send runnable");
            removeCallbacks(mLoopRunnable);  //移除
            postDelayed(mLoopRunnable, DALY_TIME); //重新发送
            mAnimating = true;
            preY = 0;

            //检测当前的位置,防止位置错乱
            checkPosition();
        } else {  //动画没有结束
            mAnimating = false;  //动画标志置为false
            Log.i("AutoScrollListView", "compute not finish");
            int dY = mScroller.getCurrY() - preY;  //获取到当前的y坐标
            ///**
            //* Scrolls the list items within the view by a specified number of pixels.
            //        *
            //* @param y the amount of pixels to scroll by vertically
            //        * @see #canScrollList(int)
            //*/
            //   public void scrollListBy(int y) {
            //      trackMotionScroll(-y, -y);
            //    }
            //ListView的item滚动距离y
            ListViewCompat.scrollListBy(this, dY); //
            preY = mScroller.getCurrY();  //获取到当前y
            invalidate();  //滚动完成之后重新绘制
        }
    }

    /**
     * 检测位置信息
     */
    private void checkPosition() {
        if (!mAutoScroll) return;
        int targetPosition = -1; //初始化目标位置
        //第一个可见的view的位置
        int firstVisiblePosition = getFirstVisiblePosition();
        if (firstVisiblePosition == 0) {
            //如果当前的所在的位置是第一个可见的view的位置,也就是第一个item
            AutoScroll autoScroll = (AutoScroll) mInnerAdapter;
            targetPosition = mInnerAdapter.getCount() - autoScroll.getVisiableCount() * 2;
        }
        //最后一个item的位置
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
            //移动的距离
            mMoveDistance += (Math.abs(ev.getX() - mPreX) + Math.abs(ev.getY() - mPreY));
            mPreX = ev.getX();
            mPreY = ev.getY();
            //移动的距离大于指定值  并且当前的滚动还没有完成
            if (mMoveDistance > 20 || !mScroller.isFinished()) {
                mIgnoreLongClick = true;
            }
            return true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP
                || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (mMoveDistance > 20 || !mScroller.isFinished()) {
                //取消长按时间
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
            mAnimating = true;  //线程启动的时候设置动画为ture
            View childAt = getChildAt(0);  //获取到第一个子view
            //得到滑动的高度  也就是当前可滑动的item的高度
            int scrollHeight = childAt.getMeasuredHeight() + getDividerHeight();
            //然后进行滑动
            mScroller.startScroll(0, 0, 0, mScrollOrientation == SCROLL_UP ? scrollHeight : -scrollHeight);
            invalidate(); //重新绘制
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
