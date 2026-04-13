package com.example.zeromeeting.core.model.chat;
import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    @SerializedName("id")
    private String id;

    @SerializedName("seqNum")
    private long seqNum;

    @SerializedName("roomId")
    private String roomId;

    @SerializedName("senderId")
    private String senderId;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("content")
    private String content;

    @SerializedName("type")
    private String type;

    @SerializedName("createdAt")
    private String createdAt;

    // TODO: Alt + Insert tạo Getters/Setters
}
