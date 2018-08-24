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
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.options.MutableDataSet;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.util.Charsets;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@Command(
    name = "render",
    description = "OpenHtmlToPdf - Render markdown document to PDF.",
    mixinStandardHelpOptions = true,
    version = "render-markdown - 0.1")
public class MarkdownRenderer implements Runnable {

  @Option(
      names = {"-d", "--debug"},
      description = "Enable debug mode. Print html files.")
  boolean debug;

  @Option(
      names = {"-r", "--markdown-header"},
      paramLabel = "FILE",
      description = "Html file to be used as header for markdown content.")
  File header;

  @Option(
      names = {"--fonts-dir"},
      paramLabel = "FILE",
      description = "Path to directory that contains the fonts to be added. Defaults to 'fonts'")
  File fontsDir = new File("fonts");

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
      String md = new String(bytes, UTF_8);
      String html = markdown(md);

      String hdr = getHeader();

      Path dstPath = getDestinationWithFallback(output, input);

      String result = hdr + html + "</body></html>";

      if (debug) {
        Files.write(Paths.get(input.getName() + ".debug.html"), result.getBytes(UTF_8));
      }

      renderPDF(result, new FileOutputStream(dstPath.toFile()));

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
    String hdr;
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
        Parser.EXTENSIONS,
        Arrays.asList(
            TocExtension.create(), AnchorLinkExtension.create(), AttributesExtension.create()));

    options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false);
    options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_FALLBACK_TO_IMAGE);

    List<Extension> extensionList =
        Stream.of(
                YamlFrontMatterExtension.create(),
                SuperscriptExtension.create(),
                EmojiExtension.create())
            .collect(Collectors.toList());

    Parser parser = Parser.builder(options).extensions(extensionList).build();
    HtmlRenderer renderer = HtmlRenderer.builder(options).extensions(extensionList).build();

    Node document = parser.parse(md);
    return renderer.render(document);
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
