package com.openhtmltopdf.bidi;

import java.util.ArrayList;
import java.util.Collections;
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
import com.openhtmltopdf.extend.ReplacedElementFactory;
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
         * @return the first char index into this paragraph from a Text node or -1 if not available.
         */
        public int getFirstCharIndexInParagraph(Text text) {
        	if (textRuns.isEmpty()) {
        		return -1;
        	}
        	
        	Integer trun = textRuns.get(text);
        	return trun == null ? -1 : trun;
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
        private FakeParagraph(IdentValue direction) {
        	super(direction, false);
        }
        
        @Override
        protected void add(String text, Text textNode) {
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
        	return null;
        }
        
        @Override
        protected void runBidiSplitter(BidiSplitter splitter, LayoutContext c) {
        }
	}
	
	private List<Paragraph> allParagraphs;
    private Map<Text, Paragraph> paragraphs;
    private Map<Element, Paragraph> blocks;
    
    /**
     * Get the paragraph object that a Text node is associated with.
     * Should never return null.
     */
    public Paragraph lookupParagraph(Text node) {
    	return paragraphs.isEmpty() ? allParagraphs.get(0) : paragraphs.get(node);
    }
    
    public Paragraph lookupBlockElement(Element elem) {
    	return blocks.isEmpty() ? allParagraphs.get(0) : blocks.get(elem);
    }
    
    /**
     * This starts everything by recursively dividing the document into paragraphs.
     */
    public void splitRoot(LayoutContext c, Document doc) {
    	boolean isLiveImplementation = c.getBidiReorderer().isLiveImplementation();
    	CalculatedStyle style = c.getSharedContext().getStyle(doc.getDocumentElement());
    	IdentValue direction = style.getDirection();
        Paragraph parent = isLiveImplementation ? new Paragraph(direction) : new FakeParagraph(direction);
        
        if (isLiveImplementation) {
        	allParagraphs = new ArrayList<Paragraph>();
        	paragraphs = new HashMap<Text, Paragraph>();
        	blocks = new HashMap<Element, Paragraph>();
        	
        	splitParagraphs(c, doc, parent);
        } else {
        	allParagraphs = Collections.singletonList(parent);
        	paragraphs = Collections.emptyMap();
        	blocks = Collections.emptyMap();
        }
	}

    /**
     * Run bidi splitting on the document's paragraphs.
     */
    public void runBidiOnParagraphs(LayoutContext c) {
   	    for (Paragraph p : allParagraphs)
   	    {
   		    p.runBidiSplitter(c.getBidiSplitterFactory().createBidiSplitter(), c);
   	    }
    }
    
    /**
     * Here we recursively split everything into paragraphs.
     */
    private void splitParagraphs(LayoutContext c, Node parent, Paragraph nearestBlock) {
    	ReplacedElementFactory reFactory = c.getReplacedElementFactory();
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
            	
            	if (element.getNodeName().equals("head") ||
            		reFactory.isReplacedElement(element)) {
            		continue;
            	}

            	CalculatedStyle style = c.getSharedContext().getStyle(element);
                
            	if (style.isParagraphContainerForBidi() || element.hasAttribute("dir") || element.getNodeName().equals("bdi")) {
                	// If a element has a dir attribute or is a bdi tag it sits in its own direction isolate.
                	Paragraph para = new Paragraph(style.getDirection());
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
