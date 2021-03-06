/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.gpf.operators.standard.reproject;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.ColorPaletteDef;
import org.esa.snap.framework.datamodel.CrsGeoCoding;
import org.esa.snap.framework.datamodel.FlagCoding;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.ImageGeometry;
import org.esa.snap.framework.datamodel.ImageInfo;
import org.esa.snap.framework.datamodel.IndexCoding;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.ProductNodeGroup;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.datamodel.SceneRasterTransform;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.dataop.dem.ElevationModel;
import org.esa.snap.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.framework.dataop.dem.ElevationModelRegistry;
import org.esa.snap.framework.dataop.dem.Orthorectifier;
import org.esa.snap.framework.dataop.dem.Orthorectifier2;
import org.esa.snap.framework.dataop.resamp.Resampling;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.jai.ImageManager;
import org.esa.snap.jai.ResolutionLevel;
import org.esa.snap.util.Debug;
import org.esa.snap.util.ProductUtils;
import org.esa.snap.util.io.FileUtils;
import org.geotools.factory.Hints;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * The reprojection operator is used to geo-reference data products.
 * Beside plain reprojection it is able to use a Digital Elevation Model (DEM) to orthorectify a data product and
 * to collocate one product with another.
 * <p>
 * The following XML sample shows how to integrate the <code>Reproject</code> operator in a processing graph (an
 * Lambert_Azimuthal_Equal_Area projection using the WGS-84 datum):
 * <pre>
 *    &lt;node id="reprojectNode"&gt;
 *        &lt;operator&gt;Reproject&lt;/operator&gt;
 *        &lt;sources&gt;
 *            &lt;sourceProducts&gt;readNode&lt;/sourceProducts&gt;
 *        &lt;/sources&gt;
 *        &lt;parameters&gt;
 *            &lt;wktFile/&gt;
 *            &lt;crs&gt;
 *              PROJCS["Lambert_Azimuthal_Equal_Area / World Geodetic System 1984",
 *                GEOGCS["World Geodetic System 1984",
 *                   DATUM["World Geodetic System 1984",
 *                      SPHEROID["WGS 84", 6378137.0, 298.257223563, AUTHORITY["EPSG","7030"]],
 *                   AUTHORITY["EPSG","6326"]],
 *                   PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]],
 *                   UNIT["degree", 0.017453292519943295],
 *                   AXIS["Geodetic longitude", EAST],
 *                   AXIS["Geodetic latitude", NORTH]],
 *                PROJECTION["Lambert_Azimuthal_Equal_Area"],
 *                PARAMETER["latitude_of_center", 0.0],
 *                PARAMETER["longitude_of_center", 0.0],
 *                PARAMETER["false_easting", 0.0],
 *                PARAMETER["false_northing", 0.0],
 *                UNIT["m", 1.0],
 *                AXIS["Easting", EAST],
 *                AXIS["Northing", NORTH]]
 *            &lt;/crs&gt;
 *            &lt;resampling&gt;Nearest&lt;/resampling&gt;
 *            &lt;referencePixelX&gt;0.5&lt;/referencePixelX&gt;
 *            &lt;referencePixelY&gt;0.5&lt;/referencePixelY&gt;
 *            &lt;easting&gt;9.5&lt;/easting&gt;
 *            &lt;northing&gt;56.84&lt;/northing&gt;
 *            &lt;orientation&gt;0.0&lt;/orientation&gt;
 *            &lt;pixelSizeX&gt;0.012&lt;/pixelSizeX&gt;
 *            &lt;pixelSizeY&gt;0.012&lt;/pixelSizeY&gt;
 *            &lt;width&gt;135010246&lt;/width&gt;
 *            &lt;height&gt;116629771&lt;/height&gt;
 *            &lt;orthorectify&gt;false&lt;/orthorectify&gt;
 *            &lt;elevationModelName/&gt;
 *            &lt;noDataValue&gt;NaN&lt;/noDataValue&gt;
 *            &lt;includeTiePointGrids&gt;true&lt;/includeTiePointGrids&gt;
 *            &lt;addDeltaBands&gt;false&lt;/addDeltaBands&gt;
 *        &lt;/parameters&gt;
 *    &lt;/node&gt;
 * </pre>
 *
 * @author Marco Zuehlke
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
@OperatorMetadata(alias = "Reproject",
                  category = "Geometric Operations",
                  version = "1.0",
                  authors = "Marco Zühlke, Marco Peters, Ralf Quast, Norman Fomferra",
                  copyright = "(c) 2009 by Brockmann Consult",
                  description = "Reprojection of a source product to a target Coordinate Reference System.",
                  internal = false)
@SuppressWarnings({"UnusedDeclaration"})
public class ReprojectionOp extends Operator {


    @SourceProduct(alias = "source", description = "The product which will be reprojected.")
    private Product sourceProduct;
    @SourceProduct(alias = "collocateWith", optional = true, label = "Collocation product",
                   description = "The source product will be collocated with this product.")
    private Product collocationProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "A file which contains the target Coordinate Reference System in WKT format.")
    private File wktFile;

    @Parameter(description = "A text specifying the target Coordinate Reference System, either in WKT or as an " +
                             "authority code. For appropriate EPSG authority codes see (www.epsg-registry.org). " +
                             "AUTO authority can be used with code 42001 (UTM), and 42002 (Transverse Mercator) " +
                             "where the scene center is used as reference. Examples: EPSG:4326, AUTO:42001")
    private String crs;

    @Parameter(alias = "resampling",
               label = "Resampling Method",
               description = "The method used for resampling of floating-point raster data.",
               valueSet = {"Nearest", "Bilinear", "Bicubic"},
               defaultValue = "Nearest")
    private String resamplingName;

    @Parameter(description = "The X-position of the reference pixel.")
    private Double referencePixelX;
    @Parameter(description = "The Y-position of the reference pixel.")
    private Double referencePixelY;
    @Parameter(description = "The easting of the reference pixel.")
    private Double easting;
    @Parameter(description = "The northing of the reference pixel.")
    private Double northing;
    @Parameter(description = "The orientation of the output product (in degree).",
               defaultValue = "0", interval = "[-360,360]")
    private Double orientation;
    @Parameter(description = "The pixel size in X direction given in CRS units.")
    private Double pixelSizeX;
    @Parameter(description = "The pixel size in Y direction given in CRS units.")
    private Double pixelSizeY;
    @Parameter(description = "The width of the target product.")
    private Integer width;
    @Parameter(description = "The height of the target product.")
    private Integer height;
    @Parameter(description = "The tile size in X direction.")
    private Integer tileSizeX;
    @Parameter(description = "The tile size in Y direction.")
    private Integer tileSizeY;

    @Parameter(description = "Whether the source product should be orthorectified. (Not applicable to all products)",
               defaultValue = "false")
    private boolean orthorectify;

    @Parameter(description = "The name of the elevation model for the orthorectification. " +
                             "If not given tie-point data is used.")
    private String elevationModelName;

    @Parameter(description = "The value used to indicate no-data.")
    private Double noDataValue;

    @Parameter(description = "Whether tie-point grids should be included in the output product.",
               defaultValue = "true")
    private boolean includeTiePointGrids;

    @Parameter(description = "Whether to add delta longitude and latitude bands.",
               defaultValue = "false")
    private boolean addDeltaBands;

    private ElevationModel elevationModel;
    private Map<String, MultiLevelModel> sourceModelMap;
    private MultiLevelModel defaultSourceModel;
    private Map<String, ImageGeometry> imageGeometryMap;
    private ImageGeometry defaultImageGeometry;

    @Override
    public void initialize() throws OperatorException {
        validateCrsParameters();
        validateResamplingParameter();
        validateReferencingParameters();
        validateTargetGridParameters();

        /*
        * 1. Compute the target CRS
        */
        CoordinateReferenceSystem targetCrs = createTargetCRS();
        /*
        * 2. Compute the target geometry
        */
        ImageGeometry targetImageGeometry = createImageGeometry(targetCrs);
        initMaps(targetCrs);
        defaultSourceModel = ImageManager.getMultiLevelModel(sourceProduct.getBandAt(0));
        defaultImageGeometry = targetImageGeometry;
        /*
        * 3. Create the target product
        */
        Rectangle targetRect = targetImageGeometry.getImageRect();
        targetProduct = new Product("projected_" + sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    targetRect.width,
                                    targetRect.height);
        targetProduct.setDescription(sourceProduct.getDescription());
        Dimension tileSize;
        if (tileSizeX != null && tileSizeY != null) {
            tileSize = new Dimension(tileSizeX, tileSizeY);
        } else {
            tileSize = ImageManager.getPreferredTileSize(targetProduct);
            Dimension sourceProductPreferredTileSize = sourceProduct.getPreferredTileSize();
            if (sourceProductPreferredTileSize != null) {
                if (sourceProductPreferredTileSize.width == sourceProduct.getSceneRasterWidth()) {
                    tileSize.width = targetProduct.getSceneRasterWidth();
                    tileSize.height = Math.min(sourceProductPreferredTileSize.height,
                                               targetProduct.getSceneRasterHeight());
                }
            }
        }
        targetProduct.setPreferredTileSize(tileSize);
        /*
        * 4. Define some target properties
        */
        if (orthorectify) {
            elevationModel = createElevationModel();
        }
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        copyIndexCoding();
        try {
            targetProduct.setGeoCoding(new CrsGeoCoding(targetImageGeometry.getMapCrs(),
                                                        targetRect,
                                                        targetImageGeometry.getImage2MapTransform()));
        } catch (Exception e) {
            throw new OperatorException(e);
        }

        ProductData.UTC meanTime = getSourceMeanTime();
        targetProduct.setStartTime(meanTime);
        targetProduct.setEndTime(meanTime);

        reprojectRasterDataNodes(sourceProduct.getBands());
        if (includeTiePointGrids) {
            reprojectRasterDataNodes(sourceProduct.getTiePointGrids());
        }
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyOverlayMasks(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());

        if (addDeltaBands) {
            addDeltaBands();
        }
    }

    @Override
    public void dispose() {
        if (elevationModel != null) {
            elevationModel.dispose();
        }
        super.dispose();
    }

    private void initMaps(CoordinateReferenceSystem targetCrs) {
        imageGeometryMap = new HashMap<>();
        sourceModelMap = new HashMap<>();

        final ProductNodeGroup<Band> sourceBands = sourceProduct.getBandGroup();
        for (int i = 0; i < sourceBands.getNodeCount(); i++) {
            addRasterDataNodeToMaps(sourceBands.get(i), targetCrs);
        }
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = sourceProduct.getTiePointGridGroup();
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            addRasterDataNodeToMaps(tiePointGridGroup.get(i), targetCrs);
        }
    }

    private void addRasterDataNodeToMaps(RasterDataNode rasterDataNode, CoordinateReferenceSystem targetCrs) {
        if (rasterDataNode.getSceneRasterTransform() == SceneRasterTransform.IDENTITY) {
            return;
        }
        final String rasterDataNodeName = rasterDataNode.getName();
        if (!imageGeometryMap.containsKey(rasterDataNodeName)) {
            imageGeometryMap.put(rasterDataNodeName, ImageGeometry.createTargetGeometry(rasterDataNode, targetCrs,
                                                                                      pixelSizeX, pixelSizeY,
                                                                                      width, height,
                                                                                      orientation, easting,
                                                                                      northing, referencePixelX,
                                                                                      referencePixelY));
        }
        if (!sourceModelMap.containsKey(rasterDataNodeName)) {
            sourceModelMap.put(rasterDataNodeName, ImageManager.getMultiLevelModel(rasterDataNode));
        }
    }

    private ElevationModel createElevationModel() throws OperatorException {
        if (elevationModelName != null) {
            final ElevationModelDescriptor demDescriptor = ElevationModelRegistry.getInstance().getDescriptor(
                    elevationModelName);
            if (!demDescriptor.isDemInstalled()) {
                throw new OperatorException("DEM not installed: " + elevationModelName);
            }
            return demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        }
        return null; // force use of elevation from tie-points
    }

    private GeoCoding getSourceGeoCoding(final RasterDataNode sourceBand) {
        if (orthorectify && sourceBand.canBeOrthorectified()) {
            return createOrthorectifier(sourceBand);
        } else {
            return sourceBand.getGeoCoding();
        }
    }

    private Orthorectifier createOrthorectifier(final RasterDataNode sourceBand) {
        return new Orthorectifier2(sourceBand.getSceneRasterWidth(),
                                   sourceBand.getSceneRasterHeight(),
                                   sourceBand.getPointing(),
                                   elevationModel, 25);
    }


    private void reprojectRasterDataNodes(RasterDataNode[] rasterDataNodes) {
        for (RasterDataNode raster : rasterDataNodes) {
            reprojectSourceRaster(raster);
        }
    }

    private void reprojectSourceRaster(RasterDataNode sourceRaster) {
        final SceneRasterTransform sceneRasterTransform = sourceRaster.getSceneRasterTransform();
        ImageGeometry imageGeometry;
        if (imageGeometryMap.containsKey(sourceRaster.getName())) {
            imageGeometry = imageGeometryMap.get(sourceRaster.getName());
        } else {
            imageGeometry = defaultImageGeometry;
        }
        MultiLevelModel sourceModel;
        if (sourceModelMap.containsKey(sourceRaster.getName())) {
            sourceModel = sourceModelMap.get(sourceRaster.getName());
        } else {
            sourceModel = defaultSourceModel;
        }
        final int targetDataType;
        MultiLevelImage sourceImage;
        if (sourceRaster.isScalingApplied()) {
            targetDataType = sourceRaster.getGeophysicalDataType();
            sourceImage = sourceRaster.getGeophysicalImage();
        } else {
            targetDataType = sourceRaster.getDataType();
            sourceImage = sourceRaster.getSourceImage();
        }
        final Number targetNoDataValue = getTargetNoDataValue(sourceRaster, targetDataType);
        final Rectangle imageRect = imageGeometry.getImageRect();
        final Band targetBand = new Band(sourceRaster.getName(), targetDataType, (int) imageRect.getWidth(), (int) imageRect.getHeight());
        targetProduct.addBand(targetBand);
        targetBand.setLog10Scaled(sourceRaster.isLog10Scaled());
        targetBand.setNoDataValue(targetNoDataValue.doubleValue());
        targetBand.setNoDataValueUsed(targetBand.getSceneRasterWidth() == targetProduct.getSceneRasterWidth() &&
                targetBand.getSceneRasterHeight() == targetProduct.getSceneRasterHeight());
        targetBand.setDescription(sourceRaster.getDescription());
        targetBand.setUnit(sourceRaster.getUnit());

        final GeoCoding sourceGeoCoding = getSourceGeoCoding(sourceRaster);
        final String exp = sourceRaster.getValidMaskExpression();
        if (exp != null) {
            sourceImage = createNoDataReplacedImage(sourceRaster, targetNoDataValue);
        }

        final Interpolation resampling = getResampling(targetBand);
        MultiLevelImage projectedImage = createProjectedImage(sourceGeoCoding, sourceImage, sourceModel, targetBand, resampling);
        if (mustReplaceNaN(sourceRaster, targetDataType, targetNoDataValue.doubleValue())) {
            final MultiLevelModel targetModel = ImageManager.createMultiLevelModel(targetBand);
            projectedImage = createNaNReplacedImage(projectedImage, targetModel, targetNoDataValue.doubleValue());
        }
        if (targetBand.isLog10Scaled()) {
            projectedImage = createLog10ScaledImage(projectedImage);
        }
        targetBand.setSourceImage(projectedImage);

        /*
        * Flag and index codings
        */
        if (sourceRaster instanceof Band) {
            final Band sourceBand = (Band) sourceRaster;
            ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            final FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
            final IndexCoding sourceIndexCoding = sourceBand.getIndexCoding();
            if (sourceFlagCoding != null) {
                final String flagCodingName = sourceFlagCoding.getName();
                final FlagCoding destFlagCoding = targetProduct.getFlagCodingGroup().get(flagCodingName);
                targetBand.setSampleCoding(destFlagCoding);
            } else if (sourceIndexCoding != null) {
                final String indexCodingName = sourceIndexCoding.getName();
                final IndexCoding destIndexCoding = targetProduct.getIndexCodingGroup().get(indexCodingName);
                targetBand.setSampleCoding(destIndexCoding);
            }
        }
    }

    private MultiLevelImage createLog10ScaledImage(final MultiLevelImage projectedImage) {
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(projectedImage.getModel()) {
            @Override
            public RenderedImage createImage(int level) {
                return new Log10OpImage(projectedImage.getImage(level));
            }
        });
    }

    private boolean mustReplaceNaN(RasterDataNode sourceRaster, int targetDataType, double targetNoDataValue) {
        final boolean isFloat = ProductData.isFloatingPointType(targetDataType);
        final boolean isNoDataGiven = sourceRaster.isNoDataValueUsed() || noDataValue != null;
        final boolean isNoDataNaN = Double.isNaN(targetNoDataValue);
        return isFloat && isNoDataGiven && !isNoDataNaN;
    }

    private Number getTargetNoDataValue(RasterDataNode sourceRaster, int targetDataType) {
        double targetNoDataValue = Double.NaN;
        if (noDataValue != null) {
            targetNoDataValue = noDataValue;
        } else if (sourceRaster.isNoDataValueUsed()) {
            targetNoDataValue = sourceRaster.getNoDataValue();
        }
        Number targetNoDataNumber;
        if (targetDataType == ProductData.TYPE_INT8) {
            targetNoDataNumber = (byte) targetNoDataValue;
        } else if (targetDataType == ProductData.TYPE_INT16 ||
                   targetDataType == ProductData.TYPE_UINT8) {
            targetNoDataNumber = (short) targetNoDataValue;
        } else if (targetDataType == ProductData.TYPE_INT32 ||
                   targetDataType == ProductData.TYPE_UINT16) {
            targetNoDataNumber = (int) targetNoDataValue;
        } else if (targetDataType == ProductData.TYPE_FLOAT32) {
            targetNoDataNumber = (float) targetNoDataValue;
        } else {
            targetNoDataNumber = targetNoDataValue;
        }
        return targetNoDataNumber;
    }

    private MultiLevelImage createNaNReplacedImage(final MultiLevelImage projectedImage, MultiLevelModel targetModel, final double value) {

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(targetModel) {

            @Override
            public RenderedImage createImage(int targetLevel) {
                return new ReplaceNaNOpImage(projectedImage.getImage(targetLevel), value);
            }
        });
    }

    private MultiLevelImage createNoDataReplacedImage(final RasterDataNode rasterDataNode, final Number noData) {
        return ImageManager.createMaskedGeophysicalImage(rasterDataNode, noData);
    }

    private MultiLevelImage createProjectedImage(final GeoCoding sourceGeoCoding, final MultiLevelImage sourceImage,
                                                 MultiLevelModel sourceModel, final Band targetBand, final Interpolation resampling) {

        final CoordinateReferenceSystem sourceModelCrs = ImageManager.getModelCrs(sourceGeoCoding);
        final CoordinateReferenceSystem targetModelCrs = ImageManager.getModelCrs(targetProduct.getGeoCoding());
        final AffineTransform i2mSourceProduct = ImageManager.getImageToModelTransform(sourceGeoCoding);
        final AffineTransform i2mTargetProduct = ImageManager.getImageToModelTransform(targetProduct.getGeoCoding());
        MultiLevelModel targetModel = ImageManager.createMultiLevelModel(targetBand);

        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(targetModel) {

            @Override
            public RenderedImage createImage(int targetLevel) {
                final double targetScale = targetModel.getScale(targetLevel);
                final int sourceLevel = sourceImage.getModel().getLevel(targetScale);
                RenderedImage leveledSourceImage = sourceImage.getImage(sourceLevel);

                final Rectangle sourceBounds = new Rectangle(leveledSourceImage.getWidth(),
                                                             leveledSourceImage.getHeight());

                // the following transformation maps the source level image to level zero and then to the model,
                // which either is a map or an image CRS
                final AffineTransform i2mSource = sourceModel.getImageToModelTransform(sourceLevel);
                i2mSource.concatenate(sourceModel.getModelToImageTransform(0));
                i2mSource.concatenate(i2mSourceProduct);

                ImageGeometry sourceGeometry = new ImageGeometry(sourceBounds,
                                                                 sourceModelCrs,
                                                                 i2mSource);

                ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(
                        ImageManager.getDataBufferType(targetBand.getDataType()),
                        targetProduct.getSceneRasterWidth(),
                        targetProduct.getSceneRasterHeight(),
                        targetProduct.getPreferredTileSize(),
                        ResolutionLevel.create(getModel(), targetLevel));
                Rectangle targetBounds = new Rectangle(imageLayout.getWidth(null), imageLayout.getHeight(null));

                // the following transformation maps the target level image to level zero and then to the model,
                // which always is a map
                final AffineTransform i2mTarget = getModel().getImageToModelTransform(targetLevel);
                i2mTarget.concatenate(getModel().getModelToImageTransform(0));
                i2mTarget.concatenate(i2mTargetProduct);

                ImageGeometry targetGeometry = new ImageGeometry(targetBounds,
                                                                 targetModelCrs,
                                                                 i2mTarget);
                Hints hints = new Hints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                hints.put(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

                Dimension tileSize = ImageManager.getPreferredTileSize(targetProduct);
                try {
                    Reproject reprojection = new Reproject(targetModel.getLevelCount());
                    return reprojection.reproject(leveledSourceImage, sourceGeometry, targetGeometry,
                                                  targetBand.getNoDataValue(), resampling, hints, targetLevel,
                                                  tileSize);
                } catch (FactoryException | TransformException e) {
                    Debug.trace(e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private int getSourceLevel(MultiLevelModel sourceModel, int targetLevel) {
        int maxSourceLevel = sourceModel.getLevelCount() - 1;
        return maxSourceLevel < targetLevel ? maxSourceLevel : targetLevel;
    }

    private ProductData.UTC getSourceMeanTime() {
        ProductData.UTC startTime = sourceProduct.getStartTime();
        ProductData.UTC endTime = sourceProduct.getEndTime();
        ProductData.UTC meanTime;
        if (startTime != null && endTime != null) {
            meanTime = new ProductData.UTC(0.5 * (startTime.getMJD() + endTime.getMJD()));
        } else if (startTime != null) {
            meanTime = startTime;
        } else if (endTime != null) {
            meanTime = endTime;
        } else {
            meanTime = null;
        }
        return meanTime;
    }

    private void copyIndexCoding() {
        final ProductNodeGroup<IndexCoding> indexCodingGroup = sourceProduct.getIndexCodingGroup();
        for (int i = 0; i < indexCodingGroup.getNodeCount(); i++) {
            IndexCoding sourceIndexCoding = indexCodingGroup.get(i);
            ProductUtils.copyIndexCoding(sourceIndexCoding, targetProduct);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ReprojectionOp.class);
        }
    }

    private CoordinateReferenceSystem createTargetCRS() throws OperatorException {
        try {
            if (wktFile != null) {
                return CRS.parseWKT(FileUtils.readText(wktFile));
            }
            if (crs != null) {
                try {
                    return CRS.parseWKT(crs);
                } catch (FactoryException ignored) {
                    // prefix with EPSG, if there are only numbers
                    if (crs.matches("[0-9]*")) {
                        crs = "EPSG:" + crs;
                    }
                    // append center coordinates for AUTO code
                    if (crs.matches("AUTO:[0-9]*")) {
                        final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                        crs = String.format("%s,%s,%s", crs, centerGeoPos.lon, centerGeoPos.lat);
                    }
                    // force longitude==x-axis and latitude==y-axis
                    return CRS.decode(crs, true);
                }
            }
            if (collocationProduct != null && collocationProduct.getGeoCoding() != null) {
                return collocationProduct.getGeoCoding().getMapCRS();
            }
        } catch (FactoryException | IOException e) {
            throw new OperatorException(String.format("Target CRS could not be created: %s", e.getMessage()), e);
        }

        throw new OperatorException("Target CRS could not be created.");
    }

    protected void validateCrsParameters() {
        final String msgPattern = "Invalid target CRS specification.\nSpecify {0} one of the " +
                                  "''wktFile'', ''crs'' or ''collocationProduct'' parameters.";

        if (wktFile == null && crs == null && collocationProduct == null) {
            throw new OperatorException(MessageFormat.format(msgPattern, "at least"));
        }

        boolean crsDefined = false;
        final String exceptionMsg = MessageFormat.format(msgPattern, "only");
        if (wktFile != null) {
            crsDefined = true;
        }
        if (crs != null) {
            if (crsDefined) {
                throw new OperatorException(exceptionMsg);
            }
            crsDefined = true;
        }
        if (collocationProduct != null) {
            if (crsDefined) {
                throw new OperatorException(exceptionMsg);
            }
        }
    }

    private Interpolation getResampling(Band band) {
        int resampleType = getResampleType();
        if (!ProductData.isFloatingPointType(band.getDataType())) {
            resampleType = Interpolation.INTERP_NEAREST;
        }
        return Interpolation.getInstance(resampleType);
    }

    private int getResampleType() {
        final int resamplingType;
        if ("Nearest".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_NEAREST;
        } else if ("Bilinear".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_BILINEAR;
        } else if ("Bicubic".equalsIgnoreCase(resamplingName)) {
            resamplingType = Interpolation.INTERP_BICUBIC;
        } else {
            resamplingType = -1;
        }
        return resamplingType;
    }

    void validateResamplingParameter() {
        if (getResampleType() == -1) {
            throw new OperatorException("Invalid resampling method: " + resamplingName);
        }
    }

    void validateReferencingParameters() {
        if (!((referencePixelX == null && referencePixelY == null && easting == null && northing == null)
              || (referencePixelX != null && referencePixelY != null && easting != null && northing != null))) {
            throw new OperatorException("Invalid referencing parameters: \n" +
                                        "'referencePixelX', 'referencePixelY', 'easting' and 'northing' have to be specified either all or not at all.");
        }
    }

    void validateTargetGridParameters() {
        if ((pixelSizeX != null && pixelSizeY == null) ||
            (pixelSizeX == null && pixelSizeY != null)) {
            throw new OperatorException("'pixelSizeX' and 'pixelSizeY' must be specified both or not at all.");
        }
    }

    private ImageGeometry createImageGeometry(CoordinateReferenceSystem targetCrs) {
        ImageGeometry imageGeometry;
        if (collocationProduct != null) {
            //todo adapt this to multi resolution products
            imageGeometry = ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct);
        } else {
            imageGeometry = ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                                                               pixelSizeX, pixelSizeY,
                                                               width, height, orientation,
                                                               easting, northing,
                                                               referencePixelX, referencePixelY);
            final AxisDirection targetAxisDirection = targetCrs.getCoordinateSystem().getAxis(1).getDirection();
            // When collocating the Y-Axis is DISPLAY_DOWN, then pixelSizeY must not negated
            if (!AxisDirection.DISPLAY_DOWN.equals(targetAxisDirection)) {
                imageGeometry.changeYAxisDirection();
            }
        }
        return imageGeometry;
    }

    private void addDeltaBands() {

        final Band deltaLonBand = targetProduct.addBand("delta_lon_angular", "longitude - LON");
        deltaLonBand.setUnit("deg");
        deltaLonBand.setDescription("Delta between old longitude and new longitude in degree");
        deltaLonBand.setNoDataValueUsed(true);
        deltaLonBand.setNoDataValue(noDataValue == null ? Double.NaN : noDataValue);
        deltaLonBand.setImageInfo(createDeltaBandImageInfo(-0.015, +0.015));

        final Band deltaLatBand = targetProduct.addBand("delta_lat_angular", "latitude - LAT");
        deltaLatBand.setUnit("deg");
        deltaLatBand.setDescription("Delta between old latitude and new latitude in degree");
        deltaLatBand.setNoDataValueUsed(true);
        deltaLatBand.setNoDataValue(noDataValue == null ? Double.NaN : noDataValue);
        deltaLatBand.setImageInfo(createDeltaBandImageInfo(-0.01, +0.01));

        final Band deltaLonMetBand = targetProduct.addBand("delta_lon_metric",
                                                           "cos(rad(LAT)) * 6378137 * rad(longitude - LON)");
        deltaLonMetBand.setUnit("m");
        deltaLonMetBand.setDescription("Delta between old longitude and new longitude in meters");
        deltaLonMetBand.setNoDataValueUsed(true);
        deltaLonMetBand.setNoDataValue(noDataValue == null ? Double.NaN : noDataValue);
        deltaLonMetBand.setImageInfo(createDeltaBandImageInfo(-1500.0, +1500.0));

        final Band deltaLatMetBand = targetProduct.addBand("delta_lat_metric", "6378137 * rad(latitude - LAT)");
        deltaLatMetBand.setUnit("m");
        deltaLatMetBand.setDescription("Delta between old latitude and new latitude in meters");
        deltaLatMetBand.setNoDataValueUsed(true);
        deltaLatMetBand.setNoDataValue(noDataValue == null ? Double.NaN : noDataValue);
        deltaLatMetBand.setImageInfo(createDeltaBandImageInfo(-1000.0, +1000.0));
    }

    private ImageInfo createDeltaBandImageInfo(double p1, double p2) {
        return new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(p1, new Color(255, 0, 0)),
                new ColorPaletteDef.Point((p1 + p2) / 2, new Color(255, 255, 255)),
                new ColorPaletteDef.Point(p2, new Color(0, 0, 127)),
        }));
    }
}
