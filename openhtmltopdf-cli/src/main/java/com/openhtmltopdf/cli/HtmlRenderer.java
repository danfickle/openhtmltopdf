package com.openhtmltopdf.cli;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.extend.impl.FSDefaultCacheStore;
import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.CacheStore;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@Command(
    name = "render",
    description = "OpenHtmlToPdf - Render html document to PDF.",
    mixinStandardHelpOptions = true,
    version = "render-html - 0.1")
public class HtmlRenderer implements Runnable {

  @Option(
      names = {"-d", "--debug"},
      description = "Enable debug mode. Print html files.")
  boolean debug;

  @Option(
      names = {"--fonts-dir"},
      paramLabel = "FILE",
      description = "Path to directory that contains the fonts to be added. Defaults to 'fonts'")
  File fontsDir = new File("fonts");

  @Option(
      names = {"-i", "--input"},
      paramLabel = "FILE",
      description =
          "File to convert to PDF. Can be any supported file format: HTML. Defaults to index.html")
  File input = new File("index.html");

  @Option(
      names = {"-o", "--out"},
      paramLabel = "FILE",
      description = "File to convert to PDF. Can be any supported file format: Markdown. ")
  File output;

  @Override
  public void run() {
    try {

      byte[] bytes = Files.readAllBytes(input.toPath());
      String html = new String(bytes, UTF_8);

      Path dstPath = getDestinationWithFallback(output, input);

      renderPDF(html, new FileOutputStream(dstPath.toFile()));

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

  private void renderPDF(String html, OutputStream outputStream) throws Exception {
    try {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useSVGDrawer(new BatikSVGDrawer());
      builder.useMathMLDrawer(new MathMLDrawer());
      builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
      builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
      builder.defaultTextDirection(PdfRendererBuilder.TextDirection.LTR);
      builder.withHtmlContent(html, ".");
      builder.useCacheStore(CacheStore.PDF_FONT_METRICS, getCache());
      useFonts(builder);

      builder.toStream(outputStream);
      builder.run();
    } finally {
      outputStream.close();
    }
  }

  private void useFonts(PdfRendererBuilder builder) throws IOException {
    Path fonts = fontsDir.toPath();
    log.debug("Loading fonts from {}", fonts.toAbsolutePath().toString());

    try (DirectoryStream<Path> ds = Files.newDirectoryStream(fonts)) {
      for (Path font : ds) {
        String familyName = readFontFamilyName(font);
        log.info("Loading font {} - {}", font.toString(), familyName);
        builder.useFont(font.toFile(), familyName);
      }
    }
  }

  private String readFontFamilyName(Path font) {
    try {
      Font f = Font.createFont(Font.TRUETYPE_FONT, font.toFile());
      return f.getFamily();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private FSCacheEx<String, FSCacheValue> getCache() {
    return new FSDefaultCacheStore();
  }
}
