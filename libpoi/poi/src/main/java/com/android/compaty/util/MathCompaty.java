package com.android.compaty.util;

/**
 * Created by lxg on 2020/4/29.
 */
public class MathCompaty {
    public static int toIntExact(long value) {
        if ((int)value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int)value;
    }

    public static double average(double[] arr) {
        if (arr == null || arr.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double v : arr) {
            sum += v;
        }
        return sum / arr.length;
    }
}
