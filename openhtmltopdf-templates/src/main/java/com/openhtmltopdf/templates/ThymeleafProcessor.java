package com.openhtmltopdf.templates;

import java.util.Locale;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class ThymeleafProcessor {
    private final TemplateEngine _thymeleaf = createTemplateEngine();

    private static TemplateEngine createTemplateEngine() {
        TemplateEngine te = new TemplateEngine();

        ClassLoaderTemplateResolver tr = new ClassLoaderTemplateResolver();
        tr.setPrefix("/templates/");
        tr.setSuffix(".html");

        te.setTemplateResolver(tr);
        te.addDialect(new Java8TimeDialect());

        return te;
    }

    public String process(String template, Map<String, Object> args) {
        Context ctx = new Context(Locale.US, args);
        return _thymeleaf.process(template, ctx);
    }
}
