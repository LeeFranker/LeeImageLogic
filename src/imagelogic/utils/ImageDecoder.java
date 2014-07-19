package imagelogic.utils;

import java.io.FileDescriptor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 图片处理类
 * 
 * @author LeeFranker
 * 
 */
public class ImageDecoder {

	private ImageDecoder() {
	}

	/**
	 * @Title: decodeSampledBitmapFromResource
	 * @Description: 压缩图片数据
	 * @param @param res 资源对象
	 * @param @param resId 资源ID
	 * @param @param reqWidth 压缩后的宽
	 * @param @param reqHeight 压缩后的搞
	 * @param @return
	 * @return Bitmap
	 * @throws
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res,
			int resId, int reqWidth, int reqHeight) {

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		BitmapFactory.decodeResource(res, resId, options);
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);
		options.inJustDecodeBounds = false;
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeResource(res, resId, options);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * @Title: decodeSampledBitmapFromFile
	 * @Description: 压缩图片数据
	 * @param @param 图片文件路径
	 * @param @param reqWidth 压缩后的宽
	 * @param @param reqHeight 压缩后的高
	 * @param @return
	 * @return Bitmap
	 * @throws
	 */
	public static Bitmap decodeSampledBitmapFromFile(String filePath,
			int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		BitmapFactory.decodeFile(filePath, options);
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);
		options.inJustDecodeBounds = false;
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeFile(filePath, options);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * @Title: decodeSampledBitmapFromInputstream
	 * @Description: 压缩图片数据
	 * @param @param InputStream
	 * @param @param reqWidth
	 * @param @param reqHeight
	 * @param @return
	 * @return Bitmap
	 * @throws
	 */
	public static Bitmap decodeSampledBitmapFromBytes(byte[] bytes,
			int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);
		options.inJustDecodeBounds = false;
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length,
					options);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * @Title: decodeSampledBitmapFromDescriptor
	 * @Description: 压缩图片数据
	 * @param @param fileDescriptor
	 * @param @param reqWidth
	 * @param @param reqHeight
	 * @param @return
	 * @return Bitmap
	 * @throws
	 */
	public static Bitmap decodeSampledBitmapFromDescriptor(
			FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);
		options.inJustDecodeBounds = false;
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null,
					options);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * @Title: calculateInSampleSize
	 * @Description: 计算图片压缩比
	 * @param @param options
	 * @param @param reqWidth
	 * @param @param reqHeight
	 * @param @return
	 * @return int
	 * @throws
	 */
	private static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}

			final float totalPixels = width * height;

			final float totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
				inSampleSize++;
			}
		}
		return inSampleSize;
	}

}
