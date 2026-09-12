package com.nyn.dialer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.format.DateFormat;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class RecentCallsActivity extends AppCompatActivity {

    private static final int REQUEST_READ_CALL_LOG = 3001;
    private final List<String> numbers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recents);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_READ_CALL_LOG);
        } else {
            loadRecents();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CALL_LOG) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadRecents();
            } else {
                Toast.makeText(this, "Call log permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadRecents() {
        List<String> lines1 = new ArrayList<>();
        List<String> lines2 = new ArrayList<>();
        numbers.clear();

        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.CACHED_NAME
        };

        try (Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null, null,
                CallLog.Calls.DATE + " DESC")) {

            if (cursor != null) {
                int limit = 200;
                while (cursor.moveToNext() && limit-- > 0) {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));

                    String typeLabel;
                    switch (type) {
                        case CallLog.Calls.OUTGOING_TYPE: typeLabel = "Outgoing"; break;
                        case CallLog.Calls.INCOMING_TYPE: typeLabel = "Incoming"; break;
                        case CallLog.Calls.MISSED_TYPE: typeLabel = "Missed"; break;
                        case CallLog.Calls.REJECTED_TYPE: typeLabel = "Rejected"; break;
                        default: typeLabel = "Call";
                    }

                    String display = (name != null && !name.isEmpty()) ? name : number;
                    String when = DateFormat.format("MMM d, h:mm a", date).toString();

                    lines1.add(display);
                    lines2.add(typeLabel + " \u00b7 " + when);
                    numbers.add(number);
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "No permission to read call log", Toast.LENGTH_SHORT).show();
            return;
        }

        if (numbers.isEmpty()) {
            lines1.add("No recent calls");
            lines2.add("");
            numbers.add(null);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.list_item_two_line, android.R.id.text1, lines1) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                android.widget.TextView text2 = view.findViewById(android.R.id.text2);
                text2.setText(lines2.get(position));
                return view;
            }
        };

        ListView listView = findViewById(R.id.recentsList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String number = numbers.get(position);
            if (number == null) return;
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        });
    }
}
