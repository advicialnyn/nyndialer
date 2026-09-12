package com.nyn.dialer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Outgoing SIP calling screen.
 *
 * Uses the AOSP android.net.sip API. This API is deprecated since API 31 and,
 * more importantly, many OEM Android builds (Samsung, Xiaomi, Oppo, etc.) ship
 * without the underlying SIP stack at all starting around Android 12. On those
 * devices SipManager.isApiSupported() / isVoipSupported() will return false and
 * registration will fail regardless of credentials. It generally still works on
 * AOSP-close builds (Pixel-line devices on older Android versions, some custom ROMs).
 */
public class SipCallActivity extends AppCompatActivity {

    private static final int REQUEST_SIP_PERMISSIONS = 2001;

    private EditText usernameField, domainField, passwordField, destinationField, labelField;
    private TextView statusView;
    private Spinner accountSpinner;

    private SipAccountStore accountStore;
    private List<SipAccount> accounts = new ArrayList<>();
    private boolean suppressSpinnerCallback = false;

    private SipManager sipManager;
    private SipProfile sipProfile;
    private SipAudioCall activeCall;

    private String pendingDestination = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sip);

        usernameField = findViewById(R.id.sipUsername);
        domainField = findViewById(R.id.sipDomain);
        passwordField = findViewById(R.id.sipPassword);
        destinationField = findViewById(R.id.sipDestination);
        labelField = findViewById(R.id.sipLabel);
        statusView = findViewById(R.id.sipStatus);
        accountSpinner = findViewById(R.id.sipAccountSpinner);

        accountStore = new SipAccountStore(this);

        Button callBtn = findViewById(R.id.btnSipCall);
        Button hangupBtn = findViewById(R.id.btnSipHangup);
        Button saveBtn = findViewById(R.id.btnSaveAccount);
        Button deleteBtn = findViewById(R.id.btnDeleteAccount);

        callBtn.setOnClickListener(v -> onCallPressed());
        hangupBtn.setOnClickListener(v -> hangUp());
        saveBtn.setOnClickListener(v -> onSaveAccount());
        deleteBtn.setOnClickListener(v -> onDeleteAccount());

        refreshAccountSpinner(null);

        accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerCallback) return;
                if (position >= 0 && position < accounts.size()) {
                    SipAccount a = accounts.get(position);
                    labelField.setText(a.label);
                    usernameField.setText(a.username);
                    domainField.setText(a.domain);
                    passwordField.setText(a.password);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (!SipManager.isApiSupported(this)) {
            statusView.setText("Status: this device does not support SIP calling");
            callBtn.setEnabled(false);
        } else if (!SipManager.isVoipSupported(this)) {
            statusView.setText("Status: SIP API present, but voice-over-IP is not supported on this device");
            callBtn.setEnabled(false);
        }
    }

    private void refreshAccountSpinner(String selectLabel) {
        accounts = accountStore.loadAll();

        List<String> names = new ArrayList<>();
        for (SipAccount a : accounts) {
            names.add(a.toString());
        }
        if (names.isEmpty()) {
            names.add("No saved accounts yet");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        suppressSpinnerCallback = true;
        accountSpinner.setAdapter(adapter);

        if (selectLabel != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.get(i).label.equalsIgnoreCase(selectLabel)) {
                    accountSpinner.setSelection(i);
                    break;
                }
            }
        }
        suppressSpinnerCallback = false;
    }

    private void onSaveAccount() {
        String label = labelField.getText().toString().trim();
        String username = usernameField.getText().toString().trim();
        String domain = domainField.getText().toString().trim();
        String password = passwordField.getText().toString();

        if (label.isEmpty() || username.isEmpty() || domain.isEmpty()) {
            Toast.makeText(this, "Give it a name, plus username and domain, before saving", Toast.LENGTH_SHORT).show();
            return;
        }

        accountStore.addOrUpdate(new SipAccount(label, username, domain, password));
        refreshAccountSpinner(label);
        Toast.makeText(this, "Saved \"" + label + "\"", Toast.LENGTH_SHORT).show();
    }

    private void onDeleteAccount() {
        String label = labelField.getText().toString().trim();
        if (label.isEmpty()) {
            Toast.makeText(this, "Select or type the account name to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete account")
                .setMessage("Remove saved account \"" + label + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    accountStore.delete(label);
                    labelField.setText("");
                    usernameField.setText("");
                    domainField.setText("");
                    passwordField.setText("");
                    refreshAccountSpinner(null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onCallPressed() {
        String username = usernameField.getText().toString().trim();
        String domain = domainField.getText().toString().trim();
        String password = passwordField.getText().toString();
        String destination = destinationField.getText().toString().trim();

        if (username.isEmpty() || domain.isEmpty() || destination.isEmpty()) {
            Toast.makeText(this, "Fill in username, domain and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasSipPermissions()) {
            pendingDestination = destination;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.USE_SIP, Manifest.permission.RECORD_AUDIO},
                    REQUEST_SIP_PERMISSIONS);
            return;
        }

        registerAndCall(username, domain, password, destination);
    }

    private boolean hasSipPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void registerAndCall(String username, String domain, String password, String destination) {
        try {
            statusView.setText("Status: registering with " + domain + " ...");

            sipManager = SipManager.newInstance(this);

            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            sipProfile = builder.build();

            String sipAction = "com.nyn.dialer.INCOMING_SIP_" + System.currentTimeMillis();
            Intent intent = new Intent(sipAction);
            intent.setPackage(getPackageName());
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_MUTABLE);

            sipManager.open(sipProfile, pendingIntent, null);

            sipManager.setRegistrationListener(sipProfile.getUriString(), new SipRegistrationListener() {
                @Override
                public void onRegistering(String localProfileUri) {
                    runOnUiThread(() -> statusView.setText("Status: registering..."));
                }

                @Override
                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    runOnUiThread(() -> {
                        statusView.setText("Status: registered. Calling " + destination + " ...");
                        placeCall(destination, domain);
                    });
                }

                @Override
                public void onRegistrationFailed(String localProfileUri, String errorCode, String errorMessage) {
                    runOnUiThread(() -> statusView.setText("Status: registration failed - " + errorMessage));
                }
            });

        } catch (Exception e) {
            statusView.setText("Status: error - " + e.getMessage());
        }
    }

    private void placeCall(String destination, String domain) {
        try {
            String callee = destination.startsWith("sip:")
                    ? destination
                    : "sip:" + destination + "@" + domain;

            activeCall = sipManager.makeAudioCall(sipProfile.getUriString(), callee,
                    new SipAudioCall.Listener() {
                        @Override
                        public void onCallEstablished(SipAudioCall call) {
                            call.startAudio();
                            runOnUiThread(() -> statusView.setText("Status: call connected"));
                        }

                        @Override
                        public void onCallEnded(SipAudioCall call) {
                            runOnUiThread(() -> statusView.setText("Status: call ended"));
                        }

                        @Override
                        public void onError(SipAudioCall call, int errorCode, String errorMessage) {
                            runOnUiThread(() -> statusView.setText("Status: call error - " + errorMessage));
                        }
                    }, 30);

        } catch (SipException e) {
            statusView.setText("Status: could not place call - " + e.getMessage());
        }
    }

    private void hangUp() {
        try {
            if (activeCall != null) {
                activeCall.close();
                activeCall = null;
            }
            if (sipManager != null && sipProfile != null) {
                sipManager.close(sipProfile.getUriString());
            }
            statusView.setText("Status: idle");
        } catch (Exception e) {
            statusView.setText("Status: error closing - " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SIP_PERMISSIONS && hasSipPermissions() && pendingDestination != null) {
            String username = usernameField.getText().toString().trim();
            String domain = domainField.getText().toString().trim();
            String password = passwordField.getText().toString();
            registerAndCall(username, domain, password, pendingDestination);
            pendingDestination = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hangUp();
    }
}
