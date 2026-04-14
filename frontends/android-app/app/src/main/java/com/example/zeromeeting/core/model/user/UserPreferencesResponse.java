package com.example.zeromeeting.core.model.user;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class UserPreferencesResponse {
    @SerializedName("settings")
    private Map<String, Object> settings;

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
}
