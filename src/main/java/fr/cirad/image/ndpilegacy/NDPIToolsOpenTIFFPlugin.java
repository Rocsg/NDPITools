/***
 * Image/J Plugins
 * Copyright (C) 2012 Christophe Deroulers
 * after ImageIOOpenPlugin Copyright (C) 2002-2004 Jarek Sacha
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
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.io.FileInfo;
import ij.plugin.PlugIn;

import java.io.File;

/**
 * Open file chooser dialog and open the TIFF image using JAI codec.
 *
 * @author C. Deroulers
 * @version $Revision: 1.5 $
 */

public class NDPIToolsOpenTIFFPlugin implements PlugIn {

    private final static String TITLE = "NDPITools Open TIFF";

    /**
     * Main processing method for the NDPIToolsOpenTIFFPlugin object.
     */
    public void run(String arg) {

        IJ.showStatus("Starting \""+TITLE+"\" plugin...");

        OpenDialog openDialog = new OpenDialog(TITLE, null);
        if (openDialog.getFileName() == null) {
            // No selection
            IJ.showStatus("");
            return;
        }

        File file = new File(openDialog.getDirectory(), 
                openDialog.getFileName());

        IJ.showStatus("Opening: " + file.getName());
        try {
            JAIReader reader = new JAIReader(file);
            ImagePlus[] images = reader.readAllImages();
            if (images != null) {
                FileInfo fi = new FileInfo();
                fi.directory = openDialog.getDirectory();
                fi.fileType = fi.TIFF; /* CHECK THIS */
                fi.fileFormat = fi.TIFF; /* CHECK THIS */
                if (images.length == 1) {
                    fi.fileName = openDialog.getFileName();
                    images[0].setFileInfo(fi);
                }
                for (int j = 0; j < images.length; j++)
                    images[j].show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = "Error opening file: " + file.getName() + ".\n\n";
            msg += (ex.getMessage() == null) ? ex.toString() : ex.getMessage();
            IJ.showMessage(TITLE, msg);
        }
        IJ.showStatus("");
    }

}
