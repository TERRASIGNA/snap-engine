package org.esa.beam.framework.datamodel;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Dimension;
import java.awt.image.RenderedImage;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class ProductMultiGeometryTest {

    private static final byte IMAGE_CONTENT = 1;

    @Test
    public void testMethod() throws FactoryException, TransformException {
        Product product = new Product("Prod", "P");

        Band europeLR = new Band("Europe_low_res", ProductData.TYPE_INT8, 250, 550);
        europeLR.setSourceImage(ConstantDescriptor.create(250f, 550f, new Byte[]{IMAGE_CONTENT}, null));
        // NW: -10, 65 ; SE: 15, 40
        europeLR.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 250, 550, -10, 65, 0.1, 0.1));
        product.addBand(europeLR);
        Band europeHR = new Band("Europe_high_res", ProductData.TYPE_INT8, 2500, 5500);
        europeHR.setSourceImage(ConstantDescriptor.create(2500f, 5500f, new Byte[]{IMAGE_CONTENT}, null));
        // NW: -10, 65 ; SE: 15, 40
        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, 2500, 5500, -10, 65, 0.01, 0.01);
        europeHR.setGeoCoding(geoCoding);
        product.addBand(europeHR);
        Band germany = new Band("Germany", ProductData.TYPE_INT8, 2000, 2000);
        germany.setSourceImage(ConstantDescriptor.create(2000f, 2000f, new Byte[]{IMAGE_CONTENT}, null));
        // NW: 5, 55 ; SE: 15, 45
        germany.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 2000, 2000, 5, 55, 0.005, 0.005));
        product.addBand(germany);

        int sceneWidth = (int) ((0.01 / 0.005) * 2500);
        int sceneHeight = (int) ((0.01 / 0.005) * 5500);
        Dimension sceneDimension = new Dimension(sceneWidth, sceneHeight);
        assertEquals(sceneDimension, product.getSceneRasterSize());
        assertEquals(sceneDimension, product.getSceneImageGeometry().getSize());

        CrsGeoCoding expectedSceneGeoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, sceneWidth, sceneHeight, -10.05, 65.05, 0.005, 0.005, 0, 0);
        GeoCoding actualSceneGeoCoding = product.getSceneGeoCoding();

        assertEquals(expectedSceneGeoCoding.getMapCRS().toWKT(), actualSceneGeoCoding.getMapCRS().toWKT());
        assertEquals(expectedSceneGeoCoding.getImageToMapTransform(), actualSceneGeoCoding.getImageToMapTransform());

        ImageGeometry sceneImageGeometry = product.getSceneImageGeometry();


        ImageGeometryTransform igt = germany.getImageGeometryTransform();
        RenderedImage germanyGeoSceneImage = igt.transform(germany.getGeophysicalImage(), germany.getGeoCoding(), sceneImageGeometry);
    }
}