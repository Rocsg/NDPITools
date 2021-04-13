/***
 * Image/J Plugins
 * Copyright (C) 2012-2014 Christophe Deroulers
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
package com.phenomen.ndpilegacy;

import ij.IJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;

import com.phenomen.factory.NDPI;
import com.vitimage.common.VitimageUtils;

import java.io.File;
import java.awt.Dimension;

/**
 * @author C. Deroulers
 * @version $Revision: 1.7.1 $
 */

public class ExtractNDPI {
    static ArrayList<Double> availableMagnifications;
    static ArrayList<Dimension> availableDimensions;
    static ArrayList<Integer> availableZOffsets;
    static long sizeOfLargestExtractedImage = 0;
    static double magnificationOfLargestExtractedImage = 0;
    static String ndpiFluorescence;


    public static void extractFromGUIBis( String TITLE,
            boolean askForParameters,String magnString) {
        String NDPIPath="";
        String typeOfPreview = null;
        double factorFromPreviewToLargestImage = 0;
        String zoneToExtractArg = null;
        ImagePlus im = null;
        long widthOfLargestExtractedImage = 0,
        heightOfLargestExtractedImage = 0;
		availableMagnifications = new ArrayList<Double>();
        availableDimensions = new ArrayList<Dimension>();
        availableZOffsets = new ArrayList<Integer>();

        if (false)
            NDPIPath = "";
        else
        {
            im = ij.WindowManager.getCurrentImage();
            if (im != null)
            {
                NDPIPath = (String) im.getProperty("PreviewOfNDPIPath");
                if (NDPIPath != null)
                {
                    typeOfPreview = (String)
                        im.getProperty("PreviewOfNDPIType");
                    factorFromPreviewToLargestImage = (Double)
                        im.getProperty("PreviewOfNDPIRatio");
                    parseListOfMagnifications(
                        (String) im.getProperty("PreviewOfNDPIAvailableMagnifications"),
                        availableMagnifications);
                    parseListOfDimensions(
                        (String) im.getProperty("PreviewOfNDPIAvailableDimensions"),
                        availableDimensions);
                    parseListOfIntegers(
                        (String) im.getProperty("PreviewOfNDPIAvailableZOffsets"),
                        availableZOffsets);
                    ndpiFluorescence = (String) im.getProperty("NDPIFluorescence");
                }
            }
            if (im == null || NDPIPath == null)
            { // Choose NDPI file and extract all
                ij.io.OpenDialog openDialog =
                    new ij.io.OpenDialog(
                        (im == null ? "No NDPI file previewed" :
                            "Current image is not a NDPI file") +
                            " -- please select one",
                        null);
                if (openDialog.getFileName() == null)
                    return;
                File ndpiFile = new File(openDialog.getDirectory(),
                    openDialog.getFileName());
                NDPIPath = ndpiFile.getPath();

                // Try to get the file's parameters by running ndpisplit
                // (without extracting a preview image)
                String[] res = new String[0];
                try
                {
                    String[] args = new String[] {"-K", "-x0", NDPIPath};
                    IJ.showStatus("Extracting parameters from: " + NDPIPath);
                    res = NDPISplit.run(args, false);
                } catch (Exception ex) {
                }
                IJ.showStatus("");
                for (int i = 0 ; i < res.length ; i++)
                {
                    int pos = res[i].indexOf(':');
                    if (pos < 0)
                        continue;
                    if (res[i].startsWith("Factor from preview image to largest image:"))
                        factorFromPreviewToLargestImage =
                            Double.parseDouble(res[i].substring(pos+1));
                    else if (res[i].startsWith("Found images at magnifications:"))
                        parseListOfMagnifications(res[i].substring(pos+1),
                            availableMagnifications);
                    else if (res[i].startsWith("Found images of sizes:"))
                        parseListOfDimensions(res[i].substring(pos+1),
                            availableDimensions);
                    else if (res[i].startsWith("Found images at z-offsets:"))
                        parseListOfIntegers(res[i].substring(pos+1),
                            availableZOffsets);
                    else if (res[i].startsWith("Fluorescence"))
                    	ndpiFluorescence = res[i].substring(pos+1);
                }
            }
        }

        ArrayList<String> args = new ArrayList<String>();
        args.add("-vv");
        args.add("-cn");
        args.add("-K");
        Custom_Extract_Dialog dialog = new Custom_Extract_Dialog();

        IJ.log("WE ARE HERE");
        VitimageUtils.printImageResume(im);
        IJ.log(""+im.getRoi());
        ij.gui.Roi roi = im != null ? im.getRoi() : null;
        ij.gui.Roi[] rois = new ij.gui.Roi[0];
        if (typeOfPreview != null && roi != null)
        {
            Calendar t = Calendar.getInstance();
            String ts = String.format("%04d%02d%02d%02d%02d%02d",
                t.get(t.YEAR), t.get(t.MONTH)+1,
                t.get(t.DAY_OF_MONTH), t.get(t.HOUR_OF_DAY),
                t.get(t.MINUTE), t.get(t.SECOND));
            dialog.label = "extract" + ts;

            if (roi instanceof ij.gui.ShapeRoi)
            {
                rois = ((ij.gui.ShapeRoi) roi).getRois();
                if (rois.length > 1)
                    dialog.label += "-#";
            }
            else
            {
                rois = new ij.gui.Roi[1];
                rois[0] = roi;
            }

            if (typeOfPreview.equals("macroscopic"))
            {
                IJ.showMessage(TITLE,
                "Not yet implemented for this type of preview image...\n" +
                "Try again without selecting a region;\n" +
                "that will extract the whole scanned image.");
                return;
            } else if (typeOfPreview.matches(
                "\\d+(?:\\.\\d*(?:[eE][+-]?\\d+])?)?x"))
            {
                sizeOfLargestExtractedImage = 0;
                for (int r = 0 ; r < rois.length ; r++)
                {
                    java.awt.Rectangle bounds = rois[r].getBounds();
                    double relW = bounds.getWidth() / im.getWidth();
                    double relH = bounds.getHeight() / im.getHeight();
                    long width = Math.round(Math.ceil(
                        bounds.getWidth() *
                        factorFromPreviewToLargestImage));
                    long height = Math.round(Math.ceil(
                        bounds.getHeight() *
                        factorFromPreviewToLargestImage));
                    long size = width * height;

                    if (size > sizeOfLargestExtractedImage)
                    {
                        widthOfLargestExtractedImage = width;
                        heightOfLargestExtractedImage = height;
                        sizeOfLargestExtractedImage = size;
                        for (int i = 0 ; i < availableDimensions.size() ; i++)
                        {
                            Dimension d = availableDimensions.get(i);
                            d.width *= relW;
                            d.height *= relH;
                            availableDimensions.set(i, d);
                        }
                    }
                }
                magnificationOfLargestExtractedImage =
                    java.util.Collections.max(availableMagnifications);
                dialog.numberOfRois = rois.length;
            } else
            {
                IJ.showMessage(TITLE,
                "Type of NDPI preview image (" + typeOfPreview + ") " +
                "not recognized. I don't know how to extract.\n" +
                "Try again without selecting a region;\n" +
                "that will extract the whole scanned image.");
                return;
            }
        } else /* roi == null || typeOfPreview == null */ {
             /* Find size of largest image from availableDimensions */
            for (int i = 0 ; i < availableDimensions.size() ; i++)
            {
                Dimension d = availableDimensions.get(i);
                long size = (long) d.width * d.height;
                if (size > sizeOfLargestExtractedImage)
                {
                    sizeOfLargestExtractedImage = size;
                    widthOfLargestExtractedImage = d.width;
                    heightOfLargestExtractedImage = d.height;
                    magnificationOfLargestExtractedImage =
                        availableMagnifications.get(i);
                }
            }
            if (sizeOfLargestExtractedImage == 0 && typeOfPreview != null)
            {
                widthOfLargestExtractedImage = Math.round(Math.ceil(
                    im.getWidth() * factorFromPreviewToLargestImage));
                heightOfLargestExtractedImage = Math.round(Math.ceil(
                    im.getHeight() * factorFromPreviewToLargestImage));
                sizeOfLargestExtractedImage = widthOfLargestExtractedImage *
                    heightOfLargestExtractedImage;
                magnificationOfLargestExtractedImage =
                    java.util.Collections.max(availableMagnifications);
            }
        }

        if (askForParameters)
        {
            dialog.run("");
            if (dialog.wasCanceled)
                return;
            args.addAll(dialog.ndpisplitCustomArgs);

            if (dialog.label.indexOf(':') >= 0 ||
                dialog.label.indexOf(',') >= 0 ||
                dialog.label.indexOf('"') >= 0)
            {
                IJ.showMessage(TITLE,
                "Label of extracted zones should not contain the " +
                "following characters: colon (:), comma (,), quotes " + 
                "(\").");
                return;
            }

            if (dialog.extractMagnifications.length > 0)
            {
                String arg = "-x";
                double maxAvailableMagnification =
                    availableMagnifications.get(0),
                    maxSelectedMagnification = -0.5;
                for (int i = 0 ; i < dialog.extractMagnifications.length ; i++)
                {
                    if (availableMagnifications.get(i) > maxAvailableMagnification)
                        maxAvailableMagnification = availableMagnifications.get(i);
                    if (dialog.extractMagnifications[i])
                    {
                        arg += availableMagnifications.get(i) + ",";
                        if (maxSelectedMagnification == -0.5 ||
                            availableMagnifications.get(i) > maxSelectedMagnification)
                            maxSelectedMagnification = availableMagnifications.get(i);
                    }
                }
                if (maxSelectedMagnification == -0.5)
                    return;
                args.add(arg);
                if (maxSelectedMagnification < maxAvailableMagnification)
                {
                    widthOfLargestExtractedImage = Math.round(Math.ceil(
                        widthOfLargestExtractedImage * maxSelectedMagnification
                        / maxAvailableMagnification));
                    heightOfLargestExtractedImage = Math.round(Math.ceil(
                        heightOfLargestExtractedImage * maxSelectedMagnification
                        / maxAvailableMagnification));
                }
            }

            if (dialog.extractZOffsets.length > 0)
            {
                String arg = "-z";
                for (int i = 0 ; i < dialog.extractZOffsets.length ; i++)
                    if (dialog.extractZOffsets[i])
                        arg += availableZOffsets.get(i) + ",";
                args.add(arg);
            }
        }

        if (typeOfPreview != null &&
            ij.IJ.getVersion().compareTo("2") < 0 &&
            (widthOfLargestExtractedImage > 65536 ||
             heightOfLargestExtractedImage > 65536))
            {
                Confirm_Extraction_Dialog confirm_dialog =
                    new Confirm_Extraction_Dialog();
                confirm_dialog.run("");
                if (confirm_dialog.wasCanceled)
                    return;
            }

        ArrayList<String> labels = new ArrayList<String>();
        if (typeOfPreview != null && roi != null)
        {
            if (dialog.label.equals("") && rois.length > 1)
              dialog.label = "-#";

            zoneToExtractArg = "-e";
            String roiNumberFormat =
                "%0" + (""+rois.length).length() + "d";

            for (int r = 0 ; r < rois.length ; r++)
            {
                java.awt.Rectangle bounds = rois[r].getBounds();
                double relX = bounds.getX() / im.getWidth();
                double relY = bounds.getY() / im.getHeight();
                double relW = bounds.getWidth() / im.getWidth();
                double relH = bounds.getHeight() / im.getHeight();
                zoneToExtractArg += relX + "," + relY + "," +
                    relW + "," + relH;
                if (dialog.label != null)
                {
                    String l = dialog.label.replace("#",
                        String.format(roiNumberFormat, r+1));
                    zoneToExtractArg += "," + l;
                    labels.add("_" + l);
                }
                zoneToExtractArg += ":";
            }

            args.add(zoneToExtractArg);
        }

        args.add(magnString);
       args.add(NDPIPath);
        String[] res;
        try
        {
    		for(int i=0;i<args.size();i++) {
    			IJ.showMessage("args["+i+"]="+args.get(i));
    		}
            res = NDPISplit.run(args.toArray(new String[0]),
                dialog.logMessagesFromNdpisplit);
            for(int i=0;i<res.length;i++) {
            	IJ.showMessage("res["+i+"]="+res[i]);
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            String msg = "Error while running ndpisplit on: " +
                NDPIPath + ".\n\n";
            msg += (ex.getMessage() == null) ? ex.toString() :
                ex.getMessage();
            IJ.showMessage(TITLE, msg);
            return;
        }

        ArrayList<String> TIFFScannedImages = new ArrayList<String>();
        for (int i = 0 ; i < res.length ; i++)
        {
                int pos = res[i].indexOf(':');
                if (pos < 0)
                    continue;
                if (res[i].startsWith("File containing a TIFF scanned image:"))
                    TIFFScannedImages.add(res[i].substring(pos+1));
        }

        if (TIFFScannedImages.size() == 0)
        {
            IJ.showMessage(TITLE, "No image was extracted by ndpisplit.");
            return;
        }

        if (! dialog.openImagesAtLargestMagnificationAfterExtraction)
            return;

        String filenamePrefix = NDPIPath;
        if (NDPIPath.toLowerCase().endsWith(".ndpi"))
            filenamePrefix = NDPIPath.substring(0, NDPIPath.length()-5);
          /* Sort the filenames of the produced TIFF files according to 
           * their magnification, in descending order (they all are in 
           * ..._x<m>_z<sh>...): first, find their largest common prefix 
           * with one underscore (which should be ..._x), then sort them 
           * according to the first number after this prefix */
        FilenameWithMagnificationComparator comparatorMagn =
                new FilenameWithMagnificationComparator(
                    largestCommonPrefix(TIFFScannedImages,
                        filenamePrefix.length(), 1));
        java.util.Collections.sort(TIFFScannedImages,
                comparatorMagn);

          /* Extract the filenames with the highest magnification: those 
           * which have the same magn. as the first one */
        ArrayList<String> TIFFScannedImagesAtLargestMagn =
                new ArrayList<String>();
        for(String s : TIFFScannedImages)
                if (comparatorMagn.compare(TIFFScannedImages.get(0), s)
                        == 0)
                    TIFFScannedImagesAtLargestMagn.add(s);
                else
                    break;

          /* For each label, open the images */
        if (labels.size() == 0)
          labels.add("");
        for (String label : labels)
        {
              /* Find images with this label */
            ArrayList<String> TIFFScannedImagesAtLargestMagnWithThisLabel =
                new ArrayList<String>();
            for (String s : TIFFScannedImagesAtLargestMagn)
                if (s.endsWith(label + ".tif"))
                    TIFFScannedImagesAtLargestMagnWithThisLabel.add(s);

              /* Sort them according to z-shift: first, find their 
               * largest common prefix with two underscores (which 
               * should be ..._x<m>_z), then sort them according to the 
               * first number after this prefix */
            if (TIFFScannedImagesAtLargestMagnWithThisLabel.size() > 1)
            {
                FilenameWithMagnificationComparator comparatorZ =
                    new FilenameWithMagnificationComparator(
                        largestCommonPrefix(TIFFScannedImagesAtLargestMagn,
                            filenamePrefix.length(), 2));
                java.util.Collections.sort(TIFFScannedImagesAtLargestMagn, 
                    comparatorZ);
            }

              /* Open the images */
            ArrayList<ImagePlus> imageList = new ArrayList<ImagePlus>();
            for (String fn : TIFFScannedImagesAtLargestMagnWithThisLabel)
            {
                IJ.showMessage("Processing "+fn);
            	File extractFile = new File(fn);
                try
                {
                    IJ.showStatus("Opening " + extractFile.getName() + 
                        "...");
                    JAIReader reader = new JAIReader(extractFile);
                    ImagePlus extract = reader.readFirstImage();
                    if (extract != null)
                    {
                        if (ndpiFluorescence != null)
                            extract.setProperty("NDPIFluorescence",
                                ndpiFluorescence);
                        imageList.add(extract);
                    }
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                    String msg = "Error opening file: " + 
                        extractFile.getName() + ".\n\n";
                    msg += (ex.getMessage() == null) ? ex.toString() :
                        ex.getMessage();
                    IJ.showMessage(TITLE, msg);
                }
            }
              /* Show the images (combined into a stack if possible) */
            ImagePlus[] images =
                JAIReader.combineImagesIntoStack(imageList,
                    (new File(NDPIPath)).getName() + label);
            for (ImagePlus i : images)
                i.show();
        } /* for label */
    }
  
    
    /**
     * Assuming that the current image is a preview of an NDPI file,
     * call ndpisplit to extract from the NDPI file the region which
     * corresponds to the selected portion of the preview, store it into
     * TIFF file(s), and open the file at maximum resolution. If there
     * is no selection, extracts all. If the current image is not a
     * preview of an NDPI file or if there is no open image, opens a
     * dialog to select an NDPI file, then extract the whole image.
     */
    public static void extractFromGUI(String fonctarg, String TITLE,
            boolean askForParameters) {
        String NDPIPath = null;
        String typeOfPreview = null;
        double factorFromPreviewToLargestImage = 0;
        String zoneToExtractArg = null;
        ImagePlus im = null;
        long widthOfLargestExtractedImage = 0,
        heightOfLargestExtractedImage = 0;
		availableMagnifications = new ArrayList<Double>();
        availableDimensions = new ArrayList<Dimension>();
        availableZOffsets = new ArrayList<Integer>();

        if (fonctarg != null && ! fonctarg.equals(""))
            NDPIPath = fonctarg;
        else
        {
            im = ij.WindowManager.getCurrentImage();
            if (im != null)
            {
                NDPIPath = (String) im.getProperty("PreviewOfNDPIPath");
                if (NDPIPath != null)
                {
                    typeOfPreview = (String)
                        im.getProperty("PreviewOfNDPIType");
                    factorFromPreviewToLargestImage = (Double)
                        im.getProperty("PreviewOfNDPIRatio");
                    parseListOfMagnifications(
                        (String) im.getProperty("PreviewOfNDPIAvailableMagnifications"),
                        availableMagnifications);
                    parseListOfDimensions(
                        (String) im.getProperty("PreviewOfNDPIAvailableDimensions"),
                        availableDimensions);
                    parseListOfIntegers(
                        (String) im.getProperty("PreviewOfNDPIAvailableZOffsets"),
                        availableZOffsets);
                    ndpiFluorescence = (String) im.getProperty("NDPIFluorescence");
                }
            }
            if (im == null || NDPIPath == null)
            { // Choose NDPI file and extract all
                ij.io.OpenDialog openDialog =
                    new ij.io.OpenDialog(
                        (im == null ? "No NDPI file previewed" :
                            "Current image is not a NDPI file") +
                            " -- please select one",
                        null);
                if (openDialog.getFileName() == null)
                    return;
                File ndpiFile = new File(openDialog.getDirectory(),
                    openDialog.getFileName());
                NDPIPath = ndpiFile.getPath();

                // Try to get the file's parameters by running ndpisplit
                // (without extracting a preview image)
                String[] res = new String[0];
                try
                {
                    String[] args = new String[] {"-K", "-x0", NDPIPath};
                    IJ.showStatus("Extracting parameters from: " + NDPIPath);
                    res = NDPISplit.run(args, false);
                } catch (Exception ex) {
                }
                IJ.showStatus("");
                for (int i = 0 ; i < res.length ; i++)
                {
                    int pos = res[i].indexOf(':');
                    if (pos < 0)
                        continue;
                    if (res[i].startsWith("Factor from preview image to largest image:"))
                        factorFromPreviewToLargestImage =
                            Double.parseDouble(res[i].substring(pos+1));
                    else if (res[i].startsWith("Found images at magnifications:"))
                        parseListOfMagnifications(res[i].substring(pos+1),
                            availableMagnifications);
                    else if (res[i].startsWith("Found images of sizes:"))
                        parseListOfDimensions(res[i].substring(pos+1),
                            availableDimensions);
                    else if (res[i].startsWith("Found images at z-offsets:"))
                        parseListOfIntegers(res[i].substring(pos+1),
                            availableZOffsets);
                    else if (res[i].startsWith("Fluorescence"))
                    	ndpiFluorescence = res[i].substring(pos+1);
                }
            }
        }

        ArrayList<String> args = new ArrayList<String>();
        args.add("-vv");
        args.add("-K");
        Custom_Extract_Dialog dialog = new Custom_Extract_Dialog();

        IJ.log("WE ARE HERE");
        VitimageUtils.printImageResume(im);
        IJ.log(""+im.getRoi());
        ij.gui.Roi roi = im != null ? im.getRoi() : null;
        ij.gui.Roi[] rois = new ij.gui.Roi[0];
        if (typeOfPreview != null && roi != null)
        {
            Calendar t = Calendar.getInstance();
            String ts = String.format("%04d%02d%02d%02d%02d%02d",
                t.get(t.YEAR), t.get(t.MONTH)+1,
                t.get(t.DAY_OF_MONTH), t.get(t.HOUR_OF_DAY),
                t.get(t.MINUTE), t.get(t.SECOND));
            dialog.label = "extract" + ts;

            if (roi instanceof ij.gui.ShapeRoi)
            {
                rois = ((ij.gui.ShapeRoi) roi).getRois();
                if (rois.length > 1)
                    dialog.label += "-#";
            }
            else
            {
                rois = new ij.gui.Roi[1];
                rois[0] = roi;
            }

            if (typeOfPreview.equals("macroscopic"))
            {
                IJ.showMessage(TITLE,
                "Not yet implemented for this type of preview image...\n" +
                "Try again without selecting a region;\n" +
                "that will extract the whole scanned image.");
                return;
            } else if (typeOfPreview.matches(
                "\\d+(?:\\.\\d*(?:[eE][+-]?\\d+])?)?x"))
            {
                sizeOfLargestExtractedImage = 0;
                for (int r = 0 ; r < rois.length ; r++)
                {
                    java.awt.Rectangle bounds = rois[r].getBounds();
                    double relW = bounds.getWidth() / im.getWidth();
                    double relH = bounds.getHeight() / im.getHeight();
                    long width = Math.round(Math.ceil(
                        bounds.getWidth() *
                        factorFromPreviewToLargestImage));
                    long height = Math.round(Math.ceil(
                        bounds.getHeight() *
                        factorFromPreviewToLargestImage));
                    long size = width * height;

                    if (size > sizeOfLargestExtractedImage)
                    {
                        widthOfLargestExtractedImage = width;
                        heightOfLargestExtractedImage = height;
                        sizeOfLargestExtractedImage = size;
                        for (int i = 0 ; i < availableDimensions.size() ; i++)
                        {
                            Dimension d = availableDimensions.get(i);
                            d.width *= relW;
                            d.height *= relH;
                            availableDimensions.set(i, d);
                        }
                    }
                }
                magnificationOfLargestExtractedImage =
                    java.util.Collections.max(availableMagnifications);
                dialog.numberOfRois = rois.length;
            } else
            {
                IJ.showMessage(TITLE,
                "Type of NDPI preview image (" + typeOfPreview + ") " +
                "not recognized. I don't know how to extract.\n" +
                "Try again without selecting a region;\n" +
                "that will extract the whole scanned image.");
                return;
            }
        } else /* roi == null || typeOfPreview == null */ {
             /* Find size of largest image from availableDimensions */
            for (int i = 0 ; i < availableDimensions.size() ; i++)
            {
                Dimension d = availableDimensions.get(i);
                long size = (long) d.width * d.height;
                if (size > sizeOfLargestExtractedImage)
                {
                    sizeOfLargestExtractedImage = size;
                    widthOfLargestExtractedImage = d.width;
                    heightOfLargestExtractedImage = d.height;
                    magnificationOfLargestExtractedImage =
                        availableMagnifications.get(i);
                }
            }
            if (sizeOfLargestExtractedImage == 0 && typeOfPreview != null)
            {
                widthOfLargestExtractedImage = Math.round(Math.ceil(
                    im.getWidth() * factorFromPreviewToLargestImage));
                heightOfLargestExtractedImage = Math.round(Math.ceil(
                    im.getHeight() * factorFromPreviewToLargestImage));
                sizeOfLargestExtractedImage = widthOfLargestExtractedImage *
                    heightOfLargestExtractedImage;
                magnificationOfLargestExtractedImage =
                    java.util.Collections.max(availableMagnifications);
            }
        }

        if (askForParameters)
        {
            dialog.run("");
            if (dialog.wasCanceled)
                return;
            args.addAll(dialog.ndpisplitCustomArgs);

            if (dialog.label.indexOf(':') >= 0 ||
                dialog.label.indexOf(',') >= 0 ||
                dialog.label.indexOf('"') >= 0)
            {
                IJ.showMessage(TITLE,
                "Label of extracted zones should not contain the " +
                "following characters: colon (:), comma (,), quotes " + 
                "(\").");
                return;
            }

            if (dialog.extractMagnifications.length > 0)
            {
                String arg = "-x";
                double maxAvailableMagnification =
                    availableMagnifications.get(0),
                    maxSelectedMagnification = -0.5;
                for (int i = 0 ; i < dialog.extractMagnifications.length ; i++)
                {
                    if (availableMagnifications.get(i) > maxAvailableMagnification)
                        maxAvailableMagnification = availableMagnifications.get(i);
                    if (dialog.extractMagnifications[i])
                    {
                        arg += availableMagnifications.get(i) + ",";
                        if (maxSelectedMagnification == -0.5 ||
                            availableMagnifications.get(i) > maxSelectedMagnification)
                            maxSelectedMagnification = availableMagnifications.get(i);
                    }
                }
                if (maxSelectedMagnification == -0.5)
                    return;
                args.add(arg);
                if (maxSelectedMagnification < maxAvailableMagnification)
                {
                    widthOfLargestExtractedImage = Math.round(Math.ceil(
                        widthOfLargestExtractedImage * maxSelectedMagnification
                        / maxAvailableMagnification));
                    heightOfLargestExtractedImage = Math.round(Math.ceil(
                        heightOfLargestExtractedImage * maxSelectedMagnification
                        / maxAvailableMagnification));
                }
            }

            if (dialog.extractZOffsets.length > 0)
            {
                String arg = "-z";
                for (int i = 0 ; i < dialog.extractZOffsets.length ; i++)
                    if (dialog.extractZOffsets[i])
                        arg += availableZOffsets.get(i) + ",";
                args.add(arg);
            }
        }

        if (typeOfPreview != null &&
            ij.IJ.getVersion().compareTo("2") < 0 &&
            (widthOfLargestExtractedImage > 65536 ||
             heightOfLargestExtractedImage > 65536))
            {
                Confirm_Extraction_Dialog confirm_dialog =
                    new Confirm_Extraction_Dialog();
                confirm_dialog.run("");
                if (confirm_dialog.wasCanceled)
                    return;
            }

        ArrayList<String> labels = new ArrayList<String>();
        if (typeOfPreview != null && roi != null)
        {
            if (dialog.label.equals("") && rois.length > 1)
              dialog.label = "-#";

            zoneToExtractArg = "-e";
            String roiNumberFormat =
                "%0" + (""+rois.length).length() + "d";

            for (int r = 0 ; r < rois.length ; r++)
            {
                java.awt.Rectangle bounds = rois[r].getBounds();
                double relX = bounds.getX() / im.getWidth();
                double relY = bounds.getY() / im.getHeight();
                double relW = bounds.getWidth() / im.getWidth();
                double relH = bounds.getHeight() / im.getHeight();
                zoneToExtractArg += relX + "," + relY + "," +
                    relW + "," + relH;
                if (dialog.label != null)
                {
                    String l = dialog.label.replace("#",
                        String.format(roiNumberFormat, r+1));
                    zoneToExtractArg += "," + l;
                    labels.add("_" + l);
                }
                zoneToExtractArg += ":";
            }

            args.add(zoneToExtractArg);
        }

         args.add(NDPIPath);
        String[] res;
        try
        {
    		for(int i=0;i<args.size();i++) {
    			IJ.showMessage("args["+i+"]="+args.get(i));
    		}
            res = NDPISplit.run(args.toArray(new String[0]),
                dialog.logMessagesFromNdpisplit);
            for(int i=0;i<res.length;i++) {
            	IJ.showMessage("res["+i+"]="+res[i]);
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            String msg = "Error while running ndpisplit on: " +
                NDPIPath + ".\n\n";
            msg += (ex.getMessage() == null) ? ex.toString() :
                ex.getMessage();
            IJ.showMessage(TITLE, msg);
            return;
        }

        ArrayList<String> TIFFScannedImages = new ArrayList<String>();
        for (int i = 0 ; i < res.length ; i++)
        {
                int pos = res[i].indexOf(':');
                if (pos < 0)
                    continue;
                if (res[i].startsWith("File containing a TIFF scanned image:"))
                    TIFFScannedImages.add(res[i].substring(pos+1));
        }

        if (TIFFScannedImages.size() == 0)
        {
            IJ.showMessage(TITLE, "No image was extracted by ndpisplit.");
            return;
        }

        if (! dialog.openImagesAtLargestMagnificationAfterExtraction)
            return;

        String filenamePrefix = NDPIPath;
        if (NDPIPath.toLowerCase().endsWith(".ndpi"))
            filenamePrefix = NDPIPath.substring(0, NDPIPath.length()-5);
          /* Sort the filenames of the produced TIFF files according to 
           * their magnification, in descending order (they all are in 
           * ..._x<m>_z<sh>...): first, find their largest common prefix 
           * with one underscore (which should be ..._x), then sort them 
           * according to the first number after this prefix */
        FilenameWithMagnificationComparator comparatorMagn =
                new FilenameWithMagnificationComparator(
                    largestCommonPrefix(TIFFScannedImages,
                        filenamePrefix.length(), 1));
        java.util.Collections.sort(TIFFScannedImages,
                comparatorMagn);

          /* Extract the filenames with the highest magnification: those 
           * which have the same magn. as the first one */
        ArrayList<String> TIFFScannedImagesAtLargestMagn =
                new ArrayList<String>();
        for(String s : TIFFScannedImages)
                if (comparatorMagn.compare(TIFFScannedImages.get(0), s)
                        == 0)
                    TIFFScannedImagesAtLargestMagn.add(s);
                else
                    break;

          /* For each label, open the images */
        if (labels.size() == 0)
          labels.add("");
        for (String label : labels)
        {
              /* Find images with this label */
            ArrayList<String> TIFFScannedImagesAtLargestMagnWithThisLabel =
                new ArrayList<String>();
            for (String s : TIFFScannedImagesAtLargestMagn)
                if (s.endsWith(label + ".tif"))
                    TIFFScannedImagesAtLargestMagnWithThisLabel.add(s);

              /* Sort them according to z-shift: first, find their 
               * largest common prefix with two underscores (which 
               * should be ..._x<m>_z), then sort them according to the 
               * first number after this prefix */
            if (TIFFScannedImagesAtLargestMagnWithThisLabel.size() > 1)
            {
                FilenameWithMagnificationComparator comparatorZ =
                    new FilenameWithMagnificationComparator(
                        largestCommonPrefix(TIFFScannedImagesAtLargestMagn,
                            filenamePrefix.length(), 2));
                java.util.Collections.sort(TIFFScannedImagesAtLargestMagn, 
                    comparatorZ);
            }

              /* Open the images */
            ArrayList<ImagePlus> imageList = new ArrayList<ImagePlus>();
            for (String fn : TIFFScannedImagesAtLargestMagnWithThisLabel)
            {
                File extractFile = new File(fn);
                try
                {
                    IJ.showStatus("Opening " + extractFile.getName() + 
                        "...");
                    JAIReader reader = new JAIReader(extractFile);
                    ImagePlus extract = reader.readFirstImage();
                    if (extract != null)
                    {
                        if (ndpiFluorescence != null)
                            extract.setProperty("NDPIFluorescence",
                                ndpiFluorescence);
                        imageList.add(extract);
                    }
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                    String msg = "Error opening file: " + 
                        extractFile.getName() + ".\n\n";
                    msg += (ex.getMessage() == null) ? ex.toString() :
                        ex.getMessage();
                    IJ.showMessage(TITLE, msg);
                }
            }
              /* Show the images (combined into a stack if possible) */
            ImagePlus[] images =
                JAIReader.combineImagesIntoStack(imageList,
                    (new File(NDPIPath)).getName() + label);
            for (ImagePlus i : images)
                i.show();
        } /* for label */
    }

    /**
     * Ask the user to select a directory and parameters for the 
     * conversion, then convert all .ndpi files in the directory into 
     * TIFF files (and additional mosaics if requested)
     */
    public static void convertDirectoryFromGUI(String arg, String TITLE,
            boolean askForParameters)
    {
        String dirPath = null;

        if (arg != null && ! arg.equals(""))
            dirPath = arg;
        else
        { // Choose directory
            ij.io.DirectoryChooser dc = new ij.io.DirectoryChooser(
                "Please select directory where all .NDPI files should" +
                " be converted into TIFF / Mosaics.");
            dirPath = dc.getDirectory();
            if (dirPath == null)
                return;
        }

        File dir = new File(dirPath);
        if (! dir.isDirectory())
            return;
        File[] files = dir.listFiles();
        ArrayList<File> NDPIFiles = new ArrayList<File>();
        for (File f : files)
        {
            if (f.isDirectory())
                continue;
            if (f.getName().toLowerCase().endsWith(".ndpi"))
                NDPIFiles.add(f);
        }
        if (NDPIFiles.size() <= 0)
            return;

        ArrayList<String> args = new ArrayList<String>();
        args.add("-vv");
        args.add("-K");
        Custom_Extract_Dialog dialog = new Custom_Extract_Dialog();
        dialog.logMessagesFromNdpisplit = true;

        if (askForParameters)
        {
            dialog.run("");
            if (dialog.wasCanceled)
                return;
            if (dialog.makeMosaic >= 1)
                args.addAll(dialog.ndpisplitCustomArgs);
        }

        args.add("");
        String[] argsArray = args.toArray(new String[0]);
        for (int i = 0 ; i < NDPIFiles.size() ; i++)
        {
            File f = NDPIFiles.get(i);
            String[] res;
            argsArray[argsArray.length-1] = f.getPath();
            IJ.showStatus("Converting file " + (i+1) + "/" +
                NDPIFiles.size() + "...");
            try
            {
                res = NDPISplit.run(argsArray,
                    dialog.logMessagesFromNdpisplit);
            } catch (Exception ex)
            {
                ex.printStackTrace();
                String msg = "Error while running ndpisplit on: " +
                    f.getPath() + ".\n\n";
                msg += (ex.getMessage() == null) ? ex.toString() :
                    ex.getMessage();
                IJ.showMessage(TITLE, msg);
                return;
            }
        }
    }

    private static class FilenameWithMagnificationComparator
        implements Comparator<String>
    {
        private int lengthOfPrefix;

        public FilenameWithMagnificationComparator(String prefix)
        {
            this.lengthOfPrefix = prefix.length();
        }

        public int compare(String f1, String f2)
        {
            double d1, d2;
            try
            {
                d1 = java.text.NumberFormat.getInstance().parse(
                    f1.substring(lengthOfPrefix)).doubleValue();
                d2 = java.text.NumberFormat.getInstance().parse(
                    f2.substring(lengthOfPrefix)).doubleValue();
                return Double.compare(d2, d1);
            } catch (Exception ex)
            {
                /*System.err.println(ex.toString());*/
                return 0;
            }
        }
    }

      /* restrictedPrefix("a_b_c_d", 3, 1) returns "a_b_c" because it is 
       * the prefix of the given string that has, starting from position 
       * 3, at most 1 underscore. */
    private static String restrictedPrefix(String s,
        int startCountingUnderscoresAt, int maxUnderscores)
    {
        int numberOfAllowedSearches = maxUnderscores + 1;
        int pos = startCountingUnderscoresAt;
        do
        {
            pos = s.indexOf('_', pos) + 1;
            numberOfAllowedSearches--;
        }
        while (pos > 0 && numberOfAllowedSearches > 0);
        return pos > 0 ? s.substring(0, pos-1) : s;
    }

    private static String largestCommonPrefix(ArrayList<String> l,
        int startCountingUnderscoresAt, int maxUnderscores)
    {
        String prefix = restrictedPrefix(l.get(0),
                            startCountingUnderscoresAt, maxUnderscores);
        for(String s : l)
        {
            String rs = restrictedPrefix(s, startCountingUnderscoresAt, 
                            maxUnderscores);
            if (rs.startsWith(prefix))
                continue;
            while (prefix.length() > 0)
            {
                prefix = prefix.substring(0, prefix.length() - 1);
                if (rs.startsWith(prefix))
                    break;
            }
        }
        return prefix;
    }

    private static void parseListOfMagnifications(String s, ArrayList<Double> l)
    {
        int i = 0;

        if (s == null)
            return;
        while (i < s.length())
        {
            try
            {
                int j = s.indexOf('x', i);
                if (j < 0)
                    j = s.length() - 1;
                l.add(Double.parseDouble(s.substring(i,j)));
                i = s.indexOf(',', i) + 1;
                if (i <= 0)
                    return;
            } catch (NumberFormatException e) {
                return;
            }
        }
        return;
    }

    private static void parseListOfDimensions(String s, ArrayList<Dimension> l)
    {
        int i = 0;

        if (s == null)
            return;
        while (i < s.length())
        {
            try
            {
                int j = s.indexOf('x', i);
                if (j < 0)
                    j = s.length() - 1;
                int width = Integer.parseInt(s.substring(i,j));
                i = j + 1;
                j = s.indexOf(',', j);
                if (j < 0)
                    j = s.length() - 1;
                int height = Integer.parseInt(s.substring(i,j));
                l.add(new Dimension(width, height));
                i = j + 1;
                if (i >= s.length())
                    return;
            } catch (NumberFormatException e) {
                return;
            }
        }
        return;
    }

    private static void parseListOfIntegers(String s, ArrayList<Integer> l)
    {
        int i = 0;

        if (s == null)
            return;
        while (i < s.length())
        {
            try
            {
                int j = s.indexOf(',', i);
                if (j < 0)
                    j = s.length() - 1;
                l.add(Integer.parseInt(s.substring(i,j)));
                i = s.indexOf(',', i) + 1;
                if (i <= 0)
                    return;
            } catch (NumberFormatException e) {
                return;
            }
        }
        return;
    }

    private static String magnificationAsString(Double md)
    {
        int mi = md.intValue();
        return (md == mi) ?
            String.format("%d", mi) : String.format("%s", md);
    }

    private static class Custom_Extract_Dialog implements ij.plugin.PlugIn {
        static final String[]
            minimalAcceptedNdpisplitVersionsForBigTIFFWriting = { "1.7.1" };


        final String dialogTitle = "My custom NDPI extract dialog";
        final String[] splitImagesFormats = { "default",
            "TIFF with JPEG compression", "uncompressed TIFF",
            "TIFF with LZW compression" };
        final String[] mosaicPiecesFormats = { "TIFF with JPEG compression",
            "JPEG", "uncompressed TIFF", "TIFF with LZW compression" };
        final String[] conditionsMakeMosaic = { "never",
            "only for largest files", "always" };
        final String[] mosaicPiecesOverlapUnits = {"pixels", "%"};

        boolean wasCanceled = false;
        String label = "";
        int numberOfRois = 0;
        int splitImagesFormat = 0;
        boolean splitImagesBigTIFFRatherThanTIFF = false;
        int makeMosaic = 1;
        int mosaicPiecesFormat = 0;
        boolean requestJPEGQualityDifferentFromInput = false;
        long JPEGQuality = 75;
        double mosaicPiecesOverlap = 0;
        int mosaicPiecesOverlapUnit = 0;
        double mosaicPiecesSizeLimitInMiB = 1024;
        long mosaicPiecesWidthInPx = 0;
        long mosaicPiecesHeightInPx = 0;
        boolean logMessagesFromNdpisplit = false;
        boolean[] extractMagnifications = new boolean[0];
        boolean[] extractZOffsets = new boolean[0];
        boolean saveChoicesInIJPrefs = true;
        boolean openImagesAtLargestMagnificationAfterExtraction = true;
        ArrayList<Double> magnificationsToExtractInIJPrefs =
            new ArrayList<Double>();
        ArrayList<Integer> zOffsetsToExtractInIJPrefs =
            new ArrayList<Integer>();
        ArrayList<String> ndpisplitCustomArgs = new ArrayList<String>();
        String[] splitImagesFormatsWithComments = splitImagesFormats;

        public void run(String arg)
        {
            {
            splitImagesFormat = (int)
                ij.Prefs.get("ndpitools.splitImagesFormat",
                    splitImagesFormat);
            splitImagesBigTIFFRatherThanTIFF =
                ij.Prefs.get("ndpitools.splitImagesBigTIFFRatherThanTIFF",
                    splitImagesBigTIFFRatherThanTIFF);
            makeMosaic = (int) ij.Prefs.get("ndpitools.makeMosaic",
                makeMosaic);
            mosaicPiecesFormat = (int)
                ij.Prefs.get("ndpitools.mosaicPiecesFormat",
                    mosaicPiecesFormat);
            requestJPEGQualityDifferentFromInput =
                ij.Prefs.get("ndpitools.requestJPEGQualityDifferentFromInput",
                    requestJPEGQualityDifferentFromInput);
            JPEGQuality = (long) ij.Prefs.get("ndpitools.JPEGQuality",
                JPEGQuality);
            mosaicPiecesOverlap =
                ij.Prefs.get("ndpitools.mosaicPiecesOverlap",
                    mosaicPiecesOverlap);
            mosaicPiecesOverlapUnit = (int)
                ij.Prefs.get("ndpitools.mosaicPiecesOverlapUnit",
                    mosaicPiecesOverlapUnit);
            mosaicPiecesSizeLimitInMiB =
                ij.Prefs.get("ndpitools.mosaicPiecesSizeLimitInMiB",
                    mosaicPiecesSizeLimitInMiB);
            mosaicPiecesWidthInPx = (long)
                ij.Prefs.get("ndpitools.mosaicPiecesWidthInPx",
                    mosaicPiecesWidthInPx);
            mosaicPiecesHeightInPx = (long)
                ij.Prefs.get("ndpitools.mosaicPiecesHeightInPx",
                    mosaicPiecesHeightInPx);
            logMessagesFromNdpisplit =
                ij.Prefs.get("ndpitools.logMessagesFromNdpisplit",
                    logMessagesFromNdpisplit);

            for (String s : ij.Prefs.get("ndpitools.magnificationsToExtract",
                                "").split("\\s*,\\s*"))
                try
                {
                    magnificationsToExtractInIJPrefs.add(
                        Double.parseDouble(s));
                } catch (NumberFormatException ex)
                {
                }

            for (String s : ij.Prefs.get("ndpitools.zOffsetsToExtract", "")
                    .split("\\s*,\\s*"))
                try
                {
                    zOffsetsToExtractInIJPrefs.add(Integer.parseInt(s));
                } catch (NumberFormatException ex)
                {
                }
            }

            ij.gui.GenericDialog gd =
                new ij.gui.GenericDialog(dialogTitle);
            if (! label.equals(""))
                gd.addStringField("Label: ", label,
                    label.length() < 12 ? 12 : label.length());
            if (numberOfRois > 1)
                gd.addMessage("Char. # in the label will be replaced with the number of the selected region.");
            if (sizeOfLargestExtractedImage > 0)
                splitImagesFormatsWithComments[2] +=
                    " (" + sizeOfLargestExtractedImage/1048576*3 +
                    " MB @" + magnificationAsString(
                        magnificationOfLargestExtractedImage) +
                    "x)";
            gd.addChoice("Format_of_split_images: ",
                splitImagesFormatsWithComments,
                splitImagesFormatsWithComments[splitImagesFormat]);
            gd.addCheckbox("Store_split_images_in_BigTIFF_files_rather_than_TIFF",
                splitImagesBigTIFFRatherThanTIFF);
            gd.addChoice("Make_mosaic: ", conditionsMakeMosaic,
                conditionsMakeMosaic[makeMosaic]);
            gd.addChoice("Mosaic_pieces_format: ", mosaicPiecesFormats,
                mosaicPiecesFormats[mosaicPiecesFormat]);
            gd.addCheckbox("Request_JPEG_compression_quality_different_from_input: ",
                requestJPEGQualityDifferentFromInput);
            gd.addNumericField("Requested_JPEG_compression quality: ",
                JPEGQuality, 0, 3, "%");
            gd.addNumericField("Mosaic_pieces_overlap: ",
                mosaicPiecesOverlap, 6, 8, "pixels or %");
            gd.addChoice("Mosaic_pieces_overlap_unit: ",
                mosaicPiecesOverlapUnits,
                mosaicPiecesOverlapUnits[mosaicPiecesOverlapUnit]);
            gd.addNumericField("Size_limit_on_each_mosaic_piece: ",
                mosaicPiecesSizeLimitInMiB, 0, 5, "MiB (0 for no limit)");
            gd.addNumericField("Width_of_each_mosaic_piece_in_pixels: ",
                mosaicPiecesWidthInPx, 0, 5, "px (0 for no spec.)");
            gd.addNumericField("Height_of_each_mosaic_piece_in_pixels: ",
                mosaicPiecesHeightInPx, 0, 5, "px (0 for no spec.)");
            gd.addCheckbox("Log_messages_from_the_ndpisplit_program",
                logMessagesFromNdpisplit);
            gd.addCheckbox("Save_these_choices_in_ImageJ's_preferences_file",
                saveChoicesInIJPrefs);
            extractMagnifications = new boolean[availableMagnifications.size()];
            {
                int numberOfSelectedMagnifications = 0;
                for (int i = 0 ; i < availableMagnifications.size() ; i++)
                {
                    boolean selectThisOne =
                        magnificationsToExtractInIJPrefs.contains(
                            availableMagnifications.get(i));
                    extractMagnifications[i] = selectThisOne;
                    if (selectThisOne)
                        numberOfSelectedMagnifications++;
                }
                if (numberOfSelectedMagnifications == 0)
                    for (int i = 0 ; i < availableMagnifications.size() ; i++)
                    {
                        extractMagnifications[i] = true;
                    }
            }
            for (int i = 0 ; i < availableMagnifications.size() ; i++)
            {
                String s = "Extract_images_at_magnification_" +
                    magnificationAsString(availableMagnifications.get(i)) + 
                    "x";
                if (availableDimensions.size() > 0)
                {
                    Dimension d = availableDimensions.get(i);
                    s += " (dim. " + d.width + "x" + d.height + ")";
                }
                gd.addCheckbox(s, extractMagnifications[i]);
            }
            extractZOffsets = new boolean[availableZOffsets.size()];
            {
                int numberOfSelectedZOffsets = 0;
                for (int i = 0 ; i < availableZOffsets.size() ; i++)
                {
                    boolean selectThisOne =
                        zOffsetsToExtractInIJPrefs.contains(
                            availableZOffsets.get(i));
                    extractZOffsets[i] = selectThisOne;
                    if (selectThisOne)
                        numberOfSelectedZOffsets++;
                }
                if (numberOfSelectedZOffsets == 0)
                    for (int i = 0 ; i < availableZOffsets.size() ; i++)
                    {
                        extractZOffsets[i] = true;
                    }
            }
            for (int i = 0 ; i < availableZOffsets.size() ; i++)
            {
                gd.addCheckbox("Extract_images_with_z-offset_" +
                        availableZOffsets.get(i),
                    extractZOffsets[i]);
            }
            gd.addCheckbox("Open_images_at_largest_magnification_after_extraction",
                openImagesAtLargestMagnificationAfterExtraction);
            gd.showDialog();
            if (gd.wasCanceled())
            {
                wasCanceled = true;
                return;
            }
            if (! label.equals(""))
                label = gd.getNextString();
            splitImagesFormat = gd.getNextChoiceIndex();
            splitImagesBigTIFFRatherThanTIFF = gd.getNextBoolean();
            makeMosaic = gd.getNextChoiceIndex();
            mosaicPiecesFormat = gd.getNextChoiceIndex();
            requestJPEGQualityDifferentFromInput = gd.getNextBoolean();
            JPEGQuality = StrictMath.round(gd.getNextNumber());
            if (JPEGQuality < 0)
                JPEGQuality = 0;
            if (JPEGQuality > 100)
                JPEGQuality = 100;
            mosaicPiecesOverlap = gd.getNextNumber();
            mosaicPiecesOverlapUnit = gd.getNextChoiceIndex();
            mosaicPiecesSizeLimitInMiB = gd.getNextNumber();
            mosaicPiecesWidthInPx = StrictMath.round(gd.getNextNumber());
            if (mosaicPiecesWidthInPx < 0)
                mosaicPiecesWidthInPx = 0;
            mosaicPiecesHeightInPx = StrictMath.round(gd.getNextNumber());
            if (mosaicPiecesHeightInPx < 0)
                mosaicPiecesHeightInPx = 0;
            logMessagesFromNdpisplit = gd.getNextBoolean();
            saveChoicesInIJPrefs = gd.getNextBoolean();
            for (int i = 0 ; i < availableMagnifications.size() ; i++)
                extractMagnifications[i] = gd.getNextBoolean();
            for (int i = 0 ; i < availableZOffsets.size() ; i++)
                extractZOffsets[i] = gd.getNextBoolean();
            openImagesAtLargestMagnificationAfterExtraction =
                gd.getNextBoolean();

            if (saveChoicesInIJPrefs)
            {
                ij.Prefs.set("ndpitools.splitImagesFormat",
                    splitImagesFormat);
                ij.Prefs.set("ndpitools.splitImagesBigTIFFRatherThanTIFF",
                    splitImagesBigTIFFRatherThanTIFF);
                ij.Prefs.set("ndpitools.makeMosaic", makeMosaic);
                ij.Prefs.set("ndpitools.mosaicPiecesFormat",
                    mosaicPiecesFormat);
                ij.Prefs.set("ndpitools.requestJPEGQualityDifferentFromInput",
                    requestJPEGQualityDifferentFromInput);
                ij.Prefs.set("ndpitools.JPEGQuality", JPEGQuality);
                ij.Prefs.set("ndpitools.mosaicPiecesOverlap",
                    mosaicPiecesOverlap);
                ij.Prefs.set("ndpitools.mosaicPiecesOverlapUnit",
                    mosaicPiecesOverlapUnit);
                ij.Prefs.set("ndpitools.mosaicPiecesSizeLimitInMiB",
                    mosaicPiecesSizeLimitInMiB);
                ij.Prefs.set("ndpitools.mosaicPiecesWidthInPx",
                    mosaicPiecesWidthInPx);
                ij.Prefs.set("ndpitools.mosaicPiecesHeightInPx",
                    mosaicPiecesHeightInPx);
                ij.Prefs.set("ndpitools.logMessagesFromNdpisplit",
                    logMessagesFromNdpisplit);

                String magnificationsToExtractInIJPrefsAsString = "";
                for (int i = 0 ; i < availableMagnifications.size() ; i++)
                    if (extractMagnifications[i])
                        magnificationsToExtractInIJPrefsAsString +=
                            "," + availableMagnifications.get(i);
                ij.Prefs.set("ndpitools.magnificationsToExtract",
                    magnificationsToExtractInIJPrefsAsString);

                String zOffsetsToExtractInIJPrefsAsString = "";
                for (int i = 0 ; i < availableZOffsets.size() ; i++)
                    if (extractZOffsets[i])
                        zOffsetsToExtractInIJPrefsAsString +=
                            "," + availableZOffsets.get(i);
                ij.Prefs.set("ndpitools.zOffsetsToExtract",
                    zOffsetsToExtractInIJPrefsAsString);

                ij.Prefs.set("ndpitools.openImagesAtLargestMagnificationAfterExtraction",
                    openImagesAtLargestMagnificationAfterExtraction);
            }

            if (splitImagesBigTIFFRatherThanTIFF)
            {
                String acceptableNdpisplitVersion =
                    NDPISplit.getAcceptableVersion(
                        minimalAcceptedNdpisplitVersionsForBigTIFFWriting, 
                        dialogTitle, "writing BigTIFF files");
                if (acceptableNdpisplitVersion == null)
                {
                    wasCanceled = true;
                    return;
                }
                ndpisplitCustomArgs.add("-8");
            }

            {
                String compressionFormatArg = "-c";

                if (splitImagesFormat > 0)
                {
                    long maxMemory =
                        java.lang.Runtime.getRuntime().maxMemory();
                    if (maxMemory != Long.MAX_VALUE)
                        compressionFormatArg += maxMemory/1048576;
                    switch (splitImagesFormat)
                    {
                        case 1: compressionFormatArg += "j"; break;
                        case 2: compressionFormatArg += "n"; break;
                        case 3: compressionFormatArg += "l"; break;
                        default: compressionFormatArg = "";
                    }
                    ndpisplitCustomArgs.add(compressionFormatArg);
                }
            }

            if (makeMosaic > 0)
            {
                String mosaicArg = "-" + (makeMosaic >= 2 ? "M" : "m") +
                    mosaicPiecesSizeLimitInMiB;
                if (mosaicPiecesFormat <= 1)
                {
                    mosaicArg +=
                        (mosaicPiecesFormat == 0 ? "j" :
                        (mosaicPiecesFormat == 1 ? "J" : ""));
                    if (requestJPEGQualityDifferentFromInput)
                        mosaicArg += JPEGQuality;
                } else
                    mosaicArg +=
                        (mosaicPiecesFormat == 2 ? "n" :
                        (mosaicPiecesFormat == 3 ? "l" : ""));
                ndpisplitCustomArgs.add(mosaicArg);
            }

            ndpisplitCustomArgs.add( "-o" + mosaicPiecesOverlap +
                (mosaicPiecesOverlapUnit == 1 ? "%" : "") );

            if (mosaicPiecesWidthInPx != 0 ||
                mosaicPiecesHeightInPx !=0)
                ndpisplitCustomArgs.add("-g" + mosaicPiecesWidthInPx +
                    "x" + mosaicPiecesHeightInPx);
        }
    }

    private static class Confirm_Extraction_Dialog implements ij.plugin.PlugIn {

        boolean wasCanceled = false;

        public void run(String arg)
        {
            ij.gui.GenericDialog gd =
                new ij.gui.GenericDialog("NDPI -- Confirm extraction ");

            gd.addMessage("Warning: the extracted image(s) at " +
                "largest magnification will overcome the dimension " +
                "limit of 65536 pixels of ImageJ 1. Continue with " +
                "extraction?");
            gd.showDialog();
            wasCanceled = gd.wasCanceled();
        }
    }
}
