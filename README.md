OPEN HTML TO PDF
---------

OVERVIEW
========
Open HTML to PDF is a pure-Java library for rendering arbitrary well-formed XML 
(or XHTML) using CSS 2.1 for layout and formatting, output to PDF and images.

LICENSE
========
Open HTML to PDF is distributed under the LGPL.  Open HTML to PDF itself is licensed 
under the GNU Lesser General Public License, version 2.1 or later, available at
http://www.gnu.org/copyleft/lesser.html. You can use Open HTML to PDF in any
way and for any purpose you want as long as you respect the terms of the 
license. A copy of the LGPL license is included as license-lgpl-2.1.txt or license-lgpl-3.txt
in our distributions and in our source tree.

Open HTML to PDF uses a couple of FOSS packages to get the job done. A list
of these, along with the license they each have, is listed in the 
LICENSE file in our distribution.   

GETTING OPEN HTML TO PDF
========
New releases of Open HTML to PDF will be distributed through Maven.  Search maven for com.openhtmltopdf. Coming soon!

GETTING STARTED
========
There is a large amount of sample code under the openhtmltopdf-examples directory (integration guide and template guide to come).

SIMPLE USAGE
========
Add these to your maven dependencies section:
````xml
  	<dependency>
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-core</artifactId>
  		<version>0.0.1-SNAPSHOT</version>
  	</dependency>
  	<dependency>
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-pdfbox</artifactId>
  		<version>0.0.1-SNAPSHOT</version>
  	</dependency>
  	<dependency>
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-rtl-support</artifactId>
  		<version>0.0.1-SNAPSHOT</version>
  	</dependency>
  ````
  Then you can use this code:
  ````java
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;

public class SimpleUsage 
{
	public static void main(String[] args)
	{
		new SimpleUsage().exportToPdfBox("file:///Users/user/path-to/document.xhtml", "/Users/user/path-to/output.pdf");
	}
	
	public void exportToPdfBox(String url, String out)
	{
            OutputStream os = null;
       
              try {
               os = new FileOutputStream(out);
       
               try {
                     PdfBoxRenderer renderer = new PdfBoxRenderer(/* testMode = */ false);

                     // The following three lines are optional. Leave them out if you do not need
                     // RTL or bi-directional text layout.
                     renderer.setBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
                     renderer.setDefaultTextDirection(false);
                     renderer.setBidiReorderer(new ICUBidiReorderer());
               
                     renderer.setDocument(url);
                     renderer.layout();

                     renderer.createPDF(os);
               } catch (Exception e) {
                     // LOG exception
               } finally {
                     try {
                            os.close();
                     } catch (IOException e) {
                            // swallow
                     }
               }
              }
              catch (IOException e1) {
                     // LOG exception.
              }
	}
}
````

CREDITS
========
Open HTML to PDF is based on Flying-saucer. Credit goes to the contributors of that project. Code will also be used from neoFlyingSaucer.

CHANGELOG
========

head
========
+ [Added right-to-left(RTL) and bi-directional text support](https://github.com/danfickle/openhtmltopdf/issues/9)
+ [Added output device using PDF-BOX 2.0.0 release candidate](https://github.com/danfickle/openhtmltopdf/issues/1)
+ [Make sure XML Document Builder doesn't resolve external DTDs](https://github.com/danfickle/openhtmltopdf/issues/2)
+ [Removed obsolete ITEXT based output devices](https://github.com/danfickle/openhtmltopdf/issues/4)
+ [Removed SWT support](https://github.com/danfickle/openhtmltopdf/issues/6)
+ Regressions (please open issue if required):
	+ PDF form controls.
	+ PDF font types other than built-in and truetype.
	+ XMP PDF metadata in PDFs.
	+ PDF encryption.
	+ [PDF text justification](https://github.com/danfickle/openhtmltopdf/issues/3)
