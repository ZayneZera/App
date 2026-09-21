package com.zayne.seedfilter.gui;

/**
 * Shared dark-mode palette for the in-game seed-filter menu. Reworked for actual layering:
 * the panel sits darkest, widgets (checkboxes/sliders) get a visibly lighter fill so they read
 * as distinct controls, and borders are a real mid-gray instead of pure black - black-on-#151515
 * has almost no contrast and made every border disappear.
 */
public class DarkTheme {
    public static final int PANEL = 0xFF191919;
    public static final int BORDER = 0xFF4A4A4A;
    public static final int WIDGET_BG = 0xFF2E2E2E;
    public static final int WIDGET_BORDER = 0xFF5A5A5A;
    public static final int TEXT = 0xFFECEAE3;
    public static final int TEXT_DIM = 0xFF9C9C9C;
    public static final int ACCENT = 0xFF5AD65A;
    public static final int HEADING = 0xFF5AD65A;

    private DarkTheme() {
    }
}
