package org.esa.beam.framework.datamodel;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;

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
        europeLR.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 250, 550, -10, 65, 0.1, 0.1));
        product.addBand(europeLR);
        Band europeHR = new Band("Europe_high_res", ProductData.TYPE_INT8, 2500, 5500);
        europeHR.setSourceImage(ConstantDescriptor.create(2500f, 5500f, new Byte[]{IMAGE_CONTENT}, null));
        europeHR.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 2500, 5500, -10, 65, 0.01, 0.01));
        product.addBand(europeHR);
        Band germany = new Band("Germany", ProductData.TYPE_INT8, 2000, 2000);
        germany.setSourceImage(ConstantDescriptor.create(2500f, 5500f, new Byte[]{IMAGE_CONTENT}, null));
        germany.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 2500, 5500, 5, 55, 0.2, 0.2));
        product.addBand(germany);

    }
}