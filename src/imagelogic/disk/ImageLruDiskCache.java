package imagelogic.disk;

import imagelogic.utils.Log;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 图片磁盘缓存类
 * 
 * @author LeeFranker
 * 
 */
public final class ImageLruDiskCache implements Closeable {

	private static final String TAG = "ImageLruDiskCache";// lOG

	private static final String MAGIC = "ImageLruDiskCache";// 日志内容抬头名称

	private static final String JOURNAL_FILE = "journal";// 日志文件名字

	private static final String JOURNAL_FILE_TMP = "journal.tmp";// 临时日志文件名字

	private static final String VERSION_1 = "1";

	private static final long ANY_SEQUENCE_NUMBER = -1;

	private static final String CLEAN = "CLEAN";
	private static final String DIRTY = "DIRTY";
	private static final String REMOVE = "REMOVE";
	private static final String READ = "READ";

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final int IO_BUFFER_SIZE = 8 * 1024;

	private static final LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap<String, Entry>(
			0, 0.75f, true);

	private final File directory;// 文件目录
	private final File journalFile;// 日志对象
	private final File journalFileTmp;// 临时日志对象

	private final int appVersion;
	private final long maxSize;
	private final int valueCount;
	private long size = 0;

	private Writer journalWriter;

	private int redundantOpCount;
	private long nextSequenceNumber = 0;

	@SuppressWarnings("unchecked")
	private static <T> T[] copyOfRange(T[] original, int start, int end) {
		final int originalLength = original.length;
		if (start > end) {
			throw new IllegalArgumentException();
		}
		if (start < 0 || start > originalLength) {
			throw new ArrayIndexOutOfBoundsException();
		}
		final int resultLength = end - start;
		final int copyLength = Math.min(resultLength, originalLength - start);
		final T[] result = (T[]) Array.newInstance(original.getClass()
				.getComponentType(), resultLength);
		System.arraycopy(original, start, result, 0, copyLength);
		return result;
	}

	// IO流变成字符串
	public static String readFully(Reader reader) throws IOException {
		try {
			StringWriter writer = new StringWriter();
			char[] buffer = new char[1024];
			int count;
			while ((count = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, count);
			}
			return writer.toString();
		} finally {
			reader.close();
		}
	}

	// 读取IO流一行的内容
	public static String readAsciiLine(InputStream in) throws IOException {
		StringBuilder result = new StringBuilder(80);
		while (true) {
			int c = in.read();
			if (c == -1) {
				throw new EOFException();
			} else if (c == '\n') {
				break;
			}
			result.append((char) c);
		}
		int length = result.length();
		if (length > 0 && result.charAt(length - 1) == '\r') {
			result.setLength(length - 1);
		}
		return result.toString();
	}

	// 关掉IO流
	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (RuntimeException rethrown) {
				throw rethrown;
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
		}
	}

	// 大于2000张清理文件
	private boolean journalRebuildRequired() {
		final int REDUNDANT_OP_COMPACT_THRESHOLD = 2000;
		Log.d(TAG, "磁盘图片个数:" + redundantOpCount);
		Log.d(TAG, "磁盘缓存个数:" + lruEntries.size());
		return redundantOpCount >= REDUNDANT_OP_COMPACT_THRESHOLD
				&& redundantOpCount >= lruEntries.size();
	}

	// 清理map缓存内容
	private void trimToSize() throws IOException {
		while (size > maxSize) {
			final Map.Entry<String, Entry> toEvict = lruEntries.entrySet()
					.iterator().next();
			remove(toEvict.getKey());
		}
	}

	// 生成日志文件
	private synchronized void rebuildJournal() throws IOException {
		if (journalWriter != null) {
			journalWriter.close();
		}
		Writer writer = new BufferedWriter(new FileWriter(journalFileTmp),
				IO_BUFFER_SIZE);
		writer.write(MAGIC);
		writer.write("\n");
		writer.write(VERSION_1);
		writer.write("\n");
		writer.write(Integer.toString(appVersion));
		writer.write("\n");
		writer.write(Integer.toString(valueCount));
		writer.write("\n");
		writer.write("\n");

		for (Entry entry : lruEntries.values()) {
			if (entry.currentEditor != null) {
				writer.write(DIRTY + ' ' + entry.key + '\n');
			} else {
				writer.write(CLEAN + ' ' + entry.key + entry.getLengths()
						+ '\n');
			}
		}
		writer.close();
		journalFileTmp.renameTo(journalFile);// 更换临时日志名字
		journalWriter = new BufferedWriter(new FileWriter(journalFile, true),
				IO_BUFFER_SIZE);
	}

	// 线程池
	private final ExecutorService executorService = new ThreadPoolExecutor(0,
			1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

	// 清理任务
	private final Callable<Void> cleanupCallable = new Callable<Void>() {
		@Override
		public Void call() throws Exception {
			synchronized (ImageLruDiskCache.this) {
				if (journalWriter == null) {
					return null;
				}
				// 清理内存
				trimToSize();
				if (journalRebuildRequired()) {
					// 重新创建日志
					rebuildJournal();
					redundantOpCount = 0;
				}
			}
			return null;
		}
	};

	// 磁盘缓存构造方法
	private ImageLruDiskCache(File directory, int appVersion, int valueCount,
			long maxSize) {
		this.directory = directory;// 磁盘缓存目录
		this.appVersion = appVersion;// 系统版本号
		this.journalFile = new File(directory, JOURNAL_FILE);// 日志对象
		this.journalFileTmp = new File(directory, JOURNAL_FILE_TMP);// 临时日志对象
		this.valueCount = valueCount;
		this.maxSize = maxSize;// 缓存容量大小
	}

	/**
	 * @Title: open
	 * @Description: 获取磁盘缓存对象
	 * @param @param directory 目录
	 * @param @param appVersion 系统版本
	 * @param @param valueCount
	 * @param @param maxSize 缓存容量最大个数
	 * @param @return
	 * @param @throws IOException
	 * @return ImageLruDiskCache
	 * @throws
	 */
	public static ImageLruDiskCache open(File directory, int appVersion,
			int valueCount, long maxSize) throws IOException {
		if (maxSize <= 0)
			throw new IOException("maxSize<=0");

		if (valueCount <= 0)
			throw new IOException("valueCount <= 0");

		if (directory != null && !directory.exists())
			directory.mkdirs();

		ImageLruDiskCache cache = new ImageLruDiskCache(directory, appVersion,
				valueCount, maxSize);

		if (cache != null && cache.journalFile.exists()) {
			try {
				Log.d(TAG, "日志文件存在，读取日志文件");
				cache.readJournal();
				cache.processJournal();
				cache.journalWriter = new BufferedWriter(new FileWriter(
						cache.journalFile, true), IO_BUFFER_SIZE);
				Log.d(TAG, "日志文件存在，读取日志文件ok");
				return cache;
			} catch (IOException e) {
				Log.e(TAG, "error:" + e.getMessage());
				try {
					cache.delete();
				} catch (IOException e1) {
					Log.e(TAG, "error:" + e1.getMessage());
				}
			}
		}
		cache = new ImageLruDiskCache(directory, appVersion, valueCount,
				maxSize);
		deleteIfExists(cache.journalFile);
		deleteIfExists(cache.journalFileTmp);
		cache.journalFileTmp.createNewFile();
		cache.rebuildJournal();
		Log.d(TAG, "删除日志文件，重新创建新的日志文件");
		return cache;
	}

	// 读取日志信息
	private void readJournal() throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(
				journalFile), IO_BUFFER_SIZE);
		try {
			String magic = readAsciiLine(in);
			String version = readAsciiLine(in);
			String appVersionString = readAsciiLine(in);
			String valueCountString = readAsciiLine(in);
			String blank = readAsciiLine(in);
			if (!MAGIC.equals(magic) || !VERSION_1.equals(version)
					|| !Integer.toString(appVersion).equals(appVersionString)
					|| !Integer.toString(valueCount).equals(valueCountString)
					|| !"".equals(blank)) {
				throw new IOException("unexpected journal header: [" + magic
						+ ", " + version + ", " + valueCountString + ", "
						+ blank + "]");
			}
			while (true) {
				try {
					readJournalLine(readAsciiLine(in));
				} catch (EOFException e) {
					e.printStackTrace();
					break;
				}
			}
		} finally {
			closeQuietly(in);
		}
	}

	// 读取日志信息
	private void readJournalLine(String line) throws IOException {
		String[] parts = line.split(" ");
		if (parts.length < 2) {
			Log.e(TAG, "日志结构error");
			throw new IOException("unexpected journal line: " + line);
		}
		String key = parts[1];
		if (parts[0].equals(REMOVE) && parts.length == 2) {
			lruEntries.remove(key);
			return;
		}
		Entry entry = lruEntries.get(key);
		if (entry == null) {
			entry = new Entry(key);
			lruEntries.put(key, entry);
		}
		if (parts[0].equals(CLEAN) && parts.length == 2 + valueCount) {
			entry.readable = true;
			entry.currentEditor = null;
			entry.setLengths(copyOfRange(parts, 2, parts.length));
		} else if (parts[0].equals(DIRTY) && parts.length == 2) {
			entry.currentEditor = new Editor(entry);
		} else if (parts[0].equals(READ) && parts.length == 2) {
			// this work was already done by calling lruEntries.get()
		} else {
			Log.e(TAG, "读取日志error");
			throw new IOException("unexpected journal line: " + line);
		}
	}

	// 清理日志
	private void processJournal() throws IOException {
		deleteIfExists(journalFileTmp);
		for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext();) {
			Entry entry = i.next();
			if (entry.currentEditor == null) {
				for (int t = 0; t < valueCount; t++) {
					size += entry.lengths[t];
				}
			} else {
				entry.currentEditor = null;
				for (int t = 0; t < valueCount; t++) {
					deleteIfExists(entry.getCleanFile(t));
					deleteIfExists(entry.getDirtyFile(t));
				}
				i.remove();
			}
		}
	}

	// 删除文件
	private static void deleteIfExists(File file) throws IOException {
		if (file.exists() && !file.delete()) {
			Log.e(TAG, "日志文件删除异常");
			throw new IOException();
		}
	}

	// 判断key值是否合法
	private void validateKey(String key) {
		if (key.contains(" ") || key.contains("\n") || key.contains("\r")) {
			Log.e(TAG, "非法key值");
			throw new IllegalStateException();
		}
	}

	// 获取图片对象
	public synchronized Snapshot get(String key) throws IOException {
		checkNotClosed();
		validateKey(key);
		Entry entry = lruEntries.get(key);
		if (entry == null) {
			return null;
		}
		if (!entry.readable) {
			return null;
		}
		InputStream[] ins = new InputStream[valueCount];
		try {
			for (int i = 0; i < valueCount; i++) {
				ins[i] = new FileInputStream(entry.getCleanFile(i));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		redundantOpCount++;
		journalWriter.append(READ + ' ' + key + '\n');
		if (journalRebuildRequired()) {
			Log.e(TAG, "开始清理磁盘文件");
			executorService.submit(cleanupCallable);
		}
		return new Snapshot(key, entry.sequenceNumber, ins);
	}

	// 刷新操作
	public synchronized void flush() throws IOException {
		checkNotClosed();
		trimToSize();
		journalWriter.flush();
	}

	// 根据key值返回编辑对象
	public Editor edit(String key) throws IOException {
		return edit(key, ANY_SEQUENCE_NUMBER);
	}

	private synchronized Editor edit(String key, long expectedSequenceNumber)
			throws IOException {
		checkNotClosed();
		validateKey(key);
		Entry entry = lruEntries.get(key);
		if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER
				&& (entry == null || entry.sequenceNumber != expectedSequenceNumber)) {
			Log.e(TAG, "snapshot对象有问题");
			return null;
		}
		if (entry == null) {
			Log.d(TAG, "重新创建新的磁盘缓存对象");
			entry = new Entry(key);
			lruEntries.put(key, entry);
		} else if (entry.currentEditor != null) {
			Log.w(TAG, "当前磁盘缓存对象还在编辑中");
			return null;
		}
		Editor editor = new Editor(entry);
		entry.currentEditor = editor;
		journalWriter.write(DIRTY + ' ' + key + '\n');
		journalWriter.flush();
		return editor;
	}

	// 返回缓存存储数据的目录对象
	public File getDirectory() {
		return directory;
	}

	// 返回缓存最大个数
	public long maxSize() {
		return maxSize;
	}

	// 返回当前缓存个数
	public synchronized long size() {
		return size;
	}

	// 完成编辑
	private synchronized void completeEdit(Editor editor, boolean success)
			throws IOException {
		checkNotClosed();
		Entry entry = editor.entry;
		if (entry.currentEditor != editor) {
			Log.e(TAG, "edit didn't same");
			throw new IOException();
		}
		if (success && !entry.readable) {
			for (int i = 0; i < valueCount; i++) {
				if (!entry.getDirtyFile(i).exists()) {
					editor.abort();
					Log.e(TAG, "edit didn't create file");
					throw new IOException();
				}
			}
		}

		for (int i = 0; i < valueCount; i++) {
			File dirty = entry.getDirtyFile(i);
			if (success) {
				if (dirty.exists()) {
					Log.d(TAG, "图片存储成功");
					File clean = entry.getCleanFile(i);
					dirty.renameTo(clean);
					long oldLength = entry.lengths[i];
					long newLength = clean.length();
					entry.lengths[i] = newLength;
					size = size - oldLength + newLength;
				}
			} else {
				Log.e(TAG, "图片存储失败");
				deleteIfExists(dirty);
			}
		}
		redundantOpCount++;
		entry.currentEditor = null;
		if (entry.readable | success) {
			entry.readable = true;
			journalWriter.write(CLEAN + ' ' + entry.key + entry.getLengths()
					+ '\n');
			if (success) {
				entry.sequenceNumber = nextSequenceNumber++;
			}
		} else {
			lruEntries.remove(entry.key);
			journalWriter.write(REMOVE + ' ' + entry.key + '\n');
		}

		if (size > maxSize || journalRebuildRequired()) {
			executorService.submit(cleanupCallable);
		}
	}

	// 删除key值对应的对象
	public synchronized boolean remove(String key) throws IOException {
		checkNotClosed();
		validateKey(key);
		Entry entry = lruEntries.get(key);
		if (entry == null || entry.currentEditor != null) {
			return false;
		}
		for (int i = 0; i < valueCount; i++) {
			File file = entry.getCleanFile(i);
			if (!file.delete()) {
				Log.e(TAG, "删除图片文件发生异常 ");
				throw new IOException();
			}
			size -= entry.lengths[i];
			entry.lengths[i] = 0;
		}
		redundantOpCount++;
		journalWriter.append(REMOVE + ' ' + key + '\n');
		lruEntries.remove(key);
		if (journalRebuildRequired()) {
			executorService.submit(cleanupCallable);
		}
		return true;
	}

	// 判断缓存是否关闭
	public boolean isClosed() {
		return journalWriter == null;
	}

	// 检查缓存是否关闭
	private void checkNotClosed() {
		if (journalWriter == null || lruEntries == null) {
			Log.e(TAG, "journalWriter == null||lruEntries==null");
			throw new IllegalStateException();
		}
	}

	// 关闭磁盘缓存并且删除所有文件
	public void delete() throws IOException {
		close();
		deleteContents(directory);
	}

	// 关闭磁盘缓存对象
	public synchronized void close() throws IOException {
		if (journalWriter == null) {
			return;
		}
		for (Entry entry : new ArrayList<Entry>(lruEntries.values())) {
			if (entry.currentEditor != null) {
				entry.currentEditor.abort();
			}
		}
		trimToSize();
		journalWriter.close();
		journalWriter = null;
	}

	// 删除文件
	public synchronized void deleteContents(File file) throws IOException {
		final File to = new File(file.getAbsolutePath()
				+ System.currentTimeMillis());
		file.renameTo(to);
		if (to.exists()) {
			String path = to.getAbsolutePath();
			String deleteCmd = "rm -r " + path;
			Runtime runtime = Runtime.getRuntime();
			try {
				runtime.exec(deleteCmd);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// 输出流变成字符串
	private static String inputStreamToString(InputStream in)
			throws IOException {
		return readFully(new InputStreamReader(in, UTF_8));
	}

	// 获取实体对象
	public final class Snapshot implements Closeable {
		private final String key;
		private final long sequenceNumber;
		private final InputStream[] ins;

		private Snapshot(String key, long sequenceNumber, InputStream[] ins) {
			this.key = key;
			this.sequenceNumber = sequenceNumber;
			this.ins = ins;
		}

		// 根据key值返回对应的编辑对象
		public Editor edit() throws IOException {
			return ImageLruDiskCache.this.edit(key, sequenceNumber);
		}

		// 返回IO流
		public InputStream getInputStream(int index) {
			return ins[index];
		}

		// IO流返回字符串
		public String getString(int index) throws IOException {
			return inputStreamToString(getInputStream(index));
		}

		@Override
		public void close() {
			for (InputStream in : ins) {
				closeQuietly(in);
			}
		}
	}

	/**
	 * @ClassName: Editor
	 * @Description: 磁盘缓存中每个图片对应一个编辑对象
	 * @author 王力
	 * @date 2013-2-6 下午02:59:58
	 * 
	 */
	public final class Editor {
		private final Entry entry;// 实体对象
		private boolean hasErrors;// 判断文件是否保存成功

		// 构造方法
		private Editor(Entry entry) {
			this.entry = entry;
		}

		// 获取文件流
		public InputStream newInputStream(int index) throws IOException {
			synchronized (ImageLruDiskCache.this) {
				if (entry.currentEditor != this) {
					throw new IllegalStateException();
				}
				if (!entry.readable) {
					return null;
				}
				return new FileInputStream(entry.getCleanFile(index));
			}
		}

		// 文件转变为字符串
		public String getString(int index) throws IOException {
			InputStream in = newInputStream(index);
			return in != null ? inputStreamToString(in) : null;
		}

		// 文件转变成输出流
		public OutputStream newOutputStream(int index) throws IOException {
			synchronized (ImageLruDiskCache.this) {
				if (entry.currentEditor != this) {
					throw new IllegalStateException();
				}
				return new FaultHidingOutputStream(new FileOutputStream(
						entry.getDirtyFile(index)));
			}
		}

		// 字符串写入文件
		public void set(int index, String value) throws IOException {
			Writer writer = null;
			try {
				writer = new OutputStreamWriter(newOutputStream(index), UTF_8);
				writer.write(value);
			} finally {
				closeQuietly(writer);
			}
		}

		// 完成编辑
		public void commit() throws IOException {
			if (hasErrors) {
				completeEdit(this, false);
				remove(entry.key);
			} else {
				completeEdit(this, true);
			}
		}

		// 阻止编辑
		public void abort() throws IOException {
			completeEdit(this, false);
		}

		// 重写过滤输出流
		private class FaultHidingOutputStream extends FilterOutputStream {
			private FaultHidingOutputStream(OutputStream out) {
				super(out);
			}

			@Override
			public void write(int oneByte) {
				try {
					out.write(oneByte);
				} catch (IOException e) {
					hasErrors = true;
				}
			}

			@Override
			public void write(byte[] buffer, int offset, int length) {
				try {
					out.write(buffer, offset, length);
				} catch (IOException e) {
					hasErrors = true;
				}
			}

			@Override
			public void close() {
				try {
					out.close();
				} catch (IOException e) {
					hasErrors = true;
				}
			}

			@Override
			public void flush() {
				try {
					out.flush();
				} catch (IOException e) {
					hasErrors = true;
				}
			}
		}
	}

	/**
	 * @ClassName: Entry
	 * @Description: 磁盘缓存map中需要保存的图片对象
	 * @author 王力
	 * @date 2013-2-6 下午02:53:34
	 * 
	 */
	private final class Entry {
		private final String key;
		private final long[] lengths;
		private boolean readable;
		private Editor currentEditor;
		private long sequenceNumber;

		private Entry(String key) {
			this.key = key;
			this.lengths = new long[valueCount];
		}

		public String getLengths() throws IOException {
			StringBuilder result = new StringBuilder();
			for (long size : lengths) {
				result.append(' ').append(size);
			}
			return result.toString();
		}

		private void setLengths(String[] strings) throws IOException {
			if (strings.length != valueCount) {
				throw invalidLengths(strings);
			}
			try {
				for (int i = 0; i < strings.length; i++) {
					lengths[i] = Long.parseLong(strings[i]);
				}
			} catch (NumberFormatException e) {
				throw invalidLengths(strings);
			}
		}

		private IOException invalidLengths(String[] strings) throws IOException {
			Log.e(TAG, "invalidLengths");
			throw new IOException();
		}

		public File getCleanFile(int i) {
			return new File(directory, key + "." + i);
		}

		public File getDirtyFile(int i) {
			return new File(directory, key + "." + i + ".tmp");
		}
	}
}
