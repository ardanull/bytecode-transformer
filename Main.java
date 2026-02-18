package com.example.bctransform;

import java.nio.file.Path;

public final class Main {
  private static void usage() {
  System.out.println("""
      Usage:
        java -jar bytecode-transformer.jar <input.jar> <output.jar> [options]

      Options:
        --include-prefix <pfx>   Only transform classes whose internal name starts with pfx (e.g. com/example/)
        --exclude-prefix <pfx>   Skip classes whose internal name starts with pfx (repeatable)
        --log                    Inject System.out.println at method entry
        --dry-run                Don't write output jar; just report what would be transformed

      Examples:
        java -jar bytecode-transformer.jar in.jar out.jar --log --include-prefix com/myapp/
      """");
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      usage();
      System.exit(2);
    }

    Path in = Path.of(args[0]);
    Path out = Path.of(args[1]);

    JarTransformer.Config cfg = new JarTransformer.Config();
    for (int i = 2; i < args.length; i++) {
      String a = args[i];
      switch (a) {
        case "--include-prefix" -> {
          if (i + 1 >= args.length) throw new IllegalArgumentException("--include-prefix needs value");
          cfg.includePrefix = args[++i];
        }
        case "--exclude-prefix" -> {
          if (i + 1 >= args.length) throw new IllegalArgumentException("--exclude-prefix needs value");
          cfg.excludePrefixes.add(args[++i]);
        }
        case "--log" -> cfg.injectEntryLog = true;
        case "--dry-run" -> cfg.dryRun = true;
        case "--help", "-h" -> {
          usage();
          return;
        }
        default -> throw new IllegalArgumentException("Unknown arg: " + a);
      }
    }

    int transformed = JarTransformer.transformJar(in, out, cfg);
    System.out.println("Done. Transformed classes: " + transformed + (cfg.dryRun ? " (dry-run)" : ""));
  }
}
