/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package org.apache.poi.sl.draw.geom;


import android.util.Log;

import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.TypeReference;
import com.android.compaty.util.ContextUtil;
import com.google.gson.Gson;

import org.apache.poi.R;
import org.apache.poi.sl.draw.binding.CTCustomGeometry2D;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;

/**
 *
 */
public class PresetGeometries extends LinkedHashMap<String, CustomGeometry> {
    private final static POILogger LOG = POILogFactory.getLogger(PresetGeometries.class);
    private final static String BINDING_PACKAGE = "org.apache.poi.sl.draw.binding";

    private static class SingletonHelper {
        private static JAXBContext JAXB_CONTEXT;
        static {
            try {
                JAXB_CONTEXT = JAXBContext.newInstance(BINDING_PACKAGE);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
    }


    protected static PresetGeometries _inst;

    protected PresetGeometries(){}

    @SuppressWarnings("unused")
    public void init(InputStream is) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }



            /*
            * 此处在Android 10 Note3 设备上测试不通过,无法解析Json 数据导致,PPT 图片无法渲染造成画面白屏
            *  具体log --->>>>> Accessing hidden method TypeReference getType()Ljava/lang/reflect/Type; (blacklist, linking, denied)
            *  在Android 10 以下系统正常显示,在7.1， 9.0 系统下测试通过
            * */

            putAll(JSON.parseObject(builder.toString(), new TypeReference<HashMap<String, CustomGeometry>>() {}.getType()));
            //修改为Google Gson 解析后问题解决
//            putAll(gson.fromJson(builder.toString(),new TypeToken<HashMap<String, CustomGeometry>>(){}.getType()));


        } catch (Exception e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }




    /**
     * Convert a single CustomGeometry object, i.e. from xmlbeans
     */
    public static CustomGeometry convertCustomGeometry(XMLStreamReader staxReader) {
        try {
            JAXBContext jaxbContext = SingletonHelper.JAXB_CONTEXT;
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<CTCustomGeometry2D> el = unmarshaller.unmarshal(staxReader, CTCustomGeometry2D.class);
            return new CustomGeometry(el.getValue());
        } catch (JAXBException e) {
            LOG.log(POILogger.ERROR, "Unable to parse single custom geometry", e);
            return null;
        }
    }

    public static synchronized PresetGeometries getInstance(){
        if (_inst == null) {
            // use a local object first to not assign a partly constructed object
            // in case of failure
            PresetGeometries lInst = new PresetGeometries();
            try {
                try (InputStream is = ContextUtil.getContext().getResources().openRawResource(R.raw.preset_shape_definitions)) {
                    lInst.init(is);
                }
            } catch (Exception e){
                throw new RuntimeException(e);
            }
            _inst = lInst;
        }
        return _inst;
    }
}
