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

package org.apache.poi.hemf.record.emfplus;

import com.android.compaty.util.ImageUtils;

import org.apache.poi.sl.draw.BitmapImageRenderer;
import org.apache.poi.util.IOUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HemfPlusGDIImageRenderer extends BitmapImageRenderer {
    private int width;
    private int height;
    private int stride;
    private HemfPlusImage.EmfPlusPixelFormat pixelFormat;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getStride() {
        return stride;
    }

    public void setStride(int stride) {
        this.stride = stride;
    }

    public HemfPlusImage.EmfPlusPixelFormat getPixelFormat() {
        return pixelFormat;
    }

    public void setPixelFormat(HemfPlusImage.EmfPlusPixelFormat pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    @Override
    public boolean canRender(String contentType) {
        return true;
    }

    @Override
    public void loadImage(InputStream data, String contentType) throws IOException {
        img = readGDIImage(IOUtils.toByteArray(data));
    }

    @Override
    public void loadImage(byte[] data, String contentType) throws IOException {
        img = readGDIImage(data);
    }

    /**
     * Converts the gdi pixel data to a buffered image
     * @param data the image data of all EmfPlusImage parts
     * @return the BufferedImage
     */
    public BufferedImage readGDIImage(final byte[] data) {
        return ImageUtils.read(new ByteArrayInputStream(data));
    }

}
