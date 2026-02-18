package com.arda.bctransform;

import static org.objectweb.asm.Opcodes.ASM9;

import java.util.regex.Pattern;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public final class ClassTransformVisitor extends ClassVisitor {

    private final Config cfg;
    private final Pattern includeMethod;
    private final TransformReport rep;

    private String ownerInternal;
    private int classTransformed = 0;

    public ClassTransformVisitor(
        ClassVisitor cv,
        Config cfg,
        Pattern includeMethod,
        TransformReport rep
    ) {
        super(ASM9, cv);
        this.cfg = cfg;
        this.includeMethod = includeMethod;
        this.rep = rep;
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces
    ) {
        this.ownerInternal = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        String signature,
        String[] exceptions
    ) {
        MethodVisitor mv = super.visitMethod(
            access,
            name,
            descriptor,
            signature,
            exceptions
        );
        return new MethodTransformAdapter(
            mv,
            access,
            ownerInternal,
            name,
            descriptor,
            cfg,
            includeMethod,
            rep,
            this
        );
    }

    void markClassTransformed() {
        if (classTransformed == 0) {
            rep.transformedClasses++;
            classTransformed = 1;
        }
    }
}
