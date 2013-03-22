/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.timeseries.core;

import static org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesFactory;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.net.URI;
import java.util.Iterator;

/**
 * Activator class for extend the default writer behavior and listening to visat´s ProductManager.
 * <p/>
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 *
 * @author Sabine Embacher
 */
public class TimeSeriesToolActivator implements Activator {

    private final DimapProductWriter.Listener dimapWriterListener;
    private DimapProductReader.ReaderExtender readerExtender;

    public TimeSeriesToolActivator() {
        dimapWriterListener = createDimapWriterListener();
        readerExtender = createReaderExtender();
    }

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();

        final Iterator<ProductWriterPlugIn> allWriterPlugIns = ioPlugInManager.getWriterPlugIns(DimapProductConstants.DIMAP_FORMAT_NAME);
        while (allWriterPlugIns.hasNext()) {
            DimapProductWriterPlugIn writerPlugIn = (DimapProductWriterPlugIn) allWriterPlugIns.next();
            writerPlugIn.addDimapWriterListener(dimapWriterListener);
        }

        final Iterator<ProductReaderPlugIn> readerPlugIns = ioPlugInManager.getReaderPlugIns(DimapProductConstants.DIMAP_FORMAT_NAME);
        while (readerPlugIns.hasNext()) {
            DimapProductReaderPlugIn readerPlugin = (DimapProductReaderPlugIn) readerPlugIns.next();
            readerPlugin.addReaderExtender(readerExtender);
        }
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
        final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();

        final Iterator<ProductWriterPlugIn> allWriterPlugIns = ioPlugInManager.getWriterPlugIns(DimapProductConstants.DIMAP_FORMAT_NAME);
        while (allWriterPlugIns.hasNext()) {
            DimapProductWriterPlugIn writerPlugIn = (DimapProductWriterPlugIn) allWriterPlugIns.next();
            writerPlugIn.removeDimapWriterListener(dimapWriterListener);
        }

        final Iterator<ProductReaderPlugIn> readerPlugIns = ioPlugInManager.getReaderPlugIns(DimapProductConstants.DIMAP_FORMAT_NAME);
        while (readerPlugIns.hasNext()) {
            DimapProductReaderPlugIn writerPlugIn = (DimapProductReaderPlugIn) readerPlugIns.next();
            writerPlugIn.removeReaderExtender(readerExtender);
        }
    }

    private DimapProductWriter.Listener createDimapWriterListener() {
        return new DimapProductWriter.Listener() {
            @Override
            public boolean vetoableShouldWrite(ProductNode node) {
                if (!isTimeSerisProduct(node.getProduct())) {
                    return true;
                } else if (node instanceof RasterDataNode) {
                    return false;
                }
                return true;
            }

            @Override
            public void intendToWriteDimapHeaderTo(File outputDir, Product product) {
                if (isTimeSerisProduct(product)) {
                    convertAbsolutPathsToRelative(product, outputDir);
                }
            }

            private boolean isTimeSerisProduct(Product product) {
                return product.getProductType().equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE);
            }
        };
    }

    public static void convertAbsolutPathsToRelative(Product product, File outputDir) {
        final MetadataElement tsRootElem = product.getMetadataRoot().getElement(AbstractTimeSeries.TIME_SERIES_ROOT_NAME);

        final MetadataElement productLocations = tsRootElem.getElement(AbstractTimeSeries.PRODUCT_LOCATIONS);
        final MetadataElement sourceProductPaths = tsRootElem.getElement(AbstractTimeSeries.SOURCE_PRODUCT_PATHS);

        replaceWithRelativePaths(productLocations.getElements(), outputDir);
        replaceWithRelativePaths(sourceProductPaths.getElements(), outputDir);
    }

    private static void replaceWithRelativePaths(MetadataElement[] elements, File outputDir) {
        for (MetadataElement element : elements) {
            final String pathName = element.getAttributeString(AbstractTimeSeries.PL_PATH);
            final URI relativeUri = FileUtils.getRelativeUri(outputDir.toURI(), new File(pathName));

            final MetadataAttribute pathAttr = element.getAttribute(AbstractTimeSeries.PL_PATH);
            final MetadataAttribute typeAttr = element.getAttribute(AbstractTimeSeries.PL_TYPE);

            element.removeAttribute(pathAttr);
            element.removeAttribute(typeAttr);

            pathAttr.dispose();
            final MetadataAttribute newPathAttr = new MetadataAttribute(AbstractTimeSeries.PL_PATH, ProductData.createInstance(relativeUri.toString()), true);

            element.addAttribute(newPathAttr);
            element.addAttribute(typeAttr);
        }
    }

    private DimapProductReader.ReaderExtender createReaderExtender() {
        return new DimapProductReader.ReaderExtender() {
            @Override
            public void completeProductNodesReading(Product product) {
                if (product.getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
                    TimeSeriesFactory.create(product);
                }
            }
        };
    }
}
