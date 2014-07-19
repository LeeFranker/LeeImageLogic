package imagelogic;

import imagelogic.disk.BitmapDiskCache;
import imagelogic.imageview.CacheableDrawable;
import imagelogic.memory.ImageLruMemoryCache;
import imagelogic.utils.ImageUtils;
import imagelogic.utils.Log;

import java.io.File;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

/**
 * 图片缓存类
 * 
 * @author LeeFranker
 * 
 */
public class ImageCache {
	// log输出标签
	private static final String TAG = "ImageCache";

	// 默认的内存缓存大小
	private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 5; // 5MB

	// 默认的磁盘缓存大小
	private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 20; // 20MB

	// 图片存储磁盘的默认参数
	private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
	private static final int DEFAULT_COMPRESS_QUALITY = 70;

	// 缓存切换的变量控制
	private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
	private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
	private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false;
	private static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = false;

	// 磁盘缓存
	private BitmapDiskCache mBitmapDiskCache;

	// 内存缓存
	private ImageLruMemoryCache<String, CacheableDrawable> mMemoryCache;

	// 缓存参数
	private ImageCacheParams mCacheParams;
	
	// 磁盘锁
	private final Object mDiskCacheLock = new Object();

	// 构造方法
	public ImageCache(ImageCacheParams cacheParams) {
		init(cacheParams);
	}

	/**
	 * @Title: init
	 * @Description: 图片缓存初始化操作
	 * @param @param cacheParams
	 * @return void
	 * @throws
	 */
	private void init(ImageCacheParams cacheParams) {
		mCacheParams = cacheParams;
		if (mCacheParams.memoryCacheEnabled) {
			mMemoryCache = new ImageLruMemoryCache<String, CacheableDrawable>(
					mCacheParams.memCacheSize) {
				@Override
				protected int sizeOf(String key, CacheableDrawable value) {
					return value.getMemorySize();
				}

				@Override
				protected void entryRemoved(boolean evicted, String key,
						CacheableDrawable oldValue, CacheableDrawable newValue) {
					oldValue.setCached(false);
					Log.d(TAG, "remove cacheableDrawable");
				}
			};
		}
		// 初始化磁盘缓存
		if (mCacheParams.initDiskCacheOnCreate) {
			initDiskCache();
		}
	}

	/**
	 * @Title: initDiskCache
	 * @Description: 初始化磁盘缓存
	 * @param
	 * @return void
	 * @throws
	 */
	public BitmapDiskCache initDiskCache() {
		File diskCacheDir = mCacheParams.diskCacheDir;
		if (diskCacheDir != null && !diskCacheDir.exists()) {
			diskCacheDir.mkdirs();
		}
		if (mBitmapDiskCache == null) {
			if (mCacheParams.diskCacheEnabled) {
				Log.d(TAG, "磁盘缓存路径:" + diskCacheDir.getAbsolutePath());
				if (ImageUtils.getUsableSpace(diskCacheDir) > mCacheParams.diskCacheSize) {
					try {
						mBitmapDiskCache = new BitmapDiskCache(diskCacheDir,
								mCacheParams.diskCacheSize);
						synchronized (mDiskCacheLock) {
							mBitmapDiskCache.initialize();
						}
					} catch (Exception e) {
						e.printStackTrace();
						Log.e(TAG, "error:" + e.getMessage());
					}
				}
			}
		}
		if (mBitmapDiskCache != null) {
			Log.d(TAG, "磁盘缓存对象初始化ok");
		} else {
			Log.e(TAG, "磁盘缓存对象初始化error");
		}
		return mBitmapDiskCache;
	}

	/**
	 * @Title: addDrawableToCache
	 * @Description: 添加图片数据到缓存中
	 * @param @param url
	 * @param @param Drawable
	 * @return void
	 * @throws
	 */
	public void addDrawableToMemoryCache(CacheableDrawable drawable,
			String memoryCacheKey) {
		if (memoryCacheKey == null || drawable == null) {
			Log.e(TAG, "memoryCacheKey==null||drawable==null");
			return;
		}
		if (mMemoryCache != null && mMemoryCache.get(memoryCacheKey) == null) {
			drawable.setCached(true);
			mMemoryCache.put(memoryCacheKey, drawable);
		}
	}

	/**
	 * @Title: addBitmapToCache
	 * @Description: 添加图片数据到缓存中
	 * @param @param url
	 * @param @param bitmap
	 * @return void
	 * @throws
	 */
	public void addBitmapToDiskCache(String uri, Bitmap bitmap) {
		if (mBitmapDiskCache == null) {
			return;
		}
		if (uri == null || bitmap == null) {
			Log.e(TAG, "uri==null||drawable==null");
			return;
		}

		final String key = ImageUtils.CalcUrl2Md5(uri);
		mBitmapDiskCache.put(key, bitmap, ImageUtils.isJpg(uri));
	}

	/**
	 * @Title: getBitmapFromMemCache
	 * @Description: 从内存获取图片数据
	 * @param @param url图片地址
	 * @param @return
	 * @return CacheableDrawable
	 * @throws
	 */
	public CacheableDrawable getDrawableFromMemCache(String url) {
		CacheableDrawable drawable = null;
		if (mMemoryCache != null) {
			drawable = mMemoryCache.get(url);
			if (null != drawable && !drawable.hasValidBitmap()) {
				Log.d(TAG, "内存中bitmap被回收，清理内存！");
				mMemoryCache.remove(url);
				drawable = null;
			}
		}
		return drawable;
	}

	/**
	 * @Title: removeDrawableFromMemCache
	 * @Description: 从内存删除图片数据
	 * @param @param url图片地址
	 * @throws
	 */
	public void removeDrawableFromMemCache(String url) {
		CacheableDrawable drawable = null;
		if (mMemoryCache != null) {
			drawable = mMemoryCache.get(url);
			if (null != drawable && !drawable.hasValidBitmap()) {
				Log.d(TAG, "内存缓存中bitmap被回收，清理内存！");
				mMemoryCache.remove(url);
				drawable = null;
			}
		}
	}

	/**
	 * @Title: getBitmapFromDiskCache
	 * @Description: 根据图片地址获取图片数据
	 * @param @param url 图片地址
	 * @param @return
	 * @return Bitmap
	 * @throws
	 */
	public Bitmap getBitmapFromDiskCache(String url) {
		final String key = ImageUtils.CalcUrl2Md5(url);
		if (mBitmapDiskCache == null) {
			Log.w(TAG, "mDiskLruCache==null");
			return null;
		}
		try {
			return mBitmapDiskCache.get(key);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "error:" + e.getMessage());
		}
		return null;
	}

	/**
	 * @Title: clearCache
	 * @Description: 清空缓存
	 * @param
	 * @return void
	 * @throws
	 */
	public void clearDiskCache() {
		clearMemoryCache();
		synchronized (mDiskCacheLock) {
			if (mBitmapDiskCache == null)
				Log.e(TAG, "mDiskLruCache==null");
			if (mBitmapDiskCache != null) {
				try {
					Log.d(TAG, "开始清理图片...");
					mBitmapDiskCache.clear();
					Log.d(TAG, "清理图片完毕");
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "error:" + e.getMessage());
				}
				mBitmapDiskCache = null;
				initDiskCache();
			}
		}
	}

	/**
	 * @Title: clearMemoryCache
	 * @Description: 清空内存
	 * @param
	 * @return void
	 * @throws
	 */
	public void clearMemoryCache() {
		if (mMemoryCache != null) {
			mMemoryCache.evictAll();
		}
	}

	/**
	 * @Title: trimMemoryCache
	 * @Description: 整理内存
	 * @param
	 * @return void
	 * @throws
	 */
	public void trimMemoryCache() {
		if (mMemoryCache != null) {
			final Set<java.util.Map.Entry<String, CacheableDrawable>> values = mMemoryCache
					.snapshot().entrySet();
			for (java.util.Map.Entry<String, CacheableDrawable> entry : values) {
				CacheableDrawable value = entry.getValue();
				if (null == value || !value.isBeingDisplayed()) {
					mMemoryCache.remove(entry.getKey());
				}
			}
		}
	}

	/**
	 * @Title: deleteDiskcache
	 * @Description: 删除磁盘文件
	 * @param @param url
	 * @throws
	 */
	public void deleteDiskcache(String url) {
		if (mBitmapDiskCache != null) {
			mBitmapDiskCache.remove(url);
		}
	}

	/**
	 * @ClassName: ImageCacheParams
	 * @Description: 磁盘缓存参数设置
	 * @author 王力
	 * @date 2013-2-5 下午04:22:46
	 */
	public static class ImageCacheParams {
		public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;// 内存缓存大小
		public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;// 磁盘缓存大小
		public File diskCacheDir;// 磁盘缓存目录文件
		public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;// 压缩格式
		public int compressQuality = DEFAULT_COMPRESS_QUALITY;// 压缩质量
		public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;// 是否使用内存缓存
		public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;// 是否使用磁盘缓存
		public boolean clearDiskCacheOnStart = DEFAULT_CLEAR_DISK_CACHE_ON_START;// 是否开始请清理磁盘文件
		public boolean initDiskCacheOnCreate = DEFAULT_INIT_DISK_CACHE_ON_CREATE;// 是否开始初始化磁盘缓存

		// 设置磁盘缓存目录
		public ImageCacheParams(String diskCacheDir) {
			this.diskCacheDir = new File(diskCacheDir);
		}

		// 设置缓存大小
		public void setMemCacheSizePercent(Context context, float percent) {
			if (percent < 0.05f || percent > 0.8f) {
				Log.w(TAG, "percent must be between 0.05 and 0.8");
			}
			int appMemSize = ImageUtils.getMemoryClass(context);
			memCacheSize = Math.round(appMemSize * 1024 * 1024 * percent);
			Log.w(TAG, "heap总大小:" + ImageUtils.btye2M(appMemSize));
			Log.w(TAG, "heap图片占用内存比例:" + percent);
			Log.w(TAG, "heap分配图片大小:" + ImageUtils.btye2M(memCacheSize));
		}

		// 设置缓存大小
		public void setMemCacheSize(int memCacheSize) {
			this.memCacheSize = memCacheSize;
		}

		// 设置磁盘大小
		public void setDiskCacheSize(int diskCacheSize) {
			this.diskCacheSize = diskCacheSize;
		}

	}

}
