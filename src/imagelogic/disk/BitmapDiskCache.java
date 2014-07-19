package imagelogic.disk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BitmapDiskCache extends DiskBasedCache {
	public static final int DEFAULT_COMPRESS_QUALITY = 70;
	public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
	private Bitmap.CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
	private int compressQuality = DEFAULT_COMPRESS_QUALITY;

	public BitmapDiskCache(File rootDirectory) {
		super(rootDirectory);
	}

	public BitmapDiskCache(File rootDirectory, int maxCacheSizeInBytes) {
		super(rootDirectory, maxCacheSizeInBytes);
	}

	public boolean put(String key, Bitmap bitmap, boolean isJpg) {
		File file = getFileForKey(key);
		FileOutputStream fos = null;
		ByteArrayOutputStream bos = null;
		try {
			fos = new FileOutputStream(file);
			if (isJpg) {
				bitmap.compress(CompressFormat.JPEG, compressQuality, fos);
			} else {
				bitmap.compress(CompressFormat.PNG, compressQuality, fos);
			}

			int size = (int) file.length();
			CacheHeader e = new CacheHeader(key, size);
			pruneIfNeeded(size);
			putEntry(key, e);
			return true;
		} catch (IOException e) {
			try {
				bos = new ByteArrayOutputStream();
				bitmap.compress(compressFormat, compressQuality, bos);
				bos.flush();
				int size = (int) bos.size();
				CacheHeader entry = new CacheHeader(key, size);
				pruneIfNeeded(size);
				putEntry(key, entry);
				fos = new FileOutputStream(file);
				fos.write(bos.toByteArray());
				return true;
			} catch (Exception e2) {
			} finally {
				try {
					if (fos != null) {
						fos.close();
					}
				} catch (Exception e3) {
				}
				try {
					if (bos != null) {
						bos.close();
					}
				} catch (Exception e3) {
				}
			}
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) {
			}
		}
		try {
			boolean deleted = file.delete();
			if (!deleted) {
				Log.d(TAG, "Could not clean up file " + file.getAbsolutePath());
			}
		} catch (Exception e) {
		}
		return false;
	}

	public Bitmap get(String key) {
		byte[] data = getData(key);
		if (data != null) {
			final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
					data.length);
			Log.d(TAG, "磁盘缓存获取图片数据ok");
			return bitmap;
		}
		return null;
	}
}
