package imagelogic.imageview;

import android.widget.ImageView;

public enum ViewScaleType {
	FIT_XY,

	CENTER_CROP;

	public static ViewScaleType fromImageView(ImageView imageView) {
		switch (imageView.getScaleType()) {
		case FIT_CENTER:
		case FIT_XY:
		case FIT_START:
		case FIT_END:
		case CENTER_INSIDE:
			return FIT_XY;
		case MATRIX:
		case CENTER:
		case CENTER_CROP:
		default:
			return CENTER_CROP;
		}
	}
}
