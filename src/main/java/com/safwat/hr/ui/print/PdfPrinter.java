package com.safwat.hr.ui.print;

import com.safwat.hr.ui.print.PrintChoices.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.print.DocFlavor;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.SheetCollate;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * التنفيذ الفعلي: الطباعة على الطابعة، أو الحفظ كملف PDF.
 */
public final class PdfPrinter {

    private static final double EXPORT_DPI = 150.0;

    private PdfPrinter() {
    }

    // ══════════════════════════ الطباعة ══════════════════════════

    public static void print(Path pdf, PrintOptions o) throws IOException, PrinterException {
        checkPrinterSupport(o);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(o.service);

            PageFormat pf = PdfRenderer.pageFormat(job.defaultPage(), o.paper, o.orientation, o.margin);
            PdfSheetPrintable sheets = PdfRenderer.sheets(doc, o);

            Book book = new Book();
            book.append(sheets, pf, sheets.getSheetCount());
            job.setPageable(book);

            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(o.copies));
            if (o.sides.attribute != null) attrs.add(o.sides.attribute);
            attrs.add(o.color.attribute);
            attrs.add(o.collate ? SheetCollate.COLLATED : SheetCollate.UNCOLLATED);

            job.print(attrs);
        }
    }

    private static void checkPrinterSupport(PrintOptions o) throws PrinterException {
        if (o.service == null) {
            throw new PrinterException("لا توجد طابعة مختارة");
        }
        DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;

        if (o.sides.attribute != null && !o.service.isAttributeValueSupported(o.sides.attribute, flavor, null)) {
            throw new PrinterException("الطابعة \"" + o.service.getName() + "\" لا تدعم: " + o.sides.label);
        }
        if (!o.service.isAttributeValueSupported(o.color.attribute, flavor, null)) {
            throw new PrinterException("الطابعة \"" + o.service.getName() + "\" لا تدعم: " + o.color.label);
        }
    }

    // ══════════════════════════ الحفظ كـ PDF ══════════════════════════

    /**
     * يرسم كل ورقة في صورة (150 DPI) ويحطها في PDF جديد.
     * النص في الملف الناتج مش هيبقى قابل للتحديد، لأن كل صفحة صورة.
     */
    public static void export(Path pdf, File out, PrintOptions o) throws IOException, PrinterException {
        try (PDDocument src = Loader.loadPDF(pdf.toFile());
             PDDocument dst = new PDDocument()) {

            PdfSheetPrintable sheets = PdfRenderer.sheets(src, o);
            PageFormat pf = PdfRenderer.pageFormat(new PageFormat(), o.paper, o.orientation, o.margin);

            for (int i = 0; i < sheets.getSheetCount(); i++) {
                BufferedImage img = PdfRenderer.renderSheet(o, sheets, i, EXPORT_DPI);

                PDRectangle box = new PDRectangle((float) pf.getWidth(), (float) pf.getHeight());
                PDPage page = new PDPage(box);
                dst.addPage(page);

                PDImageXObject image = JPEGFactory.createFromImage(dst, img, 0.9f);
                try (PDPageContentStream cs = new PDPageContentStream(dst, page)) {
                    cs.drawImage(image, 0, 0, box.getWidth(), box.getHeight());
                }
            }

            dst.save(out);
        }
    }
}