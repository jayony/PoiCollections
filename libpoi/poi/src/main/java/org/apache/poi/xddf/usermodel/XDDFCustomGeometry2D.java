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

package org.apache.poi.xddf.usermodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.openxmlformats.schemas.drawingml.x2006.main.CTConnectionSite;
import org.openxmlformats.schemas.drawingml.x2006.main.CTCustomGeometry2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuide;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPath2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPolarAdjustHandle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTXYAdjustHandle;

@Beta
public class XDDFCustomGeometry2D {
    private CTCustomGeometry2D geometry;

    protected XDDFCustomGeometry2D(CTCustomGeometry2D geometry) {
        this.geometry = geometry;
    }

    @Internal
    protected CTCustomGeometry2D getXmlObject() {
        return geometry;
    }

    public XDDFGeometryRectangle getRectangle() {
        if (geometry.isSetRect()) {
            return new XDDFGeometryRectangle(geometry.getRect());
        } else {
            return null;
        }
    }

    public void setRectangle(XDDFGeometryRectangle rectangle) {
        if (rectangle == null) {
            if (geometry.isSetRect()) {
                geometry.unsetRect();
            }
        } else {
            geometry.setRect(rectangle.getXmlObject());
        }
    }

    public XDDFAdjustHandlePolar addPolarAdjustHandle() {
        if (!geometry.isSetAhLst()) {
            geometry.addNewAhLst();
        }
        return new XDDFAdjustHandlePolar(geometry.getAhLst().addNewAhPolar());
    }

    public XDDFAdjustHandlePolar insertPolarAdjustHandle(int index) {
        if (!geometry.isSetAhLst()) {
            geometry.addNewAhLst();
        }
        return new XDDFAdjustHandlePolar(geometry.getAhLst().insertNewAhPolar(index));
    }

    public void removePolarAdjustHandle(int index) {
        if (geometry.isSetAhLst()) {
            geometry.getAhLst().removeAhPolar(index);
        }
    }

    public XDDFAdjustHandlePolar getPolarAdjustHandle(int index) {
        if (geometry.isSetAhLst()) {
            return new XDDFAdjustHandlePolar(geometry.getAhLst().getAhPolarArray(index));
        } else {
            return null;
        }
    }

    public List<XDDFAdjustHandlePolar> getPolarAdjustHandles() {
        if (geometry.isSetAhLst()) {
            List<XDDFAdjustHandlePolar> list = new ArrayList<>();
            for (CTPolarAdjustHandle handle : geometry.getAhLst().getAhPolarList()) {
                list.add(new XDDFAdjustHandlePolar(handle));
            }
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    public XDDFAdjustHandleXY addXYAdjustHandle() {
        if (!geometry.isSetAhLst()) {
            geometry.addNewAhLst();
        }
        return new XDDFAdjustHandleXY(geometry.getAhLst().addNewAhXY());
    }

    public XDDFAdjustHandleXY insertXYAdjustHandle(int index) {
        if (!geometry.isSetAhLst()) {
            geometry.addNewAhLst();
        }
        return new XDDFAdjustHandleXY(geometry.getAhLst().insertNewAhXY(index));
    }

    public void removeXYAdjustHandle(int index) {
        if (geometry.isSetAhLst()) {
            geometry.getAhLst().removeAhXY(index);
        }
    }

    public XDDFAdjustHandleXY getXYAdjustHandle(int index) {
        if (geometry.isSetAhLst()) {
            return new XDDFAdjustHandleXY(geometry.getAhLst().getAhXYArray(index));
        } else {
            return null;
        }
    }

    public List<XDDFAdjustHandleXY> getXYAdjustHandles() {
        if (geometry.isSetAhLst()) {
            List<XDDFAdjustHandleXY> list = new ArrayList<>();
            for (CTXYAdjustHandle handle : geometry.getAhLst().getAhXYList()) {
                list.add(new XDDFAdjustHandleXY(handle));
            }
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    public XDDFGeometryGuide addAdjustValue() {
        if (!geometry.isSetAvLst()) {
            geometry.addNewAvLst();
        }
        return new XDDFGeometryGuide(geometry.getAvLst().addNewGd());
    }

    public XDDFGeometryGuide insertAdjustValue(int index) {
        if (!geometry.isSetAvLst()) {
            geometry.addNewAvLst();
        }
        return new XDDFGeometryGuide(geometry.getAvLst().insertNewGd(index));
    }

    public void removeAdjustValue(int index) {
        if (geometry.isSetAvLst()) {
            geometry.getAvLst().removeGd(index);
        }
    }

    public XDDFGeometryGuide getAdjustValue(int index) {
        if (geometry.isSetAvLst()) {
            return new XDDFGeometryGuide(geometry.getAvLst().getGdArray(index));
        } else {
            return null;
        }
    }

    public List<XDDFGeometryGuide> getAdjustValues() {
        if (geometry.isSetAvLst()) {
            List<XDDFGeometryGuide> list = new ArrayList<>();
            for (CTGeomGuide handle : geometry.getAvLst().getGdList()) {
                list.add(new XDDFGeometryGuide(handle));
            }
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    public XDDFConnectionSite addConnectionSite() {
        if (!geometry.isSetCxnLst()) {
            geometry.addNewCxnLst();
        }
        return new XDDFConnectionSite(geometry.getCxnLst().addNewCxn());
    }

    public XDDFConnectionSite insertConnectionSite(int index) {
        if (!geometry.isSetCxnLst()) {
            geometry.addNewCxnLst();
        }
        return new XDDFConnectionSite(geometry.getCxnLst().insertNewCxn(index));
    }

    public void removeConnectionSite(int index) {
        if (geometry.isSetCxnLst()) {
            geometry.getCxnLst().removeCxn(index);
        }
    }

    public XDDFConnectionSite getConnectionSite(int index) {
        if (geometry.isSetCxnLst()) {
            return new XDDFConnectionSite(geometry.getCxnLst().getCxnArray(index));
        } else {
            return null;
        }
    }

    public List<XDDFConnectionSite> getConnectionSites() {
        if (geometry.isSetCxnLst()) {
            List<XDDFConnectionSite> list = new ArrayList<>();
            for (CTConnectionSite handle : geometry.getCxnLst().getCxnList()) {
                list.add(new XDDFConnectionSite(handle));
            }
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    public XDDFGeometryGuide addGuide() {
        if (!geometry.isSetGdLst()) {
            geometry.addNewGdLst();
        }
        return new XDDFGeometryGuide(geometry.getGdLst().addNewGd());
    }

    public XDDFGeometryGuide insertGuide(int index) {
        if (!geometry.isSetGdLst()) {
            geometry.addNewGdLst();
        }
        return new XDDFGeometryGuide(geometry.getGdLst().insertNewGd(index));
    }

    public void removeGuide(int index) {
        if (geometry.isSetGdLst()) {
            geometry.getGdLst().removeGd(index);
        }
    }

    public XDDFGeometryGuide getGuide(int index) {
        if (geometry.isSetGdLst()) {
            return new XDDFGeometryGuide(geometry.getGdLst().getGdArray(index));
        } else {
            return null;
        }
    }

    public List<XDDFGeometryGuide> getGuides() {
        if (geometry.isSetGdLst()) {
            List<XDDFGeometryGuide> list = new ArrayList<>();
            for (CTGeomGuide handle : geometry.getGdLst().getGdList()) {
                list.add(new XDDFGeometryGuide(handle));
            }
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    public XDDFPath addNewPath() {
        return new XDDFPath(geometry.getPathLst().addNewPath());
    }

    public XDDFPath insertNewPath(int index) {
        return new XDDFPath(geometry.getPathLst().insertNewPath(index));
    }

    public void removePath(int index) {
        geometry.getPathLst().removePath(index);
    }

    public XDDFPath getPath(int index) {
        return new XDDFPath(geometry.getPathLst().getPathArray(index));
    }

    public List<XDDFPath> getPaths() {
        List<XDDFPath> list = new ArrayList<>();
        for (CTPath2D handle : geometry.getPathLst().getPathList()) {
            list.add(new XDDFPath(handle));
        }
        return list;
    }
}
