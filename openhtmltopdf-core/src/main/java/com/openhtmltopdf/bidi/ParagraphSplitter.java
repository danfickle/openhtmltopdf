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

import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.layout.LayoutContext;

/**
 * This class aims to split text into paragraphs where they can be passed to the
 * BidiSplitter. Each text node in the document is attached to the closest element with <code>display: block</code>
 * which we assume paragraphs do not cross.
 */
public class ParagraphSplitter {
	
	public static class Paragraph {
        private final StringBuilder builder = new StringBuilder();
        private final List<Text> textNodes = new ArrayList<Text>();
        
        private final TreeMap<Integer, BidiTextRun> splitPoints = new TreeMap<Integer, BidiTextRun>();
        
        private Paragraph() { }
        
        private void add(String text, Text textNode) {
            builder.append(text);
            textNodes.add(textNode);
        }
        
        private void runBidiSplitter(BidiSplitter splitter, LayoutContext c) {
        	splitter.setParagraph(builder.toString(), c.getDefaultTextDirection());
        	copySplitPointsFromBidiSplitter(splitter);
        }
         
        /**
         * @param text
         * @return the first index of text from a Text node.
         */
        public int getFirstCharIndexInParagraph(Text text) {
        	
        	int position = 0;
        	
            for (Text t : textNodes) {
            	if (text == t) {
            		return position;
            	}
            
            	position += t.getLength();
            }
        	
            assert(false);
        	return -1;
        }
        
        private void copySplitPointsFromBidiSplitter(BidiSplitter splitter) {
        	int length = splitter.countTextRuns();
        	
        	for (int i = 0; i < length; i++) {
        		BidiTextRun run = splitter.getVisualRun(i);
        		splitPoints.put(run.getStart(), run);
        	}
        }
        
        /**
         * @param startIndexInParagraph
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
         * @param startIndexInParagraph
         * @return the BidiTextRun that starts at or before startIndexInParagraph.
         */
        public BidiTextRun prevSplit(int startIndexInParagraph) {
            Map.Entry<Integer, BidiTextRun> entry = splitPoints.floorEntry(startIndexInParagraph);
        	
        	if (entry != null)
        		return entry.getValue();
        	else
        		return null;
        }
	}
	
    private final Map<Text, Paragraph> paragraphs = new HashMap<Text, Paragraph>();
    
    public Paragraph lookupParagraph(Text node) {
    	return paragraphs.get(node);
    }
    
    public void splitRoot(LayoutContext c, Document doc) {
        Paragraph parent = new Paragraph();
        splitParagraphs(c, doc, parent);
	}

    /**
     * Run bidi splitting on the document's paragraphs.
     * @param c
     */
    public void runBidiOnParagraphs(LayoutContext c) {
    	for (Paragraph p : paragraphs.values())
    	{
    		p.runBidiSplitter(c.getBidiSplitterFactory().createBidiSplitter(), c);
    	}
    }
    
    private void splitParagraphs(LayoutContext c, Node parent, Paragraph nearestBlock) {
        Node node = parent.getFirstChild();
        
        if (node == null) {
            return;
        }
        
    	do {
            if (node.getNodeType() == Node.TEXT_NODE) {
                String text = ((Text) node).getData();
            	nearestBlock.add(text, (Text) node);
            	paragraphs.put((Text) node, nearestBlock);
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE) {
            	Element element = (Element) node;
                CalculatedStyle style = c.getSharedContext().getStyle(element);
            	
                if (style.isSpecifiedAsBlock()) {
                    splitParagraphs(c, element, new Paragraph());
                }
                else {
                    splitParagraphs(c, element, nearestBlock);
                }
            }
        } while ((node = node.getNextSibling()) != null);
    }
}
