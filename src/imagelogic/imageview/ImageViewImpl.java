package imagelogic.imageview;

import imagelogic.utils.Log;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageViewImpl implements BaseImageView {
	private static final String TAG = ImageViewImpl.class.getName();
	public static final String WARN_CANT_SET_DRAWABLE = "Can't set a drawable into view. You should call ImageLoader on UI thread for it.";
	public static final String WARN_CANT_SET_BITMAP = "Can't set a bitmap into view. You should call ImageLoader on UI thread for it.";

	protected Reference<ImageView> imageViewRef;
	protected boolean checkActualViewSize;
	private int width, height;

	public ImageViewImpl(ImageView imageView) {
		this(imageView, true);
	}

	public ImageViewImpl(ImageView imageView, boolean checkActualViewSize) {
		this.imageViewRef = new WeakReference<ImageView>(imageView);
		this.checkActualViewSize = checkActualViewSize;
	}

	public void initView(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public int getImageViewWidth(int maxWidth) {
		ImageView imageView = imageViewRef.get();
		if (imageView != null) {
			final DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
			final ViewGroup.LayoutParams params = imageView.getLayoutParams();
			int width = 0;
			if (checkActualViewSize && params != null && params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
				width = imageView.getWidth();
			}
			if (width <= 0 && params != null)
				width = params.width;
			if (width <= 0)
				width = getImageViewFieldValue(imageView, "mMaxWidth");
			if (width <= 0)
				width = maxWidth;
			if (width <= 0)
				width = displayMetrics.widthPixels;
			return width;
		}
		return maxWidth;
	}

	@Override
	public int getImageViewHeight(int maxHeight) {
		ImageView imageView = imageViewRef.get();
		if (imageView != null) {
			final DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
			final ViewGroup.LayoutParams params = imageView.getLayoutParams();
			int height = 0;
			if (checkActualViewSize && params != null && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
				height = imageView.getHeight();
			}
			if (height <= 0 && params != null)
				height = params.height;
			if (height <= 0)
				height = getImageViewFieldValue(imageView, "mMaxHeight");
			if (height <= 0)
				height = maxHeight;
			if (height <= 0)
				height = displayMetrics.heightPixels;
			return height;
		}
		return 0;
	}

	@Override
	public ViewScaleType getImageViewScaleType() {
		ImageView imageView = imageViewRef.get();
		if (imageView != null) {
			return ViewScaleType.fromImageView(imageView);
		}
		return null;
	}

	@Override
	public ImageView getImageView() {
		return imageViewRef.get();
	}

	@Override
	public boolean isImageViewCollected() {
		return imageViewRef.get() == null;
	}

	@Override
	public int getImageViewId() {
		ImageView imageView = imageViewRef.get();
		return imageView == null ? super.hashCode() : imageView.hashCode();
	}

	private static int getImageViewFieldValue(Object object, String fieldName) {
		int value = 0;
		try {
			Field field = ImageView.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			int fieldValue = (Integer) field.get(object);
			if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
				value = fieldValue;
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		return value;
	}

	public void startAnimation(Animation animation) {
		ImageView imageView = getImageView();
		if (imageView != null)
			imageView.startAnimation(animation);
	}

	public void getImageDrawable() {
		imageViewRef.get().getDrawable();

	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		ImageView imageView = getImageView();
		if (imageView != null) {
			ViewScaleType scaleType = getImageViewScaleType();
			if (scaleType != null) {
				if (scaleType.equals(ViewScaleType.FIT_XY)) {
					imageView.setScaleType(ScaleType.FIT_XY);
				} else {
					imageView.setScaleType(ScaleType.CENTER_CROP);
				}
			} else {
				imageView.setScaleType(ScaleType.FIT_XY);
			}
			if (drawable instanceof CacheableDrawable) {
				CacheableDrawable cache = (CacheableDrawable) drawable;
				if (cache.isOriginalDrawable(width, height)) {
					imageView.setImageDrawable(drawable);
				} else {
					if (cache.hasValidBitmap())
						imageView.setImageBitmap(cache.getBitmap());
					else 
						imageView.setImageResource(0);
				}
			} else {
				imageView.setImageDrawable(drawable);
			}
		}
	}

	@Override
	public void setImageBitmap(Bitmap bitmap) {
		ImageView imageView = getImageView();
		if (imageView != null) {
			imageView.setImageBitmap(bitmap);
		}
	}
}
