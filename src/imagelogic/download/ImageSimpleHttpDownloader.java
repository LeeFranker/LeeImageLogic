package imagelogic.download;

import imagelogic.utils.ImageUtils;
import imagelogic.utils.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.conn.ConnectTimeoutException;

/**
 * 图片下载实现类
 * 
 * @author LeeFranker
 * 
 */
public class ImageSimpleHttpDownloader implements ImageDownloader {
	private static final String TAG = "ImageDownloader";
	private static final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; zh-CN; rv:1.9.1.2) Gecko/20090803 Fedora/3.5.2-2.fc11 Firefox/3.5.2";

	private static final int IO_BUFFER_SIZE = 8 * 1024; // 8k
	private static final int CONN_TIMEOUT = 15 * 1000; // 网络连接超时时间
	private static final int READ_TIMEOUT = 10 * 1000; // 网络读取超时时间

	// 下载图片
	public boolean downloadToLocalStreamByUrl(String urlString,
			OutputStream outputStream) {
		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		FlushedInputStream in = null;
		InputStream stream = null;
		try {
			Log.d(TAG, "请求地址:" + urlString);
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(CONN_TIMEOUT);
			urlConnection.setReadTimeout(READ_TIMEOUT);
			urlConnection.setDoOutput(false);
			urlConnection.setDoInput(true);
			urlConnection.setUseCaches(false);
			urlConnection.addRequestProperty("User-Agent", USER_AGENT);
			int code = urlConnection.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				stream = urlConnection.getInputStream();
				if (stream != null) {
					in = new FlushedInputStream(new BufferedInputStream(stream,
							IO_BUFFER_SIZE));
					out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
					int b;
					while ((b = in.read()) != -1) {
						out.write(b);
					}

					return true;
				}
			}
		} catch (final ConnectTimeoutException e) {
			Log.e(TAG, "ConnectTimeoutException:" + e.getMessage());
		} catch (final IOException e) {
			Log.e(TAG, "IOException:" + e.getMessage());
		} catch (final Exception e) {
			Log.e(TAG, "Exception:" + e.getMessage());
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
				if (stream != null) {
					stream.close();
				}
			} catch (final IOException e) {
				Log.e(TAG, "IOException:" + e.getMessage());
			}
		}
		return false;
	}

	/**
	 * @ClassName: FlushedInputStream
	 * @Description: 重写filter为了防止android在网络比较慢的时候inputstream会中断的问题
	 * @author 王力
	 * @date 2013-2-5 下午02:43:04
	 */
	private class FlushedInputStream extends FilterInputStream {

		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int by_te = read();
					if (by_te < 0) {
						break;
					} else {
						bytesSkipped = 1;
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}

	@Override
	public byte[] downloadBytesByUrl(String urlString) {
		HttpURLConnection urlConnection = null;
		FlushedInputStream in = null;
		InputStream stream = null;
		try {
			Log.d(TAG, "请求地址:" + urlString);
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(CONN_TIMEOUT);
			urlConnection.setReadTimeout(READ_TIMEOUT);
			urlConnection.setDoOutput(false);
			urlConnection.setDoInput(true);
			urlConnection.setUseCaches(false);
			urlConnection.addRequestProperty("User-Agent", USER_AGENT);
			int code = urlConnection.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				stream = urlConnection.getInputStream();
				if (stream != null) {
					in = new FlushedInputStream(new BufferedInputStream(stream,
							IO_BUFFER_SIZE));
					return ImageUtils.getBytes(in);
				}
			}

		} catch (final IOException e) {
			Log.e(TAG, "IOException:" + e.getMessage());

		} catch (final Exception e) {
			Log.e(TAG, "Exception:" + e.getMessage());

		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (in != null) {
					in.close();
				}
				if (stream != null) {
					stream.close();
				}
			} catch (final IOException e) {
				Log.e(TAG, "IOException:" + e.getMessage());
			}
		}
		return null;
	}
}
