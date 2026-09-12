package com.nyn.dialer;

import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.VideoProfile;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class InCallActivity extends AppCompatActivity implements MyInCallService.InCallCallback {

    private TextView callerNumber, callStatus;
    private View activeControls, ringingControls;
    private Button btnMute, btnSpeaker, btnEndCall, btnAnswer, btnDecline;

    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show this screen even over the lock screen, like a real dialer.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_incall);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        callerNumber = findViewById(R.id.callerNumber);
        callStatus = findViewById(R.id.callStatus);
        activeControls = findViewById(R.id.activeControls);
        ringingControls = findViewById(R.id.ringingControls);
        btnMute = findViewById(R.id.btnMute);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnEndCall = findViewById(R.id.btnEndCall);
        btnAnswer = findViewById(R.id.btnAnswer);
        btnDecline = findViewById(R.id.btnDecline);

        btnMute.setOnClickListener(v -> toggleMute());
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        btnEndCall.setOnClickListener(v -> endCall());
        btnAnswer.setOnClickListener(v -> answerCall());
        btnDecline.setOnClickListener(v -> endCall());

        MyInCallService.setCallback(this);

        Call call = MyInCallService.getCurrentCall();
        if (call != null) {
            updateCallerInfo(call);
            onStateChanged(call.getState());
        }
    }

    private void updateCallerInfo(Call call) {
        Uri handle = call.getDetails().getHandle();
        String number = handle != null ? handle.getSchemeSpecificPart() : "Unknown";
        callerNumber.setText(number);
    }

    private void toggleMute() {
        isMuted = !isMuted;
        audioManager.setMicrophoneMute(isMuted);
        btnMute.setText(isMuted ? "Unmute" : "Mute");
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        audioManager.setSpeakerphoneOn(isSpeakerOn);
        btnSpeaker.setText(isSpeakerOn ? "Speaker Off" : "Speaker");
    }

    private void endCall() {
        Call call = MyInCallService.getCurrentCall();
        if (call != null) {
            call.disconnect();
        }
        finish();
    }

    private void answerCall() {
        Call call = MyInCallService.getCurrentCall();
        if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
        }
    }

    @Override
    public void onCallAdded(Call call) {
        runOnUiThread(() -> {
            updateCallerInfo(call);
            onStateChanged(call.getState());
        });
    }

    @Override
    public void onCallRemoved() {
        runOnUiThread(this::finish);
    }

    @Override
    public void onStateChanged(int state) {
        runOnUiThread(() -> {
            switch (state) {
                case Call.STATE_DIALING:
                    callStatus.setText("Dialing...");
                    showActiveControls();
                    break;
                case Call.STATE_RINGING:
                    callStatus.setText("Incoming call");
                    showRingingControls();
                    break;
                case Call.STATE_ACTIVE:
                    callStatus.setText("Connected");
                    showActiveControls();
                    break;
                case Call.STATE_HOLDING:
                    callStatus.setText("On hold");
                    showActiveControls();
                    break;
                case Call.STATE_DISCONNECTED:
                    callStatus.setText("Call ended");
                    finish();
                    break;
                default:
                    callStatus.setText("");
            }
        });
    }

    private void showActiveControls() {
        activeControls.setVisibility(View.VISIBLE);
        ringingControls.setVisibility(View.GONE);
    }

    private void showRingingControls() {
        activeControls.setVisibility(View.GONE);
        ringingControls.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyInCallService.setCallback(null);
    }
}
