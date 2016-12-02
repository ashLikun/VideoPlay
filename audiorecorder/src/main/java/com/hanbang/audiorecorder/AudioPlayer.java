package com.hanbang.audiorecorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by Administrator on 2016/4/19.
 */
public class AudioPlayer {

    private RecordFileUtils recordFileUtils;
    private AudioTrack audioTrack;
    private String mAudioRecordFileName;
    private boolean mThreadExitFlag = false;                         // 线程退出标志
    private int mPrimePlaySize = 0;                              // 较优播放块大小
    private int mPlayOffset = 0;                                 // 当前播放位置
    private int mPlayState = 0;                                  // 当前播放状态

    private OnCompleteListener onCompleteListener;

    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public int getmPlayState() {
        return mPlayState;
    }

    public AudioPlayer(RecordFileUtils recordFileUtils, String mAudioRecordFileName) {
        this.recordFileUtils = recordFileUtils;
        this.mAudioRecordFileName = mAudioRecordFileName;
    }

    private void initAudioTrack() {
        // 获得构建对象的最小缓冲区大小
        int minBufSize = AudioTrack.getMinBufferSize(AudioRecordUtils.sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioRecordUtils.audioFormat);
        mPrimePlaySize = minBufSize / 2;
        this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AudioRecordUtils.sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioRecordUtils.audioFormat,
                minBufSize, AudioTrack.MODE_STREAM);
//              AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
//              STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中。
//              这个和我们在socket中发送数据一样，应用层从某个地方获取数据，例如通过编解码得到PCM数据，然后write到audiotrack。
//              这种方式的坏处就是总是在JAVA层和Native层交互，效率损失较大。
//              而STATIC的意思是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
//              后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
//              这种方法对于铃声等内存占用较小，延时要求较高的声音来说很适用。
    }

    public void start() {
        if (audioTrack == null) {
            initAudioTrack();
        }
        if (mPlayState == 0) {
            mPlayOffset = 0;
            new MyAsyncTask().execute();

        }
    }

    public void stop() {
        if (mPlayState == 1) {
            mThreadExitFlag = true;
            audioTrack.stop();
        }
    }

    public void complete() {
        if (onCompleteListener != null) {
            onCompleteListener.onComplete();
        }
    }


    private int allLength = 0;


    private class MyAsyncTask extends AsyncTask<Void, Void, Void> {

        /**
         * 作者　　: 李坤
         * 创建时间: 2016/9/7 13:37
         * <p>
         * 方法功能：调用publishProgress方法触发onProgressUpdate对UI进行操作
         */

        @Override
        protected Void doInBackground(Void... params) {
            if (TextUtils.isEmpty(mAudioRecordFileName)) {
                return null;
            }
            String path = recordFileUtils.getPathOrNull("raw", mAudioRecordFileName);
            if (TextUtils.isEmpty(path)) {
                return null;
            }

            RandomAccessFile fis = null;
            try {
                fis = new RandomAccessFile(path, "rw");
                allLength = (int) (fis.length() / 2);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fis == null) return null;
            audioTrack.play();
            mPlayState = 1;
            mThreadExitFlag = false;
            while (!mThreadExitFlag) {
                int readSize = 0;
                try {
                    short[] audioData = new short[mPrimePlaySize];
                    fis.seek(mPlayOffset * 2);
                    for (readSize = 0; readSize < mPrimePlaySize; readSize++) {
                        try {
                            short b = fis.readShort();
                            audioData[readSize] = b;
                        } catch (EOFException e) {
                            break;
                        }
                    }
                    audioTrack.write(audioData, 0, readSize);
                    mPlayOffset += mPrimePlaySize;
                } catch (Exception e) {
                    e.printStackTrace();
                    stop();
                    break;
                }
                if (readSize < mPrimePlaySize) {
                    stop();
                    break;
                }
            }
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();

            }
            publishProgress();

            mPlayState = 0;
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            complete();
        }

        @Override
        protected void onPostExecute(Void aBoolean) {
            super.onPostExecute(aBoolean);
//            if (aBoolean) {
//                audioTrack.write(audioData, 0, audioData.length);
//                audioTrack.play();
//            }
        }
    }

    public String getTime(int allTime) {
        int res = 0;
        res = (int) ((mPlayOffset / (allLength * 1.0)) * allTime);
        return String.format("%02d:%02d", res / 60, res % 60);
    }

    public interface OnCompleteListener {
        void onComplete();
    }
}
