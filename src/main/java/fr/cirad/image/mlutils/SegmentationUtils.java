package fr.cirad.image.mlutils;

import java.awt.Color;
import java.awt.List;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import ij.plugin.ChannelSplitter;

import org.json.JSONArray;
import org.json.JSONObject;


import fr.cirad.image.common.Timer;
import fr.cirad.image.common.TransformUtils;
import fr.cirad.image.common.VitiDialogs;
import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.common.TransformUtils.VolumeComparator;
import fr.cirad.image.hyperweka.HyperWekaSegmentation;
import fr.cirad.image.registration.ItkTransform;
import fr.cirad.image.sorghobff.ScriptMathieu;
import fr.cirad.image.sorghobff.VesselSegmentation;
import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.Scaler;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import inra.ijpb.geometry.Ellipse;
import inra.ijpb.measure.region2d.InertiaEllipse;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.watershed.MarkerControlledWatershedTransform2D;
import mcib3d.image3d.processing.MaximaFinder;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;

import fr.cirad.image.common.VitimageUtils;

public class SegmentationUtils {
    
	
	public static void testReadCsv() {
		String[][]tab=VitimageUtils.readStringTabFromCsv("/home/rfernandez/Téléchargements/Recap_echantillons.xlsx - Image_inventory_2019_T1_WW.csv");
		for(int i=2;i<tab.length;i++) {//From i=2 because two first lines are not images
			//For each line
			System.out.println("Line "+i+" =");
			for(int j=0;j<tab[i].length;j++) {
				System.out.print(tab[i][j]+" , ");//Display one element, without \n (endline)
			}
			System.out.println();//End of this line
			System.out.print("Is rejected ? ");
			if(tab[i][11].equals("")) {
				System.out.println("No");
			}
			else System.out.println("Yes");
			System.out.print("Total des defauts = ");
			System.out.println(  (Integer.parseInt(tab[i][7])+Integer.parseInt(tab[i][8])+Integer.parseInt(tab[i][9]) )  );
			System.out.println();//End of this line
			System.out.println();//End of this line
		}
	}
	
	
	
	
	
	/** Helpers for visualization of Roi and mask over source images --------------------------------------------*/
	public static ImagePlus visualizeMaskEffectOnSourceData(ImagePlus imgSourceRGB,ImagePlus mask,int mode0VBOnly_1Enhance_2GreysOther_3greenout) {
		ImagePlus[]imgSource=VitimageUtils.channelSplitter(imgSourceRGB);
	

		if(mode0VBOnly_1Enhance_2GreysOther_3greenout==0) {
			ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
			imgMask=VitimageUtils.makeOperationOnOneImage(imgMask, 2, 0.5, false);
			imgMask=VitimageUtils.makeOperationOnOneImage(imgMask, 1, 0.5, false);
			for(int can=0;can<3;can++)imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
			return VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);				
		}
		else if(mode0VBOnly_1Enhance_2GreysOther_3greenout==2) {
			ImagePlus[]ret=new ImagePlus[3];
			ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
			IJ.run(imgSourceRGBGrey,"8-bit","");
			IJ.run(imgSourceRGBGrey,"RGB Color","");
			ImagePlus[]imgSourceGreys=VitimageUtils.channelSplitter(imgSourceRGBGrey);
			ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
			ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(imgMask);
			imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
			imgMaskGreys=VitimageUtils.makeOperationOnOneImage(imgMaskGreys, 2, 1.7, true);
			for(int can=0;can<3;can++) {
				imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
				imgSourceGreys[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceGreys[can], imgMaskGreys, 2, false);
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can],imgSourceGreys[can],1,false);
			}
//			ImagePlus deb1=VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);
//			ImagePlus deb2=VitimageUtils.compositeRGBByte(imgSourceGreys[0], imgSourceGreys[1], imgSourceGreys[2],1,1,1);
			return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
		}
		
		else if(mode0VBOnly_1Enhance_2GreysOther_3greenout==3) {
			ImagePlus[]ret=new ImagePlus[3];
			ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
			IJ.run(imgSourceRGBGrey,"8-bit","");
			IJ.run(imgSourceRGBGrey,"RGB Color","");
			ImagePlus[]imgSourceGreys=VitimageUtils.channelSplitter(imgSourceRGBGrey);
			ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
			IJ.run(imgMask,"32-bit","");
			ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(imgMask);
			imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
			IJ.run(imgMaskGreys,"32-bit","");
			imgMaskGreys=VitimageUtils.makeOperationOnOneImage(imgMaskGreys, 2, 1.7, true);
			for(int can=0;can<3;can++) {
				imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
				imgSource[can]=VitimageUtils.makeOperationOnOneImage(imgSource[can], 2, 1.2, true);
				imgSourceGreys[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceGreys[can], imgMaskGreys, 2, false);
				imgSourceGreys[can]=VitimageUtils.makeOperationOnOneImage(imgSourceGreys[can], 2, (can==1) ? 0.85 : 0.7, true);
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can],imgSourceGreys[can],1,false);
			}
//			ImagePlus deb1=VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);
//			ImagePlus deb2=VitimageUtils.compositeRGBByte(imgSourceGreys[0], imgSourceGreys[1], imgSourceGreys[2],1,1,1);
			return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
		}
		else  {
			ImagePlus imgSourceRGB2=imgSourceRGB.duplicate();
			ImagePlus[]imgSource2=VitimageUtils.channelSplitter(imgSourceRGB2);
			ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
			IJ.run(imgMask,"32-bit","");
			imgMask=VitimageUtils.makeOperationOnOneImage(imgMask, 2, 1.4, false);

			ImagePlus imgMask2=VitimageUtils.invertBinaryMask(mask);
			imgMask2=getBinaryMaskUnary(imgMask2, 0.5);
			IJ.run(imgMask2,"32-bit","");
			imgMask2=VitimageUtils.makeOperationOnOneImage(imgMask2, 2, 0.4, false);

			for(int can=0;can<3;can++) {
				imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
				imgSource2[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource2[can], imgMask2, 2, false);

				imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can],imgSource2[can],1,false);
			}
			return VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);				
		}
	}

	
	
    public static FilenameFilter getFileNameFilterToExcludeCsvAndJsonFiles () {		
    	return new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return ( (!name.contains(".csv") &&  (!name.contains(".json")) ) );
			}
    	};
	}

	
	
    /** 
     * Inputs : 
     * inputDirSource is a directory with images and Csv. The images are a series of source image , to be processed, with names ******.tif  of level 1
     * inputDirSegmentation is the corresponding directory : for each image ****.tif in inputSource, there is a corresponding image Segmentation_******.tif of level 1 subsampled 8
     * outputDir is an empty directory (verified at running time)
     * 
     * Outputs :
     * For each image of inputDirSource, one directory containing one image (boxSize X boxSize) per vessel and a Csv summary
     * */
    public static void extractVessels(String inputDirSource, String dirsub,String inputDirSegmentation,String inputDirVoronoi, String outputDir,int resampleFactor) {    	
    	int boxSize = 200 ;

    	//Loop over images
    	String[] imgName=new File(dirsub).list(getFileNameFilterToExcludeCsvAndJsonFiles ());
    	for(int indImg=0;indImg<imgName.length;indImg++) { 
    		System.out.println("In "+inputDirSource);
    		System.out.println(imgName[indImg]);
    		System.out.println("Processing "+(indImg+1)+"/"+(imgName.length)+" : "+imgName[indImg]);
	    	ImagePlus imgSource = IJ.openImage(new File(inputDirSource,imgName[indImg]).getAbsolutePath());

	    	// Load segmented image and transform in ROI[]
	    	ImagePlus imgSeg = IJ.openImage(new File(inputDirSegmentation,imgName[indImg]).getAbsolutePath());    	
	    	System.out.println(new File(inputDirSegmentation,imgName[indImg]).getAbsolutePath());
	    	Roi[] vaisseauxRoiBase = SegmentationUtils.segmentationToRoi(imgSeg);
	    	String basename=VitimageUtils.withoutExtension(imgName[indImg]);
	    	new File(outputDir,basename).mkdirs();	    	
	    	String dirImgName=new File(outputDir,basename).getAbsolutePath();
//	    	new File(dirImgName,"Source").mkdirs(); CONVERT Source/all source.tif
//	    	new File(dirImgName,"Source_sub").mkdirs();CONVERT Source_sub/all source_sub.tif
//	    	new File(dirImgName,"Segmentation_slice").mkdirs();CONVERT Segmentation_slice/all seg_slice.tif
//	    	new File(dirImgName,"Voronoi_slice").mkdirs();CONVERT Voronoi_slice/all voronoi_slice.tif
//	    	new File(dirImgName,"Segmentation_vessel").mkdirs();
	    	ImagePlus imgVor = IJ.openImage(new File(inputDirVoronoi,imgName[indImg]).getAbsolutePath());    	
	    	ImagePlus segTemp=imgSeg.duplicate();
	    	IJ.run(segTemp, "Gaussian Blur...", "sigma=1");
	    	
	    	ImagePlus segHighRes=resize(segTemp, imgSeg.getWidth()*resampleFactor,  imgSeg.getHeight()*resampleFactor, 1);
	    	segHighRes=VitimageUtils.thresholdImage(segHighRes, 127.5, 256);
	    	ImagePlus vorHighRes=resizeNearest(imgVor, imgSeg.getWidth()*resampleFactor,  imgSeg.getHeight()*resampleFactor, 1);
	    	vorHighRes=VitimageUtils.thresholdImage(vorHighRes, 127.5, 256);
	    	
	    	// Creating an arraylist to store the bounding boxes coordinates and later save them in a .csv
	    	ArrayList<String[]> csvCoordinates = new ArrayList<String[]>();
	    	String[] amorce = {"ImgName","Vaisseau#", "XCenter", "YCenter", "ExtractHalfSizeX", "ExtractHalfSizeY", "PathToOriginalImage"};
	    	csvCoordinates.add(amorce);
	    	ImagePlus[]stackSourceExtracted  =new ImagePlus[vaisseauxRoiBase.length];
	    	ImagePlus[]stackSourceSub  =new ImagePlus[vaisseauxRoiBase.length];
	    	ImagePlus[]stackSegExtracted  =new ImagePlus[vaisseauxRoiBase.length];
	    	ImagePlus[]stackVorExtracted  =new ImagePlus[vaisseauxRoiBase.length];
	    	    	
	    	for(int i=0;i<vaisseauxRoiBase.length;i++) {
	    		if(i%10==0)System.out.print("  "+i);
	    		// Extract centroids information and resample it for source image
	    		double[] centroid = vaisseauxRoiBase[i].getContourCentroid();		
	    		int centroidXSource = (int) Math.round(centroid[0]*resampleFactor);
	    		int centroidYSource = (int) Math.round(centroid[1]*resampleFactor);
	    		//verify box enters in the image
	    		int x0=centroidXSource;
	    		int y0=centroidYSource;
	    		if(x0<boxSize/2)x0=boxSize/2;
	    		if(y0<boxSize/2)y0=boxSize/2;
	    		if(x0>=(imgSource.getWidth()-boxSize/2))x0=imgSource.getWidth()-boxSize/2;
	    		if(y0>=(imgSource.getHeight()-boxSize/2))y0=imgSource.getHeight()-boxSize/2;

	    		int x0Sub=x0/resampleFactor;
	    		int y0Sub=y0/resampleFactor;

	    		
	    		//Extract source high rest extract
	    		Roi areaRoi = IJ.Roi(x0-(boxSize/2), y0-(boxSize/2), boxSize, boxSize);
	    		imgSource.setRoi(areaRoi);
	    		ImagePlus sourceExtracted = imgSource.crop();
	    		ImagePlus sourceSub=resize(sourceExtracted, 100, 100, 1);
	    		segHighRes.setRoi(areaRoi);
	    		ImagePlus segExtracted = segHighRes.crop();
	    		segExtracted=resizeNearest(segExtracted, 100, 100, 1);
	    		segExtracted=VitimageUtils.thresholdByteImage(segExtracted, 127, 256);
	    		vorHighRes.setRoi(areaRoi);
	    		ImagePlus vorExtracted = vorHighRes.crop();
	    		vorExtracted=resizeNearest(vorExtracted, 100, 100, 1);
	    		vorExtracted=VitimageUtils.thresholdByteImage(vorExtracted, 127, 256);

	    		//Save the results
	    		stackSourceExtracted[i]=sourceExtracted;
	    		stackSourceSub[i]=sourceSub;
	    		stackSegExtracted[i]=segExtracted;
	    		stackVorExtracted[i]=vorExtracted;
	    	
	    		//Add a line to the CSV file of this image
	    		csvCoordinates.add( new String[]{
	    				basename,
	    				""+(i+1),""+(x0),
	    				""+ (y0),""+ boxSize/2, ""+boxSize/2,new File(inputDirSource,imgName[indImg]).getAbsolutePath(),
				} );
	    	}
    		IJ.saveAsTiff(VitimageUtils.slicesToStack(stackSourceExtracted),new File(dirImgName,"source.tif").getAbsolutePath());	   
    		IJ.saveAsTiff(VitimageUtils.slicesToStack(stackSourceSub),new File(dirImgName,"source_sub.tif").getAbsolutePath());	   
    		IJ.saveAsTiff(VitimageUtils.slicesToStack(stackSegExtracted),new File(dirImgName,"seg_slice.tif").getAbsolutePath());	   
    		IJ.saveAsTiff(VitimageUtils.slicesToStack(stackVorExtracted),new File(dirImgName,"voronoi_slice.tif").getAbsolutePath());	   
	    	VitimageUtils.writeStringTabInCsv(csvCoordinates.toArray( new String[csvCoordinates.size()][csvCoordinates.get(0).length]), new File(dirImgName,"Extracts_descriptor.csv").getAbsolutePath());
    	}    			
    }

	
	
    
	public static double[]stringTabToDoubleTab(String[]str){
	double []ret=new double[str.length];
		for(int i=0;i<ret.length;i++) {
			  if(str[i].length()==0)ret[i]=0;
			  else ret[i]=Double.parseDouble(str[i].replace(" ", ""));
		}
		return ret;
	}

	
	public static ImagePlus visualizeBiMaskEffectOnSourceData(ImagePlus imgSourceRGB,ImagePlus mask,ImagePlus mask2,int can0R_1G_2B_3All) {
		ImagePlus blackOutMask=VitimageUtils.getBinaryMaskUnary(mask2, 0.5);
		blackOutMask=VitimageUtils.invertBinaryMask(blackOutMask);
		ImagePlus[]imgSource=VitimageUtils.splitRGBStackHeadLess(imgSourceRGB);
		ImagePlus[]ret=new ImagePlus[3];
		ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
		IJ.run(imgSourceRGBGrey,"8-bit","");
		IJ.run(imgSourceRGBGrey,"RGB Color","");
		ImagePlus[]imgSourceGreys=VitimageUtils.splitRGBStackHeadLess(imgSourceRGBGrey);
		ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
		IJ.run(imgMask,"32-bit","");
		ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(imgMask);
		imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
		IJ.run(imgMaskGreys,"32-bit","");
		imgMaskGreys=VitimageUtils.makeOperationOnOneImage(imgMaskGreys, 2, 1.7, true);
		for(int can=0;can<3;can++) {
			imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
			imgSource[can]=VitimageUtils.makeOperationOnOneImage(imgSource[can], 2, 1.2, true);
			imgSourceGreys[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceGreys[can], imgMaskGreys, 2, false);
			imgSourceGreys[can]=VitimageUtils.makeOperationOnOneImage(imgSourceGreys[can], 2, (can==1) ? 0.85 : 0.7, true);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can],imgSourceGreys[can],1,false);
			if((can0R_1G_2B_3All==3) || (can!=can0R_1G_2B_3All))ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can],blackOutMask, 2,false);
		}
		return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
	}

	
	
	public static ImagePlus visualizeMaskDifferenceOnSourceData(ImagePlus imgSourceRGB,ImagePlus maskRef,ImagePlus maskVal) {
		ImagePlus[]imgSourceRef=VitimageUtils.channelSplitter(imgSourceRGB);
		ImagePlus[]imgSourceVal=VitimageUtils.channelSplitter(imgSourceRGB);
		ImagePlus[]imgSourceBoth=VitimageUtils.channelSplitter(imgSourceRGB);
		ImagePlus[]ret=new ImagePlus[3];
		ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
		IJ.run(imgSourceRGBGrey,"8-bit","");
		IJ.run(imgSourceRGBGrey,"RGB Color","");
		ImagePlus[]imgSourceGreys=VitimageUtils.channelSplitter(imgSourceRGBGrey);

		ImagePlus mRefAndVal=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 2);
		ImagePlus imgMaskRefAndVal=getBinaryMaskUnary(mRefAndVal, 0.5);
		
		ImagePlus mRefOnly=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 4);
		ImagePlus imgMaskRefOnly=getBinaryMaskUnary(mRefOnly, 0.5);

		ImagePlus mValOnly=VitimageUtils.binaryOperationBetweenTwoImages(maskVal, maskRef, 4);
		ImagePlus imgMaskValOnly=getBinaryMaskUnary(mValOnly, 0.5);

		ImagePlus mRefOrVal=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 1);			
		ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(mRefOrVal);
		imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
		
		//la
		for(int can=0;can<3;can++) {
			imgSourceBoth[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceBoth[can], imgMaskRefAndVal, 2, false);
			imgSourceBoth[can]=VitimageUtils.makeOperationOnOneImage(imgSourceBoth[can], 2, 1.4, true);
			
			imgSourceGreys[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceGreys[can], imgMaskGreys, 2, false);
			imgSourceGreys[can]=VitimageUtils.makeOperationOnOneImage(imgSourceGreys[can], 2, (can==1) ? 0.5 : 0.5, true);

			imgSourceRef[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceRef[can], imgMaskRefOnly, 2, false);
			imgSourceRef[can]=VitimageUtils.makeOperationOnOneImage(imgSourceRef[can], 2, (can!=2) ? 0.7 : 1, true);
			imgSourceRef[can]=VitimageUtils.makeOperationOnOneImage(imgSourceRef[can], 1, (can!=2) ? 160 : 0, true);
			imgSourceRef[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceRef[can], imgMaskRefOnly, 2, false);

			imgSourceVal[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceVal[can], imgMaskValOnly, 2, false);
			imgSourceVal[can]=VitimageUtils.makeOperationOnOneImage(imgSourceVal[can], 2, (can==1) ? 0.7 : 1, true);
			imgSourceVal[can]=VitimageUtils.makeOperationOnOneImage(imgSourceVal[can], 1, (can==1) ? 160 : 0, true);
			imgSourceVal[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceVal[can], imgMaskValOnly, 2, false);

			
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceBoth[can],imgSourceGreys[can],1,false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can],imgSourceRef[can],1,false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can],imgSourceVal[can],1,false);
		}
		return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
	}

	public static ImagePlus getSizeMap(ImagePlus img,int threshLow,int step,int nCat) {

		ImagePlus imgRet=VitimageUtils.nullImage(img);
		IJ.run(imgRet,"8-bit","");
		imgRet.setDisplayRange(0, nCat*step);
		IJ.run(imgRet,"Fire","");
		for(int z=0;z<imgRet.getNSlices();z++) {
			ImagePlus temp=new Duplicator().run(img,1,1,z+1,z+1,1,1);
			IJ.run(temp,"8-bit","");
			Roi[]roiTab=segmentationToRoi(temp);
			ImageProcessor ip=imgRet.getStack().getProcessor(z+1);
			for(Roi r : roiTab) {
				int index=(int)Math.floor(getRoiSurface(r))/step+1;
				if(index>nCat)index=nCat;
				ip.setValue(index*step);
				ip.fill(r);
			}
			imgRet.getStack().setProcessor(ip, z+1);
		}
		//VitimageUtils.showWithParams(imgRet,img.getTitle()+"_size_map",1,0,nCat*step);
/*			imgRet.show();
		imgRet.setDisplayRange(0, nCat*step);
		imgRet.updateAndDraw();
		IJ.run(imgRet,"Fire","");
		IJ.run("Brightness/Contrast...");
		selectWindow("B&C");
		*/
		//if(!show)imgRet.hide();
		return imgRet;
	}
           

	


	/** Comparison of segmentations and computation of similarity scores --------------------------------------------*/    
    public static double IOU(ImagePlus imgRef,ImagePlus imgVal) {
    	ImagePlus ref=VitimageUtils.getBinaryMask(imgRef, 0.5);
    	ImagePlus val=VitimageUtils.getBinaryMask(imgVal, 0.5);
    	ImagePlus imgOR=VitimageUtils.binaryOperationBetweenTwoImages(ref, val, 1);
    	ImagePlus imgAND=VitimageUtils.binaryOperationBetweenTwoImages(ref, val, 2);
    	int nbIm1=nbPixelsInClasses(imgRef)[255];
    	int nbIm2=nbPixelsInClasses(imgVal)[255];
    	int nbAND=nbPixelsInClasses(imgAND)[255];
    	int nbOR=nbPixelsInClasses(imgOR)[255];
    	return(nbAND*1.0/nbOR);
    }

    public static Object [][] roiPairingHungarianMethod(Roi[]roiTabRef,Roi[]roiTabTest){
		fr.cirad.image.common.Timer t=new fr.cirad.image.common.Timer();
        double[][]costMatrix=new double[roiTabRef.length][roiTabTest.length];
        for(int i=0;i<roiTabRef.length;i++) {
            for(int j=0;j<roiTabTest.length;j++) {
            	costMatrix[i][j]=1-IOU(roiTabRef[i],roiTabTest[j]);
            }
        }

		HungarianAlgorithm hung=new HungarianAlgorithm(costMatrix);
		int []solutions=hung.execute();
		Object[][]ret=new Object[roiTabRef.length][];
        for(int i=0;i<roiTabRef.length;i++) {
        	double surface=getRoiSurface(roiTabRef[i]);
        	if(solutions[i]==-1)ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
        	else if(IOU(roiTabRef[i],roiTabTest[solutions[i]])<=0) ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
        	else ret[i]=new Object[] {solutions[i],IOU(roiTabRef[i],roiTabTest[solutions[i]]),new Double(surface)};
        }    		
//		t.print("Hungarian");
        return ret;
    }

    public static Object [][] roiPairing(Roi[]roiTabRef,Roi[]roiTabTest){
            boolean []hasBeenPaired=new boolean[roiTabTest.length];
            Object[][]ret=new Object[roiTabRef.length][2];
            for(int i=0;i<roiTabRef.length;i++) {
            	double surface=getRoiSurface(roiTabRef[i]);
            	ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
                double max=0.000001;
                int indmax=0;
                for(int j=0;j<roiTabTest.length;j++) {
            		if(hasBeenPaired[j])continue;
                    double val=IOU(roiTabRef[i],roiTabTest[j]);
                    if(val>max) {
//                    		System.out.println("Nouveau max : "+val);
                            max=val;
                            ret[i]=new Object[] {new Integer(j),new Double(val),new Double(surface)};
                    }
                }
            }
            return ret;
    }
    
    public static double IOU(Roi r1,Roi r2) {
        if(r1.getBounds().getMinX()>r2.getBounds().getMaxX())return 0;
        if(r1.getBounds().getMinY()>r2.getBounds().getMaxY())return 0;
        if(r2.getBounds().getMinX()>r1.getBounds().getMaxX())return 0;
        if(r2.getBounds().getMinY()>r1.getBounds().getMaxY())return 0;

        int x0=(int)Math.floor(Math.min(r1.getBounds().getMinX(), r2.getBounds().getMinX()));
        int x1=(int)Math.ceil(Math.max(r1.getBounds().getMaxX(), r2.getBounds().getMaxX()));
        int y0=(int)Math.floor(Math.min(r1.getBounds().getMinY(), r2.getBounds().getMinY()));
        int y1=(int)Math.ceil(Math.max(r1.getBounds().getMaxY(), r2.getBounds().getMaxY()));
        //System.out.println("Bbox both="+x0+","+x1+","+y0+","+y1);
        int inter=0;
        int union=0;
        for(int x=x0;x<=x1;x++) {
                for(int y=y0;y<=y1;y++) {
                        if((r1.contains(x, y)) && (r2.contains(x, y)))inter++;
                        if((r1.contains(x, y)) || (r2.contains(x, y)))union++;
                }
                //System.out.println("Result : "+inter+" , "+union);
        }
        return (1.0*inter)/union;
}

	public static void scoreComparisonSegmentations(ImagePlus segRef,ImagePlus segTest,boolean verbose) {
		double accIOU=0;
		int nTotReal=0;
		int nTotPred=0;
		int totMatch=0;
		int []totMatchClass=new int[20];
		double []iouPerClass=new double[20];
		int Z=segTest.getNSlices();
		int[]TP=new int[20];
		int TPFull=0;
		int FNFull=0;
		int FPFull=0;
		int[]FN=new int[20];
		int[]FP=new int[20];
		int[]nPred=new int[20];
		int[]nReal=new int[20];
		for(int z=0;z<Z;z++) {
			ImagePlus binaryReal=new Duplicator().run(segRef,1,1,z+1,z+1,1,1);
			ImagePlus binaryPred=new Duplicator().run(segTest,1,1,z+1,z+1,1,1);
            double iouGlob=SegmentationUtils.IOU(binaryReal,binaryPred);
            Roi[]rReal=SegmentationUtils.segmentationToRoi(binaryReal);
            Roi[]rPred=SegmentationUtils.segmentationToRoi(binaryPred);
            for(int i=0;i<rReal.length;i++) {
            	double surf=getRoiSurface(rReal[i]);
				int index=(int)Math.floor(((Double)surf/20.0));
				if(index>19)index=19;
				nReal[index]++;
            }
            for(int i=0;i<rPred.length;i++) {
            	double surf=getRoiSurface(rPred[i]);
				int index=(int)Math.floor(((Double)surf/20.0));
				if(index>19)index=19;
				nPred[index]++;
            }

            if(rPred!=null) {
				nTotReal+=rReal.length;
				nTotPred+=rPred.length;
				Object[][]tab=SegmentationUtils.roiPairingHungarianMethod(rReal,rPred);
				boolean []checkedPred=new boolean[rPred.length];
				for(int i=0;i<tab.length;i++) {	
//					System.out.println(tab[i][0]+" "+tab[i][1]+" "+tab[i][2]);
					int index=(int)Math.floor(((Double)tab[i][2]/20.0));
					if(index>19)index=19;
					//nReal[index]++;
					if((Integer)tab[i][0]<0) {FN[index]++;FNFull++;}
					else {
						checkedPred[(Integer)tab[i][0]]=true;
						int index2=(int)Math.floor(((Double)getRoiSurface(rPred[ (Integer)tab[i][0] ] )/20.0));
						if(index2>19)index2=19;
						nPred[index2]--;
						nPred[index]++;
						
						
						TP[index]++;
						TPFull++;
						accIOU+=(Double)(tab[i][1]);
						iouPerClass[index]+=(Double)(tab[i][1]);
						totMatch++;
						totMatchClass[index]++;
					}
				}
				for(int i=0;i<rPred.length;i++) {
					if(!checkedPred[i]) {
						int index2=(int)Math.floor(((Double)getRoiSurface(rPred[ i ] )/20.0));
						if(index2>19)index2=19;
						FP[index2]++;
						
					}
				}
            }
		}
		accIOU/=totMatch;

		//Compute False positive
		FPFull=nTotPred-TPFull;
		int TPpti=0;
		int TPgrand=0;
		int FNpti=0;
		int FNgrand=0;
		int FPpti=0;
		int FPgrand=0;
		double ioupti=0;
		double iougrand=0;
		int countPti=0;
		int countGrand=0;
		String[]codesPython=new String[] {"prec=[","rec=[","iou=["};
		for(int i=0;i<20;i++) {
			String precision=""+VitimageUtils.dou(TP[i]*1.0/(TP[i]+FP[i]));
			String recall=""+VitimageUtils.dou(TP[i]*1.0/(TP[i]+FN[i]));
			String iou=""+(VitimageUtils.dou(iouPerClass[i]/TP[i]));
			if(TP[i]==0) precision=recall=iou="inf";
			codesPython[0]+=""+precision+(i==19 ? "]" : ",");
			codesPython[1]+=""+recall+(i==19 ? "]" : ",");
			codesPython[2]+=""+iou+(i==19 ? "]" : ",");
			if(verbose)System.out.println("Classe ["+(i*20)+" - "+((i+1)*20)+"]: pr,rec,iou "+precision+" , "+recall+" , "+iou+"    nReal"+nReal[i]+" nPred="+nPred[i]+" nMatch="+totMatchClass[i]+" TP="+TP[i]+" FP="+FP[i]+" FN="+FN[i]);
			if(i<5) {
				TPpti+=TP[i];
				FNpti+=FN[i];
				FPpti+=FP[i];
				ioupti+=iouPerClass[i];
				countPti+=TP[i];
			}
			else {
				TPgrand+=TP[i];
				FNgrand+=FN[i];
				FPgrand+=FP[i];
				iougrand+=iouPerClass[i];
				countGrand+=TP[i];
			}
		}
		ioupti/=countPti;
		iougrand/=countGrand;
//		System.out.println("Total real="+nTotReal+" total pred="+nTotPred);
		double globPrec=VitimageUtils.dou(TPFull*1.0/(nTotPred));
		double globRec=VitimageUtils.dou(TPFull*1.0/(nTotReal));
		double globPrecPti=VitimageUtils.dou(TPpti*1.0/(TPpti+FPpti));
		double globRecPti=VitimageUtils.dou(TPpti*1.0/(TPpti+FNpti));
		double globPrecGrand=VitimageUtils.dou(TPgrand*1.0/(TPgrand+FPgrand));
		double globRecGrand=VitimageUtils.dou(TPgrand*1.0/(TPgrand+FNgrand));
		System.out.println("Summary : Prec="+globPrec+" , Rec="+globRec+" , mean IOU="+accIOU+" . ");
		System.out.println("Little's: Prec="+globPrecPti+" , Rec="+globRecPti+" , mean IOU="+ioupti+" . ");
		System.out.println("Large 's: Prec="+globPrecGrand+" , Rec="+globRecGrand+" , mean IOU="+iougrand+" . ");
		SegmentationUtils.getSizeMap(segRef,0,20,5).show();
		for(String c : codesPython)System.out.println(c);
		
	}

	public static ImagePlus getWatershed2D(ImagePlus in,ImagePlus marker,ImagePlus mask) {
		if(in.getNSlices()>1) {
			ImagePlus []tabIn=VitimageUtils.stackToSlices(in);
			ImagePlus []tabMarker=VitimageUtils.stackToSlices(marker);			
			ImagePlus []tabMask=VitimageUtils.stackToSlices(mask);			
			for(int i=0;i<tabIn.length;i++) {
				tabIn[i]=getWatershed2D(tabIn[i],tabMarker[i],tabMask[i]);
			}
			return VitimageUtils.slicesToStack(tabIn);
		}
		MarkerControlledWatershedTransform2D mark=new MarkerControlledWatershedTransform2D(in.getStack().getProcessor(1), marker.getStack().getProcessor(1), mask.getStack().getProcessor(1),4);
		ImageProcessor ip=mark.applyWithPriorityQueueAndDams();
		return new ImagePlus("Results",ip);
	}

	public static ImagePlus getSegmentationFromProbaMap3D(ImagePlus probaMap,double thresh1,double thresh2) {
		ImagePlus debug=probaMap.duplicate();
		ImagePlus []tab=new ImagePlus[probaMap.getNSlices()];
		for(int z=0;z<probaMap.getNSlices();z++) {
			ImagePlus temp=new Duplicator().run(probaMap,1,1,z+1,z+1,1,1);
			tab[z]=getSegmentationFromProbaMap2D(temp,thresh1,thresh2);
		}
		return VitimageUtils.slicesToStack(tab);
	}
	
	public static ImagePlus getPointRoiImageOfMaximaFromProbaMap2D(ImagePlus probaMap,double thresh) {
		//Lisser l'image de probabilité
		ImagePlus probaGauss=probaMap.duplicate();
		IJ.run(probaGauss, "Median...", "sigma=2 stack");

		//Extraire les maxima
		IJ.run(probaGauss, "Find Maxima...", "prominence="+thresh+" output=[Single Points]");
		VitimageUtils.waitFor(10);
		ImagePlus tmp=IJ.getImage();
		ImagePlus pts=tmp.duplicate();
		tmp.changes=false;
		tmp.close();
		pts.setTitle("Points");
		probaGauss.changes=false;
		ImagePlus ret=pts.duplicate();
		probaGauss.close();
		pts.close();
		return ret;
	}
		
	
	public static Point[] getCoordinatesOfMaximaFromProbaMap2D(ImagePlus probaMap,double thresh) {
		probaMap.show();
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(probaMap, "Find Maxima...", "prominence="+thresh+" output=[Point Selection]");
		rm.addRoi(probaMap.getRoi());
		PointRoi pr=(PointRoi) rm.getRoi(0);
		return pr.getContainedPoints();
	}
	
	public static ImagePlus getSegmentationFromProbaMap2D(ImagePlus probaMap,double thresh1,double thresh2) {
		//Get image of maxima
		ImagePlus pts=getPointRoiImageOfMaximaFromProbaMap2D(probaMap,thresh1);

		//Extraire le masque de la zone de proba interessante
		ImagePlus probaGauss2=probaMap.duplicate();
		IJ.run(probaGauss2, "Median...", "sigma=2 stack");
		ImagePlus mask=VitimageUtils.getBinaryMask(probaGauss2, thresh2);

		//Calculer le watershed et les résultats
		ImagePlus result=getWatershed2D(probaGauss2, pts, mask);
		pts.changes=false;
		pts.close();
		ImagePlus temp=result.duplicate();
		temp= cleanVesselSegmentation(result,512,2,5000);
		return temp;
	}
	
	public static ImagePlus getSegmentationFromProbaMap2D(ImagePlus probaMap,double threshold,int medianFilteringSize) {
		if(probaMap.getNSlices()>1) {
			ImagePlus []sli=VitimageUtils.stackToSlices(probaMap);
			for(int i=0;i<sli.length;i++) {
				sli[i]=getSegmentationFromProbaMap2D(sli[i],threshold,medianFilteringSize);
				IJ.run(sli[i],"Grays","");
			}
			return VitimageUtils.slicesToStack(sli);
		}

		//Extraire le masque de la zone de proba interessante
		ImagePlus probaGauss2=probaMap.duplicate();
		if(medianFilteringSize>0)IJ.run(probaGauss2, "Median...", "sigma="+medianFilteringSize+" stack");
		if(medianFilteringSize<0)IJ.run(probaGauss2, "Gaussian Blur...", "sigma="+(-medianFilteringSize)+" stack");

		//Get image of maxima
		ImagePlus pts=getPointRoiImageOfMaximaFromProbaMap2D(probaGauss2,threshold);

	//	probaGauss2.show();VitimageUtils.waitFor(5000);
		ImagePlus mask=VitimageUtils.getBinaryMask(probaGauss2, threshold);

		//Calculer le watershed et les résultats
		ImagePlus result=getWatershed2D(probaGauss2, pts, mask);
		pts.changes=false;
		pts.close();
		ImagePlus temp=result.duplicate();
		IJ.run(temp,"8-bit","");
		return temp;
	}
	public static ImagePlus drawRectangleInRGBImage(ImagePlus imgIn,int x0,int y0,int wid,int hei,int z,Color col) {
		if(imgIn.getType() != ImagePlus.COLOR_RGB)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		if(x0<0 || y0<0 || (x0+wid)>=xM || (y0+hei)>=yM )return imgIn;
		byte[][] valsImg=new byte[3][];
		ImagePlus[]chans=VitimageUtils.splitRGBStackHeadLess(imgIn);
		for(int c=0;c<3;c++) {
			int val= c==0 ? col.getRed() : c==1 ? col.getGreen() : col.getBlue();
			valsImg[c]=(byte [])chans[c].getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<=x0+wid;x++) {
				for(int y=y0;y<=y0+hei;y++) {
					valsImg[c][xM*y+x]=  (byte)( ((byte)val) & 0xff);
				}
			}
		}			
		return VitimageUtils.compositeRGBByteTab(chans);
	}

	public static ImagePlus convertShortToByte(ImagePlus labels) {
		int max=(int) labels.getDisplayRangeMax();
		labels.setDisplayRange(0,255);
		IJ.run(labels,"8-bit","");
		labels.setDisplayRange(0,max);		
		return labels;
	}
	
	
	public static ImagePlus drawOrientationCircle(ImagePlus img,double xVect,double yVect) {
		ImagePlus ret=VitimageUtils.nullImage(img);
		double[]vect=TransformUtils.normalize(new double[] {xVect,yVect});
		double co=0.85;
		double si=0.5;
		double[]vectArrToLeft=new double[] {-vect[0]*co-vect[1]*si,si*vect[0]-vect[1]*co};
		double[]vectArrToRight=new double[] {-vect[0]*co+vect[1]*si,-si*vect[0]-vect[1]*co};
		ret=SegmentationUtils.resetCalibration(VitimageUtils.nullImage(ret));
		IJ.run(ret,"8-bit","");
		int X=img.getWidth();
		int Y=img.getHeight();
		int radius=(X+Y)/4;
		int normSeg=12;
		int normArrow=radius-8;
		

		if((xVect==0) && (yVect==0)) {
			int x0=X/10;		int y0=Y/10;//Center
			int x1=X/3;		    int y1=Y/10;//Center
			int x2=X/10;		int y2=Y/3;//Center
			int x3=X/3;		int y3=Y/3;//Center
			ret=drawSegmentIn2DFloatImage(ret, 3, 255, x0, y0, x3, y3);			
			ret=drawSegmentIn2DFloatImage(ret, 3, 255, x1, y1, x2, y2);			
			ret=resizeNearest(ret, X/2, Y/2, 1);
		}
		else {
			int x0=X/2;		int y0=Y/2;//Center
			double x1=x0+normArrow*vect[0];		double y1=y0+normArrow*vect[1];//Arrow stem
			double x2=x1+normSeg*vectArrToLeft[0];		double y2=y1+normSeg*vectArrToLeft[1];//Arrow stem
			double x3=x1+normSeg*vectArrToRight[0];		double y3=y1+normSeg*vectArrToRight[1];//Arrow stem
			double x5=x0+(normArrow-normSeg+2)*vect[0];		double y5=y0+(normArrow-normSeg+2)*vect[1];//Arrow stem
			ret=VitimageUtils.drawCircleNoFillInImage(ret,1, x0,x0, 0, 230, 3);
			ret=VitimageUtils.drawCircleNoFillInImage(ret,normArrow+2, x0,x0, 0, 230, 3);
			ret=drawSegmentIn2DFloatImage(ret, 3, 255, x0, y0, x5, y5);
			ret=drawSegmentIn2DFloatImage(ret, 2, 255, x1, y1, x2, y2);
			ret=drawSegmentIn2DFloatImage(ret, 2, 255, x1, y1, x3, y3);
			ret=drawSegmentIn2DFloatImage(ret, 2, 255, x2, y2, x3, y3);
			ret=resizeNearest(ret, X/2, Y/2, 1);
		}
		IJ.run(ret,"8-bit","");		
		return ret;
	}
	
	
	public static ImagePlus drawSegmentIn2DFloatImage(ImagePlus imgIn,double thickness,double valToPrint,double x0,double y0,double x1,double y1) {
		ImagePlus img=new Duplicator().run(imgIn);
		IJ.run(img,"32-bit","");
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double[]vectAx;
		double[]vectBx;
		double[]vectAB=new double[] {(x1-x0),(y1-y0),0};
		double[]vectBA=new double[] {(x0-x1),(y0-y1),0};
		double[]vectAxproj;
		double[]vectxprojx;
		double distanceLine;
		float[] valsImg=(float [])img.getStack().getProcessor(1).getPixels();
		for(int x=0;x<xM;x++) {
			for(int y=0;y<yM;y++) {
				vectAx=new double[] {(x-x0),(y-y0),0};
				vectBx=new double[] {(x-x1),(y-y1),0};
				vectAxproj=TransformUtils.proj_u_of_v( vectAB,vectAx);
				vectxprojx=TransformUtils.vectorialSubstraction(vectAx,vectAxproj);
				double dist=TransformUtils.norm(vectxprojx);
				double scal1=TransformUtils.scalarProduct(vectAx, vectAB);
				double scal2=TransformUtils.scalarProduct(vectBx, vectBA);
				if((scal1>0) && (scal2>0) && (dist<thickness/2.0)) valsImg[xM*y+x]=  (float)(valToPrint);
			}			
		}
		return img;
	}

	
	
	public static void main(String []args) {
		ImageJ ij=new ImageJ();
		ImagePlus img3=IJ.openImage("/home/rfernandez/Bureau/testBin.tif");
		img3.show();
		ImagePlus img=VitimageUtils.connexeBinaryEasierParamsConnexitySelectvol(img3, 6, 0);
		img.show();
		double[][][]coords=SegmentationUtils.inertiaComputation65536(img, false, false,true);
		for(int i=0;i<coords.length;i++)SegmentationUtils.printEllipse(coords[i]);
		Timer t=new Timer();
	}
	
	
	public static ImagePlus[]splitBinaryPortraitIntoUpperAndDownPart(ImagePlus segg,double x0,double y0,double x1,double y1){
		ImagePlus seg=segg.duplicate();
		IJ.run(seg,"32-bit","");
		ImagePlus []tab=new ImagePlus[] {seg.duplicate(),seg.duplicate()};
		int X=seg.getWidth();
		int Y=seg.getHeight();
		float[]valTop=(float[])tab[0].getStack().getProcessor(1).getPixels();
		float[]valBottom=(float[])tab[1].getStack().getProcessor(1).getPixels();
		double[]vectLR=new double[] {x1-x0,y1-y0};
		double[]vectMF=new double[] {y1-y0,-x1+x0};
		for(int x=0;x<X;x++) for(int y=0;y<Y;y++){
			if(valTop[y*X+x]>0) {
				double[]vectPt=new double[] {x-x0,y-y0};
				double orientVal=TransformUtils.scalarProduct(vectMF, vectPt);
				//Complex calculation stands here
				if(orientVal<0)valTop[y*X+x]=0;
				if(orientVal>0)valBottom[y*X+x]=0;
			}
		}	
		tab[0].setDisplayRange(0, 255);IJ.run(tab[0],"8-bit","");
		tab[1].setDisplayRange(0, 255);IJ.run(tab[1],"8-bit","");
		return tab;
	}
	
	public static void printEllipse(double[][]tab) {
		System.out.println("->Area label "+tab[0][0]+" with center ("+dou(tab[0][1])+" , "+dou(tab[0][2])+")");
		System.out.println("  Great axis="+dou(tab[1][0])+" , short axis="+dou(tab[1][1]));
		System.out.println("  Surface="+tab[2][0]+" , ellipse surface="+dou(tab[2][1])+" , angle="+dou(tab[2][2]));
	}

	public static double dou(double d) {return VitimageUtils.dou(d);}
	
	public static double[][][]inertiaComputation(ImagePlus labels,boolean sortByVolume,boolean printEllipsis,boolean excludeZeroRegion){
		if(labels.getType()==ImagePlus.GRAY8)return inertiaComputation256(labels,sortByVolume,printEllipsis,excludeZeroRegion);
		else return inertiaComputation65536(labels,sortByVolume,printEllipsis,excludeZeroRegion);
	}

	public static double[][][]inertiaComputation256(ImagePlus labels,boolean sortByVolume,boolean printEllipsis,boolean excludeZeroRegion){
		int []tab=new int[256];for(int i=0;i<256;i++)tab[i]=i;
		Ellipse[] el=InertiaEllipse.inertiaEllipses(labels.getStack().getProcessor(1),tab, null);
		int N=(int) VitimageUtils.maxOfImage(labels)+(excludeZeroRegion ? 0 : 1);
		int[]volumes=getVolumesOfObjects(labels);
		double[][][]ret=new double[N][3][3];
		for(int n=(excludeZeroRegion ? 1 : 0);n<(N+(excludeZeroRegion ? 1 : 0));n++) {
			Ellipse e=el[n];
			Point2D center=e.center();
			ret[n-(excludeZeroRegion ? 1 : 0)]=new double[][] {{n,e.center().getX(),e.center().getY()},{e.radius1(),e.radius2()},{volumes[n],e.area(),e.orientation()}};
		}
		if(sortByVolume) {
			Arrays.sort(ret,new VolumeEllipsisComparator());
		}
		if(printEllipsis) {		
			for(int i=0;i<ret.length;i++) {
				if(ret[i][2][0]>0) {printEllipse(ret[i]);System.out.println();}
				
			}
		}
		return ret;
		
	}

	public static double[][][]inertiaComputation65536(ImagePlus labels,boolean sortByVolume,boolean printEllipsis,boolean excludeZeroRegion){
		int []tab=new int[65536];for(int i=0;i<65536;i++)tab[i]=i;
		Ellipse[] el=InertiaEllipse.inertiaEllipses(labels.getStack().getProcessor(1),tab, null);
		int N=(int) VitimageUtils.maxOfImage(labels)+(excludeZeroRegion ? 0 : 1);
		int[]volumes=getVolumesOfObjects(labels);
		double[][][]ret=new double[N][3][3];
		for(int n=(excludeZeroRegion ? 1 : 0);n<(N+(excludeZeroRegion ? 1 : 0));n++) {
			Ellipse e=el[n];
			Point2D center=e.center();
			ret[n-(excludeZeroRegion ? 1 : 0)]=new double[][] {{n,e.center().getX(),e.center().getY()},{e.radius1(),e.radius2()},{volumes[n],e.area(),e.orientation()}};
		}
		if(sortByVolume) {
			Arrays.sort(ret,new VolumeEllipsisComparator());
		}
		if(printEllipsis) {		
			for(int i=0;i<ret.length;i++) {
				printEllipse(ret[i]);
				System.out.println();
			}
		}
		return ret;
		
	}

	
	
	public static int getVolumeOfObject(ImagePlus bin){
		ImagePlus temp=bin.duplicate();
		IJ.run(temp,"32-bit","");
		int total=0;
		int X=temp.getWidth();
		int Y=temp.getHeight();
		for(int z=0;z<temp.getNSlices();z++) {
			float[]val=(float[])temp.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) for(int y=0;y<Y;y++){
				if(val[y*X+x]>0)total++;
			}
		}
		return total;
	}

	public static int[]getVolumesOfObjects(ImagePlus labels){
		if(labels.getType()==ImagePlus.GRAY8)return getVolumesOfObjects256(labels);
		else return getVolumesOfObjects65536(labels);
	}
	
	public static int []getVolumesOfObjects65536(ImagePlus labels){
		ImagePlus temp=labels.duplicate();
		if(VitimageUtils.maxOfImage(labels)>65535)return null;
		int[]volumes=new int[65536];
		IJ.run(temp,"32-bit","");
		int total=0;
		int X=temp.getWidth();
		int Y=temp.getHeight();
		for(int z=0;z<temp.getNSlices();z++) {
			float[]val=(float[])temp.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) for(int y=0;y<Y;y++){
				volumes[(int)Math.round(val[y*X+x])]++;
			}
		}
		return volumes;
	}
	
	public static int []getVolumesOfObjects256(ImagePlus labels){
		ImagePlus temp=labels.duplicate();
		if(VitimageUtils.maxOfImage(labels)>255)return null;
		int[]volumes=new int[256];
		IJ.run(temp,"32-bit","");
		int total=0;
		int X=temp.getWidth();
		int Y=temp.getHeight();
		for(int z=0;z<temp.getNSlices();z++) {
			float[]val=(float[])temp.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) for(int y=0;y<Y;y++){
				volumes[(int)Math.round(val[y*X+x])]++;
			}
		}
		return volumes;
	}

	
	public static void drawEllipse(ImagePlus img,double xC,double  yC,double  ga,double  pa,double  angle,int value) {
		ImageProcessor ip=img.getProcessor();
		int x0=(int) Math.round(xC-ga-pa);
		int x1=(int) Math.round(xC+ga+pa);
		int y0=(int) Math.round(yC-ga-pa);
		int y1=(int) Math.round(yC+ga+pa);
		if(x0<0)x0=0;
		if(y0<0)y0=0;
		if(x1>=img.getWidth())x1=img.getWidth()-1;
		if(y1>=img.getHeight())y1=img.getHeight()-1;
		for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++) {
			double xRel=(x-xC);
			double yRel=y-yC;
			double ang=-1*Math.PI*angle/180.0;
			double xAn=xRel*Math.cos(ang)-yRel*Math.sin(ang);
			double yAn=xRel*Math.sin(ang)+yRel*Math.cos(ang);
			xAn=xAn/ga;
			yAn=yAn/pa;
			if((xAn*xAn + yAn*yAn)<1)ip.set(x, y, value);
		}
		img.setProcessor(ip);
	  }
	
	
	
	public static double[][]inertiaComputationBin(ImagePlus bin,boolean debug){
		int []tab=new int[256];for(int i=0;i<256;i++)tab[i]=i;
		Ellipse el=InertiaEllipse.inertiaEllipses(bin.getStack().getProcessor(1),tab, null)[255];
		double rad1=el.radius1();
		double rad2=el.radius2();
		Point2D center=el.center();
		double surf=el.area();
		double angle=el.orientation();
		double[][]ret = new double[][] {{1,center.getX(),center.getY()},{rad1,rad2},{getVolumeOfObject(bin),surf,angle}};
		if(debug)printEllipse(ret);
		return ret;
	}
	
	public static  ImagePlus erosion(ImagePlus img, int radius,boolean is3d) {
		Strel3D str2=inra.ijpb.morphology.strel.DiskStrel.fromDiameter(radius);
		Strel3D str3=inra.ijpb.morphology.strel.BallStrel.fromDiameter(radius);
		return new ImagePlus("",Morphology.erosion(img.getImageStack(),is3d ? str3 : str2));
	}
	
	public static ImagePlus dilation(ImagePlus img, int radius,boolean is3d) {
		Strel3D str2=inra.ijpb.morphology.strel.DiskStrel.fromDiameter(radius);
		Strel3D str3=inra.ijpb.morphology.strel.BallStrel.fromDiameter(radius);
		return new ImagePlus("",Morphology.dilation(img.getImageStack(),is3d ? str3 : str2));
	}
	
	
	public static ImagePlus generateLabelImageFromMasks(ImagePlus []binaryMasks,boolean considerBgAsClass0) {
		if(!considerBgAsClass0) {IJ.showMessage("Not yet : considerBgAsClass0 false in generateLabelImage in SegmentationUtils.java");return null;}
		int N=binaryMasks.length+1;
		ImagePlus []imgs=new ImagePlus[N];
		for(int i=0;i<N-1;i++) {
			imgs[i+1]=VitimageUtils.makeOperationOnOneImage(VitimageUtils.getBinaryMaskUnary(binaryMasks[i].duplicate(),0.5),2,i+2,true);
		}
		imgs[0]=VitimageUtils.makeOperationOnOneImage(VitimageUtils.nullImage(binaryMasks[0]),1,1,true);
		for(int i=0;i<N;i++)IJ.run(imgs[i],"32-bit","");
		ImagePlus result=VitimageUtils.maxOfImageArray(imgs);
		IJ.run(result,"8-bit","");
		result.setDisplayRange(0, N);
		IJ.run(result,"Fire","");
		return result;
	}
	
    /** Weka train and apply model --------------------------------------------------------------------------------------------*/        
    public static void wekaTrainModelNary(ImagePlus imgTemp,ImagePlus labels,int[]classifierParams,boolean[]enableFeatures,String modelName,boolean debug) {
    	ImagePlus img=new Duplicator().run(imgTemp,1,1,1,debug ? 5 : imgTemp.getNSlices(),1,1);
   	 	ImagePlus mask=new Duplicator().run(labels,1,1,1,debug ? 5 : imgTemp.getNSlices(),1,1);
   	 	int numTrees=classifierParams[0];
	    int numFeatures=classifierParams[1];
	    int seed=classifierParams[2];
	    int minSigma=classifierParams[3];
	    int maxSigma=classifierParams[4];
	    Runtime. getRuntime(). gc();
        long startTime = System.currentTimeMillis();
        WekaSegmentation seg = new WekaSegmentation(img);
        int nCl=(int) VitimageUtils.maxOfImage(mask);
        int nAct=2;
        while(nAct<nCl) {
            seg.addClass();
            nAct++;
            System.out.println("Added a class");
        }
        // Classifier
        FastRandomForest rf = new FastRandomForest();
        rf.setNumTrees(numTrees);                  
        rf.setNumFeatures(numFeatures);  
        rf.setSeed( seed );    
        seg.setClassifier(rf);    
        // Parameters  
        seg.setMembranePatchSize(11);  
        seg.setMinimumSigma(minSigma);
        seg.setMaximumSigma(maxSigma);
  
        // Enable features in the segmentator
        seg.setEnabledFeatures( enableFeatures );

        // Add labeled samples in a balanced and random way
  //  	seg.saveFeatureStack(1, "/home/rfernandez/Bureau/tempdata/fsa",new File(modelName).getName());
        
        int targetExamplesPerSlice=N_EXAMPLES/(nCl*img.getNSlices()*(debug ? 10 : 1));
        System.out.println("Starting training on "+targetExamplesPerSlice+" examples per slice");
        seg.addRandomBalancedLabeledData(img, mask, targetExamplesPerSlice);
        Runtime. getRuntime(). gc();
        //seg.saveFeatureStack(1,"/home/rfernandez/Bureau/tempdata","test");
        //IJ.showMessage("Done!");
        // Train classifier
 //       mask.show();
  //      VitimageUtils.waitFor(5000);
        seg.trainClassifier();
        //ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
        //probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
       //probabilityMaps.show();
//       VitimageUtils.waitFor(5000000);
        seg.saveClassifier(modelName+".model");
        // Apply trained classifier to test image and get probabilities
     //   IJ.saveAsTiff(probabilityMaps, "/home/rfernandez/Bureau/tempdata/test.tif");
        // Print elapsed time
        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished script in " + estimatedTime + " ms **" );
        seg=null;
		Runtime.getRuntime().gc();
    }
           
    public static double[]massCenterIntensityWeighted(ImagePlus imgTemp,int slice){
    	double epsilon = 1E-20;
    	int Z=imgTemp.getNSlices();
    	int X=imgTemp.getWidth();
    	int Y=imgTemp.getHeight();
    	float[] tab=(float[])imgTemp.getStack().getPixels(slice+1);
    	double totWeight=0;    	
    	double totX=0;
    	double totY=0;
    	for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
    		if(tab[y*X+x]<epsilon)continue;
    		totWeight+=tab[y*X+x];
    		totX+=(tab[y*X+x]*x);
    		totY+=(tab[y*X+x]*y);
    	}
    	return new double[] {totX/totWeight,totY/totWeight};
    }
	
	
    /** Weka train and apply model --------------------------------------------------------------------------------------------*/        
    public static void wekaTrainModel(ImagePlus imgTemp,ImagePlus maskTemp,int[]classifierParams,boolean[]enableFeatures,String modelName) {
   	 ImagePlus img=new Duplicator().run(imgTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
   	 ImagePlus mask=new Duplicator().run(maskTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
   	 int numTrees=classifierParams[0];
	   int numFeatures=classifierParams[1];
	   int seed=classifierParams[2];
	   int minSigma=classifierParams[3];
	   int maxSigma=classifierParams[4];
	   VitimageUtils.printImageResume(img); 
       VitimageUtils.printImageResume(mask); 
	   Runtime. getRuntime(). gc();
        long startTime = System.currentTimeMillis();
        WekaSegmentation seg = new WekaSegmentation(img);
 
        // Classifier
        FastRandomForest rf = new FastRandomForest();
        rf.setNumTrees(numTrees);                  
        rf.setNumFeatures(numFeatures);  
        rf.setSeed( seed );    
        seg.setClassifier(rf);    
        // Parameters  
        seg.setMembranePatchSize(11);  
        seg.setMinimumSigma(minSigma);
        seg.setMaximumSigma(maxSigma);
  
    
        // Enable features in the segmentator
        seg.setEnabledFeatures( enableFeatures );

        // Add labeled samples in a balanced and random way
  //  	seg.saveFeatureStack(1, "/home/rfernandez/Bureau/tempdata/fsa",new File(modelName).getName());
        
        int[]nbPix=SegmentationUtils.nbPixelsInClasses(mask);
        int min=Math.min(nbPix[0],nbPix[255]);
        int targetExamplesPerSlice=N_EXAMPLES/(2*img.getNSlices());
        System.out.println("Starting training on "+targetExamplesPerSlice+" examples per slice");
        seg.addRandomBalancedBinaryData(img, mask, "class 2", "class 1", targetExamplesPerSlice);
        Runtime. getRuntime(). gc();
        //seg.saveFeatureStack(1,"/home/rfernandez/Bureau/tempdata","test");
        //IJ.showMessage("Done!");
        // Train classifier
        seg.trainClassifier();
        seg.saveClassifier(modelName+".model");
        // Apply trained classifier to test image and get probabilities
    //    ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
     //   probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
     //   IJ.saveAsTiff(probabilityMaps, "/home/rfernandez/Bureau/tempdata/test.tif");
        // Print elapsed time
        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished script in " + estimatedTime + " ms **" );
        seg=null;
		Runtime.getRuntime().gc();
    }
           
	
    /** Weka train and apply model --------------------------------------------------------------------------------------------*/        
    public static void wekaTrainModels(ImagePlus imgTemp,ImagePlus []maskTemp,int[]classifierParams,boolean[]enableFeatures,String []modelNames) {
   	 ImagePlus img=new Duplicator().run(imgTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
   	 ImagePlus mask=new Duplicator().run(maskTemp[0],1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
   	 int numTrees=classifierParams[0];
	   int numFeatures=classifierParams[1];
	   int seed=classifierParams[2];
	   int minSigma=classifierParams[3];
	   int maxSigma=classifierParams[4];
	   VitimageUtils.printImageResume(img); 
       VitimageUtils.printImageResume(mask); 
	   Runtime. getRuntime(). gc();
        long startTime = System.currentTimeMillis();
        WekaSegmentation seg = new WekaSegmentation(img);

        // Classifier
        FastRandomForest rf = new FastRandomForest();
        rf.setNumTrees(numTrees);                  
        rf.setNumFeatures(numFeatures);  
        rf.setSeed( seed );    
        seg.setClassifier(rf);    
        // Parameters  
        seg.setMembranePatchSize(11);  
        seg.setMinimumSigma(minSigma);
        seg.setMaximumSigma(maxSigma);
  
    
        // Enable features in the segmentator
        seg.setEnabledFeatures( enableFeatures );

        // Add labeled samples in a balanced and random way
  //  	seg.saveFeatureStack(1, "/home/rfernandez/Bureau/tempdata/fsa",new File(modelName).getName());
        
        int[]nbPix=SegmentationUtils.nbPixelsInClasses(mask);
        int min=Math.min(nbPix[0],nbPix[255]);
        int targetExamplesPerSlice=N_EXAMPLES/(2*img.getNSlices());
        System.out.println("Starting training on "+targetExamplesPerSlice+" examples per slice");

        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished Preparing in " + estimatedTime + " ms **" );
        for(int i=0;i<maskTemp.length;i++) {
          	ImagePlus maskT=new Duplicator().run(maskTemp[i],1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
          	seg.addRandomBalancedBinaryData(img, maskT, "class 2", "class 1", targetExamplesPerSlice);
          	Runtime. getRuntime(). gc();
	        seg.trainClassifier();
	        seg.saveClassifier(modelNames[i]+".model");
	        IJ.log( "** Finished "+i+" in " + estimatedTime + " ms **" );
	        seg.setLoadedTrainingData(null);
	        seg.setUpdateFeatures(true);
        }
        estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished script in " + estimatedTime + " ms **" );
        seg=null;
		Runtime.getRuntime().gc();
    }

    
    public static WekaSegmentation[]initModels(ImagePlus imgTest,int []classifierParams,boolean[]enableFeatures,String []modelPaths){
  	    IJ.log("weka set params  ");
	    int numTrees=classifierParams[0];
	    int numFeatures=classifierParams[1];
	    int seed=classifierParams[2];
	    int minSigma=classifierParams[3];
	    int maxSigma=classifierParams[4];
	    long startTime = System.currentTimeMillis();
	    
	    WekaSegmentation []wekas=new WekaSegmentation[modelPaths.length];
	    for(int w=0;w<wekas.length;w++) {
	    	wekas[w]= new WekaSegmentation(imgTest);
	        IJ.log("weka set features  ");
	        // Classifier
	        FastRandomForest rf = new FastRandomForest();
	        rf.setNumTrees(numTrees);                  
	        rf.setNumFeatures(numFeatures);  
	        rf.setSeed( seed );    
	        wekas[w].setClassifier(rf);    
	
	        // Parameters  
	        wekas[w].setMembranePatchSize(11);  
	        wekas[w].setMinimumSigma(minSigma);
	        wekas[w].setMaximumSigma(maxSigma);
	        wekas[w].setEnabledFeatures( enableFeatures );
	
	        VitimageUtils.garbageCollector();
	        IJ.log("weka load model  ");
	        wekas[w].loadClassifier(modelPaths[w]);
	    }
        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished loading models in " + estimatedTime + " ms **" );
	    return wekas;    	
    }
    
    /*
    public static void extractVesselInformationFromBinarySlices(String inputDir,String outputDir) {
 		String[]imgNames=new File(inputDir).list();
 		for(int indImg=0;indImg<imgNames.length;indImg++) {
 			System.out.println("Starting information extraction "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
 			ImagePlus imgInit=IJ.openImage(new File(inputDir,imgNames[indImg]).getAbsolutePath());
 			double[]enclosingCircle=SmallestEnclosingCircle.smallestEnclosingCircle(imgInit);
 			String[][]circleInfo=new String[][] {{""+enclosingCircle[0],""+enclosingCircle[1],""+enclosingCircle[2]}};
 			VitimageUtils.writeStringTabInCsv(circleInfo, VitimageUtils.withoutExtension(imgNames[indImg])+"_circle.csv");
 		}
    }
    */
    
    public static ImagePlus resetCalibration(ImagePlus img) {
    	ImagePlus ret=img.duplicate();
    	ret.setCalibration(new Calibration());
    	return ret;
    }
    
  	public static ImagePlus getVoronoi(ImagePlus imgBin,boolean binary) {
  		ImagePlus t1=imgBin.duplicate();
  		t1=VitimageUtils.invertBinaryMask(t1);
  		IJ.run(t1, "Voronoi", "");
  		if(binary)t1=VitimageUtils.thresholdByteImage(t1, 1, 256);
  		return t1;
  	}
  
    
 	public static void batchVesselSegmentation(String vesselsDir,String inputDir,String outputDir) {
 		String[]imgNames=new File(inputDir).list();
 		ImagePlus imgTest=IJ.openImage(new File(inputDir,imgNames[0]).getAbsolutePath());
 		IJ.run(imgTest,"8-bit","");
 		String[]modelPaths=new String[6];
 		for(int i=0;i<6;i++) {
 			modelPaths[i]=vesselsDir+"/Data/Processing/Models_in_prod/model_layer_1"+("_AUGSET"+i+".model");
 		}
 		WekaSegmentation[]wekas=SegmentationUtils.initModels(imgTest,SegmentationUtils.getStandardRandomForestParams(1), SegmentationUtils.getStandardRandomForestFeatures(), modelPaths);

 		IJ.log("Starting batch processing ");        
 		Timer t= new Timer();
 		for(int indImg=0;indImg<imgNames.length;indImg++) {
 			if(new File(outputDir,"Segmentation/"+imgNames[indImg]).exists())continue;
 					// 		    if(VesselSegmentation.skip(imgNames[indImg]))continue;
 			t.print("Starting ML processing image "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
 			ImagePlus imgInit=IJ.openImage(new File(inputDir,imgNames[indImg]).getAbsolutePath());
 			ImagePlus[]results=new ImagePlus[6];
 			for(int m=0;m<6;m++) {
 				t.print("--"+m);
 				ImagePlus img=imgInit.duplicate();
 				if(m<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(m)];//RGB
	            else img=VitimageUtils.getHSB(img)[(m-3)];//HSB
 				results[m]=wekas[m].applyClassifier(img,0,true);
 			}
 			ImagePlus result=VitimageUtils.meanOfImageArray(results);
 			result=new Duplicator().run(result,2,2,1,1,1,1);
 			results=null;
 			VitimageUtils.garbageCollector();
 			new File(outputDir,"ProbaMap").mkdirs();
 			new File(outputDir,"Segmentation").mkdirs();
 			IJ.save(result, new File(outputDir,"ProbaMap/"+imgNames[indImg]).getAbsolutePath());
 			t.print("Starting segmentation image "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
 			ImagePlus binary=SegmentationUtils.getSegmentationFromProbaMap3D(result,0.5,0.7);
 			IJ.save(binary, new File(outputDir,"Segmentation/"+imgNames[indImg]).getAbsolutePath());
 		}
 	}        

 	public static void batchXyPhloContour(String extractsDir,boolean skipAlreadyDone,boolean hasHeavyDutyRAM) {
 		String[]imgNames=new File(extractsDir).list();
 		String sorghoDir=VesselSegmentation.getVesselsDir();
 		String modelDir=sorghoDir+"/Data/Processing/Models_in_prod/";
 		String target="XyPhlo";
    	String []channels=new String[] {"Red","Green","Blue","Hue","Saturation","Brightness"};
		ImagePlus imgTest=IJ.openImage(new File(extractsDir,imgNames[0]+"/source_sub.tif").getAbsolutePath());
		imgTest=new Duplicator().run(imgTest,1,1,1,1,1,1);

    	IJ.run(imgTest,"8-bit","");
 		String[]modelPaths=new String[6];
 		for(int i=0;i<6;i++) {
 			modelPaths[i]=modelDir+"model_"+target+"_"+channels[i]+".model";
 		}
 		WekaSegmentation[]wekas=SegmentationUtils.initModels(imgTest,SegmentationUtils.getStandardRandomForestParamsVesselsSubSub(1), SegmentationUtils.getStandardRandomForestFeaturesVesselsSubSub(), modelPaths);

 		IJ.log("Starting batch processing ");        
 		Timer t= new Timer();
 		for(int indImg=0;indImg<imgNames.length;indImg++) {
 //		    if(VesselSegmentation.skip(imgNames[indImg]))continue;
 			String dirImgName=new File(extractsDir,imgNames[indImg]).getAbsolutePath();
	    	
 			ImagePlus sourceSub=IJ.openImage(new File(dirImgName,"source_sub.tif").getAbsolutePath());
			int vesNb=sourceSub.getNSlices();
			t.print("\n\n--------------------------------------------------------------------------------\nStarting ML processing image "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
			ImagePlus resVess=null;
			ImagePlus resOther=null;
			ImagePlus resXyl=null;
			ImagePlus resPhlo=null;
			if(new File(dirImgName+"/probaMap_phloquad.tif").exists())continue;
			
			if(hasHeavyDutyRAM) {
				ImagePlus imgInit=IJ.openImage(new File(dirImgName,"source_sub.tif").getAbsolutePath());
				ImagePlus[]results=new ImagePlus[6];
	 			for(int m=0;m<6;m++) {
	 				ImagePlus img=imgInit.duplicate();
	 				if(m<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(m)];//RGB
		            else img=VitimageUtils.getHSB(img)[(m-3)];//HSB
	 				results[m]=wekas[m].applyClassifier(img,0,true);
	 			}
	 			ImagePlus result=VitimageUtils.meanOfImageArray(results);
	 			resVess=new Duplicator().run(result,1,1,1,result.getNSlices(),1,1);resVess.setDisplayRange(0,1);IJ.run(resVess,"8-bit","");
	 			resOther=new Duplicator().run(result,2,2,1,result.getNSlices(),1,1);resOther.setDisplayRange(0,1);IJ.run(resOther,"8-bit","");
	 			resXyl=new Duplicator().run(result,3,3,1,result.getNSlices(),1,1);resXyl.setDisplayRange(0,1);IJ.run(resXyl,"8-bit","");
	 			resPhlo=new Duplicator().run(result,4,4,1,result.getNSlices(),1,1);resPhlo.setDisplayRange(0,1);IJ.run(resPhlo,"8-bit","");
	 			VitimageUtils.garbageCollector();
			}
			else {
				ImagePlus []vesStack=new ImagePlus[vesNb];
				ImagePlus []otherStack=new ImagePlus[vesNb];
				ImagePlus []xylStack=new ImagePlus[vesNb];
				ImagePlus []phloStack=new ImagePlus[vesNb];
				for(int i=0;i<vesNb;i++) {
					System.out.println(i+" / "+vesNb+" || ");
					ImagePlus imgInit=new Duplicator().run(sourceSub,1,1,(i+1),(i+1),1,1);
					ImagePlus[]results=new ImagePlus[6];
		 			for(int m=0;m<6;m++) {
		 				ImagePlus img=imgInit.duplicate();
		 				if(m<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(m)];//RGB
			            else img=VitimageUtils.getHSB(img)[(m-3)];//HSB
		 				results[m]=wekas[m].applyClassifier(img,0,true);
		 			}
		 			ImagePlus res=VitimageUtils.meanOfImageArray(results);
		 			vesStack[i]=new Duplicator().run(res,1,1,1,1,1,1);vesStack[i].setDisplayRange(0,1);IJ.run(vesStack[i],"8-bit","");
		 			otherStack[i]=new Duplicator().run(res,2,2,1,1,1,1);otherStack[i].setDisplayRange(0,1);IJ.run(vesStack[i],"8-bit","");
		 			xylStack[i]=new Duplicator().run(res,3,3,1,1,1,1);xylStack[i].setDisplayRange(0,1);IJ.run(vesStack[i],"8-bit","");
		 			phloStack[i]=new Duplicator().run(res,4,4,1,1,1,1);phloStack[i].setDisplayRange(0,1);IJ.run(vesStack[i],"8-bit","");
	 				if(i%10==0) {
			 			VitimageUtils.garbageCollector();
		 			}
				}
				resVess=VitimageUtils.slicesToStack(vesStack);
				resOther=VitimageUtils.slicesToStack(otherStack);
				resXyl=VitimageUtils.slicesToStack(xylStack);
				resPhlo=VitimageUtils.slicesToStack(phloStack);
			}
			System.out.println("Ecriture dans "+new File(dirImgName+"/probaMap_vessquad.tif").getAbsolutePath());
 			IJ.save(resVess, new File(dirImgName+"/probaMap_vessquad.tif").getAbsolutePath());
 			IJ.save(resOther, new File(dirImgName+"/probaMap_otherquad.tif").getAbsolutePath());
 			IJ.save(resXyl, new File(dirImgName+"/probaMap_xylquad.tif").getAbsolutePath());
 			IJ.save(resPhlo, new File(dirImgName+"/probaMap_phloquad.tif").getAbsolutePath());
 		}
 	}        


 	public static void batchVesselContour(String extractsDir,boolean skipAlreadyDone,boolean hasHeavyDutyRAM) {
 		String[]imgNames=new File(extractsDir).list(); 		
 		String sorghoDir=VesselSegmentation.getVesselsDir();
 		String modelDir=sorghoDir+"/Data/Processing/Models_in_prod/";
 		String target="Vessel_convex_hull";
    	String []channels=new String[] {"Red","Green","Blue","Hue","Saturation","Brightness"};
		ImagePlus imgTest=IJ.openImage(new File(extractsDir,imgNames[0]+"/source_sub.tif").getAbsolutePath());
		imgTest=new Duplicator().run(imgTest,1,1,1,1,1,1);
		
    	IJ.run(imgTest,"8-bit","");
 		String[]modelPaths=new String[6];
 		for(int i=0;i<6;i++) {
  			modelPaths[i]=modelDir+"model_"+target+"_"+channels[i]+"_sub.model";
 		}
 		WekaSegmentation[]wekas=SegmentationUtils.initModels(imgTest,SegmentationUtils.getStandardRandomForestParamsVesselsSub(1), SegmentationUtils.getStandardRandomForestFeaturesVesselsSub(), modelPaths);

 		IJ.log("Starting batch processing ");        
 		Timer t= new Timer();
 		for(int indImg=0;indImg<imgNames.length;indImg++) {
 		    //if(VesselSegmentation.skip(imgNames[indImg]))continue;
 			String dirImgName=new File(extractsDir,imgNames[indImg]).getAbsolutePath();
 			if(!new File(dirImgName).exists())new File(dirImgName).mkdirs();
 			if(new File(dirImgName,"probaMap_vessel_contour.tif").exists())continue;
			ImagePlus sourceSub=IJ.openImage(new File(dirImgName,"source_sub.tif").getAbsolutePath());
			int vesNb=sourceSub.getNSlices();
			t.print("\n\n-----------------------------------------------------------------------\nStarting ML processing image "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
			VitimageUtils.waitFor(2000);
			ImagePlus resFinal=null;
			if(hasHeavyDutyRAM) {
				ImagePlus imgInit=IJ.openImage(new File(dirImgName,"source_sub.tif").getAbsolutePath());
				ImagePlus[]results=new ImagePlus[6];
	 			for(int m=0;m<6;m++) {
	 				ImagePlus img=imgInit.duplicate();
	 				if(m<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(m)];//RGB
		            else img=VitimageUtils.getHSB(img)[(m-3)];//HSB
	 				results[m]=wekas[m].applyClassifier(img,0,true);
	 			}
	 			ImagePlus result=VitimageUtils.meanOfImageArray(results);
	 			resFinal=new Duplicator().run(result,2,2,1,result.getNSlices(),1,1);
	 			resFinal.setDisplayRange(0, 1);
	 			IJ.run(resFinal,"8-bit","");
	 			VitimageUtils.garbageCollector();
			}
			else {
				ImagePlus[]resultsStack=new ImagePlus[vesNb];
				for(int i=0;i<vesNb;i++) {
					System.out.println(i+" / "+vesNb+" || ");
					ImagePlus imgInit=new Duplicator().run(sourceSub,1,1,(i+1),(i+1),1,1);
		 			ImagePlus[]results=new ImagePlus[6];
		 			for(int m=0;m<6;m++) {
		 				ImagePlus img=imgInit.duplicate();
		 				if(m<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(m)];//RGB
			            else img=VitimageUtils.getHSB(img)[(m-3)];//HSB
		 				results[m]=wekas[m].applyClassifier(img,0,true);
		 			}
		 			ImagePlus result=VitimageUtils.meanOfImageArray(results);
		 			resultsStack[i]=new Duplicator().run(result,2,2,1,1,1,1);
		 			resultsStack[i].setDisplayRange(0, 1);
		 			IJ.run(resultsStack[i],"8-bit","");
		 			results=null;
		 			if(i%10==0) VitimageUtils.garbageCollector();
				}
	 			resFinal=VitimageUtils.slicesToStack(resultsStack);
			}
 			IJ.saveAsTiff(resFinal, new File(dirImgName,"probaMap_vessel_contour.tif").getAbsolutePath());
		}
 	}        

/*
 	public static void batchVesselContour(String extractsDir) {
 		String[]imgNames=new File(extractsDir).list();
 		String sorghoDir=VesselSegmentation.getVesselsDir();
 		String modelDir=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Models/";
 		String target="Vessel_convex_hull";
    	String []channels=new String[] {"Red","Green","Blue","Hue","Saturation","Brightness"};
		ImagePlus imgTest=IJ.openImage(new File(extractsDir,imgNames[0]+"/Source/V1.tif").getAbsolutePath());

    	IJ.run(imgTest,"8-bit","");
 		String[]modelPaths=new String[6];
 		for(int i=0;i<6;i++) {
 	 		//"model_"+target+"_"+channels[i]
 			modelPaths[i]=modelDir+"model_"+target+"_"+channels[i]+"_sub.model";
 		}
 		WekaSegmentation[]wekas=SegmentationUtils.initModels(imgTest,SegmentationUtils.getStandardRandomForestParamsVesselsSub(1), SegmentationUtils.getStandardRandomForestFeaturesVesselsSub(), modelPaths);

 		IJ.log("Starting batch processing ");        
 		Timer t= new Timer();
 		for(int indImg=0;indImg<imgNames.length;indImg++) {
 			String dirImgName=new File(extractsDir,imgNames[indImg]).getAbsolutePath();
			String outPath=new File(dirImgName,"ProbaMap_vessel_contour").getAbsolutePath();
			new File(outPath).mkdirs();
	    	
			String sourcePath=new File(dirImgName,"Source").getAbsolutePath();
			String[]vessList=new File(sourcePath).list();
			int vesNb=vessList.length;
			t.print("Starting ML processing image "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);

			ImagePlus []tab=new ImagePlus[vesNb];
			for(int i=0;i<vesNb;i++) {
				System.out.println(i+" / "+vesNb+" || ");
				if(i%50 ==0)System.out.println();
				t.print("\n\nStart");
				tab[i]=IJ.openImage(new File(dirImgName,"Source/V"+(i+1)+".tif").getAbsolutePath());
				t.print("01 after open");
			}
			ImagePlus imgInit=VitimageUtils.slicesToStack(tab);
 			ImagePlus[]results=new ImagePlus[6];
	 			for(int m=0;m<6;m++) {
					t.print("02 start m="+m);
//	 				t.print("--"+m);
	 				ImagePlus img=imgInit.duplicate();
					t.print("021 after duplicate");
	 				if(m<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(m)];//RGB
		            else img=VitimageUtils.getHSB(img)[(m-3)];//HSB
					t.print("022 afterRGBHSB");
	 				img=resize(img, 100, 100, img.getNSlices());
					t.print("023 after resize");
	 				results[m]=wekas[m].applyClassifier(img,0,true);
					t.print("024 after weka");
	 			}
				t.print("03 after m's");
	 			ImagePlus result=VitimageUtils.meanOfImageArray(results);
				t.print("04 after mean");
	 			result=new Duplicator().run(result,2,2,1,1,1,1);
				t.print("05 after dup");
 				result=resize(result, 200, 200, 1);
				t.print("06 after resize");
	 			results=null;
	 			VitimageUtils.garbageCollector();
				t.print("06 after gc");
				for(int i=0;i<vesNb;i++) {
					result=new Duplicator().run(result,1,1,i+1,+1,1,1);
					IJ.save(result, new File(outPath,"V"+(i+1)+".tif").getAbsolutePath());
				}
				t.print("06 after save");
			
 		}
 	}        


 	*/
 	
 	
 
    
    public static ImagePlus wekaApplyModel(ImagePlus imgTemp,int []classifierParams,boolean[]enableFeatures,String modelName) {
    	ImagePlus img=new Duplicator().run(imgTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
  	    IJ.log("weka set params  ");
	    int numTrees=classifierParams[0];
	    int numFeatures=classifierParams[1];
	    int seed=classifierParams[2];
	    int minSigma=classifierParams[3];
	    int maxSigma=classifierParams[4];
	    long startTime = System.currentTimeMillis();
	    WekaSegmentation seg = new WekaSegmentation(imgTemp);
        IJ.log("weka set features  ");
        // Classifier
        FastRandomForest rf = new FastRandomForest();
        rf.setNumTrees(numTrees);                  
        rf.setNumFeatures(numFeatures);  
        rf.setSeed( seed );    
        seg.setClassifier(rf);    

        // Parameters  
        seg.setMembranePatchSize(11);  
        seg.setMinimumSigma(minSigma);
        seg.setMaximumSigma(maxSigma);
        seg.setEnabledFeatures( enableFeatures );

        VitimageUtils.garbageCollector();
        IJ.log("weka load model  ");
        seg.loadClassifier(modelName+".model");

        // Apply trained classifier to test image and get probabilities
        IJ.log("Computing maps ");        
    	ImagePlus probabilityMaps=seg.applyClassifier(img,0,true);
        probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished apply model in " + estimatedTime + " ms **" );
        seg=null;
        VitimageUtils.garbageCollector();
        return probabilityMaps;
}

    public static ImagePlus wekaApplyModelSlicePerSlice(ImagePlus img,int[]classifierParams,boolean[]enableFeatures,String modelName) {
    	System.out.println("SU 1");
    	int numTrees=classifierParams[0];
 	   int numFeatures=classifierParams[1];
 	   int seed=classifierParams[2];
 	   int minSigma=classifierParams[3];
 	   int maxSigma=classifierParams[4];
       long startTime = System.currentTimeMillis();
   	System.out.println("SU 2");
       HyperWekaSegmentation seg = new HyperWekaSegmentation(new Duplicator().run(img,1,1,1,1,1,1),null);
        // Classifier
        FastRandomForest rf = new FastRandomForest();
        rf.setNumTrees(numTrees);                  
        rf.setNumFeatures(numFeatures);  
        rf.setSeed( seed );    
        seg.setClassifier(rf);    
    	System.out.println("SU 3");
        // Parameters  
        seg.setMembranePatchSize(11);  
        seg.setMinimumSigma(minSigma);
        seg.setMaximumSigma(maxSigma);
      
        // Enable features in the segmentator
        seg.setEnabledFeatures( enableFeatures );

        // Add labeled samples in a balanced and random way
        seg.updateWholeImageData();
        System.out.println("Loading model");
        seg.loadClassifier(modelName+".model");
        System.out.println("Loaded");

        
        // Apply trained classifier to test image and get probabilities
        ImagePlus []inTab=VitimageUtils.stackToSlices(img);
        ImagePlus [][]outTab=new ImagePlus[seg.getNumOfClasses()][inTab.length];
        ImagePlus []outTabChan=new ImagePlus[seg.getNumOfClasses()];
        for(int i=0;i<inTab.length;i++) {
        	System.out.println("Applying Classifier to slice number "+i);
        	ImagePlus temp=seg.applyClassifier( inTab[i], 0, true);//Sortie : C2 Z1
        	for(int c=0;c<seg.getNumOfClasses();c++)outTab[c][i] = new Duplicator().run(temp,c+1,c+1,1,1,1,1); // sortie C x Z cases
        }
    	System.out.println("Ok");
        for(int i=0;i<seg.getNumOfClasses();i++)outTabChan[i]=VitimageUtils.slicesToStack(outTab[i]);//sortie C cases de Z stacks
        ImagePlus probabilityMaps=VitimageUtils.hyperStackingChannels(outTabChan);
        System.out.print("weka step 9  ");
        probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
        // Print elapsed time
        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished script in " + estimatedTime + " ms **" );
        inTab=null;
        outTab=null;
        outTabChan=null;
        Runtime. getRuntime(). gc();
        return probabilityMaps;
}
   
           
     public static void hyperWekaTrainModel(ImagePlus imgTemp,ImagePlus maskTemp,int[]classifierParams,boolean[]enableFeatures,String modelName) {
    	 
    	 ImagePlus img=new Duplicator().run(imgTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
    	 ImagePlus mask=new Duplicator().run(maskTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
    	 int numTrees=classifierParams[0];
 	   int numFeatures=classifierParams[1];
 	   int seed=classifierParams[2];
 	   int minSigma=classifierParams[3];
 	   int maxSigma=classifierParams[4];
 	   VitimageUtils.printImageResume(img); 
       VitimageUtils.printImageResume(mask); 
 	   Runtime. getRuntime(). gc();
         long startTime = System.currentTimeMillis();
         HyperWekaSegmentation seg = new HyperWekaSegmentation(img,null);

         // Classifier
         FastRandomForest rf = new FastRandomForest();
         rf.setNumTrees(numTrees);                  
         rf.setNumFeatures(numFeatures);  
         rf.setSeed( seed );    
         seg.setClassifier(rf);    
         // Parameters  
         seg.setMembranePatchSize(11);  
         seg.setMinimumSigma(minSigma);
         seg.setMaximumSigma(maxSigma);
   
     
         // Enable features in the segmentator
         seg.setEnabledFeatures( enableFeatures );

         // Add labeled samples in a balanced and random way

         int[]nbPix=SegmentationUtils.nbPixelsInClasses(mask);
         int min=Math.min(nbPix[0],nbPix[255]);
         int targetExamplesPerSlice=N_EXAMPLES/(2*img.getNSlices());
         System.out.println("Starting training on "+targetExamplesPerSlice+" examples per slice");
         seg.addRandomBalancedBinaryData(img, mask, "class 2", "class 1", targetExamplesPerSlice);
         seg.saveFeatureStack(1, "/home/rfernandez/Bureau/tempdata/"+new Timer().hashCode());
         
         Runtime. getRuntime(). gc();
         
         // Train classifier
         seg.trainClassifier();
         seg.saveClassifier(modelName+".model");
         // Apply trained classifier to test image and get probabilities
         //ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
         //probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
         // Print elapsed time
         long estimatedTime = System.currentTimeMillis() - startTime;
         IJ.log( "** Finished script in " + estimatedTime + " ms **" );
         seg=null;
 		Runtime.getRuntime().gc();
     }
             
     public static ImagePlus hyperWekaApplyModel(ImagePlus img,int []classifierParams,boolean[]enableFeatures,String modelName) {
  	   int numTrees=classifierParams[0];
  	   int numFeatures=classifierParams[1];
  	   int seed=classifierParams[2];
  	   int minSigma=classifierParams[3];
  	   int maxSigma=classifierParams[4];
         long startTime = System.currentTimeMillis();
         System.out.print("weka step 1   ");
         HyperWekaSegmentation seg = new HyperWekaSegmentation(img,null);
         System.out.print("weka step 2   ");

         // Classifier
         FastRandomForest rf = new FastRandomForest();
         rf.setNumTrees(300);                  
         rf.setNumFeatures(14);  
         rf.setSeed( seed );    
         System.out.print("weka step 3  ");
         seg.setClassifier(rf);    
         // Parameters  
         System.out.print("weka step 4  ");
         seg.setMembranePatchSize(11);  
         seg.setMinimumSigma(minSigma);
         seg.setMaximumSigma(maxSigma);
         System.out.print("weka step 5  ");
   
      
         // Enable features in the segmentator
         seg.setEnabledFeatures( enableFeatures );
         System.out.print("weka step 6  ");

         // Add labeled samples in a balanced and random way
         seg.updateWholeImageData();
         System.out.print("weka step 65  ");
         seg.loadClassifier(modelName+".model");
         VitimageUtils.garbageCollector();
         System.out.print("weka step 7  ");
         // Train classifier

         // Apply trained classifier to test image and get probabilities
         ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
         System.out.print("weka step 9  ");
         probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
         // Print elapsed time
         long estimatedTime = System.currentTimeMillis() - startTime;
         IJ.log( "** Finished script in " + estimatedTime + " ms **" );
         seg=null;
         Runtime.getRuntime().gc();
         return probabilityMaps;
 }

     public static ImagePlus hyperWekaApplyModelSlicePerSlice(ImagePlus img,int[]classifierParams,boolean[]enableFeatures,String modelName) {
  	   int numTrees=classifierParams[0];
  	   int numFeatures=classifierParams[1];
  	   int seed=classifierParams[2];
  	   int minSigma=classifierParams[3];
  	   int maxSigma=classifierParams[4];
        long startTime = System.currentTimeMillis();
        HyperWekaSegmentation seg = new HyperWekaSegmentation(new Duplicator().run(img,1,1,1,1,1,1),null);
         // Classifier
         FastRandomForest rf = new FastRandomForest();
         rf.setNumTrees(numTrees);                  
         rf.setNumFeatures(numFeatures);  
         rf.setSeed( seed );    
         seg.setClassifier(rf);    
         // Parameters  
         seg.setMembranePatchSize(11);  
         seg.setMinimumSigma(minSigma);
         seg.setMaximumSigma(maxSigma);
       
         // Enable features in the segmentator
         seg.setEnabledFeatures( enableFeatures );

         // Add labeled samples in a balanced and random way
         seg.updateWholeImageData();
         System.out.println("Loading model");
         seg.loadClassifier(modelName+".model");
         System.out.println("Loaded");

         
         // Apply trained classifier to test image and get probabilities
         ImagePlus []inTab=VitimageUtils.stackToSlices(img);
         ImagePlus [][]outTab=new ImagePlus[seg.getNumOfClasses()][inTab.length];
         ImagePlus []outTabChan=new ImagePlus[seg.getNumOfClasses()];
         for(int i=0;i<inTab.length;i++) {
         	System.out.println("Applying Classifier to slice number "+i);
         	ImagePlus temp=seg.applyClassifier( inTab[i], 0, true);//Sortie : C2 Z1
         	for(int c=0;c<seg.getNumOfClasses();c++)outTab[c][i] = new Duplicator().run(temp,c+1,c+1,1,1,1,1); // sortie C x Z cases
         }
     	System.out.println("Ok");
         for(int i=0;i<seg.getNumOfClasses();i++)outTabChan[i]=VitimageUtils.slicesToStack(outTab[i]);//sortie C cases de Z stacks
         ImagePlus probabilityMaps=VitimageUtils.hyperStackingChannels(outTabChan);
         System.out.print("weka step 9  ");
         probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
         // Print elapsed time
         long estimatedTime = System.currentTimeMillis() - startTime;
         IJ.log( "** Finished script in " + estimatedTime + " ms **" );
         inTab=null;
         outTab=null;
         outTabChan=null;
         Runtime. getRuntime(). gc();
         return probabilityMaps;
 }
   
    
    
    /** Routines for data augmentation --------------------------------------------------------------------------------------------*/        
	public static ImagePlus[] rotationAugmentationStack(ImagePlus imgIn,ImagePlus maskIn,double probaRotation,int repetitions,int seed){
		int N=imgIn.getNSlices();
		ImagePlus []tabSlicesOut=new ImagePlus[repetitions*N];
		ImagePlus[]tab2=VitimageUtils.stackToSlices(imgIn);
		ImagePlus []tabSlicesOutMask=new ImagePlus[repetitions*N];
		ImagePlus[]tab2Mask=VitimageUtils.stackToSlices(maskIn);
		for(int i=0;i<repetitions;i++) {
			for(int j=0;j<N;j++) {
				tabSlicesOut[(i)*N+j]=tab2[j].duplicate();
				tabSlicesOutMask[(i)*N+j]=tab2Mask[j].duplicate();
			}
		}
		
		Random rand=new Random(seed);
		for(int i=0;i<tabSlicesOut.length;i++) {
			double p=rand.nextDouble();
			tabSlicesOut[i].show();
			if(p<probaRotation)IJ.run("Rotate 90 Degrees Right");
			tabSlicesOut[i].hide();
			tabSlicesOutMask[i].show();
			if(p<probaRotation)IJ.run("Rotate 90 Degrees Right");
			tabSlicesOutMask[i].hide();
		}
		return new ImagePlus[] {VitimageUtils.slicesToStack(tabSlicesOut),VitimageUtils.slicesToStack(tabSlicesOutMask)};
	}
		
	public static ImagePlus colorAugmentationStack(ImagePlus imgIn,boolean isMask,int nMult,double std,boolean keepOriginal){
		int N=imgIn.getNSlices();
		double mean=1;
		int frequency=5;
		ImagePlus []tabOut=new ImagePlus[nMult];
		ImagePlus []tabSlicesOut=new ImagePlus[N*nMult];
		int i=0;
		for(int iMult=0;iMult<nMult;iMult++) {
			System.out.println("Processing Aug "+iMult);
			tabOut[iMult]=imgIn.duplicate();
			if(isMask ||  (iMult==0 && keepOriginal)) {
				for(int j=0;j<N;j++) {
					ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
					//System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
					tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
				}
				continue;
			}
			System.out.println("Hre 0");
			ImagePlus augmentationMapR=getAugmentationMap(imgIn,mean,std,frequency);
			System.out.println("Hre 00");
			ImagePlus augmentationMapG=getAugmentationMap(imgIn,mean,std,frequency);
			System.out.println("Hre 000");
			ImagePlus augmentationMapB=getAugmentationMap(imgIn,mean,std,frequency);
			System.out.println("Hre 0000");
			tabOut[iMult].setTitle("temp");
			tabOut[iMult].show();
			IJ.selectWindow("temp");
			IJ.run("Split Channels");
			tabOut[iMult].changes=false;
			tabOut[iMult].close();
			System.out.println("Hre 01");
			ImagePlus imgR=ij.WindowManager.getImage("temp (red)");
			ImagePlus imgG=ij.WindowManager.getImage("temp (green)");
			ImagePlus imgB=ij.WindowManager.getImage("temp (blue)");
			System.out.println("H1");
			ImagePlus imgR2=VitimageUtils.makeOperationBetweenTwoImages(imgR, augmentationMapR, 2, false);
			ImagePlus imgG2=VitimageUtils.makeOperationBetweenTwoImages(imgG, augmentationMapG, 2, false);
			ImagePlus imgB2=VitimageUtils.makeOperationBetweenTwoImages(imgB, augmentationMapB, 2, false);
			imgR.close();imgG.close();imgB.close();imgR2.show();imgG2.show();imgB2.show();imgR2.setTitle("temp (red)");imgG2.setTitle("temp (green)");imgB2.setTitle("temp (blue)");
			System.out.println("Hre 03");

			IJ.run("Merge Channels...", "c1=[temp (red)] c2=[temp (green)] c3=[temp (blue)] create");
			IJ.run("Stack to RGB", "slices keep");
			tabOut[iMult]=IJ.getImage();
			ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
			for(int j=0;j<N;j++) {
				//System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
				tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
			}
			System.out.println("Hre 04");
			WindowManager.getImage("Composite").close();
			WindowManager.getImage("Composite-1").close();
			System.out.println("H2");
		}
		Runtime. getRuntime(). gc();
		System.out.println("Assembling tab of "+tabSlicesOut.length);
		return VitimageUtils.slicesToStack(tabSlicesOut);
	}
		
	public static double[]getMassCenter(ImagePlus img){
		if(img.getNSlices()>1)return null;
		ImagePlus img1=img.duplicate();
		IJ.run(img1,"32-bit","");
		double xTot=0;
		double yTot=0;
		int nHits=0;
		int X=img1.getWidth(); int Y=img1.getHeight();
		float[]tabImg1=(float[])img1.getStack().getPixels(1);
		for(int x=0;x<X;x++) for(int y=0;y<Y;y++){
			if(tabImg1[y*X+x]>0) {
				xTot+=x;
				yTot+=y;
				nHits++;
			}
		}
		return new double[] {xTot/nHits,yTot/nHits};
	}

	
	public static ImagePlus[]getMaxOfProba(ImagePlus img1,ImagePlus img2){
		int X=img1.getWidth(); int Y=img1.getHeight();int Z=img1.getNSlices();
		ImagePlus res1=img1.duplicate();
		ImagePlus res2=img2.duplicate();
		for(int z=0;z<Z;z++) {
			float[]tabImg1=(float[])img1.getStack().getPixels(z+1);
			float[]tabImg2=(float[])img2.getStack().getPixels(z+1);
			float[]tabRes1=(float[])res1.getStack().getPixels(z+1);
			float[]tabRes2=(float[])res2.getStack().getPixels(z+1);
			for(int x=0;x<X;x++) for(int y=0;y<Y;y++){
				if(tabImg1[y*X+x]>tabImg2[y*X+x]) {tabRes1[y*X+x]=1;tabRes2[y*X+x]=0;}
				else {tabRes2[y*X+x]=1;tabRes1[y*X+x]=0;}
			}			
		}
		res1.setDisplayRange(0, 1);
		res2.setDisplayRange(0, 1);
		return new ImagePlus[] {res1,res2};
	}
	
	public static ImagePlus brightnessAugmentationStack(ImagePlus imgIn,boolean isMask,int nMult,double std,boolean keepOriginal){
		int N=imgIn.getNSlices();
		double mean=1;
		int frequency=5;
		ImagePlus []tabOut=new ImagePlus[nMult];
		ImagePlus []tabSlicesOut=new ImagePlus[N*nMult];
		int i=0;
		for(int iMult=0;iMult<nMult;iMult++) {
			System.out.println("T1");VitimageUtils.waitFor(1000);
			System.out.println("Processing brightness "+iMult);
			tabOut[iMult]=imgIn.duplicate();
			if(isMask || (iMult==0 && keepOriginal)) {
				for(int j=0;j<N;j++) {
					ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
					System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
					tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
				}
				continue;
			}
			System.out.println("T2");VitimageUtils.waitFor(1000);
			ImagePlus augmentationMap=getAugmentationMap(imgIn,mean,std,frequency);
			tabOut[iMult].setTitle("temp");
			tabOut[iMult].show();
			IJ.selectWindow("temp");
			IJ.run("Split Channels");
			System.out.println("T3");VitimageUtils.waitFor(1000);
			tabOut[iMult].close();
			ImagePlus imgR=ij.WindowManager.getImage("temp (red)");
			ImagePlus imgG=ij.WindowManager.getImage("temp (green)");
			ImagePlus imgB=ij.WindowManager.getImage("temp (blue)");
			
			ImagePlus imgR2=VitimageUtils.makeOperationBetweenTwoImages(imgR, augmentationMap, 2, false);
			ImagePlus imgG2=VitimageUtils.makeOperationBetweenTwoImages(imgG, augmentationMap, 2, false);
			ImagePlus imgB2=VitimageUtils.makeOperationBetweenTwoImages(imgB, augmentationMap, 2, false);
			imgR.close();imgG.close();imgB.close();imgR2.show();imgG2.show();imgB2.show();imgR2.setTitle("temp (red)");imgG2.setTitle("temp (green)");imgB2.setTitle("temp (blue)");
			System.out.println("T4");VitimageUtils.waitFor(1000);

			IJ.run("Merge Channels...", "c1=[temp (red)] c2=[temp (green)] c3=[temp (blue)] create");
			IJ.run("Stack to RGB", "slices keep");
			tabOut[iMult]=IJ.getImage();
			ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
			System.out.println("T5");VitimageUtils.waitFor(1000);
			for(int j=0;j<N;j++) {
				System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
				tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
			}
			WindowManager.getImage("Composite").close();
			WindowManager.getImage("Composite-1").close();
		}
		System.out.println("T6");VitimageUtils.waitFor(1000);
		Runtime. getRuntime(). gc();
		System.out.println("Assembling tab of "+tabSlicesOut.length);
		return VitimageUtils.slicesToStack(tabSlicesOut);
	}

	
	public static ImagePlus brightnessAugmentationStackGrayScale(ImagePlus imgIn,boolean isMask,int nMult,double std,boolean keepOriginal){
		int N=imgIn.getNSlices();
		double mean=1;
		int frequency=5;
		ImagePlus []tabOut=new ImagePlus[nMult];
		ImagePlus []tabSlicesOut=new ImagePlus[N*nMult];
		int i=0;
		for(int iMult=0;iMult<nMult;iMult++) {
			System.out.println("T1");VitimageUtils.waitFor(1000);
			System.out.println("Processing brightness "+iMult);
			tabOut[iMult]=imgIn.duplicate();
			if(isMask || (iMult==0 && keepOriginal)) {
				for(int j=0;j<N;j++) {
					ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
					System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
					tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
				}
				continue;
			}
			ImagePlus augmentationMap=getAugmentationMap(imgIn,mean,std,frequency);
			tabOut[iMult]=VitimageUtils.makeOperationBetweenTwoImages(imgIn, augmentationMap, 2, false);
			ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
			System.out.println("T5");VitimageUtils.waitFor(1000);
			for(int j=0;j<N;j++) {
				System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
				tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
			}
		}
		Runtime. getRuntime(). gc();
		System.out.println("Assembling tab of "+tabSlicesOut.length);
		return VitimageUtils.slicesToStack(tabSlicesOut);
	}


	
	public static ImagePlus monoAugmentationMap(ImagePlus imgIn, double mean, double std) {
		ImagePlus img=imgIn.duplicate();
		IJ.run(img,"32-bit","");
		int Z=imgIn.getNSlices();
    	int X=imgIn.getWidth();
    	int Y=imgIn.getHeight();
		Random rand=new Random();
    	for(int z=0;z<Z;z++) {
    		float[] tab=(float[])img.getStack().getProcessor(z+1).getPixels();
    		float val=(float)(rand.nextGaussian()*std+mean);
        	for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
        		tab[x*Y+y]=val;
        	}
    	}
    	return img;
	}
	
    public static ImagePlus getAugmentationMap(ImagePlus imgIn,double mean,double std,int frequency) {
    	int Z=imgIn.getNSlices();
    	int X=imgIn.getWidth();
    	int Y=imgIn.getHeight();

    	if(X<120) {
    		ImagePlus ret=monoAugmentationMap(imgIn,mean, std);
    		return ret;
    	}
    	System.out.println("X="+X);
    	ImagePlus []slices=new ImagePlus[Z];
    	ImagePlus sliceExample=new Duplicator().run(imgIn,1,1,1,1,1,1);
    	Random rand=new Random();
    	int[][]coordinates=new int[frequency*frequency][3];
    	double []values=new double[frequency*frequency];
    	for(int x=0;x<frequency;x++)for(int y=0;y<frequency;y++) {
    		coordinates[x*frequency+y]=new int[] {
    				(int)Math.round(((x+0.5)*X*1.0)/frequency),
    				(int)Math.round(((y+0.5)*Y*1.0)/frequency),
    				0
    		};
    		//System.out.println(TransformUtils.stringVector(coordinates[x*frequency+y],""));
    	}
    	for(int z=0;z<Z;z++) {
    		for(int x=0;x<frequency;x++)for(int y=0;y<frequency;y++)values[x*frequency+y]=rand.nextGaussian()*std+mean;
    		slices[z]=ItkTransform.smoothImageFromCorrespondences(coordinates,values, sliceExample,X/frequency,false);
    		VitimageUtils.waitFor(100);
    	}
    	return VitimageUtils.slicesToStack(slices);
    	
    }
     
          

	
	
	
	
	
	

    /** Helpers for conversion to / from Json , to / from Roi[] , to / from binary segmentation  -------------- */
    public static ImagePlus []jsonToBinary(String dir) {
            System.out.println(dir);
            
            String[]listFiles=new File(dir).list();
            System.out.println(listFiles.length);
            int nImgs=listFiles.length-1;
            String s="";
            for(String s2 : listFiles) {
                if(s2.contains(".json"))s=s2;
            }
            
            String[][][] tabData=convertJsonToRoi(new File(dir,s).getAbsolutePath(),true);
            ImagePlus[]imgInit=new ImagePlus[nImgs];
            ImagePlus[]imgMask=new ImagePlus[nImgs];
            
            for(int indImg=0;indImg<tabData.length;indImg++) {
                   // System.out.println("Opening image "+new File(dir,tabData[indImg][0][0]).getAbsolutePath());
                    imgInit[indImg]=IJ.openImage(new File(dir,tabData[indImg][0][0]).getAbsolutePath());
                    imgMask[indImg]=imgInit[indImg].duplicate();
                    IJ.run(imgMask[indImg],"8-bit","");
                    imgMask[indImg]=VitimageUtils.nullImage(imgMask[indImg]);
                    imgMask[indImg].show();
                    for(int indRoi=0;indRoi<tabData[indImg].length;indRoi++) {
                            Roi r=roiParser(tabData[indImg][indRoi][1],tabData[indImg][indRoi][2]);
                            imgMask[indImg].setRoi(r);
                            IJ.run("Fill", "slice");
                    }
                    imgMask[indImg].hide();
            }
            ImagePlus imgInitFull=VitimageUtils.slicesToStack(imgInit);
            ImagePlus imgMaskFull=VitimageUtils.slicesToStack(imgMask);
            return new ImagePlus[] {imgInitFull,imgMaskFull};
    }
  
    
    public static void binarySlicesToJson(String dirSourceIn,String jsonPath){
    	
    }
    
    public static void binarySlicesToCsv(String dirSourceIn,String jsonPath){
    	//TODO
    }

    public static void csvToBinarySlices(String dirSourceIn,String jsonPath){
    	
    }

    public static void csvToJson(String dirSourceIn,String jsonPath){
    	//TODO    	
    }
    

    public static void jsonToCsv(String dirSourceIn,String jsonPath){
    	
    }

    
    public static void jpgToTiff(String dirSourceIn,String dirSourceOut) {
        String[]listFiles=new File(dirSourceIn).list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.contains(".jpg") || name.contains(".jpeg"));
			}
		});
		for (String file : listFiles) {
			ImagePlus img=IJ.openImage(new File(dirSourceIn,file).getAbsolutePath());
			String newName=VitimageUtils.withoutExtension(file)+".tif";
			IJ.saveAsTiff(img,new File(dirSourceOut,newName).getAbsolutePath());
		}
    }

    public static void tiffToJpeg(String dirSourceIn,String dirSourceOut) {
        String[]listFiles=new File(dirSourceIn).list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.contains(".tif") || name.contains(".tiff"));
			}
		});
		for (String file : listFiles) {
			ImagePlus img=IJ.openImage(new File(dirSourceIn,file).getAbsolutePath());
			String newName=VitimageUtils.withoutExtension(file)+".jpg";
			IJ.saveAsTiff(img,new File(dirSourceOut,newName).getAbsolutePath());
		}    	
    }
   
    public static void jsonToBinarySlices(String jsonPath,String dirSourceIn,String dirSegOut,boolean resizeLow) {
        System.out.println(dirSourceIn);
        String[]listFiles=new File(dirSourceIn).list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.contains(".csv")|| name.contains(".json"))return false;
				return true;
			}
		});
        System.out.println(listFiles.length);
        int nImgs=listFiles.length;
        
        String[][][] tabData=convertJsonToRoi(jsonPath,true);
        ImagePlus[]imgMask=new ImagePlus[nImgs];
        
        for(int indImg=0;indImg<tabData.length;indImg++) {
        	    //System.out.println("Opening image "+new File(dirSourceIn,tabData[indImg][0][0]).getAbsolutePath());
                imgMask[indImg]=IJ.openImage(new File(dirSourceIn,tabData[indImg][0][0]).getAbsolutePath());
                IJ.run(imgMask[indImg],"8-bit","");
                imgMask[indImg]=VitimageUtils.nullImage(imgMask[indImg]);
                
                if(tabData[indImg][0].length>1) {
	                imgMask[indImg].show();
	                for(int indRoi=0;indRoi<tabData[indImg].length;indRoi++) {
	                        Roi r=roiParser(tabData[indImg][indRoi][1],tabData[indImg][indRoi][2]);
	                        imgMask[indImg].setRoi(r);
	                        IJ.run("Colors...", "foreground=white background=black selection=yellow");
	                        IJ.run("Fill", "slice");
	                        imgMask[indImg].resetRoi();
	                }
	                imgMask[indImg].hide();
	                if(resizeLow) {
	                	ImagePlus img= imgMask[indImg].duplicate();
	                	VitimageUtils.printImageResume(img);
	                	imgMask[indImg]=resizeNearest(img, img.getWidth()/8,img.getHeight()/8,1);
	                	imgMask[indImg]=VitimageUtils.thresholdByteImage(imgMask[indImg], 127, 256);
	                }
                }
                IJ.save(imgMask[indImg], new File(dirSegOut,"Segmentation_"+VitimageUtils.withoutExtension(tabData[indImg][0][0])+".tif").getAbsolutePath());
        }
    }

	public static ImagePlus subscaling2D(ImagePlus img, int subscaling) {
		int X=img.getWidth();
		int Y=img.getHeight();
        return Scaler.resize(img, X/subscaling,Y/subscaling, img.getNSlices(), " interpolation=Bilinear average create"); 		
	}

    
	public static ImagePlus resize(ImagePlus img, int targetX,int targetY,int targetZ) {
        return Scaler.resize(img, targetX,targetY, targetZ, " interpolation=Bilinear average create"); 		
	}
	public static ImagePlus resizeNearest(ImagePlus img, int targetX,int targetY,int targetZ) {
        return Scaler.resize(img, targetX,targetY, targetZ, "none"); 		
	}

    
    public static String[][][]convertJsonToRoi(String jsonPath,boolean warning){
            String bag="val";
            String conf="2048";
            int fact=2;
            String pathIn=jsonPath;
            JSONObject jo = new JSONObject(VitimageUtils.readStringFromFile(pathIn));
            Iterator <String>iter=jo.keys();
            String[][][] ret=new String[jo.length()][3][];
            int indexImg=-1;

            //Iterate over images
           while(iter.hasNext()) {
                String key=(String)iter.next();
                JSONObject jo2=jo.getJSONObject(key);
                Iterator <String>iter2=jo2.keys();
                int count=0;
               
                while(iter2.hasNext()) {
                    String key2=(String)iter2.next();
                    if(key2.equals("regions")){
                        JSONArray jo3=jo2.getJSONArray(key2);
                        count=jo3.length();
                        if(jo3.length()==0) {ret[++indexImg]=new String[1][1];ret[indexImg][0][0]=key.split(".jpg")[0]+".jpg";}
                        else ret[++indexImg]=new String[jo3.length()][3];
                       //Iterate over regions
                        int indexReg=-1;
                        for(int i=0;i<jo3.length();i++) {
                            ret[indexImg][++indexReg][0]=key.split(".jpg")[0]+".jpg";//img name
                            JSONObject jo4=jo3.getJSONObject(i);
                            Iterator <String>iter4=jo4.keys();
                            while(iter4.hasNext()) {
                                String key4=(String)iter4.next();
                                if(key4.equals("shape_attributes")){
                                    JSONObject jo5=jo4.getJSONObject(key4);
                                    Iterator <String>iter5=jo5.keys();
                                   // System.out.println();
                                    while(iter5.hasNext()) {
                                        String key5=(String)iter5.next();
                                        if(key5.equals("all_points_x")){
                                    		count++;
                                            JSONArray tabX=(JSONArray) jo5.get(key5);
                                            String data=tabX.join(",");
                                            //System.out.println("PtX="+data);
                                            ret[indexImg][indexReg][1]=(data);
                                        }
                                        if(key5.equals("all_points_y")){
                                            JSONArray tabY=(JSONArray) jo5.get(key5);
                                            String data=tabY.join(",");
                                            //System.out.println("PtY="+data);
                                            ret[indexImg][indexReg][2]=(data);
                                        }
                                    }        
                                }
                            }
                            //System.out.println("   "+ret[indexImg][indexReg][0]+"   "+ret[indexImg][indexReg][1]+"   "+ret[indexImg][indexReg][2]);
                        } 
                           
                    }
                }                        
                System.out.println("Img "+key +" has "+count+" regions");
                
            }
            return ret;
    }                

    public static Roi roiParser(String xcoords,String ycoords) {
        int[]tabX=stringTabToIntTab(xcoords.split(","));
        int[]tabY=stringTabToIntTab(ycoords.split(","));
        return new PolygonRoi(tabX, tabY, tabX.length,Roi.POLYGON);
	  }
    
    public static int[]stringTabToIntTab(String[]tab){
        int[]ret=new int[tab.length];
        for(int i=0;i<tab.length;i++)ret[i]=Integer.parseInt(tab[i]);
        return ret;
    }

    public static Roi[]segmentationToRoi(ImagePlus seg){
    	ImagePlus imgSeg=VitimageUtils.getBinaryMask(seg, 0.5);
    	RoiManager rm=RoiManager.getRoiManager();
    	rm.reset();
    	imgSeg.show();
    	imgSeg.resetRoi();
    	IJ.setRawThreshold(imgSeg, 127, 255, null);
        VitimageUtils.waitFor(10);
    	
        IJ.run("Create Selection");
        //VitimageUtils.printImageResume(IJ.getImage(),"getImage");
        Roi r=IJ.getImage().getRoi();
       // System.out.println(r);
        if(r==null)return null;
        Roi[]rois;
        if(r.getClass()==PolygonRoi.class)rois=new Roi[] {r};
        else rois = ((ShapeRoi)r).getRois();
        IJ.getImage().close();
        rm.close();
        return rois;
    }
    

    public static ImagePlus selectCentralMostRoi(ImagePlus img) {
    	ImagePlus[]imgs=VitimageUtils.stackToSlices(img);
    	int Xmid=img.getWidth()/2;
    	int Ymid=img.getHeight()/2;
    	for(int i=0;i<imgs.length;i++) {
    		Roi[]rois=segmentationToRoi(imgs[i]);
    		if (rois==null || rois.length==0) {continue;}
    		double distMax=10E8;
    		int indexMax=0;
    		for(int ind=0;ind<rois.length;ind++) {
    			double[]coords=rois[ind].getContourCentroid();
    			double dist=(Xmid-coords[0])*(Xmid-coords[0])+(Ymid-coords[1])*(Ymid-coords[1]);
    			if(dist<distMax) {
    				distMax=dist;indexMax=ind;
    			}
    		}
    		imgs[i]=roiToSegmentation(imgs[i],rois[indexMax]);
    	}
    	VitimageUtils.printImageResume(imgs[0]);
    	return VitimageUtils.slicesToStack(imgs);
    }
    
    
	/*** Helpers for preparation of data (Roi, images) ---------------------------------------------------*/
	public static Roi[]pruneRoi(Roi[]roiTab,int targetResolution){
		int maxPossible=targetResolution-3;
		int minPossible=2;
		boolean []take=new boolean[roiTab.length];
		int select=0;
		for(int i=0;i<roiTab.length;i++) {
			take[i]=true;
			Roi r=roiTab[i];
			Polygon p=r.getPolygon();
			for(int val:p.xpoints)if(val<minPossible || val>maxPossible)take[i]=false;
			for(int val:p.ypoints)if(val<minPossible || val>maxPossible)take[i]=false;
			if(take[i])select++;
		}
		Roi[]tabOut=new Roi[select];
		int incr=0;
		for(int i=0;i<roiTab.length;i++) {
			if(take[i]) {
				tabOut[incr]=roiTab[i];
				incr++;
			}
		}
		//System.out.println("Prune : in="+roiTab.length+" , out="+tabOut.length);
		return tabOut;			
	}

    public static void resampleJsonAndImageSet(String dirIn,String dirOut,int resampleFactor) {
        String pathJsonIn=new File(dirIn,"via_region_data.json").getAbsolutePath();                                
        double fact=1.0/resampleFactor;

        String[]listImages=new File(dirIn).list();
        for(String s:listImages) {
                if(s.contains(".jpg")) {
                        ImagePlus img=IJ.openImage(new File(dirIn,s).getAbsolutePath());
                        int targetSize=img.getWidth()/resampleFactor;
                        img.show();
                        IJ.run("Scale...", "x="+fact+" y="+fact+" width="+targetSize+" height="+targetSize+" interpolation=Bilinear average create");//create
                        img=IJ.getImage();
                        IJ.save(img,new File(dirOut,s).getAbsolutePath());
                        img.changes=false;
                        img.close();
                        img=IJ.getImage();
                        img.changes=false;
                        img.close();
                }
        }
        String pathJsonOut=new File(dirOut,"via_region_data.json").getAbsolutePath();
        String dataIn=VitimageUtils.readStringFromFile(pathJsonIn);
        String data2=dataIn.replace("\"all_points_x\":[","PPTTXX\n").replace("\"all_points_y\":[","PPTTYY\n");
        String[]data3=data2.split("\n");
        String data4=data3[0];
        for(int lig=1;lig<data3.length;lig++) {
                int indCar=data3[lig].indexOf("]");
                String toReplace=data3[lig].substring(0, indCar);
                String[]numbers=toReplace.split(",");
                String replacing="";
                for(int ind=0;ind<numbers.length;ind++) {
                        int nb=(int)Math.round(( (Integer.parseInt(numbers[ind]))*1.0)/resampleFactor);
                        replacing+=""+nb;
                        if(ind<numbers.length-1)replacing+=",";
                }
                data3[lig].replace(toReplace, replacing);
                data4+=replacing+data3[lig].substring(indCar)+(lig<data3.length-1 ? "\n" : "");
        }
        String data5=data4.replace("PPTTXX", "\"all_points_x\":[").replace("PPTTYY", "\"all_points_y\":[").replace("\n","");
        VitimageUtils.writeStringInFile(data5, pathJsonOut);
}

	public static ImagePlus cleanVesselSegmentation(ImagePlus seg,int targetResolution,int minNbVox,int maxNbVox) {
			double voxVol=VitimageUtils.getVoxelVolume(seg);
			double minVBsurface=voxVol*minNbVox;
			double maxVBsurface=voxVol*maxNbVox;

			//Remplir les trous inutiles
			ImagePlus imgSeg=VitimageUtils.getBinaryMask(seg, 0.5);
			imgSeg.show();
			//VitimageUtils.waitFor(3000);
			//IJ.run("Fill Holes", "stack");
			//VitimageUtils.waitFor(3000);
			ImagePlus test1=imgSeg.duplicate();
			imgSeg.hide();
			//Retirer les cellules plus petites que et plus grandes que
			imgSeg=VitimageUtils.connexe2d(imgSeg, 1,256, minVBsurface, maxVBsurface , 6,0,true);
			ImagePlus test2=imgSeg.duplicate();
			return VitimageUtils.getBinaryMask(imgSeg,0.5);
		}
    
    public static double getRoiSurface(Roi r) {
        int x0=(int)Math.floor(r.getBounds().getMinX());
        int x1=(int)Math.floor(r.getBounds().getMaxX());
        int y0=(int)Math.floor(r.getBounds().getMinY());
        int y1=(int)Math.floor(r.getBounds().getMaxY());
        int inter=0;
        for(int x=x0;x<=x1;x++) {
            for(int y=y0;y<=y1;y++) {
                if(r.contains(x, y))inter++;
            }
        }
        return inter;
   	
    }
        
	public static int[]nbPixelsInClasses(ImagePlus img){
        int[]tab=img.getStack().getProcessor(1).getHistogram();
        return tab;
    }
       
	public static ImagePlus getBinaryMaskUnary(ImagePlus img,double threshold) {
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		int type=(img.getType()==ImagePlus.GRAY8 ? 8 : img.getType()==ImagePlus.GRAY16 ? 16 : img.getType()==ImagePlus.GRAY32 ? 32 : 24);
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		if(type==8) {
			for(int z=0;z<dimZ;z++) {
				byte []tabImg=(byte[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x] & 0xff) >= (byte)(((int)Math.round(threshold)) & 0xff)  )tabRet[dimX*y+x]=(byte)(1 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else if(type==16) {
			for(int z=0;z<dimZ;z++) {
				short []tabImg=(short[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x] & 0xffff) >= (short)(((int)Math.round(threshold)) & 0xffff)  )tabRet[dimX*y+x]=(byte)(1 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else if(type==32) {
			for(int z=0;z<dimZ;z++) {
				float []tabImg=(float[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x]) >= threshold )tabRet[dimX*y+x]=(byte)(1 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else VitiDialogs.notYet("getBinary Mask type "+type);
		return ret;
	}
	

	public static int NUM_TREES=200;
    public static int NUM_FEATS=14;//sqrt (#feat)
    public static int MIN_SIGMA=2;
    public static int MAX_SIGMA=16;//32
    public static int N_EXAMPLES=200000;//1M
    public static boolean debugTrain=false;
    
    public static int[]getStandardRandomForestParams(int seed){
	   return new int[] {NUM_TREES,NUM_FEATS,seed,MIN_SIGMA,MAX_SIGMA};
    }
    
    public static int[]getStandardRandomForestParamsVessels(int seed){
	   return new int[] {NUM_TREES,NUM_FEATS,seed,MIN_SIGMA*2,MAX_SIGMA*4};
    }
 
    public static int[]getStandardRandomForestParamsVesselsSub(int seed){
	   return new int[] {100,10,seed,2,32};
    }
    public static int[]getStandardRandomForestParamsVesselsSubSub(int seed){
 	   return new int[] {80,8,seed,1,16};
     }
 
    public static boolean[]getShortRandomForestFeatures(){
     	return new boolean[]{
        true,   /* Gaussian_blur */
        true,   /* Sobel_filter */
        false,   /* Hessian */
        false,   /* Difference_of_gaussians */
        false,   /* Membrane_projections */
        true,  /* Variance */
        false,  /* Mean */
        true,  /* Minimum */
        true,  /* Maximum */
        true,  /* Median */
        false,  /* Anisotropic_diffusion */
        false,  /* Bilateral */
        false,  /* Lipschitz */
        false,  /* Kuwahara */
        false,  /* Gabor */
        true,  /* Derivatives */
        true,  /* Laplacian */
        false,  /* Structure */
        false,  /* Entropy */
        false   /* Neighbors */
	};
    }

    public static boolean[]getStandardRandomForestFeatures(){
     	return new boolean[]{
        true,   /* Gaussian_blur */
        true,   /* Sobel_filter */
        true,   /* Hessian */
        true,   /* Difference_of_gaussians */
        false,   /* Membrane_projections */
        true,  /* Variance */
        true,  /* Mean */
        true,  /* Minimum */
        true,  /* Maximum */
        true,  /* Median */
        false,  /* Anisotropic_diffusion */
        false,  /* Bilateral */
        false,  /* Lipschitz */
        false,  /* Kuwahara */
        false,  /* Gabor */
        true,  /* Derivatives */
        true,  /* Laplacian */
        false,  /* Structure */
        false,  /* Entropy */
        false   /* Neighbors */
	};
}

    public static boolean[]getStandardRandomForestFeaturesVessels(){
     	return new boolean[]{
        true,   /* Gaussian_blur */
        false,   /* Sobel_filter */
        true,   /* Hessian */
        true,   /* Difference_of_gaussians */
        false,   /* Membrane_projections */
        true,  /* Variance */
        false,  /* Mean */
        true,  /* Minimum */
        true,  /* Maximum */
        true,  /* Median */
        false,  /* Anisotropic_diffusion */
        false,  /* Bilateral */
        false,  /* Lipschitz */
        false,  /* Kuwahara */
        false,  /* Gabor */
        true,  /* Derivatives */
        true,  /* Laplacian */
        false,  /* Structure */
        false,  /* Entropy */
        false   /* Neighbors */
     	};
    }
    
    
    public static boolean[]getStandardRandomForestFeaturesVesselsSub(){
     	return new boolean[]{
        true,   /* Gaussian_blur */
        false,   /* Sobel_filter */
        true,   /* Hessian */
        true,   /* Difference_of_gaussians */
        false,   /* Membrane_projections */
        true,  /* Variance */
        false,  /* Mean */
        true,  /* Minimum */
        true,  /* Maximum */
        true,  /* Median */
        false,  /* Anisotropic_diffusion */
        false,  /* Bilateral */
        false,  /* Lipschitz */
        false,  /* Kuwahara */
        false,  /* Gabor */
        true,  /* Derivatives */
        true,  /* Laplacian */
        false,  /* Structure */
        false,  /* Entropy */
        false   /* Neighbors */
     	};
    }

 
    
    public static boolean[]getStandardRandomForestFeaturesVesselsSubSub(){
     	return new boolean[]{
        true,   /* Gaussian_blur */
        false,   /* Sobel_filter */
        true,   /* Hessian */
        true,   /* Difference_of_gaussians */
        false,   /* Membrane_projections */
        true,  /* Variance */
        false,  /* Mean */
        true,  /* Minimum */
        true,  /* Maximum */
        true,  /* Median */
        false,  /* Anisotropic_diffusion */
        false,  /* Bilateral */
        false,  /* Lipschitz */
        false,  /* Kuwahara */
        false,  /* Gabor */
        true,  /* Derivatives */
        true,  /* Laplacian */
        false,  /* Structure */
        false,  /* Entropy */
        false   /* Neighbors */
     	};
    }

    
    public static ImagePlus fillHoles2D(ImagePlus imgIn) {
    	if(VitimageUtils.isNullImage(imgIn))return imgIn;
		ImagePlus img=imgIn.duplicate();
		img.setDisplayRange(0, 255);
		img=VitimageUtils.invertBinaryMask(img);
		IJ.run(img,"Fill Holes","");
		img=VitimageUtils.invertBinaryMask(img);
		return img;
    }
    
    public static ImagePlus roiToSegmentation(ImagePlus model,Roi r) {
    	ImagePlus res=VitimageUtils.nullImage(model);
    	ImageStack sRes=res.getStack();
    	for(int x=0;x<model.getWidth();x++)for(int y=0;y<model.getWidth();y++) {
    		if(r.contains(x, y))sRes.setVoxel(x, y, 0, 255);
    	}
    	return res;
    }
    
 	public static ImagePlus roiToSegmentationOld(ImagePlus model,Roi r) {
 		RoiManager rm=RoiManager.getRoiManager();
 		rm.reset();
    	VitimageUtils.waitFor(10);
 		ImagePlus temp=VitimageUtils.nullImage(model);
 		VitimageUtils.printImageResume(temp,"s1");
        temp.setTitle("Hi1");
        System.out.println("Hi1");
    	temp.show();
 		VitimageUtils.printImageResume(temp,"s2");
   	 	IJ.setRawThreshold(temp, 127, 255, null); 	
        temp.setRoi(r);
 		VitimageUtils.printImageResume(temp,"s3");
        
        
        IJ.run("Grays","");
        r.setFillColor(Color.white);
 		VitimageUtils.printImageResume(temp,"s4");
        IJ.run("Fill", "slice");
        rm.reset();
 		VitimageUtils.printImageResume(temp,"s5");
        temp.resetRoi();
        ImagePlus ret=temp.duplicate();
        temp.changes=false;
 		VitimageUtils.printImageResume(temp,"s6");
        temp.duplicate().show();System.out.println("3");
 		VitimageUtils.printImageResume(temp,"s7");
        VitimageUtils.waitFor(20000);
        temp.close();
        return ret;
 	}
 
    public static ImagePlus getConvexHull(int predilate,ImagePlus imgTemp,int dilation,boolean debug) {
    	if(imgTemp.getNSlices()>1) {
    		ImagePlus[]tab=VitimageUtils.stackToSlices(imgTemp);
    		for(int i=0;i<tab.length;i++)tab[i]=getConvexHull(predilate,tab[i],dilation,debug);
    		return VitimageUtils.slicesToStack(tab);
    	}
    	if(debug && predilate>0) {
    		System.out.println("Debug");
    		ImagePlus i0=imgTemp.duplicate();
    		i0.setTitle("main");
    		i0.show();
    		VitimageUtils.waitFor(2000);
    		i0.close();
    	}
 		RoiManager rm=RoiManager.getRoiManager();
 		rm.reset();
    	VitimageUtils.waitFor(10);
 		ImagePlus img=imgTemp.duplicate();
    	if(predilate>0) {
     		rm=RoiManager.getRoiManager();
     		rm.reset();
        	VitimageUtils.waitFor(10);
     		img=VitimageUtils.invertBinaryMask(img);
        	for(int i=0;i<predilate;i++)IJ.run(img, "Dilate", "");
        	IJ.run(img,"Fill Holes","");
    		img=VitimageUtils.invertBinaryMask(img);
        	Roi[]r=SegmentationUtils.segmentationToRoi(img);
     		rm.reset();
        	VitimageUtils.waitFor(10);
        	ImagePlus[]imgs=new ImagePlus[r.length];
        	if(debug)System.out.println("Count : "+r.length);
        	for(int i=0;i<r.length;i++) {
        		imgs[i]=roiToSegmentation(imgTemp, r[i]);
        		imgs[i]=VitimageUtils.binaryOperationBetweenTwoImages(imgs[i], imgTemp, 2);
        		imgs[i]=getConvexHull(0, imgs[i],dilation,debug);
        	}
        	for(int i=1;i<r.length;i++) {
        		imgs[0]=VitimageUtils.binaryOperationBetweenTwoImages(imgs[0], imgs[i], 1);
        	}
        	return imgs[0];
    	}

    	img.show();
   	 	IJ.setRawThreshold(img, 127, 255, null); 	
    	rm=RoiManager.getRoiManager();
        rm.reset();
    	VitimageUtils.waitFor(10);
        IJ.run("Create Selection");
    	VitimageUtils.waitFor(10);
        IJ.run(img, "Convex Hull", "");
        IJ.run(img,"Grays","");
        IJ.run("Colors...", "foreground=white background=black selection=yellow");
        IJ.run("Fill", "slice");

        img.resetRoi();
        rm.reset();
    	ImagePlus img2=img.duplicate();
        img.changes=false;
        img.close();
        img2.getStack().setSliceLabel(imgTemp.getStack().getSliceLabel(1),1);
        ij.WindowManager.closeAllWindows();
    	return img2;
    }

}


@SuppressWarnings("rawtypes")
class VolumeEllipsisComparator implements java.util.Comparator {
	   public int compare(Object o1, Object o2) {
		   
	      return -1*(new Double( ((double[][]) o1)[2][0]) ).compareTo( new Double( ((double[][]) o2)  [2][0])  );
	   }
	}


