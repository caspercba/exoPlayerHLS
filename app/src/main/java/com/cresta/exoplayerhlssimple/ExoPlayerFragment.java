package com.cresta.exoplayerhlssimple;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import com.cresta.exoplayerhlssimple.exoplayer.DemoPlayer;
import com.cresta.exoplayerhlssimple.exoplayer.HlsRendererBuilder;
import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * A placeholder fragment containing a simple view.
 */
public class ExoPlayerFragment extends Fragment {

    private static final String TAG = ExoPlayerFragment.class.getSimpleName();

    private DemoPlayer mPlayer;
    private boolean mNeedsPrepare;
    private String mUrl = "http://vevoplaylist-live.hls.adaptive.level3.net/vevo/ch1/appleman.m3u8";

    private AudioCapabilitiesReceiver mAudioCapabilitiesReceiver;
    private AudioCapabilities mAudioCapabilities;

    @Bind(R.id.surface_view) SurfaceView mSurface;

    @Bind(R.id.video_frame) AspectRatioFrameLayout mVideoFrame;

    public ExoPlayerFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getActivity(), mAudioCapabilitiesListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_exoplayer, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAudioCapabilitiesReceiver.register();
        preparePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAudioCapabilitiesReceiver.unregister();
    }


    private void preparePlayer() {
        if (mPlayer == null) {
            HlsRendererBuilder rendererBuilder = new HlsRendererBuilder(getActivity(), "USER_AGENT", mUrl, mAudioCapabilities);
            mPlayer = new DemoPlayer(rendererBuilder);
            mPlayer.addListener(mPlayerListener);
            mNeedsPrepare = true;
        }
        if (mNeedsPrepare) {
            mPlayer.prepare();
            mNeedsPrepare = false;
        }
        mPlayer.setSurface(mSurface.getHolder().getSurface());
        mPlayer.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }


    // TODO: Find more information about audioCapabilites stuff.
    private AudioCapabilitiesReceiver.Listener mAudioCapabilitiesListener = new AudioCapabilitiesReceiver.Listener() {
        @Override
        public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
            boolean audioCapabilitiesChanged = !audioCapabilities.equals(mAudioCapabilities);
            if (mPlayer == null || audioCapabilitiesChanged) {
                mAudioCapabilities = audioCapabilities;
                releasePlayer();
                preparePlayer();
            } else if (mPlayer != null) {
                mPlayer.setBackgrounded(false);
            }
        }
    };

    private DemoPlayer.Listener mPlayerListener = new DemoPlayer.Listener() {
        @Override
        public void onStateChanged(boolean playWhenReady, int playbackState) {

        }

        @Override
        public void onError(Exception e) {
        }

        @Override
        public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
            Log.d(TAG, "onVideoSizeChanged: width:" + width + ", height:" + height + ", ratio:" + pixelWidthHeightRatio);
            mVideoFrame.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
        }
    };

}
