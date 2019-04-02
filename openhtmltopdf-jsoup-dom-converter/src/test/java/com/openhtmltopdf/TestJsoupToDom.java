package com.openhtmltopdf;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestJsoupToDom {
    private W3CDom helper = new W3CDom();
    
    private void run(String html) {
        org.jsoup.nodes.Document docIn = Jsoup.parse(html);
        
        org.w3c.dom.Document docOut1 = helper.fromJsoup(docIn);
        org.w3c.dom.Document docOut2 = DOMBuilder.jsoup2DOM(docIn);
        
        System.out.println();
        System.out.println("-----JSOUP Conversion-----");
        System.out.println(helper.asString(docOut1));
        
        System.out.println();
        System.out.println("-----OpenHTMLToPDF Conversion------");
        System.out.println(helper.asString(docOut2));
        
        Assert.assertEquals(helper.asString(docOut1), helper.asString(docOut2));
    }
    
    @Test
    public void testSimple() {
        run("<html><head></head><body style=\"font-size: 12px;\">Some text<img src=\"test.jpg\"></body></html>");
    }
    
    @Test
    @Ignore // Ours is broken as it strips the xmlns attribute from the svg.
    public void testSVG() {
        run(
                "<html>\n" + 
                "<body>\n" + 
                "\n" + 
                "<svg xmlns=\"http://www.w3.org/2000/svg\" height=\"100\" width=\"100\">\n" + 
                "  <circle cx=\"50\" cy=\"50\" r=\"40\" stroke=\"black\" stroke-width=\"3\" fill=\"red\" />\n" + 
                "</svg> \n" + 
                " \n" + 
                "</body>\n" + 
                "</html>"
          );
    }
    
}
