package bvideo.lhq.com.bvideo.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import bvideo.lhq.com.bvideo.beauty.core.MediaType;
import bvideo.lhq.com.bvideo.utils.Logger;


/**
 * 直播间surface处理相机显示
 * 
 * @author Mr
 * @version 二〇一六年五月三十一日 13:36:29
 * 
 * */
@SuppressWarnings("deprecation")
public class CameraInterface implements PreviewCallback {
	private static final String TAG = "CameraInterface";
	private Camera mCamera;

	private static CameraInterface mCameraInterface;
	int cameraCount = 0;
	// 音频
	private AudioRecord mRecord;
	// 音频大小
	private int bufSize = 0;;
	// 播放音频
	private AudioTrack mTrack;
	// 播放大小
	private int playsize = 0;

	// 音频获取源
	private int audioSource = MediaRecorder.AudioSource.MIC;
	// 播放音频源
	private int audioPlay = AudioManager.STREAM_MUSIC;
	// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
	private static int sampleRateInHz = 44100;
	// 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
	private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	// 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

	AvcEncoder avcCodec;
	private int width = 640;
	private int height = 480;
	int framerate = 20;
	int bitrate = 550000;

	// video device.
	private byte[] vbuffer;
	// 保存手机厂商
	private String phonename;
	// 是否软编还是硬编
	private boolean isPushEncode;
	// 判断是前置还是后置
	private int camera_num = 1;
	// 是否播放声音
	private boolean isAudioFlag;
	// 相机是否打开成功
	private boolean isOpen;

	private float previewRate;

	public interface CamOpenOverCallback {
		public void cameraHasOpened();
	}

	public static synchronized CameraInterface getInstance() {
		if (mCameraInterface == null) {
			mCameraInterface = new CameraInterface();
		}
		return mCameraInterface;
	}

	public Camera getCamera() {
		return mCamera;
	}

	public boolean openCamera(MediaType type) {
		phonename = Build.MANUFACTURER;
		if (phonename != null && phonename != null && phonename != "") {
			if (phonename.equals("Xiaomi") || phonename.equals("HUAWEI")
					|| phonename.equals("samsung")) {
				isPushEncode = true;
			} else {
				if (isMediaCodecList()) {
					isPushEncode = false;
				} else {
					isPushEncode = true;
				}

			}
		}
		isPushEncode = true;

		if (mCamera == null) {
			try {
				mCamera = Camera.open(camera_num);
				setDefaultParameters();
				isOpen = true;
			} catch (RuntimeException e) {
				Logger.getLogger().e(e);
				isOpen = false;
			}
		}
		if (type == MediaType.MEDIA_PUSH && isOpen) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					StartAudio();
				}
			}).start();
		}
		return isOpen;
	}

	/**
	 * 停止预览，释放Camera
	 */
	public boolean releaseCamera() {
		if (null != mCamera) {
			if (isOpen) {
				try {
					mCamera.setPreviewCallback(null);
					mCamera.stopPreview();
					mCamera.release();
				} catch (Exception e) {
					Logger.getLogger().e(e);
					return false;
				}

				if (!isPushEncode) {
					avcCodec.close();
				}
			}
			mCamera = null;

		}
		if (isAudioFlag && null != mTrack) {
			mTrack.stop();
			mTrack.release();
			mTrack = null;
		}
		if (null != mRecord) {
			mRecord.stop();
			mRecord.release();
			mRecord = null;
		}

		isRun = false;

		return true;
	}

	public void resumeCamera(MediaType type) {
		openCamera(type);
	}

	public boolean isMediaCodecList() {
		boolean is_exist = false;
		try {
			// 判断4.1版本以上是否有硬解功能
			Class<?> aClass = Class.forName("android.media.MediaCodecList");
			Logger.getLogger().d(aClass.getName());
			is_exist = true;
		} catch (ClassNotFoundException e) {
			Logger.getLogger().e(e);
		}
		return is_exist;
	}

	private void setDefaultParameters() {
		Parameters parameters = mCamera.getParameters();
		// parameters.setPictureFormat(PixelFormat.JPEG);
		parameters.setPreviewFormat(ImageFormat.NV21);

		CamParaUtil.getInstance().printSupportPictureSize(parameters);
		CamParaUtil.getInstance().printSupportPreviewSize(parameters);

		// 设置PreviewSize和PictureSize

		Size previewSize = getLargePreviewSize();
		parameters.setPreviewSize(1280, 720);
		Size pictureSize = getLargePictureSize();
		parameters.setPictureSize(pictureSize.width, pictureSize.height);

		/*
		 * Size pictureSize = CamParaUtil.getInstance() .getPropPictureSize(
		 * parameters.getSupportedPictureSizes(), previewRate, 600);
		 * parameters.setPictureSize(pictureSize.width, pictureSize.height);
		 * Size previewSize = CamParaUtil.getInstance() .getPropPreviewSize(
		 * parameters.getSupportedPreviewSizes(), previewRate, 600);
		 * parameters.setPreviewSize(previewSize.width, previewSize.height);
		 */
		CamParaUtil.getInstance().printSupportFocusMode(parameters);
		List<String> focusModes = parameters.getSupportedFocusModes();
		if (focusModes.contains("continuous-video")) {
			parameters
					.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		}
		width = previewSize.width;
		height = previewSize.height;

		Log.e("startPreview", "setDefaultParameters----Width:" + width);
		Log.e("startPreview", "setDefaultParameters----Height:" + height);
		/*
		 * List<int[]> range = parameters.getSupportedPreviewFpsRange(); int
		 * max_fps = 0; for (int j = 0; j < range.size(); j++) { int[] r =
		 * range.get(j); for (int k = 0; k < r.length; k++) { max_fps =
		 * Math.max(max_fps, r[k]); } } parameters.setPreviewFpsRange(max_fps,
		 * max_fps);
		 */

		// 不开FBO的角度
		// parameters.setRotation(180);
		// mCamera.setDisplayOrientation(180);
		// 开FBO的角度
		/*
		 * parameters.setRotation(270); mCamera.setDisplayOrientation(270);
		 */

		mCamera.setParameters(parameters);
	}

	private Size getLargePreviewSize() {
		if (mCamera != null) {
			List<Size> sizes = mCamera.getParameters()
					.getSupportedPreviewSizes();
			Size temp = sizes.get(0);
			for (int i = 1; i < sizes.size(); i++) {
				if (temp.width < sizes.get(i).width)
					temp = sizes.get(i);
			}
			return temp;
		}
		return null;
	}

	private Size getLargePictureSize() {
		if (mCamera != null) {
			List<Size> sizes = mCamera.getParameters()
					.getSupportedPictureSizes();
			Size temp = sizes.get(0);
			for (int i = 1; i < sizes.size(); i++) {
				float scale = (float) (sizes.get(i).height)
						/ sizes.get(i).width;
				if (temp.width < sizes.get(i).width && scale < 0.6f
						&& scale > 0.5f)
					temp = sizes.get(i);
			}
			return temp;
		}
		return null;
	}

	public void setPreviewRate(float previewRate) {
		this.previewRate = previewRate;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		/*
		 * Log.e("onPreviewFrame", "onPreviewFrame"+data.length);
		 * 
		 * byte[] data1 = new byte[data.length];
		 * 
		 * if (camera_num == 1) { YUV420spRotateNegative90(data1, data, width,
		 * height); } else { data1 = rotateYUV420Degree90(data, width, height);
		 * } if (isPushEncode) { // 软编码 if (StartVideo.getInstance()
		 * .pushVideo(data1, height, width, 5, 100)) { } } else { // 硬编码 if
		 * (StartVideo.getInstance().isResult()) { avcCodec.offerEncoder(data1);
		 * } } addCallbackBuffer();
		 */
	}

	public void addCallbackBuffer() {
		try {
			if (null != vbuffer && mCamera != null) {
				mCamera.addCallbackBuffer(vbuffer);
			}
		} catch (Exception e) {
			Logger.getLogger().e(e);
		}
	}

	/**
	 * 视频顺时针旋转90
	 * */
	private byte[] rotateYUV420Degree90(byte[] data, int imageWidth,
			int imageHeight) {

		/*
		 * int nWidth = 0, nHeight = 0; int wh = 0; int uvHeight = 0; if
		 * (srcWidth != nWidth || srcHeight != nHeight) { nWidth = srcWidth;
		 * nHeight = srcHeight; wh = srcWidth * srcHeight; uvHeight = height /
		 * 2; }
		 * 
		 * // 旋转Y int k = 0; for (int i = 0; i < srcWidth; i++) { int nPos = 0;
		 * for (int j = 0; j < srcHeight; j++) { dst[k] = src[nPos + i]; k++;
		 * nPos += srcWidth; } }
		 * 
		 * for (int i = 0; i < srcWidth; i += 2) { int nPos = wh; for (int j =
		 * 0; j < uvHeight; j++) { dst[k] = src[nPos + i]; dst[k + 1] = src[nPos
		 * + i + 1]; k += 2; nPos += srcWidth; } }
		 */

		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
		int i = 0;
		for (int x = 0; x < imageWidth; x++) {
			for (int y = imageHeight - 1; y >= 0; y--) {
				yuv[i] = data[y * imageWidth + x];
				i++;
			}
		}
		i = imageWidth * imageHeight * 3 / 2 - 1;
		for (int x = imageWidth - 1; x > 0; x = x - 2) {
			for (int y = 0; y < imageHeight / 2; y++) {
				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
				i--;
				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
						+ (x - 1)];
				i--;
			}
		}
		return yuv;

	}

	/**
	 * 视频逆时针旋转90
	 * */
	public void YUV420spRotateNegative90(byte[] dst, byte[] src, int srcWidth,
			int height) {
		int nWidth = 0, nHeight = 0;
		int wh = 0;
		int uvHeight = 0;

		if (srcWidth != nWidth || height != nHeight) {
			nWidth = srcWidth;
			nHeight = height;
			wh = srcWidth * height;
			uvHeight = height / 2;
		}

		// 旋转Y
		int k = 0;
		for (int i = 0; i < srcWidth; i++) {
			int nPos = srcWidth - 1;
			for (int j = 0; j < height; j++) {
				dst[k] = src[nPos - i];
				k++;
				nPos += srcWidth;
			}
		}

		for (int i = 0; i < srcWidth; i += 2) {
			int nPos = wh + srcWidth - 1;
			for (int j = 0; j < uvHeight; j++) {
				dst[k] = src[nPos - i - 1];
				dst[k + 1] = src[nPos - i];
				k += 2;
				nPos += srcWidth;
			}
		}

		return;
	}

	private boolean isRun = false;

	public void audioPlay(boolean isflag) {
		// 播放声音
		isAudioFlag = isflag;
		if (isAudioFlag) {
			playsize = AudioTrack.getMinBufferSize(sampleRateInHz,
					channelConfig, audioFormat);
			mTrack = new AudioTrack(audioPlay, sampleRateInHz, channelConfig,
					audioFormat, playsize, AudioTrack.MODE_STREAM);
			mTrack.play();
		} else {
			mTrack = null;
		}
	}

	// 初始化音频采集
	public void StartAudio() {
		bufSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig,
				audioFormat);

		mRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig,
				audioFormat, bufSize);

		byte[] inByteBuf = new byte[bufSize];

		try {
			// 录音
			mRecord.startRecording();
		} catch (Exception e) {

		}

		isRun = true;
		// new Thread(new AudioRecordThread()).start();

		while (isRun) {
			// 从MIC存储到缓存区
			int readSize = mRecord.read(inByteBuf, 0, bufSize);
			// 播放缓存区的数据
			if (isAudioFlag && mTrack != null && readSize != 0) {
				byte[] playByteBuf = new byte[readSize];
				System.arraycopy(inByteBuf, 0, playByteBuf, 0, readSize);
				mTrack.write(playByteBuf, 0, playByteBuf.length);
			}

			if (readSize <= 0) {
				break;
			}
			//TODO 推流
//			StartVideo.getInstance().pushAudio(inByteBuf, readSize);
		}

	}

	private void writeDateTOFile() {
		// new一个byte数组用来存一些字节数据，大小为缓冲区大小
		byte[] audiodata = new byte[bufSize];
		FileOutputStream fos = null;
		int readsize = 0;
		try {
			File file = new File("/sdcard/love.amr");
			if (file.exists()) {
				file.delete();
			}
			fos = new FileOutputStream(file);// 建立一个可存取字节的文件
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (isRun == true) {
			readsize = mRecord.read(audiodata, 0, bufSize);
			// Log.e("tag", "-------------readSize-------------" + bufSize);
			// StartVideo.getInstance().pushAudio(audiodata, readsize);
			if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
				try {
					fos.write(audiodata);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			fos.close();// 关闭写入流
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class AudioRecordThread implements Runnable {
		@Override
		public void run() {
			writeDateTOFile();// 往文件中写入裸数据
		}
	}

	public Size getPreviewSize() {
		return mCamera.getParameters().getPreviewSize();
	}

	public int getOrientation() {
		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(camera_num, cameraInfo);
		return cameraInfo.orientation;
	}

	public void startPreview(SurfaceTexture surfaceTexture) {
		if (!isPushEncode) {
			try {
				avcCodec = new AvcEncoder(height, width, framerate, bitrate);
			} catch (Exception e) {
				isPushEncode = true;
			}
		}

		vbuffer = new byte[width * height * 3 / 2];
		try {
			mCamera.setPreviewTexture(surfaceTexture);
			mCamera.addCallbackBuffer(vbuffer);
			mCamera.setPreviewCallbackWithBuffer(this);
			mCamera.startPreview();// 开启预览
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CameraInfo getCameraInfo() {
		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(camera_num, cameraInfo);
		return cameraInfo;
	}

	public int getCameraDisplayOrientation(final Activity activity) {
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		int result;
		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(camera_num, cameraInfo);

		if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
			result = (cameraInfo.orientation + degrees) % 360;
		} else { // back-facing
			result = (cameraInfo.orientation - degrees + 360) % 360;
		}
		return result;
	}

	public boolean isFlipHorizontal() {
		return getCameraInfo().facing == CameraInfo.CAMERA_FACING_FRONT ? true
				: false;
	}

	public void onResume(MediaType type) {
		if (CameraInterface.getInstance().getCamera() == null)
			CameraInterface.getInstance().openCamera(type);
	}

	public void onPause() {
		CameraInterface.getInstance().releaseCamera();
	}

	public void onDestroy() {
	}

	public void switchCamera() {
		if (camera_num == 1)
			camera_num = 0;
		else if (camera_num == 0)
			camera_num = 1;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
