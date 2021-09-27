package com.openhtmltopdf.pdfa.testing;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.pdfbox.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.VeraPDFFoundry;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion.Status;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;

import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.util.XRLog;

public class PdfATester {
    @BeforeClass
    public static void initialize() {
        VeraGreenfieldFoundryProvider.initialise();

        XRLog.listRegisteredLoggers().forEach(log -> XRLog.setLevel(log, Level.WARNING));
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public boolean run(String resource, PDFAFlavour flavour, PdfAConformance conform) throws Exception  {
        byte[] htmlBytes = null;
        try (InputStream is = PdfATester.class.getResourceAsStream("/html/" + resource + ".html")) {
            htmlBytes = IOUtils.toByteArray(is);
        }
        String html = new String(htmlBytes, StandardCharsets.UTF_8);
        
        Files.createDirectories(Paths.get("target/test/artefacts/"));
        if (!Files.exists(Paths.get("target/test/artefacts/Karla-Bold.ttf"))) {
            try (InputStream in = PdfATester.class.getResourceAsStream("/fonts/Karla-Bold.ttf")) {
                Files.write(Paths.get("target/test/artefacts/Karla-Bold.ttf"), IOUtils.toByteArray(in));
            }
        }
        
        byte[] pdfBytes;
        
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            //builder.testMode(true);
            builder.usePdfVersion(conform.getPart() == 1 ? 1.4f : 1.5f);
            builder.usePdfAConformance(conform);
            builder.useFont(new File("target/test/artefacts/Karla-Bold.ttf"), "TestFont");
            builder.withHtmlContent(html, PdfATester.class.getResource("/html/").toString());

            // File embeds are blocked by default, allow everything.
            builder.useExternalResourceAccessControl((uri, type) -> true, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI);
            builder.useExternalResourceAccessControl((uri, type) -> true, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI);

            try (InputStream colorProfile = PdfATester.class.getResourceAsStream("/colorspaces/sRGB.icc")) {
                byte[] colorProfileBytes = IOUtils.toByteArray(colorProfile);
                builder.useColorProfile(colorProfileBytes);
            }
        
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            builder.toStream(baos);
            builder.run();
            pdfBytes = baos.toByteArray();
        
        Files.createDirectories(Paths.get("target/test/pdf/"));
        Files.write(Paths.get("target/test/pdf/" + resource + "--" + flavour + ".pdf"), pdfBytes);
        
       

        try (VeraPDFFoundry foundry = Foundries.defaultInstance();
             InputStream is = new ByteArrayInputStream(pdfBytes);
             PDFAValidator validator = foundry.createValidator(flavour, true);
             PDFAParser parser = foundry.createParser(is, flavour)) {

            ValidationResult result = validator.validate(parser);

            List<TestAssertion> asserts = result.getTestAssertions().stream()
                                    .filter(ta -> ta.getStatus() == Status.FAILED)
                                    .filter(distinctByKey(TestAssertion::getRuleId))
                                    .collect(Collectors.toList());
                    
            String errs = asserts.stream()
                    .map(ta -> String.format("%s\n    %s", ta.getMessage().replaceAll("\\s+", " "), ta.getLocation().getContext()))
                    .collect(Collectors.joining("\n    ", "[\n    ", "\n]"));
            
            System.err.format("\nDISTINCT ERRORS(%s--%s) (%d): %s\n", resource, flavour, asserts.size(), errs);
            
            return asserts.isEmpty() && result.isCompliant();
        }
    }
    
    /**
     * PDF/A conformance. Issue 326.
     * NOTE: PDF/A1 standards do not support alpha in images.
     */
    @Test
    public void testAllInOnePdfA1b() throws Exception {
        assertTrue(run("all-in-one-no-alpha", PDFAFlavour.PDFA_1_B, PdfAConformance.PDFA_1_B));
    }

    @Test
    public void testAllInOnePdfA1a() throws Exception {
        assertTrue(run("all-in-one-no-alpha", PDFAFlavour.PDFA_1_A, PdfAConformance.PDFA_1_A));
    }
    
    @Test
    public void testAllInOnePdfA2b() throws Exception {
        assertTrue(run("all-in-one", PDFAFlavour.PDFA_2_B, PdfAConformance.PDFA_2_B));
    }

    @Test
    public void testAllInOnePdfA2a() throws Exception {
        assertTrue(run("all-in-one", PDFAFlavour.PDFA_2_A, PdfAConformance.PDFA_2_A));
    }
    
    @Test
    public void testAllInOnePdfA2u() throws Exception {
        assertTrue(run("all-in-one", PDFAFlavour.PDFA_2_U, PdfAConformance.PDFA_2_U));
    }

    @Test
    public void testAllInOnePdfA3a() throws Exception {
        assertTrue(run("all-in-one", PDFAFlavour.PDFA_3_A, PdfAConformance.PDFA_3_A));
    }

    @Test
    public void testAllInOnePdfA3u() throws Exception {
        assertTrue(run("all-in-one", PDFAFlavour.PDFA_3_U, PdfAConformance.PDFA_3_U));
    }

    /**
     * File embedding is allowed as of PDF/A3.
     */
    @Test
    public void testFileEmbedA3b() throws Exception {
        assertTrue(run("file-embed", PDFAFlavour.PDFA_3_B, PdfAConformance.PDFA_3_B));
    }
}
