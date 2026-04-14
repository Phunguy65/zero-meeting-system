package com.example.zeromeeting.core.model.auth;
import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("username")
    private String username;

    public RegisterRequest(String email, String password, String fullName, String username) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.username = username;
    }
    // TODO: Bấm Alt + Insert tạo Getters/Setters

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
