package com.hanbang.videoplay;

/**
 * 作者　　: 李坤
 * 创建时间:2016/11/24　18:11
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */

public class MediaStatus {

    public static final int CURRENT_STATE_NORMAL = 0;//初始化状态
    public static final int CURRENT_STATE_PREPAREING = 1;//z准备中
    public static final int CURRENT_STATE_PLAYING = 2;//播放中
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;//缓冲中
    public static final int CURRENT_STATE_PAUSE = 5;//暂停
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;//自动播放完成
    public static final int CURRENT_STATE_ERROR = 7;//播放错误
}
