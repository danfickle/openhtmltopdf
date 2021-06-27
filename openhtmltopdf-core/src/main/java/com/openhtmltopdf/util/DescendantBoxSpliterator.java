package com.openhtmltopdf.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.InlineLayoutBox;

/**
 * A spliterator that can be used to create a stream of descendant boxes in
 * breadth first order.
 * <br><br>
 * NOTE: This is likely slower than simply creating a box list recursively for small
 * number of descendants but gets relatively faster (and uses less memory)
 * as the list of descendants increases in size.
 */
public class DescendantBoxSpliterator implements Spliterator<Box> {
    // A queue of boxes that we have to process their children.
    private final Deque<Box> unprocessed = new ArrayDeque<>();

    private State curState = State.BOX_CHILDREN;
    private int childIndex = 0;

    private enum State {
        BOX_CHILDREN,
        BLOCK_INLINE_CONTENT,
        ILB_INLINE_CHILDREN,
        DONE;
    }

    public DescendantBoxSpliterator(Box parent) {
        unprocessed.add(parent);
    }

    private boolean hasChildren(Box bx) {
        return
           bx.getChildCount() > 0 ||
           (bx instanceof BlockBox && !((BlockBox) bx).getInlineContent().isEmpty()) ||
           (bx instanceof InlineLayoutBox && ((InlineLayoutBox) bx).getInlineChildCount() > 0);
    }

    private void add(Box bx) {
        unprocessed.add(bx);
    }

    private void next() {
        setState(State.BOX_CHILDREN);
        unprocessed.remove();
    }

    private boolean hasNext() {
        return !unprocessed.isEmpty();
    }

    private Box current() {
        return unprocessed.peek();
    }

    private void setState(State newState) {
        curState = newState;
        childIndex = 0;
    }

    private Box getNext() {
        Box box = current();

        if (box == null) {
            return null;
        }

        switch (curState) {
        case BOX_CHILDREN: {
            if (childIndex < box.getChildCount()) {
                Box next = box.getChild(childIndex);
                childIndex++;

                if (hasChildren(next)) {
                    add(next);
                }

                return next;
            }

            setState(State.BLOCK_INLINE_CONTENT);
            // FALL-THRU
        }

        case BLOCK_INLINE_CONTENT: {
            if (box instanceof BlockBox) {
                BlockBox block = (BlockBox) box;
                Box next = getNextInlineChild(block.getInlineContent());

                if (next != null) {
                    return next;
                }
            }

            setState(State.ILB_INLINE_CHILDREN);
            // FALL-THRU
        }

        case ILB_INLINE_CHILDREN: {
            if (box instanceof InlineLayoutBox) {
                InlineLayoutBox ilb = (InlineLayoutBox) box;
                Box next = getNextInlineChild(ilb.getInlineChildren());

                if (next != null) {
                    return next;
                }
            }

            setState(State.DONE);
            // FALL-THRU
        }

        default:
            next();
            return null;
        }
    }

    private Box getNextInlineChild(List<? extends Object> inlineChilds) {
        if (inlineChilds == null) {
            return null;
        }

        while (childIndex < inlineChilds.size()) {
            Object inlineChild = inlineChilds.get(childIndex);
            childIndex++;

            if (inlineChild instanceof Box) {
                if (hasChildren((Box) inlineChild)) {
                    add((Box) inlineChild);
                }
                return (Box) inlineChild;
            }
        }

        return null;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Box> action) {
        Box next = getNext();

        if (next != null) {
            action.accept(next);
            return true;
        } else {
            while (hasNext()) {
                Box next2 = getNext();
                if (next2 != null) {
                    action.accept(next2);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Spliterator<Box> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return Spliterator.IMMUTABLE | Spliterator.NONNULL;
    }
}
