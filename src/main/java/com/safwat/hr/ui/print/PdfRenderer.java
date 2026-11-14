package com.safwat.hr.ui.print;

import com.safwat.hr.ui.print.PrintChoices.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.util.List;

/**
 * الكود المشترك للتخطيط والرسم، بيُستخدم في الطباعة والمعاينة والحفظ كـ PDF.
 */
public final class PdfRenderer {

    private PdfRenderer() {
    }

    /**
     * يبني PageFormat بالمقاس والاتجاه والهامش.
     * الاتجاه العرضي بيتعمل بتبديل أبعاد الورقة، والـ PageFormat نفسه بيفضل طولي.
     */
    public static PageFormat pageFormat(PageFormat base, PaperOption paper,
                                        OrientationOption orientation, MarginOption margin) {
        PageFormat pf = (PageFormat) base.clone();

        boolean landscape = orientation == OrientationOption.LANDSCAPE;
        double w = landscape ? paper.heightPt : paper.widthPt;
        double h = landscape ? paper.widthPt : paper.heightPt;
        double m = margin.points;

        Paper p = new Paper();
        p.setSize(w, h);
        p.setImageableArea(m, m, w - 2 * m, h - 2 * m);

        pf.setPaper(p);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

    /**
     * يجهز تقسيم الصفحات على الأوراق بناءً على نطاق الصفحات وعدد الصفحات في الورقة.
     */
    public static PdfSheetPrintable sheets(PDDocument doc, PrintOptions o) {
        List<Integer> pages = PrintRange.resolve(o.pages, doc.getNumberOfPages());
        return new PdfSheetPrintable(
                new PDFPrintable(doc, Scaling.SCALE_TO_FIT),
                pages,
                o.perSheet.count,
                o.orientation == OrientationOption.LANDSCAPE
        );
    }

    /**
     * يرسم ورقة واحدة في صورة بدقة معينة (DPI).
     */
    public static BufferedImage renderSheet(PrintOptions o, PdfSheetPrintable sheets,
                                            int index, double dpi) throws PrinterException {
        PageFormat pf = pageFormat(new PageFormat(), o.paper, o.orientation, o.margin);

        double scale = dpi / 72.0;
        int w = (int) Math.round(pf.getWidth() * scale);
        int h = (int) Math.round(pf.getHeight() * scale);

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.scale(scale, scale);
            sheets.print(g, pf, index);
        } finally {
            g.dispose();
        }
        return img;
    }
}