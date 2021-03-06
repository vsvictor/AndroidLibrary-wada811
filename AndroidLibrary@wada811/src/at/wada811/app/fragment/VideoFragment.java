/*
 * Copyright 2013 wada811<at.wada811@gmail.com>
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
package at.wada811.app.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import at.wada811.android.library.R;
import at.wada811.utils.DisplayUtils;
import at.wada811.utils.LogUtils;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

public class VideoFragment extends Fragment implements SurfaceHolder.Callback, OnVideoSizeChangedListener, OnBufferingUpdateListener, OnPreparedListener, OnCompletionListener {

    public static final String TAG = VideoFragment.class.getSimpleName();

    private VideoCallback mCallback;

    public SurfaceView mSurfaceView;
    public SurfaceHolder mHolder;
    public MediaPlayer mMediaPlayer;

    public static final String KEY_RES_ID = "KEY_RES_ID";
    public static final String KEY_FILE_PATH = "KEY_FILE_PATH";

    private int mDisplayWidth = 0;
    private int mDisplayHeight = 0;
    private int mVideoWidth;
    private int mVideoHeight;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    private boolean mIsVideoAutoPlay = false;

    public static interface VideoCallbackProvider {
        public VideoCallback getVideoCallback();
    }

    public static interface VideoCallback {

        public void onActivityCreated(VideoFragment videoFragment);

        public void onPrepared(VideoFragment videoFragment);

        public void onCompletion(VideoFragment videoFragment);

        public void surfaceDestroyed(VideoFragment videoFragment);
    }

    public static VideoFragment newInstance(int videoResId){
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putInt(VideoFragment.KEY_RES_ID, videoResId);
        fragment.setArguments(args);
        return fragment;
    }

    public static VideoFragment newInstance(String videoFilePath){
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(VideoFragment.KEY_FILE_PATH, videoFilePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        LogUtils.i();
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        LogUtils.i();

        if(activity instanceof VideoCallbackProvider == false){
            throw new ClassCastException("activity must implements VideoCallbackPicker.");
        }
        VideoCallbackProvider picker = (VideoCallbackProvider)activity;
        mCallback = picker.getVideoCallback();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        LogUtils.i();
        mSurfaceView = (SurfaceView)inflater.inflate(R.layout.fragment_video, null);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        return mSurfaceView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        LogUtils.i();
        initMediaPlayer();
    }

    private void initMediaPlayer(){
        LogUtils.i();
        if(getArguments().keySet().contains(KEY_RES_ID)){
            mMediaPlayer = MediaPlayer.create(getActivity(), getArguments().getInt(KEY_RES_ID));
        }else{
            mMediaPlayer = new MediaPlayer();
        }
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnInfoListener(new OnInfoListener(){
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra){
                switch(what){
                    case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING");
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_BUFFERING_END");
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_BUFFERING_START");
                        break;
                    case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_METADATA_UPDATE");
                        break;
                    case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_NOT_SEEKABLE");
                        break;
                    case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT");
                        break;
                    case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE");
                        break;
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START");
                        break;
                    case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING");
                        break;
                    case MediaPlayer.MEDIA_INFO_UNKNOWN:
                    default:
                        LogUtils.i("MediaPlayer.MEDIA_INFO_UNKNOWN");
                        break;
                }
                LogUtils.d("extra: " + extra);
                return false;
            }
        });
        mMediaPlayer.setOnErrorListener(new OnErrorListener(){
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra){
                switch(what){
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        LogUtils.e("MediaPlayer.MEDIA_ERROR_SERVER_DIED");
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    default:
                        LogUtils.e("MediaPlayer.MEDIA_ERROR_UNKNOWN");
                        break;
                }
                switch(extra){
                    case MediaPlayer.MEDIA_ERROR_IO:
                        LogUtils.e("MediaPlayer.MEDIA_ERROR_IO");
                        break;
                    case MediaPlayer.MEDIA_ERROR_MALFORMED:
                        LogUtils.e("MediaPlayer.MEDIA_ERROR_MALFORMED");
                        break;
                    case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                        LogUtils.e("MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
                        break;
                    case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                        LogUtils.e("MediaPlayer.MEDIA_ERROR_TIMED_OUT");
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                        LogUtils.e("MediaPlayer.MEDIA_ERROR_UNSUPPORTED");
                        break;
                    default:
                        LogUtils.e("extra: " + extra);
                        break;
                }
                return false;
            }
        });
        mCallback.onActivityCreated(this);
    }

    @Override
    public void onDetach(){
        super.onDetach();
        LogUtils.i();
        stop();
        reset();
        release();
        mHolder.removeCallback(this);
        doCleanUp();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        LogUtils.i();
        mMediaPlayer.setDisplay(holder);
        if(getArguments().keySet().contains(KEY_FILE_PATH)){
            LogUtils.d(getArguments().getString(KEY_FILE_PATH));
            try{
                setDateSource(getArguments().getString(KEY_FILE_PATH));
                prepare();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){
        LogUtils.i("format: " + format);
        LogUtils.i("width: " + width + ", height: " + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        LogUtils.i();
        mCallback.surfaceDestroyed(this);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height){
        LogUtils.i("width: " + width + ", height: " + height);
        if(width == 0 || height == 0){
            LogUtils.e("invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        mIsVideoSizeKnown = true;
        mVideoWidth = width;
        mVideoHeight = height;
        if(mIsVideoReadyToBePlayed && mIsVideoSizeKnown && mIsVideoAutoPlay){
            start();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent){
        LogUtils.i("percent: " + percent);
    }

    @Override
    public void onPrepared(MediaPlayer mp){
        LogUtils.i();
        mCallback.onPrepared(this);
        mIsVideoReadyToBePlayed = true;
        if(mIsVideoReadyToBePlayed && mIsVideoSizeKnown && mIsVideoAutoPlay){
            start();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp){
        LogUtils.i();
        mCallback.onCompletion(this);
    }

    public void setVideoAutoPlay(boolean isVideoAutoPlay){
        mIsVideoAutoPlay = isVideoAutoPlay;
    }

    public void setDateSource(AssetManager assets, String fileName) throws IllegalArgumentException, IllegalStateException, IOException{
        mMediaPlayer.setDataSource(assets.openFd(fileName).getFileDescriptor());
    }

    public void setDateSource(FileDescriptor fd) throws IllegalArgumentException, IllegalStateException, IOException{
        mMediaPlayer.setDataSource(fd);
    }

    public void setDateSource(String path) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException{
        mMediaPlayer.setDataSource(path);
    }

    public void setDateSource(Uri uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException{
        mMediaPlayer.setDataSource(getActivity(), uri);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setDateSource(Uri uri, Map<String, String> headers) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException{
        mMediaPlayer.setDataSource(getActivity(), uri, headers);
    }

    public void setDateSource(FileDescriptor fd, long offset, long length) throws IllegalArgumentException, IllegalStateException, IOException{
        mMediaPlayer.setDataSource(fd, offset, length);
    }

    public void setDisplayWidth(int width){
        mDisplayWidth = width;
    }

    public void setDisplayHeight(int height){
        mDisplayHeight = height;
    }

    public void prepare() throws IllegalStateException, IOException{
        mMediaPlayer.prepare();
    }

    private void setSize(){
        LogUtils.i("width: " + mVideoWidth + ", height: " + mVideoHeight);
        int videoWidth = DisplayUtils.getWidth(getActivity());
        int videoHeight = DisplayUtils.getHeight(getActivity());
        if(mDisplayWidth == 0 && mDisplayHeight == 0){
            videoHeight = mVideoHeight * videoWidth / mVideoWidth;
        }else if(mDisplayWidth != 0 && mDisplayHeight == 0){
            videoWidth = mDisplayWidth;
            videoHeight = mVideoHeight * videoWidth / mVideoWidth;
        }else if(mDisplayWidth == 0 && mDisplayHeight != 0){
            videoHeight = mDisplayHeight;
            videoWidth = mVideoWidth * videoHeight / mVideoHeight;
        }else if(mDisplayWidth != 0 && mDisplayHeight != 0){
            videoWidth = mDisplayWidth;
            videoHeight = mDisplayHeight;
        }
        LogUtils.i("width: " + videoWidth + ", height: " + videoHeight);
        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(videoWidth, videoHeight));
        mHolder.setFixedSize(videoWidth, videoHeight);
        LogUtils.d("CurrentPosition: " + mMediaPlayer.getCurrentPosition());
    }

    public void seekTo(int msec){
        mMediaPlayer.seekTo(msec);
    }

    public void start(){
        setSize();
        mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition());
        mMediaPlayer.start();
    }

    public void pause(){
        if(mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()){
                mMediaPlayer.pause();
            }
        }
    }

    public void stop(){
        if(mMediaPlayer != null){
            pause();
            mMediaPlayer.stop();
        }
    }

    public void reset(){
        if(mMediaPlayer != null){
            mMediaPlayer.reset();
        }
    }

    public void release(){
        if(mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void doCleanUp(){
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

}
