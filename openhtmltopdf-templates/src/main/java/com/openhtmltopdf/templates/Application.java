package com.openhtmltopdf.templates;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.imgscalr.Scalr;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.openhtmltopdf.templates.DataGenerator.DataProvider;
import com.openhtmltopdf.util.XRLog;
import com.uwyn.jhighlight.renderer.XmlXhtmlRenderer;

public class Application {
    private final PdfCreator _pdfCreator = new PdfCreator();
    private final ThymeleafProcessor _thymeleaf = new ThymeleafProcessor();
    private final VisualTester _tester = new VisualTester(new File("./target/test/"));
    private final File _websiteBase = new File("./target/website/pdfs/");
    private final XmlXhtmlRenderer _highlighter = new XmlXhtmlRenderer();
    private final String _codeStyles = createStyles(XmlXhtmlRenderer.DEFAULT_CSS);
    private final List<String> _templates = new ArrayList<>();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String createStyles(Map styles) {
        StringBuilder sb = new StringBuilder();
        Iterator iter = styles.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
            if (entry.getKey().equals("h1")) {
                continue;
            }
            sb.append(entry.getKey())
              .append(" { \n")
              .append(entry.getValue())
              .append("}\n");
        }

        return sb.toString();
    }

    public static class Attribution {
        public String author;
        public String link;
    }

    public static class TemplateSettings {
        public String description;
        public String license;
        public List<Attribution> attributions;
    }

    private TemplateSettings getYaml(String props) {
        Constructor cons = new Constructor(TemplateSettings.class);
        Yaml yaml = new Yaml(cons);
        return yaml.load(props);
    }

    private boolean runTemplate(String template, DataProvider provider) throws Exception {
        boolean fail = false;

        System.out.println("Running: " + template);
        _templates.add(template);

        Map<String, Object> args = provider.provide();
        String html = _thymeleaf.process(template, args);
        byte[] actual = _pdfCreator.runRenderer("/templates/", html);

        if (Application.class.getResource("/expected/" + template + ".pdf") != null) {
            try (InputStream is = Application.class.getResourceAsStream("/expected/" + template + ".pdf")) {
                byte[] expected = IOUtils.toByteArray(is);
                fail |= !_tester.testPdfEquality(expected, actual, template);
            }
        } else {
            System.err.println("Test proof not found for: " + template);
            fail = true;
        }

        File webTemplateBase = new File(_websiteBase, template);
        webTemplateBase.mkdirs();

        Files.write(new File(webTemplateBase, template + ".pdf").toPath(), actual);

        try (PDDocument docActual = PDDocument.load(actual)) {
            PDFRenderer rendActual = new PDFRenderer(docActual);
            BufferedImage bi = rendActual.renderImageWithDPI(0, 192f, ImageType.RGB);

            String filename = template + "-large.png"; 
            ImageIO.write(bi, "png", new File(webTemplateBase, filename));

            BufferedImage small = Scalr.resize(bi, 900);
            String smallname = template + "-small.png";
            ImageIO.write(small, "png", new File(webTemplateBase, smallname));
        }

        String thymeleaf;
        try (InputStream in = Application.class.getResourceAsStream("/templates/" + template + ".html")) {
            thymeleaf = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        }

        String highlightHtml = _highlighter.highlight(template + ".html", html, "UTF-8", true); 
        String highlightThymeleaf = _highlighter.highlight(template, thymeleaf, "UTF-8", true);

        String[] parts = thymeleaf.split(Pattern.quote("====="));
        TemplateSettings meta = getYaml(parts[1]);

        Map<String, Object> webArgs = new HashMap<>();
        webArgs.put("thymeleaf", highlightThymeleaf);
        webArgs.put("raw", highlightHtml);
        webArgs.put("name", template);
        webArgs.put("code_style", _codeStyles);
        webArgs.put("meta", meta);

        String webpage = _thymeleaf.process("webpage-details", webArgs);
        Files.write(new File(webTemplateBase, "details.html").toPath(), webpage.getBytes(StandardCharsets.UTF_8));

        return fail;
    }

    public int run() {
        boolean fail = false;
        try {
            _websiteBase.mkdirs();

            fail |= runTemplate("arboshiki-invoice", DataGenerator.INVOICE);
            fail |= runTemplate("cafe-menu", DataGenerator.MENU);

            Map<String, Object> args = Collections.singletonMap("templates", _templates);
            String indexHtml = _thymeleaf.process("webpage-index", args);

            Files.write(new File(_websiteBase, "index.html").toPath(), indexHtml.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            System.out.println("Exception while generating templates...");
            e.printStackTrace();
            return -1;
        }
        return fail ? -1 : 0;
    }

    public static void main(String[] args) {
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, Level.WARNING));
        System.exit(new Application().run());
    }
}
