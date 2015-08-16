package org.lance.widget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.lance.main.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

/**
 * 这是一个完全自定义的视图,只是一个处女作,不过它完成了几乎大部分自定义滚动视图所需要的操作 这可以增强Android视图的理解,关于容器会更简单一点
 * 
 * @author ganchengkai
 * 
 */
@SuppressLint("ClickableViewAccessibility")
public class HorizontalIconView extends View {
	private static final String TAG = "HorizontalIconView";
	// 无效的手指id
	private static final int INVALID_POINTER = MotionEvent.INVALID_POINTER_ID;

	private IconOnClickListener mListener;
	// 绘制滑过头的光晕效果---左边
	private EdgeEffectCompat mEdgeEffectLeft;
	// 右边
	private EdgeEffectCompat mEdgeEffectRight;

	// 跟踪手指操作
	private int mActivePointerId = INVALID_POINTER;

	private List<Drawable> mDrawables;// 绘制列表

	// 绘制对象的点击区域
	private final List<Rect> mIconPositions = new ArrayList<Rect>();

	// 每个图标的大小---像素值
	private int mIconSize;

	// 每个图标间的间距
	private int mIconSpacing;

	// 跟踪手指在屏幕上是否被拖动
	private boolean mIsBeginDragged;

	// 手指猛划的最大速率---像素/秒
	private int mMaximumVelocity;

	// 手指猛划的最小速率---像素/秒
	private int mMinimumVelocity;

	// 猛划最大的过渡滚动值---由设备决定
	private int mOverflingDistance;

	// 滚动最大的过渡滚动值---由设备决定
	private int mOverscrollDistance;

	// 滑动后手指最后的落点---可判断是否有滑动事件
	private float mPreviousX = 0;

	// 视图可滚动的像素值---(总宽度-可见宽度)
	private int mScrollRange;

	// 触摸事件需要的像素---由设备决定
	private int mTouchSlop;

	// 触摸事件的速度跟踪器
	private VelocityTracker mVelocityTracker;

	// 用于处理平滑滚动
	private OverScroller mScroller;

	// 不需要绘制的图标数---跳过这些绘制可以提高效率
	private int mSkippedIconCount = 0;

	public HorizontalIconView(Context context) {
		super(context);
		init(context);
	}

	public HorizontalIconView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public HorizontalIconView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		final Resources res = context.getResources();
		mIconSize = res.getDimensionPixelSize(R.dimen.icon_size);
		mIconSpacing = res.getDimensionPixelSize(R.dimen.icon_spacing);

		final ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();
		mMinimumVelocity = config.getScaledMinimumFlingVelocity();
		mMaximumVelocity = config.getScaledMaximumFlingVelocity();
		mOverflingDistance = config.getScaledOverflingDistance();
		mOverscrollDistance = config.getScaledOverscrollDistance();

		setWillNotDraw(false);// 设置本视图需要绘制

		mEdgeEffectLeft = new EdgeEffectCompat(context);
		mEdgeEffectRight = new EdgeEffectCompat(context);

		mScroller = new OverScroller(context);
		setFocusable(true);
	}

	public void setIconListener(IconOnClickListener iconListener) {
		this.mListener = iconListener;
	}

	/**
	 * 设置绘制对象
	 * 
	 * @param drawables
	 */
	public void setDrawables(List<Drawable> drawables) {
		if (mDrawables == null) {
			if (drawables == null) {
				return;
			}
			requestLayout();
		} else if (drawables == null) {
			requestLayout();// 重新布局
			mDrawables = null;
			return;
		} else if (mDrawables.size() == drawables.size()) {
			invalidate();// 重新绘制
		} else {
			requestLayout();
		}

		mDrawables = new ArrayList<Drawable>(drawables);
		mIconPositions.clear();
	}

	/**
	 * 测量视图的高度
	 * 
	 * @param measureSpec
	 * @return
	 */
	private int measureHeight(int measureSpec) {
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		int result = 0;
		if (specMode == MeasureSpec.EXACTLY) {// 如果尺寸是确定的,返回确定的值
			result = specSize;
		} else {
			// 计算实际需要的高度
			result = mIconSize + getPaddingTop() + getPaddingBottom();
			if (specMode == MeasureSpec.AT_MOST) {// 取最小的值---取最小的---因为不能超过父视图分配的最大可用空间
				result = Math.min(result, specSize);
			}
		}

		return result;
	}

	/**
	 * 测量视图的宽度
	 * 
	 * @param measureSpec
	 * @return
	 */
	private int measureWidth(int measureSpec) {
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		final int icons = mDrawables == null ? 0 : mDrawables.size();
		final int dividerSpace;// 计算分割空间
		if (icons <= 1) {
			dividerSpace = 0;
		} else {
			dividerSpace = (icons - 1) * mIconSpacing;
		}

		// 绘制对象尺寸
		final int iconSpace = mIconSize * icons;
		// 计算内容空间--绘制对象+分割空间+内边距
		final int maxSize = dividerSpace + iconSpace + getPaddingLeft()
				+ getPaddingRight();

		int result = 0;
		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(maxSize, specSize);
			} else {// 容纳的数值
				result = maxSize;
			}
		}

		if (maxSize > result) {
			mScrollRange = maxSize - result;
		} else {
			mScrollRange = 0;
		}

		return result;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec),
				measureHeight(heightMeasureSpec));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mDrawables == null || mDrawables.isEmpty()) {
			return;
		}

		final int width = getWidth();
		final int paddingBottom = getPaddingBottom();
		final int paddingLeft = getPaddingLeft();
		final int paddingTop = getPaddingTop();

		final int leftEdge = getScrollX();// 视图绘制的左边像素
		final int rightEdge = leftEdge + width;// 视图绘制的右边像素

		int left = paddingLeft;// 左边
		int top = paddingTop;// 顶点

		mSkippedIconCount = 0;
		final int iconCount = mDrawables.size();
		for (int i = 0; i < iconCount; i++) {
			if (left + mIconSize < leftEdge) {// 在绘制起点左边的绘制对象不可见--不需要绘制
				left += mIconSize + mIconSpacing;
				mSkippedIconCount++;
				continue;
			}
			if (left > rightEdge) {// 超过右边的对象不需要继续绘制
				break;
			}

			final Drawable icon = mDrawables.get(i);
			icon.setBounds(left, top, left + mIconSize, top + mIconSize);
			icon.draw(canvas);

			// 记录绘制图标的位置---以此来跟踪点击事件
			final int drawnPosition = i - mSkippedIconCount;
			if (drawnPosition + 1 > mIconPositions.size()) {
				final Rect rect = icon.copyBounds();// 拷贝drawable的边界信息到列表中
				mIconPositions.add(rect);
			} else {
				final Rect rect = mIconPositions.get(drawnPosition);
				icon.copyBounds(rect);// 拷贝边界信息到矩形中
			}

			left += mIconSize + mIconSpacing;
		}

		if (mEdgeEffectLeft != null) {
			if (!mEdgeEffectLeft.isFinished()) {//如果还处于拉动状态---绘制过渡的光晕效果
				// 绘制光晕之后要恢复到上一状态
				final int restoreCount = canvas.save();
				final int height = getHeight() - paddingTop - paddingBottom;

				//因为EdgeEffectCompat默认是对上下滑动的处理,所以绘制之前需要先旋转
				canvas.rotate(270);
				canvas.translate(-height + paddingTop, Math.min(0, leftEdge));
				// 设置绘制光晕的大小
				mEdgeEffectLeft.setSize(height, getWidth());
				if (mEdgeEffectLeft.draw(canvas)) {//绘制结束之后通知更新
					postSelf();
				}
				canvas.restoreToCount(restoreCount);
			}

			if (!mEdgeEffectRight.isFinished()) {
				final int restoreCount = canvas.save();
				final int height = getHeight() - paddingTop - paddingBottom;
				canvas.rotate(90);
				canvas.translate(-paddingTop,
						-(Math.max(mScrollRange, leftEdge) + width));

				mEdgeEffectRight.setSize(height, width);
				if (mEdgeEffectRight.draw(canvas)) {
					postSelf();
				}
				canvas.restoreToCount(restoreCount);
			}
		}
	}

	@Override
	// 处理猛划期间的滚动更新
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {// 判断是否结束滚动--true-动画没有结束
			int oldX = getScrollX();// 当前视图的X位置
			int x = mScroller.getCurrX();// 滚动到得当前X位置

			if (oldX != x) {
				// 调用次方法必须覆盖处理 onOverScrolled 方法
				overScrollBy(x - oldX, 0, oldX, 0, mScrollRange, 0,
						mOverflingDistance, 0, false);
				onScrollChanged(x, 0, oldX, 0);// 通知内部视图执行了滚动响应
				if (x < 0 && oldX >= 0) {// 如果向左滚动过渡---绘制左边的光晕效果
					mEdgeEffectLeft.onAbsorb((int) mScroller.getCurrVelocity());
				} else if (x > mScrollRange && oldX <= mScrollRange) {// 右边的效果
					mEdgeEffectRight
							.onAbsorb((int) mScroller.getCurrVelocity());
				}

			}
		}
	}

	@Override
	protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
			boolean clampedY) {
		if (mScroller.isFinished()) {// 如果动画已经结束---更新到指定的位置
			super.scrollTo(scrollX, scrollY);
		} else {
			setScrollX(scrollX);// 执行更新滚动---Y方向不需要滚动
			if (clampedX) {// 如果允许操作边界
				// 回弹到一个有效的位置
				mScroller.springBack(scrollX, 0, 0, mScrollRange, 0, 0);
			}
		}
	}

	/**
	 * 执行猛划操作
	 * 
	 * @param velocity
	 */
	private void fling(int velocity) {
		if (mScrollRange == 0) {
			return;
		}

		// 最大过渡猛划的X值
		final int halfWidth = (getWidth() - getPaddingLeft() - getPaddingRight()) / 2;
		// 出发一个猛划的操作
		mScroller.fling(getScrollX(), 0, velocity, 0, 0, mScrollRange, 0, 0,
				halfWidth, 0);
		invalidate();
	}

	// 当用户有两个手指在屏幕上,抬起一个手指时被调用
	private void onSecondaryPointerUp(MotionEvent event) {
		final int pointerIndex = MotionEventCompat.getActionIndex(event);
		final int pointerId = MotionEventCompat.getPointerId(event,
				pointerIndex);

		if (pointerId != mActivePointerId) {// 跟踪指针并清除速率
			// 只处理两个手指的分配-更多手指的分配不处理
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mPreviousX = event.getX(newPointerIndex);// 更新X坐标值
			// 重新分配手指标识符
			mActivePointerId = event.getPointerId(newPointerIndex);
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}

	@Override
	// 这是滚动处理最难的部分---需要跟踪所有变量所处的状态
	public boolean onTouchEvent(MotionEvent event) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);

		// 取得事件动作
		final int action = MotionEventCompat.getActionMasked(event);
		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			System.out.println("ACTION_DOWN");
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}

			// 获取第一个手指的X和手指id
			mPreviousX = (int) MotionEventCompat.getX(event, 0);
			mActivePointerId = MotionEventCompat.getPointerId(event, 0);
		}
			break;
		case MotionEvent.ACTION_MOVE: {
			final int activePointerIndex = MotionEventCompat.findPointerIndex(
					event, mActivePointerId);
			if (activePointerIndex == INVALID_POINTER) {
				Log.e(TAG, "Invalid pointerId = " + mActivePointerId
						+ " in onTouchEvent");
				break;
			}

			// 获取第一个手指的X值
			final int x = (int) MotionEventCompat.getX(event, 0);
			int deltaX = (int) (mPreviousX - x);
			if (!mIsBeginDragged && Math.abs(deltaX) > mTouchSlop) {
				mIsBeginDragged = true;
//				if (deltaX > 0) {// 适度控制偏移量
//					deltaX -= mTouchSlop;
//				} else {
//					deltaX += mTouchSlop;
//				}
			}

			if (mIsBeginDragged) {
				mPreviousX = x;

				final int oldX = getScrollX();
				final int range = mScrollRange;
				if (overScrollBy(deltaX, 0, oldX, 0, range, 0,
						mOverscrollDistance, 0, true)) {
					// 如果滚动达到了边界---清除速率
					mVelocityTracker.clear();
				}

				if (mEdgeEffectLeft != null) {
					final int pulledToX = oldX + deltaX;
					if (pulledToX < 0) {
						//计算过渡拖动的能量
						mEdgeEffectLeft.onPull((float) deltaX / getWidth());
						if (!mEdgeEffectRight.isFinished()) {
							mEdgeEffectRight.onRelease();//释放光晕效果
						}
					} else if (pulledToX > range) {
						mEdgeEffectRight.onPull((float) deltaX / getWidth());
						if (!mEdgeEffectLeft.isFinished()) {
							mEdgeEffectLeft.onRelease();
						}
					}

					if (!mEdgeEffectLeft.isFinished()
							|| !mEdgeEffectRight.isFinished()) {
						postSelf();
					}
				}
			}
		}
			break;
		case MotionEvent.ACTION_UP: {
			if (mIsBeginDragged) {// 手势事件--计算速率
				mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				// 获取水平方向上的速率
				int initialVelocity = (int) mVelocityTracker
						.getXVelocity(mActivePointerId);
				//如果手势大于猛划的最低触发速率---执行猛划
				if (Math.abs(initialVelocity) > mMinimumVelocity) {
					fling(-initialVelocity);
				} else {//回弹
					if (mScroller.springBack(getScrollX(), 0, 0, mScrollRange,
							0, 0)) {
						postSelf();
					}
				}

				mActivePointerId = INVALID_POINTER;
				mIsBeginDragged = false;
				mVelocityTracker.recycle();// 复用该对象
				mVelocityTracker = null;

				if (mEdgeEffectLeft != null) {
					mEdgeEffectLeft.onRelease();
					mEdgeEffectRight.onRelease();
				}
			} else {// 点击事件
				final int activePointerIndex = event
						.findPointerIndex(mActivePointerId);
				if (activePointerIndex == INVALID_POINTER) {
					return false;
				}

				final int x = (int) event.getX(activePointerIndex)
						+ getScrollX();
				final int y = (int) event.getY(activePointerIndex);

				for (int i = 0; i < mIconPositions.size(); i++) {
					Rect rect = mIconPositions.get(i);
					if (rect.contains(x, y)) {
						final int position = i + mSkippedIconCount;
						// 得到点击的位置,回调到主界面
						Log.i(TAG, "Clicked position " + position
								+ "; rect count:" + mIconPositions.size());
						if (mListener != null) {
							mListener.onItemClick(position);
						}
						break;
					}
				}
			}
		}
			break;
		case MotionEvent.ACTION_CANCEL: {
			if (mIsBeginDragged) {
				// 如果需要回弹,那么执行动画更新
				if (mScroller
						.springBack(getScrollX(), 0, 0, mScrollRange, 0, 0)) {
					postSelf();
				}

				mActivePointerId = INVALID_POINTER;
				mIsBeginDragged = false;
				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}

				if (mEdgeEffectLeft != null) {
					mEdgeEffectLeft.onRelease();
					mEdgeEffectRight.onRelease();
				}
			}
		}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(event);// 切换手指指针
			break;
		}
		return true;
	}

	private void postSelf() {
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			// 最低API 16, 这里需要使用反射调用,否则app最低级别需要达到16,但是我们要兼容更低级别的设备就只能反射调用
			try {//在API 14级别需要使用反射调用该方法
				Class<View> clazz = View.class; 
				Method method = clazz.getDeclaredMethod("postInvalidateOnAnimation"); 
				method.invoke(this);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} else {
			postInvalidate();
		}
	}

	public static interface IconOnClickListener {
		// 回调位置
		public void onItemClick(int position);

	}

}
