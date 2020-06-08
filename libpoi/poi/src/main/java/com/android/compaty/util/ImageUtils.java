package com.android.compaty.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by lxg on 2020/4/16.
 */
public class ImageUtils {
    public static synchronized Bitmap decodeSampledBitmapFromFile(String filename,
                                                                  int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }

    public static BufferedImage read(InputStream inputStream) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        try {
            inputStream.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bm = BitmapFactory.decodeStream(inputStream, null, options);
        return bm == null ? null : BufferedImage.create(bm);
    }

    public static boolean canDecode(InputStream byteArrayInputStream) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(byteArrayInputStream, null, options);
        if (options.outWidth < 0 || options.outHeight < 0) {
            return false;
        }

        return true;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    public static Dimension readImageSize(InputStream inputStream) {
        Bitmap bitmap = null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            return new Dimension(bitmap.getWidth(), bitmap.getHeight());
        } finally {
            CloseUtils.close(inputStream);
            CloseUtils.recycle(bitmap);
        }
    }

    public static String fileBytesShow(float size) {
        long kb = 1024;
        long mb = (kb * 1024);
        long gb = (mb * 1024);
        if (size < kb) {
            return String.format("%d B", (int) size);
        } else if (size < mb) {
            return String.format("%.2f KB", size / kb); // 保留两位小数
        } else if (size < gb) {
            return String.format("%.2f MB", size / mb);
        } else {
            return String.format("%.2f GB", size / gb);
        }
    }
}
