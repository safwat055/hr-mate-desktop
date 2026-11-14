package com.safwat.hr.ui.print;

import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Sides;

/**
 * كل الخيارات المتاحة في حوار الطباعة.
 */
public final class PrintChoices {

    private PrintChoices() {
    }

    public enum PaperOption {
        A4("A4", 595, 842),
        A3("A3", 842, 1191);

        public final String label;
        public final double widthPt;
        public final double heightPt;

        PaperOption(String label, double widthPt, double heightPt) {
            this.label = label;
            this.widthPt = widthPt;
            this.heightPt = heightPt;
        }

        public static PaperOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return A4; }
        }

        @Override
        public String toString() { return label; }
    }

    public enum SidesOption {
        ONE_SIDED("وجه واحد", Sides.ONE_SIDED),
        TWO_SIDED_LONG("وجهين - قلب على الحرف الطويل", Sides.TWO_SIDED_LONG_EDGE),
        TWO_SIDED_SHORT("وجهين - قلب على الحرف القصير", Sides.TWO_SIDED_SHORT_EDGE);

        public final String label;
        public final Sides attribute;

        SidesOption(String label, Sides attribute) {
            this.label = label;
            this.attribute = attribute;
        }

        public static SidesOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return ONE_SIDED; }
        }

        @Override
        public String toString() { return label; }
    }

    public enum OrientationOption {
        PORTRAIT("طولي"),
        LANDSCAPE("عرضي");

        public final String label;

        OrientationOption(String label) {
            this.label = label;
        }

        public static OrientationOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return PORTRAIT; }
        }

        @Override
        public String toString() { return label; }
    }

    public enum ColorOption {
        COLOR("ملون", Chromaticity.COLOR),
        MONOCHROME("أبيض وأسود", Chromaticity.MONOCHROME);

        public final String label;
        public final Chromaticity attribute;

        ColorOption(String label, Chromaticity attribute) {
            this.label = label;
            this.attribute = attribute;
        }

        public static ColorOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return COLOR; }
        }

        @Override
        public String toString() { return label; }
    }

    public enum PerSheetOption {
        ONE("صفحة واحدة", 1),
        TWO("صفحتين", 2),
        FOUR("4 صفحات", 4);

        public final String label;
        public final int count;

        PerSheetOption(String label, int count) {
            this.label = label;
            this.count = count;
        }

        public static PerSheetOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return ONE; }
        }

        @Override
        public String toString() { return label; }
    }

    public enum MarginOption {
        SMALL("صغيرة", 9),
        NORMAL("عادية", 18),
        LARGE("كبيرة", 36);

        public final String label;
        public final double points;

        MarginOption(String label, double points) {
            this.label = label;
            this.points = points;
        }

        public static MarginOption fromName(String name) {
            try { return valueOf(name); } catch (Exception e) { return NORMAL; }
        }

        @Override
        public String toString() { return label; }
    }
}