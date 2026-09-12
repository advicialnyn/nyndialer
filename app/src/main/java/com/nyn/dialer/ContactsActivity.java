package com.nyn.dialer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private static final int REQUEST_READ_CONTACTS = 4001;

    private final List<String> allNames = new ArrayList<>();
    private final List<String> allNumbers = new ArrayList<>();

    private List<String> shownNames = new ArrayList<>();
    private List<String> shownNumbers = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
        } else {
            loadContacts();
        }

        EditText search = findViewById(R.id.contactSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadContacts() {
        allNames.clear();
        allNumbers.clear();

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        try (Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                    allNames.add(name != null ? name : "");
                    allNumbers.add(number != null ? number : "");
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "No permission to read contacts", Toast.LENGTH_SHORT).show();
            return;
        }

        shownNames = new ArrayList<>(allNames);
        shownNumbers = new ArrayList<>(allNumbers);
        bindAdapter();
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase();
        shownNames = new ArrayList<>();
        shownNumbers = new ArrayList<>();

        for (int i = 0; i < allNames.size(); i++) {
            String name = allNames.get(i);
            String number = allNumbers.get(i);
            if (q.isEmpty() || name.toLowerCase().contains(q) || number.contains(q)) {
                shownNames.add(name);
                shownNumbers.add(number);
            }
        }
        bindAdapter();
    }

    private void bindAdapter() {
        List<String> namesForAdapter = shownNames.isEmpty()
                ? java.util.Collections.singletonList("No contacts found") : shownNames;
        List<String> numbersForAdapter = shownNames.isEmpty()
                ? java.util.Collections.singletonList("") : shownNumbers;

        adapter = new ArrayAdapter<String>(
                this, R.layout.list_item_two_line, android.R.id.text1, namesForAdapter) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text2 = view.findViewById(android.R.id.text2);
                text2.setText(numbersForAdapter.get(position));
                return view;
            }
        };

        ListView listView = findViewById(R.id.contactsList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (shownNumbers.isEmpty()) return;
            String number = shownNumbers.get(position);
            if (number.isEmpty()) return;
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        });
    }
}
