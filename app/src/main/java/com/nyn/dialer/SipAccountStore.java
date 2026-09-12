package com.nyn.dialer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SipAccountStore {

    private static final String PREFS = "nyn_dialer_sip_accounts";
    private static final String KEY_ACCOUNTS = "accounts_json";

    private final SharedPreferences prefs;

    public SipAccountStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<SipAccount> loadAll() {
        List<SipAccount> result = new ArrayList<>();
        String raw = prefs.getString(KEY_ACCOUNTS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                result.add(SipAccount.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // Corrupt or empty store; return whatever was parsed so far.
        }
        return result;
    }

    public void saveAll(List<SipAccount> accounts) {
        JSONArray arr = new JSONArray();
        try {
            for (SipAccount a : accounts) {
                arr.put(a.toJson());
            }
        } catch (JSONException ignored) {
        }
        prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply();
    }

    public void addOrUpdate(SipAccount account) {
        List<SipAccount> accounts = loadAll();
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).label.equalsIgnoreCase(account.label)) {
                accounts.set(i, account);
                saveAll(accounts);
                return;
            }
        }
        accounts.add(account);
        saveAll(accounts);
    }

    public void delete(String label) {
        List<SipAccount> accounts = loadAll();
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).label.equalsIgnoreCase(label)) {
                accounts.remove(i);
                break;
            }
        }
        saveAll(accounts);
    }
}
