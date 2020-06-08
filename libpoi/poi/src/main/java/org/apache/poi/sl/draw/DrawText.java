package org.apache.poi.sl.draw;

import android.text.StaticLayout;
import android.text.TextPaint;

import java.awt.Color;
import java.awt.PoiFont;

/**
 * Created by lxg on 2020/5/7.
 */
public class DrawText {
    private StaticLayout staticLayout;
    private TextPaint textPaint;
    private CharSequence text;
    private PoiFont poiFont;
    private Color color;
    private boolean empty;

    public StaticLayout getStaticLayout() {
        return staticLayout;
    }

    public DrawText setStaticLayout(StaticLayout staticLayout) {
        this.staticLayout = staticLayout;
        return this;
    }

    public TextPaint getTextPaint() {
        return textPaint;
    }

    public DrawText setTextPaint(TextPaint textPaint) {
        this.textPaint = textPaint;
        return this;
    }

    public CharSequence getText() {
        return text;
    }

    public DrawText setText(CharSequence text) {
        this.text = text;
        return this;
    }

    public PoiFont getPoiFont() {
        return poiFont;
    }

    public DrawText setPoiFont(PoiFont poiFont) {
        this.poiFont = poiFont;
        return this;
    }

    public Color getColor() {
        return color;
    }

    public DrawText setColor(Color color) {
        this.color = color;
        return this;
    }

    public boolean isEmpty() {
        return empty;
    }

    public DrawText setEmpty(boolean empty) {
        this.empty = empty;
        return this;
    }
}
