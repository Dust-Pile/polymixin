package dev.polymixin.core.diagnostics;

public final class Log {

    private static final String PREFIX = "[polymixin] ";

    private static Object logger;
    private static boolean loggerResolved;

    private Log() {
    }

    public static void debug(String format, Object... args) {
        log("DEBUG", format, args);
    }

    public static void info(String format, Object... args) {
        log("INFO", format, args);
    }

    public static void warn(String format, Object... args) {
        log("WARN", format, args);
    }

    public static void error(String format, Object... args) {
        log("ERROR", format, args);
    }

    private static void log(String level, String format, Object[] args) {
        Object target = resolveLogger();
        if (target != null && invokeMixinLogger(target, level, format, args)) {
            return;
        }
        System.out.println(PREFIX + level + " " + format(format, args));
        for (Object arg : args) {
            if (arg instanceof Throwable) {
                ((Throwable) arg).printStackTrace(System.out);
            }
        }
    }

    private static boolean invokeMixinLogger(Object target, String level, String format, Object[] args) {
        try {
            String method = level.toLowerCase(java.util.Locale.ROOT);
            target.getClass()
                    .getMethod(method, String.class, Object[].class)
                    .invoke(target, PREFIX + format, args);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private static synchronized Object resolveLogger() {
        if (!loggerResolved) {
            loggerResolved = true;
            try {
                Class<?> service = Class.forName("org.spongepowered.asm.service.MixinService");
                Object instance = service.getMethod("getService").invoke(null);
                logger = instance.getClass().getMethod("getLogger", String.class).invoke(instance, "polymixin");
            } catch (Throwable th) {
                logger = null;
            }
        }
        return logger;
    }

    public static String format(String format, Object... args) {
        StringBuilder sb = new StringBuilder();
        int arg = 0;
        int i = 0;
        while (i < format.length()) {
            int idx = format.indexOf("{}", i);
            if (idx < 0 || arg >= args.length) {
                sb.append(format, i, format.length());
                break;
            }
            sb.append(format, i, idx);
            sb.append(args[arg++]);
            i = idx + 2;
        }
        return sb.toString();
    }
}
