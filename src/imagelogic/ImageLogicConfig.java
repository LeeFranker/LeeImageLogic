package imagelogic;

import imagelogic.display.ImageDisplayConfig;
import imagelogic.display.ImageDisplayer;
import imagelogic.display.ImageSimpleDisplayer;
import imagelogic.download.ImageDownloader;
import imagelogic.download.ImageProcess;
import imagelogic.download.ImageSimpleHttpDownloader;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * 图片显示配置类
 * 
 * @author LeeFranker
 * 
 */
public class ImageLogicConfig {

	public String cachePath;// 缓存目录地址
	public ImageDisplayer displayer;// 图片显示
	public ImageDownloader downloader;// 图片下载
	public ImageProcess bitmapProcess; // 图片处理
	public ImageDisplayConfig defaultDisplayConfig;// 图片显示配置
	public float memCacheSizePercent;// 缓存百分比，android系统分配给每个APK内存的大小
	public int memCacheSize;// 内存缓存大小
	public int diskCacheSize;// 磁盘缓存大小
	public int mDisplayWidth, mDisplayHeight;

	public ImageLogicConfig(Context context) {
		// 设置动画模式,默认动画
		defaultDisplayConfig = new ImageDisplayConfig();
		defaultDisplayConfig.setAnimation(null);
		defaultDisplayConfig
				.setAnimationType(ImageDisplayConfig.AnimationType.fadeIn);
		DisplayMetrics displayMetrics = context.getResources()
				.getDisplayMetrics();
		mDisplayWidth = displayMetrics.widthPixels;
		mDisplayHeight = displayMetrics.heightPixels;
		defaultDisplayConfig.setDisplayHeight(mDisplayHeight);
		defaultDisplayConfig.setDisplayWidth(mDisplayWidth);
		int defaultWidth = (int) Math.floor(mDisplayWidth / 3);
		int defaultHeight = (int) Math.floor(mDisplayHeight / 5);
		defaultDisplayConfig.setBitmapHeight(defaultHeight);
		defaultDisplayConfig.setBitmapWidth(defaultWidth);
	}

	public void init() {
		if (downloader == null)
			downloader = new ImageSimpleHttpDownloader();

		if (displayer == null)
			displayer = new ImageSimpleDisplayer();

		if (bitmapProcess == null)
			bitmapProcess = new ImageProcess(downloader);
	}
}
