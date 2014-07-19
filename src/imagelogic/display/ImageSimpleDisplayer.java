package imagelogic.display;

import imagelogic.imageview.ImageViewImpl;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/**
 * 图片显示实现类
 * 
 * @author LeeFranker
 * 
 */
public class ImageSimpleDisplayer implements ImageDisplayer {

	public void loadCompletedisplay(ImageViewImpl imageView,
			BitmapDrawable drawable, ImageDisplayConfig config) {
		switch (config.getAnimationType()) {
		case ImageDisplayConfig.AnimationType.fadeIn:
			fadeInDisplay(imageView, drawable);
			break;
		case ImageDisplayConfig.AnimationType.userDefined:
			animationDisplay(imageView, drawable, config.getAnimation());
			break;
		default:
			break;
		}
	}

	/**
	 * @Title: loadFailDisplay
	 * @Description: 失败显示
	 * @param @param imageView
	 * @param @param bitmap
	 * @return void
	 * @throws
	 */
	public void loadFailDisplay(ImageViewImpl imageView, Bitmap bitmap) {
		imageView.setImageBitmap(bitmap);
	}

	/**
	 * @Title: fadeInDisplay
	 * @Description: 默认图片显示动画
	 * @param @param imageView
	 * @param @param drawable
	 * @return void
	 * @throws
	 */
	private void fadeInDisplay(ImageViewImpl imageView, BitmapDrawable drawable) {
		final TransitionDrawable td = new TransitionDrawable(new Drawable[] {
				new ColorDrawable(android.R.color.transparent), drawable });
		imageView.setImageDrawable(drawable);
		td.startTransition(300);
	}

	/**
	 * @Title: animationDisplay
	 * @Description: 用户自定义图片显示动画
	 * @param @param imageView
	 * @param @param drawable
	 * @param @param animation
	 * @return void
	 * @throws
	 */
	private void animationDisplay(ImageViewImpl imageView,
			BitmapDrawable drawable, Animation animation) {
		animation.setStartTime(AnimationUtils.currentAnimationTimeMillis());
		imageView.setImageDrawable(drawable);
		imageView.startAnimation(animation);
	}

}
