package com.openhtmltopdf.pdfboxout;

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

public abstract class AnnotationContainer {
    public void setRectangle(PDRectangle rectangle) {
        getPdAnnotation().setRectangle(rectangle);
    }

    public void setPrinted(boolean printed) {
        getPdAnnotation().setPrinted(printed);
    }

    public void setQuadPoints(float[] quadPoints) {};

    public abstract void setBorderStyle(PDBorderStyleDictionary styleDict);

    public abstract PDAnnotation getPdAnnotation();

    public static class PDAnnotationFileAttachmentContainer extends AnnotationContainer {
        private final PDAnnotationFileAttachment pdAnnotationFileAttachment;

        public PDAnnotationFileAttachmentContainer(PDAnnotationFileAttachment pdAnnotationFileAttachment) {
            this.pdAnnotationFileAttachment = pdAnnotationFileAttachment;
        }

        @Override
        public PDAnnotation getPdAnnotation() {
            return pdAnnotationFileAttachment;
        }

        @Override
        public void setBorderStyle(PDBorderStyleDictionary styleDict) {
            pdAnnotationFileAttachment.setBorderStyle(styleDict);
        }
    }

    public static class PDAnnotationLinkContainer extends AnnotationContainer {
        private final PDAnnotationLink pdAnnotationLink;

        public PDAnnotationLinkContainer(PDAnnotationLink pdAnnotationLink) {
            this.pdAnnotationLink = pdAnnotationLink;
        }

        @Override
        public PDAnnotation getPdAnnotation() {
            return pdAnnotationLink;
        }

        @Override
        public void setQuadPoints(float[] quadPoints) {
            pdAnnotationLink.setQuadPoints(quadPoints);
        }

        @Override
        public void setBorderStyle(PDBorderStyleDictionary styleDict) {
            pdAnnotationLink.setBorderStyle(styleDict);
        }
    }
}
