/*
 *
 * Copyright 2015 TedXiong xiong-wei@hotmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tedcoder.wkvideoplayer.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Ted on 2015/9/2.
 * 视频对象
 */
public class Video implements Serializable {

    private String mVideoName;//视频名称 如房源视频、小区视频
    private String mVideoUrl;//视频的地址列表
    private boolean isOnlineVideo = true;//是否在线视频 默认在线视频


    public Video(String mVideoName, String mVideoUrl, boolean isOnlineVideo) {
        this.mVideoName = mVideoName;
        this.mVideoUrl = mVideoUrl;
        this.isOnlineVideo = isOnlineVideo;
        if (isOnlineVideo == false && !TextUtils.isEmpty(mVideoName)) {
            mVideoName = "本地视频";
        }
    }

    public Video() {
    }

    public String getmVideoName() {
        return mVideoName;
    }

    public void setmVideoName(String mVideoName) {
        if (isOnlineVideo == false && !TextUtils.isEmpty(mVideoName)) {
            this.mVideoName = "本地视频";
        }
        this.mVideoName = mVideoName;
    }

    public String getmVideoUrl() {
        return mVideoUrl;
    }

    public void setmVideoUrl(String mVideoUrl) {
        this.mVideoUrl = mVideoUrl;
    }

    public boolean isOnlineVideo() {
        if (isOnlineVideo == false && !TextUtils.isEmpty(mVideoName)) {
            mVideoName = "本地视频";
        }
        return isOnlineVideo;

    }

    public void setIsOnlineVideo(boolean isOnlineVideo) {
        this.isOnlineVideo = isOnlineVideo;
    }

}
