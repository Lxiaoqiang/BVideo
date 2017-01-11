package bvideo.lhq.com.bvideo.camera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;


import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

public class AvcEncoder {

	private MediaCodec mediaCodec;
	int m_width;
	int m_height;
	byte[] m_info = null;

	private byte[] yuv420 = null;
	private static final String VCODEC = "video/avc";
	private BufferedOutputStream outputStream;

	private int cAmeraStyle;

	@SuppressLint("NewApi")
	public AvcEncoder(int width, int height, int framerate, int bitrate) throws Exception{

		m_width = width;
		m_height = height;
		yuv420 = new byte[width * height * 4];
		cAmeraStyle = chooseColorFormat();

		mediaCodec = MediaCodec.createEncoderByType(VCODEC);
		// 视频
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc",
				width, height);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);

		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, cAmeraStyle);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
		mediaCodec.configure(mediaFormat, null, null,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
		mediaCodec.start();

	}

	@SuppressLint("NewApi")
	public void close() {
		try {
			mediaCodec.stop();
			mediaCodec.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void AvcEncoder() {
		File f = new File(Environment.getExternalStorageDirectory(),
				"video_encoded.264");
		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(f));
			Log.i("AvcEncoder", "outputStream initialized");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressLint("NewApi")
	public int offerEncoder(byte[] input) {
		int pos = 0;

	/*	if (cAmeraStyle == 21) {
			Log.e("onPreviewFrame", "swapNV21toNV12--->"+input.length);
			Log.e("onPreviewFrame", "swapNV21toNV12--->width:"+m_width+",height:"+m_height);
			swapNV21toNV12(input, yuv420, m_width, m_height);
		} else {
			Log.e("onPreviewFrame", "swapNV21toI420:input--->"+input.length);
			Log.e("onPreviewFrame", "swapNV21toI420--->width:"+m_width+",height:"+m_height);
			swapNV21toI420(input, yuv420, m_width, m_height);
		}*/

		try {	
			ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
			ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
			int inputBufferIndex = mediaCodec.dequeueInputBuffer(10000);
			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				inputBuffer.put(input);
				mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length,
						System.nanoTime(), 0);
			}

			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,
					0);

			while (outputBufferIndex >= 0) {
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				byte[] outData = new byte[bufferInfo.size];
				outputBuffer.get(outData);
				// outputStream.write(outData, 0, outData.length);
				//
				// // 记录pps和sps
				// if (outData[0] == 0 && outData[1] == 0 && outData[2] == 0
				// && outData[3] == 1 && outData[4] == 103) {
				// mPpsSps = outData;
				// } else if (outData[0] == 0 && outData[1] == 0
				// && outData[2] == 0 && outData[3] == 1
				// && outData[4] == 101) {
				//
				// // 在关键帧前面加上pps和sps数据
				// byte[] iframeData = new byte[mPpsSps.length
				// + outData.length];
				// System.arraycopy(mPpsSps, 0, iframeData, 0, mPpsSps.length);
				// System.arraycopy(outData, 0, iframeData, mPpsSps.length,
				// outData.length);
				// outData = iframeData;
				// }
				//直播推流
//				if (StartVideo.getInstance().pushVideoH264(outData,
//						outData.length)) {
//					//Log.e("tag", "-------------成功--------------------");
//				}

				mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
				outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,
						0);

			}

		} catch (Throwable t) {
			Log.e("tag", "--------------error-------------------");
			t.printStackTrace();
		}

		return pos;
	}

	@SuppressLint("NewApi")
	private int chooseColorFormat() {
		MediaCodecInfo ci = null;

		int nbCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < nbCodecs; i++) {
			MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
			if (!mci.isEncoder()) {
				continue;
			}

			String[] types = mci.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (types[j].equalsIgnoreCase(VCODEC)) {
					// Log.i(TAG, String.format("encoder %s types: %s",
					// mci.getName(), types[j]));
					ci = mci;
					break;
				}
			}
		}

		int matchedColorFormat = 0;
		MediaCodecInfo.CodecCapabilities cc = ci.getCapabilitiesForType(VCODEC);
		for (int i = 0; i < cc.colorFormats.length; i++) {
			int cf = cc.colorFormats[i];
			
			if (cf >= cc.COLOR_Format12bitRGB444
					&& cf <= cc.COLOR_Format8bitRGB332) {
				if (cf > matchedColorFormat) {
					matchedColorFormat = cf;
				}
			}
		}

		return matchedColorFormat;
	}

	private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width,
			int height) {

		System.arraycopy(yv12bytes, 0, i420bytes, 0, width * height);
		System.arraycopy(yv12bytes, width * height + width * height / 4,
				i420bytes, width * height, width * height / 4);
		System.arraycopy(yv12bytes, width * height, i420bytes, width * height
				+ width * height / 4, width * height / 4);
	}

	public static byte[] YV12toYUV420Planar(byte[] input, byte[] output,
			int width, int height) {

		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		System.arraycopy(input, 0, output, 0, frameSize); // Y
		System.arraycopy(input, frameSize, output, frameSize + qFrameSize,
				qFrameSize);
		System.arraycopy(input, frameSize + qFrameSize, output, frameSize,
				qFrameSize); // Cb (U)

		return output;
	}

	public static byte[] YV12toYUV420PackedSemiPlanar(final byte[] input,
			final byte[] output, final int width, final int height) {

		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		System.arraycopy(input, 0, output, 0, frameSize); // Y

		for (int i = 0; i < qFrameSize; i++) {
			output[frameSize + i * 2] = input[frameSize + i + qFrameSize];
			output[frameSize + i * 2 + 1] = input[frameSize + i];
		}
		return output;
	}

	byte[] i420bytes = null;

	/*
	 * private byte[] swapYV12toI420(byte[] yv12bytes, int width, int height) {
	 * if (i420bytes == null) i420bytes = new byte[yv12bytes.length]; for (int i
	 * = 0; i < width * height; i++) i420bytes[i] = yv12bytes[i]; for (int i =
	 * width * height; i < width * height + (width / 2 * height / 2); i++)
	 * i420bytes[i] = yv12bytes[i + (width / 2 * height / 2)]; for (int i =
	 * width * height + (width / 2 * height / 2); i < width height + 2 * (width
	 * / 2 * height / 2); i++) i420bytes[i] = yv12bytes[i - (width / 2 * height
	 * / 2)]; return i420bytes; }
	 */

	private void swapNV21toI420(byte[] nv21bytes, byte[] i420bytes, int width,
			int height) {
		final int iSize = width * height;
		System.arraycopy(nv21bytes, 0, i420bytes, 0, iSize);

		for (int iIndex = 0; iIndex < iSize / 2; iIndex += 2) {
			i420bytes[iSize + iIndex / 2 + iSize / 4] = nv21bytes[iSize
					+ iIndex]; // U
			i420bytes[iSize + iIndex / 2] = nv21bytes[iSize + iIndex + 1]; // V
		}
	}

	private void swapNV21toNV12(byte[] nv21bytes, byte[] nv12bytes, int width,
			int height) {
		byte bTmp = 0;
		final int iSize = width * height; 
		
		for (int i = iSize; i < iSize + iSize / 2; i += 2) {
			bTmp = nv21bytes[i + 1];
			nv21bytes[i + 1] = nv21bytes[i];
			nv21bytes[i] = bTmp;
		}
		System.arraycopy(nv21bytes, 0, nv12bytes, 0, nv21bytes.length);
	}

	/*
	 * public byte[] swapYV12toI420(byte[] yv12bytes, int width, int height) {
	 * byte[] i420bytes = new byte[yv12bytes.length]; for (int i = 0; i < width*
	 * height; i++) i420bytes[i] = yv12bytes[i]; for (int i = width * height;i <
	 * width * height + (width / 2 * height / 2); i++) i420bytes[i] =yv12bytes[i
	 * + (width / 2 * height / 2)]; for (int i = width * height +(width / 2 *
	 * height / 2); i < width height + 2 * (width / 2 * height / 2); i++)
	 * i420bytes[i] = yv12bytes[i - (width / 2 * height / 2)]; return i420bytes;
	 * }
	 */
}
