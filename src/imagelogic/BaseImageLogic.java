package imagelogic;

import imagelogic.ImageLogic.BitmapFinishCallback;
import imagelogic.display.ImageDisplayConfig;
import imagelogic.exception.TaskCancelledException;
import imagelogic.imageview.CacheableDrawable;
import imagelogic.imageview.ImageViewImpl;
import imagelogic.threads.ImageAsyncTask;
import imagelogic.utils.Log;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * 
 * @author LeeFranker
 * 
 */
public abstract class BaseImageLogic {

	protected static final String TAG = "ImageLogic";// Log

	protected Resources mResources;// 资源

	protected ImageLogicConfig mConfig;// 图片处理配置

	protected ImageCache mImageCache;// 磁盘对象

	protected boolean mExitTasksEarly = false;// 是否更早退出线程

	protected boolean mPauseWork = false;// 是否需要停止

	protected final Object mPauseWorkLock = new Object();// 停止锁

	// 判断ImageView是否重用
	@SuppressLint("UseSparseArrays")
	private final Map<Integer, String> cacheKeysForImageAwares = Collections
			.synchronizedMap(new HashMap<Integer, String>());

	// 是否滚动暂停
	protected boolean waitIfPaused(String key, ImageViewImpl imageview) {
		if (mPauseWork) {
			synchronized (mPauseWorkLock) {
				if (mPauseWork) {
					try {
						mPauseWorkLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return isTaskNotActual(key, imageview);
	}

	// 删除图片ID对应的唯一key
	protected void cancelDisplayTaskFor(ImageViewImpl imageView) {
		cacheKeysForImageAwares.remove(imageView.getImageViewId());
	}

	// 获取imageView的线程对象
	private static BitmapLoadAndDisplayTask getBitmapTaskFromImageView(
			ImageViewImpl imageview) {
		final ImageView iv = imageview.getImageView();
		if (iv != null) {
			final Drawable drawable = iv.getDrawable();
			if (drawable instanceof DefaultDrawable) {
				final DefaultDrawable asyncDrawable = (DefaultDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	// 检测imageView中是否已经有线程在运行
	public static boolean checkImageTask(String url, ImageViewImpl imageview) {
		final BitmapLoadAndDisplayTask bitmapWorkerTask = getBitmapTaskFromImageView(imageview);
		if (bitmapWorkerTask != null) {
			final String bitmapData = bitmapWorkerTask.uri;
			if (bitmapData == null || !bitmapData.equals(url)) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	public static boolean checkImageTaskForNet(String url,
			ImageViewImpl imageview) {
		final BitmapLoadAndDisplayTask bitmapWorkerTask = getBitmapTaskFromImageView(imageview);
		if (bitmapWorkerTask != null) {
			final String bitmapData = bitmapWorkerTask.uri;
			if (bitmapWorkerTask.isCacheThread()) {
				return true;
			} else if (bitmapData == null || !bitmapData.equals(url)) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	// 是否真正执行线程
	private boolean isTaskNotActual(String key, ImageViewImpl imageview) {
		return isViewCollected(imageview) || isViewReused(key, imageview);
	}

	// ImageView是否被回收
	private boolean isViewCollected(ImageViewImpl imageview) {
		if (imageview.isImageViewCollected()) {
			return true;
		}
		return false;
	}

	private boolean isViewReused(String key, ImageViewImpl imageview) {
		String currentCacheKey = getLoadingUriForView(imageview);
		if (currentCacheKey == null)
			return false;
		return !key.equals(currentCacheKey);
	}

	// 获取图片ID对应的唯一key
	protected String getLoadingUriForView(ImageViewImpl imageView) {
		return cacheKeysForImageAwares.get(imageView.getImageViewId());
	}

	// 存储图片ID对应的唯一key
	protected void prepareDisplayTaskFor(ImageViewImpl imageView, String key) {
		cacheKeysForImageAwares.put(imageView.getImageViewId(), key);
	}

	/**
	 * 清理线程
	 */
	protected void stop() {
		if (cacheKeysForImageAwares != null)
			cacheKeysForImageAwares.clear();
	}

	private void checkTaskNotActual(String key, ImageViewImpl imageview)
			throws TaskCancelledException {
		checkViewCollected(imageview);
		checkViewReused(key, imageview);
	}

	private void checkViewCollected(ImageViewImpl imageview)
			throws TaskCancelledException {
		if (isViewCollected(imageview)) {
			throw new TaskCancelledException();
		}
	}

	private void checkViewReused(String key, ImageViewImpl imageview)
			throws TaskCancelledException {
		if (isViewReused(key, imageview)) {
			throw new TaskCancelledException();
		}
	}

	// 图片下载显示的异步任务
	protected class BitmapLoadAndDisplayTask extends
			ImageAsyncTask<Object, Void, CacheableDrawable> {
		private ImageViewImpl imageview;
		private final ImageDisplayConfig displayConfig;
		private final BitmapFinishCallback bitmapFinishCallback;
		protected String uri;
		private volatile boolean mFromCache;
		private int retryCount = 1;
		private final WeakReference<ImageViewImpl> imageViewReference;

		public boolean isCacheThread() {
			return mFromCache;
		}

		public BitmapLoadAndDisplayTask(ImageViewImpl imageview,
				ImageDisplayConfig config,
				BitmapFinishCallback bitmapFinishCallback, boolean fromCache) {
			this.imageview = imageview;
			this.displayConfig = config;
			this.bitmapFinishCallback = bitmapFinishCallback;
			this.imageViewReference = new WeakReference<ImageViewImpl>(
					imageview);
			this.mFromCache = fromCache;
		}

		@Override
		protected CacheableDrawable doInBackground(Object... params) {
			if (mExitTasksEarly) {
				return null;
			}
			uri = String.valueOf(params[0]);
			Bitmap bitmap = null;
			CacheableDrawable drawable = null;
			if (waitIfPaused(uri, imageview)) {
				return null;
			}
			try {
				checkTaskIsCancel();
				if (mImageCache == null) {
					Log.e(TAG, "mImageCache==null");
				}
				if (mFromCache && mImageCache != null) {
					// 从磁盘获取bitmap对象
					bitmap = mImageCache.getBitmapFromDiskCache(uri);
				} else {
					// 从网络获取bitmap对象
					bitmap = tryLoadBitmap();
				}
				if (bitmap == null && mFromCache) {
					// 磁盘获取bitmap对象为空
					return null;
				}
				checkTaskIsCancel();
				if (bitmapFinishCallback != null) {
					// bitmap回调
					bitmap = bitmapFinishCallback.creatBitmap(bitmap);
				}
				if (bitmap != null) {
					drawable = new CacheableDrawable(uri, mResources, bitmap);
				}
				// 添加磁盘
				if (!mFromCache && bitmap != null && mImageCache != null) {
					mImageCache.addBitmapToDiskCache(uri, bitmap);
				}
				// 添加内存
				if (drawable != null && mImageCache != null) {
					mImageCache.addDrawableToMemoryCache(drawable, uri);
				}
				return drawable;
			} catch (TaskCancelledException e) {
				mFromCache = false;
				return null;
			}
		}

		@Override
		protected void onPostExecute(CacheableDrawable drawable) {
			if (mExitTasksEarly) {
				drawable = null;
			}
			imageview = getAttachedImageView();
			if (imageview == null) {
				return;
			}
			// 网络请求
			if (mFromCache && drawable == null) {
				loadFromNetwork(imageview);
				return;
			}
			if (drawable != null) {
				Log.i(TAG, "图片显示成功:" + uri);
				mConfig.displayer.loadCompletedisplay(imageview, drawable,
						displayConfig);
				if (bitmapFinishCallback != null)
					bitmapFinishCallback.showSuccess();
				cancelDisplayTaskFor(imageview);
			} else if (drawable == null) {
				Log.i(TAG, "图片显示失败:" + uri);
				mConfig.displayer.loadFailDisplay(imageview,
						displayConfig.getLoadingBitmap());
				if (bitmapFinishCallback != null) {
					bitmapFinishCallback.showError();
				}
			}
		}

		@Override
		protected void onCancelled(CacheableDrawable drawable) {
			super.onCancelled(drawable);
		}

		/**
		 * 网络请求
		 */
		private Bitmap tryLoadBitmap() throws TaskCancelledException {
			Bitmap bitmap = null;
			while (retryCount > 0) {
				Log.d(TAG, "开始从服务器获取图片数据...");
				bitmap = processBitmap(uri, displayConfig);
				if (bitmap != null) {
					Log.i(TAG, "服务器获取图片数据ok:" + retryCount);
					break;
				}
				retryCount--;
				checkTaskIsCancel();
			}
			return bitmap;
		}

		/**
		 * 磁盘加载失败，从网络从新加载
		 */
		private void loadFromNetwork(ImageViewImpl imageview) {
			if (checkImageTaskForNet(uri, imageview)) {
				BitmapLoadAndDisplayTask task = new BitmapLoadAndDisplayTask(
						imageview, displayConfig, bitmapFinishCallback, false);
				final DefaultDrawable defaultDrawable = new DefaultDrawable(
						mResources, displayConfig.getLoadingBitmap(), task);
				imageview.setImageDrawable(defaultDrawable);
				task.executeOnExecutor(ImageAsyncTask.NET_THREAD_EXECUTOR, uri,
						uri);
			}
		}

		/**
		 * check task need cancel,if need throw TaskCancelledException
		 */
		private void checkTaskIsCancel() throws TaskCancelledException {
			checkTaskNotActual(uri, imageview);
			if (mExitTasksEarly || isCancelled()) {
				throw new TaskCancelledException();
			}
		}

		/**
		 * 获取线程匹配的imageView,防止出现闪动的现象
		 */
		private ImageViewImpl getAttachedImageView() {
			final ImageViewImpl imageview = imageViewReference.get();
			final BitmapLoadAndDisplayTask bitmapWorkerTask = getBitmapTaskFromImageView(imageview);
			if (this == bitmapWorkerTask)
				return imageview;
			return null;
		}

	}

	public abstract Bitmap processBitmap(String uri,
			ImageDisplayConfig displayConfig);

	// 默认图片
	protected static class DefaultDrawable extends BitmapDrawable {
		private WeakReference<BitmapLoadAndDisplayTask> bitmapWorkerTaskReference;

		public DefaultDrawable(Resources res, Bitmap bitmap,
				BitmapLoadAndDisplayTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapLoadAndDisplayTask>(
					bitmapWorkerTask);
		}

		public BitmapLoadAndDisplayTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}

		public void setBitmapLoadAndDisplayTask(
				BitmapLoadAndDisplayTask bitmapWorkerTask) {
			bitmapWorkerTaskReference = new WeakReference<BitmapLoadAndDisplayTask>(
					bitmapWorkerTask);
		}
	}

}
