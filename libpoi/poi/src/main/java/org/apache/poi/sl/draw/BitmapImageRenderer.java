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
import android.graphics.Rect;

import com.android.compaty.util.ImageUtils;

import org.apache.poi.sl.usermodel.PictureData.PictureType;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * For now this class renders only images supported by the javax.imageio.ImageIO framework.
 **/
public class BitmapImageRenderer implements ImageRenderer {
    private final static POILogger LOG = POILogFactory.getLogger(BitmapImageRenderer.class);

    protected BufferedImage img;

    @Override
    public boolean canRender(String contentType) {
        PictureType[] pts = {
                PictureType.JPEG, PictureType.PNG, PictureType.BMP, PictureType.GIF
        };
        for (PictureType pt : pts) {
            if (pt.contentType.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        LOG.log(POILogger.ERROR, "unsupport pciture type : " + contentType);
        return false;
    }

    @Override
    public void loadImage(InputStream data, String contentType) throws IOException {
        img = readImage(data, contentType);
    }

    @Override
    public void loadImage(byte[] data, String contentType) throws IOException {
        img = readImage(new ByteArrayInputStream(data), contentType);
    }

    /**
     * Read the image data via ImageIO and optionally try to workaround metadata errors.
     *
     * @param data the data stream
     * @param contentType the content type
     * @return the bufferedImage or null, if there was no image reader for this content type
     * @throws IOException thrown if there was an error while processing the image
     */
    private static BufferedImage readImage(final InputStream data, final String contentType) throws IOException {
        BufferedImage img = null;

        final ByteArrayInputStream bis;
        if (data instanceof ByteArrayInputStream) {
            bis = (ByteArrayInputStream)data;
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(0x3FFFF);
            IOUtils.copy(data, bos);
            bis = new ByteArrayInputStream(bos.toByteArray());
        }
        img = ImageUtils.read(bis);
        if (img != null) {
            img.setContentType(contentType);
        }
        return img;
    }

    @Override
    public BufferedImage getImage() {
        return img;
    }

    @Override
    public BufferedImage getImage(Dimension2D dim) {
        if (img == null) {
            return img;
        }
        double w_old = img.getWidth();
        double h_old = img.getHeight();
        double w_new = dim.getWidth();
        double h_new = dim.getHeight();
        if (w_old == w_new && h_old == h_new) {
            return img;
        }
        return BufferedImage.create((int)w_new, (int)h_new);
    }

    @Override
    public Rectangle2D getBounds() {
        return (img == null)
                ? new Rectangle2D.Double()
                : new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight());
    }

    @Override
    public void setAlpha(double alpha) {
        img = setAlpha(img, alpha);
    }

    public static BufferedImage setAlpha(BufferedImage image, double alpha) {
        if (image == null) {
            return BufferedImage.create(1, 1);
        }
        LOG.log(POILogger.WARN, "Not implements");
        return image;
    }

    @Override
    public boolean drawImage(Graphics2D graphics, Rectangle2D anchor) {
        return drawImage(graphics, anchor, null);
    }

    @Override
    public boolean drawImage(Graphics2D graphics, Rectangle2D anchor, Insets clip) {
        if (img == null) return false;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);

        if (clip == null) {
            clip = new Insets(0,0,0,0);
        }

        int iw = img.getWidth();
        int ih = img.getHeight();
        double cx = clip.left / 100000.0;
        double cy = clip.top / 100000.0;
        double cw = (100000-clip.left-clip.right) / 100000.0;
        double ch = (100000-clip.top-clip.bottom) / 100000.0;

        Rect src = new Rect((int) (iw * cx), (int) (ih * cy), (int) (iw * (cx + cw)), (int) (ih * (cy + ch)));
        Rect dst = new Rect((int)anchor.getX(), (int)anchor.getY(),
                (int)(anchor.getX() + anchor.getWidth()),
                (int)(anchor.getY() + anchor.getHeight()));

        Shape clipOld = graphics.getClip();
        graphics.drawImage(img, src, dst, paint);
        graphics.setClip(clipOld);
        return true;
    }

    @Override
    public Rectangle2D getNativeBounds() {
        return new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight());
    }
}
