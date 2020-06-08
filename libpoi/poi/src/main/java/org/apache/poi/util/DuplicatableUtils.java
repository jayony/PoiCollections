package org.apache.poi.util;

import org.apache.poi.common.Duplicatable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lxg on 2020/4/29.
 */
public class DuplicatableUtils {
    public static <T extends Duplicatable> T[] copy(T[] arr) {
        if (arr == null) {
            return null;
        }
        List<Duplicatable> list = new ArrayList<>();
        for (T t : arr) {
            list.add(t.copy());
        }
        return (T[]) list.toArray();
    }

    public static <T extends Duplicatable> List<T> copy(List<T> src) {
        if (src == null) {
            return null;
        }
        List<T> list = new ArrayList<>();
        for (T t : src) {
            list.add((T) t.copy());
        }
        return list;
    }
}
