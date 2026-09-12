package com.nyn.dialer;

import org.json.JSONException;
import org.json.JSONObject;

public class SipAccount {
    public String label;      // friendly name, e.g. "Work" or "Amarip"
    public String username;
    public String domain;
    public String password;

    public SipAccount(String label, String username, String domain, String password) {
        this.label = label;
        this.username = username;
        this.domain = domain;
        this.password = password;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("label", label);
        o.put("username", username);
        o.put("domain", domain);
        o.put("password", password);
        return o;
    }

    public static SipAccount fromJson(JSONObject o) throws JSONException {
        return new SipAccount(
                o.getString("label"),
                o.getString("username"),
                o.getString("domain"),
                o.optString("password", "")
        );
    }

    @Override
    public String toString() {
        return label + " (" + username + "@" + domain + ")";
    }
}
