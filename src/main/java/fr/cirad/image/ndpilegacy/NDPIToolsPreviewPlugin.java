/***
 * Image/J Plugins
 * Copyright (C) 2012-2014 Christophe Deroulers
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
import ij.ImageJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.io.File;

/**
 * Open file chooser dialog, call ndpisplit to extract a preview image 
 * from open the selected NDPI file, and open the preview image using 
 * JAI codec.
 *
 * @author C. Deroulers
 * @version $Revision: 1.7.1 $
 */

public class NDPIToolsPreviewPlugin implements ij.plugin.PlugIn {

    private static final String TITLE = "NDPITools Preview NDPI";
    private static final String[] minimalAcceptedNdpisplitVersions =
        { "1.5-1", "1.6.6" };
    double factorFromPreviewToLargestImage = -1;
    String typeOfPreviewImage = "";
    String pathOfFileContainingMap = "";
    String availableMagnifications = "";
    String availableDimensions = "";
    String availableZOffsets = "";
    String ndpiFluorescence;

    /**
     * Main processing method for the NDPIToolsPreviewPlugin object.
     */
    
    public void run(String arg) {
         File ndpiFile;

        IJ.showStatus("Starting \""+TITLE+"\" plugin...");

        String acceptableNdpisplitVersion =
            NDPISplit.getAcceptableVersion(
                minimalAcceptedNdpisplitVersions, TITLE, "");
        /*System.err.printf("acceptable version \"%s\"\n", 
            acceptableNdpisplitVersion);*/
        if (acceptableNdpisplitVersion == null)
        {
            IJ.showStatus("");
            return;
        }

        if (arg == null || arg.equals(""))
        {
            ij.io.OpenDialog openDialog = new ij.io.OpenDialog(TITLE, null);
            if (openDialog.getFileName() == null)
            {
                // No selection
                IJ.showStatus("");
                return;
            }
            ndpiFile = new File(openDialog.getDirectory(),
                openDialog.getFileName());
        } else
        {
            ndpiFile = new File(arg);
        }

        String[] res;

        try
        {
            /*java.awt.Dimension screenSize = IJ.getScreenSize();*/
            java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            String[] args = new String[] {"-K",
                "-p0," + screen.width + "x" + screen.height,
                ndpiFile.getPath()};
            IJ.showStatus("Extracting preview from: " + ndpiFile.getName());
            res = NDPISplit.run(args, false);
            System.out.println("Resolution tab : ");
            for(int i=0;i<res.length;i++) {
            	System.out.println(res[i]);
            }
            if (res.length == 0 ||
                (res.length == 1 && res[0].startsWith("Ndpisplit version:")))
                throw new Exception("The ndpisplit program did not output anything.");
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = "Error opening file: " + ndpiFile.getName() +
                ".\n\n";
            msg += (ex.getMessage() == null) ? ex.toString() : ex.getMessage();
            IJ.showMessage(TITLE, msg);
            IJ.showStatus("");
            return;
        }

        ArrayList<ImagePlus> imageList = new ArrayList<ImagePlus>();

        for (int i = 0 ; i < res.length ; i++)
        {
            int pos = res[i].indexOf(':');
            if (pos < 0)
                continue;
            if (res[i].startsWith("Factor from preview image to largest image:"))
                factorFromPreviewToLargestImage =
                    Double.parseDouble(res[i].substring(pos+1));
            else if (res[i].startsWith("Type of preview image:"))
                typeOfPreviewImage = res[i].substring(pos+1);
            else if (res[i].startsWith("File containing map:"))
                pathOfFileContainingMap = res[i].substring(pos+1);
            else if (res[i].startsWith("Found images at magnifications:"))
                availableMagnifications = res[i].substring(pos+1);
            else if (res[i].startsWith("Found images of sizes:"))
                availableDimensions = res[i].substring(pos+1);
            else if (res[i].startsWith("Found images at z-offsets:"))
                availableZOffsets = res[i].substring(pos+1);
            else if (res[i].startsWith("Fluorescence:"))
            	ndpiFluorescence = res[i].substring(pos+1);
            else if (res[i].startsWith("File containing a preview image:"))
            {
                File file = new File(res[i].substring(pos+1));
                try
                {
                    JAIReader reader = new JAIReader(file);
                    ImagePlus image = reader.readFirstImage();
                    if (image != null)
                    {
                        image.setProperty("PreviewOfNDPIPath",
                            ndpiFile.getPath());
                        image.setProperty("PreviewOfNDPIType",
                            typeOfPreviewImage);
                        image.setProperty("PreviewOfNDPIRatio",
                            new Double(factorFromPreviewToLargestImage));
                        image.setProperty("PreviewOfNDPIAvailableMagnifications",
                            availableMagnifications);
                        image.setProperty("PreviewOfNDPIAvailableDimensions",
                            availableDimensions);
                        image.setProperty("PreviewOfNDPIAvailableZOffsets",
                            availableZOffsets);
                        image.setProperty("NDPIFluorescence",
                            ndpiFluorescence);
                        imageList.add(image);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String msg = "Error opening file: " + file.getName() 
                     + ".\n\n";
                    msg += (ex.getMessage() == null) ? ex.toString() :
                        ex.getMessage();
                    IJ.showMessage(TITLE, msg);
                }
            }
        }
        if (imageList.size() != 0)
        {
        	System.out.println("Processing last part with size="+imageList.size());
            ImagePlus[] images =
                JAIReader.combineImagesIntoStack(imageList,
                    ndpiFile.getName() + " preview (" + 
                        typeOfPreviewImage + ")");
            for (int i = 0 ; i < images.length ; i++)
                images[i].show();
        }
        /*else
        { *//* Open map if nothing else is available *//*
            File file = new File(pathOfFileContainingMap);
        }*/
        IJ.showStatus("");
    }

}
