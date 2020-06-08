package org.apache.poi.common.usermodel.fonts;

import android.graphics.Typeface;

import java.awt.PoiFont;

/**
 * Created by lxg on 2020/4/14.
 */
public class FontInfo {
    private PoiFont poiFont;

    public FontInfo() {
        poiFont = new PoiFont();
    }

    public FontInfo(PoiFont poiFont) {
        this.poiFont = poiFont;
    }

    public PoiFont getPoiFont() {
        return poiFont;
    }

    public FontInfo setPoiFont(PoiFont poiFont) {
        this.poiFont = poiFont;
        return this;
    }

    public Typeface getTypeface() {
        return getPoiFont().getTypeface();
    }

    public FontInfo setTypeface(Typeface typeface) {
        getPoiFont().setTypeface(typeface);
        return this;
    }

    public float getSize() {
        return getPoiFont().getSize();
    }

    public FontInfo setSize(float size) {
        getPoiFont().setSize(size);
        return this;
    }

    public int getStyle() {
        // TODO - convert Typeface style to Font style
        return getPoiFont().getStyle();
    }

    public String getFamily() {
        return getPoiFont().getFamily();
    }

    public boolean isBold() {
        return getPoiFont().isBold();
    }

    public boolean isItalic() {
        return getPoiFont().isItalic();
    }
}
