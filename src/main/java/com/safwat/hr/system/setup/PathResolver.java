package com.safwat.hr.system.setup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * يكتشف بنية التوزيع تلقائياً:
 * <pre>
 *   hr-mate-win/
 *   ├── hr-mate/         ← الفرونت (هنا يعمل التطبيق)
 *   ├── hr-mate-system/  ← الباك إند
 *   └── pgsql/           ← قاعدة البيانات
 * </pre>
 */
public final class PathResolver {

    public static final class Distribution {
        public Path root;
        public Path frontend;
        public Path backend;
        public Path pgRoot;

        public Path pgBin() {
            return pgRoot.resolve("bin");
        }

        public Path pgData() {
            return pgRoot.resolve("data");
        }

        public Path pgInitDb() {
            return pgBin().resolve(win() ? "initdb.exe" : "initdb");
        }

        public Path pgCtl() {
            return pgBin().resolve(win() ? "pg_ctl.exe" : "pg_ctl");
        }

        public Path pgConf() {
            return pgData().resolve("postgresql.conf");
        }

        public Path backendExe() {
            return backend.resolve(win() ? "hr-mate-system.exe" : "hr-mate-system");
        }

        public Path backendConfig() {
            return backend.resolve("app/config/application.properties");
        }

        // داخل PathResolver.Distribution
        public Path backendAppConfig() {
            return backend.resolve("app").resolve("config").resolve("app_config.json");
        }

        public Path frontendConfig() {
            return frontend.resolve("config/app_config.json");
        }

        public boolean signature() {
            return Files.isDirectory(pgRoot) && Files.isDirectory(backend);
        }

        @Override
        public String toString() {
            return "root=" + root + " | fe=" + frontend
                    + " | be=" + backend + " | pg=" + pgRoot;
        }
    }

    private static boolean win() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * يبدأ من user.dir ويصعد للأعلى للبحث عن التوقيع
     */
    public static Optional<Distribution> detect() {
        Path cursor = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 4 && cursor != null; i++) {
            Optional<Distribution> d = tryAt(cursor);
            if (d.isPresent()) return d;
            cursor = cursor.getParent();
        }
        return Optional.empty();
    }

    private static Optional<Distribution> tryAt(Path candidate) {
        // صيغة 1: candidate = hr-mate-win
        Path pg = candidate.resolve("pgsql");
        Path be = candidate.resolve("hr-mate-system");
        Path fe = candidate.resolve("hr-mate");
        if (Files.isDirectory(pg) && Files.isDirectory(be)) {
            Distribution d = new Distribution();
            d.root = candidate;
            d.pgRoot = pg;
            d.backend = be;
            d.frontend = Files.isDirectory(fe) ? fe : candidate;
            return Optional.of(d);
        }
        // صيغة 2: candidate = hr-mate → الأب هو root
        Path parent = candidate.getParent();
        if (parent != null) {
            Path pg2 = parent.resolve("pgsql");
            Path be2 = parent.resolve("hr-mate-system");
            if (Files.isDirectory(pg2) && Files.isDirectory(be2)) {
                Distribution d = new Distribution();
                d.root = parent;
                d.pgRoot = pg2;
                d.backend = be2;
                d.frontend = candidate;
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }
}