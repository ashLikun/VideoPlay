package com.android.tedcoder.wkvideoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.android.tedcoder.wkvideoplayer.model.Video;
import com.android.tedcoder.wkvideoplayer.view.MediaController;
import com.android.tedcoder.wkvideoplayer.view.SuperVideoPlayer;

import java.util.ArrayList;


public class VideoPlayActivity extends Activity {
    private SuperVideoPlayer mSuperVideoPlayer;
    public static String INTENT_DATAS_FLAG = "videos";
    public static String INTENT_DATAS_POSTION_FLAG = "videos_postion";
    public static String INTENT_DATA_FLAG = "video";

    private SuperVideoPlayer.VideoPlayCallbackImpl mVideoPlayCallback = new SuperVideoPlayer.VideoPlayCallbackImpl() {
        @Override
        public void onCloseVideo() {
            mSuperVideoPlayer.close();
            mSuperVideoPlayer.setVisibility(View.GONE);
            resetPageToPortrait();
            finish();
        }

        @Override
        public void onSwitchPageType() {
            if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                mSuperVideoPlayer.setPageType(MediaController.PageType.SHRINK);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                mSuperVideoPlayer.setPageType(MediaController.PageType.EXPAND);
            }
        }

        @Override
        public void onPlayFinish() {
            finish();
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /***
     * 旋转屏幕之后回调
     *
     * @param newConfig newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (null == mSuperVideoPlayer) return;
//        /***
//         * 根据屏幕方向重新设置播放器的大小
//         */
//        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            float height = DensityUtil.getWidthInPx(this);
//            float width = DensityUtil.getHeightInPx(this);
//            mSuperVideoPlayer.getLayoutParams().height = (int) height;
//            mSuperVideoPlayer.getLayoutParams().width = (int) width;
//            Log.e("11111111111111", width + "        " + height);
//        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//            float width = DensityUtil.getWidthInPx(this);
//            float height = DensityUtil.getHeightInPx(this);
//            mSuperVideoPlayer.getLayoutParams().width = (int) width;
//            mSuperVideoPlayer.getLayoutParams().height = (int) (width / mSuperVideoPlayer.getVideoWidth() * mSuperVideoPlayer.getVideoHeight());
//            Log.e("2222222222222222222222", width + "        " + height);
//        }
    }


    private void play() {
        ArrayList<Video> videos = (ArrayList<Video>) getIntent().getSerializableExtra(INTENT_DATAS_FLAG);
        if (videos != null && videos.size() != 0) {
            mSuperVideoPlayer.loadMultipleVideo(videos, getIntent().getIntExtra(INTENT_DATAS_POSTION_FLAG, 0), 0);

        } else {
            Video video = (Video) getIntent().getSerializableExtra(INTENT_DATA_FLAG);
            if (video == null) {
                Toast.makeText(this, "没有播放的视频", Toast.LENGTH_SHORT);
                finish();
            } else {
                mSuperVideoPlayer.loadSingleVideo(video);
            }
        }
    }


    public static boolean startActivity(Context context, ArrayList<Video> videos, int playPostion) {
        if (videos == null || videos.size() == 0) {
            return false;
        }
        Intent intent = new Intent(context, VideoPlayActivity.class);
        intent.putExtra(INTENT_DATAS_FLAG,  videos);
        intent.putExtra(INTENT_DATAS_POSTION_FLAG, playPostion);
        context.startActivity(intent);
        return true;
    }

    public static boolean startActivity(Context context, Video video) {
        if (video == null) {
            return false;
        }
        Intent intent = new Intent(context, VideoPlayActivity.class);
        intent.putExtra(INTENT_DATA_FLAG,  video);
        context.startActivity(intent);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_main_play_activity);
        mSuperVideoPlayer = (SuperVideoPlayer) findViewById(R.id.video_player_item_1);
        mSuperVideoPlayer.setVideoPlayCallback(mVideoPlayCallback);
        play();
    }

    /***
     * 恢复屏幕至竖屏
     */
    private void resetPageToPortrait() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mSuperVideoPlayer.setPageType(MediaController.PageType.SHRINK);
        }
    }


}
