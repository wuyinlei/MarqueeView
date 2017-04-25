# 自定义ListView实现任意View跑马灯效果

标签（空格分隔）： 开源项目

---
##看图
话不多说,先来看下大图效果吧,这里的GIF录制有点渣,不过真实的跑出来的效果还是挺不错的。<br>

![](http://ww1.sinaimg.cn/mw690/006jcGvzly1fequ18ywfjg306u0bs3yu.gif)

<br>
![](http://ww1.sinaimg.cn/mw690/006jcGvzly1fequ3uxiobg306u0b2jrs.gif)

###前言
最近项目中会加入一个新的需求,那就是把图片和文字都实现那种跑马灯的效果,之前想的不就是一个TextView的跑马灯么,这个很好整的啊,并且开源的也是有这个的.这里给出这个TextView跑马灯的开源地址.[MarqueeView][1],但是这个并不符合我们的产品需求啊(需求如图,整个View都要进行滚动),找了许久也没找到自己能用的,看来只有自己去实现了。

###目标想法
目标很简单,就是只要实现这个效果,什么方式并没有限制啊,但是过程就是比较复杂的,有时候甚至充满了荆棘坎坷,这里想到的一种就是可不可以使用ListView,显示几个item通过方法去设定,然后通过一个线程来让item进行滚动起来,并且实现循环,这样不就是相当于实现了这个产品需求了么,想想也是哈,需求不就是这样的么,当前可见的item是可以滚动的,而且也是循环的.哈哈看来自己的想法是可以的,接下来就看如何去实现了。
###代码实现
既然是对ListView的自定义(谷歌官方的ListView并没有这个需求的相关函数和方法哈)

#####第一步：

```
    AutoScrollListView extends ListView{
        //然后重写几个构造方法
        public AutoScrollListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLoopRunnable = new LoopRunnable();
        mScroller = new Scroller(context, new AccelerateInterpolator());
        mInnerAdapter = new InnerAdapter();
    }

    public AutoScrollListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    }
```
    
#####第二步：
因为需要线程来控制滚动的时间,这里我们使用LoopRunnable(自定义的)
```
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
    
```
 //可以看到这里使用了 private Scroller mScroller;
 这里就不详细讲解为啥使用Scroller(可以实现想要的效果滑动),这里附上一篇Scroller的讲解的文章  [Android中滑屏实现----手把手教你如何实现触摸滑屏以及Scroller类详解][2]
还有两点,就是防止泄露内存,这个时候我们需要在View依附Window和接触Window的时候把线程移除
```
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i("AutoScrollListView", "onAttachedToWindow");
        //发送延时消息开始线程,也就是开始View的滚动
        postDelayed(mLoopRunnable, DALY_TIME);
        mAnimating = true;//设置动画
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i("AutoScrollListView", "onDetachedFromWindow");
        removeCallbacks(mLoopRunnable);//移除线程防止泄露内存
    }
```
这个时候我们一个重要的问题就是怎么去测量我们的滚动视图的高度
首先我们需要获取到视图的高度(因为视图的高度我们上层并不能首先获取到,因为我们要写一个方法后者接口,留给使用者去实现然后后去高度),因此这个时候我们写一个接口
```
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
```
然后在子类中去获取到(我们的布局View的高度是可知的,也就是固定的),然后子类中如下后去(根据自己的UI需求制定的高度进行设置)
```
@Override
	public int getListItemHeight(Context context) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
	}

```
然后我们通过获取到了滚动视图的高度之后,我们可以重写onMeasure方法进行测量了。
```
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
```
当然我们还需要重写computeScroll()方法(***由父视图调用用来请求子视图根据偏移值 mScrollX,mScrollY重新绘制*** )  为了实现偏移控制，一般自定义View/ViewGroup都需要重载该方法.
其实移动一个view的简单三部曲

* 第一、调用Scroller实例去产生一个偏移控制(对应于startScroll()方法)
* 第二、手动调用invalid()方法去重新绘制，剩下的就是在 computeScroll()里根据当前已经逝去的时间，获取当前应该偏移的坐标(由Scroller实例对应的computeScrollOffset()计算而得)，
* 第三、当前应该偏移的坐标，调用scrollBy()方法去缓慢移动至该坐标处
```
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

```
这里面有一个检测位置防止错乱的方法
```
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
```

到此差不多就能完成了滚动,接下来就是一些优化了,比如长按点击,点击事件,设置自动滑动,停止自动滑动,设置滚动延时时间。
点击事件(然后在相应的位置进行逻辑处理)
```
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
    
    
        class InnerOnItemLongClickListener implements OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                       int position, long id) {
            return mOutterOnItemLongClickListener != null && mInnerAdapter != null && !mIgnoreLongClick && mOutterOnItemLongClickListener.onItemLongClick(parent, view, (int) mInnerAdapter.getItemId(position), id);
        }

    }

    //长按事件处理
      @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        if (mInnerOnItemLongClickListener == null) {
            mInnerOnItemLongClickListener = new InnerOnItemLongClickListener();
        }
        mOutterOnItemLongClickListener = listener;
        super.setOnItemLongClickListener(mInnerOnItemLongClickListener);
    }

```
自动和停止滚动
```
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

```
滚动延时时间
```
 /**
     * 设置延时事件
     *
     * @param dalyTime 延时事件   单位: ms
     */
    public static void setDalyTime(int dalyTime) {
        DALY_TIME = dalyTime;
    }
```
好了,代码也就差不多这么多了,注释也是比较容易理解的,因为是对于ListView的自定义,那么用法和ListViwe的使用时大致类似,只要注意两个方法,手动实现AutoScrollListView.AutoScroll这个接口
```

	//获取到当前滚动视图的高度
	@Override
	public int getListItemHeight(Context context) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
	}


	@Override
	public int getVisiableCount() {
		return 2;  //显示滚动的item 的个数
	}

```
其他的都是类似ListView的用法了,这个地方代码就不进行贴附了,这里直接附上github地址,有需要的和想要学习的可以直接到git上获取,在此说明,小弟才疏学浅,并不能面面俱到,希望有问题互相交流,共同进步.对于效果图可以见开头的两个gif图片。

***重要的事情说三遍***
https://github.com/wuyinlei/MarqueeView<br>
https://github.com/wuyinlei/MarqueeView<br>
https://github.com/wuyinlei/MarqueeView<br>


 


  [1]: https://github.com/sfsheng0322/MarqueeView
  [2]: http://blog.csdn.net/qinjuning/article/details/7419207
