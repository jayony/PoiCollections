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

package org.apache.poi.sl.draw;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.UnderlineSpan;

import com.android.compaty.util.FontSizeUtil;
import com.android.compaty.util.StringUtil;

import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.sl.usermodel.AutoNumberingScheme;
import org.apache.poi.sl.usermodel.Hyperlink;
import org.apache.poi.sl.usermodel.Insets2D;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.Sheet;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextParagraph.TextAlign;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.sl.usermodel.TextRun.FieldType;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.sl.usermodel.TextShape.TextDirection;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.PoiFont;
import java.awt.PoiPaint;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.io.InvalidObjectException;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DrawTextParagraph implements Drawable {
    private static final POILogger LOG = POILogFactory.getLogger(DrawTextParagraph.class);

    /** Keys for passing hyperlinks to the graphics context */
    public static final XlinkAttribute HYPERLINK_HREF = new XlinkAttribute("href");
    public static final XlinkAttribute HYPERLINK_LABEL = new XlinkAttribute("label");

    protected TextParagraph<?,?,?> paragraph;
    double x, y;
    protected List<DrawTextFragment> lines = new ArrayList<>();
    protected String rawText;
    protected DrawTextFragment bullet;
    protected int autoNbrIdx;

    /**
     * the highest line in this paragraph. Used for line spacing.
     */
    protected double maxLineHeight;
    private boolean calculateHeight;

    /**
     * Defines an attribute used for storing the hyperlink associated with
     * some renderable text.
     */
    private static class XlinkAttribute extends Attribute {

        XlinkAttribute(String name) {
            super(name);
        }

        /**
         * Resolves instances being deserialized to the predefined constants.
         */
        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (HYPERLINK_HREF.getName().equals(getName())) {
                return HYPERLINK_HREF;
            }
            if (HYPERLINK_LABEL.getName().equals(getName())) {
                return HYPERLINK_LABEL;
            }
            throw new InvalidObjectException("unknown attribute name");
        }
    }


    public DrawTextParagraph(TextParagraph<?,?,?> paragraph) {
        this.paragraph = paragraph;
    }

    public void setPosition(double x, double y) {
        // TODO: replace it, by applyTransform????
        this.x = x;
        this.y = y;
    }

    public double getY() {
        return y;
    }

    public DrawTextParagraph setCalculateHeight(boolean calculateHeight) {
        this.calculateHeight = calculateHeight;
        return this;
    }

    /**
     * Sets the auto numbering index of the handled paragraph
     * @param index the auto numbering index
     */
    public void setAutoNumberingIdx(int index) {
        autoNbrIdx = index;
    }

    @Override
    public void draw(Graphics2D graphics){
        if (lines.isEmpty()) {
            return;
        }

        double penY = y;

        boolean firstLine = true;
        int indentLevel = paragraph.getIndentLevel();
        Double leftMargin = paragraph.getLeftMargin();
        if (leftMargin == null) {
            // if the marL attribute is omitted, then a value of 347663 is implied
            leftMargin = Units.toPoints(347663L*indentLevel);
        }
        Double indent = paragraph.getIndent();
        if (indent == null) {
            indent = Units.toPoints(347663L*indentLevel);
        }
        if (isHSLF()) {
            // special handling for HSLF
            indent -= leftMargin;
        }

        //        Double rightMargin = paragraph.getRightMargin();
        //        if (rightMargin == null) {
        //            rightMargin = 0d;
        //        }

        //The vertical line spacing
        Double spacing = paragraph.getLineSpacing();
        if (spacing == null) {
            spacing = 100d;
        }

        for(DrawTextFragment line : lines){
            double penX;

            if(firstLine) {
                if (!isEmptyParagraph()) {
                    // TODO: find out character style for empty, but bulleted/numbered lines
                    bullet = getBullet(graphics, line);
                }

                if (bullet != null){
                    double y = penY + (line.getHeight() - bullet.getHeight()) / 2;
                    bullet.setPosition(x+leftMargin+indent, y);
                    bullet.draw(graphics);
                    // don't let text overlay the bullet and advance by the bullet width
                    double bulletWidth = bullet.getWidth() + 1;
                    penX = x + Math.max(leftMargin, leftMargin+indent+bulletWidth);
                } else {
                    penX = x + leftMargin;
                }
            } else {
                penX = x + leftMargin;
            }

            Rectangle2D anchor = DrawShape.getAnchor(graphics, paragraph.getParentShape());
            // Insets are already applied on DrawTextShape.drawContent
            // but (outer) anchor need to be adjusted
            Insets2D insets = paragraph.getParentShape().getInsets();
            double leftInset = insets.left;
            double rightInset = insets.right;

            TextAlign ta = paragraph.getTextAlign();
            if (ta == null) {
                ta = TextAlign.LEFT;
            }
            switch (ta) {
                case CENTER:
                    penX += (anchor.getWidth() - line.getWidth() - leftInset - rightInset - leftMargin) / 2;
                    break;
                case RIGHT:
                    penX += (anchor.getWidth() - line.getWidth() - leftInset - rightInset);
                    break;
                default:
                    break;
            }

            line.setPosition(penX, penY);
            if (!calculateHeight) {
                line.draw(graphics);
            }

            if(spacing > 0) {
                // If linespacing >= 0, then linespacing is a percentage of normal line height.
                penY += spacing*0.01* line.getHeight();
            } else {
                // negative value means absolute spacing in points
                penY += -spacing;
            }

            firstLine = false;
        }

        y = penY - y;
    }

    public float getFirstLineLeading() {
        return 0;//(lines.isEmpty()) ? 0 : lines.get(0).getLeading();
    }

    public float getFirstLineHeight() {
        return (lines.isEmpty()) ? 0 : lines.get(0).getHeightWithLeading();
    }

    public float getLastLineHeight() {
        return (lines.isEmpty()) ? 0 : lines.get(lines.size()-1).getHeight();
    }

    public boolean isEmptyParagraph() {
        return (lines.isEmpty() || rawText.trim().isEmpty());
    }

    @Override
    public void applyTransform(Graphics2D graphics) {
    }

    @Override
    public void drawContent(Graphics2D graphics) {
    }

    /**
     * break text into lines, each representing a line of text that fits in the wrapping width
     *
     * @param graphics The drawing context for computing text-lengths.
     */
    protected void breakText(Graphics2D graphics){
        lines.clear();

        DrawFactory fact = DrawFactory.getInstance(graphics);
        StringBuilder text = new StringBuilder();
        DrawText drawText = getDrawText(graphics, text);
        boolean emptyParagraph = text.toString().trim().isEmpty();
        TextPaint textPaint = getPaint(drawText.getPoiFont(), drawText.getColor());
        CharSequence charSequence = drawText.getText();
        Rectangle2D anchor = DrawShape.getAnchor(graphics, paragraph.getParentShape());
        double wrappingWidth = getWrappingWidth(lines.isEmpty(), graphics);
        StaticLayout staticLayout = createLayout(charSequence, textPaint, (int)wrappingWidth);
        int count = staticLayout.getLineCount();
        int lineStart = 0;
        int lineEnd = 0;
        for (int i = 0; i < count; i++) {

            lineEnd = staticLayout.getLineEnd(i);
            if (lineStart >= lineEnd) {
                break;
            }
            CharSequence subSequence = charSequence.subSequence(lineStart, lineEnd);
            if (i < count - 1 && subSequence.toString().endsWith("\n")) {
                continue;
            }
            StaticLayout subLayout = createLayout(subSequence, textPaint, (int)wrappingWidth);

            DrawText dt = new DrawText();
            dt.setPoiFont(drawText.getPoiFont()).setColor(drawText.getColor());
            dt.setStaticLayout(subLayout).setText(subSequence)
                    .setEmpty(emptyParagraph).setTextPaint(textPaint);
            DrawTextFragment line = fact.getTextFragment(dt);
            lines.add(line);
            int height = staticLayout.getHeight();
            maxLineHeight = Math.max(maxLineHeight, height);
            if (i == count - 1) {
                break;
            }
            lineStart = lineEnd;
        }
        rawText = text.toString();
    }

    private StaticLayout createLayout(CharSequence charSequence, TextPaint textPaint, int width) {
        return new StaticLayout(charSequence, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
    }

    protected DrawTextFragment getBullet(Graphics2D graphics, DrawTextFragment line) {
        TextParagraph.BulletStyle bulletStyle = paragraph.getBulletStyle();
        if (bulletStyle == null) {
            return null;
        }

        String buCharacter;
        AutoNumberingScheme ans = bulletStyle.getAutoNumberingScheme();
        if (ans != null) {
            buCharacter = ans.format(autoNbrIdx);
        } else {
            buCharacter = bulletStyle.getBulletCharacter();
        }
        if (buCharacter == null) {
            return null;
        }

        PlaceableShape<?,?> ps = getParagraphShape();
        PaintStyle fgPaintStyle = bulletStyle.getBulletFontColor();
        PoiPaint fgPaint;
        if (fgPaintStyle == null) {
            fgPaint = line.getDrawText().getColor();
        } else {
            fgPaint = new DrawPaint(ps).getPaint(graphics, fgPaintStyle);
        }

        PoiFont poiFont = bulletStyle.getBulletFont();
        if (poiFont == null) {
            poiFont = paragraph.getDefaultPoiFont();
        }
        assert(poiFont != null);
        FontInfo buFont = new DrawFontInfo(poiFont);
        DrawFontManager dfm = DrawFactory.getInstance(graphics).getFontManager(graphics);
        // TODO: check font group defaulting to Symbol

        String bulletStr = dfm.mapFontCharset(graphics, buFont, buCharacter);

        Rectangle2D anchor = DrawShape.getAnchor(graphics, paragraph.getParentShape());

        DrawFactory fact = DrawFactory.getInstance(graphics);
        Color color = null;
        if (fgPaint instanceof Color) {
            color = (Color) fgPaint;
        }
        if (color == null) {
            color = Color.TRANSLUCENT;
        }
        TextPaint textPaint = getPaint(poiFont, color);
        float width = StaticLayout.getDesiredWidth(bulletStr, textPaint);
        StaticLayout staticLayout = new StaticLayout(bulletStr, textPaint, (int) width,
                Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        DrawText text = new DrawText();
        text.setStaticLayout(staticLayout).setText(bulletStr)
                .setColor(color).setPoiFont(poiFont)
                .setTextPaint(textPaint);
        return fact.getTextFragment(text);
    }

    private TextPaint getPaint(PoiFont poiFont, Color color) {
        TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(poiFont.getTypeface());
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setDither(true);
        textPaint.setTextSize(poiFont.getSize());
        textPaint.setColor(color.getRGB());
        return textPaint;
    }

    protected String getRenderableText(Graphics2D graphics, TextRun tr) {
        if (tr.getFieldType() == FieldType.SLIDE_NUMBER) {
            Slide<?,?> slide = (Slide<?,?>)graphics.getRenderingHint(Drawable.CURRENT_SLIDE);
            return (slide == null) ? "" : Integer.toString(slide.getSlideNumber());
        }
        return getRenderableText(tr);
    }

    @Internal
    public String getRenderableText(final TextRun tr) {
        String txtSpace = tr.getRawText();
        if (txtSpace == null) {
            return null;
        }
        if (txtSpace.contains("\t")) {
            txtSpace = txtSpace.replace("\t", tab2space(tr));
        }
        txtSpace = txtSpace.replace('\u000b', '\n');
        final Locale loc = LocaleUtil.getUserLocale();

        switch (tr.getTextCap()) {
            case ALL:
                return txtSpace.toUpperCase(loc);
            case SMALL:
                return txtSpace.toLowerCase(loc);
            default:
                return txtSpace;
        }
    }

    /**
     * Replace a tab with the effective number of white spaces.
     */
    private String tab2space(TextRun tr) {
        final int numSpaces = 4;
        char[] buf = new char[numSpaces];
        Arrays.fill(buf, ' ');
        return new String(buf);
    }


    /**
     * Returns wrapping width to break lines in this paragraph
     *
     * @param firstLine whether the first line is breaking
     *
     * @return  wrapping width in points
     */
    protected double getWrappingWidth(boolean firstLine, Graphics2D graphics){
        TextShape<?,?> ts = paragraph.getParentShape();

        // internal margins for the text box
        Insets2D insets = ts.getInsets();
        double leftInset = insets.left;
        double rightInset = insets.right;

        int indentLevel = paragraph.getIndentLevel();
        if (indentLevel == -1) {
            // default to 0, if indentLevel is not set
            indentLevel = 0;
        }
        Double leftMargin = paragraph.getLeftMargin();
        if (leftMargin == null) {
            // if the marL attribute is omitted, then a value of 347663 is implied
            leftMargin = Units.toPoints(347663L*(indentLevel+1));
        }
        Double indent = paragraph.getIndent();
        if (indent == null) {
            indent = Units.toPoints(347663L*indentLevel);
        }
        Double rightMargin = paragraph.getRightMargin();
        if (rightMargin == null) {
            rightMargin = 0d;
        }
        Rectangle2D anchor = DrawShape.getAnchor(graphics, ts);
        TextDirection textDir = ts.getTextDirection();
        Dimension pageDim = ts.getSheet().getSlideShow().getPageSize();
        double width;
        if (!ts.getWordWrap()) {
            // if wordWrap == false then we return the advance to the (right) border of the sheet
            switch (textDir) {
                default:
                    width = pageDim.getWidth() - anchor.getX();
                    break;
                case VERTICAL:
                    width = pageDim.getHeight() - anchor.getX();
                    break;
                case VERTICAL_270:
                    width = anchor.getX();
                    break;
            }
        } else {
            switch (textDir) {
                default:
                    width = anchor.getWidth() - leftInset - rightInset - leftMargin - rightMargin;
                    break;
                case VERTICAL:
                case VERTICAL_270:
                    width = anchor.getHeight() - leftInset - rightInset - leftMargin - rightMargin;
                    break;
            }
            if (firstLine && !isHSLF()) {
                if (bullet != null){
                    if (indent > 0) {
                        width -= indent;
                    }
                } else {
                    if (indent > 0) {
                        width -= indent; // first line indentation
                    } else if (indent < 0) { // hanging indentation: the first line start at the left margin
                        width -= indent;
                    }
                 }
            }
        }
        if (width < 0) {
            width = 0;
        }
        return width + 1;
    }

    private static class AttributedStringData {
        Attribute attribute;
        Object value;
        int beginIndex, endIndex;
        AttributedStringData(Attribute attribute, Object value, int beginIndex, int endIndex) {
            this.attribute = attribute;
            this.value = value;
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }
    }

    private static class SpannableData {
        Object value;
        int beginIndex, endIndex;

        public SpannableData(Object value, int beginIndex, int endIndex) {
            this.value = value;
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }
    }

    /**
     * Helper method for paint style relative to bounds, e.g. gradient paint
     */
    @SuppressWarnings("rawtypes")
    private PlaceableShape<?,?> getParagraphShape() {
        return new PlaceableShape(){
            @Override
            public ShapeContainer<?,?> getParent() { return null; }
            @Override
            public Rectangle2D getAnchor() { return paragraph.getParentShape().getAnchor(); }
            @Override
            public void setAnchor(Rectangle2D anchor) {}
            @Override
            public double getRotation() { return 0; }
            @Override
            public void setRotation(double theta) {}
            @Override
            public void setFlipHorizontal(boolean flip) {}
            @Override
            public void setFlipVertical(boolean flip) {}
            @Override
            public boolean getFlipHorizontal() { return false; }
            @Override
            public boolean getFlipVertical() { return false; }
            @Override
            public Sheet<?,?> getSheet() { return paragraph.getParentShape().getSheet(); }
        };
    }

    protected DrawText getDrawText(Graphics2D graphics, StringBuilder text) {
        DrawText drawText = new DrawText();
        if (text == null) {
            text = new StringBuilder();
        }

        PlaceableShape<?,?> ps = getParagraphShape();
        DrawFontManager dfm = DrawFactory.getInstance(graphics).getFontManager(graphics);
        assert(dfm != null);

        Layout.Alignment alignment;
        TextAlign textAlign = paragraph.getTextAlign();
        if (textAlign == TextAlign.LEFT) {
            alignment = Layout.Alignment.ALIGN_NORMAL;
        } else if (textAlign == TextAlign.RIGHT) {
            alignment = Layout.Alignment.ALIGN_OPPOSITE;
        } else if (textAlign == TextAlign.CENTER) {
            alignment = Layout.Alignment.ALIGN_CENTER;
        } else {
            alignment = Layout.Alignment.ALIGN_NORMAL;
        }
        SpannableStringBuilder builder;
        List<SpannableData> spannableDataList = new ArrayList<>();
        for (TextRun run : paragraph){
            String runText = getRenderableText(graphics, run);
            // skip empty runs
            if (runText.isEmpty()) {
                continue;
            }

            // user can pass an custom object to convert fonts
            runText = dfm.mapFontCharset(graphics, run.getFontInfo(), runText);
            int beginIndex = text.length();
            text.append(runText);
            int endIndex = text.length();

            int style = Typeface.NORMAL;
            if (run.isBold() && run.isItalic()) {
                style = Typeface.BOLD_ITALIC;
            } else if (run.isItalic()) {
                style = Typeface.ITALIC;
            } else if (run.isBold()) {
                style = Typeface.BOLD;
            }

            PoiFont poiFont = run.getFontInfo().getPoiFont();
            if (poiFont == null) {
                poiFont = paragraph.getDefaultPoiFont();
            }
            poiFont.setStyle(style);
            Color color = null;
            PaintStyle fgPaintStyle = run.getFontColor();
            PoiPaint fgPaint = new DrawPaint(ps).getPaint(graphics, fgPaintStyle);
            if (fgPaint instanceof Color) {
                color = (Color) fgPaint;
            }
            if (color == null) {
                color = Color.TRANSLUCENT;
            }
            drawText.setColor(color).setPoiFont(poiFont);
            ColorStateList colorStateList = ColorStateList.valueOf(color.getRGB());
            addTextAppearanceSpan(spannableDataList, runText, beginIndex, poiFont, colorStateList);
            if(run.isUnderlined()) {
                spannableDataList.add(new SpannableData(new UnderlineSpan(), beginIndex, endIndex));
            }
            if(run.isStrikethrough()) {
                spannableDataList.add(new SpannableData(new StrikethroughSpan(), beginIndex, endIndex));
            }
            if(run.isSubscript()) {
                spannableDataList.add(new SpannableData(new SubscriptSpan(), beginIndex, endIndex));
            }
            if(run.isSuperscript()) {
                spannableDataList.add(new SpannableData(new SuperscriptSpan(), beginIndex, endIndex));
            }
            spannableDataList.add(new SpannableData(new AlignmentSpan.Standard(alignment), beginIndex, endIndex));
        }

        // ensure that the paragraph contains at least one character
        // We need this trick to correctly measure text
        if (text.length() == 0) {
            String emptyStr = " ";
            Double fontSz = paragraph.getDefaultFontSize();
            text.append(emptyStr);
            PoiFont poiFont = paragraph.getDefaultPoiFont();
            Color color = Color.TRANSLUCENT;
            drawText.setColor(color).setPoiFont(poiFont);
            ColorStateList colorStateList = ColorStateList.valueOf((color.getRGB()));
            spannableDataList.add(new SpannableData(new TextAppearanceSpan(poiFont.getFamily(),
                    poiFont.getStyle(), fontSz.intValue(),
                    colorStateList, colorStateList), 0, 1));
        }
        builder = new SpannableStringBuilder(text.toString());
        for (SpannableData data : spannableDataList) {
            builder.setSpan(data.value, data.beginIndex, data.endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        drawText.setText(builder);
        return drawText;
    }


    private void addTextAppearanceSpan(List<SpannableData> spannableDataList, String runText, int beginIndex,
                                       PoiFont poiFont, ColorStateList colorStateList) {
        int length = runText.length();
        float fontSize = poiFont.getSize();
        boolean isChnese = true;
        int englishCount = 0;
        for (int i = 1; i < length; i++) {
            if (StringUtil.isEnglish(runText.charAt(i))) {
                englishCount++;
            }
            if (englishCount >= length / 3) {
                isChnese = false;
                break;
            }
        }
        poiFont.setSize(fontSize);
        spannableDataList.add(new SpannableData(new TextAppearanceSpan(poiFont.getFamily(), poiFont.getStyle(),
                (int) getFontSize(isChnese, fontSize), colorStateList, colorStateList), beginIndex, beginIndex + length));
    }

    private float getFontSize(boolean isChinese, float fontSize) {
        return isChinese ? fontSize : FontSizeUtil.winToAndForEn(fontSize);
    }

    protected AttributedString getAttributedString(Graphics2D graphics, StringBuilder text){
        List<AttributedStringData> attList = new ArrayList<>();
        if (text == null) {
            text = new StringBuilder();
        }

        PlaceableShape<?,?> ps = getParagraphShape();
        DrawFontManager dfm = DrawFactory.getInstance(graphics).getFontManager(graphics);
        assert(dfm != null);

        for (TextRun run : paragraph){
            String runText = getRenderableText(graphics, run);
            // skip empty runs
            if (runText.isEmpty()) {
                continue;
            }

            // user can pass an custom object to convert fonts

            runText = dfm.mapFontCharset(graphics, run.getFontInfo(), runText);
            int beginIndex = text.length();
            text.append(runText);
            int endIndex = text.length();

            PaintStyle fgPaintStyle = run.getFontColor();
            PoiPaint fgPaint = new DrawPaint(ps).getPaint(graphics, fgPaintStyle);
            attList.add(new AttributedStringData(TextAttribute.FOREGROUND, fgPaint, beginIndex, endIndex));

            Double fontSz = run.getFontSize();
            if (fontSz == null) {
                fontSz = paragraph.getDefaultFontSize();
            }
            attList.add(new AttributedStringData(TextAttribute.SIZE, fontSz.floatValue(), beginIndex, endIndex));

            if(run.isBold()) {
                attList.add(new AttributedStringData(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, beginIndex, endIndex));
            }
            if(run.isItalic()) {
                attList.add(new AttributedStringData(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, beginIndex, endIndex));
            }
            if(run.isUnderlined()) {
                attList.add(new AttributedStringData(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, beginIndex, endIndex));
                attList.add(new AttributedStringData(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_TWO_PIXEL, beginIndex, endIndex));
            }
            if(run.isStrikethrough()) {
                attList.add(new AttributedStringData(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, beginIndex, endIndex));
            }
            if(run.isSubscript()) {
                attList.add(new AttributedStringData(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, beginIndex, endIndex));
            }
            if(run.isSuperscript()) {
                attList.add(new AttributedStringData(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, beginIndex, endIndex));
            }

            Hyperlink<?,?> hl = run.getHyperlink();
            if (hl != null) {
                attList.add(new AttributedStringData(HYPERLINK_HREF, hl.getAddress(), beginIndex, endIndex));
                attList.add(new AttributedStringData(HYPERLINK_LABEL, hl.getLabel(), beginIndex, endIndex));
            }

            processGlyphs(graphics, dfm, attList, beginIndex, run, runText);
        }

        // ensure that the paragraph contains at least one character
        // We need this trick to correctly measure text
        if (text.length() == 0) {
            Double fontSz = paragraph.getDefaultFontSize();
            text.append(" ");
            attList.add(new AttributedStringData(TextAttribute.SIZE, fontSz.floatValue(), 0, 1));
        }

        AttributedString string = new AttributedString(text.toString());
        for (AttributedStringData asd : attList) {
            string.addAttribute(asd.attribute, asd.value, asd.beginIndex, asd.endIndex);
        }

        return string;
    }

    /**
     * Processing the glyphs is done in two steps.
     * <li>determine the font group - a text run can have different font groups. Depending on the chars,
     * the correct font group needs to be used
     *
     * @see <a href="https://blogs.msdn.microsoft.com/officeinteroperability/2013/04/22/office-open-xml-themes-schemes-and-fonts/">Office Open XML Themes, Schemes, and Fonts</a>
     */
    private void processGlyphs(Graphics2D graphics, DrawFontManager dfm, List<AttributedStringData> attList, final int beginIndex, TextRun run, String runText) {
        // determine font group ranges of the textrun to focus the fallback handling only on that font group
        int rangeBegin = 0;
            FontInfo fiRun = run.getFontInfo();
            if (fiRun == null) {
                // if the font group specific font wasn't defined, fallback to LATIN
                fiRun = run.getFontInfo();
            }
            FontInfo fiMapped = dfm.getMappedFont(graphics, fiRun);
            FontInfo fiFallback = dfm.getFallbackFont(graphics, fiRun);
            assert(fiFallback != null);
            if (fiMapped == null) {
                fiMapped = dfm.getMappedFont(graphics, new DrawFontInfo(paragraph.getDefaultPoiFont()));
            }
            if (fiMapped == null) {
                fiMapped = fiFallback;
            }

            PoiFont fontMapped = dfm.createAWTFont(graphics, fiMapped, PoiFont.DEFAULT_SIZE, run.isBold(), run.isItalic());
            PoiFont fontFallback = dfm.createAWTFont(graphics, fiFallback, PoiFont.DEFAULT_SIZE, run.isBold(), run.isItalic());

            // check for unsupported characters and add a fallback font for these
            final int rangeLen = 0;
            int partEnd = rangeBegin;
            while (partEnd<rangeBegin+rangeLen) {
                // start with the assumption that the font is able to display the chars
                int partBegin = partEnd;
                partEnd = nextPart(fontMapped, runText, partBegin, rangeBegin+rangeLen, true);

                // Now we have 3 cases:
                // (a) the first part couldn't be displayed,
                // (b) only part of the text run could be displayed
                // (c) or all chars can be displayed (default)

                if (partBegin < partEnd) {
                    // handle (b) and (c)
                    attList.add(new AttributedStringData(TextAttribute.FAMILY, fontMapped.getFamily(), beginIndex+partBegin, beginIndex+partEnd));
                    if (LOG.check(POILogger.DEBUG)) {
                        LOG.log(POILogger.DEBUG, "mapped: ",fontMapped.getFamily()," ",(beginIndex+partBegin)," ",(beginIndex+partEnd)," - ",runText.substring(beginIndex+partBegin, beginIndex+partEnd));
                    }
                }

                // fallback for unsupported glyphs
                partBegin = partEnd;
                partEnd = nextPart(fontMapped, runText, partBegin, rangeBegin+rangeLen, false);

                if (partBegin < partEnd) {
                    // handle (a) and (b)
                    attList.add(new AttributedStringData(TextAttribute.FAMILY, fontFallback.getFamily(), beginIndex+partBegin, beginIndex+partEnd));
                    if (LOG.check(POILogger.DEBUG)) {
                        LOG.log(POILogger.DEBUG, "fallback: ",fontFallback.getFamily()," ",(beginIndex+partBegin)," ",(beginIndex+partEnd)," - ",runText.substring(beginIndex+partBegin, beginIndex+partEnd));
                    }
            }
            rangeBegin += rangeLen;
        }
    }

    private static int nextPart(PoiFont fontMapped, String runText, int beginPart, int endPart, boolean isDisplayed) {
        int rIdx = beginPart;
        while (rIdx < endPart) {
            int codepoint = runText.codePointAt(rIdx);
            if (fontMapped.canDisplay(codepoint) != isDisplayed) {
                break;
            }
            rIdx += Character.charCount(codepoint);
        }
        return rIdx;
    }

    /**
     * @return {@code true} if the HSLF implementation is used
     */
    protected boolean isHSLF() {
        return DrawShape.isHSLF(paragraph.getParentShape());
    }
}
