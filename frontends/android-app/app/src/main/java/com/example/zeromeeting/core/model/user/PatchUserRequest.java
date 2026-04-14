package com.example.zeromeeting.core.model.user;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class PatchUserRequest {
    private String fullName;
    private String avatarUrl;
    private String username;
    private Map<String, Object> preferences;

    // TODO: Alt + Insert tạo Getters/Setters

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }
}
