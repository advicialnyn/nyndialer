package com.nyn.dialer;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CALL_PERMISSION = 1001;
    private static final int REQUEST_ROLE_DIALER = 1002;
    private EditText numberDisplay;
    private String pendingNumber = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        numberDisplay = findViewById(R.id.numberDisplay);

        int[] keyIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnStar, R.id.btnHash
        };

        for (int id : keyIds) {
            findViewById(id).setOnClickListener(v -> {
                String digit = ((android.widget.Button) v).getText().toString();
                appendDigit(digit);
            });
        }

        findViewById(R.id.btnDelete).setOnClickListener(v -> deleteDigit());
        findViewById(R.id.btnDelete).setOnLongClickListener(v -> {
            numberDisplay.setText("");
            return true;
        });

        findViewById(R.id.btnCall).setOnClickListener(v -> makeCall());

        findViewById(R.id.btnOpenSip).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SipCallActivity.class)));

        findViewById(R.id.btnRecents).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentCallsActivity.class)));

        findViewById(R.id.btnContacts).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ContactsActivity.class)));

        Button setDefaultBtn = findViewById(R.id.btnSetDefault);
        setDefaultBtn.setOnClickListener(v -> requestDefaultDialer());
        updateDefaultDialerButtonLabel(setDefaultBtn);

        requestCorePermissionsIfNeeded();
    }

    private void requestCorePermissionsIfNeeded() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        String[] needed = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.READ_CONTACTS
        };
        for (String p : needed) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), REQUEST_CALL_PERMISSION);
        }
    }

    private void updateDefaultDialerButtonLabel(Button button) {
        boolean isDefault = isDefaultDialer();
        button.setText(isDefault ? "This is your default dialer \u2713" : "Set as default dialer");
    }

    private boolean isDefaultDialer() {
        TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
        return telecomManager != null
                && getPackageName().equals(telecomManager.getDefaultDialerPackage());
    }

    private void requestDefaultDialer() {
        if (isDefaultDialer()) {
            Toast.makeText(this, "Already your default dialer", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                startActivityForResult(intent, REQUEST_ROLE_DIALER);
            } else {
                Toast.makeText(this, "This device does not support the dialer role", Toast.LENGTH_LONG).show();
            }
        } else {
            Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
            startActivityForResult(intent, REQUEST_ROLE_DIALER);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ROLE_DIALER) {
            Button setDefaultBtn = findViewById(R.id.btnSetDefault);
            updateDefaultDialerButtonLabel(setDefaultBtn);
            if (isDefaultDialer()) {
                Toast.makeText(this, "Nyn Dialer is now your default dialer", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Button setDefaultBtn = findViewById(R.id.btnSetDefault);
        updateDefaultDialerButtonLabel(setDefaultBtn);
    }

    private void appendDigit(String digit) {
        String current = numberDisplay.getText().toString();
        numberDisplay.setText(current + digit);
    }

    private void deleteDigit() {
        String current = numberDisplay.getText().toString();
        if (!current.isEmpty()) {
            numberDisplay.setText(current.substring(0, current.length() - 1));
        }
    }

    private void makeCall() {
        String number = numberDisplay.getText().toString().trim();
        if (number.isEmpty()) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingNumber = number;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    REQUEST_CALL_PERMISSION);
            return;
        }

        dial(number);
    }

    private void dial(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        try {
            startActivity(intent);
        } catch (SecurityException e) {
            // Permission was denied at the OS level; fall back to the system dialer.
            Intent fallback = new Intent(Intent.ACTION_DIAL);
            fallback.setData(Uri.parse("tel:" + number));
            startActivity(fallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && pendingNumber != null) {
            dial(pendingNumber);
            pendingNumber = null;
        }
    }
}
