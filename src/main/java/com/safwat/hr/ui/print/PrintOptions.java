package com.safwat.hr.ui.print;

import com.safwat.hr.ui.print.PrintChoices.*;

import javax.print.PrintService;
import java.util.List;

/**
 * كل الخيارات اللي المستخدم اختارها في الحوار، بتتمرر للطباعة أو المعاينة أو الحفظ.
 */
public final class PrintOptions {

    public PrintService service;          // ممكن يكون null لو الحفظ كـ PDF
    public PaperOption paper;
    public SidesOption sides;
    public OrientationOption orientation;
    public PerSheetOption perSheet;
    public MarginOption margin;
    public List<Integer> pages;           // أرقام من 1، أو null يعني كل الصفحات
    public int copies;
    public boolean collate;
    public ColorOption color;
    public boolean saveAsPdf;
}