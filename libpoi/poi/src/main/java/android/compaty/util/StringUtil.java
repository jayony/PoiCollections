package com.android.compaty.util;

/**
 * Created by lxg on 2020/4/9.
 */

public class StringUtil {

    public static String toStringSafely(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    public static boolean isNullOrEmpty(final String string) {
        return (string == null || string.trim().length() <= 0);
    }

    public static boolean isNotBlank(final String string) {
        return (string != null && string.trim().length() > 0);
    }

    public static boolean isBlank(final String string) {
        return !isNotBlank(string);
    }

    public static boolean isInteger(final String string) {
        if (isNullOrEmpty(string)) {
            return false;
        }
        String str = string;
        if (string.charAt(0) == '-') {
            if (string.length() <= 1) {
                return false;
            }
            str = string.substring(1, string.length() - 1);
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String join(String delimiter, Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(arg);
        }
        return builder.toString();
    }

    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
    }

    public static boolean isEnglish(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static boolean isChinese(String s) {
        if (isNullOrEmpty(s)) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (isChinese(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
