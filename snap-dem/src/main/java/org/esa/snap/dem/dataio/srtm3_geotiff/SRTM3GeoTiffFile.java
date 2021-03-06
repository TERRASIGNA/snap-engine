/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.srtm3_geotiff;

import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.dataop.dem.ElevationFile;
import org.esa.snap.framework.dataop.dem.ElevationTile;
import org.esa.snap.util.Settings;

import java.io.File;
import java.io.IOException;

/**
 * Holds information about a dem file.
 */
public final class SRTM3GeoTiffFile extends ElevationFile {

    private final SRTM3GeoTiffElevationModel demModel;

    private static final String remoteFTP = Settings.instance().get("DEM.srtm3GeoTiffDEM_FTP");
    private static final String remotePath = Settings.getPath("DEM.srtm3GeoTiffDEM_remotePath");
    private static final String remoteHTTP = Settings.instance().get("DEM.srtm3GeoTiffDEM_HTTP");

    public SRTM3GeoTiffFile(final SRTM3GeoTiffElevationModel model, final File localFile, final ProductReader reader) {
        super(localFile, reader);
        this.demModel = model;
    }

    protected String getRemoteFTP() {
        return remoteFTP;
    }

    protected String getRemotePath() {
        return remotePath;
    }

    protected ElevationTile createTile(final Product product) throws IOException {
        final SRTM3GeoTiffElevationTile tile = new SRTM3GeoTiffElevationTile(demModel, product);
        demModel.updateCache(tile);
        return tile;
    }

    protected boolean getRemoteFile() throws IOException {
        try {
            if (remoteFTP.isEmpty() || remoteFTP.startsWith("http")) {
                return getRemoteHttpFile(remoteHTTP);
            }
            return getRemoteFTPFile();
        } catch (Exception e) {
            return getRemoteHttpFile(remoteHTTP);
        }
    }
}
