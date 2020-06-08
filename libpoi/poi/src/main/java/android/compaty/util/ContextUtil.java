package com.android.compaty.util;

import android.content.Context;

/**
 * Created by lxg on 2020/5/25.
 */
public class ContextUtil {

    private static Context context;

    public static void initContext(Context context) {
        ContextUtil.context = context.getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
