package com.boilerplate.app.model;

/**
 * KDP Template configuration for PDF generation.
 * Defines page size, margins, layout, and typography.
 */
public class KdpTemplate {
    private String name;
    private double pageWidth; // inches
    private double pageHeight; // inches
    private ImageLayout layout;
    private double marginTop;
    private double marginBottom;
    private double marginInside; // Binding side (larger for gutter)
    private double marginOutside;
    private int fontSize;
    private String fontFamily;
    private boolean hasBleed;

    /**
     * Default constructor for serialization.
     */
    public KdpTemplate() {
    }

    /**
     * Full constructor.
     */
    public KdpTemplate(String name, double pageWidth, double pageHeight,
            ImageLayout layout, double marginTop, double marginBottom,
            double marginInside, double marginOutside,
            int fontSize, String fontFamily, boolean hasBleed) {
        this.name = name;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.layout = layout;
        this.marginTop = marginTop;
        this.marginBottom = marginBottom;
        this.marginInside = marginInside;
        this.marginOutside = marginOutside;
        this.fontSize = fontSize;
        this.fontFamily = fontFamily;
        this.hasBleed = hasBleed;
    }

    // === Page dimensions in points (72 points = 1 inch) ===

    public int getPageWidthPoints() {
        return (int) (pageWidth * 72);
    }

    public int getPageHeightPoints() {
        return (int) (pageHeight * 72);
    }

    public int getMarginTopPoints() {
        return (int) (marginTop * 72);
    }

    public int getMarginBottomPoints() {
        return (int) (marginBottom * 72);
    }

    public int getMarginInsidePoints() {
        return (int) (marginInside * 72);
    }

    public int getMarginOutsidePoints() {
        return (int) (marginOutside * 72);
    }

    // === Getters and Setters ===

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(double pageWidth) {
        this.pageWidth = pageWidth;
    }

    public double getPageHeight() {
        return pageHeight;
    }

    public void setPageHeight(double pageHeight) {
        this.pageHeight = pageHeight;
    }

    public ImageLayout getLayout() {
        return layout;
    }

    public void setLayout(ImageLayout layout) {
        this.layout = layout;
    }

    public double getMarginTop() {
        return marginTop;
    }

    public void setMarginTop(double marginTop) {
        this.marginTop = marginTop;
    }

    public double getMarginBottom() {
        return marginBottom;
    }

    public void setMarginBottom(double marginBottom) {
        this.marginBottom = marginBottom;
    }

    public double getMarginInside() {
        return marginInside;
    }

    public void setMarginInside(double marginInside) {
        this.marginInside = marginInside;
    }

    public double getMarginOutside() {
        return marginOutside;
    }

    public void setMarginOutside(double marginOutside) {
        this.marginOutside = marginOutside;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public boolean isHasBleed() {
        return hasBleed;
    }

    public void setHasBleed(boolean hasBleed) {
        this.hasBleed = hasBleed;
    }

    @Override
    public String toString() {
        return name + " (" + pageWidth + "x" + pageHeight + "\")";
    }
}
