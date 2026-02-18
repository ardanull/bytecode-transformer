package com.arda.bctransform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Config {

    public boolean entryLog = false;
    public boolean timing = false;
    public boolean dryRun = false;

    public String logger = "stdout";
    public String logPrefix = "[BC]";

    public String includeClassRegex = null;
    public List<String> excludeClassRegex = new ArrayList<>();

    public String includeMethodRegex = null;
    public List<String> excludeMethodRegex = new ArrayList<>();

    public String requireAnnotationDesc = null;

    public Path reportPath = null;
}
