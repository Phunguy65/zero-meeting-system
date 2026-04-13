package com.example.zeromeeting.core.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CursorScrollResponse<T> {
    @SerializedName("content")
    private List<T> content;

    @SerializedName("size")
    private int size;

    @SerializedName("nextPageToken")
    private String nextPageToken;

    // TODO: Alt + Insert tạo Getters/Setters
}
