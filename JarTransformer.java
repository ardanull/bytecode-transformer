package com.example.bctransform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public final class JarTransformer {

  public static final class Config {
    public boolean injectEntryLog = false;
    public boolean dryRun = false;

    /** Internal name prefix, e.g. "com/example/" (null => all). */
    public String includePrefix = null;

    /** Internal name prefixes to skip, e.g. "java/", "kotlin/", "org/slf4j/" */
    public final List<String> excludePrefixes = new ArrayList<>(List.of(
        "java/", "javax/", "jdk/", "sun/", "kotlin/"
    ));
  }

  public static int transformJar(Path inputJar, Path outputJar, Config cfg) throws IOException {
    Objects.requireNonNull(inputJar, "inputJar");
    Objects.requireNonNull(outputJar, "outputJar");
    Objects.requireNonNull(cfg, "cfg");

    int transformedCount = 0;

    try (JarFile jarFile = new JarFile(inputJar.toFile())) {
      Manifest manifest = jarFile.getManifest();

      if (cfg.dryRun) {
        // Still scan to report transform count, but don't write output.
        Enumeration<JarEntry> en = jarFile.entries();
        while (en.hasMoreElements()) {
          JarEntry e = en.nextElement();
          if (e.isDirectory() || !e.getName().endsWith(".class")) continue;
          if (shouldTransform(e.getName(), cfg)) transformedCount++;
        }
        return transformedCount;
      }

      Files.createDirectories(outputJar.toAbsolutePath().getParent());

      try (OutputStream fos = Files.newOutputStream(outputJar);
           BufferedOutputStream bos = new BufferedOutputStream(fos);
           JarOutputStream jos = (manifest != null) ? new JarOutputStream(bos, manifest) : new JarOutputStream(bos)) {

        Set<String> written = new HashSet<>();

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry inEntry = entries.nextElement();

          // Skip manifest if JarOutputStream already wrote it from Manifest
          if (manifest != null && "META-INF/MANIFEST.MF".equalsIgnoreCase(inEntry.getName())) {
            continue;
          }

          if (!written.add(inEntry.getName())) {
            continue; // avoid duplicates
          }

          JarEntry outEntry = new JarEntry(inEntry.getName());
          outEntry.setTime(inEntry.getTime());
          jos.putNextEntry(outEntry);

          try (InputStream is = jarFile.getInputStream(inEntry)) {
            if (!inEntry.isDirectory() && inEntry.getName().endsWith(".class") && shouldTransform(inEntry.getName(), cfg)) {
              byte[] original = is.readAllBytes();
              byte[] transformed = transformClassBytes(original, cfg);
              jos.write(transformed);
              transformedCount++;
            } else {
              // copy as-is
              is.transferTo(jos);
            }
          }

          jos.closeEntry();
        }
      }
    }

    return transformedCount;
  }

  private static boolean shouldTransform(String entryName, Config cfg) {
    // entryName already internal-like path: com/example/Foo.class
    String internal = entryName;
    if (internal.endsWith(".class")) internal = internal.substring(0, internal.length() - ".class".length());

    if (cfg.includePrefix != null && !internal.startsWith(cfg.includePrefix.replace('.', '/'))) return false;

    for (String p : cfg.excludePrefixes) {
      if (internal.startsWith(p.replace('.', '/'))) return false;
    }

    // Skip module-info, package-info by default (usually not worth touching)
    if (internal.endsWith("module-info") || internal.endsWith("package-info")) return false;

    return cfg.injectEntryLog; // only transform if some transform enabled
  }

  private static byte[] transformClassBytes(byte[] classBytes, Config cfg) {
    ClassReader cr = new ClassReader(classBytes);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);

    var cv = cw;
    if (cfg.injectEntryLog) {
      cv = new EntryLoggingClassVisitor(cv);
    }

    // EXPAND_FRAMES makes AdviceAdapter safer when COMPUTE_FRAMES is used
    cr.accept(cv, ClassReader.EXPAND_FRAMES);
    return cw.toByteArray();
  }

  private JarTransformer() {}
}
