package com.hanbang.videoplay.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.hanbang.videoplay.R;


/**
 * Manage UI
 * Created by Nathen
 * On 2016/04/10 15:45
 */
public class VideoPlayerSimple extends VideoPlayer {

    public VideoPlayerSimple(Context context) {
        super(context);
    }

    public VideoPlayerSimple(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getLayoutId() {
        return R.layout.video_play_layout_base;
    }

    @Override
    public boolean setUp(String url, int screen, Object... objects) {
        if (super.setUp(url, screen, objects)) {
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                fullscreenButton.setImageResource(R.drawable.material_exit_full_screen);
            } else {
                fullscreenButton.setImageResource(R.drawable.material_full_screen);
            }
            fullscreenButton.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    @Override
    public void setUiWitStateAndScreen(int state) {
        super.setUiWitStateAndScreen(state);
        switch (currentState) {
            case CURRENT_STATE_NORMAL:
                startButton.setVisibility(View.VISIBLE);
                break;
            case CURRENT_STATE_PREPAREING:
                startButton.setVisibility(View.INVISIBLE);
                break;
            case CURRENT_STATE_PLAYING:
                startButton.setVisibility(View.VISIBLE);
                break;
            case CURRENT_STATE_PAUSE:
                break;
            case CURRENT_STATE_ERROR:
                break;
        }
        updateStartImage();
    }

    private void updateStartImage() {
        if (currentState == CURRENT_STATE_PLAYING) {
            startButton.setImageResource(R.drawable.material_video_play_pause);
        } else if (currentState == CURRENT_STATE_ERROR) {
            startButton.setImageResource(R.drawable.material_video_play_warning);
        } else {
            startButton.setImageResource(R.drawable.material_video_play_play);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fullscreen && currentState == CURRENT_STATE_NORMAL) {
            Toast.makeText(getContext(), "Play video first", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onClick(v);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (currentState == CURRENT_STATE_NORMAL) {
                Toast.makeText(getContext(), "Play video first", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        super.onProgressChanged(seekBar, progress, fromUser);
    }

    @Override
    public void onRelease() {

    }

    @Override
    public boolean goToOtherListener() {
        return false;
    }
}
