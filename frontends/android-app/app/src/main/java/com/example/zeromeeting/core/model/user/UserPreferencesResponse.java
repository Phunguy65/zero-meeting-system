package com.example.zeromeeting.core.model.user;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class UserPreferencesResponse {
    @SerializedName("settings")
    private Map<String, Object> settings;

    // TODO: Bấm Alt + Insert tạo Getters/Setters
}
