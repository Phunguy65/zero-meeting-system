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

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}
