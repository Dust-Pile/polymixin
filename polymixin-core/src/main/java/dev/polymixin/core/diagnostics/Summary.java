package dev.polymixin.core.diagnostics;

import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import dev.polymixin.core.scan.ScanCache;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Summary {

    public static final String PROP_DELAY = "polymixin.summary.delaySeconds";

    private static final Map<String, MixinStats> STATS = new LinkedHashMap<>();
    private static final AtomicBoolean PRINTED = new AtomicBoolean();

    private static volatile long lastChange;
    private static volatile boolean armed;

    private Summary() {
    }

    public static synchronized MixinStats stats(String mixinName) {
        return STATS.computeIfAbsent(mixinName, MixinStats::new);
    }

    public static void noteApplied(String mixinClassName) {
        GeneratedMixin generated = GeneratedRegistry.byGeneratedName(mixinClassName);
        if (generated == null) {
            return;
        }
        generated.markApplied();
        lastChange = System.nanoTime();
    }

    public static synchronized void arm() {
        if (armed) {
            return;
        }
        armed = true;
        lastChange = System.nanoTime();

        long delay = delaySeconds();
        Thread watcher = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(1000L);
                    if (System.nanoTime() - lastChange > delay * 1_000_000_000L) {
                        print();
                        return;
                    }
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "polymixin-summary");
        watcher.setDaemon(true);
        watcher.start();

        Runtime.getRuntime().addShutdownHook(new Thread(Summary::print, "polymixin-summary-shutdown"));
    }

    public static void print() {
        if (!PRINTED.compareAndSet(false, true)) {
            return;
        }
        List<MixinStats> all;
        synchronized (Summary.class) {
            all = new ArrayList<>(STATS.values());
        }
        if (all.isEmpty()) {
            return;
        }
        Log.info("dynamic targeting summary ({} ms scan, {} roots, {} classes)",
                ScanCache.elapsedMillis(), ScanCache.rootCount(), ScanCache.classCount());
        for (MixinStats stats : all) {
            List<GeneratedMixin> generated = GeneratedRegistry.forOriginal(stats.mixinName);
            int applied = 0;
            List<GeneratedMixin> failed = new ArrayList<>();
            for (GeneratedMixin mixin : generated) {
                if (mixin.applied()) {
                    applied++;
                }
                if (mixin.failure() != null) {
                    failed.add(mixin);
                }
            }
            Log.info("  {} ({})", stats.mixinName, stats.configName);
            Log.info("      declared   {}  {}", stats.declared.size(), String.join(", ", stats.declared));
            Log.info("      discovered {}", stats.discovered);
            Log.info("      generated  {}{}", generated.size(), stats.skipped == 0 ? "" : "   (" + stats.skipped + " skipped: " + stats.skipReasons() + ")");
            Log.info("      applied    {}", applied);
            Log.info("      failed     {}", failed.size());
            for (GeneratedMixin mixin : failed) {
                Log.info("          {}  ({})", mixin.targetName(), mixin.failure());
            }
        }
    }

    private static long delaySeconds() {
        try {
            return Math.max(1L, Long.parseLong(System.getProperty(PROP_DELAY, "30")));
        } catch (NumberFormatException ex) {
            return 30L;
        }
    }

    public static final class MixinStats {

        final String mixinName;
        String configName = "";
        List<String> declared = new ArrayList<>();
        int discovered;
        int skipped;
        private final Map<String, Integer> skips = new LinkedHashMap<>();

        MixinStats(String mixinName) {
            this.mixinName = mixinName;
        }

        public void configName(String name) {
            this.configName = name;
        }

        public void declared(List<String> targets) {
            this.declared = new ArrayList<>(targets);
        }

        public void discovered(int count) {
            this.discovered = count;
        }

        public void skip(String reason) {
            this.skipped++;
            this.skips.merge(reason, 1, Integer::sum);
        }

        String skipReasons() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : this.skips.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(entry.getValue()).append(' ').append(entry.getKey());
            }
            return sb.toString();
        }
    }
}
