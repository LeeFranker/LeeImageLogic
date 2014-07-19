package imagelogic.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.StatFs;

/**
 * 图片工具类
 * 
 * @author LeeFranker
 * 
 */
public class ImageUtils {

	private static final String TAG = "ImageUtils";
	private static final String IMAGE_DIR = "ImageCache";// 图片缓存文件夹名字

	private ImageUtils() {

	};

	/**
	 * @Title: hasFroyo
	 * @Description: 判断系统版本是否大于2.2
	 * @param @return
	 * @return boolean true 大于 false 小于
	 * @throws
	 */
	public static boolean hasFroyo() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	/**
	 * @Title: hasFroyo
	 * @Description: 判断系统版本是否大于2.3
	 * @param @return
	 * @return boolean true 大于 false 小于
	 * @throws
	 */
	public static boolean hasGingerbread() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	/**
	 * @Title: hasFroyo
	 * @Description: 判断系统版本是否大于4.1
	 * @param @return
	 * @return boolean true 大于 false 小于
	 * @throws
	 */
	public static boolean hasJellyBean() {
		return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;
	}

	/**
	 * @Title: hasFroyo
	 * @Description: 判断系统版本是否大于3.0
	 * @param @return
	 * @return boolean true 大于 false 小于
	 * @throws
	 */
	public static boolean hasHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	/**
	 * @Title: hasFroyo
	 * @Description: 判断系统版本是否大于3.1
	 * @param @return
	 * @return boolean true 大于 false 小于
	 * @throws
	 */
	public static boolean hasHoneycombMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
	}

	/**
	 * @Title: getDiskCacheDir
	 * @Description: 获取磁盘缓存图片目录文件
	 * @param @param context 环境上下文
	 * @param @param uniqueName 目录名字(现阶段写死)
	 * @param @return
	 * @return File 目录文件
	 * @throws
	 */
	public static File getDiskCacheDir(Context context) {
		final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState()) ? getExternalCacheDir(context)
				.getPath() : getInternalCacheDir(context).getPath();
		return new File(cachePath);
	}

	/**
	 * @Title: getDiskCacheFilePath
	 * @Description: 获取磁盘缓存图片文件
	 * @param @param context 环境上下文
	 * @param @param url 图片地址
	 * @param @return
	 * @return 图片在磁盘所处绝对路径
	 * @throws
	 */
	public static String getDiskCacheFilePath(Context context, String url) {
		String cachePath = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState()) ? getExternalCacheDir(context)
				.getPath() : getInternalCacheDir(context).getPath();
		cachePath = cachePath + File.separator + IMAGE_DIR + File.separator;
		String key = ImageUtils.CalcUrl2Md5(url);
		cachePath = cachePath + key;
		return cachePath;
	}

	/**
	 * @Title: getDiskCacheExistedFilePath
	 * @Description: 获取磁盘缓存图片文件
	 * @param @param context 环境上下文
	 * @param @param rl 图片地址
	 * @param @return 文件存在返回路径，不存在返回null
	 * @return 图片在磁盘所处绝对路径
	 * @throws
	 */
	public static String getDiskCacheExistedFilePath(Context context, String url) {
		String cachePath = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState()) ? getExternalCacheDir(context)
				.getPath() : getInternalCacheDir(context).getPath();
		cachePath = cachePath + File.separator + IMAGE_DIR + File.separator;
		String key = ImageUtils.CalcUrl2Md5(url);
		cachePath = cachePath + key;
		if (new File(cachePath).exists())
			return cachePath;
		return null;
	}

	/**
	 * @Title: getExternalCacheDir
	 * @Description: 创建外部存储图片目录
	 * @param @param context 环境上下文
	 * @param @param uniqueName 目录名字
	 * @param @return
	 * @return File 目录文件
	 * @throws
	 */
	private static File getExternalCacheDir(Context context) {
		String path = Environment.getExternalStorageDirectory().getPath();
		File f = new File(path);
		if (!f.exists()) {
			f.mkdir();
		}
		return f;
	}

	/**
	 * @Title: getInternalCacheDir
	 * @Description: 创建内部存储图片目录
	 * @param @param context 环境上下文
	 * @param @param uniqueName 目录名字
	 * @param @return
	 * @return File 目录文件
	 * @throws
	 */
	private static File getInternalCacheDir(Context context) {
		String path = context.getFilesDir().getPath();
		File f = new File(path);
		if (!f.exists()) {
			f.mkdir();
		}
		return f;
	}

	/**
	 * @Title: getBitmapSize
	 * @Description: 获取bitmap对象字节数
	 * @param @param bitmap
	 * @param @return
	 * @return int
	 * @throws
	 */
	public static int getBitmapSize(Bitmap bitmap) {
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	/**
	 * @Title: getUsableSpace
	 * @Description: 获取目录下面的存储空间
	 * @param @param path
	 * @param @return
	 * @return long
	 * @throws
	 */
	@SuppressWarnings("deprecation")
	public static long getUsableSpace(File path) {
		if (hasGingerbread()) {
			return path.getUsableSpace();
		}
		final StatFs stats = new StatFs(path.getPath());
		return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
	}

	/**
	 * @Title: getMemoryClass
	 * @Description: 获取内存大小
	 * @param @param context
	 * @param @return
	 * @return int
	 * @throws
	 */
	public static int getMemoryClass(Context context) {
		int memoryAllSize = ((ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		return memoryAllSize;
	}

	/**
	 * @Title: CalcUrl2Md5
	 * @Description: 把字符串转化为MD5
	 * @param @param str
	 * @param @return
	 * @return String
	 * @throws
	 */
	public static String CalcUrl2Md5(String str) {
		try {
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(str.getBytes());
			return toHexString(algorithm.digest(), "") + ".0";
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static String toHexString(byte[] bytes, String separator) {
		StringBuilder hexString = new StringBuilder();
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };
		for (byte b : bytes) {
			hexString.append(hexDigits[b >> 4 & 0xf]);
			hexString.append(hexDigits[b & 0xf]);
		}
		return hexString.toString();
	}

	/**
	 * @Title: getBitmapFromId
	 * @Description: 资源文件转换为bitmap对象
	 * @param @param context
	 * @param @param resId
	 * @param @return
	 * @return Bitmap
	 * @throws
	 */
	public static Bitmap getBitmapFromId(Context context, int resId) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory
					.decodeResource(context.getResources(), resId);
			return bitmap;
		} catch (OutOfMemoryError error) {
			error.printStackTrace();
			Log.e(TAG, "资源文件转换bitmap发生OOM错误");
		}
		return bitmap;
	}

	/**
	 * @Title: getBitmapFromPath
	 * @Description: 资源文件转换为bitmap对象
	 * @param @param context
	 * @param @param path路径
	 * @param @return
	 * @return Bitmap
	 * @throws
	 */
	public static Bitmap getBitmapFromPath(Context context, String path) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeFile(path);
			return bitmap;
		} catch (OutOfMemoryError error) {
			error.printStackTrace();
			Log.e(TAG, "资源文件转换bitmap发生OOM错误");
		}
		return bitmap;
	}

	/**
	 * 放大缩小图片
	 * 
	 * @param bitmap
	 * @param w
	 * @param h
	 * @return
	 */
	public static Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Matrix matrix = new Matrix();
		float scaleWidht = ((float) w / width);
		float scaleHeight = ((float) h / height);
		matrix.postScale(scaleWidht, scaleHeight);
		Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, true);
		return newbmp;
	}

	/**
	 * 将Drawable转化为Bitmap
	 * 
	 * @param drawable
	 * @return
	 */
	public static Bitmap drawableToBitmap(Drawable drawable) {
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, drawable
				.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
				: Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		drawable.draw(canvas);
		return bitmap;

	}

	/**
	 * 获得圆角图片的方法
	 * 
	 * @param bitmap
	 * @param roundPx
	 * @return
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {

		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	/**
	 * 获得带倒影的图片方法
	 * 
	 * @param bitmap
	 * @return
	 */
	public static Bitmap createReflectionImageWithOrigin(Bitmap bitmap) {
		final int reflectionGap = 4;
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, height / 2,
				width, height / 2, matrix, false);

		Bitmap bitmapWithReflection = Bitmap.createBitmap(width,
				(height + height / 2), Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmapWithReflection);
		canvas.drawBitmap(bitmap, 0, 0, null);
		Paint deafalutPaint = new Paint();
		canvas.drawRect(0, height, width, height + reflectionGap, deafalutPaint);

		canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0,
				bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff,
				0x00ffffff, TileMode.CLAMP);
		paint.setShader(shader);
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		canvas.drawRect(0, height, width, bitmapWithReflection.getHeight()
				+ reflectionGap, paint);

		return bitmapWithReflection;
	}

	/**
	 * @param file
	 *            文件
	 * @return 文件转换MD5
	 */
	public static String calcMd5(File file) {
		FileInputStream in = null;
		try {
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			in = new FileInputStream(file);
			FileChannel ch = in.getChannel();
			MappedByteBuffer byteBuffer;
			byteBuffer = ch
					.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			algorithm.update(byteBuffer);
			return toHexString(algorithm.digest(), "");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 字节转化成M和G单位
	public static String btye2M(long size) {
		double temp = (double) size;
		StringBuffer buffer = new StringBuffer();
		if ((temp / 1024 / 1024) > 1000) {
			temp = temp / 1024 / 1024 / 1024;
			BigDecimal b = new BigDecimal(temp);
			temp = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
			buffer.append(temp);
			buffer.append("G");
		} else {
			temp = temp / 1024 / 1024;
			buffer.append((int) temp);
			buffer.append("M");
		}
		return buffer.toString();
	}

	// 获取字节数组
	public static byte[] getBytes(InputStream is) {
		ByteArrayOutputStream bot = new ByteArrayOutputStream();
		byte[] bytes = new byte[2048];
		int rc = 0;
		try {
			while ((rc = is.read(bytes, 0, 2048)) != -1) {
				bot.write(bytes, 0, rc);
			}
			bot.flush();
			bot.close();
			return bot.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	// 获取字节数组
	public static byte[] getBytes(File file) {
		byte[] buffer = null;
		try {
			FileInputStream fis = new FileInputStream(file);
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
			byte[] b = new byte[2048];
			int n;
			while ((n = fis.read(b)) != -1) {
				bos.write(b, 0, n);
			}
			fis.close();
			bos.close();
			buffer = bos.toByteArray();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public static boolean isJpg(String url) {
		if (url == null) {
			return false;
		}
		int index = url.lastIndexOf(".");
		if (index > -1 && index < url.length()) {
			String ext = url.substring(index + 1); // --扩展名
			if (ext != null
					&& (ext.equalsIgnoreCase("jpg") || ext
							.equalsIgnoreCase("jpeg"))) {
				return true;
			}
		}
		return false;
	}
}
