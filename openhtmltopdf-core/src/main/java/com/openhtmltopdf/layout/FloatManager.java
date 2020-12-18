/*
 * {{{ header & license
 * Copyright (c) 2005 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.layout;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.derived.RectPropertySet;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.LineBox;

/**
 * A class that manages all floated boxes in a given block formatting context.
 * It is responsible for positioning floats and calculating clearance for
 * non-floated (block) boxes.
 */
public class FloatManager {
    public enum FloatDirection {
        LEFT,
        RIGHT;
    }

    /* Lazily created for performance. */
    private List<BoxOffset> _leftFloats = Collections.emptyList();
    private List<BoxOffset> _rightFloats = Collections.emptyList();

    private final Box _master;

    public FloatManager(Box master) {
        this._master = master;
    }

    private List<BoxOffset> getAddableFloats(FloatDirection direction) {
        if (getFloats(direction).isEmpty()) {
            setFloats(direction, new ArrayList<>());
        }

        return getFloats(direction);
    }

    private void setFloats(FloatDirection direction, List<BoxOffset> list) {
        if (direction == FloatDirection.LEFT) {
            _leftFloats = list;
        } else {
            assert direction == FloatDirection.RIGHT;
            _rightFloats = list;
        }
    }

    public void floatBox(LayoutContext c, Layer layer, BlockFormattingContext bfc, BlockBox box) {
        if (box.getStyle().isFloatedLeft()) {
            position(c, bfc, box, FloatDirection.LEFT);
            save(box, layer, bfc, FloatDirection.LEFT);
        } else if (box.getStyle().isFloatedRight()) {
            position(c, bfc, box, FloatDirection.RIGHT);
            save(box, layer, bfc, FloatDirection.RIGHT);
        }
    }

    public void clear(CssContext cssCtx, BlockFormattingContext bfc, Box box) {
        if (box.getStyle().isClearLeft()) {
            moveClear(cssCtx, bfc, box, getFloats(FloatDirection.LEFT));
        }
        if (box.getStyle().isClearRight()) {
            moveClear(cssCtx, bfc, box, getFloats(FloatDirection.RIGHT));
        }
    }

    private void save(
            BlockBox current,
            Layer layer,
            BlockFormattingContext bfc,
            FloatDirection direction) {

        Point p = bfc.getOffset();
        getAddableFloats(direction).add(new BoxOffset(current, p.x, p.y));
        layer.addFloat(current, bfc);

        current.getFloatedBoxData().setManager(this);
        current.calcCanvasLocation();
        current.calcChildLocations();
    }

    private void position(CssContext cssCtx, BlockFormattingContext bfc,
                          BlockBox current, FloatDirection direction) {
        moveAllTheWayOver(current, direction);

        alignToLastOpposingFloat(cssCtx, bfc, current, direction);
        alignToLastFloat(cssCtx, bfc, current, direction);

        if (!fitsInContainingBlock(current) ||
                overlaps(cssCtx, bfc, current, getFloats(direction))) {
            moveAllTheWayOver(current, direction);
            moveFloatBelow(cssCtx, bfc, current, getFloats(direction));
        }

        if (overlaps(cssCtx, bfc, current, getOpposingFloats(direction))) {
            moveAllTheWayOver(current, direction);
            moveFloatBelow(cssCtx, bfc, current, getFloats(direction));
            moveFloatBelow(cssCtx, bfc, current, getOpposingFloats(direction));
        }

        if (current.getStyle().isCleared()) {
            if (current.getStyle().isClearLeft() && direction == FloatDirection.LEFT) {
                moveAllTheWayOver(current, FloatDirection.LEFT);
            } else if (current.getStyle().isClearRight() && direction == FloatDirection.RIGHT) {
                moveAllTheWayOver(current, FloatDirection.RIGHT);
            }
            moveFloatBelow(cssCtx, bfc, current, getFloats(direction));
        }
    }

    public List<BoxOffset> getFloats(FloatDirection direction) {
        return direction == FloatDirection.LEFT ? _leftFloats : _rightFloats;
    }

    public Stream<BoxOffset> getFloatStream(FloatDirection direction) {
        return getFloats(direction).stream();
    }

    private List<BoxOffset> getOpposingFloats(FloatDirection direction) {
        return direction == FloatDirection.LEFT ? _rightFloats : _leftFloats;
    }

    private void alignToLastFloat(
            CssContext cssCtx,
            BlockFormattingContext bfc,
            BlockBox current,
            FloatDirection direction) {

        List<BoxOffset> floats = getFloats(direction);
        if (floats.size() > 0) {
            Point offset = bfc.getOffset();
            BoxOffset lastOffset = floats.get(floats.size() - 1);
            BlockBox last = lastOffset.getBox();

            Rectangle currentBounds = current.getMarginEdge(cssCtx, -offset.x, -offset.y);

            Rectangle lastBounds = last.getMarginEdge(cssCtx, -lastOffset.getX(), -lastOffset.getY());

            boolean moveOver = false;

            if (currentBounds.y < lastBounds.y) {
                currentBounds.translate(0, lastBounds.y - currentBounds.y);
                moveOver = true;
            }

            if (currentBounds.y >= lastBounds.y && currentBounds.y < lastBounds.y + lastBounds.height) {
                moveOver = true;
            }

            if (moveOver) {
                if (direction == FloatDirection.LEFT) {
                    currentBounds.x = lastBounds.x + last.getWidth();
                } else if (direction == FloatDirection.RIGHT) {
                    currentBounds.x = lastBounds.x - current.getWidth();
                }

                currentBounds.translate(offset.x, offset.y);

                current.setX(currentBounds.x);
                current.setY(currentBounds.y);
            }
        }
    }

    private void alignToLastOpposingFloat(
            CssContext cssCtx,
            BlockFormattingContext bfc, BlockBox current, FloatDirection direction) {

        List<BoxOffset> floats = getOpposingFloats(direction);
        if (!floats.isEmpty()) {
            Point offset = bfc.getOffset();
            BoxOffset lastOffset = floats.get(floats.size() - 1);

            Rectangle currentBounds = current.getMarginEdge(cssCtx, -offset.x, -offset.y);

            Rectangle lastBounds = lastOffset.getBox().getMarginEdge(cssCtx,
                    -lastOffset.getX(), -lastOffset.getY());

            if (currentBounds.y < lastBounds.y) {
                currentBounds.translate(0, lastBounds.y - currentBounds.y);

                currentBounds.translate(offset.x, offset.y);

                current.setY(currentBounds.y);
            }
        }
    }

    private void moveAllTheWayOver(BlockBox current, FloatDirection direction) {
        if (direction == FloatDirection.LEFT) {
            current.setX(0);
        } else if (direction == FloatDirection.RIGHT) {
            current.setX(current.getContainingBlock().getContentWidth() - current.getWidth());
        }
    }

    private boolean fitsInContainingBlock(BlockBox current) {
        return current.getX() >= 0 &&
                (current.getX() + current.getWidth()) <= current.getContainingBlock().getContentWidth();
    }

    private int findLowestY(CssContext cssCtx, List<BoxOffset> floats) {
        return floats
               .stream()
               .map(floater -> floater.getBox().getMarginEdge(cssCtx, -floater.getX(), -floater.getY()))
               .mapToInt(bounds -> bounds.y + bounds.height)
               .max()
               .orElse(0);
    }

    public int getClearDelta(CssContext cssCtx, int bfcRelativeY) {
        int lowestLeftY = findLowestY(cssCtx, getFloats(FloatDirection.LEFT));
        int lowestRightY = findLowestY(cssCtx, getFloats(FloatDirection.RIGHT));

        int lowestY = Math.max(lowestLeftY, lowestRightY);

        return lowestY - bfcRelativeY;
    }

    private boolean overlaps(CssContext cssCtx, BlockFormattingContext bfc,
                             BlockBox current, List<BoxOffset> floats) {
        Point offset = bfc.getOffset();
        Rectangle bounds = current.getMarginEdge(cssCtx, -offset.x, -offset.y);

        return floats
               .stream()
               .map(floater -> floater.getBox().getMarginEdge(cssCtx, -floater.getX(), -floater.getY()))
               .anyMatch(floaterBounds -> floaterBounds.intersects(bounds));
    }

    private void moveFloatBelow(CssContext cssCtx, BlockFormattingContext bfc,
                                   Box current, List<BoxOffset> floats) {
        if (floats.isEmpty()) {
            return;
        }

        Point offset = bfc.getOffset();
        int boxY = current.getY() - offset.y;
        int floatY = findLowestY(cssCtx, floats);

        if (floatY - boxY > 0) {
            current.setY(current.getY() + (floatY - boxY));
        }
    }

    private void moveClear(CssContext cssCtx, BlockFormattingContext bfc,
                           Box current, List<BoxOffset> floats) {
        if (floats.isEmpty()) {
            return;
        }

        // Translate from box coords to BFC coords
        Point offset = bfc.getOffset();
        Rectangle bounds = current.getBorderEdge(
                current.getX()-offset.x, current.getY()-offset.y, cssCtx);

        int y = findLowestY(cssCtx, floats);

        if (bounds.y < y) {
            // Translate bottom margin edge of lowest float back to box coords
            // and set the box's border edge to that value
            bounds.y = y;

            bounds.translate(offset.x, offset.y);

            current.setY(bounds.y - (int)current.getMargin(cssCtx).top());
        }
    }

    public void removeFloat(BlockBox floater) {
        removeFloat(floater, getFloats(FloatDirection.LEFT));
        removeFloat(floater, getFloats(FloatDirection.RIGHT));
    }

    private void removeFloat(BlockBox floater, List<BoxOffset> floats) {
        for (Iterator<BoxOffset> i = floats.iterator(); i.hasNext();) {
            BoxOffset boxOffset = i.next();
            if (boxOffset.getBox().equals(floater)) {
                i.remove();
                floater.getFloatedBoxData().setManager(null);
            }
        }
    }

    public void calcFloatLocations() {
        calcFloatLocations(getFloats(FloatDirection.LEFT));
        calcFloatLocations(getFloats(FloatDirection.RIGHT));
    }

    private void calcFloatLocations(List<BoxOffset> floats) {
        for (BoxOffset boxOffset : floats) {
            boxOffset.getBox().calcCanvasLocation();
            boxOffset.getBox().calcChildLocations();
        }
    }

    private void applyLineHeightHack(CssContext cssCtx, Box line, Rectangle bounds) {
        // this is a hack to deal with lines w/o width or height. is this valid?
        // possibly, since the line doesn't know how long it should be until it's already
        // done float adjustments
        if (line.getHeight() == 0) {
            bounds.height = (int)line.getStyle().getLineHeight(cssCtx);
        }
    }

    public int getNextLineBoxDelta(CssContext cssCtx, BlockFormattingContext bfc,
            LineBox line, int containingBlockContentWidth) {
        BoxDistance left = getFloatDistance(cssCtx, bfc, line, containingBlockContentWidth, getFloats(FloatDirection.LEFT), FloatDirection.LEFT);
        BoxDistance right = getFloatDistance(cssCtx, bfc, line, containingBlockContentWidth, getFloats(FloatDirection.RIGHT), FloatDirection.RIGHT);

        int leftDelta = left.getBox() != null ? calcDelta(cssCtx, line, left) : 0;
        int rightDelta = right.getBox() != null ? calcDelta(cssCtx, line, right) : 0;

        return Math.max(leftDelta, rightDelta);
    }

    private int calcDelta(CssContext cssCtx, LineBox line, BoxDistance boxDistance) {
        BlockBox floated = boxDistance.getBox();

        Rectangle rect = floated.getBorderEdge(floated.getAbsX(), floated.getAbsY(), cssCtx);
        RectPropertySet margin = floated.getMargin(cssCtx);

        // NOTE: This was not taking account of margins and thus was not clearing properly.
        // See: https://github.com/danfickle/openhtmltopdf/pull/618
        int bottom = (int) Math.ceil(rect.y + rect.height + margin.bottom());

        return bottom - line.getAbsY();
    }

    public int getLeftFloatDistance(CssContext cssCtx, BlockFormattingContext bfc,
            LineBox line, int containingBlockContentWidth) {
        return getFloatDistance(cssCtx, bfc, line, containingBlockContentWidth, getFloats(FloatDirection.LEFT), FloatDirection.LEFT).getDistance();
    }

    public int getRightFloatDistance(CssContext cssCtx, BlockFormattingContext bfc,
            LineBox line, int containingBlockContentWidth) {
        return getFloatDistance(cssCtx, bfc, line, containingBlockContentWidth, getFloats(FloatDirection.RIGHT), FloatDirection.RIGHT).getDistance();
    }

    private BoxDistance getFloatDistance(
            CssContext cssCtx,
            BlockFormattingContext bfc,
            LineBox line,
            int containingBlockContentWidth,
            List<BoxOffset> floatsList,
            FloatDirection direction) {

        if (floatsList.isEmpty()) {
            return new BoxDistance(null, 0);
        }

        Point offset = bfc.getOffset();
        Rectangle lineBounds = line.getMarginEdge(cssCtx, -offset.x, -offset.y);
        lineBounds.width = containingBlockContentWidth;

        int farthestOver = direction == FloatDirection.LEFT ? lineBounds.x : lineBounds.x + lineBounds.width;

        applyLineHeightHack(cssCtx, line, lineBounds);

        BlockBox farthestOverBox = null;

        for (BoxOffset floater : floatsList) {
            Rectangle fr = floater.getBox().getMarginEdge(cssCtx, -floater.getX(), -floater.getY());

            if (lineBounds.intersects(fr)) {
                if (direction == FloatDirection.LEFT && fr.x + fr.width > farthestOver) {
                    farthestOver = fr.x + fr.width;
                } else if (direction == FloatDirection.RIGHT && fr.x < farthestOver) {
                    farthestOver = fr.x;
                }

                farthestOverBox = floater.getBox();
            }
        }

        if (direction == FloatDirection.LEFT) {
            return new BoxDistance(farthestOverBox, farthestOver - lineBounds.x);
        } else {
            return new BoxDistance(farthestOverBox, lineBounds.x + lineBounds.width - farthestOver);
        }
    }

    public Box getMaster() {
        return _master;
    }

    public Point getOffset(BlockBox floater) {
        // FIXME inefficient (but probably doesn't matter)
        return getOffset(floater, 
                        getFloatStream(floater.getStyle().isFloatedLeft() ? FloatDirection.LEFT : FloatDirection.RIGHT));
    }

    private Point getOffset(BlockBox floater, Stream<BoxOffset> floats) {
        return floats.filter(boxOffset -> boxOffset.getBox().equals(floater))
                     .findFirst()
                     .map(boxOffset -> new Point(boxOffset.getX(), boxOffset.getY()))
                     .orElse(null);
    }

    private void performFloatOperation(FloatOperation op, List<BoxOffset> floats) {
        for (BoxOffset boxOffset : floats) {
            BlockBox box = boxOffset.getBox();

            box.setAbsX(box.getX() + getMaster().getAbsX() - boxOffset.getX());
            box.setAbsY(box.getY() + getMaster().getAbsY() - boxOffset.getY());

            op.operate(box);
        }
    }

    public void performFloatOperation(FloatOperation op) {
        performFloatOperation(op, getFloats(FloatDirection.LEFT));
        performFloatOperation(op, getFloats(FloatDirection.RIGHT));
    }

    public static class BoxOffset {
        private final BlockBox _box;
        private final int _x;
        private final int _y;

        public BoxOffset(BlockBox box, int x, int y) {
            _box = box;
            _x = x;
            _y = y;
        }

        public BlockBox getBox() {
            return _box;
        }

        public int getX() {
            return _x;
        }

        public int getY() {
            return _y;
        }
    }

    private static class BoxDistance {
        private final BlockBox _box;
        private final int _distance;

        public BoxDistance(BlockBox box, int distance) {
            _box = box;
            _distance = distance;
        }

        BlockBox getBox() {
            return _box;
        }

        int getDistance() {
            return _distance;
        }

        @Override
        public String toString() {
            return "BoxDistance [_box=" + _box + ", _distance=" + _distance + "]";
        }
    }

    @FunctionalInterface
    public interface FloatOperation {
        public void operate(Box floater);
    }
}
