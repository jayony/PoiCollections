package com.android.compaty.util;

/**
 * Created by lxg on 2020/5/14.
 */
public class FontSizeUtil {
    public static float winToAndForEn(float winFontSize) {
        float scale;
        if (winFontSize <= 12) {
            return winFontSize;
        }
        if (winFontSize < 18) {
            scale = 0.95f;
        } else if (winFontSize >= 18 && winFontSize <= 24) {
            scale = 0.92f;
        } else {
            scale = 0.91f;
        }
        return Math.round(winFontSize * scale);
    }
}
