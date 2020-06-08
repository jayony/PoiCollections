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

import org.apache.poi.sl.draw.binding.CTGeomGuide;

import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

/**
 * A simple pattern parser of shape guide formulas in DrawingML
 */
public class Guide implements Formula {
    enum Op {
        muldiv,addsub,adddiv,ifelse,val,abs,sqrt,max,min,at2,sin,cos,tan,cat2,sat2,pin,mod
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private String name;
    private String fmla;
    private transient Op op;
    private transient String[] operands;

    public Guide() {
    }

    public Guide(CTGeomGuide gd) {
        this(gd.getName(), gd.getFmla());
    }

    public Guide(String nm, String fm){
        name = nm;
        fmla = fm;
        updateOp(fm);
    }

    public String getName(){
        return name;
    }

    public Guide setName(String name) {
        this.name = name;
        return this;
    }

    public Guide setFmla(String fmla) {
        this.fmla = fmla;
        return this;
    }

    public String getFmla() {
        return fmla;
    }

    public Op getOp() {
        return op;
    }

    private void updateOp() {
        if (op == null || operands == null) {
            updateOp(fmla);
        }
    }

    private void updateOp(String fmla) {
        operands = WHITESPACE.split(fmla);
        switch (operands[0]) {
            case "*/": op = Op.muldiv; break;
            case "+-": op = Op.addsub; break;
            case "+/": op = Op.adddiv; break;
            case "?:": op = Op.ifelse; break;
            default: op = Op.valueOf(operands[0]); break;
        }
    }

    @Override
    public double evaluate(Context ctx) {
        updateOp();
        double x = (operands.length > 1) ? ctx.getValue(operands[1]) : 0;
        double y = (operands.length > 2) ? ctx.getValue(operands[2]) : 0;
        double z = (operands.length > 3) ? ctx.getValue(operands[3]) : 0;
        switch (op) {
            case abs:
                // Absolute Value Formula
                return abs(x);
            case adddiv:
                // Add Divide Formula
                return (z == 0) ? 0 : (x + y) / z;
            case addsub:
                // Add Subtract Formula
                return (x + y) - z;
            case at2:
                // ArcTan Formula: "at2 x y" = arctan( y / z ) = value of this guide
                return toDegrees(atan2(y, x)) * OOXML_DEGREE;
            case cos:
                // Cosine Formula: "cos x y" = (x * cos( y )) = value of this guide
                return x * cos(toRadians(y / OOXML_DEGREE));
            case cat2:
                // Cosine ArcTan Formula: "cat2 x y z" = (x * cos(arctan(z / y) )) = value of this guide
                return x * cos(atan2(z, y));
            case ifelse:
                // If Else Formula: "?: x y z" = if (x > 0), then y = value of this guide,
                // else z = value of this guide
                return x > 0 ? y : z;
            case val:
                // Literal Value Expression
                return x;
            case max:
                // Maximum Value Formula
                return max(x, y);
            case min:
                // Minimum Value Formula
                return min(x, y);
            case mod:
                // Modulo Formula: "mod x y z" = sqrt(x^2 + b^2 + c^2) = value of this guide
                return sqrt(x*x + y*y + z*z);
            case muldiv:
                // Multiply Divide Formula
                return (z == 0) ? 0 : (x * y) / z;
            case pin:
                // Pin To Formula: "pin x y z" = if (y < x), then x = value of this guide
                // else if (y > z), then z = value of this guide
                // else y = value of this guide
                return max(x, min(y, z));
            case sat2:
                // Sine ArcTan Formula: "sat2 x y z" = (x*sin(arctan(z / y))) = value of this guide
                return x * sin(atan2(z, y));
            case sin:
                // Sine Formula: "sin x y" = (x * sin( y )) = value of this guide
                return x * sin(toRadians(y / OOXML_DEGREE));
            case sqrt:
                // Square Root Formula: "sqrt x" = sqrt(x) = value of this guide
                return sqrt(x);
            case tan:
                // Tangent Formula: "tan x y" = (x * tan( y )) = value of this guide
                return x * tan(toRadians(y / OOXML_DEGREE));
            default:
                return 0;
        }
    }
}
