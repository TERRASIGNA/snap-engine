package org.esa.beam.framework.datamodel;

import java.awt.Dimension;

class SceneImageGeometry implements ImageGeometry{

    private Dimension size;
    private Dimension tileSize;
    private GeoCoding geoCoding;

    public SceneImageGeometry(Dimension sceneRasterSize) {
        this.size = sceneRasterSize;
    }

    @Override
    public Dimension getSize() {
        return size == null ? null : (Dimension) size.clone();
    }

    public void setSize(Dimension size) {
        this.size = size == null ? null : (Dimension) size.clone();
    }

    @Override
    public int getWidth() {
        return size != null ? size.width : 0;
    }

    @Override
    public int getHeight() {
        return size != null ? size.height : 0;
    }

    public Dimension getTileSize() {
        return tileSize == null ? null : (Dimension) tileSize.clone();
    }

    public void setTileSize(Dimension tileSize) {
        this.tileSize = tileSize == null ? null : (Dimension) tileSize.clone();
    }

    @Override
    public int getTileWidth() {
        return tileSize != null ? tileSize.width : 0;
    }

    @Override
    public int getTileHeight() {
        return tileSize != null ? tileSize.height : 0;
    }

    @Override
    public GeoCoding getGeoCoding() {
        return geoCoding;
    }

    public void setGeoCoding(GeoCoding geoCoding) {
        this.geoCoding = geoCoding;
    }

    public void dispose() {
        if(geoCoding != null ) {
            geoCoding.dispose();
        }
        geoCoding = null;
        tileSize = null;
        size = null;
    }
}
