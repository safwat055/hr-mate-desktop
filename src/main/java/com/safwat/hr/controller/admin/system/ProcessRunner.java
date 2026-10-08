package com.safwat.hr.controller.admin.system;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * تشغيل الأوامر بدون shell (ProcessBuilder بـ list) + إدارة شجرة العمليات بـ ProcessHandle.
 * بيشتغل على ويندوز ولينكس من غير أوامر خاصة بنظام معين.
 */
final class ProcessRunner {

    static final int SVC_STOPPED = 1;
    static final int SVC_RUNNING = 4;

    /** سطور sc query: "TYPE : 10 ..." و"STATE : 4 ..." — بنعتمد على الأرقام مش الكلمات عشان الترجمة. */
    private static final Pattern SC_NUMBER = Pattern.compile("(?m)^\\s*\\S[^:\\n]*:\\s*(\\d+)\\s+");

    record Result(int exitCode, String output) {
        boolean ok() {
            return exitCode == 0;
        }
    }

    private ProcessRunner() {
    }

    static Result run(long timeoutSec, List<String> cmd) throws IOException, InterruptedException {
        return run(timeoutSec, cmd, null, Map.of());
    }

    static Result run(long timeoutSec, List<String> cmd, String stdin, Map<String, String> env)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        pb.environment().putAll(env);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (var in = p.getInputStream()) {
                out.append(new String(in.readAllBytes(), Charset.defaultCharset()));
            } catch (IOException ignored) {
            }
        }, "proc-reader");
        reader.setDaemon(true);
        reader.start();

        try (var os = p.getOutputStream()) {
            if (stdin != null) os.write(stdin.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // العملية ممكن تقفل قبل ما نكتب كل الإدخال
        }

        if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            reader.join(1000);
            return new Result(-1, out + "\n[انتهى الوقت]");
        }
        reader.join(2000);
        return new Result(p.exitValue(), out.toString().trim());
    }

    // ══════════════════ Process tree ══════════════════

    static Optional<ProcessHandle> handle(long pid) {
        return ProcessHandle.of(pid).filter(ProcessHandle::isAlive);
    }

    /** يقفل العملية وكل الـ children بتاعتها. */
    static boolean killTree(ProcessHandle h) {
        if (h == null || !h.isAlive()) return true;
        h.descendants().forEach(ProcessHandle::destroy);
        h.destroy();
        if (!waitExit(h, 10)) {
            h.descendants().forEach(ProcessHandle::destroyForcibly);
            h.destroyForcibly();
            waitExit(h, 5);
        }
        return !h.isAlive();
    }

    private static boolean waitExit(ProcessHandle h, long seconds) {
        try {
            h.onExit().get(seconds, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return !h.isAlive();
        } catch (Exception e) {
            return !h.isAlive();
        }
    }

    static boolean matchesExe(ProcessHandle h, Path exe) {
        return h.info().command().map(c -> samePath(c, exe)).orElse(false);
    }

    /** كل العمليات اللي شغالة من نفس الملف بالظبط (المسار الكامل). */
    static List<ProcessHandle> findByExecutable(Path exe) {
        if (exe == null) return List.of();
        return ProcessHandle.allProcesses()
                .filter(h -> matchesExe(h, exe))
                .toList();
    }

    private static boolean samePath(String command, Path exe) {
        try {
            Path a = Paths.get(command).toAbsolutePath().normalize();
            Path b = exe.toAbsolutePath().normalize();
            return OsSupport.WINDOWS
                    ? a.toString().equalsIgnoreCase(b.toString())
                    : a.equals(b);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    // ══════════════════ Windows services (sc) ══════════════════

    static boolean serviceInstalled(String name) {
        if (!OsSupport.WINDOWS || name == null || name.isBlank()) return false;
        try {
            return run(10, List.of("sc", "query", name)).ok();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /** يرجع رقم الحالة (1 = STOPPED، 4 = RUNNING) أو null. */
    static Integer serviceState(String name) {
        if (!OsSupport.WINDOWS || name == null || name.isBlank()) return null;
        try {
            Result r = run(10, List.of("sc", "query", name));
            if (!r.ok()) return null;
            Matcher m = SC_NUMBER.matcher(r.output());
            List<Integer> nums = new ArrayList<>();
            while (m.find()) nums.add(Integer.valueOf(m.group(1)));
            // الرقم التاني = STATE
            return nums.size() >= 2 ? nums.get(1) : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    static boolean serviceRunning(String name) {
        Integer s = serviceState(name);
        return s != null && s == SVC_RUNNING;
    }

    // ══════════════════ Helpers ══════════════════

    static boolean waitUntil(BooleanSupplier condition, int seconds) throws InterruptedException {
        for (int i = 0; i < seconds; i++) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(1000);
        }
        return condition.getAsBoolean();
    }
}