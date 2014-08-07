package org.esa.beam.framework.datamodel;

import java.awt.*;

/**
 * Class representing the size, the tiling and the geo-coding of an image.
 */
public interface ImageGeometry {

    /**
     * @return The size in pixel of the image.
     */
    Dimension getSize();

    /**
     * @return The width in pixel of the image.
     */
    int getWidth();

    /**
     * @return The height in pixel of the image.
     */
    int getHeight();

    /**
     * @return The width in pixel of the tiles of the image.
     */
    int getTileWidth();

    /**
     * @return The height in pixel of the tiles of the image.
     */
    int getTileHeight();

    /**
     * @return The geo-coding of this image, or {@code null} if the image is not geo-coded.
     */
    GeoCoding getGeoCoding();

}
