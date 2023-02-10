/***
 * Image/J Plugins
 * Copyright (C) 2012 Christophe Deroulers
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at http://www.imnc.in2p3.fr/pagesperso/deroulers/software/ndpitools
 */
package fr.cirad.image.ndpilegacy;

import ij.IJ;

/**
 * Assuming that the current image is a preview of an NDPI file, ask for 
 * extraction parameters, then call ndpisplit to extract from the NDPI
 * file the region which corresponds to the selected portion of the
 * preview, store it into TIFF file(s), and open the file at maximum
 * resolution. If there is no selection, extracts all. If the current
 * image is not a preview of an NDPI file or if there is no open image,
 * opens a dialog to select an NDPI file, then extract all.
 * 
 * @author C. Deroulers
 * @version $Revision: 1.4.2 $
 */

public class NDPIToolsCustomConvertDirectoryPlugin implements
        ij.plugin.PlugIn {

    private static final String TITLE = "NDPITools Convert all NDPI file(s) in a directory to TIFF / Mosaics";

    /**
     * Main processing method.
     */
    public void run(String arg) {
        
        IJ.showStatus("Starting \""+TITLE+"\" plugin...");
        ExtractNDPI.convertDirectoryFromGUI(null, TITLE, true);
        IJ.showStatus("");
    }
}
