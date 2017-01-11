package bvideo.lhq.com.bvideo.beauty.core;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaRecordMuxerWrapper extends MediaMuxerWrapper {
	protected static final String TAG = "MediaRecordMuxerWrapper";
	
	private String mOutputPath;
	private final MediaMuxer mMediaMuxer; // API >= 18

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MediaRecordMuxerWrapper(String outputPath) throws IOException {
		super();
		mOutputPath = outputPath;
		mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
	}

	public String getOutputPath() {
        return mOutputPath;
    }
	
	@Override
	public void prepare() throws IOException {
		if (mVideoEncoder != null)
            mVideoEncoder.prepare();
        if (mAudioEncoder != null)
            mAudioEncoder.prepare();
	}
	
	@Override
	public void startRecording() {
		if (mVideoEncoder != null)
            mVideoEncoder.startRecording();
        if (mAudioEncoder != null)
            mAudioEncoder.startRecording();
	}
	
	@Override
	public void stopRecording() {
		if (mVideoEncoder != null)
            mVideoEncoder.stopRecording();
        mVideoEncoder = null;
        if (mAudioEncoder != null)
            mAudioEncoder.stopRecording();
        mAudioEncoder = null;
	}
	
	public synchronized boolean isStarted() {
        return mIsStarted;
    }
	
	/**
     * assign encoder to this calss. this is called from encoder.
     * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder
     */
	@Override
    void addEncoder(final MediaEncoder encoder) {
        if (encoder instanceof MediaVideoEncoder) {
        	if (DEBUG) Log.v(TAG,  "addEncoder:MediaVideoEncoder");
            if (mVideoEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaAudioEncoder) {
        	if (DEBUG) Log.v(TAG,  "addEncoder:MediaAudioEncoder");
            if (mAudioEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mAudioEncoder = encoder;
        } else
            throw new IllegalArgumentException("unsupported encoder");
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    /**
     * request start recording from encoder
     * @return true when muxer is ready to write
     */
	@Override
	synchronized boolean start() {
        if (DEBUG) Log.v(TAG,  "start:");
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
            if (DEBUG) Log.v(TAG,  "MediaMuxer started:");
        }
        return mIsStarted;
    }

    /**
     * request stop recording from encoder when encoder received EOS
    */
    @Override
	synchronized boolean stop() {
        if (DEBUG) Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
        mStatredCount--;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsStarted = false;
            if (DEBUG) Log.v(TAG,  "MediaMuxer stopped:");
            return true;
        }
        return false;
    }

    /**
     * assign encoder to muxer
     * @param format
     * @return minus value indicate error
     */
	@Override
	synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted)
            throw new IllegalStateException("muxer already started");
        final int trackIx = mMediaMuxer.addTrack(format);
        if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
        return trackIx;
    }

    /**
     * write encoded data to muxer
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
	@Override
	synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
    	if (DEBUG) Log.i(TAG, "writeSampleData:trackNum=" + mEncoderCount + ",trackIx=" + trackIndex);
        if (mStatredCount > 0)
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
    }
	
	@Override
    synchronized void removeFailEncoder() {
        mEncoderCount--;

        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
            if (DEBUG) Log.v(TAG,  "MediaMuxer force start");
        }
    }

//**********************************************************************
//**********************************************************************
    /**
     * generate output file
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    /*public static final File getCaptureFile(final String type, final String ext) {
        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), DIR_NAME);
        Log.d(TAG, "path=" + dir.toString());
        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, getDateTimeString() + ext);
        }
        return null;
    }*/

    /**
     * get current date and time as String
     * @return
     */
    /*private static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }*/
}
