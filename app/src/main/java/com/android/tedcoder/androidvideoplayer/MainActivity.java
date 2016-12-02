package com.android.tedcoder.androidvideoplayer;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.hanbang.audiorecorder.RecordActivity;
import com.hanbang.videoplay.view.VideoPlayerStandard;


public class MainActivity extends AppCompatActivity {
    private View mPlayBtnView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        VideoPlayerStandard playerStandard = (VideoPlayerStandard) findViewById(R.id.play);
        playerStandard.setUp("http://2449.vod.myqcloud.com/2449_22ca37a6ea9011e5acaaf51d105342e3.f20.mp4"
                , VideoPlayerStandard.SCREEN_LAYOUT_LIST, "嫂子坐这");
//        mPlayBtnView = findViewById(R.id.play_btn);
//        mPlayBtnView.setOnClickListener(this);



        findViewById(R.id.kaishi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                VideoCameraActivity.startActivity(MainActivity.this, Environment.getExternalStorageDirectory().getPath() + "/neiCan/file");
//                ;
                RecordActivity.startUi(MainActivity.this,100, Environment.getExternalStorageDirectory().getPath() + "/neiCan/file");
            }
        });
    }


}
