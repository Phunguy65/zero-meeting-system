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

    public int getNextOffset() {
        return nextOffset;
    }

    public void setNextOffset(int nextOffset) {
        this.nextOffset = nextOffset;
    }
}
