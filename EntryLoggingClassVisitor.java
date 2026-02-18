package com.example.bctransform;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

public final class EntryLoggingClassVisitor extends ClassVisitor {

    private String classNameInternal;

    public EntryLoggingClassVisitor(ClassVisitor cv) {
        super(ASM9, cv);
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
        this.classNameInternal = name; // e.g. com/example/Foo
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

        // Skip abstract/native methods
        if ((access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) return mv;

        // Skip <init> and <clinit> (istersen kaldırabilirsin)
        if (name.equals("<init>") || name.equals("<clinit>")) return mv;

        return new AdviceAdapter(ASM9, mv, access, name, descriptor) {
            @Override
            protected void onMethodEnter() {
                // Get System.out
                visitFieldInsn(
                    GETSTATIC,
                    "java/lang/System",
                    "out",
                    "Ljava/io/PrintStream;"
                );

                // Build message
                String cn = classNameInternal.replace('/', '.');
                String msg = "[BC] " + cn + "." + name + descriptor;

                visitLdcInsn(msg);
                visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/io/PrintStream",
                    "println",
                    "(Ljava/lang/String;)V",
                    false
                );
            }
        };
    }
}
