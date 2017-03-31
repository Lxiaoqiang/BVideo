package bvideo.lhq.com.bvideo.beauty.filter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera.Size;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Environment;
import android.util.Log;

import bvideo.lhq.com.bvideo.beauty.core.EglCore;
import bvideo.lhq.com.bvideo.beauty.core.MediaAudioEncoder;
import bvideo.lhq.com.bvideo.beauty.core.MediaEncoder;
import bvideo.lhq.com.bvideo.beauty.core.MediaMuxerWrapper;
import bvideo.lhq.com.bvideo.beauty.core.MediaRecordMuxerWrapper;
import bvideo.lhq.com.bvideo.beauty.core.MediaType;
import bvideo.lhq.com.bvideo.beauty.core.MediaVideoEncoder;
import bvideo.lhq.com.bvideo.beauty.core.WindowSurface;
import bvideo.lhq.com.bvideo.beauty.utils.MagicFilterFactory;
import bvideo.lhq.com.bvideo.beauty.utils.MagicFilterParam;
import bvideo.lhq.com.bvideo.beauty.utils.MagicFilterType;
import bvideo.lhq.com.bvideo.beauty.utils.OpenGLUtils;
import bvideo.lhq.com.bvideo.beauty.utils.Rotation;
import bvideo.lhq.com.bvideo.beauty.utils.TextureRotationUtil;
import bvideo.lhq.com.bvideo.camera.CameraInterface;

/**
 * MagicCameraDisplay is used for camera preview
 */
@SuppressWarnings("deprecation")
public class MagicCameraDisplay implements Renderer {
	/**
	 * 所选择的滤镜，类型为MagicBaseGroupFilter
	 * 1.mCameraInputFilter将SurfaceTexture中YUV数据绘制到FrameBuffer
	 * 2.mFilters将FrameBuffer中的纹理绘制到屏幕中
	 */
	protected GPUImageFilter mFilters;
	/**
	 * 用于绘制相机预览数据，当无滤镜及mFilters为Null或者大小为0时，绘制到屏幕中， 否则，绘制到FrameBuffer中纹理
	 */
	private MagicCameraInputFilter mCameraInputFilter;

	/**
	 * Camera预览数据接收层，必须和OpenGL绑定 过程见{@link
	 * OpenGLUtils.getExternalOESTextureID()};
	 */
	private SurfaceTexture mSurfaceTexture;

	/**
	 * 所有预览数据绘制画面
	 */
	protected GLSurfaceView mGLSurfaceView;

	/**
	 * SurfaceTexure纹理id
	 */
	protected int mTextureId = OpenGLUtils.NO_TEXTURE;
	/**
	 * GLSurfaceView的宽高
	 */
	protected int mSurfaceWidth, mSurfaceHeight;
	/**
	 * 图像宽高
	 */
	protected int mImageWidth, mImageHeight;
	/**
	 * 顶点坐标
	 */
	protected FloatBuffer mGLCubeBuffer;
	/**
	 * 纹理坐标
	 */
	protected FloatBuffer mGLTextureBuffer;

	protected Context mContext;

	// private MagicFilterAdjuster mFilterAdjust;

	/**
	 * 是直播还是录播
	 */
	private MediaType mMediaType;

	public MagicCameraDisplay(Context context, GLSurfaceView glSurfaceView,
			MediaType type) {
		this.mMediaType = type;
		mContext = context;
		mGLSurfaceView = glSurfaceView;

		mFilters = MagicFilterFactory.getFilters(MagicFilterType.NONE, context);

		mGLCubeBuffer = ByteBuffer
				.allocateDirect(TextureRotationUtil.CUBE.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

		mGLTextureBuffer = ByteBuffer
				.allocateDirect(
						TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(
				0);

		mGLSurfaceView.setEGLContextClientVersion(2);
		mGLSurfaceView.setRenderer(this);
		mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mCameraInputFilter = new MagicCameraInputFilter();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glDisable(GL10.GL_DITHER);
		GLES20.glClearColor(0, 0, 0, 0);
		GLES20.glEnable(GL10.GL_CULL_FACE);
		GLES20.glEnable(GL10.GL_DEPTH_TEST);
		MagicFilterParam.initMagicFilterParam(gl);
		mCameraInputFilter.init();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		onFilterChanged();
	}

	@SuppressLint("NewApi")
	@Override
	public void onDrawFrame(GL10 gl) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		mSurfaceTexture.updateTexImage();
		float[] mtx = new float[16];
		mSurfaceTexture.getTransformMatrix(mtx);
		mCameraInputFilter.setTextureTransformMatrix(mtx);

		if (mFilters == null) {
			mCameraInputFilter.onDrawFrame(mTextureId, mGLCubeBuffer,
					mGLTextureBuffer);
		} else {
			int textureID = mCameraInputFilter.onDrawToTexture(mTextureId);
			mFilters.onDrawFrame(textureID, mGLCubeBuffer, mGLTextureBuffer);

			if (mIsRecording) {
				// create encoder surface
				if (mCodecInput == null) {
					mEGLCore = new EglCore(EGL14.eglGetCurrentContext(),
							EglCore.FLAG_RECORDABLE);
					mCodecInput = new WindowSurface(mEGLCore,
							mVideoEncoder.getSurface(), false);
				}
				// Draw on encoder surface
				mCodecInput.makeCurrent();
				GLES20.glViewport(0, 0, OUTPLAY_ENCODE_WIDTH,
						OUTPLAY_ENCODE_HEIGHT);

				mFilters.onDrawFrame(textureID, mGLCubeBuffer, mGLTextureBuffer);

				mCodecInput.swapBuffers();
				mVideoEncoder.frameAvailableSoon();

				// 还原视图
				// Make screen surface be current surface
				mEGL.eglMakeCurrent(mEGLDisplay, mEGLScreenSurface,
						mEGLScreenSurface, mEGLContext);
				GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
			}
		}

	}

	private OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {

		@Override
		public void onFrameAvailable(SurfaceTexture surfaceTexture) {
			// TODO Auto-generated method stub
			mGLSurfaceView.requestRender();
		}
	};

	private void setUpCamera() {
		mGLSurfaceView.queueEvent(new Runnable() {

			@Override
			public void run() {
				if (mTextureId == OpenGLUtils.NO_TEXTURE) {
					mTextureId = OpenGLUtils.getExternalOESTextureID();
					mSurfaceTexture = new SurfaceTexture(mTextureId);
					mSurfaceTexture
							.setOnFrameAvailableListener(mOnFrameAvailableListener);
				}
				Size size = CameraInterface.getInstance().getPreviewSize();
				int orientation = CameraInterface.getInstance()
						.getOrientation();
				if (orientation == 90 || orientation == 270) {
					mImageWidth = size.height;
					mImageHeight = size.width;
				} else {
					mImageWidth = size.width;
					mImageHeight = size.height;
				}
				mCameraInputFilter.onOutputSizeChanged(mImageWidth,
						mImageHeight);
				CameraInterface.getInstance().startPreview(mSurfaceTexture,mContext);
			}
		});
	}

	protected void onFilterChanged() {
		initEGLContent();
		if (mFilters != null) {
			mFilters.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
			mFilters.onOutputSizeChanged(mImageWidth, mImageHeight);
		}

		mCameraInputFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
		if (mFilters != null)
			mCameraInputFilter.initCameraFrameBuffer(mImageWidth, mImageHeight);
		else
			mCameraInputFilter.destroyFramebuffers();
	}

	public void onResume() {
		if (CameraInterface.getInstance().getCamera() == null)
			CameraInterface.getInstance().openCamera(mMediaType);
		if (CameraInterface.getInstance().getCamera() != null) {
			boolean flipHorizontal = CameraInterface.getInstance()
					.isFlipHorizontal();
			int orintation=0;// 此处角度的考虑放到 camera.setDisplayOriontation里了，如果此处考虑角度，则需要mCamera.setDisplayOrientation(0)
			adjustPosition(orintation,
					flipHorizontal, !flipHorizontal);
		}
		setUpCamera();
	}

	public void onPause() {
		CameraInterface.getInstance().releaseCamera();
	}

	public void onDestroy() {
		releaseEncodeSurface();
	}

	public void switchCamera() {
		CameraInterface.getInstance().switchCamera();
		CameraInterface.getInstance().releaseCamera();
		onResume();
	}

	protected void onGetBitmapFromGL(Bitmap bitmap) {
	}

	protected void getBitmapFromGL(final Bitmap bitmap, final boolean newTexture) {
		mGLSurfaceView.queueEvent(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				int[] mFrameBuffers = new int[1];
				int[] mFrameBufferTextures = new int[1];
				GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
				GLES20.glGenTextures(1, mFrameBufferTextures, 0);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
						mFrameBufferTextures[0]);
				GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
						width, height, 0, GLES20.GL_RGBA,
						GLES20.GL_UNSIGNED_BYTE, null);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
				GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,
						mFrameBuffers[0]);
				GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
						GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
						mFrameBufferTextures[0], 0);
				GLES20.glViewport(0, 0, width, height);
				mFilters.onOutputSizeChanged(width, height);
				mFilters.onDisplaySizeChanged(mImageWidth, mImageHeight);
				int textureId = OpenGLUtils.NO_TEXTURE;
				if (newTexture)
					textureId = OpenGLUtils.loadTexture(bitmap,
							OpenGLUtils.NO_TEXTURE, true);
				else
					textureId = mTextureId;
				mFilters.onDrawFrame(textureId);
				IntBuffer ib = IntBuffer.allocate(width * height);
				GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA,
						GLES20.GL_UNSIGNED_BYTE, ib);
				Bitmap mBitmap = Bitmap.createBitmap(width, height,
						Bitmap.Config.ARGB_8888);
				mBitmap.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
				if (newTexture)
					GLES20.glDeleteTextures(1, new int[] { textureId }, 0);
				GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
				GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
				GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
				mFilters.destroy();
				mFilters.init();
				mFilters.onOutputSizeChanged(mImageWidth, mImageHeight);
				onGetBitmapFromGL(mBitmap);
			}
		});
	}

	private void adjustPosition(int orientation, boolean flipHorizontal,
			boolean flipVertical) {
		Rotation mRotation = Rotation.fromInt(orientation);
		float[] textureCords = TextureRotationUtil.getRotation(mRotation,
				flipHorizontal, flipVertical);
		mGLTextureBuffer.clear();
		mGLTextureBuffer.put(textureCords).position(0);
	}

	protected void deleteTextures() {
		if (mTextureId != OpenGLUtils.NO_TEXTURE)
			mGLSurfaceView.queueEvent(new Runnable() {

				@Override
				public void run() {
					GLES20.glDeleteTextures(1, new int[] { mTextureId }, 0);
					mTextureId = OpenGLUtils.NO_TEXTURE;
				}
			});
	}

	/**
	 * 设置滤镜
	 * 
	 * @param 参数类型
	 */
	public void setFilter(final int filterType) {
		mGLSurfaceView.queueEvent(new Runnable() {

			@Override
			public void run() {
				if (mFilters != null)
					mFilters.destroy();
				mFilters = null;
				mFilters = MagicFilterFactory.getFilters(filterType, mContext);
				if (mFilters != null)
					mFilters.init();
				onFilterChanged();
				// mFilterAdjust = new MagicFilterAdjuster(mFilters);
			}
		});
		mGLSurfaceView.requestRender();
	}

	/**
	 * 设置滤镜
	 * 
	 * @param 参数类型
	 */
	public void setFilter(final GPUImageFilter filter) {
		mGLSurfaceView.queueEvent(new Runnable() {

			@Override
			public void run() {
				if (mFilters != null)
					mFilters.destroy();
				mFilters = null;
				mFilters = filter;
				if (mFilters != null)
					mFilters.init();
				onFilterChanged();
				// mFilterAdjust = new MagicFilterAdjuster(mFilters);
			}
		});
		mGLSurfaceView.requestRender();
	}

	/*---------录制视频部分------------*/

	public static final int OUTPLAY_ENCODE_WIDTH = 368;
	public static final int OUTPLAY_ENCODE_HEIGHT = 640;
	// public static final int OUTPLAY_ENCODE_WIDTH = 276;
	// public static final int OUTPLAY_ENCODE_HEIGHT = 480;
	// public static final int OUTPLAY_ENCODE_WIDTH = 345;
	// public static final int OUTPLAY_ENCODE_HEIGHT = 600;

	private MediaMuxerWrapper mMuxer;
	private MediaVideoEncoder mVideoEncoder;
	private MediaAudioEncoder mAudioEncoder;

	private WindowSurface mCodecInput;

	private EGLSurface mEGLScreenSurface;
	private EGL10 mEGL;
	private EGLDisplay mEGLDisplay;
	private EGLContext mEGLContext;
	private EglCore mEGLCore;

	private boolean mIsRecording = false;

	private static File getOutputRecordPath() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "bvideo");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
//		Log.e("path",String.format("path==%s",mediaFile.getAbsolutePath()));
		return mediaFile;
	}

	public void startRecording() {
		if (mIsRecording) {
			return;
		}

		try {
			if (mMediaType == MediaType.MEDIA_PUSH) {
				mMuxer = new MediaMuxerWrapper();
			} else if (mMediaType == MediaType.MEDIA_RECORD) {
				mMuxer = new MediaRecordMuxerWrapper(getOutputRecordPath().getAbsolutePath());
				// for audio capturing
                mAudioEncoder = new MediaAudioEncoder(mMuxer, mMediaEncoderListener, mMediaType);
			}

			mVideoEncoder = new MediaVideoEncoder(mMuxer,
					mMediaEncoderListener, OUTPLAY_ENCODE_WIDTH,
					OUTPLAY_ENCODE_HEIGHT, mMediaType);
			mMuxer.prepare();
			mMuxer.startRecording();

			mIsRecording = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stopRecording() {
		if (!mIsRecording) {
			return;
		}
		if (mMediaType == MediaType.MEDIA_RECORD) {
			mMuxer.stopRecording();
		}
		mIsRecording = false;
		releaseEncodeSurface();
	}

	private void releaseEncodeSurface() {
		if (mEGLCore != null) {
			mEGLCore.makeNothingCurrent();
			mEGLCore.release();
			mEGLCore = null;
		}

		if (mCodecInput != null) {
			mCodecInput.release();
			mCodecInput = null;
		}
	}
	
	/**
	 * 获取录播的视频地址
	 * @return
	 */
	public String getMediaOutPath() {
		if(mMuxer != null && mMuxer instanceof MediaRecordMuxerWrapper) {
			return ((MediaRecordMuxerWrapper)mMuxer).getOutputPath();
		}
		return null;
	}

	/**
	 * callback methods from encoder
	 */
	private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
		@Override
		public void onPrepared(final MediaEncoder encoder) {
		}

		@Override
		public void onStopped(final MediaEncoder encoder) {
		}

		@Override
		public void onMuxerStopped() {
		}
	};

	private void initEGLContent() {
		mEGL = (EGL10) EGLContext.getEGL();
		mEGLDisplay = mEGL.eglGetCurrentDisplay();
		mEGLContext = mEGL.eglGetCurrentContext();
		mEGLScreenSurface = mEGL.eglGetCurrentSurface(EGL10.EGL_DRAW);
	}
}
