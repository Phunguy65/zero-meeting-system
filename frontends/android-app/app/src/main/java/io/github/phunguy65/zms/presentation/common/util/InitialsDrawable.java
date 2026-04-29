package io.github.phunguy65.zms.presentation.common.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A drawable that displays user initials on a colored circular background.
 *
 * <p>Used as a fallback avatar when the user has no custom avatar image.
 * The background color is deterministic based on the user's ID to ensure
 * consistency across sessions.
 */
public class InitialsDrawable extends Drawable {

    private static final int[] PALETTE = {
        0xFF1976D2, // Blue
        0xFF388E3C, // Green
        0xFFD32F2F, // Red
        0xFF7B1FA2, // Purple
        0xFFF57C00, // Orange
        0xFF00796B, // Teal
        0xFF5D4037, // Brown
        0xFF455A64, // Blue Grey
        0xFFC2185B, // Pink
        0xFF0097A7, // Cyan
    };

    private final Paint backgroundPaint;
    private final Paint textPaint;
    private final String initials;

    /**
     * Creates an InitialsDrawable for the given name.
     *
     * @param fullName the user's full name
     * @param userId the user's ID (used for deterministic color selection)
     */
    public InitialsDrawable(String fullName, String userId) {
        this.initials = extractInitials(fullName);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(selectColor(userId));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Extracts initials from a full name.
     *
     * <p>Examples:
     * <ul>
     *   <li>"Jane Doe" → "JD"</li>
     *   <li>"John" → "J"</li>
     *   <li>"Alice Bob Charles" → "AC"</li>
     *   <li>"" or null → "?"</li>
     * </ul>
     *
     * @param fullName the full name
     * @return 1-2 character initials
     */
    private static String extractInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }

        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        if (parts.length >= 1 && !parts[0].isEmpty()) {
            initials.append(Character.toUpperCase(parts[0].charAt(0)));
        }
        if (parts.length >= 2 && !parts[parts.length - 1].isEmpty()) {
            initials.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }

        return initials.length() > 0 ? initials.toString() : "?";
    }

    /**
     * Selects a deterministic color based on the user ID.
     *
     * @param userId the user's ID
     * @return a color from the palette
     */
    private static int selectColor(String userId) {
        if (userId == null || userId.isEmpty()) {
            return PALETTE[0];
        }
        int hash = Math.abs(userId.hashCode());
        return PALETTE[hash % PALETTE.length];
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float cx = bounds.centerX();
        float cy = bounds.centerY();
        float radius = Math.min(bounds.width(), bounds.height()) / 2f;

        canvas.drawCircle(cx, cy, radius, backgroundPaint);

        textPaint.setTextSize(radius * 0.9f);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(initials, cx, textY, textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        backgroundPaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        backgroundPaint.setColorFilter(colorFilter);
        textPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
