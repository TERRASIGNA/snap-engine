package org.esa.beam.framework.datamodel;

import java.awt.Dimension;

class SceneImageGeometry implements ImageGeometry {

    private Dimension size;
    private int tileWidth;
    private int tileHeight;
    private GeoCoding geoCoding;

    @Override
    public int getWidth() {
        return size != null ? size.width : 0;
    }

    @Override
    public int getHeight() {
        return size != null ? size.height : 0;
    }

    @Override
    public int getTileWidth() {
        return tileWidth;
    }

    @Override
    public int getTileHeight() {
        return tileHeight;
    }

    @Override
    public GeoCoding getGeoCoding() {
        return geoCoding;
    }
}
