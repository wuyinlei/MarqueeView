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

    //线程管理类  需要用线程来管理
    private LoopRunnable mLoopRunnable;

    private boolean mAnimating = false;

    //滚动工具类
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
        mLoopRunnable = new LoopRunnable();  //初始化
        mScroller = new Scroller(context, new AccelerateInterpolator());  //scroller初始化
        mInnerAdapter = new InnerAdapter();  //InnerAdapter初始化
    }

    public AutoScrollListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAutoScroll && mOutterAdapter != null) {
            AutoScroll autoScroll = (AutoScroll) mOutterAdapter;
            //高度测量
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
        postDelayed(mLoopRunnable, DALY_TIME);  //在依附于window的时候开始循环
        mAnimating = true; //有动画
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i("AutoScrollListView", "onDetachedFromWindow");
        removeCallbacks(mLoopRunnable);  //移除线程  防止内存泄漏
    }

    int preY = 0;

    //在这个函数里我们可以去取得事先设置好的成员变量mScroller中的位置信息、速度信息
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

    /**
     * 检测位置信息
     */
    private void checkPosition() {
        if (!mAutoScroll) return;  //不是自动滚动
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
            setSelection(targetPosition);  //设置选择的item
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
        removeCallbacks(mLoopRunnable); //移除线程
        mAnimating = false; //动画停止
    }

    /**
     * 线程管理类
     */
    class LoopRunnable implements Runnable {

        @Override
        public void run() {
            Log.i("AutoScrollListView", "run");
            mAnimating = true;
            View childAt = getChildAt(0);  //获取到第一个子view
            int scrollHeight = childAt.getMeasuredHeight() + getDividerHeight(); //获取到滚动的高度  自身高度+分隔线高度
            //开始移动
            mScroller.startScroll(0, 0, 0, mScrollOrientation == SCROLL_UP ? scrollHeight : -scrollHeight);
            //重新绘制
            invalidate();
        }

    }

    class InnerAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            //首先  是否是自动滚动
            // 如果是  数量是mOutterAdapter.getCount()+((AutoScroll) mOutterAdapter).getVisiableCount() * 2
            // 如果不是自动滚动   mOutterAdapter.getCount()
            return mOutterAdapter == null ? 0 :
                    (mAutoScroll ? mOutterAdapter.getCount() +
                            ((AutoScroll) mOutterAdapter).getVisiableCount() * 2
                            : mOutterAdapter.getCount());
        }

        @Override
        public Object getItem(int position) {
            return mOutterAdapter.getItem((int) getItemId(position));
        }

        @Override
        public long getItemId(int position) {
            if (mAutoScroll) {
                AutoScroll autoScroll = (AutoScroll) mOutterAdapter;
                //可见的个数 2
                int immovableCount = autoScroll.getVisiableCount();
                //外部的  没有显示的view的个数
                int outerCount = mOutterAdapter.getCount();  //3

                Log.d("InnerAdapter", "immovableCount:" + immovableCount);

                Log.d("InnerAdapter", "outerCount:" + outerCount);
                //如果当前的位置小于可见的数量   比如  显示1个  当前为0
                if (position < immovableCount) {//第一组
                    Log.d("InnerAdapter", "第一组position:" + position);
                    //那么返回的就是  position=0  返回1  position = 1  返回2
                    return outerCount - immovableCount + position;
                    //如果 1  1  3
                } else if (position < immovableCount + outerCount) {//第二组
                    Log.d("InnerAdapter", "第二组position:" + position);
                    //显示0  position= 2  返回0
                    return position - immovableCount;
                } else {//第三组
                    //显示 2  1  3
                    Log.d("InnerAdapter", "第三组position:" + position);
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
