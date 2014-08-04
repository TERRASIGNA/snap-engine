package org.esa.beam.framework.datamodel;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Dimension;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class ProductSceneGeometryTest {

    @Test
    public void testMethod() throws FactoryException, TransformException {
        Product product = new Product("Test", "T");


        Band b1 = new Band("b1", ProductData.TYPE_INT16, 50, 100);
        b1.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 50, 100, -5, 55, 0.2, 0.1));
        b1.setTileSize(new Dimension(50, 5));
        product.addBand(b1);

        assertEquals(new Dimension(50, 100), product.getSceneRasterSize());
        assertEquals(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 50, 100, -5, 55, 0.2, 0.1), product.getSceneGeoCoding());
        assertEquals(new Dimension(50, 5), product.getSceneTileSize());

        ImageGeometry ig = product.getSceneImageGeometry();
        assertEquals(new Dimension(50, 100), new Dimension(ig.getWidth(), ig.getHeight()));
        assertEquals(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 50, 100, -5, 55, 0.2, 0.1), ig.getGeoCoding());
        assertEquals(new Dimension(50, 5), new Dimension(ig.getTileWidth(), ig.getTileHeight()));


        Band b2 = new Band("b2", ProductData.TYPE_INT16, 100, 200);
        b2.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 200, -5, 55, 0.1, 0.05));
        b2.setTileSize(new Dimension(10, 20));
        product.addBand(b2);

        assertEquals(new Dimension(100, 200), product.getSceneRasterSize());
        assertEquals(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 200, -5, 55, 0.1, 0.05), product.getSceneGeoCoding());
        assertEquals(new Dimension(100, 200), product.getSceneTileSize()); // What do we expect here?

        ig = product.getSceneImageGeometry();
        assertEquals(new Dimension(100, 200), new Dimension(ig.getWidth(), ig.getHeight()));
        assertEquals(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 100, 200, -5, 55, 0.1, 0.05), ig.getGeoCoding());
        assertEquals(new Dimension(100, 200), new Dimension(ig.getTileWidth(), ig.getTileHeight()));


    }
}