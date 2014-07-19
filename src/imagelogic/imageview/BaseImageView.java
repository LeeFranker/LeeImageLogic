package imagelogic.imageview;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * 图片常用接口
 * 
 * @author LeeFranker
 * 
 */
public interface BaseImageView {

	int getImageViewWidth(int maxWidth);

	int getImageViewHeight(int maxHeight);

	ViewScaleType getImageViewScaleType();

	View getImageView();

	boolean isImageViewCollected();

	int getImageViewId();

	void setImageDrawable(Drawable drawable);

	void setImageBitmap(Bitmap bitmap);
}
