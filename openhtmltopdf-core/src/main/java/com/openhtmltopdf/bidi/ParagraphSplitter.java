package com.openhtmltopdf.bidi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.layout.LayoutContext;

/**
 * This class aims to split text into paragraphs where they can be passed to the
 * BidiSplitter. Each text node in the document is attached to the closest element with <code>display: block</code>
 * which we assume paragraphs do not cross.
 */
public class ParagraphSplitter {
	
    public static byte convertDirectionIdent(IdentValue ident) {
    	if (ident == IdentValue.RTL) {
    		return BidiSplitter.RTL;
    	} else {
    		return BidiSplitter.LTR;
    	}
    }
	
	/**
	 * A text run is just a text dom element and its associated position in the extracted 
	 * paragraph text. 
	 */
	public static class TextRun {
		private final Text domText;
		private final int startIndexInParagraph;
		@SuppressWarnings("unused")
		private final int endIndexInParagraph;
		
		TextRun(Text domText, int startIndexInParagraph, int endIndexInParagraph) {
			this.domText = domText;
			this.startIndexInParagraph = startIndexInParagraph;
			this.endIndexInParagraph = endIndexInParagraph;
		}
	}
	
	/**
	 * A paragraph object collects the text of one paragraph.
	 * That is the text in a block element wiht possible holes from BIDI isolation tags.
	 * This text is then used to run the Unicode BIDI algorithm splitting text
	 * up into runs of LTR and RTL text.
	 */
	public static class Paragraph {
        private final StringBuilder builder = new StringBuilder();
        private final List<TextRun> textRuns = new ArrayList<TextRun>();
        private final TreeMap<Integer, BidiTextRun> splitPoints = new TreeMap<Integer, BidiTextRun>();
        private final IdentValue cssDirection; // One of LTR, RTL or AUTO.
        private byte actualDirection = BidiSplitter.LTR;
        
        private Paragraph(IdentValue direction) {
        	this.cssDirection = direction;
        }
        
        /**
         * Here we add a textnode and its postion to a list. We also build the paragraph string.
         */
        private void add(String text, Text textNode) {
            int startIndex = builder.length();
            builder.append(text);
            int endIndex = builder.length();
           
            textRuns.add(new TextRun(textNode, startIndex, endIndex));
        }
        
        /**
         * Here we call out to the actual BIDI algorithm.
         */
        private void runBidiSplitter(BidiSplitter splitter, LayoutContext c) {
        	byte defaultDirection = BidiSplitter.LTR;
        	String para = builder.toString();
        	
        	if (cssDirection == IdentValue.RTL) {
        		defaultDirection = BidiSplitter.RTL;
        	} else if (cssDirection == IdentValue.AUTO) {
        		defaultDirection = splitter.getBaseDirection(para);
        	}

        	this.actualDirection = defaultDirection == BidiSplitter.NEUTRAL ? BidiSplitter.LTR : defaultDirection;
        	splitter.setParagraph(para, actualDirection);
        	copySplitPointsFromBidiSplitter(splitter);
        }
         
        /**
         * @return the first char index into this paragraph from a Text node.
         */
        public int getFirstCharIndexInParagraph(Text text) {
            for (TextRun t : textRuns) {
                if (text == t.domText) {
                    return t.startIndexInParagraph;
                }
            }
        	
            assert(false);
        	return -1;
        }
        
        /**
         * Here we copy the split points from the BIDI processor to our tree map for easy access.
         */
        private void copySplitPointsFromBidiSplitter(BidiSplitter splitter) {
        	int length = splitter.countTextRuns();
        	
        	for (int i = 0; i < length; i++) {
        		BidiTextRun run = splitter.getVisualRun(i);
        		splitPoints.put(run.getStart(), run);
        	}
        }
        
        /**
         * @return the BidiTextRun that starts at or above startIndexInPararagraph.
         */
        public BidiTextRun nextSplit(int startIndexInParagraph) {
            Map.Entry<Integer, BidiTextRun> entry = splitPoints.ceilingEntry(startIndexInParagraph);
        	
        	if (entry != null)
        		return entry.getValue();
        	else
        		return null;
        }

        /**
         * @return the BidiTextRun that starts at or before startIndexInParagraph.
         */
        public BidiTextRun prevSplit(int startIndexInParagraph) {
            Map.Entry<Integer, BidiTextRun> entry = splitPoints.floorEntry(startIndexInParagraph);
        	
        	if (entry != null)
        		return entry.getValue();
        	else
        		return null;
        }
        
        public byte getActualDirection() {
        	return this.actualDirection;
        }
        
        public IdentValue getCSSDirection() {
        	return cssDirection;
        }
	}
	
    private final Map<Text, Paragraph> paragraphs = new HashMap<Text, Paragraph>();
    private final Map<Element, Paragraph> blocks = new HashMap<Element, Paragraph>();
    
    /**
     * Get the paragraph object that a Text node is associated with.
     * Should never return null.
     */
    public Paragraph lookupParagraph(Text node) {
    	return paragraphs.get(node);
    }
    
    public Paragraph lookupBlockElement(Element elem) {
    	return blocks.get(elem);
    }
    
    /**
     * This starts everything by recursively dividing the document into paragraphs.
     */
    public void splitRoot(LayoutContext c, Document doc) {
    	CalculatedStyle style = c.getSharedContext().getStyle(doc.getDocumentElement());
    	IdentValue direction = style.getDirection();
        Paragraph parent = new Paragraph(direction);
        splitParagraphs(c, doc, parent);
	}

    /**
     * Run bidi splitting on the document's paragraphs.
     */
    public void runBidiOnParagraphs(LayoutContext c) {
    	for (Paragraph p : paragraphs.values())
    	{
    		p.runBidiSplitter(c.getBidiSplitterFactory().createBidiSplitter(), c);
    	}
    }
    
    /**
     * Here we recursively split everything into paragraphs.
     */
    private void splitParagraphs(LayoutContext c, Node parent, Paragraph nearestBlock) {
        Node node = parent.getFirstChild();
        
        if (node == null) {
            return;
        }
        
    	do {
            if (node.getNodeType() == Node.TEXT_NODE
                || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                String text = ((Text) node).getData();
                nearestBlock.add(text, (Text) node);
                paragraphs.put((Text) node, nearestBlock);
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE) {
            	Element element = (Element) node;
            	CalculatedStyle style = c.getSharedContext().getStyle(element);
            	
            	if (element.getNodeName().equals("head")) {
            		continue;
            	}
            	
                if (style.isSpecifiedAsBlock() || element.hasAttribute("dir") || element.getNodeName().equals("bdi")) {
                	// If a element has a dir attribute or is a bdi tag it sits in its own direction isolate.
                	Paragraph para = new Paragraph(style.getDirection());
              		blocks.put(element, para);
                	splitParagraphs(c, element, para);
                }
                else {
                	// Else the element forms part of this directional block.
                	blocks.put(element, nearestBlock);
                    splitParagraphs(c, element, nearestBlock);
                }
            }
        } while ((node = node.getNextSibling()) != null);
    }
}
