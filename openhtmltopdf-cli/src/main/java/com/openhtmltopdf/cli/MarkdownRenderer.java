package com.openhtmltopdf.cli;

import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.pdfbox.util.Charsets;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "render",
    description = "OpenHtmlToPdf - Render markdown document to PDF.",
    mixinStandardHelpOptions = true,
    version = "render-markdown - 0.1")
public class MarkdownRenderer implements Runnable {

  @Option(
      names = {"-r", "--markdown-header"},
      paramLabel = "FILE",
      description = "Html file to be used as header for markdown content.")
  File header;

  @Option(
      names = {"-i", "--input"},
      paramLabel = "FILE",
      description =
          "File to convert to PDF. Can be any supported file format: Markdown. Defaults to README.md")
  File input = new File("README.md");

  @Option(
      names = {"-o", "--out"},
      paramLabel = "FILE",
      description = "File to convert to PDF. Can be any supported file format: Markdown. ")
  File output;

  @Override
  public void run() {
    try {

      byte[] bytes = Files.readAllBytes(input.toPath());
      String md = new String(bytes, StandardCharsets.UTF_8);
      String html = markdown(md);

      String hdr = getHeader();

      Path dstPath = getDestinationWithFallback(output, input);

      renderPDF(hdr + html + "</body></html>", new FileOutputStream(dstPath.toFile()));

    } catch (Exception e) {
      System.err.printf("Exception %s", e);
    }
  }

  private Path getDestinationWithFallback(File destination, File source) {
    if (destination != null) {
      return destination.toPath();
    }
    return Paths.get(source.getName() + ".pdf");
  }

  private String getHeader() throws IOException {
    String hdr = null;
    if (header != null) {
      byte[] hdrBytes = Files.readAllBytes(header.toPath());
      hdr = new String(hdrBytes, Charsets.UTF_8);
    } else {
      hdr = "<html> <head> </head> <body> ";
    }
    return hdr;
  }

  private String markdown(String md) {
    MutableDataSet options = new MutableDataSet();
    options.set(
        Parser.EXTENSIONS, Arrays.asList(TocExtension.create(), AnchorLinkExtension.create()));
    options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false);

    Parser parser = Parser.builder(options).build();
    HtmlRenderer renderer = HtmlRenderer.builder(options).build();

    Node document = parser.parse(md);
    return renderer.render(document);
  }

  private void renderPDF(String html, OutputStream outputStream) throws Exception {
    try {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useSVGDrawer(new BatikSVGDrawer());
      builder.useMathMLDrawer(new MathMLDrawer());

      builder.withHtmlContent(html, ".");
      builder.toStream(outputStream);
      builder.run();
    } finally {
      outputStream.close();
    }
  }
}
