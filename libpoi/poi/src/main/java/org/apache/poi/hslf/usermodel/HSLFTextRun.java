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

package org.apache.poi.hslf.usermodel;

import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.model.textproperties.BitMaskTextProp;
import org.apache.poi.hslf.model.textproperties.CharFlagsTextProp;
import org.apache.poi.hslf.model.textproperties.TextProp;
import org.apache.poi.hslf.model.textproperties.TextPropCollection;
import org.apache.poi.hslf.model.textproperties.TextPropCollection.TextPropType;
import org.apache.poi.sl.draw.DrawFontInfo;
import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PaintStyle.SolidPaint;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import java.awt.Color;
import java.awt.PoiFont;
import java.util.List;


/**
 * Represents a run of text, all with the same style
 *
 */
@SuppressWarnings({"WeakerAccess", "Duplicates", "unused"})
public final class HSLFTextRun implements TextRun {
	private static final POILogger logger = POILogFactory.getLogger(HSLFTextRun.class);

	/** The TextRun we belong to */
	private HSLFTextParagraph parentParagraph;
	private String _runText = "";
	/** Caches the font info objects until the text runs are attached to the container */
	private HSLFFontInfo[] cachedFontInfo;
	private HSLFHyperlink link;
	protected PoiFont poiFont = new PoiFont();
	/**
	 * Our paragraph and character style.
	 * Note - we may share these styles with other RichTextRuns
	 */
	private TextPropCollection characterStyle = new TextPropCollection(1, TextPropType.character);

	/**
	 * Create a new wrapper around a rich text string
	 * @param parentParagraph the parent paragraph
	 */
	public HSLFTextRun(HSLFTextParagraph parentParagraph) {
		this.parentParagraph = parentParagraph;
	}

	public TextPropCollection getCharacterStyle() {
	    return characterStyle;
	}

	public void setCharacterStyle(TextPropCollection characterStyle) {
	    this.characterStyle = characterStyle.copy();
	    this.characterStyle.updateTextSize(_runText.length());
	}

	/**
	 * Supply the SlideShow we belong to
	 */
	public void updateSheet() {
	}

	/**
	 * Get the length of the text
	 */
	public int getLength() {
		return _runText.length();
	}

	/**
	 * Fetch the text, in raw storage form
	 */
	@Override
    public String getRawText() {
		return _runText;
	}

	/**
	 * Change the text
	 */
	@Override
    public void setText(String text) {
	    if (text == null) {
	        throw new HSLFException("text must not be null");
	    }
	    String newText = HSLFTextParagraph.toInternalString(text);
	    if (!newText.equals(_runText)) {
	        _runText = newText;
	        if (HSLFSlideShow.getLoadSavePhase() == HSLFSlideShow.LoadSavePhase.LOADED) {
	            parentParagraph.setDirty();
	        }
	    }
	}

	// --------------- Internal helpers on rich text properties -------

	/**
	 * Fetch the value of the given flag in the CharFlagsTextProp.
	 * Returns false if the CharFlagsTextProp isn't present, since the
	 *  text property won't be set if there's no CharFlagsTextProp.
	 */
	private boolean isCharFlagsTextPropVal(int index) {
		return getFlag(index);
	}

	boolean getFlag(int index) {
		BitMaskTextProp prop = (characterStyle == null) ? null : characterStyle.findByName(CharFlagsTextProp.NAME);

		if (prop == null || !prop.getSubPropMatches()[index]) {
		    prop = getMasterProp();
		}

		return prop != null && prop.getSubValue(index);
	}

	private <T extends TextProp> T getMasterProp() {
        final int txtype = parentParagraph.getRunType();
        final HSLFSheet sheet = parentParagraph.getSheet();
        if (sheet == null) {
            logger.log(POILogger.ERROR, "Sheet is not available");
            return null;
        }

        final HSLFMasterSheet master = sheet.getMasterSheet();
        if (master == null) {
            logger.log(POILogger.WARN, "MasterSheet is not available");
            return null;
        }

        String name = CharFlagsTextProp.NAME;
        final TextPropCollection col = master.getPropCollection(txtype, parentParagraph.getIndentLevel(), name, true);
        return (col == null) ? null : col.findByName(name);
	}


	/**
	 * Set the value of the given flag in the CharFlagsTextProp, adding
	 *  it if required.
	 */
	private void setCharFlagsTextPropVal(int index, boolean value) {
	    // TODO: check if paragraph/chars can be handled the same ...
		if (getFlag(index) != value) {
		    setFlag(index, value);
		    parentParagraph.setDirty();
		}
	}

	/**
	 * Sets the value of the given Paragraph TextProp, add if required
	 * @param propName The name of the Paragraph TextProp
	 * @param val The value to set for the TextProp
	 */
	public void setCharTextPropVal(String propName, Integer val) {
	    getTextParagraph().setPropVal(characterStyle, propName, val);
	}


	// --------------- Friendly getters / setters on rich text properties -------

	@Override
	public boolean isBold() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.BOLD_IDX);
	}

	@Override
	public void setBold(boolean bold) {
		setCharFlagsTextPropVal(CharFlagsTextProp.BOLD_IDX, bold);
	}

	@Override
	public boolean isItalic() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.ITALIC_IDX);
	}

	@Override
	public void setItalic(boolean italic) {
		setCharFlagsTextPropVal(CharFlagsTextProp.ITALIC_IDX, italic);
	}

	@Override
	public boolean isUnderlined() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.UNDERLINE_IDX);
	}

	@Override
	public void setUnderlined(boolean underlined) {
		setCharFlagsTextPropVal(CharFlagsTextProp.UNDERLINE_IDX, underlined);
	}

	/**
	 * Does the text have a shadow?
	 */
	public boolean isShadowed() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.SHADOW_IDX);
	}

	/**
	 * Does the text have a shadow?
	 */
	public void setShadowed(boolean flag) {
		setCharFlagsTextPropVal(CharFlagsTextProp.SHADOW_IDX, flag);
	}

	/**
	 * Is this text embossed?
	 */
	 public boolean isEmbossed() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.RELIEF_IDX);
	}

	/**
	 * Is this text embossed?
	 */
	 public void setEmbossed(boolean flag) {
		setCharFlagsTextPropVal(CharFlagsTextProp.RELIEF_IDX, flag);
	}

	@Override
	public boolean isStrikethrough() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.STRIKETHROUGH_IDX);
	}

	@Override
	public void setStrikethrough(boolean flag) {
		setCharFlagsTextPropVal(CharFlagsTextProp.STRIKETHROUGH_IDX, flag);
	}

	/**
	 * Gets the subscript/superscript option
	 *
	 * @return the percentage of the font size. If the value is positive, it is superscript, otherwise it is subscript
	 */
	public int getSuperscript() {
		TextProp tp = getTextParagraph().getPropVal(characterStyle, "superscript");
		return tp == null ? 0 : tp.getValue();
	}

	/**
	 * Sets the subscript/superscript option
	 *
	 * @param val the percentage of the font size. If the value is positive, it is superscript, otherwise it is subscript
	 */
	public void setSuperscript(int val) {
	    setCharTextPropVal("superscript", val);
	}

    @Override
	public Double getFontSize() {
		updateFontSize();
		return Double.valueOf(poiFont.getSize());
	}


	@Override
	public void setFontSize(Double fontSize) {
		poiFont.setSize(fontSize == null ? -1 : fontSize.floatValue());
		Integer iFontSize = (fontSize == null) ? null : fontSize.intValue();
		setCharTextPropVal("font.size", iFontSize);
	}

	@Override
	public void setPoiFont(PoiFont poiFont) {
		this.poiFont = poiFont;
	}

    @Override
	public PoiFont getPoiFont() {
		updateFontSize();
        return poiFont;
	}

	private void updateFontSize() {
		TextProp tp = parentParagraph.getPropVal(characterStyle, "font.size");
		poiFont.setSize(tp == null ? -1 : tp.getValue());
	}

	@Override
	public FontInfo getFontInfo() {
		return new DrawFontInfo(getPoiFont());
	}

	/**
	 * @return font color as PaintStyle
	 */
	@Override
	public SolidPaint getFontColor() {
		TextProp tp = getTextParagraph().getPropVal(characterStyle, "font.color");
		if (tp == null) {
            return null;
        }
		Color color = HSLFTextParagraph.getColorFromColorIndexStruct(tp.getValue(), parentParagraph.getSheet());
        return DrawPaint.createSolidPaint(color);
	}

	/**
	 * Sets color of the text, as a int bgr.
	 * (PowerPoint stores as BlueGreenRed, not the more
	 *  usual RedGreenBlue)
	 * @see java.awt.Color
	 */
	public void setFontColor(int bgr) {
		setCharTextPropVal("font.color", bgr);
	}


    @Override
    public void setFontColor(Color color) {
        setFontColor(DrawPaint.createSolidPaint(color));
    }

	@Override
	public void setFontColor(PaintStyle color) {
	    if (!(color instanceof SolidPaint)) {
	        throw new IllegalArgumentException("HSLF only supports solid paint");
	    }
		// In PowerPont RGB bytes are swapped, as BGR
	    SolidPaint sp = (SolidPaint)color;
	    Color c = DrawPaint.applyColorTransform(sp.getSolidColor());
		int rgb = new Color(c.getBlue(), c.getGreen(), c.getRed(), 254).getRGB();
		setFontColor(rgb);
	}

    private void setFlag(int index, boolean value) {
        BitMaskTextProp prop = characterStyle.addWithName(CharFlagsTextProp.NAME);
        prop.setSubValue(value, index);
    }

    public HSLFTextParagraph getTextParagraph() {
        return parentParagraph;
    }

    @Override
    public TextCap getTextCap() {
        return TextCap.NONE;
    }

    @Override
    public boolean isSubscript() {
        return getSuperscript() < 0;
    }

    @Override
    public boolean isSuperscript() {
        return getSuperscript() > 0;
    }

    @Override
    public byte getPitchAndFamily() {
        return 0;
    }

    /**
     * Sets the hyperlink - used when parsing the document
     *
     * @param link the hyperlink
     */
    /* package */ void setHyperlink(HSLFHyperlink link) {
        this.link = link;
    }

    @Override
    public HSLFHyperlink getHyperlink() {
        return link;
    }

    @Override
    public HSLFHyperlink createHyperlink() {
        if (link == null) {
            link = HSLFHyperlink.createHyperlink(this);
            parentParagraph.setDirty();
        }
        return link;
    }

    @Override
    public FieldType getFieldType() {
        HSLFTextShape ts = getTextParagraph().getParentShape();
        Placeholder ph = ts.getPlaceholder();

        if (ph != null) {
            switch (ph) {
            case SLIDE_NUMBER:
                return FieldType.SLIDE_NUMBER;
            case DATETIME:
                return FieldType.DATE_TIME;
            default:
                break;
            }
        }

        if (ts.getSheet() instanceof MasterSheet) {
            TextShape<?,? extends TextParagraph<?,?,? extends TextRun>> ms = ts.getMetroShape();
            if (ms == null || ms.getTextParagraphs().isEmpty()) {
                return null;
            }
            List<? extends TextRun> trList = ms.getTextParagraphs().get(0).getTextRuns();
            if (trList.isEmpty()) {
                return null;
            }
            return trList.get(0).getFieldType();
        }

        return null;
    }

	@Override
	public HSLFTextParagraph getParagraph() {
		return parentParagraph;
	}
}
