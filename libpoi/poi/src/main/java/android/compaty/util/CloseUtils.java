package com.android.compaty.util;

import android.graphics.Bitmap;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by lxg on 2020/4/16.
 */
public class CloseUtils {
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
