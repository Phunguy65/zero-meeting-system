package com.example.zeromeeting.core.model.common;

import java.util.Map;

public class JsonNullableMap {
    private boolean present;
    private boolean undefined;
    private Map<String, Object> value;

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

    public Map<String, Object> getValue() {
        return value;
    }

    public void setValue(Map<String, Object> value) {
        this.value = value;
    }
}
