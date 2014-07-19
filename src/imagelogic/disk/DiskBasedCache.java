/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package imagelogic.disk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import android.util.Log;

/**
 * Disk Cache
 * 
 * @author LeeFranker
 * 
 */
public class DiskBasedCache {

	protected static final String TAG = "DiskBasedCache";

	/** Map of the Key, CacheHeader pairs */
	private final Map<String, CacheHeader> mEntries = new LinkedHashMap<String, CacheHeader>(
			16, .75f, true);

	/** Total amount of space currently used by the cache in bytes. */
	private long mTotalSize = 0;

	/** The root directory to use for the cache. */
	private final File mRootDirectory;

	/** The maximum size of the cache in bytes. */
	private final int mMaxCacheSizeInBytes;

	/** Default maximum disk usage in bytes. */
	private static final int DEFAULT_DISK_USAGE_BYTES = 5 * 1024 * 1024;

	/** High water mark percentage for the cache */
	private static final float HYSTERESIS_FACTOR = 0.9f;

	private boolean mDiskCacheStarting = true;

	/**
	 * Constructs an instance of the DiskBasedCache at the specified directory.
	 * 
	 * @param rootDirectory
	 *            The root directory of the cache.
	 * @param maxCacheSizeInBytes
	 *            The maximum size of the cache in bytes.
	 */
	public DiskBasedCache(File rootDirectory, int maxCacheSizeInBytes) {
		mRootDirectory = rootDirectory;
		mMaxCacheSizeInBytes = maxCacheSizeInBytes;
	}

	/**
	 * Constructs an instance of the DiskBasedCache at the specified directory
	 * using the default maximum cache size of 5MB.
	 * 
	 * @param rootDirectory
	 *            The root directory of the cache.
	 */
	public DiskBasedCache(File rootDirectory) {
		this(rootDirectory, DEFAULT_DISK_USAGE_BYTES);
	}

	/**
	 * Clears the cache. Deletes all cached files from disk.
	 */
	public synchronized void clear() {
		File[] files = mRootDirectory.listFiles();
		if (files != null) {
			for (File file : files) {
				file.delete();
			}
		}
		mEntries.clear();
		mTotalSize = 0;
		mDiskCacheStarting = false;
		Log.d(TAG, "Cache cleared.");
	}

	/**
	 * Returns the cache bytes with the specified key if it exists, null
	 * otherwise.
	 */
	public byte[] getData(String key) {
		CacheHeader entry = mEntries.get(key);
		// if the entry does not exist and disk not init complete or size is 0,
		// return.
		if ((entry == null && !mDiskCacheStarting)
				|| (entry != null && entry.size == 0)) {
			return null;
		}
		File file = getFileForKey(key);
		if (!file.exists() || file.length() == 0) {
			return null;
		}
		CountingInputStream cis = null;
		try {
			cis = new CountingInputStream(new FileInputStream(file));
			byte[] data = streamToBytes(cis,
					(int) (file.length() - cis.bytesRead));
			return data;
		} catch (IOException e) {
			Log.d(TAG, file.getAbsolutePath() + ": " + e.toString());
			remove(key);
			return null;
		} finally {
			if (cis != null) {
				try {
					cis.close();
				} catch (IOException ioe) {
					return null;
				}
			}
		}
	}

	/**
	 * Initializes the DiskBasedCache by scanning for all files currently in the
	 * specified root directory. Creates the root directory if necessary.
	 */
	public synchronized void initialize() {
		if (!mRootDirectory.exists()) {
			if (!mRootDirectory.mkdirs()) {
				Log.e(TAG,
						"Unable to create cache dir %s"
								+ mRootDirectory.getAbsolutePath());
			}
			return;
		}
		File[] files = mRootDirectory.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			FileInputStream fis = null;
			try {
				CacheHeader entry = new CacheHeader();
				entry.key = file.getName();
				entry.size = file.length();
				if (entry.size == 0) {
					throw new RuntimeException("no size file");
				}
				putEntry(entry.key, entry);
			} catch (Exception e) {
				e.printStackTrace();
				if (file != null) {
					file.delete();
				}
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (IOException ignored) {
				}
			}
		}
		mDiskCacheStarting = false;
	}

	/**
	 * Invalidates an entry in the cache.
	 * 
	 * @param key
	 *            Cache key
	 * @param fullExpire
	 *            True to fully expire the entry, false to soft expire
	 */
	public synchronized void invalidate(String key, boolean fullExpire) {

	}

	public boolean put(String key, byte[] data) {
		int size = data.length;
		pruneIfNeeded(size);
		File file = getFileForKey(key);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			CacheHeader e = new CacheHeader(key, size);
			fos.write(data);
			fos.close();
			putEntry(key, e);
			return true;
		} catch (IOException e) {
		}
		boolean deleted = file.delete();
		if (!deleted) {
			Log.d(TAG, "Could not clean up file " + file.getAbsolutePath());
		}
		return false;
	}

	public OutputStream getOutputStream(String key) throws Exception {
		File file = null;
		try {
			file = getFileForKey(key);
			FileOutputStream fos = new FileOutputStream(file);
			return fos;
		} catch (Exception e) {
			boolean deleted = file.delete();
			if (!deleted) {
				Log.d(TAG, "Could not clean up file " + file.getAbsolutePath());
			}
			throw e;
		}

	}

	public OutputStream getTempOutputStream(String key) throws Exception {
		File file = null;
		try {
			file = getFileForKey(key + ".temp");
			FileOutputStream fos = new FileOutputStream(file);
			return fos;
		} catch (Exception e) {
			boolean deleted = file.delete();
			if (!deleted) {
				Log.d(TAG, "Could not clean up file " + file.getAbsolutePath());
			}
			throw e;
		}

	}

	public void delTempFile(String key) throws Exception {
		File file = null;
		try {
			file = getFileForKey(key + ".temp");
			file.delete();
		} catch (Exception e) {
			boolean deleted = file.delete();
			if (!deleted) {
				Log.d(TAG, "Could not clean up file " + file.getAbsolutePath());
			}
			throw e;
		}

	}

	public void commit(String key) {
		File file = null;
		try {
			file = getFileForKey(key + ".temp");
			if (file.length() == 0) {
				file.delete();
				return;
			}
			File newfile = getFileForKey(key);
			file.renameTo(newfile);
			long size = file.length();
			pruneIfNeeded((int) size);
			CacheHeader e = new CacheHeader(key, size);
			putEntry(key, e);
		} catch (Exception e) {
			if (file != null) {
				boolean deleted = file.delete();
				if (!deleted) {
					Log.d(TAG,
							"Could not clean up file " + file.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Removes the specified key from the cache if it exists.
	 */
	public synchronized void remove(String key) {
		boolean deleted = getFileForKey(key).delete();
		removeEntry(key);
		if (!deleted) {
			Log.d(TAG, "Could not delete cache entry for key=" + key
					+ ", filename=" + getFilenameForKey(key));
		}
	}

	/**
	 * Creates a pseudo-unique filename for the specified cache key.
	 * 
	 * @param key
	 *            The key to generate a file name for.
	 * @return A pseudo-unique filename.
	 */
	private String getFilenameForKey(String key) {
		int firstHalfLength = key.length() / 2;
		String localFilename = String.valueOf(key.substring(0, firstHalfLength)
				.hashCode());
		localFilename += String.valueOf(key.substring(firstHalfLength)
				.hashCode());
		return localFilename;
	}

	/**
	 * Returns a file object for the given cache key.
	 */
	public File getFileForKey(String key) {
		return new File(mRootDirectory, key);
	}

	/**
	 * Prunes the cache to fit the amount of bytes specified.
	 * 
	 * @param neededSpace
	 *            The amount of bytes we are trying to fit into the cache.
	 */
	protected synchronized void pruneIfNeeded(int neededSpace) {
		if ((mTotalSize + neededSpace) < mMaxCacheSizeInBytes) {
			return;
		}
		Log.d(TAG, "Pruning old cache entries." + (mTotalSize / 1024));

		Iterator<Map.Entry<String, CacheHeader>> iterator = mEntries.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, CacheHeader> entry = iterator.next();
			CacheHeader e = entry.getValue();
			boolean deleted = getFileForKey(e.key).delete();
			if (deleted) {
				mTotalSize -= e.size;
			} else {
				Log.d(TAG, "Could not delete cache entry for key=" + e.key
						+ ", filename=" + e.key);
			}
			iterator.remove();
			if ((mTotalSize + neededSpace) < mMaxCacheSizeInBytes
					* HYSTERESIS_FACTOR) {
				break;
			}
		}
	}

	/**
	 * Puts the entry with the specified key into the cache.
	 * 
	 * @param key
	 *            The key to identify the entry by.
	 * @param entry
	 *            The entry to cache.
	 */
	protected synchronized void putEntry(String key, CacheHeader entry) {
		if (!mEntries.containsKey(key)) {
			mTotalSize += entry.size;
		} else {
			CacheHeader oldEntry = mEntries.get(key);
			mTotalSize += (entry.size - oldEntry.size);
		}
		mEntries.put(key, entry);
	}

	/**
	 * Removes the entry identified by 'key' from the cache.
	 */
	private void removeEntry(String key) {
		CacheHeader entry = mEntries.get(key);
		if (entry != null) {
			mTotalSize -= entry.size;
			mEntries.remove(key);
		}
	}

	/**
	 * Reads the contents of an InputStream into a byte[].
	 * */
	private static byte[] streamToBytes(InputStream in, int length)
			throws IOException {
		byte[] bytes = new byte[length];
		int count;
		int pos = 0;
		while (pos < length
				&& ((count = in.read(bytes, pos, length - pos)) != -1)) {
			pos += count;
		}
		if (pos != length) {
			throw new IOException("Expected " + length + " bytes, read " + pos
					+ " bytes");
		}
		return bytes;
	}

	/**
	 * Handles holding onto the cache headers for an entry.
	 */
	protected static class CacheHeader {
		/**
		 * The size of the data identified by this CacheHeader. (This is not
		 * serialized to disk.
		 */
		public long size;

		/** The key that identifies the cache entry. */
		public String key;

		private CacheHeader() {
		}

		public CacheHeader(String key, long length) {
			this.key = key;
			this.size = length;
		}

	}

	private static class CountingInputStream extends FilterInputStream {
		private int bytesRead = 0;

		private CountingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			int result = super.read();
			if (result != -1) {
				bytesRead++;
			}
			return result;
		}

		@Override
		public int read(byte[] buffer, int offset, int count)
				throws IOException {
			int result = super.read(buffer, offset, count);
			if (result != -1) {
				bytesRead += result;
			}
			return result;
		}
	}

}
