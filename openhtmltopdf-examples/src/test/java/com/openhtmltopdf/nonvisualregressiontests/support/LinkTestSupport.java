package com.openhtmltopdf.nonvisualregressiontests.support;

import java.io.IOException;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;

import com.openhtmltopdf.util.ArrayUtil;

public class LinkTestSupport {
    private static boolean floatEqual(float f1, float f2) {
        return Float.isFinite(f1) && Float.isFinite(f2) && Math.abs(f2 - f1) < 0.5f;
    }

    public static class PDRectEquals extends CustomTypeSafeMatcher<PDRectangle> {
        final PDRectangle expected;
        final int pageNo;
        final int annotIndex;

        PDRectEquals(int pageNo, int annotIdx, PDRectangle expected) {
            super(String.format(Locale.ROOT, "Comparing link: page %d, annotation %d", pageNo, annotIdx));
            this.expected = expected;
            this.pageNo = pageNo;
            this.annotIndex = annotIdx;
        }

        String describe(String property, float f1, float f2) {
            return floatEqual(f1, f2) ? "" :
                String.format(Locale.ROOT, "For %s, was expecting %.2f but got %.2f", property, f1, f2);
        }

        @Override
        protected void describeMismatchSafely(PDRectangle actual, Description mismatch) {
            String desc = ArrayUtil.join(new String[] {
                    describe("lowerLeftX", expected.getLowerLeftX(), actual.getLowerLeftX()),
                    describe("lowerLeftY", expected.getLowerLeftY(), actual.getLowerLeftY()),
                    describe("width", expected.getWidth(), actual.getWidth()),
                    describe("height", expected.getHeight(), actual.getHeight()) }, "\n");
            mismatch.appendText(desc);
        }

        @Override
        protected boolean matchesSafely(PDRectangle actual) {
            return floatEqual(expected.getLowerLeftX(), actual.getLowerLeftX()) &&
                   floatEqual(expected.getLowerLeftY(), actual.getLowerLeftY()) &&
                   floatEqual(expected.getUpperRightX(), actual.getUpperRightX()) &&
                   floatEqual(expected.getUpperRightY(), actual.getUpperRightY());
        }
    }

    public static PDRectEquals pdRectEquals(int pageNo, int annotIdx, float lowerX, float lowerY, float width, float height) {
        return new PDRectEquals(
                pageNo, annotIdx,
                new PDRectangle(lowerX, lowerY, width, height));
    }

    public static PDRectangle linkArea(PDDocument doc, int page, int annotIndex) throws IOException {
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(page).getAnnotations().get(annotIndex);
        return link.getRectangle();
    }

    public static PDPageXYZDestination linkDestinationXYZ(PDDocument doc, int page, int annotIndex) throws IOException {
        PDAnnotationLink link0 = (PDAnnotationLink) doc.getPage(page).getAnnotations().get(annotIndex);
        if (link0.getAction() instanceof PDActionGoTo) {
            PDActionGoTo goto0 = (PDActionGoTo) link0.getAction();
            if (goto0.getDestination() instanceof PDPageXYZDestination) {
                return (PDPageXYZDestination) goto0.getDestination();
            }
        }
        return null;
    }

    public static int linkDestinationPageNo(PDDocument doc, int page, int annotIdx) throws IOException {
        return doc.getPages().indexOf(linkDestinationXYZ(doc, page, annotIdx).getPage());
    }

    public static int linkDestinationTop(PDDocument doc, int page, int annotIdx) throws IOException {
        return linkDestinationXYZ(doc, page, annotIdx).getTop();
    }

    public static String linkDestinationUri(PDDocument doc, int page, int annotIndex) throws IOException {
        PDAnnotationLink link0 = (PDAnnotationLink) doc.getPage(page).getAnnotations().get(annotIndex);
        if (link0.getAction() instanceof PDActionURI) {
            PDActionURI uriAction = (PDActionURI) link0.getAction();
            return uriAction.getURI();
        }
        return null;
    }

}
