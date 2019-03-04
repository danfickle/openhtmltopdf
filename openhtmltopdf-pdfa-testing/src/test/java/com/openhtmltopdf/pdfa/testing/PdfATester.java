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
import java.util.stream.Collectors;

import org.apache.pdfbox.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion.Status;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;

public class PdfATester {
    @BeforeClass
    public static void initialize() {
        VeraGreenfieldFoundryProvider.initialise();
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
                Files.copy(in, Paths.get("target/test/artefacts/Karla-Bold.ttf"));
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
        
        PDFAValidator validator = Foundries.defaultInstance().createValidator(flavour, true);
        try (InputStream is = new ByteArrayInputStream(pdfBytes);
             PDFAParser parser = Foundries.defaultInstance().createParser(is, flavour)) {

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
    
}
