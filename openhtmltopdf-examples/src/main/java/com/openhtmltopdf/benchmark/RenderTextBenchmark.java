package com.openhtmltopdf.benchmark;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.performance.PerformanceCaseGenerator;
import com.openhtmltopdf.util.XRLog;
import org.apache.pdfbox.io.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * To run these benchmarks in the repo root directory:
 * <pre>
 * mvn install -DskipTests
 * java -jar ./openhtmltopdf-examples/target/benchmarks.jar
 * </pre>
 *
 * They should take a couple of minutes.
 *
 * @author schrader
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 6, timeUnit = TimeUnit.SECONDS)
@Fork(warmups = 0, value = 1)
public class RenderTextBenchmark {

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RenderTextBenchmark.class.getSimpleName())
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

        contents.put("/performance/table-rows", PerformanceCaseGenerator.tableRows(1_000));
        contents.put("/performance/paragraphs", PerformanceCaseGenerator.paragraphs(100));
        contents.put("/performance/page-break-blocks", PerformanceCaseGenerator.pageBreakAvoidBlocks(300));
        contents.put("/performance/blocks", PerformanceCaseGenerator.blocks(300));
    }

    @Benchmark
    public void renderText_Plain() throws Exception {
        runRenderer(contents.get("/benchmark/render-text-plain.html"));
    }

    @Benchmark
    public void renderText_SoftHyphens() throws Exception {
        runRenderer(contents.get("/benchmark/render-text-soft-hyphens.html"));
    }

    @Benchmark
    public void renderTableRows() throws IOException {
        runRenderer(contents.get("/performance/table-rows"));
    }

    @Benchmark
    public void renderParagraphs() throws IOException {
        runRenderer(contents.get("/performance/paragraphs"));
    }

    @Benchmark
    public void renderBlocksPageBreak() throws IOException {
        runRenderer(contents.get("/performance/page-break-blocks"));
    }

    @Benchmark
    public void renderBlocks() throws IOException {
        runRenderer(contents.get("/performance/blocks"));
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
            return new String(htmlBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
