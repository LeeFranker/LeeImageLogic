package imagelogic.memory;

import imagelogic.utils.ImageUtils;
import imagelogic.utils.Log;

import java.util.LinkedHashMap;
import java.util.Map;

import android.annotation.SuppressLint;

/**
 * 图片内存缓存类
 * 
 * @author LeeFranker
 * 
 * @param <K>
 * @param <V>
 */
@SuppressLint("DefaultLocale")
public class ImageLruMemoryCache<K, V> {
	private static final String TAG = "ImageLruMemoryCache";

	// Java程序中利用LinkedHashMap可以非常方便的实现基于LRU策略的缓存
	private final LinkedHashMap<K, V> map;
	// 内存缓存统计容量
	private int size;
	// 内存缓存最大容量
	private int maxSize;
	// 内存缓存添加个数统计
	private int putCount;
	// 内存缓存对象重新创建个数统计
	private int createCount;
	// 内存缓存删除K个数统计
	private int evictionCount;
	// 获取V不为空的个数统计
	private int hitCount;
	// 获取V为空的个数统计
	private int missCount;

	/**
	 * @param maxSize
	 */
	public ImageLruMemoryCache(int maxSize) {
		if (maxSize <= 0) {
			Log.e(TAG, "maxSize <= 0");
			throw new IllegalArgumentException("maxSize <= 0");
		}
		this.maxSize = maxSize;
		this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
	}

	/**
	 * @Title: get
	 * @Description: 获取key对应的value
	 * @param @param key
	 * @param @return
	 * @return V
	 * @throws
	 */
	public final V get(K key) {
		if (key == null) {
			Log.e(TAG, "key == null");
			throw new NullPointerException("key == null");
		}
		V mapValue;
		synchronized (this) {
			mapValue = map.get(key);
			if (mapValue != null) {
				hitCount++;
				return mapValue;
			}
			missCount++;
		}
		// 重新创建value
		V createdValue = create(key);
		if (createdValue == null) {
			return null;
		}
		// 注意:下面操作不执行！！！
		synchronized (this) {
			createCount++;
			mapValue = map.put(key, createdValue);
			if (mapValue != null) {
				map.put(key, mapValue);
			} else {
				size += safeSizeOf(key, createdValue);
			}
		}
		if (mapValue != null) {
			entryRemoved(false, key, createdValue, mapValue);
			return mapValue;
		} else {
			trimToSize(maxSize);
			return createdValue;
		}
	}

	/**
	 * @Title: put
	 * @Description: 存储value
	 * @param @param key
	 * @param @param value
	 * @param @return
	 * @return V
	 * @throws
	 */
	public final V put(K key, V value) {
		if (key == null || value == null) {
			Log.e(TAG, "key == null || value == null");
			throw new NullPointerException("key == null || value == null");
		}
		V previous;
		synchronized (this) {
			putCount++;
			// 统计容量缓存大小
			size += safeSizeOf(key, value);
			// 返回之前key对应的旧数据
			previous = map.put(key, value);
			if (previous != null) {
				// 如果有旧数据，容量不变
				size -= safeSizeOf(key, previous);
			}
		}
		if (previous != null) {
			// 删除旧的，添加新的
			entryRemoved(false, key, previous, value);
		}
		// 增加新value，随时清理缓存
		trimToSize(maxSize);
		return previous;
	}

	/**
	 * @Title: trimToSize
	 * @Description: 清理缓存
	 * @param @param maxSize
	 * @return void
	 * @throws
	 */
	private void trimToSize(int maxSize) {
		while (true) {
			K key;
			V value;
			synchronized (this) {
				if (size < 0 || (map.isEmpty() && size != 0)) {
					Log.e(TAG, "清理缓存发生异常情况");
				}
				// Log.d(TAG, "内存积累总大小:" + ImageUtils.btye2M(size));
				// Log.d(TAG, "内存总大小:" + ImageUtils.btye2M(maxSize));
				if (size <= maxSize || map.isEmpty()) {
					android.util.Log.d(TAG,
							"size<=maxSize，不用清理缓存" + ImageUtils.btye2M(size));
					break;
				} else {
					android.util.Log.i(TAG,
							"size>maxSize，可以清理缓存" + ImageUtils.btye2M(maxSize));
				}
				// 移除操作
				Map.Entry<K, V> toEvict = map.entrySet().iterator().next();
				key = toEvict.getKey();
				value = toEvict.getValue();
				map.remove(key);
				size -= safeSizeOf(key, value);
				evictionCount++;
			}
			if (value != null) {
				entryRemoved(true, key, value, null);
			}
		}
	}

	/**
	 * @Title: remove
	 * @Description: 移除内存中的value
	 * @param @param key
	 * @param @return
	 * @return V
	 * @throws
	 */
	public final V remove(K key) {
		if (key == null) {
			Log.e(TAG, "key == null");
			throw new NullPointerException("key == null");
		}
		// key对应的数据
		V previous;
		synchronized (this) {
			// 移除缓存中key值
			previous = map.remove(key);
			if (previous != null) {
				// 容量减少
				size -= safeSizeOf(key, previous);
			}
		}
		if (previous != null) {
			entryRemoved(false, key, previous, null);
		}
		return previous;
	}

	protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {

	}

	protected V create(K key) {
		return null;
	}

	private int safeSizeOf(K key, V value) {
		int result = sizeOf(key, value);
		if (result < 0) {
			Log.e(TAG, "获取图片自身大小异常");
		}
		return result;
	}

	protected int sizeOf(K key, V value) {
		return 1;
	}

	// 清理缓存，移除所有key
	public final void evictAll() {
		trimToSize(-1);
	}

	// 返回当前缓存的容量
	public synchronized final int size() {
		return size;
	}

	// 返回缓存的最大容量
	public synchronized final int maxSize() {
		return maxSize;
	}

	// 返回获取value不为null的次数
	public synchronized final int hitCount() {
		return hitCount;
	}

	// 返回获取value为null，重新创建value的次数
	public synchronized final int missCount() {
		return missCount;
	}

	// 返回创建value的次数
	public synchronized final int createCount() {
		return createCount;
	}

	// 返回添加value的次数
	public synchronized final int putCount() {
		return putCount;
	}

	// 返回移除vlaue的次数
	public synchronized final int evictionCount() {
		return evictionCount;
	}

	// 返回当前缓存的拷贝对象
	public synchronized final Map<K, V> snapshot() {
		return new LinkedHashMap<K, V>(map);
	}

	// 返回当前缓存的内存个数
	public synchronized final int mapSize() {
		int mapSize = 0;
		if (map != null) {
			mapSize = map.size();
		}
		return mapSize;
	}

	// 返回内存缓存访问信息
	public synchronized final String toString() {
		int accesses = hitCount + missCount;
		int hitPercent = accesses != 0 ? (100 * hitCount / accesses) : 0;
		return String
				.format("ImageLruMemoryCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]",
						maxSize, hitCount, missCount, hitPercent);
	}
}
