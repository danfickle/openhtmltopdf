package org.xhtmlrenderer.layout;


import org.xhtmlrenderer.render.*;
import java.awt.Point;
import java.awt.Rectangle;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.util.u;
import org.xhtmlrenderer.render.Box;



public class DefaultLayout implements Layout {
    public int contents_height;

    /* ============= layout code =================== */

    public Box layout(Context c, Element elem) {
        //u.p("Layout.layout(): " + elem);
        Box box = createBox(c,elem);
        return layoutChildren(c, box);
    }

    public Box createBox(Context c, Node node) {
        Box box = new Box();
        box.node = node;
        return box;
    }

    /*
    // we need to pass the graphics incase we need to grab font info for sizing
    public Rectangle layoutNode(Context c, Node node) {
        return new Rectangle(0,0);
    }
    */

    public Box layoutChildren(Context c, Box box) {
        //u.p("Layout.layoutChildren: " + box);

        Element elem = (Element)box.node;
        // prepare for the list items
        int old_counter = c.getListCounter();
        c.setListCounter(0);

        // for each child
        NodeList nl = elem.getChildNodes();
        for(int i=0; i<nl.getLength(); i++) {
            Node child = nl.item(i);

            // get the layout for this child
            Layout layout = getLayout(child);
            if(layout == null) { continue; }
            if(layout instanceof NullLayout) { continue; }
            if(child.getNodeName().equals("br")) { continue; }
            if(child.getNodeType() == child.COMMENT_NODE) { continue; }

            Box child_box = null;
            if(child.getNodeType() == child.ELEMENT_NODE) {
                // update the counter for printing OL list items
                c.setListCounter(c.getListCounter()+1);
                //u.p("elem = " + child.getNodeName() + "  counter = " + c.getListCounter());
                
                Element child_elem = (Element)child;
                // execute the layout and get the return bounds
                //u.p("doing element layout: " + layout);
                c.parent_box = box;
                c.placement_point = new Point(0,box.height);
                child_box = layout.layout(c,child_elem);
                child_box.list_count = c.getListCounter();
                //u.p("child box = " + child_box);
            } else {
                //u.p("we have to do an anonymous text block on this: " + child.getNodeValue());
                // create anonymous block box
                // prepare the node list of the text children
                //child_box = new AnonymousBlockBox(child);
                // call layout
                //u.p("layout = " + layout);
                //u.p("doing non element layout: " + layout);
                child_box = ((AnonymousBoxLayout)layout).layout(c,elem,child);

                // skip text children if the prev_child == anonymous block box
                // because that means they were sucked into this block
                Node last_node = ((AnonymousBlockBox)child_box).last_node;
                // if anonymous box is only one node wide then skip this
                // junk
                if(child != last_node) {
                    while(true) {
                        i++;
                        Node ch = nl.item(i);
                        //u.p("trying to skip: " + ch);
                        if(ch == last_node) {
                            break;
                        }
                    }
                }



            }
            box.addChild(child_box);
            // set the child_box location
            child_box.x = 0;
            child_box.y = box.height;

            //joshy fix the 'fixed' stuff later
            // if fixed then don't modify the final layout bounds
            // because fixed elements are removed from normal flow
            if(child_box.fixed) {
                continue;
            }


            // increase the final layout width if the child was greater
            if(child_box.width > box.width) {
                box.width = child_box.width;
            }

            // increase the final layout height by the height of the child
            box.height += child_box.height;
            //u.p("final extents = " + lt);
            //u.p("final child box was: " + child_box);
        }
        c.addMaxWidth(box.width);
        
        c.setListCounter(old_counter);

        return box;
    }



    /* ========== painting code ============== */
    // the core function that implements the recursive layout/paint loop
    // perhaps we should call it something else?
    public void paint(Context c, Box box) {
        //u.p("Layout.paint() " + box);
        //Point old_cursor = new Point(c.getCursor());
        //Rectangle contents = layoutChildren(c,elem);
        //c.cursor = old_cursor;
        paintBackground(c,box);
        paintComponent(c,box);
        paintChildren(c,box);
        paintBorder(c,box);
        this.contents_height = box.height;
    }

    public void paintBackground(Context c, Box box) { }
    public void paintComponent(Context c, Box box) { }
    public void paintBorder(Context c, Box box) { }

    public void paintChildren(Context c, Box box) {
        //u.p("Layout.paintChildren(): " + box);
        //u.p("child count = " + box.boxes.size());
        for(int i=0; i<box.getChildCount(); i++) {
            Box child = (Box)box.getChild(i);
            //u.p("child = " + child);
            Layout layout = null;
            if(child.isAnonymous()) {
                layout = new InlineLayout();
            } else {
                layout = getLayout(child.node);
            }
            paintChild(c,child,layout);
        }
    }

    public void paintChild(Context c, Box box, Layout layout) {
        layout.paint(c,box);
    }


    /* =========== utility code ============= */
    public Layout getLayout(Node node) {
        return LayoutFactory.getLayout(node);
    }

    public static String getPosition(Context c, Box box) {
        String position = c.css.getStringProperty(box.node,"position",false);
        if(position == null) {
            position = "static";
        }
        return position;
    }

    public boolean isFixed(Context c, Box box) {
        if(getPosition(c,box).equals("fixed")) {
            return true;
        }
        return false;
    }


    public static boolean isFloated(Box inline, Context c) {
        return isFloated(inline.node,c);
    }
    public static boolean isFloated(Node node, Context c) {
        String float_val = c.css.getStringProperty(node,"float");
        if(float_val == null) { return false; }
        if(float_val.equals("left")) { return true; }
        if(float_val.equals("right")) { return true; }
        return false;
    }

    public boolean isBlockLayout(Element elem, Context c) {
        NodeList children = elem.getChildNodes();
        for(int i=0; i<children.getLength(); i++) {
            Node child = children.item(i);
            if(isBlockNode(child,c)) {
                //u.p("this layout is block");
                return true;
            }
        }
        return false;
    }

    public static boolean isBlockNode(Node child, Context c) {
        if(child instanceof Element) {
            Element el = (Element)child;
            String display = c.css.getStringProperty(el,"display",false);
            //u.p("display = " + display);
            if(display != null && display.equals("block")) {
                if(!isFloated(el,c)) {
                    //u.p(child.getNodeName() + " is a block");
                    return true;
                } else {
                    //u.p("isBlockNode() found a floated block");
                }
            }
        }
        return false;
    }

    public static boolean isHiddenNode(Node child, Context c) {
        if(child instanceof Element) {
            Element el = (Element)child;
            String display = c.css.getStringProperty(el,"display",false);
            //u.p("display = " + display);
            if(display != null && display.equals("none")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReplaced(Node node) {
        // all images are replaced (because they have intrinsic sizes)
        if(node.getNodeName().equals("img")) {
            return true;
        }
        if(node.getNodeName().equals("select")) {
            return true;
        }
        if(node.getNodeName().equals("textarea")) {
            return true;
        }
        // all input elements are replaced except for hidden forms
        if(node.getNodeName().equals("input")) {
            Element el = (Element)node;
            // skip hidden forms. they aren't replaced
            if(el.getAttribute("type") != null
            && el.getAttribute("type").equals("hidden")) {
                return false;
            }
            return true;
        }

        return false;
    }

    public static boolean isFloatedBlock(Node node, Context c) {
        if(node.getNodeType() != node.ELEMENT_NODE) {
            return false;
        }

        Element el = (Element)node;
        String display = c.css.getStringProperty(el,"display",false);
        //u.p("display = " + display);
        if(display != null && display.equals("block")) {
            if(isFloated(node,c)) {
                //u.p("it's a floated block");
                return true;
            }
        }
        return false;
    }

}
