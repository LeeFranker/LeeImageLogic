package imagelogic.imageview;

import imagelogic.utils.ImageUtils;
import imagelogic.utils.Log;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;


/**
 * 自定义drawable对象，通过此对象的设置，可以判断是否需要回收drawable对象。
 * @author LeeFranker
 *
 */
public class CacheableDrawable extends BitmapDrawable {
	private static final String TAG = "CacheableDrawable";
	// 延迟回收时间点
	private final int UNUSED_DRAWABLE_RECYCLE_DELAY_MS = 2000;
	// 地址
	private final String mUrl;
	// 显示次数
	public int mDisplayingCount;
	// 是否正在显示中
	public boolean mHasBeenDisplayed;
	// 缓存次数
	public int mCacheCount;
	// 检查自身状态线程
	private Runnable mCheckStateRunnable;
	private static final Handler sHandler = new Handler(Looper.getMainLooper());
	// 字节大小
	private int mOriginalSize;
	// 图片宽高
	private int height, width;

	// 构造方法
	public CacheableDrawable(String url, Resources resources, Bitmap bitmap) {
		super(resources, bitmap);
		mUrl = url;
		mDisplayingCount = 0;
		mCacheCount = 0;
	}

	@Override
	public void draw(Canvas canvas) {
		try {
			super.draw(canvas);
		} catch (Exception e) {
			Log.e(TAG, "Cannot draw recycled bitmaps");
		}
	}

	public boolean isOriginalDrawable(int width, int height) {
		if (this.width != 0 && this.height != 0)
			return false;
		this.width=width;
		this.height=height;
		return true;
	}

	@SuppressLint("NewApi")
	public int getMemorySize() {
		int size = mOriginalSize;
		final Bitmap bitmap = getBitmap();
		if (ImageUtils.hasHoneycombMR1()) {
			size = bitmap.getByteCount();
		} else {
			size = bitmap.getRowBytes() * bitmap.getHeight();
		}
		mOriginalSize = size;
		return size;
	}

	// 获取地址
	public String getUrl() {
		return mUrl;
	}

	// bitmap是否回收
	public synchronized boolean hasValidBitmap() {
		Bitmap bitmap = getBitmap();
		return null != bitmap && !bitmap.isRecycled();
	}

	// 是否正在显示
	public synchronized boolean isBeingDisplayed() {
		return mDisplayingCount > 0;
	}

	// 是否正在被缓存使用
	public synchronized boolean isReferencedByCache() {
		return mCacheCount > 0;
	}

	// 标示状态
	public synchronized void setBeingUsed(boolean beingUsed) {
		if (beingUsed) {
			mDisplayingCount++;
			mHasBeenDisplayed = true;
		} else {
			mDisplayingCount--;
		}
		Log.d(TAG, mUrl + "，显示个数=" + mDisplayingCount + "，引用个数=" + mCacheCount + "，setBeingUsed=" + beingUsed);
		checkState();
	}

	// 设置缓存状态
	public synchronized void setCached(boolean added) {
		if (added) {
			mCacheCount++;
		} else {
			mCacheCount--;
		}
		Log.d(TAG, mUrl + "，显示个数=" + mDisplayingCount + "，引用个数=" + mCacheCount + "，added" + added);
		checkState();
	}

	// 取消检查状态线程
	private void cancelCheckStateCallback() {
		if (null != mCheckStateRunnable) {
			sHandler.removeCallbacks(mCheckStateRunnable);
			mCheckStateRunnable = null;
		}
	}

	public int getmDisplayingCount() {
		return mDisplayingCount;
	}

	public int getmCacheCount() {
		return mCacheCount;
	}

	public void setmDisplayingCount(int mDisplayingCount) {
		this.mDisplayingCount = mDisplayingCount;
	}

	public void setmCacheCount(int mCacheCount) {
		this.mCacheCount = mCacheCount;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	// 默认方法
	private void checkState() {
		checkState(true);
	}

	// 如果没有缓存引用，如果没有显示，回收bitmap
	private synchronized void checkState(final boolean ignoreBeenDisplayed) {
		if (!mHasBeenDisplayed) {
			return;
		}

		cancelCheckStateCallback();
		
		if (mDisplayingCount <= 0 && mCacheCount <= 0 && hasValidBitmap()) {
			if (mHasBeenDisplayed && ignoreBeenDisplayed) {
				Log.i(TAG, mUrl + "，立刻回收bitmap");
				getBitmap().recycle();
			} else {
				Log.i(TAG, mUrl + "，延迟回收bitmap");
				mCheckStateRunnable = new CheckStateRunnable(this);
				sHandler.postDelayed(mCheckStateRunnable, UNUSED_DRAWABLE_RECYCLE_DELAY_MS);
			}
		}

	}

	// 检查状态线程
	private static final class CheckStateRunnable extends CacheableWeakReferenceRunnable<CacheableDrawable> {

		public CheckStateRunnable(CacheableDrawable object) {
			super(object);
		}

		@Override
		public void run(CacheableDrawable object) {
			object.checkState(true);
		}
	}

}
