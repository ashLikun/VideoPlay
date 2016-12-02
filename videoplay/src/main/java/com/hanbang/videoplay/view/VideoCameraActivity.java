package com.hanbang.videoplay.view;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hanbang.videoplay.R;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Created by Administrator on 2016/2/25.
 * <p>
 * <p>
 * 1:CameraVideo.startActivity(MeActivity.this);
 * <p>
 * 2:if (requestCode == CameraVideo.FINSH_INTENT_FLAG && resultCode == CameraVideo.FINSH_INTENT_FLAG) {
 * String filePath = data.getStringExtra("filePath");
 * }
 */
public class VideoCameraActivity extends Activity implements MediaRecorder.OnErrorListener, CamerListener {
    protected MediaRecorder mediarecorder;// 录制视频的类
    private SurfaceView surfaceview;// 显示视频的控件

    private TextView timeTv;
    private ImageView startIv;
    private ImageView flashlight;
    private ImageView switchCamer;
    private String path;
    public static String RESULT = "filePath";

    private File mVecordFile = null;// 文件

    CameraUtils cameraUtils;

    /**
     * 0：停止
     * 1:正在录制
     */
    private int status = 0;
    private int time = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getIntent().getStringExtra("path");


        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        // 设置横屏显示
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // 选择支持半透明模式,在有surfaceview的activity中使用。
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.video_camear);

        initView();
    }

    public static void startActivity(Activity activity, String path, int code) {
        if (path == null || path.length() == 0) {
            Toast.makeText(activity, "文件路径错误", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(activity, VideoCameraActivity.class);
        intent.putExtra("path", path);
        activity.startActivityForResult(intent, code);
    }

    private void initView() {
        cameraUtils = new CameraUtils(this);
        cameraUtils.setCamerListener(this);
        flashlight = (ImageView) findViewById(R.id.flashlight);
        switchCamer = (ImageView) findViewById(R.id.switchCamer);

        surfaceview = (SurfaceView) this.findViewById(R.id.surfaceview);
        startIv = (ImageView) findViewById(R.id.start);
        startIv.setSelected(false);
        timeTv = (TextView) findViewById(R.id.time);
        startIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!startIv.isSelected() && status == 0) {
                    if (record()) {
                        startIv.setSelected(true);
                        switchCamer.setVisibility(View.GONE);
                    } else {
                        releaseRecord();
                        startIv.setSelected(false);
                        switchCamer.setVisibility(View.VISIBLE);
                    }
                } else {
                    switchCamer.setVisibility(View.VISIBLE);
                    save();
                }

            }
        });

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (status == 1) {
                    new AlertDialog.Builder(VideoCameraActivity.this)
                            .setTitle("友情提示")
                            .setMessage("您的视频正在录制中！确认放弃？")
                            .setNegativeButton("继续录制", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setPositiveButton("残忍放弃", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    finish();
                                }
                            })
                            .show();
                } else {
                    finish();
                }
            }
        });
        flashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraUtils.setFlashMode();
            }
        });
        switchCamer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (status == 0) {
                    cameraUtils.switchCamera();
                }
            }
        });

    }

    protected void save() {
        if (mVecordFile != null) {
            stop();
            if (mVecordFile.exists()) {
                setResult(RESULT_OK, new Intent().putExtra(RESULT, mVecordFile.getAbsolutePath()));
                Log.e("mVecordFile", mVecordFile.getAbsolutePath());
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraUtils.setSurfaceHolder(surfaceview.getHolder());
    }


    private void createRecordDir() {
        File sampleDir = new File(path);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        File vecordDir = sampleDir;
        // 创建文件
        try {
            mVecordFile = File.createTempFile(getDate(), ".mp4", vecordDir);//mp4格式
        } catch (IOException e) {
        }
    }

    /**
     * 初始化
     *
     * @throws IOException
     * @author lip
     * @date 2015-3-16
     */
    private void initRecord() throws IOException {
        mediarecorder = new MediaRecorder();
        //mediarecorder.reset();
        if (cameraUtils.getmCamera() != null) {
            cameraUtils.getmCamera().unlock();
            mediarecorder.setCamera(cameraUtils.getmCamera());
        }
        // 设置录制视频源为Camera(相机)
        mediarecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
        mediarecorder
                .setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // 设置录制的视频编码h263 h264
        mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
        mediarecorder.setVideoSize(cameraUtils.getScreenHeight(), cameraUtils.getScreenWidth());
        // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
        mediarecorder.setVideoFrameRate(25);
        mediarecorder.setVideoEncodingBitRate((int) Math.round(1.1 * 1024 * 1024));// 设置帧频率，然后就清晰了
        mediarecorder.setPreviewDisplay(cameraUtils.getSurface());
        mediarecorder.setOrientationHint(cameraUtils.getRotation());
        // mediaRecorder.setMaxDuration(Constant.MAXVEDIOTIME * 1000);
        mediarecorder.setOutputFile(mVecordFile.getAbsolutePath());

        mediarecorder.prepare();

        try {
            mediarecorder.start();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int i, int i1) {

    }

    /**
     * 开始录制视频
     */


    private boolean record() {
        createRecordDir();
        try {
            initRecord();
            time = 0;
            status = 1;
            if (adSwitchTask == null) {
                timeTv.post(adSwitchTask = new AdSwitchTask(VideoCameraActivity.this));
            }

        } catch (IOException e) {
            e.printStackTrace();
            mVecordFile = null;
            return false;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            mVecordFile = null;
            return false;
        }
        return true;
    }

    /**
     * 停止拍摄
     */
    public void stop() {
        stopRecord();
        releaseRecord();
        cameraUtils.freeCameraResource();
    }

    private void stopRecord() {
        try {

            if (mediarecorder != null) {
                // 设置后不会崩
                mediarecorder.setOnErrorListener(null);
                mediarecorder.setPreviewDisplay(null);
                // 停止录制
                mediarecorder.stop();
                // 释放资源
                mediarecorder.release();
                mediarecorder = null;
                status = 0;
            }
        } catch (Exception e) {
        }

    }

    /**
     * 释放资源
     *
     * @author liuyinjun
     * @date 2015-2-5
     */
    private void releaseRecord() {
        if (mediarecorder != null) {
            mediarecorder.setOnErrorListener(null);
            try {
                mediarecorder.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mediarecorder = null;
    }


    /**
     * 获取系统时间
     */
    private String getDate() {
        SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss", Locale.getDefault());
        Date date = new Date();
        String key = format.format(date);

        Random r = new Random();
        key = key + r.nextInt();
        key = key.substring(0, 15);
        return key;
    }

    private AdSwitchTask adSwitchTask;


    static class AdSwitchTask implements Runnable {

        private final WeakReference<VideoCameraActivity> reference;

        AdSwitchTask(VideoCameraActivity cameraVideo) {
            this.reference = new WeakReference<VideoCameraActivity>(cameraVideo);
        }

        @Override
        public void run() {
            VideoCameraActivity cameraVideo = reference.get();
            if (cameraVideo != null && cameraVideo.status == 1) {
                cameraVideo.timeTv.setText(cameraVideo.getTimeToString(cameraVideo.time));
                cameraVideo.time++;
                cameraVideo.timeTv.postDelayed(cameraVideo.adSwitchTask, 1000);
            }
        }
    }

    private String getTimeToString(int time) {
        StringBuilder stringBuilder = new StringBuilder();
        if (time >= 0) {
            int hours = (time % (24 * 60 * 60)) / (60 * 60);
            int minutes = ((time % (24 * 60 * 60)) % (60 * 60)) / 60;
            int second = ((time % (24 * 60 * 60)) % (60 * 60)) % 60;
            stringBuilder.append(String.format("%02d", hours));
            stringBuilder.append(":");
            stringBuilder.append(String.format("%02d", minutes));
            stringBuilder.append(":");
            stringBuilder.append(String.format("%02d", second));
        } else {
            stringBuilder.append("00:00:00");
        }
        return stringBuilder.toString();

    }

    @Override
    protected void onDestroy() {
        stop();

        super.onDestroy();

    }


    @Override
    public void onFlashImg(boolean isOpen) {
        flashlight.setSelected(isOpen);
    }

    @Override
    public void setFlashImgVisibility(int visibility) {
        flashlight.setVisibility(visibility);
    }
}
