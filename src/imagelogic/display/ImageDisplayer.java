package imagelogic.display;

import imagelogic.imageview.ImageViewImpl;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * 图片现实接口
 * 
 * @author LeeFranker
 * 
 */
public interface ImageDisplayer {
	/**
	 * @Title: loadCompletedisplay
	 * @Description: 图片加载成功的回调接口
	 * @param @param imageView 图片对象
	 * @param @param bitmap 图片数据
	 * @param @param config 图片显示配置信息
	 * @return void
	 * @throws
	 */
	public void loadCompletedisplay(ImageViewImpl imageView,
			BitmapDrawable drawable, ImageDisplayConfig config);

	/**
	 * @Title: loadFailDisplay
	 * @Description:图片加载失败的回调函数
	 * @param @param imageView 图片对象
	 * @param @param bitmap 图片数据
	 * @return void
	 * @throws
	 */
	public void loadFailDisplay(ImageViewImpl imageView, Bitmap bitmap);

}
