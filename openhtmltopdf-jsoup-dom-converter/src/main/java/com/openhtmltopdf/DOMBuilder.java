package com.openhtmltopdf;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
*
* @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
*
*/
public class DOMBuilder {
  private DOMBuilder() { }
  
  /**
   * Returns a W3C DOM that exposes the same content as the supplied Jsoup
   * document into a W3C DOM.
   *
   * @param jsoupDocument
   * The Jsoup document to convert.
   * @return A W3C Document.
   */
  public static Document jsoup2DOM(org.jsoup.nodes.Document jsoupDocument) {

    Document document = null;

    try {

      /* Obtain the document builder for the configured XML parser. */
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
          .newInstance();

      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

      /* Create a document to contain the content. */
      document = docBuilder.newDocument();

      Map<String, String> nsMap = new HashMap<String, String>();

      createDOM(jsoupDocument, document, document, nsMap);
      
    } catch (ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    }

    return document;
  }

  /**
   * The internal helper that copies content from the specified Jsoup
   * <tt>Node</tt> into a W3C {@link Node}.
   *
   * @param node
   * The Jsoup node containing the content to copy to the specified W3C
   * {@link Node}.
   * @param out
   * The W3C {@link Node} that receives the DOM content.
   */
  private static void createDOM(org.jsoup.nodes.Node node, Node out,
      Document doc, Map<String, String> ns) 
  {
    if (node instanceof org.jsoup.nodes.Document) {
      org.jsoup.nodes.Document d = ((org.jsoup.nodes.Document) node);

      for (org.jsoup.nodes.Node n : d.childNodes()) {
        createDOM(n, out, doc, ns);
      }
    }
    else if (node instanceof org.jsoup.nodes.Element) {
      org.jsoup.nodes.Element e = ((org.jsoup.nodes.Element) node);
      org.w3c.dom.Element _e = doc.createElement(e.tagName());
      out.appendChild(_e);
      org.jsoup.nodes.Attributes atts = e.attributes();

      for (org.jsoup.nodes.Attribute a : atts) {
        String attName = a.getKey();
        // omit xhtml namespace
        if (attName.equals("xmlns")) {
          continue;
        }
        
        String attPrefix = getNSPrefix(attName);
        
        if (attPrefix != null) {
          if (attPrefix.equals("xmlns")) {
            ns.put(getLocalName(attName), a.getValue());
          } else if (!attPrefix.equals("xml")) {
            String namespace = ns.get(attPrefix);
            if (namespace == null) {
              // fix attribute names looking like qnames
              attName = attName.replace(':', '_');
            }
          }
        }
        _e.setAttribute(attName, a.getValue());
        if ("id".equals(attName)) {
          _e.setIdAttribute(attName, true);
        }
      }

      for (org.jsoup.nodes.Node n : e.childNodes()) {
        createDOM(n, _e, doc, ns);
      }
    } else if (node instanceof org.jsoup.nodes.TextNode) {
      org.jsoup.nodes.TextNode t = ((org.jsoup.nodes.TextNode) node);

      if (!(out instanceof Document)) {
        out.appendChild(doc.createTextNode(t.getWholeText()));
      }
    } else if (node instanceof org.jsoup.nodes.DataNode) {
      org.jsoup.nodes.DataNode d = (org.jsoup.nodes.DataNode) node;
      out.appendChild(doc.createCDATASection(d.getWholeData()));
    } else if (node instanceof org.jsoup.nodes.DocumentType) {
      // Ignored
    } else if (node instanceof org.jsoup.nodes.Comment) {
      // Ignored
    } else {
      assert(false);
    }
  }

  // some hacks for handling namespace in jsoup2DOM conversion
  private static String getNSPrefix(String name) {
    if (name != null) {
      int pos = name.indexOf(':');
      if (pos > 0) {
        return name.substring(0, pos);
      }
    }
    return null;
  }

  private static String getLocalName(String name) {
    if (name != null) {
      int pos = name.lastIndexOf(':');
      if (pos > 0) {
        return name.substring(pos + 1);
      }
    }
    return name;
  }
}
