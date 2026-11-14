package com.safwat.hr.ui.print;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.List;

/**
 * يرسم عدة صفحات PDF على ورقة واحدة في شبكة (1، 2، أو 4 خانات).
 */
public final class PdfSheetPrintable implements Printable {

    private final Printable source;
    private final List<Integer> pages; // فهارس من 0
    private final int perSheet;
    private final int cols;
    private final int rows;

    public PdfSheetPrintable(Printable source, List<Integer> pages, int perSheet, boolean landscape) {
        this.source = source;
        this.pages = pages;
        this.perSheet = perSheet;
        if (perSheet == 1) {
            cols = 1;
            rows = 1;
        } else if (perSheet == 2) {
            cols = landscape ? 2 : 1;
            rows = landscape ? 1 : 2;
        } else {
            cols = 2;
            rows = 2;
        }
    }

    public int getSheetCount() {
        return (pages.size() + perSheet - 1) / perSheet;
    }

    @Override
    public int print(Graphics graphics, PageFormat format, int sheetIndex) throws PrinterException {
        if (sheetIndex >= getSheetCount()) return NO_SUCH_PAGE;

        double x0 = format.getImageableX();
        double y0 = format.getImageableY();
        double cellW = format.getImageableWidth() / cols;
        double cellH = format.getImageableHeight() / rows;

        PageFormat cell = (PageFormat) format.clone();
        Paper cellPaper = new Paper();
        cellPaper.setSize(cellW, cellH);
        cellPaper.setImageableArea(0, 0, cellW, cellH);
        cell.setPaper(cellPaper);

        int first = sheetIndex * perSheet;
        for (int k = 0; k < perSheet; k++) {
            int idx = first + k;
            if (idx >= pages.size()) break;

            int row = k / cols;
            int col = k % cols;

            Graphics2D cg = (Graphics2D) graphics.create();
            try {
                cg.translate(x0 + col * cellW, y0 + row * cellH);
                cg.clip(new Rectangle2D.Double(0, 0, cellW, cellH));
                source.print(cg, cell, pages.get(idx));
            } finally {
                cg.dispose();
            }
        }
        return PAGE_EXISTS;
    }
}