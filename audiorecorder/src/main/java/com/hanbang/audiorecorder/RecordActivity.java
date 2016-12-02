package com.hanbang.audiorecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;


/**
 * Created by Administrator on 2016/4/19.
 */
public class RecordActivity extends Activity implements View.OnClickListener {

    public static String MP3PATH = "MP3PATH";

    public static String path = "";

    private AudioRecordUtils audioRecordUtils = null;
    private String fileName = "record" + System.currentTimeMillis();
    private AudioPlayer audioPlayer = null;

    protected ImageView luzhi;
    protected ImageView playIv;
    protected ImageView wancheng;
    protected ImageView congxin;
    protected TextView recordTime;
    protected ShimmerTextView statusTv;

    private Shimmer shimmer;
    private RecordFileUtils recordFileUtils;


    private class PlayRunnable implements Runnable {

        public boolean ok = false;
        private int allTime = 0;

        public PlayRunnable() {
            if (audioRecordUtils != null) {
                allTime = audioRecordUtils.getTimeS();
            }
        }

        public void setOk() {
            ok = true;
        }

        @Override
        public void run() {
            if (!ok) {
                recordTime.setText(audioPlayer.getTime(allTime));
                recordTime.postDelayed(playRunnable, 100);
            }
        }
    }

    private PlayRunnable playRunnable = null;

    public static void startUi(Activity activity, int RESULT_CODE, String path) {
        Intent intent = new Intent(activity, RecordActivity.class);
        intent.putExtra("path", path);
        activity.startActivityForResult(intent, RESULT_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        path = getIntent().getStringExtra("path");
        recordFileUtils = new RecordFileUtils();

        audioPlayer = new AudioPlayer(recordFileUtils, fileName);
        initView();
    }

    private void initView() {

        luzhi = (ImageView) findViewById(R.id.luzhi);
        playIv = (ImageView) findViewById(R.id.play);
        wancheng = (ImageView) findViewById(R.id.wancheng);
        congxin = (ImageView) findViewById(R.id.congxin);
        recordTime = (TextView) findViewById(R.id.recordTime);
        statusTv = (ShimmerTextView) findViewById(R.id.status);

        audioRecordUtils = new AudioRecordUtils(this, fileName);

        audioRecordUtils.setOnTimeChang(new AudioRecordUtils.OnTimeChang() {
            @Override
            public void onTime(int time) {
                recordTime.setText(audioRecordUtils.getTime());
            }
        });
        luzhi.setTag(1);//1未录制  2:录制中
        playIv.setTag(1);


        luzhi.setOnClickListener(this);
        playIv.setOnClickListener(this);
        wancheng.setOnClickListener(this);
        congxin.setOnClickListener(this);


        shimmer = new Shimmer();
        statusTv.setShimmering(false);
        shimmer.start(statusTv);

        audioPlayer.setOnCompleteListener(new AudioPlayer.OnCompleteListener() {
            @Override
            public void onComplete() {
                completePlay();
            }
        });


        findViewById(R.id.delete).setOnClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioRecordUtils.pauseRecord();
        if (audioPlayer != null && audioPlayer.getmPlayState() == 1) {//播放中
            stopPlay();
        }
        recordFileUtils.deleteAllFiles(RecordFileUtils.RECORDED_NO_MP3_DELETE, fileName);
    }

    @Override
    public void onClick(View view) {

        int i = view.getId();
        /**
         * 录制点击事件
         */
        if (i == R.id.luzhi) {
            if (((Integer) luzhi.getTag()) == 1) {//未录制
                startRecord();
            } else if (((Integer) luzhi.getTag()) == 2) {//录制中，暂停
                pauseRecord();
            }
        }

        /**
         * 完成点击事件
         */
        else if (i == R.id.wancheng) {
            createMp3File();
        } else if (i == R.id.play) {
            /**
             * 播放点击事件
             */
            if (audioPlayer.getmPlayState() == 0) {//未播放
                play();
            } else if (audioPlayer.getmPlayState() == 1) {//播放中
                stopPlay();
            }
        }
        /**
         * 从新录制点击事件
         */
        else if (i == R.id.congxin) {
            audioRecordUtils.reRecord();
            startRecord();
        } else if (i == R.id.delete) {
            finish();
        }

    }


    private void play() {

        if (audioRecordUtils.allTime <= 0) {
            Toast.makeText(this, "请先录制", Toast.LENGTH_SHORT).show();
            return;
        }

        audioPlayer.start();
        recordTime.postDelayed(playRunnable = new PlayRunnable(), 100);
        statusChang(1);
    }

    private void stopPlay() {

        audioPlayer.stop();
        if (playRunnable != null) {
            playRunnable.setOk();
        }
        statusChang(2);
    }

    public void completePlay() {
        statusChang(5);
    }

    /**
     * 开始录制
     */
    private void startRecord() {

        if (audioRecordUtils.startRecord()) {

            stopPlay();
            statusChang(3);
        } else {
            Toast.makeText(this, "正在录制中", Toast.LENGTH_SHORT).show();
        }


    }

    /**
     * 暂停录制
     */
    private void pauseRecord() {

        audioRecordUtils.pauseRecord();
        statusChang(4);
    }

    /**
     * 生成Mp3文件
     */
    private void createMp3File() {

        String path = null;
        audioRecordUtils.stopRecord(new AudioRecordUtils.FileOnSuccess() {
            @Override
            public void onSuccess(String path) {
                if (path == null) path = "";
                setResult(RESULT_OK, new Intent().putExtra(MP3PATH, path));
                finish();
            }
        });
        if (audioPlayer.getmPlayState() == 1) {//播放中
            stopPlay();
        }
        luzhi.setImageResource(R.drawable.material_ar_luyin);
        playIv.setImageResource(R.drawable.material_video_play_play);
        recordTime.setText("00:00");

    }

    /**
     * 作者　　: 李坤
     * 创建时间: 2016/9/6 18:14
     * <p>
     * 方法功能：状态切换时d的view变化与动画
     * <p>
     * st   1:开始播放
     * 2：暂停播放
     * 3:开始录制
     * 4：暂停录制
     * 5:完成播放
     */

    private void statusChang(int st) {

        if (st == 1) {
            playIv.setImageResource(R.drawable.material_video_play_pause);
            wancheng.setVisibility(View.GONE);
            congxin.setVisibility(View.GONE);
            luzhi.setVisibility(View.GONE);
            recordTime.setText("00:00");
            statusTv.setText("播放中...");
            statusTv.setShimmering(true);
        } else if (st == 2) {
            recordTime.setText(audioRecordUtils.getTime());
            congxin.setVisibility(View.VISIBLE);
            wancheng.setVisibility(View.VISIBLE);
            luzhi.setVisibility(View.VISIBLE);
            playIv.setImageResource(R.drawable.material_video_play_play);
            statusTv.setText("  ");
            statusTv.setShimmering(false);
        } else if (st == 3) {
            luzhi.setTag(2);
            congxin.setVisibility(View.GONE);
            wancheng.setVisibility(View.GONE);
            playIv.setVisibility(View.GONE);
            luzhi.setImageResource(R.drawable.material_ar_tingzhi);
            statusTv.setText("录制中...");
            statusTv.setShimmering(true);
        } else if (st == 4) {
            luzhi.setTag(1);

            congxin.setVisibility(View.VISIBLE);
            wancheng.setVisibility(View.VISIBLE);
            playIv.setVisibility(View.VISIBLE);
            luzhi.setImageResource(R.drawable.material_ar_luyin);
            statusTv.setText("  ");
            statusTv.setShimmering(false);
        } else if (st == 5) {
            playIv.setImageResource(R.drawable.material_video_play_play);
            recordTime.setText(audioRecordUtils.getTime());
            congxin.setVisibility(View.VISIBLE);
            wancheng.setVisibility(View.VISIBLE);
            luzhi.setVisibility(View.VISIBLE);
            statusTv.setText("  ");
            statusTv.setShimmering(false);
            statusTv.setText("  ");
            statusTv.setShimmering(false);
        }
    }
}
