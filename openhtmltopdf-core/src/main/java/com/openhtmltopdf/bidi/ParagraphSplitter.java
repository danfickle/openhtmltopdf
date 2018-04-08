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
 * BidiSplitter. Each text node in the document is attached to the closest block-like element
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
	 * A paragraph object collects the text of one paragraph.
	 * That is the text in a block element wiht possible holes from BIDI isolation tags.
	 * This text is then used to run the Unicode BIDI algorithm splitting text
	 * up into runs of LTR and RTL text.
	 */
	public static class Paragraph {

		private final StringBuilder builder;
        private final TreeMap<Integer, BidiTextRun> splitPoints;
        
        // A map from Text nodes to their first index in the paragraph.
        protected final Map<Text, Integer> textRuns = new HashMap<Text, Integer>();

        // One of LTR, RTL or AUTO.
        protected final IdentValue cssDirection;
        
        private byte actualDirection = BidiSplitter.LTR;
        
        private Paragraph(IdentValue direction) {
        	this(direction, true);
        }
        
        private Paragraph(IdentValue direction, boolean isLiveImplementation) {
        	this.builder = isLiveImplementation ? new StringBuilder() : null;
        	this.splitPoints = isLiveImplementation ? new TreeMap<Integer, BidiTextRun>() : null;
        	this.cssDirection = direction;
        }
        
        /**
         * Here we add a textnode and its postion to a list. We also build the paragraph string.
         */
        protected void add(String text, Text textNode) {
            int startIndex = builder.length();
            builder.append(text);
            textRuns.put(textNode, startIndex);
        }
        
        /**
         * Here we call out to the actual BIDI algorithm.
         */
        protected void runBidiSplitter(BidiSplitter splitter, LayoutContext c) {
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
        	return textRuns.get(text);
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
	
	/**
	 * A fake paragraqph only supports manual BIDI classification.
	 */
	public static class FakeParagraph extends Paragraph {
        private int characterCount = 0;
        private BidiTextRun fakeRun;
        
        private FakeParagraph(IdentValue direction) {
        	super(direction, false);
        }
        
        @Override
        protected void add(String text, Text textNode) {
            textRuns.put(textNode, characterCount);
            characterCount += text.length();
        }
        
        @Override
        public byte getActualDirection() {
        	return cssDirection == IdentValue.RTL ? BidiSplitter.RTL : BidiSplitter.LTR; 
        }
        
        @Override
        public BidiTextRun nextSplit(int startIndexInParagraph) {
        	return null;
        }
        
        @Override
        public BidiTextRun prevSplit(int startIndexInParagraph) {
        	return fakeRun;
        }
        
        @Override
        protected void runBidiSplitter(BidiSplitter splitter, LayoutContext c) {
        	this.fakeRun = new BidiTextRun(0, characterCount, this.getActualDirection());
        }
	}
	
	private final List<Paragraph> allParagraphs = new ArrayList<Paragraph>();
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
        Paragraph parent = c.getBidiReorderer().isLiveImplementation() ? new Paragraph(direction) : new FakeParagraph(direction);
        splitParagraphs(c, doc, parent);
	}

    /**
     * Run bidi splitting on the document's paragraphs.
     */
    public void runBidiOnParagraphs(LayoutContext c) {
    	if (c.getBidiReorderer().isLiveImplementation()) {
    	    for (Paragraph p : allParagraphs)
    	    {
    		    p.runBidiSplitter(c.getBidiSplitterFactory().createBidiSplitter(), c);
    	    }
    	} else {
    		for (Paragraph p : allParagraphs)
    	    {
    		    p.runBidiSplitter(null, c);
    	    }
    	}
    }
    
    /**
     * Here we recursively split everything into paragraphs.
     */
    private void splitParagraphs(LayoutContext c, Node parent, Paragraph nearestBlock) {
        Node node = parent.getFirstChild();
        boolean isLiveImplementaton = c.getBidiReorderer().isLiveImplementation();
        
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
            	
                if (style.isParagraphContainerForBidi() || element.hasAttribute("dir") || element.getNodeName().equals("bdi")) {
                	// If a element has a dir attribute or is a bdi tag it sits in its own direction isolate.
                	Paragraph para = isLiveImplementaton ? new Paragraph(style.getDirection()) : new FakeParagraph(style.getDirection());
                	allParagraphs.add(para);
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
