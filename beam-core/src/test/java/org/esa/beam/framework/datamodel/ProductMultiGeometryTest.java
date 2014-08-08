package org.esa.beam.framework.datamodel;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class ProductMultiGeometryTest {

    @Test
    public void testMethod() throws FactoryException, TransformException {
        Product product = new Product("Prod", "P");

        Band band = new Band("Europe_low_res", ProductData.TYPE_INT16, 250, 550);
        band.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 50, 100, -10, 65, 0.1, 0.1));
        product.addBand(band);

    }
}