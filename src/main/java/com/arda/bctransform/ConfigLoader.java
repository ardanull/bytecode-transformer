package com.arda.bctransform;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

public final class ConfigLoader {

    public static Config load(Path path) throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(path)) {
            Object obj = yaml.load(in);
            if (!(obj instanceof Map)) return new Config();
            Map<?, ?> m = (Map<?, ?>) obj;

            Config cfg = new Config();
            cfg.entryLog = getBool(m, "entryLog", cfg.entryLog);
            cfg.timing = getBool(m, "timing", cfg.timing);
            cfg.logger = getStr(m, "logger", cfg.logger);
            cfg.logPrefix = getStr(m, "logPrefix", cfg.logPrefix);

            cfg.includeClassRegex = getStrOrNull(m, "includeClassRegex");
            cfg.includeMethodRegex = getStrOrNull(m, "includeMethodRegex");
            cfg.requireAnnotationDesc = getStrOrNull(
                m,
                "requireAnnotationDesc"
            );

            cfg.excludeClassRegex = getStrList(
                m,
                "excludeClassRegex",
                cfg.excludeClassRegex
            );
            cfg.excludeMethodRegex = getStrList(
                m,
                "excludeMethodRegex",
                cfg.excludeMethodRegex
            );

            return cfg;
        }
    }

    private static boolean getBool(Map<?, ?> m, String k, boolean d) {
        Object v = m.get(k);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return d;
    }

    private static String getStr(Map<?, ?> m, String k, String d) {
        Object v = m.get(k);
        return v == null ? d : String.valueOf(v);
    }

    private static String getStrOrNull(Map<?, ?> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private static List<String> getStrList(
        Map<?, ?> m,
        String k,
        List<String> d
    ) {
        Object v = m.get(k);
        if (v == null) return d;
        if (v instanceof List) {
            List<?> l = (List<?>) v;
            List<String> out = new ArrayList<>();
            for (Object o : l) out.add(String.valueOf(o));
            return out;
        }
        return d;
    }

    private ConfigLoader() {}
}
