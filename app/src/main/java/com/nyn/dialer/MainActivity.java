package com.nyn.dialer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CALL_PERMISSION = 1001;
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
