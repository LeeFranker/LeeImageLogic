package imagelogic.display;

import imagelogic.utils.ImageUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.animation.Animation;

/**
 * 图片显示配置类
 * 
 * @author LeeFranker
 * 
 */
public class ImageDisplayConfig {
	private int bitmapWidth; // 图片宽
	private int bitmapHeight;// 图片高
	private int displayWidth;// 手机宽
	private int displayHeight;// 手机高
	private Animation animation;// 动画对象
	private int animationType;// 动画类型
	private Bitmap loadingBitmap;// 默认加载图片

	public int getDisplayWidth() {
		return displayWidth;
	}

	public void setDisplayWidth(int displayWidth) {
		this.displayWidth = displayWidth;
	}

	public int getDisplayHeight() {
		return displayHeight;
	}

	public void setDisplayHeight(int displayHeight) {
		this.displayHeight = displayHeight;
	}

	public int getBitmapWidth() {
		return bitmapWidth;
	}

	public void setBitmapWidth(int bitmapWidth) {
		this.bitmapWidth = bitmapWidth;
	}

	public int getBitmapHeight() {
		return bitmapHeight;
	}

	public void setBitmapHeight(int bitmapHeight) {
		this.bitmapHeight = bitmapHeight;
	}

	public Animation getAnimation() {
		return animation;
	}

	public void setAnimation(Animation animation) {
		this.animation = animation;
	}

	public int getAnimationType() {
		return animationType;
	}

	public void setAnimationType(int animationType) {
		this.animationType = animationType;
	}

	public Bitmap getLoadingBitmap() {
		if (loadingBitmap != null && !loadingBitmap.isRecycled()) {
			return loadingBitmap;
		}
		return null;
	}

	public void setLoadingBitmap(Bitmap loadingBitmap) {
		this.loadingBitmap = loadingBitmap;
	}

	public void setLoadingBitmap(Context context, int resId) {
		this.loadingBitmap = ImageUtils.getBitmapFromId(context, resId);
	}

	public class AnimationType {
		// 用户定义
		public static final int userDefined = 0;
		// 默认动画
		public static final int fadeIn = 1;
	}

}
