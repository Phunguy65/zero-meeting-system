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

    public String getPageTokenRaw() {
        return pageTokenRaw;
    }

    public void setPageTokenRaw(String pageTokenRaw) {
        this.pageTokenRaw = pageTokenRaw;
    }

    public String getQueryRaw() {
        return queryRaw;
    }

    public void setQueryRaw(String queryRaw) {
        this.queryRaw = queryRaw;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
