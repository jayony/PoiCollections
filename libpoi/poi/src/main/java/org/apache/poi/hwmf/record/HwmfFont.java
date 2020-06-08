/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hwmf.record;

import com.android.compaty.util.function.Supplier;

import org.apache.poi.common.usermodel.GenericRecord;
import org.apache.poi.common.usermodel.fonts.FontFamily;
import org.apache.poi.common.usermodel.fonts.FontHeader;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.common.usermodel.fonts.FontPitch;
import org.apache.poi.ss.usermodel.FontCharset;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.GenericRecordJsonWriter;
import org.apache.poi.util.GenericRecordUtil;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LittleEndianInputStream;

import java.awt.PoiFont;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The Font object specifies the attributes of a logical font
 */
@SuppressWarnings({"unused", "Duplicates"})
public class HwmfFont extends FontInfo {
    protected final WmfClipPrecision clipPrecision = new WmfClipPrecision();
    protected WmfFontQuality quality;
    protected double height;
    protected int width;
    protected int escapement;
    protected int orientation;
    protected int weight;
    protected int pitchAndFamily;
    protected boolean italic;
    protected boolean underline;
    protected boolean strikeOut;
    protected FontCharset charSet;
    protected WmfOutPrecision outPrecision;
    protected String facename;

    public HwmfFont() {
        super(new PoiFont());
    }

    public HwmfFont(PoiFont poiFont) {
        super(poiFont);
    }

    public int init(LittleEndianInputStream leis, long recordSize) throws IOException {
        height = leis.readShort();
        width = leis.readShort();
        escapement = leis.readShort();
        orientation = leis.readShort();
        weight = leis.readShort();
        italic = leis.readByte() != 0;
        underline = leis.readByte() != 0;
        strikeOut = leis.readByte() != 0;
        charSet = FontCharset.valueOf(leis.readUByte());
        outPrecision = WmfOutPrecision.valueOf(leis.readUByte());
        clipPrecision.init(leis);
        quality = WmfFontQuality.valueOf(leis.readUByte());
        pitchAndFamily = leis.readUByte();

        StringBuilder sb = new StringBuilder();
        int readBytes = readString(leis, sb, 32);
        if (readBytes == -1) {
            throw new IOException("Font facename can't be determined.");
        }
        facename = sb.toString();

        return 5*LittleEndianConsts.SHORT_SIZE+8*LittleEndianConsts.BYTE_SIZE+readBytes;
    }

    public void initDefaults() {
        height = -12;
        width = 0;
        escapement = 0;
        weight = FontHeader.REGULAR_WEIGHT;
        italic = false;
        underline = false;
        strikeOut = false;
        charSet = FontCharset.ANSI;
        outPrecision = WmfOutPrecision.OUT_DEFAULT_PRECIS;
        quality = WmfFontQuality.ANTIALIASED_QUALITY;
        pitchAndFamily = FontFamily.FF_DONTCARE.getFlag() | (FontPitch.DEFAULT.getNativeId() << 6);
        facename = "SansSerif";
    }

    protected int readString(LittleEndianInputStream leis, StringBuilder sb, int limit) throws IOException {
        byte[] buf = new byte[limit];
        byte b;
        byte readBytes = 0;
        do {
            if (readBytes == limit) {
                return -1;
            }

            buf[readBytes++] = b = leis.readByte();
        } while (b != 0 && b != -1 && readBytes <= limit);

        sb.append(new String(buf, 0, readBytes-1, StandardCharsets.ISO_8859_1));

        return readBytes;
    }

    public double getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getEscapement() {
        return escapement;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public boolean isStrikeOut() {
        return strikeOut;
    }

    public enum WmfOutPrecision {
        /**
         * A value that specifies default behavior.
         */
        OUT_DEFAULT_PRECIS(0x00000000),
        /**
         * A value that is returned when rasterized fonts are enumerated.
         */
        OUT_STRING_PRECIS(0x00000001),
        /**
         * A value that is returned when TrueType and other outline fonts, and
         * vector fonts are enumerated.
         */
        OUT_STROKE_PRECIS(0x00000003),
        /**
         * A value that specifies the choice of a TrueType font when the system
         * contains multiple fonts with the same name.
         */
        OUT_TT_PRECIS(0x00000004),
        /**
         * A value that specifies the choice of a device font when the system
         * contains multiple fonts with the same name.
         */
        OUT_DEVICE_PRECIS(0x00000005),
        /**
         * A value that specifies the choice of a rasterized font when the system
         * contains multiple fonts with the same name.
         */
        OUT_RASTER_PRECIS(0x00000006),
        /**
         * A value that specifies the requirement for only TrueType fonts. If
         * there are no TrueType fonts installed in the system, default behavior is specified.
         */
        OUT_TT_ONLY_PRECIS(0x00000007),
        /**
         * A value that specifies the requirement for TrueType and other outline fonts.
         */
        OUT_OUTLINE_PRECIS (0x00000008),
        /**
         * A value that specifies a preference for TrueType and other outline fonts.
         */
        OUT_SCREEN_OUTLINE_PRECIS (0x00000009),
        /**
         * A value that specifies a requirement for only PostScript fonts. If there
         * are no PostScript fonts installed in the system, default behavior is specified.
         */
        OUT_PS_ONLY_PRECIS (0x0000000A);


        int flag;
        WmfOutPrecision(int flag) {
            this.flag = flag;
        }

        public static WmfOutPrecision valueOf(int flag) {
            for (WmfOutPrecision op : values()) {
                if (op.flag == flag) {
                    return op;
                }
            }
            return null;
        }
    }

    public static class WmfClipPrecision implements GenericRecord {

        /** Specifies that default clipping MUST be used. */
        private static final BitField DEFAULT_PRECIS = BitFieldFactory.getInstance(0x0003);


        /** This value SHOULD NOT be used. */
        private static final BitField CHARACTER_PRECIS = BitFieldFactory.getInstance(0x0001);

        /** This value MAY be returned when enumerating rasterized, TrueType and vector fonts. */
        private static final BitField STROKE_PRECIS = BitFieldFactory.getInstance(0x0002);

        /**
         * This value is used to control font rotation, as follows:
         * If set, the rotation for all fonts SHOULD be determined by the orientation of the coordinate system;
         * that is, whether the orientation is left-handed or right-handed.
         *
         * If clear, device fonts SHOULD rotate counterclockwise, but the rotation of other fonts
         * SHOULD be determined by the orientation of the coordinate system.
         */
        private static final BitField LH_ANGLES = BitFieldFactory.getInstance(0x0010);

        /** This value SHOULD NOT be used. */
        private static final BitField TT_ALWAYS = BitFieldFactory.getInstance(0x0020);

        /** This value specifies that font association SHOULD be turned off. */
        private static final BitField DFA_DISABLE = BitFieldFactory.getInstance(0x0040);

        /**
         * This value specifies that font embedding MUST be used to render document content;
         * embedded fonts are read-only.
         */
        private static final BitField EMBEDDED = BitFieldFactory.getInstance(0x0080);

        private static final int[] FLAG_MASKS = {
                0x0001, 0x0002, 0x0010, 0x0020, 0x0040, 0x0080
        };

        private static final String[] FLAG_NAMES = {
                "CHARACTER_PRECIS",
                "STROKE_PRECIS",
                "LH_ANGLES",
                "TT_ALWAYS",
                "DFA_DISABLE",
                "EMBEDDED"
        };

        private int flag;

        public int init(LittleEndianInputStream leis) {
            flag = leis.readUByte();
            return LittleEndianConsts.BYTE_SIZE;
        }

        public boolean isDefaultPrecision() {
            return !DEFAULT_PRECIS.isSet(flag);
        }

        public boolean isCharacterPrecision() {
            return CHARACTER_PRECIS.isSet(flag);
        }

        public boolean isStrokePrecision() {
            return STROKE_PRECIS.isSet(flag);
        }

        public boolean isLeftHandAngles() {
            return LH_ANGLES.isSet(flag);
        }

        public boolean isTrueTypeAlways() {
            return TT_ALWAYS.isSet(flag);
        }

        public boolean isFontAssociated() {
            return !DFA_DISABLE.isSet(flag);
        }

        public boolean useEmbeddedFont() {
            return EMBEDDED.isSet(flag);
        }

        @Override
        public String toString() {
            return GenericRecordJsonWriter.marshal(this);
        }

        @Override
        public Map<String, Supplier<?>> getGenericProperties() {
            return GenericRecordUtil.getGenericProperties(
                    "isDefaultPrecision", this::isDefaultPrecision,
                    "flag", GenericRecordUtil.getBitsAsString(() -> flag, FLAG_MASKS, FLAG_NAMES)
            );
        }
    }

    public enum WmfFontQuality {
        /**
         * Specifies that the character quality of the font does not matter, so DRAFT_QUALITY can be used.
         */
        DEFAULT_QUALITY (0x00),

        /**
         * Specifies that the character quality of the font is less important than the
         * matching of logical attribuetes. For rasterized fonts, scaling SHOULD be enabled, which
         * means that more font sizes are available.
         */
        DRAFT_QUALITY (0x01),

        /**
         * Specifies that the character quality of the font is more important than the
         * matching of logical attributes. For rasterized fonts, scaling SHOULD be disabled, and the font
         * closest in size SHOULD be chosen.
         */
        PROOF_QUALITY (0x02),

        /**
         * Specifies that anti-aliasing SHOULD NOT be used when rendering text.
         */
        NONANTIALIASED_QUALITY (0x03),

        /**
         * Specifies that anti-aliasing SHOULD be used when rendering text, if the font supports it.
         */
        ANTIALIASED_QUALITY (0x04),

        /**
         * Specifies that ClearType anti-aliasing SHOULD be used when rendering text, if the font supports it.
         *
         * Fonts that do not support ClearType anti-aliasing include type 1 fonts, PostScript fonts,
         * OpenType fonts without TrueType outlines, rasterized fonts, vector fonts, and device fonts.
         */
        CLEARTYPE_QUALITY (0x05);

        int flag;
        WmfFontQuality(int flag) {
            this.flag = flag;
        }

        public static WmfFontQuality valueOf(int flag) {
            for (WmfFontQuality fq : values()) {
                if (fq.flag == flag) {
                    return fq;
                }
            }
            return null;
        }
    }
}
