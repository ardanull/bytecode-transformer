package com.arda.bctransform;

import static org.objectweb.asm.Opcodes.*;

import java.util.regex.Pattern;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public final class MethodTransformAdapter extends AdviceAdapter {

    private final int accessFlags;
    private final String ownerInternal;
    private final String name;
    private final String desc;
    private final Config cfg;
    private final Pattern includeMethod;
    private final TransformReport rep;
    private final ClassTransformVisitor parent;

    private boolean hasRequiredAnnotation = false;
    private int startTimeLocal = -1;
    private boolean eligible = false;

    public MethodTransformAdapter(
        MethodVisitor mv,
        int access,
        String ownerInternal,
        String name,
        String desc,
        Config cfg,
        Pattern includeMethod,
        TransformReport rep,
        ClassTransformVisitor parent
    ) {
        super(ASM9, mv, access, name, desc);
        this.accessFlags = access;
        this.ownerInternal = ownerInternal;
        this.name = name;
        this.desc = desc;
        this.cfg = cfg;
        this.includeMethod = includeMethod;
        this.rep = rep;
        this.parent = parent;
        this.eligible = computeEligible();
    }

    private boolean computeEligible() {
        if ((accessFlags & (ACC_ABSTRACT | ACC_NATIVE)) != 0) return false;
        if (name.equals("<init>") || name.equals("<clinit>")) return false;

        String sig = name + desc;
        if (
            includeMethod != null && !includeMethod.matcher(sig).find()
        ) return false;

        for (String ex : cfg.excludeMethodRegex) {
            if (Pattern.compile(ex).matcher(sig).find()) return false;
        }

        return cfg.entryLog || cfg.timing;
    }

    @Override
    public AnnotationVisitor visitAnnotation(
        String descriptor,
        boolean visible
    ) {
        if (
            cfg.requireAnnotationDesc != null &&
            cfg.requireAnnotationDesc.equals(descriptor)
        ) {
            hasRequiredAnnotation = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    protected void onMethodEnter() {
        if (!eligible) return;
        if (cfg.requireAnnotationDesc != null && !hasRequiredAnnotation) return;

        if (cfg.entryLog) {
            emitLog("enter");
        }

        if (cfg.timing) {
            invokeStatic(
                Type.getType(System.class),
                new org.objectweb.asm.commons.Method("nanoTime", "()J")
            );
            startTimeLocal = newLocal(Type.LONG_TYPE);
            storeLocal(startTimeLocal, Type.LONG_TYPE);
        }

        record(true, cfg.entryLog, cfg.timing);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (!eligible) return;
        if (cfg.requireAnnotationDesc != null && !hasRequiredAnnotation) return;
        if (!cfg.timing) return;
        if (startTimeLocal < 0) return;

        invokeStatic(
            Type.getType(System.class),
            new org.objectweb.asm.commons.Method("nanoTime", "()J")
        );
        loadLocal(startTimeLocal, Type.LONG_TYPE);
        math(SUB, Type.LONG_TYPE);

        int durLocal = newLocal(Type.LONG_TYPE);
        storeLocal(durLocal, Type.LONG_TYPE);

        if (opcode == ATHROW) {
            emitTiming("throw", durLocal);
        } else {
            emitTiming("exit", durLocal);
        }
    }

    private void emitLog(String phase) {
        String owner = ownerInternal.replace('/', '.');
        String msg =
            cfg.logPrefix + " " + phase + " " + owner + "." + name + desc;
        if ("slf4j".equalsIgnoreCase(cfg.logger)) {
            visitLdcInsn(ownerInternal);
            visitMethodInsn(
                INVOKESTATIC,
                "org/slf4j/LoggerFactory",
                "getLogger",
                "(Ljava/lang/String;)Lorg/slf4j/Logger;",
                false
            );
            visitLdcInsn(msg);
            visitMethodInsn(
                INVOKEINTERFACE,
                "org/slf4j/Logger",
                "info",
                "(Ljava/lang/String;)V",
                true
            );
            return;
        }
        visitFieldInsn(
            GETSTATIC,
            "java/lang/System",
            "out",
            "Ljava/io/PrintStream;"
        );
        visitLdcInsn(msg);
        visitMethodInsn(
            INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            "(Ljava/lang/String;)V",
            false
        );
    }

    private void emitTiming(String phase, int durLocal) {
        String owner = ownerInternal.replace('/', '.');
        if ("slf4j".equalsIgnoreCase(cfg.logger)) {
            visitLdcInsn(ownerInternal);
            visitMethodInsn(
                INVOKESTATIC,
                "org/slf4j/LoggerFactory",
                "getLogger",
                "(Ljava/lang/String;)Lorg/slf4j/Logger;",
                false
            );
            visitLdcInsn(
                cfg.logPrefix +
                    " " +
                    phase +
                    " " +
                    owner +
                    "." +
                    name +
                    desc +
                    " durationNs="
            );
            loadLocal(durLocal, Type.LONG_TYPE);
            visitMethodInsn(
                INVOKESTATIC,
                "java/lang/Long",
                "toString",
                "(J)Ljava/lang/String;",
                false
            );
            visitMethodInsn(
                INVOKEVIRTUAL,
                "java/lang/String",
                "concat",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false
            );
            visitMethodInsn(
                INVOKEINTERFACE,
                "org/slf4j/Logger",
                "info",
                "(Ljava/lang/String;)V",
                true
            );
            return;
        }

        visitFieldInsn(
            GETSTATIC,
            "java/lang/System",
            "out",
            "Ljava/io/PrintStream;"
        );
        visitTypeInsn(NEW, "java/lang/StringBuilder");
        dup();
        visitMethodInsn(
            INVOKESPECIAL,
            "java/lang/StringBuilder",
            "<init>",
            "()V",
            false
        );

        visitLdcInsn(
            cfg.logPrefix +
                " " +
                phase +
                " " +
                owner +
                "." +
                name +
                desc +
                " durationNs="
        );
        visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            false
        );

        loadLocal(durLocal, Type.LONG_TYPE);
        visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(J)Ljava/lang/StringBuilder;",
            false
        );

        visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "toString",
            "()Ljava/lang/String;",
            false
        );
        visitMethodInsn(
            INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            "(Ljava/lang/String;)V",
            false
        );
    }

    private void record(boolean transformed, boolean entry, boolean timing) {
        if (!transformed) return;
        parent.markClassTransformed();
        rep.transformedMethods++;
        rep.methods.add(
            new TransformReport.TransformedMethod(
                ownerInternal.replace('/', '.'),
                name,
                desc,
                entry,
                timing
            )
        );
    }
}
