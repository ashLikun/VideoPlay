package com.android.tedcoder.androidvideoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.hanbang.videoplay.view.JCVideoPlayerStandard;


public class MainActivity extends AppCompatActivity {
    private View mPlayBtnView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JCVideoPlayerStandard playerStandard = (JCVideoPlayerStandard) findViewById(R.id.play);
        playerStandard.setUp("http://2449.vod.myqcloud.com/2449_22ca37a6ea9011e5acaaf51d105342e3.f20.mp4"
                , JCVideoPlayerStandard.SCREEN_LAYOUT_LIST, "嫂子坐这");
//        mPlayBtnView = findViewById(R.id.play_btn);
//        mPlayBtnView.setOnClickListener(this);
    }


}
