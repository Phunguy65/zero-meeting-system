package com.example.zeromeeting.core.model.common;

public class JsonNullableString {
    private boolean present;
    private boolean undefined;
    private String value; // Bạn có thể thêm trường này để lưu giá trị thực

    // TODO: Alt + Insert tạo Getters/Setters

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public boolean isUndefined() {
        return undefined;
    }

    public void setUndefined(boolean undefined) {
        this.undefined = undefined;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
