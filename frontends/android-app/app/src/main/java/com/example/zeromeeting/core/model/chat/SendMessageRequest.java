package com.example.zeromeeting.core.model.chat;
import com.google.gson.annotations.SerializedName;

public class SendMessageRequest {
    @SerializedName("senderName")
    private String senderName;

    @SerializedName("content")
    private String content;

    @SerializedName("replyToSeqNum")
    private Long replyToSeqNum; // Dùng Long thay vì int để có thể null nếu không phải là tin nhắn reply

    public SendMessageRequest(String senderName, String content) {
        this.senderName = senderName;
        this.content = content;
    }

    // TODO: Alt + Insert tạo Getters/Setters
}
