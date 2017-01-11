package bvideo.lhq.com.bvideo;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import bvideo.lhq.com.bvideo.utils.AppCacheManager;
import bvideo.lhq.com.bvideo.utils.CustomToast;
import bvideo.lhq.com.bvideo.video.interfaces.Ijkplayer;

/**
 * 录制小视频---播放上传
 * 
 * @author sym
 * 
 * */
public class RecordMovieUploadActivity extends Activity implements OnClickListener {

	/**
	 * 点击预览播放视频
	 */
	private LinearLayout tvPreview;
	/**
	 * 重新录制
	 */
	private ImageView ivReRecord;
	/**
	 * 保存
	 */
	private ImageView ivSave;
	/**
	 * 用来显示第一帧图片
	 */
	private ImageView mFirstFrameImg;
	private Bitmap mFirstBitmap;
	private SeekBar mSeekBar;
	
	/**
	 * 视频本地地址
	 */
	private String moviePath;
	/**
	 * 视频预览
	 */
	private TextureView mTextureView;
	/**
	 * 分钟，秒
	 */
	private TextView playMinText, playSecText, allMinText, allSecText;
	
	private Ijkplayer player;
	private int videoWidth = 368;
	private int videoHeight = 640;
	private Surface mSurface;
	private PlayProgressTimer mPlayProgressTimer;
	private long video_length;
	
	/**
	 * 播放标识
	 */
	private boolean timer_start;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recordmovie_upload_layout);
		initView();
		initParams();
		initPlayer();
	}

	private void initView() {
		mFirstFrameImg = (ImageView) findViewById(R.id.iv_record_movie_firstframe);
		tvPreview = (LinearLayout) findViewById(R.id.tv_record_movie_preview);
		ivReRecord = (ImageView) findViewById(R.id.iv_record_movie_re_record);
		ivSave = (ImageView) findViewById(R.id.iv_record_movie_save_movie);
		mSeekBar = (SeekBar) findViewById(R.id.sb_movie_record_seek);
		
		tvPreview.setOnClickListener(this);
		ivReRecord.setOnClickListener(this);
		ivSave.setOnClickListener(this);
		
		playMinText = (TextView) findViewById(R.id.iv_record_play_mintute);
		playSecText = (TextView) findViewById(R.id.iv_record_play_second);
		allMinText = (TextView) findViewById(R.id.iv_record_all_mintute);
		allSecText = (TextView) findViewById(R.id.iv_record_all_second);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			// 开始拖动时触发
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			/**
			 * 是否为用户自己触发
			 */
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					long time = video_length * progress / 100;
					setMintuteSecond(playMinText, playSecText, time);
					player.seekTo((int) (time * 1000));
				}
			}
		});
	}
	
	/**
	 * 获取视频路径，初始化第一帧图像
	 */
	private void initParams() {
		Intent intent = getIntent();
		moviePath = intent.getStringExtra(MainActivity.RECORD_MOVIE_PATH);
		if(!TextUtils.isEmpty(moviePath)) {
			File file = new File(moviePath);
			if(file.exists()) {
				MediaMetadataRetriever media = new MediaMetadataRetriever();
				media.setDataSource(moviePath);
				mFirstBitmap = media.getFrameAtTime();
				mFirstFrameImg.setImageBitmap(mFirstBitmap);
			} else {
				CustomToast.showToast(this,"获取视频出错",2000);
			}
		}
	}
	
	private void initPlayer() {
		player = new Ijkplayer(onCallback);
		mTextureView = (TextureView) findViewById(R.id.iv_record_movie_surface);
		mTextureView.setSurfaceTextureListener(textureListener);
		
		try {
			player.reset();
			player.setDataSource(moviePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mPlayProgressTimer = new PlayProgressTimer();
		video_length = player.getDuration();
	}
	
	/**
	 * 播放
	 */
	private void play() {
		if(!player.isPlaying()) {
			mSeekBar.setProgress(0);
			mPlayProgressTimer.postProgressMsg();
			player.prepareAsync();
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tv_record_movie_preview: //预览
			play();
			break;
		case R.id.iv_record_movie_re_record:// 重录
			finish();
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			break;
		case R.id.iv_record_movie_save_movie:// 保存
			break;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (player != null) {
			player.release();
		}
		
		if (mFirstBitmap != null && !mFirstBitmap.isRecycled()) {
			mFirstBitmap.recycle();
		}
		// 退出时，删除临时文件
		AppCacheManager.deleteFileFromPath(moviePath);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		drawPlayerView();
		super.onConfigurationChanged(newConfig);

	}

	private void drawPlayerView() {
		RelativeLayout.LayoutParams params = null;
		double proportion = (double) videoWidth / videoHeight;
		int width = 0;
		int height = 0;

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		int widthSpecSize = dm.widthPixels;
		int heightSpecSize = dm.heightPixels;

		boolean shouldBeWider = widthSpecSize / heightSpecSize > proportion;
		if (shouldBeWider) {
			// too wide, fix width
			width = widthSpecSize;
			height = (int) (width / proportion);
			if (height < heightSpecSize) {
				height = heightSpecSize;
				width = (int) (height * proportion);
			}
		} else {
			// too high, fix height
			height = heightSpecSize;
			width = (int) (height * proportion);
			if (width < widthSpecSize) {
				width = widthSpecSize;
				height = (int) (width / proportion);
			}
		}

		params = new RelativeLayout.LayoutParams(width, height);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mTextureView.setLayoutParams(params);
	}
	
	private Ijkplayer.OnPlayerStateCallback onCallback = new Ijkplayer.OnPlayerStateCallback() {

		@Override
		public int onCallback(int state, Object... msg) {
			switch (state) {
			case Ijkplayer.PLAY_STATE_FIST_FRAME:
				videoWidth = (Integer) msg[0];
				videoHeight = (Integer) msg[1];
				drawPlayerView();
				break;
			// case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
			// loading();
			// break;
			// case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
			// dismissProgerssDialog();
			// break;
			default:
				break;
			}
			return 0;
		}
	};
	
	private SurfaceTextureListener textureListener = new SurfaceTextureListener() {
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface,
				int width, int height) {
			mSurface = new Surface(surface);
			if (player != null) {
				player.setSurface(mSurface);
			}
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
				int width, int height) {
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
			return false;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {

		}
	};
	
	private class PlayProgressTimer {
		private Handler handler = new Handler();
		private Runnable progress_runnable = new Runnable() {
			@Override
			public void run() {
				int time = (int) (player.getCurrentPosition() / 1000);
				setMintuteSecond(playMinText, playSecText, time);
				if (video_length > 0) {
					int progress = (int) (time * 100 / video_length);
					if (progress >= 100) {
						progress = 100;
						mSeekBar.setProgress(progress);
						setMintuteSecond(playMinText, playSecText, video_length);
						player.stop();
						removeMsg();
						return;
					}
					mSeekBar.setProgress(progress);
					handler.postDelayed(progress_runnable, 500);
				} else {
					// Utils.ShowTips(RecordVideoActivity.this, "获取视频失败！");
					// removeMsg();
					video_length = player.getDuration() / 1000;
					if (video_length > 0)
						setMintuteSecond(allMinText, allSecText, video_length);
					handler.postDelayed(progress_runnable, 500);
				}
			}
		};

		public void postProgressMsg() {
			if (!timer_start) {
				timer_start = true;
				// 显示播放view，隐藏预览图片
				tvPreview.setVisibility(View.INVISIBLE);
				mFirstFrameImg.setVisibility(View.INVISIBLE);
				// 防止textureview过渡时出现没有画面的情况
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mTextureView.setVisibility(View.VISIBLE);
					}
				}, 200);
				handler.post(progress_runnable);
			}
		}

		public void removeMsg() {
			if (timer_start) {
				timer_start = false;
				// 隐藏播放view，显示预览图片
				mSeekBar.setProgress(0);
				mTextureView.setVisibility(View.INVISIBLE);
				tvPreview.setVisibility(View.VISIBLE);
				mFirstFrameImg.setVisibility(View.VISIBLE);
				handler.removeCallbacks(progress_runnable);
			}
		}
	}
	
	/**
	 * 统一设置时间格式
	 * @param m
	 * @param s
	 * @param time
	 */
	private void setMintuteSecond(TextView m, TextView s, long time) {
		String[] video_time = secToTime(time);
		m.setText(video_time[0]);
		s.setText(video_time[1]);
	}
	/**
	 * 时间转换为00:00格式
	 *
	 * @param time
	 * @return
	 */
	public static String[] secToTime(long time) {
		String timeStr = null;
		int minute = 0;
		int second = 0;
		String[] video_time = new String[2];
		if (time <= 0) {
			video_time[0] = "00";
			video_time[1] = "00";
		} else if (time < 60) {
			video_time[0] = "00";
			second = (int) (time % 60);
			timeStr = unitFormat(second);
			video_time[1] = timeStr;
		} else {
			minute = (int) (time / 60);
			second = (int) (time - minute * 60);
			video_time[0] = unitFormat(minute);
			video_time[1] = unitFormat(second);
		}
		return video_time;
	}
	public static String unitFormat(int i) {
		String retStr = null;
		if (i >= 0 && i < 10)
			retStr = "0" + Integer.toString(i);
		else
			retStr = "" + i;
		return retStr;
	}
}
