package com.nyn.dialer;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;

/**
 * Android hands us a Call object here the moment a call starts or comes in,
 * but only once this app is granted the ROLE_DIALER role (see MainActivity's
 * "Set as default dialer" button). Without that role, this service is never
 * invoked and calls fall back to whatever the system's actual default dialer is.
 */
public class MyInCallService extends InCallService {

    private static Call currentCall;
    private static InCallCallback callback;

    public interface InCallCallback {
        void onCallAdded(Call call);
        void onCallRemoved();
        void onStateChanged(int state);
    }

    public static void setCallback(InCallCallback cb) {
        callback = cb;
        if (cb != null && currentCall != null) {
            cb.onCallAdded(currentCall);
        }
    }

    public static Call getCurrentCall() {
        return currentCall;
    }

    private final Call.Callback callStateListener = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            if (callback != null) {
                callback.onStateChanged(state);
            }
        }
    };

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        currentCall = call;
        call.registerCallback(callStateListener);

        Intent intent = new Intent(this, InCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        if (callback != null) {
            callback.onCallAdded(call);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(callStateListener);
        currentCall = null;
        if (callback != null) {
            callback.onCallRemoved();
        }
    }
}
