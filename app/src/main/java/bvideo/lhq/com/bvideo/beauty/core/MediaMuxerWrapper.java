package bvideo.lhq.com.bvideo.beauty.core;

/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaMuxerWrapper.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;


@TargetApi(18)
public class MediaMuxerWrapper {
	protected static final boolean DEBUG = false;
	protected static final String TAG = "MediaMuxerWrapper";

	private static final boolean OPEN_WRITE_FILE = false;

	protected int mEncoderCount, mStatredCount;
	protected boolean mIsStarted;
	protected MediaEncoder mVideoEncoder, mAudioEncoder;

	private FileOutputStream outputStream;

	public MediaMuxerWrapper() throws IOException {
		mEncoderCount = mStatredCount = 0;
		mIsStarted = false;
	}

	public void prepare() throws IOException {
		if (mVideoEncoder != null)
			mVideoEncoder.prepare();
		if (mAudioEncoder != null)
			mAudioEncoder.prepare();
	}

	public void startRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.startRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.startRecording();

		if (OPEN_WRITE_FILE) {
			try {
				outputStream = new FileOutputStream(getOutputMediaFileH264());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public void stopRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.stopRecording();
		mVideoEncoder = null;
		if (mAudioEncoder != null)
			mAudioEncoder.stopRecording();
		mAudioEncoder = null;

		if (OPEN_WRITE_FILE) {
			if (outputStream != null) {
				try {
					outputStream.close();
					outputStream = null;
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	public synchronized boolean isStarted() {
		return mIsStarted;
	}

	// **********************************************************************
	// **********************************************************************
	/**
	 * assign encoder to this calss. this is called from encoder.
	 * 
	 * @param encoder
	 *            instance of MediaVideoEncoder or MediaAudioEncoder
	 */
	/* package */void addEncoder(final MediaEncoder encoder) {
		if (encoder instanceof MediaVideoEncoder) {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException(
						"Video encoder already added.");
			mVideoEncoder = encoder;
		} else
			throw new IllegalArgumentException("unsupported encoder");
		mEncoderCount = (mVideoEncoder != null ? 1 : 0)
				+ (mAudioEncoder != null ? 1 : 0);
	}

	/**
	 * request start recording from encoder
	 * 
	 * @return true when muxer is ready to write
	 */
	/* package */synchronized boolean start() {
		if (DEBUG)
			Log.v(TAG, "start:");
		mStatredCount++;
		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
			// mMediaMuxer.start();
			mIsStarted = true;
			notifyAll();
			if (DEBUG)
				Log.v(TAG, "MediaMuxer started:");
		}
		return mIsStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	 */
	/* package */synchronized boolean stop() {
		if (DEBUG)
			Log.v(TAG, "stop:mStatredCount=" + mStatredCount);
		mStatredCount--;
		if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
			// mMediaMuxer.stop();
			// mMediaMuxer.release();
			mIsStarted = false;
			if (DEBUG)
				Log.v(TAG, "MediaMuxer stopped:");
			return true;
		}
		return false;
	}

	/**
	 * assign encoder to muxer
	 * 
	 * @param format
	 * @return minus value indicate error
	 */
	/* package */synchronized int addTrack(final MediaFormat format) {
		if (mIsStarted)
			throw new IllegalStateException("muxer already started");
		if (DEBUG)
			Log.v("addTrack", "addTrack");
		sendAVCDecoderConfigurationRecord(format);
		return 0;

		/*
		 * final int trackIx = mMediaMuxer.addTrack(format); if (DEBUG)
		 * Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" +
		 * trackIx + ",format=" + format); return trackIx;
		 */
	}

	/**
	 * write encoded data to muxer
	 * 
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/* package */synchronized void writeSampleData(final int trackIndex,
			final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if (mStatredCount > 0) {
			// mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
			sendRealData(byteBuf);
		}
	}

	synchronized void writeSampleData2(byte[] realData) {
		if (mStatredCount > 0) {
			sendRealData2(realData);
		}
	}

	private void sendRealData2(byte[] realData) {
//		if (!StartVideo.getInstance().isResult()) {
//			return;
//		}

		/*
		 * if (StartVideo.getInstance().pushVideoH264(finalBuff,
		 * finalBuff.length)) { // Log.e("tag",
		 * "-------------成功--------------------"); }
		 */

		if (Packager.H264Packager.H264Header != null) {
			if (DEBUG)
				Log.v("writeSampleData", "sendRealData,length:"
						+ realData.length);
			if (firstFrame) {
				// firstFrame = false;
				byte[] outData = new byte[Packager.H264Packager.H264Header.length
						+ realData.length];
				System.arraycopy(Packager.H264Packager.H264Header, 0, outData,
						0, Packager.H264Packager.H264Header.length);
				System.arraycopy(realData, 0, outData,
						Packager.H264Packager.H264Header.length,
						realData.length);

				if (OPEN_WRITE_FILE) {
					try {
						outputStream.write(outData);
					} catch (Exception e) {
					}
				} else {
//					if (StartVideo.getInstance().pushVideoH264(outData,
//							outData.length)) {
//						// Log.e("tag", "-------------成功--------------------");
//					}
				}
			} else {
				if (OPEN_WRITE_FILE) {
					try {
						outputStream.write(realData);
					} catch (Exception e) {
					}
				} else {
//					if (StartVideo.getInstance().pushVideoH264(realData,
//							realData.length)) {
//						// Log.e("tag", "-------------成功--------------------");
//					}
				}
			}
		} else {
			if (DEBUG)
				Log.v("writeSampleData", "h264header is empty");
		}

		/*
		 * if(!firstFrame) { firstFrame = true;
		 * 
		 * if(Packager.H264Packager.H264Header != null) { byte[] outData = new
		 * byte[Packager.H264Packager.H264Header.length + finalBuff.length];
		 * System.arraycopy(Packager.H264Packager.H264Header, 0, outData, 0,
		 * Packager.H264Packager.H264Header.length); System.arraycopy(finalBuff,
		 * 0, outData, Packager.H264Packager.H264Header.length,
		 * finalBuff.length); if
		 * (StartVideo.getInstance().pushVideoH264(outData, finalBuff.length)) {
		 * //Log.e("tag", "-------------成功--------------------"); } } } else {
		 * if (StartVideo.getInstance().pushVideoH264(finalBuff,
		 * finalBuff.length)) { //Log.e("tag",
		 * "-------------成功--------------------"); } }
		 */
		// ((MediaVideoEncoder)mVideoEncoder).offerEncoder(finalBuff);
		/*
		 * if (StartVideo.getInstance().pushVideoH264(finalBuff,
		 * finalBuff.length)) { //Log.e("tag",
		 * "-------------成功--------------------"); }
		 */

		/*
		 * int realDataLength = realData.remaining(); int packetLen =
		 * Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
		 * Packager.FLVPackager.NALU_HEADER_LENGTH + realDataLength; byte[]
		 * finalBuff = new byte[packetLen]; realData.get(finalBuff,
		 * Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
		 * Packager.FLVPackager.NALU_HEADER_LENGTH, realDataLength); int
		 * frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
		 * Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;
		 * Packager.FLVPackager.fillFlvVideoTag(finalBuff, 0, false, frameType
		 * == 5, realDataLength); if
		 * (StartVideo.getInstance().pushVideoH264(finalBuff, finalBuff.length))
		 * { }
		 */
		/*
		 * byte[] finalBuff =
		 * Packager.H264Packager.generateH264Package(realData); if
		 * (StartVideo.getInstance().pushVideoH264(finalBuff, finalBuff.length))
		 * { }
		 */

	}

	private void sendAVCDecoderConfigurationRecord(MediaFormat format) {
		// byte[] finalBuff =
		// Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
		byte[] finalBuff = Packager.H264Packager.getSpsPps(format);

		if (DEBUG)
			Log.v("addTrack", "sendAVCDecoderConfigurationRecord,length:"
					+ finalBuff.length);

//		if (StartVideo.getInstance().pushVideoH264(finalBuff, finalBuff.length)) {
//			// Log.e("tag", "-------------成功--------------------");
//		}

		// ((MediaVideoEncoder)mVideoEncoder).offerEncoder(finalBuff);

		/*
		 * byte[] AVCDecoderConfigurationRecord =
		 * Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
		 * int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
		 * AVCDecoderConfigurationRecord.length; byte[] finalBuff = new
		 * byte[packetLen]; Packager.FLVPackager.fillFlvVideoTag(finalBuff, 0,
		 * true, true, AVCDecoderConfigurationRecord.length);
		 * System.arraycopy(AVCDecoderConfigurationRecord, 0, finalBuff,
		 * Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH,
		 * AVCDecoderConfigurationRecord.length); if
		 * (StartVideo.getInstance().pushVideoH264(finalBuff, finalBuff.length))
		 * { }
		 */
	}

	private boolean firstFrame = true;

	private void sendRealData(ByteBuffer realData) {
//		if (!StartVideo.getInstance().isResult()) {
//			return;
//		}
		int realDataLength = realData.remaining();
		byte[] finalBuff = new byte[realDataLength];
		realData.get(finalBuff, 0, realDataLength);

		/*
		 * if (StartVideo.getInstance().pushVideoH264(finalBuff,
		 * finalBuff.length)) { // Log.e("tag",
		 * "-------------成功--------------------"); }
		 */

		if (Packager.H264Packager.H264Header != null) {
			if (DEBUG)
				Log.v("writeSampleData", "sendRealData,length:"
						+ finalBuff.length);
			if (firstFrame) {
				firstFrame = false;
				byte[] outData = new byte[Packager.H264Packager.H264Header.length
						+ finalBuff.length];
				System.arraycopy(Packager.H264Packager.H264Header, 0, outData,
						0, Packager.H264Packager.H264Header.length);
				System.arraycopy(finalBuff, 0, outData,
						Packager.H264Packager.H264Header.length,
						finalBuff.length);

				if (OPEN_WRITE_FILE) {
					try {
						outputStream.write(outData);
					} catch (Exception e) {
					}
				} else {
//					if (StartVideo.getInstance().pushVideoH264(outData,
//							outData.length)) {
//						// Log.e("tag", "-------------成功--------------------");
//					}
				}
			} else {
				if (OPEN_WRITE_FILE) {
					try {
						outputStream.write(finalBuff);
					} catch (Exception e) {
					}
				} else {
//					if (StartVideo.getInstance().pushVideoH264(finalBuff,
//							finalBuff.length)) {
//						// Log.e("tag", "-------------成功--------------------");
//					}
				}
			}
		} else {
			if (DEBUG)
				Log.v("writeSampleData", "h264header is empty");
		}

		/*
		 * if(!firstFrame) { firstFrame = true;
		 * 
		 * if(Packager.H264Packager.H264Header != null) { byte[] outData = new
		 * byte[Packager.H264Packager.H264Header.length + finalBuff.length];
		 * System.arraycopy(Packager.H264Packager.H264Header, 0, outData, 0,
		 * Packager.H264Packager.H264Header.length); System.arraycopy(finalBuff,
		 * 0, outData, Packager.H264Packager.H264Header.length,
		 * finalBuff.length); if
		 * (StartVideo.getInstance().pushVideoH264(outData, finalBuff.length)) {
		 * //Log.e("tag", "-------------成功--------------------"); } } } else {
		 * if (StartVideo.getInstance().pushVideoH264(finalBuff,
		 * finalBuff.length)) { //Log.e("tag",
		 * "-------------成功--------------------"); } }
		 */
		// ((MediaVideoEncoder)mVideoEncoder).offerEncoder(finalBuff);
		/*
		 * if (StartVideo.getInstance().pushVideoH264(finalBuff,
		 * finalBuff.length)) { //Log.e("tag",
		 * "-------------成功--------------------"); }
		 */

		/*
		 * int realDataLength = realData.remaining(); int packetLen =
		 * Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
		 * Packager.FLVPackager.NALU_HEADER_LENGTH + realDataLength; byte[]
		 * finalBuff = new byte[packetLen]; realData.get(finalBuff,
		 * Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
		 * Packager.FLVPackager.NALU_HEADER_LENGTH, realDataLength); int
		 * frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
		 * Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;
		 * Packager.FLVPackager.fillFlvVideoTag(finalBuff, 0, false, frameType
		 * == 5, realDataLength); if
		 * (StartVideo.getInstance().pushVideoH264(finalBuff, finalBuff.length))
		 * { }
		 */
		/*
		 * byte[] finalBuff =
		 * Packager.H264Packager.generateH264Package(realData); if
		 * (StartVideo.getInstance().pushVideoH264(finalBuff, finalBuff.length))
		 * { }
		 */

	}
	private static File getOutputMediaFileH264() {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"BVideoAPP");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("BVideoAPP", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "IMG_" + timeStamp + ".h264");

		return mediaFile;
	}

	// TODO
	private void sendAVCDecoderConfigurationRecord2(MediaFormat format) {
		byte[] AVCDecoderConfigurationRecord = Packager.H264Packager
				.generateAVCDecoderConfigurationRecord(format);
		int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH
				+ AVCDecoderConfigurationRecord.length;
		byte[] finalBuff = new byte[packetLen];
		Packager.FLVPackager.fillFlvVideoTag(finalBuff, 0, true, true,
				AVCDecoderConfigurationRecord.length);
		System.arraycopy(AVCDecoderConfigurationRecord, 0, finalBuff,
				Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH,
				AVCDecoderConfigurationRecord.length);

//		if (StartVideo.getInstance().isResult()) {
//			if (StartVideo.getInstance().pushVideoH264(finalBuff,
//					finalBuff.length)) {
//				// Log.e("tag", "-------------成功--------------------");
//			}
//		}
	}

	private void sendRealData2(ByteBuffer realData) {
//		if (!StartVideo.getInstance().isResult()) {
//			return;
//		}
		int realDataLength = realData.remaining();
		int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH
				+ Packager.FLVPackager.NALU_HEADER_LENGTH + realDataLength;
		byte[] finalBuff = new byte[packetLen];
		realData.get(finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH
				+ Packager.FLVPackager.NALU_HEADER_LENGTH, realDataLength);
		int frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH
				+ Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;
		Packager.FLVPackager.fillFlvVideoTag(finalBuff, 0, false,
				frameType == 5, realDataLength);

//		if (StartVideo.getInstance().isResult()) {
//			if (StartVideo.getInstance().pushVideoH264(finalBuff,
//					finalBuff.length)) {
//				// Log.e("tag", "-------------成功--------------------");
//			}
//		}
	}

	synchronized void removeFailEncoder() {
		mEncoderCount--;

		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
			// mMediaMuxer.start();
			mIsStarted = true;
			notifyAll();
			if (DEBUG)
				Log.v(TAG, "MediaMuxer force start");
		}
	}

	// **********************************************************************
	// **********************************************************************
	/**
	 * generate output file
	 * 
	 * @param type
	 *            Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
	 * @param ext
	 *            .mp4(.m4a for audio) or .png
	 * @return return null when this app has no writing permission to external
	 *         storage.
	 */
	/*
	 * public static final File getCaptureFile(final String type, final String
	 * ext) { final File dir = new
	 * File(Environment.getExternalStoragePublicDirectory(type), DIR_NAME);
	 * Log.d(TAG, "path=" + dir.toString()); dir.mkdirs(); if (dir.canWrite()) {
	 * return new File(dir, getDateTimeString() + ext); } return null; }
	 */

	/**
	 * get current date and time as String
	 * 
	 * @return
	 */
	/*
	 * private static final String getDateTimeString() { final GregorianCalendar
	 * now = new GregorianCalendar(); return
	 * mDateTimeFormat.format(now.getTime()); }
	 */

}
