package com.example.zeromeeting.core.model.user;

import com.google.gson.annotations.SerializedName;

public class SearchUsersRequest {
    @SerializedName("size")
    private Integer size;
    @SerializedName("pageTokenRaw")
    private String pageTokenRaw;
    @SerializedName("queryRaw")
    private String queryRaw;

    // TODO: Alt + Insert tạo Getters/Setters
}
