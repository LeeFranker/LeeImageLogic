package imagelogic.download;

import java.io.OutputStream;

/**
 * 图片下载接口
 * 
 * @author LeeFranker
 * 
 */
public interface ImageDownloader {

	/**
	 * @Title: downloadToLocalStreamByUrl
	 * @Description: 从服务端获取图片数据
	 * @param @param urlString 图片地址
	 * @param @param outputStream 输出流
	 * @return boolean true 成功 false 失败
	 * @throws
	 */
	public boolean downloadToLocalStreamByUrl(String urlString,
			OutputStream outputStream);

	/**
	 * @Title: downloadBytesByUrl
	 * @Description: 从服务端获取图片数据
	 * @param @param urlString 图片地址
	 * @return 字节数组
	 * @throws
	 */
	public byte[] downloadBytesByUrl(String urlString);
}
