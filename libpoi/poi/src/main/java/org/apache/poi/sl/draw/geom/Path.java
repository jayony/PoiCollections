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

import org.apache.poi.sl.draw.binding.*;
import org.apache.poi.sl.usermodel.PaintStyle.PaintModifier;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifies a creation path consisting of a series of moves, lines and curves
 * that when combined forms a geometric shape
 */
public class Path {

    private transient List<PathCommand> commands;
    private List<MoveToCommand> moveToCommand;
    private List<LineToCommand> lineToCommand;
    private List<ArcToCommand> arcToCommand;
    private List<QuadToCommand> quadToCommand;
    private List<CurveToCommand> curveToCommand;
    private List<ClosePathCommand> closePathCommand;
    PaintModifier fill;
    boolean stroke;
    long w, h;

    public Path(){
        this(true, true);
    }

    public Path(boolean fill, boolean stroke){
        initList();
        w = -1;
        h = -1;
        this.fill = (fill) ? PaintModifier.NORM : PaintModifier.NONE;
        this.stroke = stroke;
    }

    private void initList() {
        commands = new ArrayList<>();
        moveToCommand = new ArrayList<>();
        lineToCommand = new ArrayList<>();
        arcToCommand = new ArrayList<>();
        quadToCommand = new ArrayList<>();
        curveToCommand = new ArrayList<>();
        closePathCommand = new ArrayList<>();
    }

    public Path(CTPath2D spPath){
        switch (spPath.getFill()) {
            case NONE: fill = PaintModifier.NONE; break;
            case DARKEN: fill = PaintModifier.DARKEN; break;
            case DARKEN_LESS: fill = PaintModifier.DARKEN_LESS; break;
            case LIGHTEN: fill = PaintModifier.LIGHTEN; break;
            case LIGHTEN_LESS: fill = PaintModifier.LIGHTEN_LESS; break;
            default:
            case NORM: fill = PaintModifier.NORM; break;
        }
        stroke = spPath.isStroke();
        w = spPath.isSetW() ? spPath.getW() : -1;
        h = spPath.isSetH() ? spPath.getH() : -1;
        initList();
        for(Object ch : spPath.getCloseOrMoveToOrLnTo()){
            if(ch instanceof CTPath2DMoveTo){
                CTAdjPoint2D pt = ((CTPath2DMoveTo)ch).getPt();
                MoveToCommand moveToCommand = new MoveToCommand(pt);
                commands.add(moveToCommand);
                this.moveToCommand.add(moveToCommand);
            } else if (ch instanceof CTPath2DLineTo){
                CTAdjPoint2D pt = ((CTPath2DLineTo)ch).getPt();
                LineToCommand lineToCommand = new LineToCommand(pt);
                commands.add(lineToCommand);
                this.lineToCommand.add(lineToCommand);
            } else if (ch instanceof CTPath2DArcTo){
                CTPath2DArcTo arc = (CTPath2DArcTo)ch;
                ArcToCommand arcToCommand = new ArcToCommand(arc);
                commands.add(arcToCommand);
                this.arcToCommand.add(arcToCommand);
            } else if (ch instanceof CTPath2DQuadBezierTo){
                CTPath2DQuadBezierTo bez = ((CTPath2DQuadBezierTo)ch);
                CTAdjPoint2D pt1 = bez.getPt().get(0);
                CTAdjPoint2D pt2 = bez.getPt().get(1);
                QuadToCommand quadToCommand = new QuadToCommand(pt1, pt2);
                commands.add(quadToCommand);
                this.quadToCommand.add(quadToCommand);
            } else if (ch instanceof CTPath2DCubicBezierTo){
                CTPath2DCubicBezierTo bez = ((CTPath2DCubicBezierTo)ch);
                CTAdjPoint2D pt1 = bez.getPt().get(0);
                CTAdjPoint2D pt2 = bez.getPt().get(1);
                CTAdjPoint2D pt3 = bez.getPt().get(2);
                CurveToCommand curveToCommand = new CurveToCommand(pt1, pt2, pt3);
                commands.add(curveToCommand);
                this.curveToCommand.add(curveToCommand);
            } else if (ch instanceof CTPath2DClose){
                ClosePathCommand closePathCommand = new ClosePathCommand();
                commands.add(closePathCommand);
                this.closePathCommand.add(closePathCommand);
            }  else {
                throw new IllegalStateException("Unsupported path segment: " + ch);
            }
        }
    }

    public void addCommand(PathCommand cmd){
        commands.add(cmd);
        if (cmd instanceof MoveToCommand) {
            moveToCommand.add((MoveToCommand) cmd);
        } else if (cmd instanceof LineToCommand) {
            lineToCommand.add((LineToCommand) cmd);
        } else if (cmd instanceof ArcToCommand) {
            arcToCommand.add((ArcToCommand) cmd);
        } else if (cmd instanceof QuadToCommand) {
            quadToCommand.add((QuadToCommand) cmd);
        } else if (cmd instanceof CurveToCommand) {
            curveToCommand.add((CurveToCommand) cmd);
        } else if (cmd instanceof ClosePathCommand) {
            closePathCommand.add((ClosePathCommand) cmd);
        }
    }

    /**
     * Convert the internal represenation to java.awt.geom.Path2D
     */
    public Path2D.Double getPath(Context ctx) {
        Path2D.Double path = new Path2D.Double();
        for(PathCommand cmd : getCommands()) {
            cmd.execute(path, ctx);
        }
        return path;
    }

    public void setCommands(List<PathCommand> commands) {
        this.commands = commands;
    }

    public List<MoveToCommand> getMoveToCommand() {
        return moveToCommand;
    }

    public void setMoveToCommand(List<MoveToCommand> moveToCommand) {
        this.moveToCommand = moveToCommand;
    }

    public List<LineToCommand> getLineToCommand() {
        return lineToCommand;
    }

    public void setLineToCommand(List<LineToCommand> lineToCommand) {
        this.lineToCommand = lineToCommand;
    }

    public List<ArcToCommand> getArcToCommand() {
        return arcToCommand;
    }

    public void setArcToCommand(List<ArcToCommand> arcToCommand) {
        this.arcToCommand = arcToCommand;
    }

    public List<QuadToCommand> getQuadToCommand() {
        return quadToCommand;
    }

    public void setQuadToCommand(List<QuadToCommand> quadToCommand) {
        this.quadToCommand = quadToCommand;
    }

    public List<CurveToCommand> getCurveToCommand() {
        return curveToCommand;
    }

    public void setCurveToCommand(List<CurveToCommand> curveToCommand) {
        this.curveToCommand = curveToCommand;
    }

    public List<ClosePathCommand> getClosePathCommand() {
        return closePathCommand;
    }

    public void setClosePathCommand(List<ClosePathCommand> closePathCommand) {
        this.closePathCommand = closePathCommand;
    }

    public boolean isStroked(){
        return stroke;
    }

    public boolean isFilled(){
        return fill != PaintModifier.NONE;
    }

    public List<PathCommand> getCommands() {
        if (commands.isEmpty()) {
            commands.addAll(moveToCommand);
            commands.addAll(lineToCommand);
            commands.addAll(arcToCommand);
            commands.addAll(quadToCommand);
            commands.addAll(curveToCommand);
            commands.addAll(closePathCommand);
        }
        return commands;
    }

    public PaintModifier getFill() {
        return fill;
    }

    public void setFill(PaintModifier fill) {
        this.fill = fill;
    }

    public boolean isStroke() {
        return stroke;
    }

    public void setStroke(boolean stroke) {
        this.stroke = stroke;
    }

    public long getW() {
        return w;
    }

    public void setW(long w) {
        this.w = w;
    }

    public long getH() {
        return h;
    }

    public void setH(long h) {
        this.h = h;
    }
}
