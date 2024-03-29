package com.hanbang.videoplay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Nathen on 2016年8月31日15:58:47.
 */
public abstract class JCVideoPlayer extends FrameLayout implements JCMediaPlayerListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener, TextureView.SurfaceTextureListener {

    public static final String TAG = "JieCaoVideoPlayer";

    public static final int FULLSCREEN_ID = 33797;
    public static final int TINY_ID = 33798;
    public static final int THRESHOLD = 80;
    public static final int FULL_SCREEN_NORMAL_DELAY = 500;
    public static final int AnimationDuration = 300;

    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;
    public static boolean WIFI_TIP_DIALOG_SHOWED = false;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    public static final int SCREEN_LAYOUT_LIST = 0;
    public static final int SCREEN_WINDOW_FULLSCREEN = 1;
    public static final int SCREEN_WINDOW_TINY = 2;
    public static final int SCREEN_LAYOUT_DETAIL = 3;

    public static final int CURRENT_STATE_NORMAL = 0;
    public static final int CURRENT_STATE_PREPAREING = 1;
    public static final int CURRENT_STATE_PLAYING = 2;
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    public static final int CURRENT_STATE_PAUSE = 5;
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    public static final int CURRENT_STATE_ERROR = 7;

    public int currentState = -1;
    public int currentScreen = -1;


    public String url = null;
    public Object[] objects = null;
    public boolean looping = false;
    public Map<String, String> mapHeadData = new HashMap<>();
    public int seekToInAdvance = -1;

    public ImageView startButton;
    public SeekBar progressBar;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup topContainer, bottomContainer;
    public Surface surface;

    protected static JCBuriedPoint JC_BURIED_POINT;
    protected static Timer UPDATE_PROGRESS_TIMER;

    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected Handler mHandler;
    protected ProgressTimerTask mProgressTimerTask;
    protected int mBackUpBufferState = -1;

    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected int mDownPosition;
    protected int mGestureDownVolume;
    protected int mSeekTimePosition;

    public JCVideoPlayer(Context context) {
        super(context);
        init(context);
    }

    public JCVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AnimatorSet animatorHint = new AnimatorSet();
    public AnimatorSet animatorShow = new AnimatorSet();

    public void init(Context context) {
        animatorHint.setDuration(300);
        animatorShow.setDuration(300);
        animatorHint.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                topContainer.setVisibility(GONE);
                bottomContainer.setVisibility(GONE);

            }
        });
        View.inflate(context, getLayoutId(), this);
        startButton = (ImageView) findViewById(R.id.start);
        fullscreenButton = (ImageView) findViewById(R.id.fullscreen);
        progressBar = (SeekBar) findViewById(R.id.progress);
        currentTimeTextView = (TextView) findViewById(R.id.current);
        totalTimeTextView = (TextView) findViewById(R.id.total);
        bottomContainer = (ViewGroup) findViewById(R.id.layout_bottom);
        textureViewContainer = (RelativeLayout) findViewById(R.id.surface_container);
        topContainer = (ViewGroup) findViewById(R.id.layout_top);

        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);

        textureViewContainer.setOnTouchListener(this);
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler();
    }

    public boolean setUp(String url, int screen, Object... objects) {
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
            return false;
        this.currentState = CURRENT_STATE_NORMAL;
        this.url = url;
        this.objects = objects;
        this.currentScreen = screen;
        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
        return true;
    }

    public boolean setUp(String url, int screen, Map<String, String> mapHeadData, Object... objects) {
        if (setUp(url, screen, objects)) {
            this.mapHeadData.clear();
            this.mapHeadData.putAll(mapHeadData);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(getContext(), "播放地址无效", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) {
                if (!url.startsWith("file") && !JCUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                prepareVideo();
                onEvent(currentState != CURRENT_STATE_ERROR ? JCBuriedPoint.ON_CLICK_START_ICON : JCBuriedPoint.ON_CLICK_START_ERROR);
            } else if (currentState == CURRENT_STATE_PLAYING) {
                onEvent(JCBuriedPoint.ON_CLICK_PAUSE);
                JCMediaManager.instance().mediaPlayer.pause();
                setUiWitStateAndScreen(CURRENT_STATE_PAUSE);
            } else if (currentState == CURRENT_STATE_PAUSE) {
                onEvent(JCBuriedPoint.ON_CLICK_RESUME);
                JCMediaManager.instance().mediaPlayer.start();
                setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onEvent(JCBuriedPoint.ON_CLICK_START_AUTO_COMPLETE);
                prepareVideo();
            }
        } else if (i == R.id.fullscreen) {
            if (currentState == CURRENT_STATE_AUTO_COMPLETE) return;
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                //quit fullscreen
                backPress();
            } else {
                onEvent(JCBuriedPoint.ON_ENTER_FULLSCREEN);
                startWindowFullscreen();
            }
        } else if (i == R.id.surface_container && currentState == CURRENT_STATE_ERROR) {
            prepareVideo();
        }
    }

    public void prepareVideo() {
        if (JCVideoPlayerManager.listener() != null) {
            JCVideoPlayerManager.listener().onCompletion();
        }
        JCVideoPlayerManager.setListener(this);
        addTextureView();
        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        JCUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        JCMediaManager.instance().prepare(url, mapHeadData, looping);
        setUiWitStateAndScreen(CURRENT_STATE_PREPAREING);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchingProgressBar = true;

                    mDownX = x;
                    mDownY = y;
                    mChangeVolume = false;
                    mChangePosition = false;
                    /////////////////////
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                        if (!mChangePosition && !mChangeVolume) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                cancelProgressTimer();
                                if (absDeltaX >= THRESHOLD) {
                                    mChangePosition = true;
                                    mDownPosition = getCurrentPositionWhenPlaying();
                                } else {
                                    mChangeVolume = true;
                                    mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        int totalTimeDuration = getDuration();
                        mSeekTimePosition = (int) (mDownPosition + deltaX * totalTimeDuration / mScreenWidth);
                        if (mSeekTimePosition > totalTimeDuration)
                            mSeekTimePosition = totalTimeDuration;
                        String seekTime = JCUtils.stringForTime(mSeekTimePosition);
                        String totalTime = JCUtils.stringForTime(totalTimeDuration);

                        showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY;
                        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                        int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);

                        showVolumDialog(-deltaY, volumePercent);
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumDialog();
                    if (mChangePosition) {
                        onEvent(JCBuriedPoint.ON_TOUCH_SCREEN_SEEK_POSITION);
                        JCMediaManager.instance().mediaPlayer.seekTo(mSeekTimePosition);
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        progressBar.setProgress(progress);
                    }
                    if (mChangeVolume) {
                        onEvent(JCBuriedPoint.ON_TOUCH_SCREEN_SEEK_VOLUME);
                    }
                    startProgressTimer();
                    break;
            }
        }
        return false;
    }

    public void addTextureView() {
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }
        JCMediaManager.textureView = null;
        JCMediaManager.textureView = new JCResizeTextureView(getContext());
        JCMediaManager.textureView.setSurfaceTextureListener(this);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        textureViewContainer.addView(JCMediaManager.textureView, layoutParams);
    }

    public void setUiWitStateAndScreen(int state) {
        currentState = state;
        switch (currentState) {
            case CURRENT_STATE_NORMAL:
                if (isCurrentMediaListener()) {
                    cancelProgressTimer();
                    JCMediaManager.instance().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_PREPAREING:
                resetProgressAndTime();
                break;
            case CURRENT_STATE_PLAYING:
                startProgressTimer();
                break;
            case CURRENT_STATE_PAUSE:
                startProgressTimer();
                break;
            case CURRENT_STATE_ERROR:
                if (isCurrentMediaListener()) {
                    JCMediaManager.instance().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                cancelProgressTimer();
                progressBar.setProgress(100);
                currentTimeTextView.setText(totalTimeTextView.getText());
                break;
        }
    }

    public void startProgressTimer() {
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    @Override
    public void onPrepared() {

        if (currentState != CURRENT_STATE_PREPAREING) return;
        JCMediaManager.instance().mediaPlayer.start();
        if (seekToInAdvance != -1) {
            JCMediaManager.instance().mediaPlayer.seekTo(seekToInAdvance);
            seekToInAdvance = -1;
        }
        startProgressTimer();
        setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
    }

    public void clearFullscreenLayout() {
        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(FULLSCREEN_ID);
        View oldT = vp.findViewById(TINY_ID);
        if (oldF != null) {
            vp.removeView(oldF);
        }
        if (oldT != null) {
            vp.removeView(oldT);
        }
        showSupportActionBar(getContext());
    }

    @Override
    public void onAutoCompletion() {
        onEvent(JCBuriedPoint.ON_AUTO_COMPLETE);
        if (JCVideoPlayerManager.listener() != null) {
            JCVideoPlayerManager.listener().onCompletion();
            JCVideoPlayerManager.setListener(null);
        }
        if (JCVideoPlayerManager.lastListener() != null) {
            JCVideoPlayerManager.lastListener().onCompletion();
            JCVideoPlayerManager.setLastListener(null);
        }
    }

    @Override
    public void onCompletion() {
        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }

        JCVideoPlayerManager.setListener(null);//这里还不完全,
//        JCVideoPlayerManager.setLastListener(null);
        JCMediaManager.instance().currentVideoWidth = 0;
        JCMediaManager.instance().currentVideoHeight = 0;

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        JCUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        clearFullscreenLayout();
    }

    @Override
    public boolean goToOtherListener() {//这里这个名字这么写并不对,这是在回退的时候gotoother,如果直接gotoother就不叫这个名字

        if (currentScreen == SCREEN_WINDOW_FULLSCREEN
                || currentScreen == SCREEN_WINDOW_TINY) {
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(this, "rotation", 90.0f, 0f);
                animator.setDuration(AnimationDuration).start();

                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
                        vp.removeView(JCVideoPlayer.this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
            } else {
                ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
                vp.removeView(this);
            }
            onEvent(currentScreen == SCREEN_WINDOW_FULLSCREEN ?
                    JCBuriedPoint.ON_QUIT_FULLSCREEN :
                    JCBuriedPoint.ON_QUIT_TINYSCREEN);
            if (JCVideoPlayerManager.lastListener() == null) {//directly fullscreen
                JCVideoPlayerManager.listener().onCompletion();
                showSupportActionBar(getContext());
                return true;
            }

            JCVideoPlayerManager.setListener(JCVideoPlayerManager.lastListener());
            JCVideoPlayerManager.setLastListener(null);
            JCMediaManager.instance().lastState = currentState;//save state
            JCVideoPlayerManager.listener().goBackThisListener();
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    long lastAutoFullscreenTime = 0;

    @Override
    public void autoFullscreenLeft() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
                && isCurrentMediaListener()
                && currentState == CURRENT_STATE_PLAYING
                && currentScreen != SCREEN_WINDOW_FULLSCREEN
                && currentScreen != SCREEN_WINDOW_TINY) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            startWindowFullscreen();
        }
    }

    @Override
    public void autoFullscreenRight() {

    }

    @Override
    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
                && isCurrentMediaListener()
                && currentState == CURRENT_STATE_PLAYING
                && currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    @Override
    public void onBufferingUpdate(int percent) {
        if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            currentState = CURRENT_STATE_PLAYING;
        }

        if (currentState != CURRENT_STATE_NORMAL && currentState != CURRENT_STATE_PREPAREING) {
            setTextAndProgress(percent);
        }
    }

    @Override
    public void onSeekComplete() {
    }

    @Override
    public void onError(int what, int extra) {
        if (what != 38 && what != -38) {
            setUiWitStateAndScreen(CURRENT_STATE_ERROR);
        }
    }

    @Override
    public void onInfo(int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            mBackUpBufferState = currentState;
            setUiWitStateAndScreen(CURRENT_STATE_PLAYING_BUFFERING_START);
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            if (mBackUpBufferState != -1) {
                setUiWitStateAndScreen(mBackUpBufferState);
                mBackUpBufferState = -1;
            }
        }
    }

    @Override
    public void onVideoSizeChanged() {

        int mVideoWidth = JCMediaManager.instance().currentVideoWidth;
        int mVideoHeight = JCMediaManager.instance().currentVideoHeight;
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            JCMediaManager.textureView.requestLayout();
        }
    }

    @Override
    public void goBackThisListener() {

        currentState = JCMediaManager.instance().lastState;
        setUiWitStateAndScreen(currentState);
        addTextureView();

        showSupportActionBar(getContext());
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.surface = new Surface(surface);
        JCMediaManager.instance().setDisplay(this.surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        surface.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        onEvent(JCBuriedPoint.ON_SEEK_POSITION);
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (currentState != CURRENT_STATE_PLAYING &&
                currentState != CURRENT_STATE_PAUSE) return;
        int time = seekBar.getProgress() * getDuration() / 100;
        JCMediaManager.instance().mediaPlayer.seekTo(time);
    }

    public static boolean backPress() {
        if (JCVideoPlayerManager.listener() != null) {
            return JCVideoPlayerManager.listener().goToOtherListener();
        }
        return false;
    }

    public void startWindowFullscreen() {

        hideSupportActionBar(getContext());

        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old);
        }
        if (textureViewContainer.getChildCount() > 0) {
            //textureViewContainer.removeAllViews();
        }
        try {
            Constructor<JCVideoPlayer> constructor = (Constructor<JCVideoPlayer>) JCVideoPlayer.this.getClass().getConstructor(Context.class);
            JCVideoPlayer jcVideoPlayer = constructor.newInstance(getContext());

            jcVideoPlayer.setId(FULLSCREEN_ID);
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            int w = wm.getDefaultDisplay().getWidth();
            int h = wm.getDefaultDisplay().getHeight();
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(h, w);
            lp.setMargins((w - h) / 2, -(w - h) / 2, 0, 0);
            vp.addView(jcVideoPlayer, lp);

            jcVideoPlayer.setUp(url, SCREEN_WINDOW_FULLSCREEN, objects);
            jcVideoPlayer.setUiWitStateAndScreen(currentState);
            JCVideoPlayerManager.setLastListener(this);
            JCVideoPlayerManager.setListener(jcVideoPlayer);

            jcVideoPlayer.addTextureView();

            ObjectAnimator animator = ObjectAnimator.ofFloat(jcVideoPlayer, "rotation", 0.0f, 90.0f);
            animator.setDuration(AnimationDuration).start();


        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startWindowTiny() {
        onEvent(JCBuriedPoint.ON_ENTER_TINYSCREEN);

        ViewGroup vp = (ViewGroup) (JCUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(TINY_ID);
        if (old != null) {
            vp.removeView(old);
        }
//        if (textureViewContainer.getChildCount() > 0) {
//            //textureViewContainer.removeAllViews();
//        }
        try {
            Constructor<JCVideoPlayer> constructor = (Constructor<JCVideoPlayer>) JCVideoPlayer.this.getClass().getConstructor(Context.class);
            JCVideoPlayer mJcVideoPlayer = constructor.newInstance(getContext());
            mJcVideoPlayer.setId(TINY_ID);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(400, 400);
            lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
            vp.addView(mJcVideoPlayer, lp);
            mJcVideoPlayer.setUp(url, SCREEN_WINDOW_TINY, objects);
            mJcVideoPlayer.setUiWitStateAndScreen(currentState);
            mJcVideoPlayer.addTextureView();
            JCVideoPlayerManager.setLastListener(this);
            JCVideoPlayerManager.setListener(mJcVideoPlayer);

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setTextAndProgress(0);
                    }
                });
            }
        }
    }

    public int getCurrentPositionWhenPlaying() {
        int position = 0;
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            try {
                position = (int) JCMediaManager.instance().getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return JCMediaManager.instance().currentPosition;
            }
        } else {
            return JCMediaManager.instance().duration;
        }
        return position;
    }

    public int getDuration() {
        int duration = 0;
        try {
            if (currentState == CURRENT_STATE_PLAYING) {
                duration = JCMediaManager.instance().getDuration();
            } else {
                return JCMediaManager.instance().duration;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return JCMediaManager.instance().duration;
        }
        return duration;
    }

    public void setTextAndProgress(int secProgress) {
        int position = getCurrentPositionWhenPlaying();
        int duration = getDuration();
        int progress = position * 100 / (duration == 0 ? 1 : duration);

        setProgressAndTime(progress, secProgress, position, duration);
    }

    public void setProgressAndTime(int progress, int secProgress, int currentTime, int totalTime) {
        if (!mTouchingProgressBar) {
            if (progress != 0) progressBar.setProgress(progress);
        }
        if (secProgress > 95) secProgress = 100;
        if (secProgress != 0) progressBar.setSecondaryProgress(secProgress);
        if (currentTime != 0) currentTimeTextView.setText(JCUtils.stringForTime(currentTime));
        Log.e("aaaaa", progress + " progress" + "    secProgress" + secProgress + "    currentTime" + currentTime + "    totalTime " + totalTime);
        totalTimeTextView.setText(JCUtils.stringForTime(totalTime));
    }

    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(JCUtils.stringForTime(0));
        totalTimeTextView.setText(JCUtils.stringForTime(0));
    }

    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (JCMediaManager.instance().mediaPlayer.isPlaying()) {
                        JCMediaManager.instance().mediaPlayer.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    public void release() {
        if (isCurrentMediaListener() &&
                (System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            releaseAllVideos();
        }
    }

    public boolean isCurrentMediaListener() {
        return JCVideoPlayerManager.listener() != null
                && JCVideoPlayerManager.listener() == this;
    }

    public static void releaseAllVideos() {
        if (JCVideoPlayerManager.listener() != null) {
            JCVideoPlayerManager.listener().onCompletion();
        }
        if (JCVideoPlayerManager.lastListener() != null) {
            JCVideoPlayerManager.lastListener().onCompletion();
        }
        JCMediaManager.instance().releaseMediaPlayer();
    }

    public static void setJcBuriedPoint(JCBuriedPoint jcBuriedPoint) {
        JC_BURIED_POINT = jcBuriedPoint;
    }

    public void onEvent(int type) {
        if (JC_BURIED_POINT != null && isCurrentMediaListener()) {
            JC_BURIED_POINT.onEvent(type, url, currentScreen, objects);
        }
    }

    public static void startFullscreen(Context context, Class _class, String url, Object... objects) {

        hideSupportActionBar(context);
        ViewGroup vp = (ViewGroup) (JCUtils.getAppCompActivity(context)).findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old);
        }
        try {
            Constructor<JCVideoPlayer> constructor = _class.getConstructor(Context.class);
            JCVideoPlayer jcVideoPlayer = constructor.newInstance(context);
            jcVideoPlayer.setId(FULLSCREEN_ID);
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            int w = wm.getDefaultDisplay().getWidth();
            int h = wm.getDefaultDisplay().getHeight();
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(h, w);
            lp.setMargins((w - h) / 2, -(w - h) / 2, 0, 0);
            vp.addView(jcVideoPlayer, lp);

            jcVideoPlayer.setUp(url, SCREEN_WINDOW_FULLSCREEN, objects);
            jcVideoPlayer.addTextureView();
            jcVideoPlayer.startButton.performClick();
            ObjectAnimator animator = ObjectAnimator.ofFloat(jcVideoPlayer, "rotation", 0.0f, 90.0f);
            animator.setDuration(AnimationDuration).start();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = JCUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            JCUtils.getAppCompActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static void showSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = JCUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.show();
            }
        }
        if (TOOL_BAR_EXIST) {
            JCUtils.getAppCompActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static class JCAutoFullscreenListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {//可以得到传感器实时测量出来的变化值
            float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            if (x < -11) {
                //direction right
            } else if (x > 11) {
                //direction left
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().autoFullscreenLeft();
                }
            } else if (y > 11) {
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().autoQuitFullscreen();
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public void showWifiDialog() {
    }

    public void showProgressDialog(float deltaX,
                                   String seekTime, int seekTimePosition,
                                   String totalTime, int totalTimeDuration) {
    }

    public void dismissProgressDialog() {

    }

    public void showVolumDialog(float deltaY, int volumePercent) {

    }

    public void dismissVolumDialog() {

    }


    public abstract int getLayoutId();


}
