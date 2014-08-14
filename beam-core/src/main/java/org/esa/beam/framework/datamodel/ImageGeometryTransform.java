package org.esa.beam.framework.datamodel;

import java.awt.image.RenderedImage;

/**
 * The <code>ImageGeometryTransform</code> interface can be implemented to define different ways of transformation.
 * The source image will be transformed (extended, cropped, scaled, etc.) to the provided target image geometry.
 */
public interface ImageGeometryTransform {

    /**
     * Transforms the source image to the provided target geometry while considering the geo-coding of the source.
     *
     * @param source          The source image to be transformed.
     * @param sourceGeoCoding The geo-coding of the source image.
     * @param targetGeometry  The image geometry the returned image will have.
     * @return The transformed image.
     */
    RenderedImage transform(RenderedImage source, GeoCoding sourceGeoCoding, ImageGeometry targetGeometry);
}
