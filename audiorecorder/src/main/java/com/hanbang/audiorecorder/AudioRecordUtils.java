package com.hanbang.audiorecorder;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.buihha.audiorecorder.SimpleLame;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 2016/4/18.
 */
public class AudioRecordUtils {
    private SimpleLame simpleLame;
    private float voiceLevel = 0;
    private final int audioSource = MediaRecorder.AudioSource.MIC;
    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22800016000,11025
    public static final int sampleRateInHz = 16000;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_IN_MONO为单声道
    public static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    public static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int inBufSize;

    /*录音需要的一些变量
    */

    private AudioRecord audioRecord;

    private Dialog dialog = null;
    /**
     * 录音状态
     */
    private boolean isRecord = false;
    /**
     * 是否转换ok
     */
    private boolean convertOk = true;

    private Context mContext;
    //录音总时长 毫秒
    public int allTime = 0;
    /**
     * 录制的音频文件名称
     */
    private String mAudioRecordFileName;
    private OnTimeChang onTimeChang = null;

    public void setOnTimeChang(OnTimeChang onTimeChang) {
        this.onTimeChang = onTimeChang;
    }

    public interface OnTimeChang {
        void onTime(int time);
    }

    public interface FileOnSuccess {
        void onSuccess(String path);
    }


    private Handler handler = null;

    /**
     * 开始录音
     */
    public boolean startRecord() {
        if (!isRecord) {
            new AudioRecordTask().execute();
            return true;
        } else {
            //正在录制
            return false;
        }
    }

    /**
     * 暂停录音
     */
    public void pauseRecord() {
        isRecord = false;
    }

    /**
     * 停止录音,生成mp3文件
     */
    public void stopRecord(FileOnSuccess fileOnSuccess) {
        pauseRecord();
        if (convertOk) {
            new AudioEncoderTask(fileOnSuccess).execute();
        }
    }

    /**
     * 重新录制
     */
    public void reRecord() {
        //重新录制时，删除录音文件夹中的全部文件
        RecordFileUtils.deleteAllFiles(RecordFileUtils.RECORDED_NO_ALL_DELETE, mAudioRecordFileName);
        allTime = 0;
    }

    public AudioRecordUtils(Context context, String audioRecordFileName) {

        handler = new Handler();
        mContext = context;
        dialog = new Dialog(context,R.style.ARDialog);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.ar_dialog_loadding);
        simpleLame = new SimpleLame();
        mAudioRecordFileName = audioRecordFileName;
        RecordFileUtils.deleteAllFiles(RecordFileUtils.RECORDED_NO_ALL_DELETE, mAudioRecordFileName);
        initAudioRecord();
    }

    /**
     * 计时功能
     */
    private RtRunnable runnable = null;

    private class RtRunnable implements Runnable {
        public boolean pause = false;

        public void setPause() {
            pause = true;
        }


        @Override
        public void run() {
            if (!pause) {
                allTime += 100;
                handler.postDelayed(runnable, 100);
                if (onTimeChang != null) {
                    onTimeChang.onTime(allTime);
                }
            }
        }
    }

    /**
     * 开始统计时间
     */
    private void startTime() {
        handler.postDelayed(runnable = new RtRunnable(), 100);
    }

    /**
     * 暂停时间统计
     */
    private void pauseTime() {
        if (runnable != null) {
            runnable.setPause();
        }
    }

    /**
     * 初始化对象
     */
    private void initAudioRecord() {
        inBufSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        audioRecord = new AudioRecord(audioSource, sampleRateInHz,
                channelConfig, audioFormat, inBufSize);
        // SimpleLame.init(sampleRateInHz, 1, sampleRateInHz, 32);
        simpleLame.init(sampleRateInHz, 1, sampleRateInHz, 96, 8);

    }


    /**
     * 转码
     */
    private synchronized String encodeAudio() {
        //清除时间
        allTime = 0;
        // 开始转换
        File file = new File(RecordFileUtils.getAudioRecordFilePath(), mAudioRecordFileName + ".raw");
        if (!file.exists() || file.length() <= 0) {
            //源文件不存在  或者没内容
            return null;
        }
        convertOk = true;
        return convertOk ? RecordFileUtils.getMp3FilePath(mAudioRecordFileName) : null;// convertOk==true,return true


    }


    //吧录制的数据存到文件中(异步)
    class AudioRecordTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            if (audioRecord == null) {
                initAudioRecord();
            }
            DataOutputStream output = null;
            DataOutputStream encodeDps = null;
            try {
                String p = RecordFileUtils.getRawFilePath(mAudioRecordFileName);
                String p2 = RecordFileUtils.getMp3FilePath(mAudioRecordFileName);
                if (TextUtils.isEmpty(p) || TextUtils.isEmpty(p2)) {
                    return null;
                }
                output = new DataOutputStream(new BufferedOutputStream(
                        new FileOutputStream(p, true)));
                encodeDps = new DataOutputStream(new BufferedOutputStream(
                        new FileOutputStream(p2, true)));
                //开始录制音频
                audioRecord.startRecording();
                //加入时间
                startTime();
                isRecord = true;
                short[] mBuffer = new short[inBufSize];
                byte[] mMp3Buffer = new byte[inBufSize * 2];
                while (isRecord) {
                    int readSize = audioRecord.read(mBuffer, 0,
                            mBuffer.length);
                    for (int i = 0; i < readSize; i++) {
                        output.writeShort(mBuffer[i]);
                    }
                    //编码
                    int encResult = simpleLame.encode(mBuffer, mBuffer, readSize, mMp3Buffer);
                    if (encResult > 0) {
                        //buff写入到mp3文件
                        encodeDps.write(mMp3Buffer, 0, encResult);
                    } else {
                        //编码时挂了
                        Log.e("encode", "编码时挂了");
                    }
                }
                pauseTime();
                isRecord = false;
                //停止录制
                audioRecord.stop();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null) {
                        output.close();
                        encodeDps.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }


    //吧录制的文件数据编码(异步)
    class AudioEncoderTask extends AsyncTask<Void, Long, String> {

        FileOnSuccess fileOnSuccess = null;

        public AudioEncoderTask(FileOnSuccess fileOnSuccess) {
            this.fileOnSuccess = fileOnSuccess;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            convertOk = false;
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            convertOk = false;
            return encodeAudio();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (fileOnSuccess != null) {
                fileOnSuccess.onSuccess(result);
            }
        }
    }


    /**
     * 获取时间总和  单位 S
     */
    public String getTime() {

        return String.format("%02d:%02d", (int) (allTime / 1000.0) / 60, (int) (allTime / 1000.0) % 60);
    }

    /**
     * 获取时间总和  单位 S  不从新赋值
     */
    public int getTimeS() {
        return (int) (allTime / 1000.0);
    }
}