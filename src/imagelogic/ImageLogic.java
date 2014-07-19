package imagelogic;

import imagelogic.disk.BitmapDiskCache;
import imagelogic.display.ImageDisplayConfig;
import imagelogic.display.ImageDisplayer;
import imagelogic.display.ImageSimpleDisplayer;
import imagelogic.download.ImageDownloader;
import imagelogic.download.ImageSimpleHttpDownloader;
import imagelogic.imageview.CacheableDrawable;
import imagelogic.imageview.ImageViewImpl;
import imagelogic.threads.ImageAsyncTask;
import imagelogic.utils.ImageUtils;
import imagelogic.utils.Log;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.ImageView;

/**
 * 图片操作类
 * 
 * @author LeeFranker
 * 
 */
@SuppressLint("UseSparseArrays")
public class ImageLogic extends BaseImageLogic {

	private Context mContext;// 上下文

	private static ImageLogic mImageLogic; // 静态对象

	// 默认图片对应显示配置
	private HashMap<Integer, ImageDisplayConfig> mResConfigMap = new HashMap<Integer, ImageDisplayConfig>();
	private HashMap<String, ImageDisplayConfig> mPathConfigMap = new HashMap<String, ImageDisplayConfig>();

	// 构造方法
	private ImageLogic(Context context) {
		mContext = context;
		mResources = context.getResources();
		mConfig = new ImageLogicConfig(context);
		// 配置磁盘缓存目录路径
		configDiskCachePath(ImageUtils.getDiskCacheDir(context)
				.getAbsolutePath());
		configDisplayer(new ImageSimpleDisplayer());// 配置显示
		configDownlader(new ImageSimpleHttpDownloader());// 配置下载
	}

	/**
	 * @Title: create
	 * @Description: 获取图片处理类对象
	 * @param @param ctx 上下文
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public static ImageLogic create(Context ctx) {
		if (mImageLogic == null) {
			mImageLogic = new ImageLogic(ctx.getApplicationContext());
			mImageLogic.init();
		}
		return mImageLogic;
	}

	/**
	 * @Title: create
	 * @Description: 获取图片处理类对象
	 * @param @param context 上下文
	 * @param @param diskCachePath 磁盘缓存目录路径
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public static ImageLogic create(Context context, String diskCachePath) {
		if (mImageLogic == null) {
			mImageLogic = new ImageLogic(context.getApplicationContext());
			mImageLogic.configDiskCachePath(diskCachePath);
			mImageLogic.init();
		}
		return mImageLogic;

	}

	/**
	 * @Title: create
	 * @Description: 获取图片处理类对象
	 * @param @param ctx
	 * @param @param diskCachePath
	 * @param @param memoryCacheSizePercent
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public static ImageLogic create(Context ctx, String diskCachePath,
			float memoryCacheSizePercent) {
		if (mImageLogic == null) {
			mImageLogic = new ImageLogic(ctx.getApplicationContext());
			mImageLogic.configDiskCachePath(diskCachePath);
			mImageLogic.configMemoryCachePercent(memoryCacheSizePercent);
			mImageLogic.init();
		}

		return mImageLogic;
	}

	/**
	 * @Title: create
	 * @Description: 获取图片处理类对象
	 * @param @param ctx
	 * @param @param diskCachePath
	 * @param @param memoryCacheSize
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public static ImageLogic create(Context ctx, String diskCachePath,
			int memoryCacheSize) {
		if (mImageLogic == null) {
			mImageLogic = new ImageLogic(ctx.getApplicationContext());
			mImageLogic.configDiskCachePath(diskCachePath);
			mImageLogic.configMemoryCacheSize(memoryCacheSize);
			mImageLogic.init();
		}

		return mImageLogic;
	}

	/**
	 * @Title: create
	 * @Description: 获取图片处理类对象
	 * @param @param ctx
	 * @param @param diskCachePath
	 * @param @param memoryCacheSizePercent
	 * @param @param diskCacheSize
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public static ImageLogic create(Context ctx, String diskCachePath,
			float memoryCacheSizePercent, int diskCacheSize) {
		if (mImageLogic == null) {
			mImageLogic = new ImageLogic(ctx.getApplicationContext());
			mImageLogic.configDiskCachePath(diskCachePath);
			mImageLogic.configMemoryCachePercent(memoryCacheSizePercent);
			mImageLogic.configDiskCacheSize(diskCacheSize);
			mImageLogic.init();
		}

		return mImageLogic;
	}

	/**
	 * @Title: create
	 * @Description: 获取图片处理类对象
	 * @param @param ctx
	 * @param @param diskCachePath
	 * @param @param memoryCacheSize
	 * @param @param diskCacheSize
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public static ImageLogic create(Context ctx, String diskCachePath,
			int memoryCacheSize, int diskCacheSize) {
		if (mImageLogic == null) {
			mImageLogic = new ImageLogic(ctx.getApplicationContext());
			mImageLogic.configDiskCachePath(diskCachePath);
			mImageLogic.configMemoryCacheSize(memoryCacheSize);
			mImageLogic.configDiskCacheSize(diskCacheSize);
			mImageLogic.init();
		}

		return mImageLogic;
	}

	/**
	 * @Title: configDownlader
	 * @Description: 设置下载类对象，可以设置自己另外实现的下载类
	 * @param @param downlader
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public ImageLogic configDownlader(ImageDownloader downlader) {
		mConfig.downloader = downlader;
		return this;
	}

	/**
	 * @Title: configDisplayer
	 * @Description: 设置显示类对象，可以设置自己另外实现的显示类
	 * @param @param displayer
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public ImageLogic configDisplayer(ImageDisplayer displayer) {
		mConfig.displayer = displayer;
		return this;
	}

	/**
	 * @Title: configCalculateBitmapSizeWhenDecode
	 * @Description: 配置加载图片的时候是否计算图片大小，如果配置为真，则decode图片的时候可能会造成out of
	 *               memory的异常，建议计算图片大小
	 * @param @param neverCalculate
	 * @param @return
	 * @return ImageLogic
	 * @throws
	 */
	public ImageLogic configCalculateBitmapSizeWhenDecode(boolean neverCalculate) {
		if (mConfig != null && mConfig.bitmapProcess != null)
			mConfig.bitmapProcess.configCalculateBitmap(neverCalculate);
		return this;
	}

	// 配置磁盘缓存路径
	private ImageLogic configDiskCachePath(String strPath) {
		if (!TextUtils.isEmpty(strPath)) {
			mConfig.cachePath = strPath;
		}
		return this;
	}

	// 配置内存缓存大小(2MB以上有效)
	private ImageLogic configMemoryCacheSize(int size) {
		mConfig.memCacheSize = size;
		return this;
	}

	// 设置应缓存的在APK总内存的百分比，优先级大于configMemoryCacheSize
	private ImageLogic configMemoryCachePercent(float percent) {
		mConfig.memCacheSizePercent = percent;
		return this;
	}

	// 设置磁盘缓存大小（5MB以上有效）
	private ImageLogic configDiskCacheSize(int size) {
		mConfig.diskCacheSize = size;
		return this;
	}

	// 图片逻辑对象初始化
	private ImageLogic init() {
		mConfig.init();
		ImageCache.ImageCacheParams imageCacheParams = new ImageCache.ImageCacheParams(
				mConfig.cachePath);
		if (mConfig.memCacheSizePercent > 0.05
				&& mConfig.memCacheSizePercent < 0.8) {
			imageCacheParams.setMemCacheSizePercent(mContext,
					mConfig.memCacheSizePercent);
		} else {
			// 2MB以上有效
			if (mConfig.memCacheSize > 1024 * 1024 * 2) {
				imageCacheParams.setMemCacheSize(mConfig.memCacheSize);
			} else {
				// 设置默认的内存缓存大小
				imageCacheParams.setMemCacheSizePercent(mContext, 0.15f);
			}
		}
		// 5MB以上有效
		if (mConfig.diskCacheSize > 1024 * 1024 * 5) {
			imageCacheParams.setDiskCacheSize(mConfig.diskCacheSize);
		}
		// 初始化缓存对象
		mImageCache = new ImageCache(imageCacheParams);
		// 初始化磁盘缓存对象
		initCache();
		return this;
	}

	// 显示图片
	public void display(ImageView imageView, String uri) {
		doDisplay(imageView, uri, null, null);
	}

	// 显示图片
	public void display(ImageView imageView, String uri,
			ImageDisplayConfig config) {
		doDisplay(imageView, uri, config, null);
	}

	// 显示图片
	public void display(ImageView imageView, String uri,
			BitmapFinishCallback bitmapFinishCallback) {
		doDisplay(imageView, uri, null, bitmapFinishCallback);
	}

	// 显示图片
	public void display(ImageView imageView, String uri,
			ImageDisplayConfig config, BitmapFinishCallback bitmapFinishCallback) {
		doDisplay(imageView, uri, config, bitmapFinishCallback);
	}

	// 显示图片
	public void display(ImageView imageView, String uri, int loadingResId) {
		ImageDisplayConfig displayConfig = mResConfigMap.get(loadingResId);
		try {
			if (displayConfig == null) {
				displayConfig = getDisplayConfig();
				displayConfig.setLoadingBitmap(ImageUtils.getBitmapFromId(
						mContext, loadingResId));
				mResConfigMap.put(loadingResId, displayConfig);
			}
			doDisplay(imageView, uri, displayConfig, null);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
	}


	// 显示图片
	public void display(ImageView imageView, String uri, int loadingResId,
			BitmapFinishCallback bitmapFinishCallback) {
		ImageDisplayConfig displayConfig = mResConfigMap.get(loadingResId);
		try {
			if (displayConfig == null) {
				displayConfig = getDisplayConfig();
				displayConfig.setLoadingBitmap(ImageUtils.getBitmapFromId(
						mContext, loadingResId));
				mResConfigMap.put(loadingResId, displayConfig);
			}
			doDisplay(imageView, uri, displayConfig, bitmapFinishCallback);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
	}

	// 显示服务器图片
	private void doDisplay(ImageView iv, String uri,
			ImageDisplayConfig displayConfig,
			BitmapFinishCallback bitmapFinishCallback) {
		if (iv == null) {
			Log.e(TAG, "imageview == null");
			return;
		}

		ImageViewImpl imageview = null;
		if (imageview == null) {
			imageview = new ImageViewImpl(iv);
		}

		if (displayConfig == null) {
			displayConfig = mConfig.defaultDisplayConfig;
		}

		int maxWidth = displayConfig.getBitmapWidth();
		int maxHeight = displayConfig.getBitmapHeight();
		int width = imageview.getImageViewWidth(maxWidth);
		int height = imageview.getImageViewHeight(maxHeight);
		imageview.initView(width, height);
		Log.d(TAG, "doDisplay-width:" + width);
		Log.d(TAG, "doDisplay_height:" + height);

		prepareDisplayTaskFor(imageview, uri);
		Log.d(TAG, "doDisplay:" + uri);

		CacheableDrawable drawable = null;
		if (mImageCache != null) {
			drawable = mImageCache.getDrawableFromMemCache(uri);
		}

		if (drawable != null && drawable.hasValidBitmap()) {

			imageview.setImageDrawable(drawable);

			if (bitmapFinishCallback != null)
				bitmapFinishCallback.showSuccess();
		} else if (checkImageTask(uri, imageview)) {
			Log.d(TAG, "默认图片加载成功:" + uri);

			BitmapLoadAndDisplayTask task = new BitmapLoadAndDisplayTask(
					imageview, displayConfig, bitmapFinishCallback, true);
			DefaultDrawable defaultDrawable = new DefaultDrawable(mResources,
					displayConfig.getLoadingBitmap(), task);

			imageview.setImageDrawable(defaultDrawable);
			if (!mExitTasksEarly) {
				task.execute(uri);
			}
		}
	}

	// 下载配置图片
	public void downloadImage(String url) {
		new DownloadImageTask(url, mConfig.defaultDisplayConfig).execute();
	}

	// 默认显示配置
	public ImageDisplayConfig getDisplayConfig() {
		ImageDisplayConfig config = new ImageDisplayConfig();
		config.setAnimation(mConfig.defaultDisplayConfig.getAnimation());
		config.setAnimationType(mConfig.defaultDisplayConfig.getAnimationType());
		config.setBitmapHeight(mConfig.defaultDisplayConfig.getBitmapHeight());
		config.setBitmapWidth(mConfig.defaultDisplayConfig.getBitmapWidth());
		config.setLoadingBitmap(mConfig.defaultDisplayConfig.getLoadingBitmap());
		return config;
	}

	// 初始缓存
	private void initDiskCacheInternal() {
		BitmapDiskCache diskCache = null;
		if (mImageCache != null) {
			diskCache = mImageCache.initDiskCache();
		}
		if (mConfig.bitmapProcess != null && diskCache != null) {
			mConfig.bitmapProcess.initHttpDiskCache(diskCache);
		}
	}

	// 清空缓存
	private void clearMemoryCache() {
		if (mImageCache != null) {
			mImageCache.clearMemoryCache();
		}
	}

	// 整理缓存
	private void trimMemoryCache() {
		if (mImageCache != null) {
			mImageCache.trimMemoryCache();
		}
	}

	// 清理所有缓存
	public void clearAllDiskCaches() {
		if (mImageCache != null) {
			mImageCache.clearDiskCache();
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.clearDiskCache();
		}
	}

	// 下载图片
	public Bitmap processBitmap(String uri, ImageDisplayConfig config) {
		if (mConfig != null && mConfig.bitmapProcess != null) {
			return mConfig.bitmapProcess.processBitmap(uri, config);
		}
		return null;
	}

	// 下载图片
	private boolean downloadImage(String uri, ImageDisplayConfig config) {
		if (mConfig != null && mConfig.bitmapProcess != null) {
			return mConfig.bitmapProcess.downloadImage(uri, config);
		}
		return false;
	}

	// 是否更早退出线程
	private void setExitTasksEarly(boolean exitTasksEarly) {
		mExitTasksEarly = exitTasksEarly;
	}

	/**
	 * resume
	 */
	public void resume() {
		Log.d(TAG, "resume-恢复下载");
		setExitTasksEarly(false);
		pauseWork(false);// 防止线程被锁死，外部调用滚动监听处理有问题。
	}

	/**
	 * pause
	 */
	public void pause() {
		Log.d(TAG, "pause-停止下载");
		stop();
		setExitTasksEarly(true);
	}

	/**
	 * 程序退出的时候调用这个方法
	 */
	public void exitApp(boolean exitTasksEarly) {
		mExitTasksEarly = exitTasksEarly;
		stop();
		if (mResConfigMap != null)
			mResConfigMap.clear();
		if (mPathConfigMap != null)
			mPathConfigMap.clear();
		cleanConfigMap();
		if (exitTasksEarly) {
			pauseWork(false);
			closeCache();
		}
	}

	/**
	 * 当内存不够的时候调用
	 */
	public void onLowMemory() {
		Log.d(TAG, "onLowMemory-内存不够");
		trimCache();
	}

	/**
	 * 初始化缓存
	 */
	private void initCache() {
		new Thread(new CacheExecutecTask(
				CacheExecutecTask.MESSAGE_INIT_DISK_CACHE)).start();

	}

	/**
	 * 关闭缓存
	 */
	private void closeCache() {
		new Thread(new CacheExecutecTask(CacheExecutecTask.MESSAGE_CLOSE))
				.start();
	}

	/**
	 * 整理缓存
	 */
	private void trimCache() {
		new Thread(new CacheExecutecTask(CacheExecutecTask.MESSAGE_TRIM))
				.start();
	}

	/**
	 * 暂停正在加载的线程
	 */
	public void pauseWork(boolean pauseWork) {
		Log.d(TAG, "listview滚动，是否停止工作:" + pauseWork);
		synchronized (mPauseWorkLock) {
			mPauseWork = pauseWork;
			if (!mPauseWork) {
				mPauseWorkLock.notifyAll();
			}
		}
	}

	/**
	 * 清空mConfigMap
	 */
	public void cleanConfigMap() {
		if (mResConfigMap != null) {
			mResConfigMap.clear();
		}
	}

	// 缓存处理的异步任务
	private class CacheExecutecTask implements Runnable {
		public static final int MESSAGE_INIT_DISK_CACHE = 1;// 初始化磁盘缓存
		public static final int MESSAGE_TRIM = 2;// 整理磁盘缓存
		public static final int MESSAGE_CLOSE = 3;// 关闭磁盘缓存

		private int mTaskState = 0;

		public CacheExecutecTask(int TaskState) {
			mTaskState = TaskState;
		}

		@Override
		public void run() {
			switch (mTaskState) {
			case MESSAGE_INIT_DISK_CACHE:
				initDiskCacheInternal();
				break;
			case MESSAGE_TRIM:
				trimMemoryCache();
				break;
			case MESSAGE_CLOSE:
				clearMemoryCache();
				break;
			}
		}
	}

	private class DownloadImageTask extends
			ImageAsyncTask<Object, Void, Boolean> {
		private String url;
		private ImageDisplayConfig displayConfig;

		public DownloadImageTask(String url, ImageDisplayConfig config) {
			this.url = url;
			this.displayConfig = config;
		}

		@Override
		protected Boolean doInBackground(Object... params) {
			if (url == null || url.equals("")) {
				return false;
			}
			boolean result = downloadImage(url, displayConfig);
			String path = ImageUtils.getDiskCacheFilePath(mContext, url);
			Log.d(TAG, "下载配置图片地址:" + url);
			if (!result) {
				Log.d(TAG, "下载失败,重新下载配置图片地址:" + url);
				result = downloadImage(url, displayConfig);
				if (!result) {
					Log.d(TAG, "下载失败,重新下载配置图片地址:" + url);
					result = downloadImage(url, displayConfig);
				} else {
					Log.d(TAG, "下载成功，文件路径:" + path);
				}
			} else {
				Log.d(TAG, "下载成功，文件路径:" + path);
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
		}

	}

	/**
	 * 图片下载完成的回调方法
	 * 
	 * @author LeeFranker
	 * 
	 */
	public interface BitmapFinishCallback {

		public Bitmap creatBitmap(Bitmap bitmap);

		public void showSuccess();

		public void showError();
	}

}
