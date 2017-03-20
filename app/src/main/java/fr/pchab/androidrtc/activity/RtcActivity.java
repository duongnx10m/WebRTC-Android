/*
 * Copyright 2014 Pierre Chabardes
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

package fr.pchab.androidrtc.activity;

import android.app.FragmentTransaction;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import fr.pchab.androidrtc.MyApplication;
import fr.pchab.androidrtc.R;
import fr.pchab.androidrtc.fragments.CallFragment;
import fr.pchab.webrtcclient.PeerConnectionParameters;
import fr.pchab.webrtcclient.VideoRendererGui;
import fr.pchab.webrtcclient.WebRtcClient;

public class RtcActivity extends AppCompatActivity implements WebRtcClient.RtcListener {
    public static final String KEY_CALLER_ID = "caller_id";
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    private WebRtcClient client;
    private String mSocketAddress;
    private String callerId;
    //
    private EglBase rootEglBase;
    private SurfaceViewRenderer pipRenderer;
    private SurfaceViewRenderer fullscreenRenderer;
    private final ProxyRenderer remoteProxyRenderer = new ProxyRenderer();
    private final ProxyRenderer localProxyRenderer = new ProxyRenderer();
    private CallFragment callFragment;
    private boolean isSwappedFeeds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.activity_call);
        mSocketAddress = "https://" + getResources().getString(R.string.host);
        //mSocketAddress += (":" + getResources().getString(R.string.port) + "/");

        pipRenderer = (SurfaceViewRenderer) findViewById(R.id.pip_video_view);
        fullscreenRenderer = (SurfaceViewRenderer) findViewById(R.id.fullscreen_video_view);
//        // create renderer
        rootEglBase = EglBase.create();
        pipRenderer.init(rootEglBase.getEglBaseContext(), null);
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullscreenRenderer.init(rootEglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true /* enabled */);
        fullscreenRenderer.setEnableHardwareScaler(true /* enabled */);
        setSwappedFeeds(true /* isSwappedFeeds */);
        // local and remote render
        pipRenderer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSwappedFeeds(!isSwappedFeeds);
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            callerId = bundle.getString(KEY_CALLER_ID);
        }

        // add call controls
        callFragment = new CallFragment();
        callFragment.setCallEvents(onCallEvents);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, callFragment);
        ft.commit();

        init();
    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

        client = new WebRtcClient(this, mSocketAddress, params, rootEglBase.getEglBaseContext());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (client != null) {
            client.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if (client != null) {
            client.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onCallReady(String callId) {
        Log.d("duongnx", "RtcListener onCallReady:" + callId);

        if (!TextUtils.isEmpty(callerId)) {
            try {
                answer(callerId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            //call(callId);
            startCam();
        }
    }

    public void answer(String callerId) throws JSONException {
        Log.d("duongnx", "answer:" + callerId);
        //
        client.sendMessage(callerId, "init", null);
        startCam();
    }


    public void startCam() {
        // Camera settings
        client.start(MyApplication.getInstance().getLoginUser());
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        Log.d("duongnx", "RtcListener onStatusChanged:" + newStatus);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        Log.d("duongnx", "RtcListener onLocalStream:" + localStream.label());
        localStream.videoTracks.get(0).setEnabled(true);
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localProxyRenderer));

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        Log.d("duongnx", "RtcListener onAddRemoteStream:" + remoteStream.label());
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteProxyRenderer));

    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        Log.d("duongnx", "RtcListener onRemoveRemoteStream:");

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d("duongnx", "setSwappedFeeds: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyRenderer.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
        fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(!isSwappedFeeds);
    }

    private CallFragment.OnCallEvents onCallEvents = new CallFragment.OnCallEvents() {
        @Override
        public void onCallHangUp() {
            disconnect();
        }

        @Override
        public void onCameraSwitch() {
            if (client != null) {
                client.switchCamera();
            }
        }

        @Override
        public void onVideoScalingSwitch(RendererCommon.ScalingType scalingType) {

        }

        @Override
        public void onCaptureFormatChange(int width, int height, int framerate) {

        }

        @Override
        public boolean onToggleMic() {
            return false;
        }
    };

    private void disconnect() {
        remoteProxyRenderer.setTarget(null);
        localProxyRenderer.setTarget(null);


        if (pipRenderer != null) {
            pipRenderer.release();
            pipRenderer = null;
        }

        if (fullscreenRenderer != null) {
            fullscreenRenderer.release();
            fullscreenRenderer = null;
        }
//        if (audioManager != null) {
//            audioManager.stop();
//            audioManager = null;
//        }

        if (client != null) {
            client.onDestroy();
        }

        finish();
    }


    private class ProxyRenderer implements VideoRenderer.Callbacks {
        private VideoRenderer.Callbacks target;

        synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
            if (target == null) {
                Logging.d("duongnx", "Dropping frame in proxy because target is null.");
                VideoRenderer.renderFrameDone(frame);
                return;
            }

            target.renderFrame(frame);
        }

        synchronized public void setTarget(VideoRenderer.Callbacks target) {
            this.target = target;
        }
    }
}