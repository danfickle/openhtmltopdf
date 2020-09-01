package com.openhtmltopdf.benchmark;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.util.Charsets;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author schrader
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class RenderTextBenchmark {

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RenderTextBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    private Map<String, String> contents = new HashMap<>();

    @Setup
    public void setUp() {
        XRLog.setLoggerImpl(new NoopLogger());

        Arrays.asList(
                "/benchmark/render-text-plain.html",
                "/benchmark/render-text-soft-hyphens.html"
        ).forEach(path -> contents.put(path, readContent(path)));
    }

    @Benchmark
    public void renderText_Plain() throws Exception {
        runRenderer(contents.get("/benchmark/render-text-plain.html"));
    }

    @Benchmark
    public void renderText_SoftHyphens() throws Exception {
        runRenderer(contents.get("/benchmark/render-text-soft-hyphens.html"));
    }

    private void runRenderer(String html) throws IOException {
        ByteArrayOutputStream actual = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, null);
        builder.toStream(actual);
        builder.useFastMode();
        builder.testMode(true);

        builder.run();
    }

    private String readContent(String path) {
        try (InputStream htmlIs = RenderTextBenchmark.class.getResourceAsStream(path)) {
            byte[] htmlBytes = IOUtils.toByteArray(htmlIs);
            return new String(htmlBytes, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
