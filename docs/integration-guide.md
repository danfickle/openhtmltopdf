OPEN HTML TO PDF
---------

GETTING OPEN HTML TO PDF
========
New releases of Open HTML to PDF will be distributed through Maven.  Search maven for [com.openhtmltopdf](http://mvnrepository.com/artifact/com.openhtmltopdf).
Current maven release is ````0.0.1-RC17````. If you would like to be notified of new releases, please subscribe to the [Maven issue](https://github.com/danfickle/openhtmltopdf/issues/7).
[You can ask for a new release, if needed](https://github.com/danfickle/openhtmltopdf/issues/182).

MAVEN ARTIFACTS
========
Add these to your maven dependencies section as needed:
````xml
  	<properties>
  		<!-- Define the version of OPEN HTML TO PDF in the properties section of your POM. -->  	       
  		<openhtml.version>0.0.1-RC17</openhtml.version>
  	</properties>

  	<dependency>
  		<!-- ALWAYS required. -->
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-core</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>
  	
  	<dependency>
  		<!-- Required for PDF output. -->
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-pdfbox</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>
  	
  	<dependency>
  		<!-- Required for image output only. -->  	
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-java2d</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>
  	
  	<dependency>
  		<!-- Optional, leave out if you do not need right-to-left or bi-directional text support. -->
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-rtl-support</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>
  	
  	<dependency>
        <!-- Optional, leave out if you do not need HTML5 parsing support. -->
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-jsoup-dom-converter</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>
  	 
  	<dependency>
  	    <!-- Optional, leave out if you do not need logging via slf4j. -->
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-slf4j</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>
  	
  	<dependency>
  	    <!-- Optional, leave out if you do not need logging via log4j. -->
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-log4j</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>

  	<dependency>
  	    <!-- Optional, leave out if you do not need SVG support. -->
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-svg-support</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>

  	<dependency>
  	    <!-- Optional, leave out if you do not need MathML support. -->
  	    <!-- Introduced in RC-13. -->
  		<groupId>com.openhtmltopdf</groupId>
  		<artifactId>openhtmltopdf-mathml-support</artifactId>
  		<version>${openhtml.version}</version>
  	</dependency>

````

MINIMAL USAGE
========
Most of the options avaiable for PDF output are settable on the [PdfRendererBuilder](https://github.com/danfickle/openhtmltopdf/blob/open-dev-v1/openhtmltopdf-pdfbox/src/main/java/com/openhtmltopdf/pdfboxout/PdfRendererBuilder.java) builder class. This shows the minimal possible configuration to output a PDF from an XHTML document.

````java
import java.io.FileOutputStream;
import java.io.OutputStream;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public class SimpleUsage 
{
    public static void main(String[] args) throws Exception { 
        try (OutputStream os = new FileOutputStream("/Users/me/Documents/pdf/out.pdf")) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withUri("file:///Users/me/Documents/pdf/in.htm");
            builder.toStream(os);
            builder.run();
        }
    }
}
````

SUPPORT FOR BI-DIRECTIONAL (RTL) AND SHAPED TEXT
========
````java
// Add these imports (and remember the rtl-support maven module).
import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;

// Then call on the builder.
builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
builder.defaultTextDirection(TextDirection.LTR); // OR RTL
````

HTML5 PARSER SUPPORT
============
While Open HTML to PDF works with a standard w3c DOM, the project provides a converter from the Jsoup HTML5 parser provided Document to
a w3c DOM Document. This allows you to parse and use HTML5, rather than the default strict XML required by the project.

Then you can use one of the ````Jsoup.parse```` methods to parse HTML5 and ````DOMBuilder.jsoup2DOM```` to convert the Jsoup document to a w3c DOM one.
````java
	public org.w3c.dom.Document html5ParseDocument(String urlStr, int timeoutMs) throws IOException 
	{
		URL url = new URL(urlStr);
		org.jsoup.nodes.Document doc;
		
		if (url.getProtocol().equalsIgnoreCase("file")) {
			doc = Jsoup.parse(new File(url.getPath()), "UTF-8");
		}
		else {
			doc = Jsoup.parse(url, timeoutMs);	
		}
		
		return DOMBuilder.jsoup2DOM(doc);
	}
````
Then you can set the renderer document with ````builder.withW3cDocument(doc, url)```` in place of ````builder.withUri(url)````.

PLUGGABLE HTTP CLIENT
=================
Open HTML to PDF makes it simple to plugin an external client for HTTP and HTTPS requests. In fact this is recommended if you are using
HTTP/HTTPS resources, as the built-in Java client is showing its age. For example, to use the excellent [OkHttp](http://square.github.io/okhttp/) library is
as simple as adding the following code:
````java
     public static class OkHttpStreamFactory implements HttpStreamFactory {
           private final OkHttpClient client = new OkHttpClient();
		
            @Override
            public HttpStream getUrl(String url) {
               Request request = new Request.Builder()
                 .url(url)
                 .build();

             try {
              final Response response = client.newCall(request).execute();

              return new HttpStream() {
                  @Override
                  public InputStream getStream() {
                      return response.body().byteStream();
                  }

                  @Override
                  public Reader getReader() {
                      return response.body().charStream();
                  }
             };
           }
           catch (IOException e) {
               e.printStackTrace();
           }

           return null;
	  }
     }
````
Then use ````builder.useHttpStreamImplementation(new OkHttpStreamFactory())````.
The library should close the reader or stream when it is finished with it.

CACHE BETWEEN RUNS (BROKEN)
=======
IMPORTANT (1): This cache system should now be considered deprecated as it is not thread safe. It will be replaced with a simple byte array cache system in the future.

IMPORTANT (2): The cache system is totally broken, please see [discussion in 204](https://github.com/danfickle/openhtmltopdf/issues/204) for replacement options.

URI RESOLVER
=======
By default, the code attempts to resolve relative URIs by using the document URI or CSS stylesheet URI as a base URI.
Absolute URIs are returned unchanged. If you wish to plugin your own resolver, you can.
This can not only resolve relative URIs but also resolve URIs in a private address space or even reject a URI. To use an external resolver 
implement ````FSUriResolver```` and use it with ````builder.useUriResolver(new MyResolver())````. The following example requires resources to be loaded through
SSL.
````java
        	 final NaiveUserAgent.DefaultUriResolver defaultUriResolver = new NaiveUserAgent.DefaultUriResolver();
        	 
        	 builder.useUriResolver(new FSUriResolver() {
				@Override
				public String resolveURI(String baseUri, String uri) {
					// First get an absolute version.
					String supResolved = defaultUriResolver.resolveURI(baseUri, uri);
					
					if (supResolved == null || supResolved.isEmpty())
						return null;
					
					try {
						URI uriObj = new URI(supResolved);
						
						// Only let through resources that are loaded through ssl.
						if (uriObj.getScheme().equalsIgnoreCase("https"))
							return uriObj.toString();
					
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
					return null;
				}
			});
````

LOGGING
=======
Three options are provided by Open HTML to PDF. The default is to use java.util.logging. If you prefer to output using log4j or slf4j, adapters are provided.
Add the appropriate maven module, then at the start of your code, before calling any Open HTML to PDF methods, use this code:
````java
  XRLog.setLoggingEnabled(true);

  // For slf4j:
  XRLog.setLoggerImpl(new Slf4jLogger());
  // or for log4j 1.2.17:
  XRLog.setLoggerImpl(new Log4JXRLogger());    
````

SVG AND MATHML SUPPORT
=======
Add the appropriate maven module and include this line in your builder code for SVG support.
````java
  builder.useSVGDrawer(new BatikSVGDrawer());
````
For MathML support:
````java
  builder..useMathMLDrawer(new MathMLDrawer());
````

IMAGE OUTPUT
=======
For an example of outputting to images see [issue #73](https://github.com/danfickle/openhtmltopdf/issues/73#issuecomment-291070264)


COMING SOON
=======
+ Loads more (stay tuned).

FINALLY
=======
Thanks for using openhtmltopdf and please feel free to file any issues you are having trouble with.

