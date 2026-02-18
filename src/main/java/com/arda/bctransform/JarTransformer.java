package com.arda.bctransform;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public final class JarTransformer {

    public static TransformReport transformJar(
        Path inputJar,
        Path outputJar,
        Config cfg
    ) throws Exception {
        TransformReport rep = new TransformReport();
        Pattern includeClass =
            cfg.includeClassRegex == null
                ? null
                : Pattern.compile(cfg.includeClassRegex);
        Pattern includeMethod =
            cfg.includeMethodRegex == null
                ? null
                : Pattern.compile(cfg.includeMethodRegex);

        try (JarFile jar = new JarFile(inputJar.toFile())) {
            Manifest manifest = jar.getManifest();

            if (cfg.dryRun) {
                Enumeration<JarEntry> en = jar.entries();
                while (en.hasMoreElements()) {
                    JarEntry e = en.nextElement();
                    if (
                        e.isDirectory() || !e.getName().endsWith(".class")
                    ) continue;
                    rep.scannedClasses++;
                    if (
                        eligibleClass(e.getName(), includeClass, cfg)
                    ) rep.transformedClasses++;
                }
                return rep;
            }

            if (outputJar.getParent() != null) Files.createDirectories(
                outputJar.getParent()
            );

            try (
                OutputStream fos = Files.newOutputStream(outputJar);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                JarOutputStream jos =
                    manifest != null
                        ? new JarOutputStream(bos, manifest)
                        : new JarOutputStream(bos)
            ) {
                Set<String> written = new HashSet<>();
                Enumeration<JarEntry> en = jar.entries();

                while (en.hasMoreElements()) {
                    JarEntry inEntry = en.nextElement();

                    if (
                        manifest != null &&
                        "META-INF/MANIFEST.MF".equalsIgnoreCase(
                            inEntry.getName()
                        )
                    ) {
                        continue;
                    }

                    if (!written.add(inEntry.getName())) continue;

                    JarEntry outEntry = new JarEntry(inEntry.getName());
                    outEntry.setTime(inEntry.getTime());
                    jos.putNextEntry(outEntry);

                    try (InputStream is = jar.getInputStream(inEntry)) {
                        if (
                            !inEntry.isDirectory() &&
                            inEntry.getName().endsWith(".class")
                        ) {
                            rep.scannedClasses++;
                            if (
                                eligibleClass(
                                    inEntry.getName(),
                                    includeClass,
                                    cfg
                                ) &&
                                (cfg.entryLog || cfg.timing)
                            ) {
                                byte[] original = is.readAllBytes();
                                TransformReport classRep =
                                    new TransformReport();
                                byte[] transformed = transformClassBytes(
                                    original,
                                    cfg,
                                    includeMethod,
                                    classRep
                                );
                                jos.write(transformed);
                                rep.transformedClasses +=
                                    classRep.transformedClasses;
                                rep.transformedMethods +=
                                    classRep.transformedMethods;
                                rep.methods.addAll(classRep.methods);
                            } else {
                                is.transferTo(jos);
                            }
                        } else {
                            is.transferTo(jos);
                        }
                    }

                    jos.closeEntry();
                }
            }
        }

        return rep;
    }

    private static boolean eligibleClass(
        String entryName,
        Pattern includeClass,
        Config cfg
    ) {
        String name = entryName.endsWith(".class")
            ? entryName.substring(0, entryName.length() - 6)
            : entryName;
        if (
            name.endsWith("module-info") || name.endsWith("package-info")
        ) return false;
        if (
            includeClass != null &&
            !includeClass.matcher(name.replace('/', '.')).find()
        ) return false;
        for (String ex : cfg.excludeClassRegex) {
            if (
                Pattern.compile(ex).matcher(name.replace('/', '.')).find()
            ) return false;
        }
        return true;
    }

    private static byte[] transformClassBytes(
        byte[] bytes,
        Config cfg,
        Pattern includeMethod,
        TransformReport rep
    ) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        ClassTransformVisitor cv = new ClassTransformVisitor(
            cw,
            cfg,
            includeMethod,
            rep
        );
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private JarTransformer() {}
}
