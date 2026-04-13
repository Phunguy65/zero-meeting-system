package io.github.phunguy65.zms.presentation.common;

import java.util.Objects;

/**
 * Represents a language option in the language picker.
 * Immutable data class holding language code and display names.
 */
public final class LanguageItem {

    private final String code;
    private final String nativeName;
    private final String label;

    /**
     * Creates a new LanguageItem.
     *
     * @param code       BCP 47 language tag (e.g., "en", "vi")
     * @param nativeName Language name in its native form (e.g., "English", "Tiếng Việt")
     * @param label      Full label with translation (e.g., "English (English)", "Tiếng Việt (Vietnamese)")
     */
    public LanguageItem(String code, String nativeName, String label) {
        this.code = code;
        this.nativeName = nativeName;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getNativeName() {
        return nativeName;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanguageItem that = (LanguageItem) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "LanguageItem{" + "code='"
                + code + '\'' + ", nativeName='"
                + nativeName + '\'' + ", label='"
                + label + '\'' + '}';
    }
}
