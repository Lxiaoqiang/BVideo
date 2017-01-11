package bvideo.lhq.com.bvideo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import bvideo.lhq.com.bvideo.beauty.core.MediaType;
import bvideo.lhq.com.bvideo.beauty.filter.GPUImageFilter;
import bvideo.lhq.com.bvideo.beauty.filter.MagicCameraDisplay;
import bvideo.lhq.com.bvideo.beauty.utils.MagicFilterFactory;
import bvideo.lhq.com.bvideo.beauty.utils.MagicFilterType;
import bvideo.lhq.com.bvideo.camera.CameraInterface;
import bvideo.lhq.com.bvideo.utils.AppCacheManager;
import bvideo.lhq.com.bvideo.utils.CustomToast;
import bvideo.lhq.com.bvideo.utils.DisplayUtil;
import bvideo.lhq.com.bvideo.view.TimerRecordView;

public class MainActivity extends Activity implements TimerRecordView.OnRecordEndListener {

    private GLSurfaceView glSurfaceView;

    private TimerRecordView timerRecordView;

    private MagicCameraDisplay mMagicCameraDisplay;

    private MediaRecorder mRecorders = new MediaRecorder();

    private boolean beauty = true;

    private boolean mIsRecording = false;

    public final static String RECORD_MOVIE_PATH = "movie_path";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mRecorders.setAudioSource(MediaRecorder.AudioSource.MIC);
        setContentView(R.layout.activity_main);
        initView();
        initSurface();


    }

    private void initView() {
        timerRecordView = (TimerRecordView) findViewById(R.id.timer_record_view);
        timerRecordView.setOnTimerEndListener(this);
    }

    /**
     * 初始化相机预览
     */
    private void initSurface() {
        if (CameraInterface.getInstance().openCamera(MediaType.MEDIA_RECORD)) {
        } else {
            CustomToast.showToast(getApplicationContext(),"您的相机不可用,请检查相机是否被禁用",2000);
        }

        glSurfaceView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        mMagicCameraDisplay = new MagicCameraDisplay(this, glSurfaceView, MediaType.MEDIA_RECORD);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                openBeauty();
            }
        }, 1000);

        float previewRate = DisplayUtil.getScreenRate(this); // 默认全屏的比例预览
        CameraInterface.getInstance().setPreviewRate(previewRate);
    }

    protected void openBeauty() {
//        if (beauty) {
//            beauty = false;
//            mMagicCameraDisplay.setFilter(MagicFilterType.NONE);
//        } else {
//            beauty = true;
//            GPUImageFilter mFilter = MagicFilterFactory.getFilters(
//                    MagicFilterType.BEAUTY, this);
//            mMagicCameraDisplay.setFilter(mFilter);
//        }
        GPUImageFilter mFilter = MagicFilterFactory.getFilters(
                MagicFilterType.BEAUTY, this);
        mMagicCameraDisplay.setFilter(mFilter);
    }
    private void startRecording() {
        if(!mIsRecording) {
            mIsRecording = true;
            mMagicCameraDisplay.startRecording();
        }
    }

    private void stopRecording() {
        if(mIsRecording) {
            mIsRecording = false;
            mMagicCameraDisplay.stopRecording();
        }
    }
    @Override
    public void onStartRecord() {
        startRecording();
    }

    @Override
    public void onEndRecord(int seconds) {
        stopRecording();
        if(seconds < 8) {
            CustomToast.showToast(getApplicationContext(),"录制时间必须大于8s",2000);
            if (mMagicCameraDisplay != null) {
                String path = mMagicCameraDisplay.getMediaOutPath();
                AppCacheManager.deleteFileFromPath(path);
            }
        } else {
            //TODO 跳转至视频预览播放界面
            finish();
            Intent intent = new Intent(this, RecordMovieUploadActivity.class);
            intent.putExtra(RECORD_MOVIE_PATH, mMagicCameraDisplay.getMediaOutPath());
            startActivity(intent);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mIsRecording) {
            mMagicCameraDisplay.startRecording();
        }
        if (mMagicCameraDisplay != null) {
            mMagicCameraDisplay.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraInterface.getInstance().onPause();
        if (mIsRecording) {
            mMagicCameraDisplay.stopRecording();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        CameraInterface.getInstance().onPause();
        if (mIsRecording) {
            mMagicCameraDisplay.stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onCloseCamera();
    }

    /**
     * 关闭摄像头，同时释放美颜
     */
    private void onCloseCamera() {
        CameraInterface.getInstance().onDestroy();
        if (mMagicCameraDisplay != null)
            mMagicCameraDisplay.onDestroy();
    }
}
