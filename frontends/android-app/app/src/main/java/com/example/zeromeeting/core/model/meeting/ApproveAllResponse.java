package com.example.zeromeeting.core.model.meeting;
import com.google.gson.annotations.SerializedName;

public class ApproveAllResponse {
    @SerializedName("approvedCount")
    private int approvedCount;

    // TODO: Alt + Insert tạo Getters/Setters

    public int getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(int approvedCount) {
        this.approvedCount = approvedCount;
    }
}
