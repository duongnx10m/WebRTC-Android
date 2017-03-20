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

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import fr.pchab.androidrtc.MyApplication;
import fr.pchab.androidrtc.R;
import fr.pchab.webrtcclient.PeerConnectionParameters;
import fr.pchab.webrtcclient.VideoRendererGui;
import fr.pchab.webrtcclient.WebRtcClient;

public class RtcActivity2 extends AppCompatActivity implements WebRtcClient.RtcListener {
    public static final String KEY_CALLER_ID = "caller_id";


    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private String mSocketAddress;
    private String callerId;
    //
    private EglBase rootEglBase;
    private SurfaceViewRenderer pipRenderer;
    private SurfaceViewRenderer fullscreenRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.main);
        mSocketAddress = "https://" + getResources().getString(R.string.host);
        //mSocketAddress += (":" + getResources().getString(R.string.port) + "/");

//        pipRenderer = (SurfaceViewRenderer) findViewById(R.id.pip_video_view);
//        fullscreenRenderer = (SurfaceViewRenderer) findViewById(R.id.fullscreen_video_view);
//        // create renderer
        rootEglBase = EglBase.create();
//        pipRenderer.init(rootEglBase.getEglBaseContext(), null);
//        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//
//        fullscreenRenderer.init(rootEglBase.getEglBaseContext(), null);
//        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
//        pipRenderer.setZOrderMediaOverlay(true);
//        pipRenderer.setEnableHardwareScaler(true /* enabled */);
//        fullscreenRenderer.setEnableHardwareScaler(true /* enabled */);

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            callerId = bundle.getString(KEY_CALLER_ID);
        }
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
        vsv.onPause();
        if (client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
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
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, false);
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        Log.d("duongnx", "RtcListener onAddRemoteStream:" + remoteStream.label());
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType, false);
    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        Log.d("duongnx", "RtcListener onRemoveRemoteStream:");
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


}