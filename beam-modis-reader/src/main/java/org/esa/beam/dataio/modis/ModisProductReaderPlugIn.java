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
package org.esa.beam.dataio.modis;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ModisProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        File file = null;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        }

        if (file == null || !file.isFile()) {
            return DecodeQualification.UNABLE;
        }

        final String filePath = file.getPath();
        if (!filePath.toLowerCase().endsWith(ModisConstants.DEFAULT_FILE_EXTENSION)) {
            return DecodeQualification.UNABLE;
        }

        NetcdfFile ncfile = null;
        try {
            if (NetcdfFile.canOpen(filePath)) {
                ncfile = NetcdfFile.open(filePath);

                ModisGlobalAttributes modisAttributes;
                final Variable structMeta = ncfile.findTopVariable(ModisConstants.STRUCT_META_KEY);
                if (structMeta == null) {
                    // is IMAPP format
                    System.out.println("IMAPP format");
                } else {
                    final List<Variable> variables = ncfile.getVariables();
                    modisAttributes = new ModisDaacAttributes(variables);
                }
            }
        } catch (IOException ignore) {
        } finally {
            if (ncfile != null) {
                try {
                    ncfile.close();
                } catch (IOException ignore) {
                }
            }
        }

        // @todo 1 tb/tb - add tests
        // @todo 1 tb/tb - remove HDF
//        if (file != null && file.exists() && file.isFile() && file.getPath().toLowerCase().endsWith(ModisConstants.DEFAULT_FILE_EXTENSION)) {
//            try {
//                String path = file.getPath();
//                if (HDF.getWrap().Hishdf(path)) {
//                    int fileId = HDFConstants.FAIL;
//                    int sdStart = HDFConstants.FAIL;
//                    try {
//                        fileId = HDF.getWrap().Hopen(path, HDFConstants.DFACC_RDONLY);
//                        sdStart = HDF.getWrap().SDstart(path, HDFConstants.DFACC_RDONLY);
//                        HdfAttributes globalAttrs = HdfUtils.readAttributes(sdStart);
//
//                        // check wheter daac or imapp
//                        ModisGlobalAttributes modisAttributes;
//                        if (globalAttrs.getStringAttributeValue(ModisConstants.STRUCT_META_KEY) == null) {
//                            modisAttributes = new ModisImappAttributes(file, sdStart, globalAttrs);
//                        } else {
//                            modisAttributes = new ModisDaacAttributes(globalAttrs);
//                        }
//                        final String productType = modisAttributes.getProductType();
//                        if (ModisProductDb.getInstance().isSupportedProduct(productType)) {
//                            return DecodeQualification.INTENDED;
//                        }
//                    } catch (HDFException ignore) {
//                    } finally {
//                        if (sdStart != HDFConstants.FAIL) {
//                            HDF.getWrap().Hclose(sdStart);
//                        }
//                        if (fileId != HDFConstants.FAIL) {
//                            HDF.getWrap().Hclose(fileId);
//                        }
//                    }
//                }
//            } catch (Exception ignore) {
//            }
//        }
        return DecodeQualification.UNABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new ModisProductReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        final String[] formatNames = getFormatNames();

        return new BeamFileFilter(formatNames[0], getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return new String[]{ModisConstants.DEFAULT_FILE_EXTENSION};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return ModisConstants.READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return new String[]{ModisConstants.FORMAT_NAME};
    }
}
