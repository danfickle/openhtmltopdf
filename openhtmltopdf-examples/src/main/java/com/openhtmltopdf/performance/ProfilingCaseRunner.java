package com.openhtmltopdf.performance;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public class ProfilingCaseRunner {

    private static void run(String name, String html) throws Exception {
        System.out.println("Starting profiling case: " + name);
        System.out.println("Attach your profiler now.");
        
        Thread.sleep(20_000);
        
        System.out.println("Beginning actual case now...");
        
        long start = System.currentTimeMillis();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(0xffff);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, PerformanceCaseGenerator.class.getResource("/").toExternalForm());
        builder.toStream(baos);
        builder.useFastMode();
        builder.run();
        long end = System.currentTimeMillis();
        
        System.out.println("Profiling case took " + (end - start) + " milliseconds.");
        
        Files.createDirectories(Paths.get("target/test/profiling/"));
        Files.write(Paths.get("target/test/profiling/" + name + ".pdf"), baos.toByteArray());
    }
    
    public static void main(String... args) throws Exception {
        // Just uncomment the one you want to profile...
        
        //run("paragraphs", PerformanceCaseGenerator.paragraphs(80_000));
        //run("table-rows", PerformanceCaseGenerator.tableRows(50_000));
        run("border-radius", PerformanceCaseGenerator.borderRadius(30_000));
    }
    
}
