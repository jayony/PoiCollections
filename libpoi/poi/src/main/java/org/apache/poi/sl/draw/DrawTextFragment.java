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

import android.graphics.Paint;

import java.awt.Graphics2D;

public class DrawTextFragment implements Drawable  {
    final DrawText drawText;
    double x, y;

    public DrawTextFragment(DrawText drawText) {
        this.drawText = drawText;
    }

    public void setPosition(double x, double y) {
        // TODO: replace it, by applyTransform????
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics2D graphics){
        if (drawText.isEmpty()) {
            return;
        }
        graphics.drawLayout(drawText.getStaticLayout(), (float) x, (float) y);
    }

    public void applyTransform(Graphics2D graphics) {
    }

    public void drawContent(Graphics2D graphics) {
    }

    public DrawText getDrawText() {
        return drawText;
    }

    /**
     * @return full height of this text run which is sum of ascent, descent and leading
     */
    public float getHeight(){
        return drawText.getStaticLayout().getHeight() + getLeading();
    }

    public float getHeightWithLeading() {
        return drawText.getStaticLayout().getHeight();
    }

    /**
     * @return the leading height before/after a text line
     */
    public float getLeading() {
        Paint.FontMetrics metrics = drawText.getTextPaint().getFontMetrics();
        float leading = metrics.leading;
        if (leading == 0) {
            // see https://stackoverflow.com/questions/925147
            // we use a 115% value instead of the 120% proposed one, as this seems to be closer to LO/OO
            leading = (-metrics.ascent - metrics.descent) * 0.12f;
        }
        return leading;
    }
    
    /**
     *
     * @return width if this text run
     */
    public float getWidth(){
        return drawText.getStaticLayout().getWidth();
    }

    /**
     *
     * @return the string to be painted
     */
    public String getString(){
        return drawText.getText().toString();
    }

    @Override
    public String toString(){
        return "[" + getClass().getSimpleName() + "] " + getString();
    }
}
