package io.github.phunguy65.zms.domain.model;

/**
 * Represents the video layout mode for participant arrangement in a video call.
 *
 * <p>Layout modes determine how participant video tiles are displayed on the call surface.
 * This is a client-side preference stored in ViewModel state.
 */
public enum VideoLayout {
    /**
     * Automatic layout that dynamically adjusts grid based on participant count.
     * Uses 1 column for 1 participant, 2 columns for 2-4, 3 columns for 5+.
     */
    AUTO,

    /**
     * Fixed two-column tiled layout regardless of participant count.
     * Provides consistent grid arrangement.
     */
    TILED,

    /**
     * Spotlight layout that emphasizes the active speaker.
     * Phase 1 implementation uses enhanced grid with speaker prominence.
     */
    SPOTLIGHT,

    /**
     * Sidebar layout with main speaker view and participant strip.
     * Phase 1 implementation uses grid fallback with layout selection feedback.
     */
    SIDEBAR
}
