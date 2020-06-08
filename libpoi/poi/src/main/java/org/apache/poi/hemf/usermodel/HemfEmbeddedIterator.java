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

package org.apache.poi.hemf.usermodel;

import org.apache.poi.hemf.record.emf.HemfComment;
import org.apache.poi.hemf.record.emf.HemfRecord;
import org.apache.poi.hemf.record.emfplus.HemfPlusImage.EmfPlusBitmapDataType;
import org.apache.poi.hemf.record.emfplus.HemfPlusImage.EmfPlusImage;
import org.apache.poi.hemf.record.emfplus.HemfPlusObject;
import org.apache.poi.hemf.record.emfplus.HemfPlusObject.EmfPlusObject;
import org.apache.poi.hwmf.record.HwmfBitmapDib;
import org.apache.poi.hwmf.record.HwmfFill;
import org.apache.poi.hwmf.usermodel.HwmfEmbedded;
import org.apache.poi.hwmf.usermodel.HwmfEmbeddedType;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.util.IOUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

//import javax.imageio.ImageIO;

public class HemfEmbeddedIterator implements Iterator<HwmfEmbedded> {
    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 100_000_000;

    private final Deque<Iterator<?>> iterStack = new ArrayDeque<>();
    private Object current;

    public HemfEmbeddedIterator(HemfPicture emf) {
        this(emf.getRecords().iterator());
    }

    public HemfEmbeddedIterator(Iterator<HemfRecord> recordIterator) {
        iterStack.add(recordIterator);
    }

    @Override
    public boolean hasNext() {
        if (iterStack.isEmpty()) {
            return false;
        }

        if (current != null) {
            // don't search twice and potentially skip items
            return true;
        }

        Iterator<?> iter;
        do {
            iter = iterStack.peek();
            while (iter.hasNext()) {
                Object obj = iter.next();
                if (obj instanceof HemfComment.EmfComment) {
                    HemfComment.EmfCommentData cd = ((HemfComment.EmfComment)obj).getCommentData();
                    if (
                        cd instanceof HemfComment.EmfCommentDataWMF ||
                        cd instanceof HemfComment.EmfCommentDataGeneric
                    ) {
                        current = obj;
                        return true;
                    }

                    if (cd instanceof HemfComment.EmfCommentDataMultiformats) {
                        Iterator<?> iter2 = ((HemfComment.EmfCommentDataMultiformats)cd).getFormats().iterator();
                        if (iter2.hasNext()) {
                            iterStack.push(iter2);
                            continue;
                        }
                    }

                    if (cd instanceof HemfComment.EmfCommentDataPlus) {
                        Iterator<?> iter2 = ((HemfComment.EmfCommentDataPlus)cd).getRecords().iterator();
                        if (iter2.hasNext()) {
                            iter = iter2;
                            iterStack.push(iter2);
                            continue;
                        }
                    }
                }

                if (obj instanceof HemfComment.EmfCommentDataFormat) {
                    current = obj;
                    return true;
                }

                if (obj instanceof EmfPlusObject && ((EmfPlusObject)obj).getObjectType() == HemfPlusObject.EmfPlusObjectType.IMAGE) {
                    current = obj;
                    return true;
                }

                if (obj instanceof HwmfFill.WmfStretchDib) {
                    HwmfBitmapDib bitmap = ((HwmfFill.WmfStretchDib) obj).getBitmap();
                    if (bitmap.isValid()) {
                        current = obj;
                        return true;
                    }
                }
            }
            iterStack.pop();
        } while (!iterStack.isEmpty());

        return false;
    }

    @Override
    public HwmfEmbedded next() {
        HwmfEmbedded emb;
        if ((emb = checkEmfCommentDataWMF()) != null) {
            return emb;
        }
        if ((emb = checkEmfCommentDataGeneric()) != null) {
            return emb;
        }
        if ((emb = checkEmfCommentDataFormat()) != null) {
            return emb;
        }
        if ((emb = checkEmfPlusObject()) != null) {
            return emb;
        }
        if ((emb = checkWmfStretchDib()) != null) {
            return emb;
        }

        return null;
    }

    private HwmfEmbedded checkEmfCommentDataWMF() {
        if (!(current instanceof HemfComment.EmfCommentDataWMF && ((HemfComment.EmfComment)current).getCommentData() instanceof HemfComment.EmfCommentDataWMF)) {
            return null;
        }

        HemfComment.EmfCommentDataWMF wmf = (HemfComment.EmfCommentDataWMF)((HemfComment.EmfComment)current).getCommentData();
        HwmfEmbedded emb = new HwmfEmbedded();
        emb.setEmbeddedType(HwmfEmbeddedType.WMF);
        emb.setData(wmf.getWMFData());
        current = null;
        return emb;
    }

    private HwmfEmbedded checkEmfCommentDataGeneric() {
        if (!(current instanceof HemfComment.EmfComment && ((HemfComment.EmfComment)current).getCommentData() instanceof HemfComment.EmfCommentDataGeneric)) {
            return null;
        }
        HemfComment.EmfCommentDataGeneric cdg = (HemfComment.EmfCommentDataGeneric)((HemfComment.EmfComment)current).getCommentData();
        HwmfEmbedded emb = new HwmfEmbedded();
        emb.setEmbeddedType(HwmfEmbeddedType.UNKNOWN);
        emb.setData(cdg.getPrivateData());
        current = null;
        return emb;
    }

    private HwmfEmbedded checkEmfCommentDataFormat() {
        if (!(current instanceof HemfComment.EmfCommentDataFormat)) {
            return null;
        }
        HemfComment.EmfCommentDataFormat cdf = (HemfComment.EmfCommentDataFormat)current;
        HwmfEmbedded emb = new HwmfEmbedded();
        boolean isEmf = (cdf.getSignature() == HemfComment.EmfFormatSignature.ENHMETA_SIGNATURE);
        emb.setEmbeddedType(isEmf ? HwmfEmbeddedType.EMF : HwmfEmbeddedType.EPS);
        emb.setData(cdf.getRawData());
        current = null;
        return emb;
    }

    private HwmfEmbedded checkWmfStretchDib() {
        if (!(current instanceof HwmfFill.WmfStretchDib)) {
            return null;
        }
        HwmfEmbedded emb = new HwmfEmbedded();
        emb.setData(((HwmfFill.WmfStretchDib) current).getBitmap().getBMPData());
        emb.setEmbeddedType(HwmfEmbeddedType.BMP);
        current = null;
        return emb;
    }

    private HwmfEmbedded checkEmfPlusObject() {
        if (!(current instanceof EmfPlusObject)) {
            return null;
        }

        EmfPlusObject epo = (EmfPlusObject)current;
        assert(epo.getObjectType() == HemfPlusObject.EmfPlusObjectType.IMAGE);
        EmfPlusImage img = epo.getObjectData();
        assert(img.getImageDataType() != null);

        final HwmfEmbedded emb = getEmfPlusImageData();
        if (emb == null) {
            return null;
        }

        final HwmfEmbeddedType et;
        switch (img.getImageDataType()) {
            case BITMAP:
                if (img.getBitmapType() == EmfPlusBitmapDataType.COMPRESSED) {
                    switch (FileMagic.valueOf(emb.getRawData())) {
                        case JPEG:
                            et = HwmfEmbeddedType.JPEG;
                            break;
                        case GIF:
                            et = HwmfEmbeddedType.GIF;
                            break;
                        case PNG:
                            et = HwmfEmbeddedType.PNG;
                            break;
                        case TIFF:
                            et = HwmfEmbeddedType.TIFF;
                            break;
                        default:
                            et = HwmfEmbeddedType.BITMAP;
                            break;
                    }
                } else {
                    et = HwmfEmbeddedType.PNG;
                    compressGDIBitmap(img, emb, et);
                }
                break;
            case METAFILE:
                assert(img.getMetafileType() != null);
                switch (img.getMetafileType()) {
                    case Wmf:
                    case WmfPlaceable:
                        et = HwmfEmbeddedType.WMF;
                        break;
                    case Emf:
                    case EmfPlusDual:
                    case EmfPlusOnly:
                        et = HwmfEmbeddedType.EMF;
                        break;
                    default:
                        et = HwmfEmbeddedType.UNKNOWN;
                        break;
                }
                break;
            default:
                et = HwmfEmbeddedType.UNKNOWN;
                break;
        }
        emb.setEmbeddedType(et);

        return emb;
    }

    /**
     * Compress GDIs internal format to something useful
     */
    private void compressGDIBitmap(EmfPlusImage img, HwmfEmbedded emb, HwmfEmbeddedType et) {
        BufferedImage bi = img.readGDIImage(emb.getRawData());
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // use HwmfEmbeddedType literal for conversion
//            ImageIO.write(bi, et.toString(), bos);
            emb.setData(bos.toByteArray());
        } catch (Exception e) {
            // TODO: throw appropriate exception
            throw new RuntimeException(e);
        }
    }


    private HwmfEmbedded getEmfPlusImageData() {
        EmfPlusObject epo = (EmfPlusObject)current;
        assert(epo.getObjectType() == HemfPlusObject.EmfPlusObjectType.IMAGE);

        final int objectId = epo.getObjectId();

        HwmfEmbedded emb = new HwmfEmbedded();

        EmfPlusImage img = epo.getObjectData();
        assert(img.getImageDataType() != null);

        int totalSize = epo.getTotalObjectSize();
        IOUtils.safelyAllocateCheck(totalSize, MAX_RECORD_LENGTH);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(epo.getTotalObjectSize());
        try {
            for (;;) {
                bos.write(img.getImageData());

                current = null;
                //noinspection ConstantConditions
                if (hasNext() &&
                    (current instanceof EmfPlusObject) &&
                    ((epo = (EmfPlusObject) current).getObjectId() == objectId) &&
                    bos.size() < totalSize-16
                ) {
                    img = epo.getObjectData();
                } else {
                    return emb;
                }
            }
        } catch (IOException e) {
            // ByteArrayOutputStream doesn't throw IOException
            return null;
        } finally {
            emb.setData(bos.toByteArray());
        }
    }
}