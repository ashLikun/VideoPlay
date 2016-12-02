package com.hanbang.audiorecorder;


import java.io.File;
import java.io.IOException;

/**
 * Created by Administrator on 2016/4/18.
 */
public class RecordFileUtils {
    public static final int RECORDED_NO_ALL_DELETE = 2;

    //除了MP3文件
    public static final int RECORDED_NO_MP3_DELETE = 1;

    public static String getRawFilePath(String mAudioRecordFileName) {
        return getPath("raw", mAudioRecordFileName);
    }

    //    public static String getAacFilePath(String mAudioRecordFileName) {
//        return getPath("aac", mAudioRecordFileName);
//    }
    public static String getMp3FilePath(String mAudioRecordFileName) {
        return getPath("mp3", mAudioRecordFileName);
    }
//    public static String getM4aFilePath(String mAudioRecordFileName) {
//        return getPath("m4a", mAudioRecordFileName);
//    }

    /**
     * 清空音频录制文件夹中的所有文件
     *
     * @param isRecorded
     */
    public static void deleteAllFiles(int isRecorded, String name) {
        File[] files = new File(RecordFileUtils.getAudioRecordFilePath()).listFiles();
        if (files == null || files.length <= 0) return;
        switch (isRecorded) {
            case RECORDED_NO_MP3_DELETE:
                for (File file : files) {
                    String fileName = file.getName().toUpperCase();
                    if (fileName.contains(name.toUpperCase())) {
                        if (!fileName.endsWith(".MP3")) {
                            file.delete();
                        }
                    }
                }
                break;
            case RECORDED_NO_ALL_DELETE:
                for (File file : files) {
                    String fileName = file.getName().toUpperCase();
                    if (fileName.contains(name.toUpperCase())) {
                        file.delete();
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 音频录制文件夹中的所有文件
     *
     * @return
     */
    public static String getAudioRecordFilePath() {
        return RecordActivity.path + "/record";
    }

    private static String getPath(String tag, String mAudioRecordFileName) {
        String path = RecordActivity.path + "/record";
        File file = new File(path);
        if (file.exists() || file.mkdirs())
            ;
        file = new File(file, mAudioRecordFileName + "." + tag);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return file.getPath();
    }

    public static String getPathOrNull(String tag, String mAudioRecordFileName) {
        String path = RecordActivity.path + "/record";
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        file = new File(file, mAudioRecordFileName + "." + tag);
        if (!file.exists()) {
            return null;
        }
        return file.getPath();
    }

    /**
     * 执行cmd命令，并等待结果
     *
     * @param command 命令
     * @return 是否成功执行
     */
    private static boolean runCommand(String command) {
        boolean ret = false;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            process.waitFor();
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
}
