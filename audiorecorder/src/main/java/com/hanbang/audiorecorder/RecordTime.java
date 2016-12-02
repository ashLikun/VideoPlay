package com.hanbang.audiorecorder;

/**
 * Created by Administrator on 2016/4/19.
 */
public class RecordTime {

    private long startTime = 0;
    private long endTime = 0;
    private boolean isFinish = false;

    public RecordTime() {
    }

    public boolean isFinish() {
        return isFinish;
    }

    public void setIsFinish(boolean isFinish) {
        this.isFinish = isFinish;
    }

    public RecordTime(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getTime() {
        int res = (int) ((endTime - startTime) / 1000.0f);
        return res < 0 ? 0 : res;
    }
}
