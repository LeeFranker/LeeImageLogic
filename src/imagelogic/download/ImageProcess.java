package imagelogic.download;

import imagelogic.disk.BitmapDiskCache;
import imagelogic.display.ImageDisplayConfig;
import imagelogic.utils.ImageDecoder;
import imagelogic.utils.ImageUtils;
import imagelogic.utils.Log;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 下载图片
 * 
 * @author LeeFranker
 * 
 */
public class ImageProcess {
	private static final String TAG = "ImageProcess";

	private BitmapDiskCache mDiskCache;// 磁盘缓存对象

	private ImageDownloader downloader; // 下载对象

	private boolean neverCalculate = false;// 是否处理图片

	private AtomicBoolean mInitDiskCache = new AtomicBoolean(false);

	/**
	 * Description:构造方法
	 * 
	 * @param downloader
	 * @param filePath
	 * @param cacheSize
	 */
	public ImageProcess(ImageDownloader downloader) {
		this.downloader = downloader;
	}

	/**
	 * @Title: configCalculateBitmap
	 * @Description: 是否处理图片
	 * @param @param neverCalculate
	 * @return void
	 * @throws
	 */
	public void configCalculateBitmap(boolean neverCalculate) {
		this.neverCalculate = neverCalculate;
	}

	/**
	 * @Title: processBitmap
	 * @Description: 处理bitmap
	 * @param @param data
	 * @param @param config
	 * @param @return
	 * @return Bitmap
	 * @throws
	 */
	public Bitmap processBitmap(String url, ImageDisplayConfig config) {
		Log.d(TAG, "processBitmap下载地址:" + url);
		byte[] bytes = null;
		bytes = downloader.downloadBytesByUrl(url);
		Bitmap bitmap = null;
		if (bytes != null) {
			if (neverCalculate) {
				bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			} else {
				bitmap = ImageDecoder.decodeSampledBitmapFromBytes(bytes,
						config.getBitmapWidth(), config.getBitmapHeight());
			}
		}
		return bitmap;
	}

	/**
	 * @Title: downloadImage
	 * @Description: 下载图片
	 * @param @param data
	 * @param @param config
	 * @param @return
	 * @return void
	 * @throws
	 */
	public boolean downloadImage(String url, ImageDisplayConfig config) {
		Log.d(TAG, "downloadImage下载地址:" + url);
		final String key = ImageUtils.CalcUrl2Md5(url);
		boolean result = false;
		OutputStream out = null;
		if (mDiskCache == null) {
			Log.e(TAG, "mDiskCache==null");
			return false;
		}
		try {
			out = mDiskCache.getTempOutputStream(key);
			boolean success = downloader.downloadToLocalStreamByUrl(url, out);
			if (success) {
				mDiskCache.commit(key);
			} else {
				mDiskCache.delTempFile(key);
			}
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * @Title: initHttpDiskCache
	 * @Description: 初始化磁盘缓存
	 * @param
	 * @return void
	 * @throws
	 */
	public void initHttpDiskCache(BitmapDiskCache diskLruCache) {
		mDiskCache = diskLruCache;
	}

	/**
	 * @Title: clearCacheInternal
	 * @Description: 设置清理缓存的标志
	 */
	public void clearDiskCache() {
		mInitDiskCache.set(true);
	}

}
