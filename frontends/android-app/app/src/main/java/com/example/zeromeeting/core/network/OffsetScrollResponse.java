package com.example.zeromeeting.core.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OffsetScrollResponse<T> {
    @SerializedName("content")
    private List<T> content;

    @SerializedName("size")
    private int size;

    @SerializedName("nextOffset")
    private int nextOffset;

    // TODO: Alt + Insert tạo Getters/Setters
}
