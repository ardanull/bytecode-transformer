package com.arda.bctransform;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "bctransform",
    mixinStandardHelpOptions = true,
    version = "2.0.0",
    description = "Bytecode transformer CLI (instrumentation, not obfuscation)"
)
public final class Main implements Callable<Integer> {

    @Option(
        names = { "-i", "--input" },
        required = true,
        description = "Input jar path"
    )
    Path input;

    @Option(
        names = { "-o", "--output" },
        required = true,
        description = "Output jar path"
    )
    Path output;

    @Option(
        names = { "-c", "--config" },
        description = "YAML config file (optional)"
    )
    Path config;

    @Option(
        names = { "--dry-run" },
        description = "Scan and report only; do not write output"
    )
    boolean dryRun;

    @Option(
        names = { "--report" },
        description = "Write JSON report to this path (optional)"
    )
    Path report;

    @Option(
        names = { "--include-class-regex" },
        description = "Only transform classes matching regex"
    )
    String includeClassRegex;

    @Option(
        names = { "--exclude-class-regex" },
        description = "Skip classes matching regex (repeatable)"
    )
    List<String> excludeClassRegex;

    @Option(
        names = { "--include-method-regex" },
        description = "Only transform methods matching regex: name+desc e.g. foo\\(I\\)V"
    )
    String includeMethodRegex;

    @Option(
        names = { "--exclude-method-regex" },
        description = "Skip methods matching regex (repeatable)"
    )
    List<String> excludeMethodRegex;

    @Option(
        names = { "--require-annotation-desc" },
        description = "Only transform methods having this annotation descriptor, e.g. Lcom/acme/Trace;"
    )
    String requireAnnotationDesc;

    @Option(
        names = { "--entry-log" },
        description = "Inject log at method entry"
    )
    boolean entryLog;

    @Option(
        names = { "--timing" },
        description = "Inject nanoTime duration logging at method exit"
    )
    boolean timing;

    @Option(
        names = { "--logger" },
        description = "Logger target: stdout|slf4j",
        defaultValue = "stdout"
    )
    String logger;

    @Option(
        names = { "--log-prefix" },
        description = "Log message prefix",
        defaultValue = "[BC]"
    )
    String logPrefix;

    public static void main(String[] args) {
        int code = new CommandLine(new Main()).execute(args);
        System.exit(code);
    }

    @Override
    public Integer call() throws Exception {
        Config cfg = new Config();
        if (config != null) cfg = ConfigLoader.load(config);

        if (includeClassRegex != null) cfg.includeClassRegex =
            includeClassRegex;
        if (
            excludeClassRegex != null && !excludeClassRegex.isEmpty()
        ) cfg.excludeClassRegex = excludeClassRegex;
        if (includeMethodRegex != null) cfg.includeMethodRegex =
            includeMethodRegex;
        if (
            excludeMethodRegex != null && !excludeMethodRegex.isEmpty()
        ) cfg.excludeMethodRegex = excludeMethodRegex;
        if (requireAnnotationDesc != null) cfg.requireAnnotationDesc =
            requireAnnotationDesc;

        if (entryLog) cfg.entryLog = true;
        if (timing) cfg.timing = true;
        if (logger != null) cfg.logger = logger;
        if (logPrefix != null) cfg.logPrefix = logPrefix;

        cfg.dryRun = dryRun;
        cfg.reportPath = report;

        TransformReport rep = JarTransformer.transformJar(input, output, cfg);
        if (cfg.reportPath != null) ReportWriter.write(cfg.reportPath, rep);
        System.out.println("Transformed classes: " + rep.transformedClasses);
        System.out.println("Transformed methods: " + rep.transformedMethods);
        return 0;
    }
}
