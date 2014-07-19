package imagelogic.imageview;

import imagelogic.utils.Log;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CacheableImageView extends ImageView {
	private static final String TAG = CacheableImageView.class.getSimpleName();

	private void onDrawableSet(Drawable drawable) {
		if (drawable instanceof CacheableDrawable) {
			((CacheableDrawable) drawable).setBeingUsed(true);
		}
	}

	private void onDrawableUnset(final Drawable drawable) {
		if (drawable instanceof CacheableDrawable) {
			((CacheableDrawable) drawable).setBeingUsed(false);
		}
	}

	public CacheableImageView(Context context) {
		super(context);
	}

	public CacheableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		final Drawable previousDrawable = getDrawable();
		super.setImageDrawable(drawable);
		if (drawable != previousDrawable) {
			onDrawableSet(drawable);
			onDrawableUnset(previousDrawable);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		setImageDrawable(null);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		try {
			super.onDraw(canvas);
		} catch (Exception e) {
			Log.e(TAG, "Cannot draw recycled bitmaps");
		}
	}

}
