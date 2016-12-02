package com.hanbang.videoplay.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;


/**
 * Created by Nathen
 * On 2016/04/27 10:49
 */
public class VideoPlayerStandardShowTitleAfterFullscreen extends VideoPlayerStandard {
    public VideoPlayerStandardShowTitleAfterFullscreen(Context context) {
        super(context);
    }

    public VideoPlayerStandardShowTitleAfterFullscreen(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean setUp(String url, int screen, Object... objects) {
        if (super.setUp(url, screen, objects)) {
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                titleTextView.setVisibility(View.VISIBLE);
            } else {
                titleTextView.setVisibility(View.INVISIBLE);
            }
            return true;
        }
        return false;
    }
}
