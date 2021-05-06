/***
 * JAIReader Copyright (C) 2012 C. Deroulers
 * after JAIReader from ij-ImageIO Copyright (C) 2002-2004 Jarek Sacha
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
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
 * Latest release available at http://sourceforge.net/projects/ij-plugins/
 */
package fr.cirad.image.ndpilegacy;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.io.TiffDecoder;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import com.sun.media.jai.codec.*;
import com.sun.media.jai.codecimpl.*;
import com.sun.media.jai.codecimpl.util.DataBufferDouble;
import com.sun.media.jai.codecimpl.util.DataBufferFloat;
import com.sun.media.jai.codecimpl.util.FloatDoubleColorModel;

import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

/**
 * Reads image files using JAI image I/O codec
 * (http://developer.java.sun.com/developer/sampsource/jai/) and 
 * converts them to Image/J representation.
 * 
 * @author C. Deroulers, after Jarek Sacha
 * @version $Revision: 1.4.2 $
 */
public class JAIReader {
	private FileSeekableStream fss;
    private ImageDecoder decoder = null;
    private String decoderName = null;
    private File file = null;

    /**
     * Opens the image file and creates image decoder.
     * 
     * @param file Image file.
     * @throws Exception
     */
    public JAIReader(File file) throws Exception
    {
        open(file);
    }

    /**
     * Creates image decoder to read the image file.
     * 
     * @param file Image file name.
     * @throws Exception Description of Exception
     */
    private void open(File file) throws Exception
    {
        this.file = file;

        // Find matching decoders
        fss = new FileSeekableStream(file);
        String[] decoders = ImageCodec.getDecoderNames(fss);
        if (decoders == null || decoders.length == 0) {
            throw new Exception("Unsupported file format. "
                    + "Cannot find decoder capable of reading: " +
                    file.getName());
        }

        this.decoderName = decoders[0];

        // Create decoder
        this.decoder = ImageCodec.createImageDecoder(decoderName, fss, null);
    }
    
    public void close() {
    	try {
			this.fss.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Reads the first image of the file.
     * If reading from a TIFF file, image resolution is decoded but not
     * ImageJ's description string containing calibration information.
     */
    public ImagePlus readFirstImage()
            throws
            UnsupportedImageFileFormatException,
            UnsupportedImageModelException,
            IOException, Exception
    {
        ImagePlus im = read(0);
        return im;
    }

    /**
     * Reads all images in the file using registered codecs.
     * If reading from a TIFF file, image resolution is decoded but not
     * ImageJ's description string containing calibration information.
     * 
     * @return Array of images contained in the file.
     * @throws Exception when unable to read image from the specified file.
     */
    public ImagePlus[] readAllImages() throws Exception
    {
        // Get number of sub images
        int nbPages = getNumPages();
        if (nbPages < 1) {
            throw new Exception("Image decoding problem. "
                    + "Image file has less then 1 page. Nothing to decode.");
        }

        int pageIndex[] = new int[nbPages];
        for (int i = 0; i < nbPages; ++i)
            pageIndex[i] = i;

        // Iterate through pages
        IJ.showProgress(0);
        ArrayList<ImagePlus> imageList = new ArrayList<ImagePlus>();
        for (int i = 0; i < pageIndex.length; ++i) {
            if (pageIndex[i] != 0)
                IJ.showStatus("Reading page " + pageIndex[i]);

            imageList.add(read(pageIndex[i]));
            IJ.showProgress((double) (i + 1) / pageIndex.length);
        }
        IJ.showProgress(1);

        return combineImagesIntoStack(imageList, file.getName());
    }


        @SuppressWarnings("serial")
    private static class CantCombine extends Exception
    {
        public CantCombine()
        {
        }
    }


    /**
     * Attempts to combine images into a single stack. Images can be
     * combined into a stack if all of them are single slice images of
     * the same type and dimensions.
     * 
     * @param imageList ArrayList of images.
     * @param titleOfStack title to give to the stack, if combination 
     * is successful.
     * @return Array of images: either an Array with a single element, 
     * the stack of images, if combination was successful, or Array with 
     * all images, if it wasn't.
     */
    public static ImagePlus[] combineImagesIntoStack(
        ArrayList<ImagePlus> imageList, String titleOfStack)
    {
        try
        {
            if (imageList == null || imageList.size() == 0)
                throw new CantCombine();

            ImagePlus im0 = imageList.get(0);
            if (im0.getStackSize() != 1)
                throw new CantCombine();

            int fileType = im0.getFileInfo().fileType;
            int w = im0.getWidth();
            int h = im0.getHeight();
            ImageStack stack = im0.getStack();
            for (int i = 1; i < imageList.size(); ++i)
            {
                ImagePlus im = imageList.get(i);
                if (im.getStackSize() != 1)
                    throw new CantCombine();
                if (fileType == im.getFileInfo().fileType
                        && w == im.getWidth() && h == im.getHeight())
                    stack.addSlice(null, im.getProcessor().getPixels());
                else
                    throw new CantCombine();
            }

            im0.setStack(titleOfStack, stack);
            return new ImagePlus[] {im0};
        }
        catch (CantCombine ex)
        {
            return imageList.toArray(new ImagePlus[0]);
        }
    }

    public static ImageProcessor createProcessor(int w, int h, DataBuffer buffer,
            ColorModel cm) throws Exception {
         
         if (buffer.getOffset() != 0) {
            throw new Exception("Expecting BufferData with no offset.");
         }
         switch (buffer.getDataType()) {
            case DataBuffer.TYPE_BYTE:
               return new ByteProcessor(w, h, ((DataBufferByte) buffer).getData(), cm);
            case DataBuffer.TYPE_USHORT:
               return new ShortProcessor(w, h, ((DataBufferUShort) buffer).getData(),
                     cm);
            case DataBuffer.TYPE_SHORT:
               short[] pixels = ((DataBufferShort) buffer).getData();
               for (int i = 0; i < pixels.length; ++i) {
                  pixels[i] = (short) (pixels[i] + 32768);
               }
               return new ShortProcessor(w, h, pixels, cm);
            case DataBuffer.TYPE_INT:
               return new FloatProcessor(w, h, ((DataBufferInt) buffer).getData());
            case DataBuffer.TYPE_FLOAT: {
               DataBufferFloat dbFloat = (DataBufferFloat) buffer;
               return new FloatProcessor(w, h, dbFloat.getData(), cm);
            }
            case DataBuffer.TYPE_DOUBLE:
               return new FloatProcessor(w, h, ((DataBufferDouble) buffer).getData());
            case DataBuffer.TYPE_UNDEFINED:
               throw new Exception("Pixel type is undefined.");
            default:
               throw new Exception("Unrecognized DataBuffer data type");
         }
      }
      
    
    
    public static ImagePlus makeStackFromRenderedImage(String name, RenderedImage[] rImage) {
        Object pixels = null;
        ImagePlus imp = null;
        if (name == null) {
           name = "None";
        }
       ImageStack stack =         new ImageStack(rImage[0].getWidth(), rImage[0].getHeight());
       for (int i = 0; i < (rImage.length); i++) {
          DataBuffer dBuff = rImage[i].getData().getDataBuffer();
          ColorModel cm = rImage[i].getColorModel();
          try {
             ImageProcessor ip =
                   createProcessor(rImage[i].getWidth(), rImage[i].getHeight(),
                   dBuff, cm);
             stack.addSlice(String.valueOf(i - 1), ip);
          } catch (Exception ex) {
             ex.printStackTrace();
          }
          //stack.addSlice();
       }
       if (stack == null) {
          return null;
       }
       if (stack.getSize() == 0) {
          return null;
       }
       imp = new ImagePlus(name, stack);
        return imp;
     }
     

    
    
    
    /**
     * @param pageNb index of image to read in the file
     * @return image
     * @throws Exception
     */
    private ImagePlus read(int pageNb) throws Exception
    {
        RenderedImage ri = null;
        try {
            ri = decoder.decodeAsRenderedImage(pageNb);
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = ex.getMessage();
            if (msg == null || msg.trim().length() < 1) {
                msg = "Error decoding rendered image.";
            }
            throw new Exception(msg);
        }
        //if(true)return makeStackFromRenderedImage(file.getName() + " [" + (pageNb + 1) + "/" + getNumPages() + "]", new RenderedImage[] {ri});
        //ImageIO.write(ri, ".jpg", new File("/home/rfernandez/Bureau/temp.jpg"));
        WritableRaster wr = ImagePlusCreator.forceTileUpdate(ri);

        ImagePlus im = ImagePlusCreator.create(wr, ri.getColorModel());
        im.setTitle(file.getName() + " [" + (pageNb + 1) + "/" + getNumPages() + "]");

        if (im.getType() == ImagePlus.COLOR_RGB) {
            /* Convert RGB to gray if all bands are equal */
            Opener.convertGrayJpegTo8Bits(im);
        }

        /* Extract TIFF tags */
        if (ri instanceof TIFFImage)
        {
            TIFFImage ti = (TIFFImage) ri;
            try
            {
                Object o = ti.getProperty("tiff_directory");
                if (o instanceof TIFFDirectory)
                {
                    TIFFDirectory dir = (TIFFDirectory) o;

                    /* ImageJ description string is ignored */

                    Calibration c = im.getCalibration();
                    if (c == null)
                        c = new Calibration(im);

                    /* X resolution */
                    TIFFField xResField = dir.getField(TIFFImageDecoder.TIFF_X_RESOLUTION);
                    if (xResField != null)
                    {
                        double xRes = xResField.getAsDouble(0);
                        if (xRes != 0)
                            c.pixelWidth = 1 / xRes;
                    }

                    /* Y resolution */
                    TIFFField yResField = dir.getField(TIFFImageDecoder.TIFF_Y_RESOLUTION);
                    if (yResField != null)
                    {
                        double yRes = yResField.getAsDouble(0);
                        if (yRes != 0)
                                c.pixelHeight = 1 / yRes;
                    }

                    /* Resolution unit */
                    TIFFField resolutionUnitField =
                        dir.getField(TIFFImageDecoder.TIFF_RESOLUTION_UNIT);
                    if (resolutionUnitField != null)
                    {
                        int resolutionUnit = resolutionUnitField.getAsInt(0);
                        if (resolutionUnit == 1 && c.getUnit() == null)
                            // no meaningful units
                            c.setUnit(" ");
                        else if (resolutionUnit == 2)
                            c.setUnit("inch");
                        else if (resolutionUnit == 3)
                          c.setUnit("cm");
                    }

                    im.setCalibration(c);
                }
            } catch (NegativeArraySizeException ex) {
                /* my be thrown by ti.getPrivateIFD(8) */
                ex.printStackTrace();
            }
        }

        return im;
    }

    /**
     * @return The NumPages value
     * @throws IOException
     */
    private int getNumPages() throws IOException {
        return decoder.getNumPages();
    }
}
