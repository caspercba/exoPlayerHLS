package com.cresta.exoplayerhlssimple;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.cresta.exoplayerhlssimple.exoplayer.DemoPlayer;
import com.cresta.exoplayerhlssimple.exoplayer.HlsRendererBuilder;
import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.chunk.Format;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Created by Gaspar de Elias (gaspar.deelias@cresta.com.ar)
 */

public class ExoPlayerFragment extends Fragment {

    private static final String TAG = ExoPlayerFragment.class.getSimpleName();

    public enum PlayerUiState {noStream, streamSoon, streaming, error, buffering, notAllowed};

    private DemoPlayer mPlayer;
    private boolean mNeedsPrepare;
    private String mUrl = "http://vevoplaylist-live.hls.adaptive.level3.net/vevo/ch1/appleman.m3u8";

    private AudioCapabilitiesReceiver mAudioCapabilitiesReceiver;
    private AudioCapabilities mAudioCapabilities;

    @Bind(R.id.surface_view) SurfaceView mSurface;

    @Bind(R.id.video_frame) AspectRatioFrameLayout mVideoFrame;

    @Bind(R.id.playerInfoText) TextView mPlayerInfoText;

    @Bind(R.id.playerDebugText) TextView mPlayerDebugText;

    @Bind(R.id.debugBlock) LinearLayout mDebugBlock;

    @Bind(R.id.logLevelSpinner) Spinner mLogLevelSpinner;

    @Bind(R.id.playerDebugCheck) CheckBox mPlayerDebugCheckbox;

    @Bind(R.id.bufferingProgress) ProgressBar mBufferingProgress;

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
        setupDebugStuff();
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
        releasePlayer();
    }


    private void preparePlayer() {
        if (mPlayer == null) {
            HlsRendererBuilder rendererBuilder = new HlsRendererBuilder(getActivity(), "USER_AGENT", mUrl, mAudioCapabilities);
            mPlayer = new DemoPlayer(rendererBuilder);
            mPlayer.addListener(mPlayerListener);
            mPlayer.setInfoListener(mInfoListener);
            mNeedsPrepare = true;
        }
        if (mNeedsPrepare) {
            mPlayer.prepare();
            mNeedsPrepare = false;
        }
        mPlayer.setSurface(mSurface.getHolder().getSurface());
        mPlayer.setPlayWhenReady(true);
        mVideoFrame.setAspectRatio(1.6f);
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
            Log.d(TAG, "onStateChanged playWhenReady:" + playWhenReady + " playbackState:" + playbackState);
            addDebugMessage("onStateChanged: state:" + playbackState + " playwhenready:" + playWhenReady, LogLevel.DEBUG);

            switch(playbackState) {
                case ExoPlayer.STATE_IDLE:
                    updatePlayerUi(PlayerUiState.noStream);
                    break;
                case ExoPlayer.STATE_PREPARING:
                    updatePlayerUi(PlayerUiState.buffering);
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    updatePlayerUi(PlayerUiState.buffering);
                    break;
                case ExoPlayer.STATE_READY:
                    updatePlayerUi(PlayerUiState.streaming);
                    break;
                case ExoPlayer.STATE_ENDED:
                    updatePlayerUi(PlayerUiState.noStream);
                    break;
            }
        }

        private void updatePlayerUi(PlayerUiState playerUiState) {

            mPlayerInfoText.setVisibility(View.VISIBLE);
            mBufferingProgress.setVisibility(View.INVISIBLE);

            switch(playerUiState) {
                case noStream:
                    mPlayerInfoText.setText(R.string.livestream_status_no_stream);
                    break;
                case streamSoon:
                    mPlayerInfoText.setText(R.string.livestream_status_starting_soon);
                    break;
                case streaming:
                    mPlayerInfoText.setVisibility(View.GONE);
                    break;
                case buffering:
                    mPlayerInfoText.setText(R.string.livestream_status_buffering);
                    mBufferingProgress.setVisibility(View.VISIBLE);
                    break;
                case error:
                    mPlayerInfoText.setText(R.string.error_video_player_failed);
                    break;
                case notAllowed:
                    //FIXME
                    mPlayerInfoText.setText("NOT ALLOWED");
                    //mPlayerInfoText.setVisibility(View.GONE);
                    break;
            }

        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, "onError " + e.getMessage());
            updatePlayerUi(PlayerUiState.error);
            addDebugMessage("ERROR: " + e.getMessage() + ":" + e.toString(), LogLevel.ERROR);
        }

        @Override
        public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
            Log.d(TAG, "onVideoSizeChanged: width:" + width + ", height:" + height + ", ratio:" + pixelWidthHeightRatio);
            mVideoFrame.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
            addDebugMessage("OnVideSize: " + width + "x" + height + " : ratio: " + pixelWidthHeightRatio, LogLevel.DEBUG);
        }
    };

    private DemoPlayer.InfoListener mInfoListener = new DemoPlayer.InfoListener() {
        @Override
        public void onVideoFormatEnabled(Format format, int trigger, int mediaTimeMs) {
            addDebugMessage("videoFormat:" + format.toString(), LogLevel.DEBUG);
        }

        @Override
        public void onAudioFormatEnabled(Format format, int trigger, int mediaTimeMs) {
            addDebugMessage("audioFormat:" + format.toString(), LogLevel.DEBUG);
        }

        @Override
        public void onDroppedFrames(int count, long elapsed) {
            addDebugMessage("droppedFrames:" + count + ":elapsed:" + elapsed, LogLevel.VERBOSE);
        }

        @Override
        public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
            addDebugMessage("onBandwidthSample:" + bytes + " bytes, bitrateEstimate:" + bitrateEstimate, LogLevel.DEBUG);
        }

        @Override
        public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format, int mediaStartTimeMs, int mediaEndTimeMs) {
            addDebugMessage("onLoadStarted: "+sourceId+":"+ (format!=null?format.bitrate:null)+" bps.", LogLevel.VERBOSE);
        }

        @Override
        public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format, int mediaStartTimeMs, int mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
            addDebugMessage("onLoadCompleted: "+sourceId+":"+ (format!=null?format.bitrate:null)+" bps.", LogLevel.VERBOSE);
        }

        @Override
        public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {
            addDebugMessage("onDecoderInitialized: " + decoderName + ", elapsedRealTime: " + elapsedRealtimeMs + ", initDurationMs: " +initializationDurationMs, LogLevel.VERBOSE);
        }

        @Override
        public void onSeekRangeChanged(TimeRange seekRange) {
            addDebugMessage("onSeekRangeChanged: range:" + seekRange.toString(), LogLevel.VERBOSE);
        }
    };


    /**
     * DEBUG Section
     */

    private enum LogLevel{ERROR, DEBUG, VERBOSE};
    private LogLevel mLogLevel = LogLevel.VERBOSE;

    private void setupDebugStuff() {

        mDebugBlock.setVisibility(View.VISIBLE);
        mPlayerDebugCheckbox.setChecked(true);
        mPlayerDebugText.setMovementMethod(new ScrollingMovementMethod());

        mPlayerDebugCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPlayerDebugText.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.debugLogLevel, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLogLevelSpinner.setAdapter(adapter);
        mLogLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               String selection = (String) parent.getItemAtPosition(position);
                mLogLevel = LogLevel.valueOf(selection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Add debug message. (taken from http://stackoverflow.com/questions/3506696/auto-scrolling-textview-in-android-to-bring-text-into-view)
     * @param msg
     */
    private void addDebugMessage(String msg, LogLevel level) {

        if (level.ordinal() <=mLogLevel.ordinal()) {
            // append the new string
            mPlayerDebugText.append(msg + "\n");
            // find the amount we need to scroll.  This works by
            // asking the TextView's internal layout for the position
            // of the final line and then subtracting the TextView's height

            Layout textViewLayout = mPlayerDebugText.getLayout();
            if (textViewLayout != null) {
                final int scrollAmount = textViewLayout.getLineTop(mPlayerDebugText.getLineCount()) - mPlayerDebugText.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    mPlayerDebugText.scrollTo(0, scrollAmount);
                else
                    mPlayerDebugText.scrollTo(0, 0);
            }
        }
    }




}
