package com.arda.bctransform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReportWriter {

    public static void write(Path path, TransformReport report)
        throws Exception {
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        ObjectMapper om = new ObjectMapper().enable(
            SerializationFeature.INDENT_OUTPUT
        );
        om.writeValue(path.toFile(), report);
    }

    private ReportWriter() {}
}
