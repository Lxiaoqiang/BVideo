package bvideo.lhq.com.bvideo.video.interfaces;

import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


public class Ijkplayer implements IMediaPlayer.OnPreparedListener,IMediaPlayer.OnCompletionListener,IMediaPlayer.OnBufferingUpdateListener,IMediaPlayer.OnSeekCompleteListener,IMediaPlayer.OnErrorListener,IMediaPlayer.OnInfoListener,IMediaPlayer.OnVideoSizeChangedListener {
	private IjkMediaPlayer player;
	private final static String TAG = "IJK_PLAYER";
	
	private final static int INIT = 0;
	private final static int ERROR = -1;
	private final static int STOP = 1;
	private final static int PREPARE = 2;
	private final static int START = 3;
	private final static int PAUSE = 4;
	private final static int COMPLETION = 5;
	
	public final static int PLAY_STATE_FIST_FRAME = 0;
	
	private int state = 0;
	private OnPlayerStateCallback onCallback;
	
	public Ijkplayer(OnPlayerStateCallback callback){
		player = new IjkMediaPlayer();
		this.onCallback = callback;
		player.reset();
		state = 0;
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        
      	player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.setOnBufferingUpdateListener(this);
		player.setOnSeekCompleteListener(this);
		player.setOnErrorListener(this);
		player.setOnInfoListener(this);
		player.setOnVideoSizeChangedListener(this);
		player.setVolume(1.0f, 1.0f);
		player.setLooping(true);
		
	}
	
	public void setDisplay(SurfaceHolder sh) {
		player.setDisplay(sh);
	}
	public void setSurface(Surface surface){
		player.setSurface(surface);
	}
	private String url = null;
	
	public void setUrl(String url) throws Exception {
		this.url = url;
		player.reset();
		player.setDataSource(url);
		player.prepareAsync();
	}
	
	public void setDataSource(String url) throws Exception{
		player.setDataSource(url);
	}
	
	public void prepareAsync(){
		state = PREPARE;
		player.prepareAsync();
	}
	
	public void stop() {
		state = STOP;
		player.stop();
	}

	public void pause() {
		state = PAUSE;
		player.pause();
	}

	public void start(){
		state = START;
		player.start();
	}


	public boolean isPlaying() {
		return player.isPlaying();
	}

	public void seekTo(long msec)  {
		if(state > STOP && player != null){
			player.seekTo(msec);
		}else {
			Log.d(TAG, "不能进行SEEK操作，当前状态是:"+state+";player对象是不是为空:"+(player!= null));
		}
	}

	public long getCurrentPosition() {
		return player.getCurrentPosition();
	}

	public long getDuration() {
		return player.getDuration();
	}

	public void release() {
		player.release();
	}

	public void reset() {
		player.reset();
	}


	@Override
	public boolean onInfo(IMediaPlayer mp, int what, int extra) {
		switch (what) {
		case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
			onCallback.onCallback(PLAY_STATE_FIST_FRAME, mp.getVideoWidth(),mp.getVideoHeight());
			break;
//		case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
//			onCallback.onCallback(IMediaPlayer.MEDIA_INFO_BUFFERING_START);
//			break;
//		case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
//			onCallback.onCallback(IMediaPlayer.MEDIA_INFO_BUFFERING_END);
//			break;
		default:
			break;
		}
		return false;
	}


	@Override
	public boolean onError(IMediaPlayer mp, int what, int extra) {
		
		return false;
	}

	private Handler handler = new Handler();

	@Override
	public void onSeekComplete(IMediaPlayer mp) {
		
	}


	@Override
	public void onBufferingUpdate(IMediaPlayer mp, int percent) {
		
	}


	@Override
	public void onCompletion(IMediaPlayer mp) {
		
	}


	@Override
	public void onPrepared(IMediaPlayer mp) {
		start();
	}

	@Override
	public void onVideoSizeChanged(IMediaPlayer mp, int width, int height,int sar_num, int sar_den) {
		
	}
	
	
	


	public interface OnPlayerStateCallback{
		public int onCallback(int state, Object... msg);
	}
}
