package com.arda.bctransform;

import java.util.ArrayList;
import java.util.List;

public final class TransformReport {

    public int scannedClasses = 0;
    public int transformedClasses = 0;
    public int transformedMethods = 0;
    public final List<TransformedMethod> methods = new ArrayList<>();

    public static final class TransformedMethod {

        public String owner;
        public String name;
        public String desc;
        public boolean entryLog;
        public boolean timing;

        public TransformedMethod() {}

        public TransformedMethod(
            String owner,
            String name,
            String desc,
            boolean entryLog,
            boolean timing
        ) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.entryLog = entryLog;
            this.timing = timing;
        }
    }
}
