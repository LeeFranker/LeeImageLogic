package imagelogic.listener;

import imagelogic.ImageLogic;
import android.content.Context;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

public class ImageOnScrollListener implements OnScrollListener {

	private ImageLogic mImageLogic;

	private final boolean pauseOnScroll = true;
	private final OnScrollListener mExternalListener;

	public ImageOnScrollListener(Context context) {
		this(context, null);
	}

	public ImageOnScrollListener(Context context,
			OnScrollListener customListener) {
		this.mImageLogic = ImageLogic.create(context);
		mExternalListener = customListener;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
			if (pauseOnScroll)
				mImageLogic.pauseWork(true);
		} else {
			if (pauseOnScroll)
				mImageLogic.pauseWork(false);
		}
		if (mExternalListener != null) {
			mExternalListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (mExternalListener != null) {
			mExternalListener.onScroll(view, firstVisibleItem,
					visibleItemCount, totalItemCount);
		}
	}

}
