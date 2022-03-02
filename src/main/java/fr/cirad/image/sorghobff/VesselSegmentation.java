package fr.cirad.image.sorghobff;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import fr.cirad.image.common.Timer;
import fr.cirad.image.common.TransformUtils;
import fr.cirad.image.common.VitiDialogs;
import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.hyperweka.HyperWeka;
import fr.cirad.image.hyperweka.HyperWekaSegmentation;
import fr.cirad.image.mlutils.SegmentationUtils;
import fr.cirad.image.mlutils.SmallestEnclosingCircle;
import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.plugin.tool.PlugInTool;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;
import weka.core.expressionlanguage.common.Primitives.DoubleExpression;

public class VesselSegmentation extends PlugInTool{
	int year1=0;
	int year2=0;
 	/**
 	 * This class is more a script : it executes successive operations to :
 	 * 1) prepare data from json and source images to ML-ready data into valid and train sets
 	 * 
 	 */
	    
	public static void splitting() {
		int split=10;
		String sorghoDir= getVesselsDir();
    	String dirSourceSubIn=sorghoDir+"/Data/Test/02_tif_sub8";
    	String[]files=new File(dirSourceSubIn).list();
    	int[][]tab=VitimageUtils.listForThreads(files.length, split);
    	for(int i=0;i<split;i++) {
    		String str="";
//    		new File(sorghoDir+"/Data/Splitting/split_"+(i+1)+"_over_8").mkdirs();
    		for(int j=0;j<tab[i].length;j++){
    			String basename=VitimageUtils.withoutExtension(files[tab[i][j]]);
    			str+=""+basename+"\n";
    		}
    		VitimageUtils.writeStringInFile(str, sorghoDir+"/Data/Splitting/split_"+(i+1)+"_over_8.txt");
    	}
	}
	
	
	public static boolean isOnJeanZay() {
		return new File("/gpfswork/rech/qlb/uol13er").exists();
	}
	
	public static boolean skip(String fileName) {
	 	   if(new File("/users/bionanonmri").exists()) {
	 		   return(!fileName.contains("2019"));
 		   }
	 	   else return(fileName.contains("2019"));
	}

	public static void main(String[]args) {
        ImageJ ij=new ImageJ();
        WindowManager.closeAllWindows();
       //step_C_08_segment_xylem_parts_and_identify_eyes(""+1);// debugEllipsis(1, "2019_G80_P102_E14");
        //anomalyEllipsis(1,"2018_G01_P5_E24");//2016_G28_P3_E10   2019_G80_P102_E14  2019_G80_P63_E13
        /* for(int split=1;split<=9;split++) {
        	step_C_08_segment_xylem_parts_and_identify_eyes(""+split);
        }*/
		new VesselSegmentation().run("TestEclipse");
	}

	public void run(String arg) {
	   int startStep=12;
	   int lastStep=12;
	   String split="";
 	   if((arg==null) || arg.length()==0) {
 		   IJ.log("THIS IS THE ONE !");
 		   startStep=VitiDialogs.getIntUI("startStep", 1);
 		   lastStep=VitiDialogs.getIntUI("lastStep", 1);
 		   split=""+VitiDialogs.getIntUI("Split", 1);
 		  this.startThirdPhase(startStep,lastStep,split);
	  }
 	   else if (arg.equals("TestEclipse")) {
 		   System.out.println("Running from Eclipse "+startStep+" , "+lastStep+" , "+split);
 		   this.startThirdPhase(startStep,lastStep,split);
 		   System.out.println("Over !");
 	   }
 	   else {
 		   String[]spl=arg.split(" ");
 		   startStep=Integer.parseInt(spl[0]);
 		   lastStep=Integer.parseInt(spl[1]);
 		   split=spl[2];
 		   System.out.println("Running steps "+startStep+" to "+lastStep+" with split "+split+" from command line in jean zay");
 		   System.out.println("Up to 1106 14062021");
 		   this.startThirdPhase(startStep,lastStep,split);
 	   }
 	   if(isOnJeanZay())System.exit(0);// To stop the slurm job
	}	
	
	//TODO for all splits :
	//script copy 
	
	
	//TODO for split 9 :
	//Isolate Test into TestIsolate.
	//Rebuild Test/ with 
	//01_tif with images of split 9 in it
	//run C00
	//run C01
	//run C02
	//run C03
	//run C04
	//Send to Jean Zay as split_9_over_8
	//Run on Jean Zay split 9 C05and CC06
	//Gather on CIRS
	//Run C07
	//Run C08

	
	//Fonction from split to main et from main to split
	//Bash qui effectue les copies vers jean zay et les recup from jeanzay
	
	//Introduce all subs8 in it
	
	   /** Third, from contour segmentation to structure proba maps  ---------------------------------------*/        		
    public void startThirdPhase (int startStep,int lastStep,String split){
     	t=new Timer();
		   boolean bothMathieuAndRomain=true;
		   IJ.log("Starting"); 
		   t.print("Starting step 0");
		   if(split.equals("")) {
			   for(int sp=1;sp<12;sp++)  startThirdPhase(startStep, lastStep, ""+sp);
			   return;
		   }
		   System.out.println("\n\n\nRunning split "+split+" from step "+startStep+" to "+lastStep);
		   int indSplit=Integer.parseInt(split);
		   if(startStep<=1000 && lastStep>=1000)centralToSplit();
		   if(startStep<=0 && lastStep>=0)step_C_00_list_and_subsample(indSplit);
		   if(startStep<=1 && lastStep>=1)step_C_01_slice_ml(indSplit);
		   if(startStep<=2 && lastStep>=2)step_C_02_gather_ml(indSplit) ;
		   if(startStep<=3 && lastStep>=3)step_C_03_compute_circle_and_contour(indSplit) ;
		   if(startStep<=4 && lastStep>=4) step_C_04_BIS_debug_voronoi_and_slice_centers(indSplit);
		   if(startStep<=5 && lastStep>=5) step_C_05_extractVessels(indSplit);
		   if(startStep<=6 && lastStep>=6) step_C_06_vessels_ml(indSplit);
		   if(startStep<=7 && lastStep>=7) step_C_07_xyphlo_ml(indSplit);
		   if(startStep<=8 && lastStep>=8) step_C_08_segment_vessels_contour(indSplit);
		   if(startStep<=9 && lastStep>=9) step_C_09_segment_xylem_parts_and_identify_eyes(indSplit);
		   if(startStep<=10 && lastStep>=10)  step_C_10_anomalyEllipsis(indSplit);
		   if(startStep<=11 && lastStep>=11) step_C_11_generateEllipsisSlices(indSplit);
		   if(startStep<=12 && lastStep>=12) step_C_12_generateColouredVoronoi(indSplit);
		   t.print("Final end of script");
    }	
    
    public void step_C_chiasse() {
    	String sorghoDir= getVesselsDir();
    	String central=sorghoDir+"/Data/Splitting/";
    	for(int sp=1;sp<9;sp++) {
    		String rep=central+"split_"+sp+"_over_8/";
    		System.out.println("\n\n\n\n\nProcessing split "+rep);
    		String[]imgNames=new File(rep).list();
    		for(String imgName : imgNames) {
        		System.out.println("Processing "+imgName);
    			String repImg=rep+imgName;
    			int nb=new File(repImg+"/Source_sub").list().length;
    			ImagePlus []tabStack=new ImagePlus[nb];
    			for(int n=1;n<=nb;n++) {
    				tabStack[n-1]=IJ.openImage(repImg+"/Source_sub/V"+n+".tif");
    			}
    			IJ.saveAsTiff(VitimageUtils.slicesToStack(tabStack), repImg+"/source_sub.tif");
    			
    		}
    	}
    }
    
    public void centralToSplit() {
    	String sorghoDir= getVesselsDir();
    	String centralSplit=sorghoDir+"/Data/Splitting/";
    	String centralDir=sorghoDir+"/Data/Test/";
    	int firstSplit=1;
    	int lastSplit=8;
    	for(int sp=firstSplit;sp<=lastSplit;sp++) {
    		String repSplit=centralSplit+"split_"+sp+"_over_8";
    		System.out.println("\n\n\n\n\nProcessing split "+repSplit);
    		String[]imgNames=VitimageUtils.readStringFromFile(new File(repSplit+".txt").getAbsolutePath()).split("\n");
    		int incr=0;
    		for(String imgName : imgNames) {
        		System.out.println("Processing "+imgName +" "+(incr++));
        		System.out.println("Opening "+centralDir+"08_extracts/"+imgName+"/source_sub.tif");
        		ImagePlus img=IJ.openImage(centralDir+"08_extracts/"+imgName+"/source_sub.tif");
        		new File(repSplit+"/"+imgName+"/08_extracts").mkdirs();
        		System.out.println("Saving "+repSplit+"/"+imgName+"/08_extracts/source_sub.tif");
        		IJ.saveAsTiff(img,repSplit+"/"+imgName+"/08_extracts/source_sub.tif");
        		//Actions to do
				//1) Copy centralDir+"08_extracts/"+imgName+"/source.tif" to repSplit+"/08_extracts/"+imgName+"/source.tif"   //OK
				//2) Copy centralDir+"08_extracts/"+imgName+"/source_sub.tif" to repSplit+"/08_extracts/"+imgName+"/source_sub.tif"   //OK
				//3) Batch scp source.tif to Jean Zay //OK
				//4) BashlistSplit9 
				//5) Apply steps 1 to 4 to split 9, then remove things Split9, then copy level1 stuf in HighResTifDir,
				//6) Copy results to central
				//7) Copy source and source sub of 8 extracts to Jean Zay
        		//    			String repImg=rep+imgName;//TODO
				//   			ImagePlus img=IJ.openImage(repImg+"/source_sub.tif");
    			//IJ.saveAsTiff(VitimageUtils.slicesToStack(tabStack), repImg+"/source_sub.tif");
    			
    		}
    	}
    }
   
    public static String getSplitDir(int split) {
    	return getVesselsDir()+"/Data/Splitting/"+"split_"+split+"_over_8";
    }
    
    
    //Get input dir 1_tif, make subsampling into and 2_tif_sub8, apply ML filters, and write resulting vessel segmentation in 3_vess_seg
    public void step_C_00_list_and_subsample(int split) {
		String sorghoDir= getVesselsDir();
    	String centralSplit=sorghoDir+"/Data/Splitting/";
    	String repSplit=centralSplit+"split_"+split+"_over_8";
		String dirSourceIn=getHighResTifDir();

		//List specimen
    	String[]imgNames=VitimageUtils.readStringFromFile(new File(repSplit+".txt").getAbsolutePath()).split("\n");
    	
    	//Create the split dir
		new File(repSplit).mkdirs();
		for(String s : imgNames) {
	    	//Create in it one dir for each specimen of the list
			String specPath=new File(repSplit,s).getAbsolutePath();
			new File(specPath).mkdirs();
			//Make the subscaling
			ImagePlus img=IJ.openImage(new File(getHighResTifDir(),s+".tif").getAbsolutePath());
			IJ.saveAsTiff(SegmentationUtils.subscaling2D(img, 8),new File(specPath,"slice_sub_8.tif").getAbsolutePath());
		}		
    }
    
   //"/media/rfernandez/DATA_RO_A/Sorgho_Slices_BFF/Img_lvl_1/2016_G1_P11_E11.tif"
    
    //Process the first ML part, to compute probaMap of vessels, and extract first vessel segmentation
    public void step_C_01_slice_ml(int split) {
    	//Collect all sub from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();
    	System.out.println(splitDir);
    	
    	//Gather in a temp dir
    	new File(splitDir,"tempIn").mkdirs();
    	new File(splitDir,"tempOut").mkdirs();
    	for(String spec : specNames) {
        	System.out.println(spec);
    		if(spec.contains("temp")  )continue;
    		ImagePlus img=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
    		IJ.saveAsTiff(img, splitDir+"/tempIn/"+spec+".tif");
    	}
    	
    	//Process
    	SegmentationUtils.batchVesselSegmentation(getVesselsDir(),new File(splitDir,"tempIn").getAbsolutePath(),new File(splitDir,"tempOut").getAbsolutePath());
    }	
   	
	
 
 	//extract center of the minimal circle surrounding all the vessels, and write each center in a unique CSV
    public void step_C_02_gather_ml(int split) {
    	//Collect results : probaMaps and seg
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();
		if(new File(splitDir+"/tempOut").exists()) {
	    	for(String spec : specNames) {
	    		
	    		if(spec.contains("temp"))continue;
	    		ImagePlus img=IJ.openImage(splitDir+"/tempOut/ProbaMap/"+spec+".tif");
	    		IJ.saveAsTiff(img, splitDir+"/"+spec+"/probaMap_slice.tif");
	    		img=IJ.openImage(splitDir+"/tempOut/Segmentation/"+spec+".tif");
	    		IJ.saveAsTiff(img, splitDir+"/"+spec+"/segmentation_slice.tif");
	    	}
	    	try {
	    		FileUtils.deleteDirectory(new File(splitDir,"tempOut"));
				FileUtils.deleteDirectory(new File(splitDir,"tempIn"));
			} catch (IOException e) {			e.printStackTrace();		}
    	}
	    	//List data from the split
    }
    
    public void step_C_03_compute_circle_and_contour(int split) {
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();
    	int incr=0;
    	int NN=specNames.length;
		for(String spec : specNames) {

			//Get segmentation image
    		ImagePlus imgSeg=IJ.openImage(splitDir+"/"+spec+"/segmentation_slice.tif");    		
    		System.out.println((incr++)+"/"+NN+" "+spec);
    		
        	//Apply a series of morpho operations to get main component (in case where multiple slices present on the imaged space)
    		ImagePlus imgDil=SegmentationUtils.dilation(imgSeg, 150, false);
 			imgDil=VitimageUtils.connexe(imgDil,0.5,256,0,1E100,6,1,true);
 			ImagePlus imgInit=VitimageUtils.binaryOperationBetweenTwoImages(imgSeg, imgDil, 2);

 	    	//Get smallest enclosing circle
 			Object[]obj=SmallestEnclosingCircle.smallestEnclosingCircle(imgInit,true);
 			double[]enclosingCircle=(double[]) obj[0];
 			ImagePlus im=(ImagePlus) obj[1];
 	   		IJ.saveAsTiff(im,splitDir+"/"+spec+"/segmentation_no_out.tif");    		
            ImagePlus img2=SegmentationUtils.getConcaveHull(im, 200);
            IJ.saveAsTiff(img2,splitDir+"/"+spec+"/concave_hull.tif");    		
            
            
 	    	//Write circle info 
 	   		String[][]circleInfo=new String[][] {{""+enclosingCircle[0],""+enclosingCircle[1],""+enclosingCircle[2]},{""+enclosingCircle[0]*8,""+enclosingCircle[1]*8,""+enclosingCircle[2]*8}};
 			VitimageUtils.writeStringTabInCsv(circleInfo, new File(splitDir+"/"+spec+"/circle.csv").getAbsolutePath());
    	}
    	
    	
    	/*
    	String sorghoDir= getVesselsDir();
    	String dirSourceIn2=sorghoDir+"/Data/TestSplit9/03_slice_seg/Segmentation2";
    	new File(dirSourceIn2).mkdirs();
    	String dirSourceSubIn=sorghoDir+"/Data/TestSplit9/02_tif_sub8";
    	new File(dirSourceSubIn).mkdirs();
    	String dirSourceIn=sorghoDir+"/Data/TestSplit9/03_slice_seg/Segmentation";
    	String dirSegOut=sorghoDir+"/Data/TestSplit9/04_slice_centers";
    	new File(dirSegOut).mkdirs();
 		String[]imgNames=new File(dirSourceSubIn).list();
 		for(int indImg=0;indImg<imgNames.length;indImg++) {
 			System.out.println("Starting information extraction "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
 			ImagePlus imgInit=IJ.openImage(new File(dirSourceIn,imgNames[indImg]).getAbsolutePath());
 			ImagePlus imgDil=SegmentationUtils.dilation(imgInit, 150, false);
 			imgDil=VitimageUtils.connexe(imgDil,0.5,256,0,1E100,6,1,true);
 			imgInit=VitimageUtils.binaryOperationBetweenTwoImages(imgInit, imgDil, 2);
 			IJ.saveAsTiff(imgInit, new File(dirSourceIn2,imgNames[indImg] ).getAbsolutePath());
 			Object[]obj=SmallestEnclosingCircle.smallestEnclosingCircle(imgInit,true);
 			double[]enclosingCircle=(double[]) obj[0];
 			ImagePlus seg2=(ImagePlus) obj[1];
 			IJ.saveAsTiff(seg2, new File(dirSourceIn2,imgNames[indImg]).getAbsolutePath());
 			String[][]circleInfo=new String[][] {{""+enclosingCircle[0],""+enclosingCircle[1],""+enclosingCircle[2]},{""+enclosingCircle[0]*8,""+enclosingCircle[1]*8,""+enclosingCircle[2]*8}};
 			VitimageUtils.writeStringTabInCsv(circleInfo, new File(dirSegOut,VitimageUtils.withoutExtension(imgNames[indImg])+"_circle.csv").getAbsolutePath());
 		}
 		*/
    }
    
    
   	//Debug image built with a voronoi and from coordinate of the surrounding circle
    public void step_C_04_debug_voronoi_and_slice_centers(int split) {
    	//List data from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();

    	for(String spec : specNames) {
    		System.out.println("Processing "+spec);
    		//Get the circle file, the segmentation image and the grayscale extract sub
    		ImagePlus imgSeg=IJ.openImage(splitDir+"/"+spec+"/segmentation_no_out.tif");
    		ImagePlus imgRGB=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
    		ImagePlus imgGray=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
    		IJ.run(imgGray,"8-bit","");
    		double[]circleCoords=SegmentationUtils.stringTabToDoubleTab(
    				VitimageUtils.readStringTabFromCsv(
    						new File(splitDir+"/"+spec+"/circle.csv").getAbsolutePath())[0]);
    	
			ImagePlus disk=VitimageUtils.drawCircleInImage(SegmentationUtils.resetCalibration(VitimageUtils.nullImage(imgGray)), circleCoords[2],(int)circleCoords[0],(int)circleCoords[1],0,255);
			ImagePlus circle=VitimageUtils.drawCircleNoFillInImage(SegmentationUtils.resetCalibration(VitimageUtils.nullImage(imgGray)), circleCoords[2],(int)circleCoords[0],(int)circleCoords[1],0,255,3);
			ImagePlus voronoi=SegmentationUtils.getVoronoi(imgSeg, true);
			ImagePlus voronoiIn=VitimageUtils.binaryOperationBetweenTwoImages(voronoi, disk, 2);
			ImagePlus contours=VitimageUtils.binaryOperationBetweenTwoImages(voronoiIn, circle, 1);
			ImagePlus sum=VitimageUtils.binaryOperationBetweenTwoImages(contours, imgSeg, 1);
			ImagePlus result=SegmentationUtils.visualizeBiMaskEffectOnSourceData(imgRGB,imgSeg,contours,2);
			IJ.saveAsTiff(result,	splitDir+"/"+spec+"/debug_circ.tif");
			IJ.saveAsTiff(voronoi,	splitDir+"/"+spec+"/voronoi.tif");
    	}
    }	
/*    		
    		String sorghoDir= getVesselsDir();
	    	String dirSourceSubIn=sorghoDir+"/Data/TestSplit9/02_tif_sub8";
	       	String dirSourceSubSeg=sorghoDir+"/Data/TestSplit9/03_slice_seg/Segmentation";
	       	String dirCsvIn=sorghoDir+"/Data/TestSplit9/04_slice_centers";
	    	String dirOutVor=sorghoDir+"/Data/TestSplit9/05_voronoi";
	    	new File(dirOutVor).mkdirs();
	    	String dirOutCirc=sorghoDir+"/Data/TestSplit9/06_slice_circles";
	    	new File(dirOutCirc).mkdirs();
	    	String dirOutDebug=sorghoDir+"/Data/TestSplit9/07_debug";
	    	new File(dirOutDebug).mkdirs();
    		String[]imgNames=new File(dirSourceSubIn).list();
    		for(int indImg=0;indImg<imgNames.length;indImg++) {
    			String imgName=imgNames[indImg];
    			System.out.println("Starting information extraction "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
    			ImagePlus imgInit=IJ.openImage(new File(dirSourceSubIn,imgName).getAbsolutePath());
    			ImagePlus imgGray=imgInit.duplicate();
    			ImagePlus imgSegSub=IJ.openImage(new File(dirSourceSubSeg,imgName).getAbsolutePath());
    			IJ.run(imgGray,"8-bit","");
    			double[]circleCoords=SegmentationUtils.stringTabToDoubleTab(VitimageUtils.readStringTabFromCsv(new File(dirCsvIn,VitimageUtils.withoutExtension(imgName)+"_circle.csv").getAbsolutePath())[0]);
//    			System.out.println(circleCoords[0]+" , "+circleCoords[1]+" , "+circleCoords[2]);
    			ImagePlus disk=VitimageUtils.drawCircleInImage(SegmentationUtils.resetCalibration(VitimageUtils.nullImage(imgGray)), circleCoords[2],(int)circleCoords[0],(int)circleCoords[1],0,255);
    			ImagePlus circle=VitimageUtils.drawCircleNoFillInImage(SegmentationUtils.resetCalibration(VitimageUtils.nullImage(imgGray)), circleCoords[2],(int)circleCoords[0],(int)circleCoords[1],0,255,3);
    			ImagePlus voronoi=SegmentationUtils.getVoronoi(imgSegSub, true);
    			ImagePlus voronoiIn=VitimageUtils.binaryOperationBetweenTwoImages(voronoi, disk, 2);
    			ImagePlus contours=VitimageUtils.binaryOperationBetweenTwoImages(voronoiIn, circle, 1);
    			ImagePlus sum=VitimageUtils.binaryOperationBetweenTwoImages(contours, imgSegSub, 1);
    			//ImagePlus result=VitimageUtils.compositeRGBByte(imgSegSub, contours, imgGray, 1, 1, 1);
    			ImagePlus result=SegmentationUtils.visualizeBiMaskEffectOnSourceData(imgInit,imgSegSub,contours,2);
    			IJ.saveAsTiff(result,	new File(dirOutDebug,imgName).getAbsolutePath());
    			IJ.saveAsTiff(circle,	new File(dirOutCirc,imgName).getAbsolutePath());
    			IJ.saveAsTiff(voronoi,	new File(dirOutVor,imgName).getAbsolutePath());    		
    		}
       }*/
       
   	//Debug image built with a voronoi and from coordinate of the surrounding circle
    public void step_C_04_BIS_debug_voronoi_and_slice_centers(int split) {
    	//List data from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();

    	for(String spec : specNames) {
    		//Get the circle file, the segmentation image and the grayscale extract sub
    		ImagePlus imgSeg=IJ.openImage(splitDir+"/"+spec+"/segmentation_no_out.tif");
    		ImagePlus imgRGB=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
    		ImagePlus imgGray=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
    		IJ.run(imgGray,"8-bit","");
    		double[]circleCoords=SegmentationUtils.stringTabToDoubleTab(
    				VitimageUtils.readStringTabFromCsv(
    						new File(splitDir+"/"+spec+"/circle.csv").getAbsolutePath())[0]);
    	
    		ImagePlus circle=IJ.openImage(splitDir+"/"+spec+"/concave_hull.tif");
    		ImagePlus disk=circle.duplicate();
    		IJ.run(disk,"Invert","");
    		IJ.run(disk,"Fill Holes","");
    		IJ.run(disk,"Invert","");
			ImagePlus voronoi=SegmentationUtils.getVoronoi(imgSeg, true);
			ImagePlus voronoiIn=VitimageUtils.binaryOperationBetweenTwoImages(voronoi, disk, 2);
			ImagePlus contours=VitimageUtils.binaryOperationBetweenTwoImages(voronoiIn, circle, 1);
			ImagePlus sum=VitimageUtils.binaryOperationBetweenTwoImages(contours, imgSeg, 1);
			ImagePlus result=SegmentationUtils.visualizeBiMaskEffectOnSourceData(imgRGB,imgSeg,contours,2);
			IJ.saveAsTiff(result,	splitDir+"/"+spec+"/debug_circ.tif");
			IJ.saveAsTiff(voronoi,	splitDir+"/"+spec+"/voronoi.tif");
    	}
    }	
/*    		
    		String sorghoDir= getVesselsDir();
	    	String dirSourceSubIn=sorghoDir+"/Data/TestSplit9/02_tif_sub8";
	       	String dirSourceSubSeg=sorghoDir+"/Data/TestSplit9/03_slice_seg/Segmentation";
	       	String dirCsvIn=sorghoDir+"/Data/TestSplit9/04_slice_centers";
	    	String dirOutVor=sorghoDir+"/Data/TestSplit9/05_voronoi";
	    	new File(dirOutVor).mkdirs();
	    	String dirOutCirc=sorghoDir+"/Data/TestSplit9/06_slice_circles";
	    	new File(dirOutCirc).mkdirs();
	    	String dirOutDebug=sorghoDir+"/Data/TestSplit9/07_debug";
	    	new File(dirOutDebug).mkdirs();
    		String[]imgNames=new File(dirSourceSubIn).list();
    		for(int indImg=0;indImg<imgNames.length;indImg++) {
    			String imgName=imgNames[indImg];
    			System.out.println("Starting information extraction "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
    			ImagePlus imgInit=IJ.openImage(new File(dirSourceSubIn,imgName).getAbsolutePath());
    			ImagePlus imgGray=imgInit.duplicate();
    			ImagePlus imgSegSub=IJ.openImage(new File(dirSourceSubSeg,imgName).getAbsolutePath());
    			IJ.run(imgGray,"8-bit","");
    			double[]circleCoords=SegmentationUtils.stringTabToDoubleTab(VitimageUtils.readStringTabFromCsv(new File(dirCsvIn,VitimageUtils.withoutExtension(imgName)+"_circle.csv").getAbsolutePath())[0]);
//    			System.out.println(circleCoords[0]+" , "+circleCoords[1]+" , "+circleCoords[2]);
    			ImagePlus disk=VitimageUtils.drawCircleInImage(SegmentationUtils.resetCalibration(VitimageUtils.nullImage(imgGray)), circleCoords[2],(int)circleCoords[0],(int)circleCoords[1],0,255);
    			ImagePlus circle=VitimageUtils.drawCircleNoFillInImage(SegmentationUtils.resetCalibration(VitimageUtils.nullImage(imgGray)), circleCoords[2],(int)circleCoords[0],(int)circleCoords[1],0,255,3);
    			ImagePlus voronoi=SegmentationUtils.getVoronoi(imgSegSub, true);
    			ImagePlus voronoiIn=VitimageUtils.binaryOperationBetweenTwoImages(voronoi, disk, 2);
    			ImagePlus contours=VitimageUtils.binaryOperationBetweenTwoImages(voronoiIn, circle, 1);
    			ImagePlus sum=VitimageUtils.binaryOperationBetweenTwoImages(contours, imgSegSub, 1);
    			//ImagePlus result=VitimageUtils.compositeRGBByte(imgSegSub, contours, imgGray, 1, 1, 1);
    			ImagePlus result=SegmentationUtils.visualizeBiMaskEffectOnSourceData(imgInit,imgSegSub,contours,2);
    			IJ.saveAsTiff(result,	new File(dirOutDebug,imgName).getAbsolutePath());
    			IJ.saveAsTiff(circle,	new File(dirOutCirc,imgName).getAbsolutePath());
    			IJ.saveAsTiff(voronoi,	new File(dirOutVor,imgName).getAbsolutePath());    		
    		}
       }*/
    
    
          
   	//Get seg and centers, extract each unique vessel
    public void step_C_05_extractVessels(int split) {
    	//List data from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();

    	for(String spec : specNames) {
    		System.out.println("Spec "+spec);
    		SegmentationUtils.extractVesselsOfSpecimen(splitDir+"/"+spec,getHighResTifDir()+"/"+spec+".tif",8);
    	}
    	/*	   	String sorghoDir= getVesselsDir();
    	String dirSegIn=sorghoDir+"/Data/TestSplit9/03_slice_seg/Segmentation";
    	String dirVoronoiIn=sorghoDir+"/Data/TestSplit9/05_voronoi";
    	String dirSegOut=sorghoDir+"/Data/TestSplit9/08_extracts";
    	String dirSourceSubIn=sorghoDir+"/Data/TestSplit9/02_tif_sub8";
    	new File(dirSegOut).mkdirs();
    	SegmentationUtils.extractVessels(getHighResTifDir(), dirSourceSubIn,dirSegIn,dirVoronoiIn,dirSegOut,8);
*/
	}
	
    //TODO : 
    //Extract vessels using the vessel binary model. If result contain multiple segmented zones, recompute a seed base watershed and update the segmented bin vessel
    //900 s per image
    public void step_C_06_vessels_ml(int split) {
    	//if(split==null)SegmentationUtils.batchVesselContour(getVesselsDir()+"/Data/Test/08_extracts",true,true);
    	//else{
    		//int indSplit=Integer.parseInt(split);
    		SegmentationUtils.batchVesselContour(getSplitDir(split),true,true);
//    	}
    }    


    //700 s per image
    public void step_C_07_xyphlo_ml(int split) {
		SegmentationUtils.batchXyPhloContour(getSplitDir(split),true,true);
/*    	if(split==null)SegmentationUtils.batchXyPhloContour(getVesselsDir()+"/Data/Test/08_extracts",true,true);
    	else{
    		int indSplit=Integer.parseInt(split);
    		SegmentationUtils.batchXyPhloContour(getVesselsDir()+"/Data/Splitting/split_"+indSplit+"_over_8",true,true);
    	}*/
    }


    public void step_C_08_segment_vessels_contour() {
    	String sorghoDir= getVesselsDir();
    	String dirExtracts=sorghoDir+"/Data/Test/08_extracts";
		String[]imgNames=new File(dirExtracts).list();
 		for(int indImg=0;indImg<imgNames.length;indImg++) {
 			String dataPath=new File(dirExtracts,imgNames[indImg]).getAbsolutePath();
 			ImagePlus source=IJ.openImage(new File(dataPath,"source.tif").getAbsolutePath());
 			//TODO from there
 			String []listVess=new File(dataPath,"Source").list();
 			System.out.println("Starting vessels contour of "+imgNames[indImg]+" = "+(indImg+1)+"/"+imgNames.length);
 			int N=listVess.length;
 			int incr=0;
 			for(String vesName : listVess) { 				
 				System.out.println(vesName+" : "+(++incr)+" / "+N);
 				//Take probamap and voronoi
	 			ImagePlus probamap=IJ.openImage(dataPath+"/ProbaMap_vessel_contour/"+vesName);
	 			probamap =SegmentationUtils.resize(probamap, 200, 200, 1);

	 			ImagePlus voronoi=IJ.openImage(dataPath+"/Voronoi_slice/"+vesName);
	 			voronoi=VitimageUtils.getBinaryMaskUnary(voronoi, 0.5);
	 			voronoi=VitimageUtils.invertBinaryMask(voronoi);
	 			probamap=VitimageUtils.makeOperationBetweenTwoImages(probamap, voronoi, 2, true);
	 			ImagePlus seg=getVesselSegmentationFromHighResProbaMap(probamap);
	 			IJ.saveAsTiff(seg, dataPath+"/Segmentation_vessel/"+vesName);
 			}
 		}
    }
    
	public void step_C_08_segment_vessels_contour(int split) {
    	//List data from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();

    	for(String spec : specNames) {
 			String dataPath=new File(splitDir,spec).getAbsolutePath();
 			ImagePlus source=IJ.openImage(new File(dataPath,"source_vesselstack.tif").getAbsolutePath());

 			System.out.println("Starting vessels contour of "+spec);
 			int N=source.getNSlices();
 			ImagePlus probamap=IJ.openImage(dataPath+"/probaMap_vessel_contour.tif");
 			probamap =SegmentationUtils.resize(probamap, 200, 200, N);

 			ImagePlus voronoi=IJ.openImage(dataPath+"/voronoi_vessels.tif");
 			voronoi=VitimageUtils.getBinaryMaskUnary(voronoi, 0.5);
 			voronoi=VitimageUtils.invertBinaryMask(voronoi);
 			probamap=VitimageUtils.makeOperationBetweenTwoImages(probamap, voronoi, 2, true);
 			ImagePlus seg=getVesselSegmentationFromHighResProbaMap(probamap);
 			IJ.saveAsTiff(seg, dataPath+"/segmentation_vessels.tif");
 		}
    }

	
	
    
    
    

    //TODO
    public static void step_C_09_segment_xylem_parts_and_identify_eyes(int split) {
    	boolean makeImageDebug=false;
    	boolean debug=false;
		Timer t=new Timer();
    	//List data from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();

    	int incr=0;
    	for(String spec : specNames) {    		
 			t.print("\nStarting xylem contour "+(incr++)+"/"+specNames.length+" of "+spec);
    		String dataPath=new File(splitDir,spec).getAbsolutePath();
    		File f=new File(dataPath+"/segmentation_combined.tif");
    		long lastModified = f.lastModified();
    		String pattern = "MM-dd";
    		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);    		
    		Date lastModifiedDate = new Date( lastModified );
    		String s=simpleDateFormat.format( lastModifiedDate );
    		System.out.println( "The file " + f + " was last modified on " + s );
    		if((Integer.parseInt(s.split("-")[0])==9) && (Integer.parseInt(s.split("-")[1])>=20)) {
        		VitimageUtils.waitFor(500);
    			continue;
    		}

 			String csvPath=new File(dataPath,"circle.csv").getAbsolutePath();
 			double[]sliceCircle=SegmentationUtils.stringTabToDoubleTab(VitimageUtils.readStringTabFromCsv(csvPath)[1]);
 			csvPath=new File(dataPath,"Extracts_descriptor.csv").getAbsolutePath();
 			String[][]vesselCenters=VitimageUtils.readStringTabFromCsv(csvPath); 			

/* 			String imgName=imgNames[indImg]; //Collection Mathieu 2016_G28_P3_E10   2017_G28_P1_E13.tif  2018_G01_P5_E24  2019_G80_P102_E14 2019_G80_P63_E13 (parfaite)
 			if(makeImageDebug) {
 				String[]matCol=new String[] {"2016_G28_P3_E10","2017_G28_P1_E13","2018_G01_P5_E24","2019_G80_P102_E14","2019_G80_P63_E13"};
 				int score=0;
 				for(String s:matCol)if(imgName.contains(s))score++;
 				if(score==0)continue;
 			}*/
 			ImagePlus source=IJ.openImage(dataPath+"/source_vesselstack.tif");
 			ImagePlus hull=IJ.openImage(dataPath+"/concave_hull.tif");
			
 			int N=source.getNSlices();
 			String [][]csvTab=new String[N+1][54];
 			for(int i=0;i<N+1;i++)for(int j=0;j<54;j++)csvTab[i][j]="";
 			//Vessel
 			ImagePlus probamap=IJ.openImage(dataPath+"/probaMap_vessel_contour.tif");
 			probamap=SegmentationUtils.resize(probamap, 200, 200, 1);
 			ImagePlus voronoi=IJ.openImage(dataPath+"/voronoi_vessels.tif");
 			voronoi=VitimageUtils.getBinaryMaskUnary(voronoi, 0.5);
 			voronoi=VitimageUtils.invertBinaryMask(voronoi);
 			probamap=VitimageUtils.makeOperationBetweenTwoImages(probamap, voronoi, 2, true);
 			ImagePlus vesselSegStack=getVesselSegmentationFromHighResProbaMap(probamap);
 			IJ.saveAsTiff(vesselSegStack,dataPath+"/vesselSeg.tif");
 			
			//Phloem : Take probamap and voronoi
 			probamap=IJ.openImage(dataPath+"/probaMap_phloquad.tif");
 			probamap=SegmentationUtils.resize(probamap, 200, 200, 1);
 			ImagePlus vesselSeg2Stack=VitimageUtils.getBinaryMaskUnary(vesselSegStack, 0.5);
 			probamap=VitimageUtils.makeOperationBetweenTwoImages(probamap, vesselSeg2Stack, 2, true);
 			ImagePlus phloemSegStack=getPhloemSegmentationFromHighResProbaMap(probamap);
 			IJ.saveAsTiff(phloemSegStack,dataPath+"/phloemSeg.tif");
 			
 			//Xylem : Take probamap and voronoi
 			probamap=IJ.openImage(dataPath+"/probaMap_xylquad.tif");
 			probamap=SegmentationUtils.resize(probamap, 200, 200, 1);
 			probamap=VitimageUtils.makeOperationBetweenTwoImages(probamap, vesselSegStack, 2, true);
 			vesselSeg2Stack=VitimageUtils.thresholdImage(vesselSeg2Stack, 0.5,	256);
 			IJ.run(vesselSeg2Stack,"8-bit","");
 			ImagePlus xylemSegStack=getXylemSegmentationFromHighResProbaMap(probamap);
 			//xylemSeg.duplicate().show();
 			ImagePlus xylemLabelsStack=VitimageUtils.connexe2d(xylemSegStack,1 , 256, 0,1E10, 6,0,true);
 			xylemLabelsStack=SegmentationUtils.convertShortToByte(xylemLabelsStack);	 			
			IJ.saveAsTiff(xylemLabelsStack,dataPath+"/xylemLabels.tif");
			//xylemLabelsStack.duplicate().show();
			
			ImagePlus[]orientationCircleTab=new ImagePlus[N];
			ImagePlus[]combinedSegTab=new ImagePlus[N];
			for(int indexVes=1;indexVes<=N;indexVes++) { 								
				csvTab[indexVes][2]="";
				csvTab[indexVes][3]="";
				csvTab[indexVes][4]="";
				String vesName="V"+indexVes;
 				if((indexVes%20)==0)System.out.print(indexVes+"/"+N+"  ");
 				if(indexVes==300)System.out.println();
				ImagePlus vesselSeg=new Duplicator().run(vesselSegStack,1,1,indexVes,indexVes,1,1); 				
				ImagePlus phloemSeg=new Duplicator().run(phloemSegStack,1,1,indexVes,indexVes,1,1);
				ImagePlus xylemSeg=new Duplicator().run(xylemSegStack,1,1,indexVes,indexVes,1,1);
				ImagePlus xylemLabels=new Duplicator().run(xylemLabelsStack,1,1,indexVes,indexVes,1,1);
				
				
				//Get equivalent ellipsoid of the vessel, then all possible xylems sorted by size (the first, the bigger)
 				double[][]ellVessel=SegmentationUtils.inertiaComputationBin(vesselSeg,debug);
	 			double[][][]ellXylem=SegmentationUtils.inertiaComputation(xylemLabels,true,debug,true);
	 		

	 			 //Inspect if less than two xylems or no phloem.
	 			 int nbXyl=ellXylem.length;
	 			 int nbMeta=nbXyl > 5 ? 5 : nbXyl;
	 			 boolean hasPhloem=(VitimageUtils.maxOfImage(phloemSeg)!=0);
 				 if(nbXyl<2) {csvTab[indexVes][2]="NONVALID";csvTab[indexVes][3]="STEP8_2";}

	 			 
	 			 //Compute geometry of vessel
				 double[]sliceCenter=new double[] {sliceCircle[0],sliceCircle[1]};
				 double sliceRadius=sliceCircle[2];
	 			 double[]vesCenterRelativeToSliceCenter=new double[] {Double.parseDouble(vesselCenters[indexVes][2]) ,Double.parseDouble(vesselCenters[indexVes][3]) };

	 			 double distance=SegmentationUtils.getDistancesFromCenterToContour(new double[][] {{ Double.parseDouble(vesselCenters[indexVes][2])/8,Double.parseDouble(vesselCenters[indexVes][3])/8 }},hull,new double[] {sliceCircle[0]/8,sliceCircle[1]/8})[0];
	 			 double []vectSliceToVess=TransformUtils.vectorialSubstraction(vesCenterRelativeToSliceCenter, sliceCenter);
				 double distanceSliceToVess=TransformUtils.norm(vectSliceToVess);
				 double[]normalizedVectSliceToVess=TransformUtils.normalize(vectSliceToVess);
 				 double normalisedDistanceCenterToSlice=distanceSliceToVess/sliceRadius;
 				 double[]vesCenter=new double[] {ellVessel[0][1],ellVessel[0][2]};
				 double vesselRadius=ellVessel[1][0]/2.0+ellVessel[1][1]/2.0;
		 			 
	 			//For all possible combination Right eye, Left eye
	 			double[][]combScores=new double[nbXyl*(nbXyl-1)][8];
	 			double[][]combIndices=new double[nbXyl*(nbXyl-1)][2];
	 			int index=-1;
	 			int i1top=0;
	 			int i2top=1;
	 			
	 			ImagePlus phloTop=null;
	 			ImagePlus protoTop=null;
	 			ImagePlus metaTop=null;
	 			double xVectTop=0;
	 			double yVectTop=0;
	 			double scoreMax=-100;
	 			//System.out.println(" has a phloem : "+hasPhloem+" with "+nbXyl+" xylem elements");
	 			for(int i1=0;i1<nbMeta;i1++) {
		 			for(int i2=0;i2<nbMeta;i2++) {
		 				 if (i1==i2) continue;
		 				 index++;
		 				 combIndices[index][0]=i1;
		 				 combIndices[index][1]=i2;
		 				 int labeli1=(int) ellXylem[i1][0][0];
		 				
		 				   
		 				 int labeli2=(int) ellXylem[i2][0][0];
		 				 if(debug)System.out.println("\nStarting evaluation "+index+" : "+i1+" , "+i2+" with labels "+labeli1+"-"+labeli2);

		 				 double[]leftEyeCenter=new double[] {ellXylem[i1][0][1],ellXylem[i1][0][2]};
		 				 double[]rightEyeCenter=new double[] {ellXylem[i2][0][1],ellXylem[i2][0][2]};
		 				 double[]eyesCenter=TransformUtils.vectorialMean(leftEyeCenter, rightEyeCenter);
		 				 double[]vectLeftToRight=TransformUtils.vectorialSubstraction(rightEyeCenter,leftEyeCenter);
		 				 double[]vectMouthToNose=new double[] {vectLeftToRight[1],-vectLeftToRight[0]};//Warning: y downside. 1 0  ->  0 -1     0 1 -> 1 0       -1 0 -> 0 1   0 -1 -> -1 0                x=y  y=-x
		 				 double[]normalizedVectMouthToNose=TransformUtils.normalize(vectMouthToNose);
		 				 
		 				 double distanceInterEyes=TransformUtils.norm(vectLeftToRight);
		 				 double distanceEyesCenterToVesselCenter=TransformUtils.norm(TransformUtils.vectorialSubstraction(eyesCenter, vesCenter));
		 				 if(debug)System.out.println("Eyes center="+eyesCenter[0]+","+eyesCenter[1]);
		 				 if(debug)System.out.println("distanceInterEyes="+distanceInterEyes);
		 				 if(debug)System.out.println("distanceEyesCenterToVesselCenter="+distanceEyesCenterToVesselCenter);

		 				 double meanVolume=(ellXylem[i1][2][0]+ellXylem[i2][2][0])/2.0;
		 				 double diffVolume=Math.abs(ellXylem[i1][2][0]-ellXylem[i2][2][0]);
		 				 if(debug)System.out.println("Volume, mean="+meanVolume+" diff="+diffVolume);
		 				 
		 				 double meanGax=(ellXylem[i1][1][0]+ellXylem[i2][1][0])/2.0;
		 				 double diffGax=Math.abs(ellXylem[i1][1][0]-ellXylem[i2][1][0]);
		 				 if(debug)System.out.println("Gax, mean="+meanGax+" diff="+diffGax);

		 				 double meanPax=(ellXylem[i1][1][1]+ellXylem[i2][1][1])/2.0;
		 				 double diffPax=Math.abs(ellXylem[i1][1][1]-ellXylem[i2][1][1]);
		 				 if(debug)System.out.println("Pax, mean="+meanPax+" diff="+diffPax);

		 				 ImagePlus meta=VitimageUtils.thresholdByteImage(xylemLabels, labeli1-0.1,labeli1+0.1);
		 				 ImagePlus temp=VitimageUtils.thresholdByteImage(xylemLabels, labeli2-0.1,labeli2+0.1);
		 				 meta=VitimageUtils.binaryOperationBetweenTwoImages(meta, temp, 1);
		 				 
		 				 ImagePlus proto=VitimageUtils.switchValueInImage(VitimageUtils.switchValueInImage(xylemLabels, labeli1,0),labeli2,0);
		 				 proto=VitimageUtils.thresholdImage(proto, 0.5, 256);
		 				 ImagePlus []splitProto =SegmentationUtils.splitBinaryPortraitIntoUpperAndDownPart(proto,leftEyeCenter[0],leftEyeCenter[1],rightEyeCenter[0],rightEyeCenter[1]);
		 				 ImagePlus []splitPhlo =SegmentationUtils.splitBinaryPortraitIntoUpperAndDownPart(phloemSeg,leftEyeCenter[0],leftEyeCenter[1],rightEyeCenter[0],rightEyeCenter[1]);
		 				 double volPhloUp=SegmentationUtils.getVolumeOfObject(splitPhlo[0]);
		 				 double volPhloBottom=SegmentationUtils.getVolumeOfObject(splitPhlo[1]);
		 				 double volProtUp=SegmentationUtils.getVolumeOfObject(splitProto[0]);
		 				 double volProtBottom=SegmentationUtils.getVolumeOfObject(splitProto[1]);
 					 				 
		 				//Score 0 = cos (mouthToNose , centerToVessel)*(normalisedDistanceCenterToSlice<0.15 ? 1 : 0.5Oui
		 				 
		 				combScores[index][0]=TransformUtils.scalarProduct(normalizedVectMouthToNose, normalizedVectSliceToVess);
		 				
		 				//Score 1 = vesRad - dist(vesCent,eyesCent) / vesRad
		 				combScores[index][1]=(vesselRadius-distanceEyesCenterToVesselCenter)/(vesselRadius);
		 				
		 				//Score 2 = dist(eyes) / (2*vesRad)
		 				combScores[index][2]=(distanceInterEyes)/(2*vesselRadius);
		 				
		 				//Score 3 = (meanVol - diffVol)/meanVol
		 				combScores[index][3]=(meanVolume-diffVolume*0.5)/meanVolume;
		 				
		 				//Score 4 = (meanGratAx - diffGratAx)/meanGratAx
		 				combScores[index][4]=(meanGax-diffGax*0.5)/meanGax;
		 				
		 				//Score 5 = (meanLitAx - diffLitAx)/meanLitAx
		 				combScores[index][5]=(meanPax-diffPax*0.5)/meanPax;

		 				//Score 6 = relative protemness down
		 				combScores[index][6]=volPhloUp/(VitimageUtils.EPSILON+ volPhloUp+volPhloBottom);
		 				
		 				//Score 7 = relative phloemness up) 
		 				combScores[index][7]=volProtBottom/(VitimageUtils.EPSILON+ volProtUp+volProtBottom);

		 				
		 				if(debug)System.out.println("Scores : ");
		 				if(debug)for(int i=0;i<8;i++){System.out.print("("+i+")="+combScores[index][i]);if(Double.isNaN(combScores[index][i]))combScores[index][i]=-1;}
		 				double score=VitimageUtils.mean(combScores[index]);
		 				if(debug)System.out.println("\nMean score="+score);
		 				if(score>scoreMax) {
		 					//System.out.println("\nWe have got a new champion !\n\n\n");
		 					scoreMax=score;
 							i1top=i1;
		 					i2top=i2;
		 					xVectTop=normalizedVectMouthToNose[0];
		 					yVectTop=normalizedVectMouthToNose[1];
		 					protoTop=splitProto[1];
		 					phloTop=splitPhlo[0];
		 					metaTop=meta;
		 				}
		 			} 
	 			}

	 			double[][][]inertiaProto=null;
	 			double[][]inertiaPhlo=null;
	 			if((nbMeta>1) && (hasPhloem) ) {
		 			orientationCircleTab[indexVes-1]=SegmentationUtils.drawOrientationCircle(vesselSeg,xVectTop,yVectTop); 

		 			
		 			//Keep only the main CC of phlotop and regularize area
		 			phloTop=VitimageUtils.connexeBinaryEasierParamsConnexitySelectvol(phloTop, 6, 1);
		 			phloTop=VitimageUtils.thresholdImage(phloTop, 0.5, 1000);
		 			IJ.run(phloTop,"8-bit","");
		 			phloTop=SegmentationUtils.fillHoles2D(phloTop);
		 			phloTop=SegmentationUtils.dilation(phloTop, 2, false);
		 			phloTop=SegmentationUtils.erosion(phloTop, 2, false);
		 			
		 			//Compute proto CC, and exclude ones that are not central with respect to eyes
		 			ImagePlus protoTopCC=VitimageUtils.connexeBinaryEasierParamsConnexitySelectvol(protoTop, 6, 0);
		 			ImagePlus metaTopCC=VitimageUtils.connexeBinaryEasierParamsConnexitySelectvol(metaTop, 6, 0);
		 			inertiaPhlo=SegmentationUtils.inertiaComputationBin(phloTop, false);
		 			inertiaProto=SegmentationUtils.inertiaComputation65536(protoTopCC, true, false, true);
		 			double[][][]inertiaMeta=SegmentationUtils.inertiaComputation65536(metaTopCC, true, false, true);
		 			boolean []valid=new boolean[inertiaProto.length];
		 			for(int i=0;i<valid.length;i++) {
		 				//System.out.println("\nEvaluating likelihood of nose number "+i);
		 				double[]posE1=new double[] {inertiaMeta[0][0][1],inertiaMeta[0][0][2],0};
		 				double[]posE2=new double[] {inertiaMeta[1][0][1],inertiaMeta[1][0][2],0};
		 				double[]posProt=new double[] {inertiaProto[i][0][1],inertiaProto[i][0][2],0};
		 				//TransformUtils.printVector(posE1, "posE1");
		 				//TransformUtils.printVector(posE2, "posE2");
		 				//TransformUtils.printVector(posProt, "posN");

		 				double[]vectE2ToE1=TransformUtils.vectorialSubstraction(posE1,posE2);
		 				double[]vectE1ToE2=TransformUtils.vectorialSubstraction(posE2,posE1);
		 				double[]vectE1ToNose=TransformUtils.vectorialSubstraction(posProt, posE1);
		 				double[]vectE2ToNose=TransformUtils.vectorialSubstraction(posProt, posE2);
		 				//TransformUtils.printVector(vectE1ToE2, "E1E2");
		 				//TransformUtils.printVector(vectE1ToNose, "E1N");
		 				//TransformUtils.printVector(vectE2ToNose, "E2N");
			 			double scalProd1=TransformUtils.scalarProduct(vectE1ToE2,vectE1ToNose);
			 			double scalProd2=TransformUtils.scalarProduct(vectE2ToE1,vectE2ToNose);
			 			//System.out.println("ScalProds="+scalProd1+" , "+scalProd2);
			 			if( (scalProd1<0) || (scalProd2<0) ) {
			 				valid[i]=false;
			 				protoTopCC=VitimageUtils.switchValueInImage(protoTopCC, (int)Math.round(inertiaProto[i][0][0]), 0);
			 				//System.out.println("Img "+imgName+" vessel "+indexVes+" a proto rejected at coords "+TransformUtils.stringVector(posProt, ""));
			 			}
			 			
			 			
			 			//Take data for the CSV
			 			
			 			
		 			}
		 			//if(indexVes>2)VitimageUtils.waitFor(10000);
		 			protoTop=VitimageUtils.thresholdImage(protoTopCC, 0.5, 1000);
		 			IJ.run(protoTop,"8-bit","");
		 			protoTop=SegmentationUtils.fillHoles2D(protoTop);
 					protoTopCC=VitimageUtils.connexeBinaryEasierParamsConnexitySelectvol(protoTop, 6, 0);
		 			inertiaProto=SegmentationUtils.inertiaComputation65536(protoTopCC, true, false, true);
		 			
		 			//TODO : keep only a convex hull around the structures, or a distance map around it (element 46)
		 			ImagePlus merge=VitimageUtils.binaryOperationBetweenTwoImages(protoTop,metaTop,1);
		 			merge=VitimageUtils.binaryOperationBetweenTwoImages(merge,phloTop,1);
		 			merge=SegmentationUtils.dilation(merge, 7, true);
		 			ImagePlus vessel=SegmentationUtils.getConvexHull(0,merge,0,false);
		 			ellVessel=SegmentationUtils.inertiaComputationBin(vessel, false);
		 			//vesCenter=new double[] {ellVessel[0][1],ellVessel[0][2]};
		 			
		 			//TODO : estimate if the fit is good, and if the threshold around structures should be adapted

		 			ImagePlus combinedSeg=SegmentationUtils.generateLabelImageFromMasks(new ImagePlus[] {vessel,phloTop,protoTop,VitimageUtils.nullImage(protoTop),VitimageUtils.nullImage(protoTop),VitimageUtils.nullImage(protoTop),metaTop} ,true);
	 				combinedSeg=VitimageUtils.makeOperationOnOneImage(combinedSeg, 4, 1, false);
	 				combinedSeg.setDisplayRange(0, 7);
	 				combinedSegTab[indexVes-1]=combinedSeg.duplicate();
	 			}
	 			else {
	 				orientationCircleTab[indexVes-1]=SegmentationUtils.drawOrientationCircle(vesselSeg,0,0); 
	 				 
	 				ImagePlus combinedSeg=SegmentationUtils.generateLabelImageFromMasks(new ImagePlus[] {vesselSeg,phloemSeg,VitimageUtils.nullImage(phloemSeg),xylemSeg} ,true);
	 				combinedSeg=VitimageUtils.makeOperationOnOneImage(combinedSeg, 4, 1, false);
	 				combinedSeg.setDisplayRange(0, 7);
	 				combinedSegTab[indexVes-1]=combinedSeg.duplicate();
	 				csvTab[indexVes][2]="NONVALID";csvTab[indexVes][3]=(hasPhloem ? "NoBiMeta" : "NoPhloem");
	 			}

	 			String[]header=new String[] {"ImgName","VesselIndex","InvalidFlag","Stamp","Metainfo",/*0 - 4  */
	 										"XextractCenter","YextractCenter","CoordsExtractRho","CoordsExtractTheta",/* 5 - 8  */
	 										"Xvessel","Yvessel","SurfaceVessel","LongRadiusVessel","ShortRadiusVessel","LongRadiusAngleVessel",/* 9 - 14 */
	 										"NbPhloem","NbMeta","NbProto",/* 15 - 17 */
	 										"XPhloem","YPhloem","SurfacePhloem","LongRadiusPhloem","ShortRadiusPhloem","LongRadiusAnglePhloem",/* 18 - 23 */
	 										"XLeftEye","YLeftEye","SurfaceLeftEye","LongRadiusLeftEye","ShortRadiusLeftEye","LongRadiusAngleLeftEye",/* 24 - 29 */
	 										"XRightEye","YRightEye","SurfaceRightEye","LongRadiusRightEye","ShortRadiusRightEye","LongRadiusAngleRightEye",/* 30 - 35 */
	 										"XProto1","YProto1","SurfaceProto1","LongRadiusProto1","ShortRadiusProto1","LongRadiusAngleProto1",/* 36 - 41 */
	 										"XProto2","YProto2","SurfaceProto2","LongRadiusProto2","ShortRadiusProto2","LongRadiusAngleProto2",/* 42 - 47 */
	 										"XProto3","YProto3","SurfaceProto3","LongRadiusProto3","ShortRadiusProto3","LongRadiusAngleProto3"/* 48 - 53 */};
	 										 			
	 			csvTab[0]=header;
	 			int i=indexVes;
	 			csvTab[i][0]=spec;
	 			csvTab[i][1]=""+indexVes;  
	 			double vesX=Double.parseDouble(vesselCenters[i][2]);
	 			double vesY=Double.parseDouble(vesselCenters[i][3]);
	 			if(VitimageUtils.distance(vesX,vesY, sliceCircle[0], sliceCircle[1])>sliceCircle[2]*1.02) {csvTab[i][2]="NONVALID";csvTab[i][3]="OutOfPerimeter";}
	 			csvTab[i][4]=""+IJ.d2s(distance,6);
	 			//csvTab[i][4]="_"+vesX+"_"+vesY+"_"+sliceCircle[0]+"_"+sliceCircle[1]
	 					//+"_RAD="+VitimageUtils.distance(vesX,vesY, sliceCircle[0], sliceCircle[1]);

	 			
				csvTab[i][5]=""+(vesselCenters[i][2]);//xExtract
	 			csvTab[i][6]=""+(vesselCenters[i][3]);//yExtract
	 			csvTab[i][7]=""+dou(VitimageUtils.distance(vesX,vesY, sliceCircle[0], sliceCircle[1]));//Radius from slice center
	 			csvTab[i][8]=""+dou(Math.atan2(vesY-sliceCircle[1],vesX-sliceCircle[0]));//Angle from slice center

	 			
	 			
	 			csvTab[i][9]=""+dou(ellVessel[0][1])   ;// X Vessel
	 			csvTab[i][10]=""+dou(ellVessel[0][2])   ;// Y
	 			csvTab[i][11]=""+dou(ellVessel[2][0])   ;// Surf
	 			csvTab[i][12]=""+dou(ellVessel[1][0] )  ;// GA
	 			csvTab[i][13]=""+dou(ellVessel[1][1] )  ;// PA
	 			csvTab[i][14]=""+dou(ellVessel[2][2]  ) ;// Angle

	 			if(inertiaPhlo!=null) {
		 			csvTab[i][18]=""+dou(inertiaPhlo[0][1])   ;// X Phloem
		 			csvTab[i][19]=""+dou(inertiaPhlo[0][2])   ;// Y
		 			csvTab[i][20]=""+dou(inertiaPhlo[2][0])   ;// Surf
		 			csvTab[i][21]=""+dou(inertiaPhlo[1][0] )  ;// GA
		 			csvTab[i][22]=""+dou(inertiaPhlo[1][1] )  ;// PA
		 			csvTab[i][23]=""+dou(inertiaPhlo[2][2] )  ;// Angle
		 			if(inertiaPhlo[2][0]<VitimageUtils.EPSILON) {csvTab[i][2]="NONVALID";csvTab[i][3]+="VoidPhloem";}
		 			double[]posE1=new double[] {ellXylem[i1top][0][1],ellXylem[i1top][0][2],0};
		 			double[]posE2=new double[] {ellXylem[i2top][0][1],ellXylem[i2top][0][2],0};
		 			double[]posPhlo=new double[] {inertiaPhlo[0][1],inertiaPhlo[0][2],0};
	 				double[]vectE2ToE1=TransformUtils.vectorialSubstraction(posE1,posE2);
	 				double[]vectE1ToE2=TransformUtils.vectorialSubstraction(posE2,posE1);
	 				double[]vectE1ToPhlo=TransformUtils.vectorialSubstraction(posPhlo, posE1);
	 				double[]vectE2ToPhlo=TransformUtils.vectorialSubstraction(posPhlo, posE2);
		 			double scalProd1=TransformUtils.scalarProduct(vectE1ToE2,vectE1ToPhlo);
		 			double scalProd2=TransformUtils.scalarProduct(vectE2ToE1,vectE2ToPhlo);
		 			//System.out.println("ScalProds="+scalProd1+" , "+scalProd2);
		 			if( (scalProd1<0) || (scalProd2<0) ) {
		 				csvTab[i][2]="NONVALID";csvTab[i][3]+="PhloemNonCentered";
	 				}
	 			}
	 			csvTab[i][15]="1";//nbPhloem 
	 			csvTab[i][16]="2";//NbMeta
	
	
	 			if(ellXylem.length>=2) {
		 			csvTab[i][24]=""+dou(ellXylem[i1top][0][1]  ) ;// X Left eye
		 			csvTab[i][25]=""+dou(ellXylem[i1top][0][2] )  ;// Y
		 			csvTab[i][26]=""+dou(ellXylem[i1top][2][0] )  ;// Surf
		 			csvTab[i][27]=""+dou(ellXylem[i1top][1][0] )  ;// GA
		 			csvTab[i][28]=""+dou(ellXylem[i1top][1][1]  ) ;// PA
		 			csvTab[i][29]=""+dou(ellXylem[i1top][2][2]  ) ;// Angle
	
		 			csvTab[i][30]=""+dou(ellXylem[i2top][0][1]  ) ;// X Right eye
		 			csvTab[i][31]=""+dou(ellXylem[i2top][0][2] )  ;// Y
		 			csvTab[i][32]=""+dou(ellXylem[i2top][2][0]  ) ;// Surf
		 			csvTab[i][33]=""+dou(ellXylem[i2top][1][0] )  ;// GA
		 			csvTab[i][34]=""+dou(ellXylem[i2top][1][1] )  ;// PA
		 			csvTab[i][35]=""+dou(ellXylem[i2top][2][2]  ) ;// Angle
	 			}
 				if(inertiaProto!=null) {
		 			csvTab[i][17]=""+inertiaProto.length; 
		 			
		 			if(inertiaProto.length>0) {
			 			csvTab[i][36]=""+dou(inertiaProto[0][0][1] )  ;// X First proto
			 			csvTab[i][37]=""+dou(inertiaProto[0][0][2] )  ;// Y
			 			csvTab[i][38]=""+dou(inertiaProto[0][2][0] )  ;// Surf
			 			csvTab[i][39]=""+dou(inertiaProto[0][1][0] )  ;// GA
			 			csvTab[i][40]=""+dou(inertiaProto[0][1][1] )  ;// PA
			 			csvTab[i][41]=""+dou(inertiaProto[0][2][2] )  ;// Angle
		 			}
		 			
		 			if(inertiaProto.length>1) {
			 			csvTab[i][42]=""+dou(inertiaProto[1][0][1] )  ;// X Second proto
			 			csvTab[i][43]=""+dou(inertiaProto[1][0][2] )  ;// Y
			 			csvTab[i][44]=""+dou(inertiaProto[1][2][0] )  ;// Surf
			 			csvTab[i][45]=""+dou(inertiaProto[1][1][0] )  ;// GA
			 			csvTab[i][46]=""+dou(inertiaProto[1][1][1] )  ;// PA
			 			csvTab[i][47]=""+dou(inertiaProto[1][2][2] )  ;// Angle
		 			}
		 			
		 			if(inertiaProto.length>2) {
			 			csvTab[i][48]=""+dou(inertiaProto[2][0][1] )  ;// X Third proto
			 			csvTab[i][49]=""+dou(inertiaProto[2][0][2] )  ;// Y
			 			csvTab[i][50]=""+dou(inertiaProto[2][2][0] )  ;// Surf
			 			csvTab[i][51]=""+dou(inertiaProto[2][1][0] )  ;// GA
			 			csvTab[i][52]=""+dou(inertiaProto[2][1][1] )  ;// PA
			 			csvTab[i][53]=""+dou(inertiaProto[2][2][2] )  ;// Angle
		 			}
	 			}
			}
 			IJ.saveAsTiff(VitimageUtils.slicesToStack(orientationCircleTab), new File(dataPath+"/orientation_circle.tif").getAbsolutePath());
 			IJ.saveAsTiff(VitimageUtils.slicesToStack(combinedSegTab), new File(dataPath+"/segmentation_combined.tif").getAbsolutePath());
 			VitimageUtils.writeStringTabInCsv(csvTab, new File(dataPath+"/Vessels_descriptor.csv").getAbsolutePath());
 		}
    }	
    
    
    public static void step_C_10_anomalyEllipsis(int split) {
    	//List data from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();

 		Timer t=new Timer();

 		int incr=0;
 		for(String spec : specNames) {
 			System.out.println("Incr "+(incr++)+" / "+(specNames.length)+" "+spec);
 			String dataPath=new File(splitDir,spec).getAbsolutePath();
 			detectAnomalyEllipsis(split, spec);
 		}
    }
  
    public static void detectAnomalyEllipsis(int split,String spec) {
    	String splitDir=getSplitDir(split);
    	String dataPath=new File(splitDir,spec).getAbsolutePath();
			
    	String[][]tab=pruneCsv(VitimageUtils.readStringTabFromCsv(dataPath+"/Vessels_descriptor.csv"));
 		String csvPath=new File(dataPath,"circle.csv").getAbsolutePath();
		double[]sliceCircle=SegmentationUtils.stringTabToDoubleTab(VitimageUtils.readStringTabFromCsv(csvPath)[1]);
			
		//TODO
		/* QuickFix for a mismatch between rads and degrees inf last execution of step 8. Not useful anymore after the next run of step 8*/
		for(int line=0;line<tab.length;line++) {
			double vesCenX=Double.parseDouble(tab[line][5]);
			double vesCenY=Double.parseDouble(tab[line][6]);
 			tab[line][8]=""+dou(Math.atan2(vesCenY-sliceCircle[1],vesCenX-sliceCircle[0]));//Angle from slice center
		}
		
		
    	int[]vesTest=null;
    	int[]vesOk=null;
    	
    	/*Debug informations. Was used to tune the model*/
    	if(spec.contains("2016_G28_P3_E10")) {
    		vesTest=new int[] {22,105,103,97,118,149,196,171,194,208,239,269,303,347,242};
    		vesOk=new int[] {10,25,33,38,50,55,78,83,87,95,152,163,197};
    	}
    	if(spec.contains("2019_G80_P102_E14")) {
    		vesTest=new int[] {22,149,176,145,134};
    		vesOk=new int[] {10,25,33,38,50,55,78,83,87,95,152,163,197};
    	}
    	if(spec.contains("2019_G80_P63_E13")) {
    		vesTest=new int[] {133};
    		vesOk=new int[] {10,25,33,38,50,55,78,83,87,95,152,163,197};
    	}
    	if(spec.contains("2018_G01_P5_E24")) {
    		vesTest=new int[] {35,17,10,14,8,3,35,41,47,76,66,98,122,135,125,146,145,132,104,116,119,131,150,183,166,197,201,219,212,280,274,267,256,250,239};
    		vesOk=new int[] {10,25,33,38,50,55,78,83,87,95,152,163,197};
    	}
		ImagePlus imgInit=IJ.openImage(getHighResTifDir()+"/"+spec+".tif");
		ImagePlus img=imgInit.duplicate();
		IJ.run(img,"8-bit","");
		img=VitimageUtils.nullImage(img);
		int N=tab.length;
		Timer t=new Timer();

		double exclusionVal=7;
		int nMinExclusion=1;
		boolean log=false;
		double data[][]=new double[9][N];//Surf vess, surf sum xyl, surf phlo, ga, pa , ratio VX, ratioVP, ratio XP, ratio GP
		String []items=new String[] {"Surface vaisseau","Surface meta","Surface phloem", "Long axis","little axis","Ratio VX","Ratio VP","Ratio XP","Ratio GP"};
		for(int n=0;n<N;n++) {
			data[0][n]=safeParseDouble(tab[n][11]);
			data[1][n]=safeParseDouble(tab[n][26])+safeParseDouble(tab[n][32]);
			data[2][n]=safeParseDouble(tab[n][20]);
			data[3][n]=safeParseDouble(tab[n][12]);
			data[4][n]=safeParseDouble(tab[n][13]);
			data[5][n]=data[0][n]/data[1][n];
			data[6][n]=data[0][n]/data[2][n];
			data[7][n]=data[1][n]/data[2][n];
			data[8][n]=data[3][n]/data[4][n];
			if(log)for(int i=0;i<data.length;i++)data[i][n]=Math.log(data[i][n]);
		}
		int[]exclusions=new int[N];
		String[]reasons=new String[N];for(int i=0;i<N;i++)reasons[i]="";
		String[]reas=new String[] {"MADeSvessel","MADeSmeta","MADeSphlo","MADeLongAxis","MADeShortAxis","MADeSv/Sm","MADeSv/Sp","MADeSm/Sp","MADeLong/Short"};
		for(int car=0;car<9;car++) {
			System.out.print("\n"+items[car]);
			double[]vals=VitimageUtils.statistics1D(noNan(data[car]));
			double[]medStat=VitimageUtils.MADeStatsDoubleSided(noNan(data[car]),null);
			int exWrong=0;
			int exRight=0;
//			System.out.println("Mean+-std : ["+dou(vals[0]-vals[1])+" - "+dou(vals[0])+" - "+dou(vals[0]+vals[1]));
			System.out.println(" -> med+-made : ["+dou(medStat[1])+" - "+dou(medStat[0])+" - "+dou(medStat[2]));
			System.out.print("Exclusion list");
			//System.out.println("yCar"+car+"=[");
			for(int i=0;i<N;i++) {
				if(tab[i][2].contains("NONVALID"))continue;
				double val=data[car][i];
				//System.out.println(val+",");
				if(Double.isNaN(val))continue;
				double margin=dou(  ( (val<medStat[0]) ? (-medStat[0]+val)/(medStat[0]-medStat[1]) :  (-medStat[0]+val)/(medStat[2]-medStat[0]) )          );
				if(Math.abs(margin)>exclusionVal) {
					reasons[i]+=reas[car]+"_";
					exclusions[i]++;
					boolean youpi=((vesTest==null) ? true : contains(vesTest, i+1));
					if(youpi)exRight++;
					else exWrong++;
					System.out.print("  out:"+(i+1)+"("+margin+")"+(youpi ? "Y" : "W"));
				}
			}
			System.out.println("\nWrongEx="+exWrong+" , RightEx="+exRight);
		}
		int nbFalseExcluded=0;
		int nbProcessed=0;
		int nbKept=0;
		for(int i=0;i<N;i++) {
			if(tab[i][2].contains("NONVALID")) {
				if((vesTest!=null) && contains(vesTest,i+1))nbFalseExcluded++;
				continue;
			}
//			System.out.println("Index "+(i+1)+" was excluded "+exclusions[i]+" times.");
			if((exclusions[i]>=nMinExclusion)) {
				nbFalseExcluded++;tab[i][2]="NONVALID";tab[i][3]=reasons[i];}
			if((vesTest!=null) && contains(vesTest,i+1) && (exclusions[i]<nMinExclusion))System.out.println("Traitor "+(i+1)+" was released");
			nbProcessed++;
			if(exclusions[i]==0)nbKept++;
		}			
		//TODO : prodscal
		System.out.println("Purge = "+nbFalseExcluded+"/"+((vesTest!=null) ? vesTest.length : 0)+" Total dismissed="+(nbProcessed-nbKept)+"   NbKept="+nbKept+"/"+nbProcessed);
		String[][]tab2=VitimageUtils.readStringTabFromCsv(dataPath+"/Vessels_descriptor.csv");
		for(int i=0;i<tab.length;i++) {
			for(int j=0;j<tab[i].length;j++)tab2[i+1][j]=tab[i][j];
//			tab2[i+1][8]=""+dou(Double.parseDouble(tab2[i+1][8])*(180.0/Math.PI));
		}
		VitimageUtils.writeStringTabInCsv(tab2, dataPath+"/Vessels_descriptor_anomaly_check.csv");
	}
    

    
    
    
    
    public static void step_C_11_generateEllipsisSlices(int split) {
    	//List data from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();
		Timer t=new Timer();

		int incr=0;
    	for(String spec : specNames) {
 			System.out.println("Incr "+(incr++)+" / "+(specNames.length)+" "+spec);
 			String dataPath=new File(splitDir,spec).getAbsolutePath();
 			generateEllipsisSlice(split,spec,false);
 			generateEllipsisSlice(split,spec,true);
		}
    }

    
    public static void step_C_12_generateColouredVoronoi(int split) {
    	//List data from the split
    	String splitDir=getSplitDir(split);
    	String []specNames=new File(splitDir).list();
		Timer t=new Timer();

		int incr=0;
    	for(String spec : specNames) {
 			String dataPath=new File(splitDir,spec).getAbsolutePath();
 			ImagePlus img=generateColouredVoronoiMode1(split,spec);
 			IJ.saveAsTiff(img, splitDir+"/"+spec+"/Cute_distance_voronoi.tif");
		}
    }

    public static ImagePlus generateColouredVoronoiMode1(int split,String spec) {
    	String splitDir=getSplitDir(split);
    	String pathToCsv=splitDir+"/"+spec+"/Vessels_descriptor_anomaly_check.csv";
    	int extMode=1;//0=blanc, 1=grey, 2=keep
    	
    	//Get slice center
    	String[][]data=VitimageUtils.readStringTabFromCsv(splitDir+"/"+spec+"/circle.csv");
    	double[]sliceCenter=new double[] {Double.parseDouble(data[0][0]),Double.parseDouble(data[0][1])};
    	
    	//Get contour mask
		ImagePlus imgContour=IJ.openImage(splitDir+"/"+spec+"/concave_hull.tif");
		ImagePlus imgSeg=IJ.openImage(splitDir+"/"+spec+"/segmentation_no_out.tif");
    	
		//Take voronoi mask and Compute colormap of voronoi against distance
		ImagePlus voronoi=IJ.openImage(splitDir+"/"+spec+"/voronoi.tif");
		ImagePlus imgVorCells=IJ.openImage(splitDir+"/"+spec+"/voronoi.tif");
		ImagePlus imgVorInv=IJ.openImage(splitDir+"/"+spec+"/voronoi.tif");
		IJ.run(imgVorInv,"Invert","");
		IJ.run(imgVorCells,"Invert","");
		Roi[]rois=SegmentationUtils.segmentationToRoi(imgVorInv);
		double[][]centroidsVor=new double[rois.length][];
		imgVorInv.show();
		for(int n=0;n<rois.length;n++) {
			centroidsVor[n]=rois[n].getContourCentroid();
			double[]distances=SegmentationUtils.getDistancesFromCenterToContour(new double[][] {centroidsVor[n]}, imgContour, sliceCenter);
			imgVorInv.setRoi(rois[n]);
			int value=(int)Math.round(distances[0]*255);
			imgVorInv.setColor(new Color(value,value,value));
			imgVorInv.getRoi().setFillColor(new Color(value,value,value));
		    IJ.run(imgVorInv,"Fill","");
			imgVorInv.resetRoi();
		}
		ImagePlus vorInsideDistanceColor=imgVorInv.duplicate();
		ImagePlus vorInsideDistanceColorOne=imgVorInv.duplicate();
		IJ.run(vorInsideDistanceColorOne,"8-bit","");
		IJ.run(vorInsideDistanceColorOne,"32-bit","");
		vorInsideDistanceColorOne=VitimageUtils.makeOperationOnOneImage(vorInsideDistanceColorOne, 3, 255, true);
		
		IJ.run(vorInsideDistanceColor,"Fire","");
		IJ.run(vorInsideDistanceColor,"RGB Color", "");
		
		//Get initial source data
		ImagePlus imgSourceRGB=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
		ImagePlus imgSourceGray=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
		IJ.run(imgSourceGray,"8-bit","");
		double[]circleCoords=SegmentationUtils.stringTabToDoubleTab(				VitimageUtils.readStringTabFromCsv(						new File(splitDir+"/"+spec+"/circle.csv").getAbsolutePath())[0]);	
		ImagePlus circle=IJ.openImage(splitDir+"/"+spec+"/concave_hull.tif");

		//Generate mask of contour inside
		ImagePlus disk=circle.duplicate();
		IJ.run(disk,"Invert","");		IJ.run(disk,"Fill Holes","");		IJ.run(disk,"Invert","");
		ImagePlus voronoiIn=VitimageUtils.binaryOperationBetweenTwoImages(voronoi, disk, 2);
		ImagePlus contours=VitimageUtils.binaryOperationBetweenTwoImages(voronoiIn, circle, 1);
		ImagePlus maskVoronoi=VitimageUtils.getBinaryMaskUnary(contours, 0.5);
		
		
		int[]colorContourVess=new int[] {200,200,200};
		int contourVessSize=2;
		double alphaGrey=2.0;
		int offsetGrey=120;
		double dzetaGrey=1.4;
		double betaGrey=0.15;
		int[]colorContourVoronoi=new int[] {20,20,255};
		ImagePlus contourVess=imgSeg.duplicate();
		SegmentationUtils.dilation(contourVess, contourVessSize, false);
		contourVess=VitimageUtils.binaryOperationBetweenTwoImages(contourVess, imgSeg, 4);
		contourVess=VitimageUtils.getBinaryMaskUnary(contourVess, 0.5);

		//We have segmentation_no_out, mask of cells
		//contours, mask of voronoi and circle
		//imgVorCells, mask of voronoi areas inside the circle
		//contourVess
		//vorInsideDistanceColor, imagecoloured with respect to distance inside influence areas
		
		//Build mask of only distance color
		imgSeg=VitimageUtils.getBinaryMaskUnary(imgSeg, 0.5);
		ImagePlus maskOnlyDistance=VitimageUtils.binaryOperationBetweenTwoImages(contours, contourVess	, 1);
		maskOnlyDistance=VitimageUtils.binaryOperationBetweenTwoImages(maskOnlyDistance, imgSeg	, 1);
		maskOnlyDistance=VitimageUtils.invertBinaryMask(maskOnlyDistance);		
		
		
		//Prepare data
		ImagePlus[]imgSourceVess=VitimageUtils.splitRGBStackHeadLess(imgSourceRGB);
		ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
		ImagePlus imgSourceRGBGrey2=imgSourceRGB.duplicate();
		IJ.run(imgSourceRGBGrey,"8-bit","");
		IJ.run(imgSourceRGBGrey,"32-bit","");
		ImagePlus imgSourceRGBGreyExt=imgSourceRGBGrey.duplicate();
		IJ.run(imgSourceRGBGreyExt,"RGB Color","");
		imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 3	, alphaGrey, true);
		imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 1	, offsetGrey, true);
		imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 2	, 1.0/255.0, true);
		vorInsideDistanceColorOne=VitimageUtils.makeOperationOnOneImage(vorInsideDistanceColorOne, 2, betaGrey, true);
		imgSourceRGBGrey=VitimageUtils.makeOperationBetweenTwoImages(imgSourceRGBGrey, vorInsideDistanceColorOne, 1, true);
		//imgSourceRGBGrey.show();
		//VitimageUtils.waitFor(500000);
		ImagePlus[]imgSourceDist=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
		ImagePlus[]imgContourVess=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
		ImagePlus[]imgContourVor=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
		ImagePlus[]imgExtRGB=VitimageUtils.splitRGBStackHeadLess(extMode==1 ? imgSourceRGBGreyExt : imgSourceRGBGrey2);
		contourVess=VitimageUtils.getBinaryMaskUnaryFloat(contourVess, 0.5);
		contours=VitimageUtils.getBinaryMaskUnaryFloat(contours, 0.5);
		ImagePlus[]ret=new ImagePlus[3];
		disk=VitimageUtils.getBinaryMaskUnaryFloat(disk, 0.5);
		ImagePlus diskOut=VitimageUtils.invertBinaryMask(disk);
		
		
		for(int can=0;can<3;can++) {
			//Reset values of distance color where needed
			imgSourceDist[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], maskOnlyDistance, 2, true);
			//Multiply by grey values
			imgSourceDist[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], imgSourceRGBGrey, 2, true);
			imgSourceDist[can]=VitimageUtils.makeOperationOnOneImage(imgSourceDist[can], 2,dzetaGrey, true);
			IJ.run(imgSourceDist[can],"8-bit","");

			//Reset values of vessels color where needed
			imgSourceVess[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceVess[can],imgSeg, 2, true);
			imgSourceVess[can]=VitimageUtils.makeOperationOnOneImage(imgSourceVess[can],1,1.4, true);
			IJ.run(imgSourceVess[can],"8-bit","");

			//Set contourVess
			imgContourVess[can]=VitimageUtils.makeOperationOnOneImage(contourVess, 2, colorContourVess[can],true);
			IJ.run(imgContourVess[can],"8-bit","");

			//Set contour
			imgContourVor[can]=VitimageUtils.makeOperationOnOneImage(contours, 2, colorContourVoronoi[can],true);
			IJ.run(imgContourVor[can],"8-bit","");

			//Add everything
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], imgSourceVess[can], 1, false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgContourVess[can], 1, false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgContourVor[can], 1, false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can],disk,2, false);
			
			//Process outside
			imgExtRGB[can]=VitimageUtils.makeOperationBetweenTwoImages(imgExtRGB[can],diskOut,2, false);
			imgExtRGB[can]=VitimageUtils.makeOperationOnOneImage(imgExtRGB[can],2, 1.5,false);
			if(extMode==0) {
				imgExtRGB[can]=VitimageUtils.makeOperationOnOneImage(imgExtRGB[can],2,1000000000,false);
			}
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgExtRGB[can], 1, false);
		}
	
		ImagePlus result=VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
		result.setTitle(spec);
		//result.show();
		VitimageUtils.waitFor(20);
		imgVorInv.changes=false;
		imgVorInv.close();//2016_G1_P3_E19 jolie
		return result;
    }

    
    
	public static void generateEllipsisSlice(int split,String spec,boolean afterAnomalyDetection) {
		ImagePlus img=debugEllipsis(split,spec,afterAnomalyDetection);
	   	IJ.saveAsTiff(img,getSplitDir(split)+"/"+spec+"/Vessels_descriptor"+(afterAnomalyDetection?"_anomaly_check":"")+".tif");
	}
   
	public static ImagePlus debugEllipsis(int split,String imgName,boolean afterAnomalyDetection) {
		String pathToCsv=getSplitDir(split)+"/"+imgName+"/Vessels_descriptor"+(afterAnomalyDetection?"_anomaly_check":"")+".csv";
		String pathToImg=getHighResTifDir()+"/"+imgName+".tif";
		return debugEllipsis(pathToCsv,pathToImg);
	}
	
	public static ImagePlus debugEllipsis(String pathToCsv,String pathToLevel1Img) {
		String sorghoDir= getVesselsDir();
    	String[][]tab=VitimageUtils.readStringTabFromCsv(pathToCsv);
		ImagePlus imgInit=IJ.openImage(pathToLevel1Img);
		return debugEllipsis(tab, imgInit);
	}

	public static ImagePlus debugEllipsis(String [][]tab,ImagePlus imgInit) {
		ImagePlus img=imgInit.duplicate();
		IJ.run(img,"8-bit","");
		img=VitimageUtils.nullImage(img);
		int N=tab.length;
		Timer t=new Timer();
		for(int n=1;n<N;n++) {
			if((n%20)==0)t.print(""+n);

			String[]vals=tab[n];
			double dx=Double.parseDouble(vals[5])-100;
			double dy=Double.parseDouble(vals[6])-100;
			if(vals[2].contains("NON")) {VitimageUtils.writeBlackTextOnGivenImage("X"+n, img,20,(int)dx+80,(int)dy+15);continue;}
			
			//draw Vessel
 			double x=Double.parseDouble(vals[9]);
 			double y=Double.parseDouble(vals[10]);
 			double pa=Double.parseDouble(vals[12]);
 			double ga=Double.parseDouble(vals[13]);
 			double angle=Double.parseDouble(vals[14]);
 			SegmentationUtils.drawEllipse(img, x+dx, y+dy, ga, pa, angle+90, 80);
			
			//draw Phlo
 			x=Double.parseDouble(vals[18]);
 			y=Double.parseDouble(vals[19]);
 			pa=Double.parseDouble(vals[21]);
 			ga=Double.parseDouble(vals[22]);
 			angle=Double.parseDouble(vals[23]);
 			SegmentationUtils.drawEllipse(img, x+dx, y+dy, ga, pa, angle+90, 170);

		
			//draw left eye
 			x=Double.parseDouble(vals[24]);
 			y=Double.parseDouble(vals[25]);
 			pa=Double.parseDouble(vals[27]);
 			ga=Double.parseDouble(vals[28]);
 			angle=Double.parseDouble(vals[29]);
 			SegmentationUtils.drawEllipse(img, x+dx, y+dy, ga, pa, angle+90, 255);
			//draw right eye
 			x=Double.parseDouble(vals[30]);
 			y=Double.parseDouble(vals[31]);
 			pa=Double.parseDouble(vals[33]);
 			ga=Double.parseDouble(vals[34]);
 			angle=Double.parseDouble(vals[35]);
 			SegmentationUtils.drawEllipse(img, x+dx, y+dy, ga, pa, angle+90, 255);

			if(vals[17].contains("0")) {VitimageUtils.writeBlackTextOnGivenImage("V"+n, img,20,(int)dx+80,(int)dy+15);continue;}
			//draw Proto1
 			x=Double.parseDouble(vals[36]);
 			y=Double.parseDouble(vals[37]);
 			pa=Double.parseDouble(vals[39]);
 			ga=Double.parseDouble(vals[40]);
 			angle=Double.parseDouble(vals[41]);
 			SegmentationUtils.drawEllipse(img, x+dx, y+dy, ga, pa, angle+90, 120);
			if(vals[17].contains("1")) {VitimageUtils.writeBlackTextOnGivenImage("V"+n, img,20,(int)dx+80,(int)dy+15);continue;}
			//draw Proto2
 			x=Double.parseDouble(vals[42]);
 			y=Double.parseDouble(vals[43]);
 			pa=Double.parseDouble(vals[45]);
 			ga=Double.parseDouble(vals[46]);
 			angle=Double.parseDouble(vals[47]);
 			SegmentationUtils.drawEllipse(img, x+dx, y+dy, ga, pa, angle+90, 120);
			if(vals[17].contains("2")) {VitimageUtils.writeBlackTextOnGivenImage("V"+n, img,20,(int)dx+80,(int)dy+15);continue;}
			//draw Proto3
 			x=Double.parseDouble(vals[48]);
 			y=Double.parseDouble(vals[49]);
 			pa=Double.parseDouble(vals[51]);
 			ga=Double.parseDouble(vals[52]);
 			angle=Double.parseDouble(vals[53]);
 			SegmentationUtils.drawEllipse(img, x+dx, y+dy, ga, pa, angle+90, 120);
 			VitimageUtils.writeBlackTextOnGivenImage("V"+n, img,20,(int)dx+80,(int)dy+15);
 			
		}
		return img;
	}    
	
   
	
	
	
	
	
	
	
	
	
	
	public static int getSplitOf(String imgName) {
		String sorghoDir= getVesselsDir();
    	for(int i=1;i<=9;i++) {
    		if(new File(sorghoDir+"/Data/Splitting/split_"+i+"_over_8/"+imgName).exists())return i;
		}
    	return 0;
	}
	
    public static double dou(double d) {
    	return VitimageUtils.dou(d);
    }
    
    public static String[][]pruneCsv(String[][]tab){
    	String[][]tabRet;
    	int nb=tab.length;
    	for(int i=0;i<tab.length;i++) {
    		if( (tab[i][2].contains("Invalid"))/* || (tab[i][2].contains("NONVALID") ) */) {
    			nb--;
    		}    		
    	}
    	tabRet=new String[nb][tab[0].length];
    	nb=0;
    	for(int i=0;i<tab.length;i++) {
    		if( (!tab[i][2].contains("Invalid"))/* && (!tab[i][2].contains("NONVALID") )*/ ) {
    			tabRet[nb]=tab[i];
    			nb++;
    		}    		
    	}
    	return tabRet;
    }
    
    public static double safeParseDouble(String ddd) {
    	String d=ddd.replace(" ", "");
    	if(d==null)return Double.NaN;
    	if(d.equals(""))return Double.NaN;
    	if(d.equals(" "))return Double.NaN;
    	double dd=0;
    	dd= Double.parseDouble(d);
    	return dd;
    }
    
    public static double[]noNan(double[]tab){
    	double[]ret;
    	int n=tab.length;
    	for(int i=0;i<tab.length;i++)if(Double.isNaN(tab[i]))n--;
    	ret=new double[n];
    	n=0;
    	for(int i=0;i<tab.length;i++)if(!Double.isNaN(tab[i])){ret[n++]=tab[i];};
    	return ret;
    }
       
    public static boolean contains(int[]tab,int d) {
    	for(int dd:tab)if(dd==d)return true;
    	return false;
    }
    
    
    

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
    //TODO
    public static void step_C_08_segment_xylem_parts_and_identify_eyes() {
		int waiting=1000;
		boolean debug=false;
		String sorghoDir= getVesselsDir();
    	String dirExtracts=sorghoDir+"/Data/Test/08_extracts";
    	String dirCenters=sorghoDir+"/Data/Test/04_slice_centers";
		String[]imgNames=new File(dirExtracts).list();
 		for(int indImg=0;indImg<imgNames.length;indImg++) {
 			System.out.println("Starting xylem contour of "+imgNames[indImg]+" = "+(indImg+1)+"/"+imgNames.length);

 			String imgName=imgNames[indImg];
 			String dataPath=new File(dirExtracts,imgName).getAbsolutePath();
 			String csvPath=new File(dirCenters,imgName+"_circle.csv").getAbsolutePath();
 			double[]sliceCircle=SegmentationUtils.stringTabToDoubleTab(VitimageUtils.readStringTabFromCsv(csvPath)[1]);
 			csvPath=new File(dataPath,"Extracts_descriptor.csv").getAbsolutePath();
 			String[][]vesselCenters=VitimageUtils.readStringTabFromCsv(csvPath);
 			
 			String []listVess=new File(dataPath,"Source").list();
 			new File(dataPath,"Orientation_circle").mkdirs();
 			new File(dataPath,"Flags").mkdirs();

 			int N=listVess.length;
 			for(int indexVes=1;indexVes<=N;indexVes++) { 								
 				String vesName="V"+indexVes;
 				System.out.print(vesName+" : "+(indexVes)+" / "+N+"   ");
 				//Vessel
	 			ImagePlus probamap=IJ.openImage(dataPath+"/ProbaMap_vessel_contour/"+vesName+".tif");
	 			probamap=SegmentationUtils.resize(probamap, 200, 200, 1);
	 			ImagePlus voronoi=IJ.openImage(dataPath+"/Voronoi_slice/"+vesName+".tif");
	 			voronoi=VitimageUtils.getBinaryMaskUnary(voronoi, 0.5);
	 			voronoi=VitimageUtils.invertBinaryMask(voronoi);
	 			probamap=VitimageUtils.makeOperationBetweenTwoImages(probamap, voronoi, 2, true);
	 			ImagePlus vesselSeg=getVesselSegmentationFromHighResProbaMap(probamap);
 	
 				//Phloem : Take probamap and voronoi
	 			probamap=IJ.openImage(dataPath+"/ProbaMap_phloemquad/"+vesName+".tif");
	 			probamap=SegmentationUtils.resize(probamap, 200, 200, 1);
	 			ImagePlus source=IJ.openImage(dataPath+"/Source/"+vesName+".tif");
	 			ImagePlus vesselSeg2=VitimageUtils.getBinaryMaskUnary(vesselSeg, 0.5);
	 			probamap=VitimageUtils.makeOperationBetweenTwoImages(probamap, vesselSeg2, 2, true);
	 			ImagePlus phloemSeg=getPhloemSegmentationFromHighResProbaMap(probamap);
	 			
	 			//Xylem : Take probamap and voronoi
	 			probamap=IJ.openImage(dataPath+"/ProbaMap_xylemquad/"+vesName+".tif");
	 			probamap=SegmentationUtils.resize(probamap, 200, 200, 1);
	 			probamap=VitimageUtils.makeOperationBetweenTwoImages(probamap, vesselSeg, 2, true);
	 			vesselSeg2=VitimageUtils.thresholdImage(vesselSeg2, 0.5,	256);
	 			IJ.run(vesselSeg2,"8-bit","");
	 			ImagePlus xylemSeg=getXylemSegmentationFromHighResProbaMap(probamap);
	 			ImagePlus xylemLabels=VitimageUtils.connexe2d(xylemSeg,1 , 256, 0,1E10, 6,0,true);
	 			xylemLabels=SegmentationUtils.convertShortToByte(xylemLabels);	 			
//	 			probamap.show();
//	 			xylemLabels.show();
//	 			VitimageUtils.waitFor(100000);
	 			
	 			//Get equivalent ellipsoid of the vessel, then all possible xylems sorted by size (the first, the bigger)
 				double[][]ellVessel=SegmentationUtils.inertiaComputationBin(vesselSeg,debug);
	 			double[][][]ellXylem=SegmentationUtils.inertiaComputation(xylemLabels,true,debug,true);
	 		

	 			 //Inspect if less than two xylems or no phloem.
	 			 int nbXyl=ellXylem.length-1;
	 			 int nbMeta=nbXyl > 3 ? 3 : nbXyl;
	 			 boolean hasPhloem=(VitimageUtils.maxOfImage(phloemSeg)!=0);

	 			 
	 			 //Compute geometry of vessel
				 double[]sliceCenter=new double[] {sliceCircle[0],sliceCircle[1]};
				 double sliceRadius=sliceCircle[2];
	 			 double[]vesCenterRelativeToSliceCenter=new double[] {Double.parseDouble(vesselCenters[indexVes][2]) ,Double.parseDouble(vesselCenters[indexVes][3]) };
				 double []vectSliceToVess=TransformUtils.vectorialSubstraction(vesCenterRelativeToSliceCenter, sliceCenter);
				 double distanceSliceToVess=TransformUtils.norm(vectSliceToVess);
				 double[]normalizedVectSliceToVess=TransformUtils.normalize(vectSliceToVess);
 				 double normalisedDistanceCenterToSlice=distanceSliceToVess/sliceRadius;
 				 double[]vesCenter=new double[] {ellVessel[0][1],ellVessel[0][2]};
				 double vesselRadius=ellVessel[1][0]/2.0+ellVessel[1][1]/2.0;
		 			 
	 			//For all possible combination Right eye, Left eye
	 			double[][]combScores=new double[nbXyl*(nbXyl-1)][8];
	 			double[][]combIndices=new double[nbXyl*(nbXyl-1)][2];
	 			int index=-1;
	 			int i1top=0;
	 			int i2top=1;
	 			ImagePlus phloTop=null;
	 			ImagePlus protoTop=null;
	 			ImagePlus metaTop=null;
	 			double xVectTop=0;
	 			double yVectTop=0;
	 			double scoreMax=-100;
	 			System.out.println(" has a phloem : "+hasPhloem+" with "+nbXyl+" xylem elements");
	 			for(int i1=0;i1<nbMeta;i1++) {
		 			for(int i2=0;i2<nbMeta;i2++) {
		 				 if (i1==i2) continue;
		 				 index++;
		 				 combIndices[index][0]=i1;
		 				 combIndices[index][1]=i2;
		 				 int labeli1=(int) ellXylem[i1][0][0];
		 				 int labeli2=(int) ellXylem[i2][0][0];
		 				 if(debug)System.out.println("\nStarting evaluation "+index+" : "+i1+" , "+i2+" with labels "+labeli1+"-"+labeli2);

		 				 double[]leftEyeCenter=new double[] {ellXylem[i1][0][1],ellXylem[i1][0][2]};
		 				 double[]rightEyeCenter=new double[] {ellXylem[i2][0][1],ellXylem[i2][0][2]};
		 				 double[]eyesCenter=TransformUtils.vectorialMean(leftEyeCenter, rightEyeCenter);
		 				 double[]vectLeftToRight=TransformUtils.vectorialSubstraction(rightEyeCenter,leftEyeCenter);
		 				 double[]vectMouthToNose=new double[] {vectLeftToRight[1],-vectLeftToRight[0]};//Warning: y downside. 1 0  ->  0 -1     0 1 -> 1 0       -1 0 -> 0 1   0 -1 -> -1 0                x=y  y=-x
		 				 double[]normalizedVectMouthToNose=TransformUtils.normalize(vectMouthToNose);
		 				 
		 				 double distanceInterEyes=TransformUtils.norm(vectLeftToRight);
		 				 double distanceEyesCenterToVesselCenter=TransformUtils.norm(TransformUtils.vectorialSubstraction(eyesCenter, vesCenter));
		 				 if(debug)System.out.println("Eyes center="+eyesCenter[0]+","+eyesCenter[1]);
		 				 if(debug)System.out.println("distanceInterEyes="+distanceInterEyes);
		 				 if(debug)System.out.println("distanceEyesCenterToVesselCenter="+distanceEyesCenterToVesselCenter);

		 				 double meanVolume=(ellXylem[i1][2][0]+ellXylem[i2][2][0])/2.0;
		 				 double diffVolume=Math.abs(ellXylem[i1][2][0]-ellXylem[i2][2][0]);
		 				 if(debug)System.out.println("Volume, mean="+meanVolume+" diff="+diffVolume);
		 				 
		 				 double meanGax=(ellXylem[i1][1][0]+ellXylem[i2][1][0])/2.0;
		 				 double diffGax=Math.abs(ellXylem[i1][1][0]-ellXylem[i2][1][0]);
		 				 if(debug)System.out.println("Gax, mean="+meanGax+" diff="+diffGax);

		 				 double meanPax=(ellXylem[i1][1][1]+ellXylem[i2][1][1])/2.0;
		 				 double diffPax=Math.abs(ellXylem[i1][1][1]-ellXylem[i2][1][1]);
		 				 if(debug)System.out.println("Pax, mean="+meanPax+" diff="+diffPax);

		 				 ImagePlus meta=VitimageUtils.thresholdByteImage(xylemLabels, labeli1-0.1,labeli1+0.1);
		 				 ImagePlus temp=VitimageUtils.thresholdByteImage(xylemLabels, labeli2-0.1,labeli2+0.1);
		 				 meta=VitimageUtils.binaryOperationBetweenTwoImages(meta, temp, 1);
		 				 
		 				 ImagePlus proto=VitimageUtils.switchValueInImage(VitimageUtils.switchValueInImage(xylemLabels, labeli1,0),labeli2,0);
		 				 proto=VitimageUtils.thresholdImage(proto, 0.5, 256);
		 				 ImagePlus []splitProto =SegmentationUtils.splitBinaryPortraitIntoUpperAndDownPart(proto,leftEyeCenter[0],leftEyeCenter[1],rightEyeCenter[0],rightEyeCenter[1]);
		 				 ImagePlus []splitPhlo =SegmentationUtils.splitBinaryPortraitIntoUpperAndDownPart(phloemSeg,leftEyeCenter[0],leftEyeCenter[1],rightEyeCenter[0],rightEyeCenter[1]);
		 				 double volPhloUp=SegmentationUtils.getVolumeOfObject(splitPhlo[0]);
		 				 double volPhloBottom=SegmentationUtils.getVolumeOfObject(splitPhlo[1]);
		 				 double volProtUp=SegmentationUtils.getVolumeOfObject(splitProto[0]);
		 				 double volProtBottom=SegmentationUtils.getVolumeOfObject(splitProto[1]);
		 				 
		 				//Score 0 = cos (mouthToNose , centerToVessel)*(normalisedDistanceCenterToSlice<0.15 ? 1 : 0.5
		 				combScores[index][0]=TransformUtils.scalarProduct(normalizedVectMouthToNose, normalizedVectSliceToVess);
		 				
		 				//Score 1 = vesRad - dist(vesCent,eyesCent) / vesRad
		 				combScores[index][1]=(vesselRadius-distanceEyesCenterToVesselCenter)/(vesselRadius);
		 				
		 				//Score 2 = dist(eyes) / (2*vesRad)
		 				combScores[index][2]=(distanceInterEyes)/(2*vesselRadius);
		 				
		 				//Score 3 = (meanVol - diffVol)/meanVol
		 				combScores[index][3]=(meanVolume-diffVolume*0.5)/meanVolume;
		 				
		 				//Score 4 = (meanGratAx - diffGratAx)/meanGratAx
		 				combScores[index][4]=(meanGax-diffGax*0.5)/meanGax;
		 				
		 				//Score 5 = (meanLitAx - diffLitAx)/meanLitAx
		 				combScores[index][5]=(meanPax-diffPax*0.5)/meanPax;

		 				//Score 6 = relative protemness down
		 				combScores[index][6]=volPhloUp/(VitimageUtils.EPSILON+ volPhloUp+volPhloBottom);
		 				
		 				//Score 7 = relative phloemness up) 
		 				combScores[index][7]=volProtBottom/(VitimageUtils.EPSILON+ volProtUp+volProtBottom);

		 				
		 				if(debug)System.out.println("Scores : ");
		 				if(debug)for(int i=0;i<8;i++){System.out.print("("+i+")="+combScores[index][i]);if(Double.isNaN(combScores[index][i]))combScores[index][i]=-1;}
		 				double score=VitimageUtils.mean(combScores[index]);
		 				if(debug)System.out.println("\nMean score="+score);
		 				if(score>scoreMax) {
		 					//System.out.println("\nWe have got a new champion !\n\n\n");
		 					scoreMax=score;
 							i1top=i1;
		 					i2top=i2;
		 					xVectTop=normalizedVectMouthToNose[0];
		 					yVectTop=normalizedVectMouthToNose[1];
		 					protoTop=splitProto[1];
		 					phloTop=splitPhlo[0];
		 					metaTop=meta;
		 				}
		 			} 
	 			}
	 			if((nbMeta>1) && (hasPhloem) ) {
		 			ImagePlus orientationCircle=SegmentationUtils.drawOrientationCircle(vesselSeg,xVectTop,yVectTop); 
		 			IJ.saveAsTiff(orientationCircle, new File(dataPath+"/Orientation_circle/"+vesName+".tif").getAbsolutePath());

	 				ImagePlus combinedSeg=SegmentationUtils.generateLabelImageFromMasks(new ImagePlus[] {vesselSeg,phloTop,protoTop,VitimageUtils.nullImage(protoTop),VitimageUtils.nullImage(protoTop),VitimageUtils.nullImage(protoTop),metaTop} ,true);
	 				combinedSeg=VitimageUtils.makeOperationOnOneImage(combinedSeg, 4, 1, false);
	 				combinedSeg.setDisplayRange(0, 7);
		 			IJ.saveAsTiff(combinedSeg, new File(dataPath+"/Segmentation_vessel/"+vesName+".tif").getAbsolutePath());
	 			}
	 			else {
		 			ImagePlus orientationCircle=SegmentationUtils.drawOrientationCircle(vesselSeg,0,0); 
	 				IJ.saveAsTiff(orientationCircle, new File(dataPath+"/Orientation_circle/"+vesName+".tif").getAbsolutePath());
	 				ImagePlus combinedSeg=SegmentationUtils.generateLabelImageFromMasks(new ImagePlus[] {vesselSeg,phloemSeg,VitimageUtils.nullImage(phloemSeg),xylemSeg} ,true);
	 				combinedSeg=VitimageUtils.makeOperationOnOneImage(combinedSeg, 4, 1, false);
	 				combinedSeg.setDisplayRange(0, 7);
		 			IJ.saveAsTiff(combinedSeg, new File(dataPath+"/Segmentation_vessel/"+vesName+".tif").getAbsolutePath());
	 			}
 			}
 		}
    }
    
    public void step_C_09_identify_eyes() {
    	
    }
    
    public void step_C_10_segment_phloem_and_proto() {
    	//Get the two eyes. A line joining the centers
    	//Phloem lies only on the top part
    }
    
    public void step_C_10_validate_and_publish_vessel() {
    	
    }
    
    
    public static ImagePlus getVesselSegmentationFromHighResProbaMap(ImagePlus vessel) {
    	IJ.run(vessel, "Median...", "radius=2 stack");
		IJ.run(vessel,"Grays","");
		ImagePlus vessTmp01=VitimageUtils.thresholdImage(vessel, 127.5, 256);//SegmentationUtils.getSegmentationFromProbaMap2D(vessel,0.5,0);
		vessTmp01.setDisplayRange(0, 1);
		IJ.run(vessTmp01,"8-bit","");
		vessTmp01=SegmentationUtils.dilation(vessTmp01, 20, false);
		vessTmp01=SegmentationUtils.erosion(vessTmp01, 20, false);
		if(VitimageUtils.isNullImage(vessTmp01))return vessTmp01;
		//VitimageUtils.printImageResume(vessTmp01);
		
		ImagePlus vessTmp02=VitimageUtils.invertBinaryMask(vessTmp01);
		//VitimageUtils.printImageResume(vessTmp02);
		IJ.run(vessTmp02, "Fill Holes", "stack");
		vessTmp02=VitimageUtils.invertBinaryMask(vessTmp02);		
		vessTmp02=VitimageUtils.connexe2d(vessTmp02,1 , 256, 0,1E10, 6,-1,true);
		IJ.run(vessTmp02,"8-bit","");
		return vessTmp02;
    }

    public static ImagePlus getPhloemSegmentationFromHighResProbaMap(ImagePlus vessel) {
		IJ.run(vessel, "Median...", "radius=2 stack");
		IJ.run(vessel,"8-bit","");
		ImagePlus vessTmp01=VitimageUtils.thresholdImage(vessel, 127, 256);//SegmentationUtils.getSegmentationFromProbaMap2D(vessel,0.5,0);
		IJ.run(vessTmp01,"Grays","");
		if(VitimageUtils.isNullImage(vessTmp01))return vessTmp01;		
		IJ.run(vessTmp01,"8-bit","");
		return vessTmp01;
    }
  
    public static ImagePlus getXylemSegmentationFromHighResProbaMap(ImagePlus vessel) {
		IJ.run(vessel, "Median...", "radius=2 stack");
		IJ.run(vessel,"8-bit","");
		ImagePlus vessTmp01=VitimageUtils.thresholdImage(vessel, 85, 256);//SegmentationUtils.getSegmentationFromProbaMap2D(vessel,0.5,0);
		IJ.run(vessTmp01,"Grays","");
//		vessTmp01=SegmentationUtils.dilation(vessTmp01, 20, false);
//		vessTmp01=SegmentationUtils.erosion(vessTmp01, 20, false);
		if(VitimageUtils.isNullImage(vessTmp01))return vessTmp01;
		
//		ImagePlus vessTmp02=VitimageUtils.invertBinaryMask(vessTmp01);
//		IJ.run(vessTmp02, "Fill Holes", "stack");
//		vessTmp02=VitimageUtils.invertBinaryMask(vessTmp02);		
		//vessTmp02=VitimageUtils.connexe2d(vessTmp02,1 , 256, 0,1E10, 6,-1,true);
		IJ.run(vessTmp01,"8-bit","");
		return vessTmp01;
    }
   

   //Vessel contour quality check according to surface, compactness, great and small axis, then prune

   
   
   //Segment xylema, occlude with vessel area, then prune
   
   
   //Perform axis determination
   

   
   //Output oriented vessels


   
   //Compute missing elements (proto and phloem)

   /** Second phase, training xylem and phloem segmentation  ---------------------------------------*/        		
   public void startSecondPhase (){
		   t=new Timer();
		   int startStep=-1;
		   int lastStep=-1;
		   boolean bothMathieuAndRomain=true;
		   IJ.log("Starting"); 
		   t.print("Starting step 0");
		   if(startStep<=-1 && lastStep>=-1)step_B_00_testTernalTrain();
		   if(startStep<=0 && lastStep>=0)step_B_00_collectJsonAndPrepareSourceAndTargetLabels(bothMathieuAndRomain);
		   t.print("Starting step 1");
		   if(startStep<=1 && lastStep>=1)step_B_01_splitTrainValTest(bothMathieuAndRomain);
		   t.print("Starting step 2");
		   if(startStep<=2 && lastStep>=2)step_B_02_augmentTrainingData();
		   t.print("Starting step 3");
		   if(startStep<=3 && lastStep>=3)step_B_03_trainModelsSub();
		   t.print("Starting step 4");
		   if(startStep<=4 && lastStep>=4)step_B_04_applyModels(3);
		   if(startStep<=4 && lastStep>=4)step_B_04_applyModels(2);
		   if(startStep<=4 && lastStep>=4)step_B_04_applyModels(1);
		   t.print("Starting step 5");
		   if(startStep<=5 && lastStep>=5)step_B_05_extract_structures(3);

		   
		   IJ.log("End test.");
		   System.out.println("End test.");
		}        

   
   //Collect json from Romain and Mathieu, prune vessels when not centered and transform it into a setup for training to segment vessel, phloem and xylem 
    //Input : json files
    //Output : training setup
    public void step_B_00_collectJsonAndPrepareSourceAndTargetLabels(boolean bothMathieuAndRomain){
    	String sorghoDir= getVesselsDir();
    	String jsonDir=sorghoDir+(bothMathieuAndRomain? "/Data/Insights_and_annotations/Vessels_dataset/Full_jsons/" : "/Data/Insights_and_annotations/Vessels_dataset/Separated_jsons/Romain/");
    	String dirSourceIn=sorghoDir+(bothMathieuAndRomain? "/Data/Insights_and_annotations/Vessels_dataset/Selected_vessels_jpg/" : "/Data/Insights_and_annotations/Vessels_dataset/Dir_Ro_VIA/");
    	String dirSegOut=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Image_and_annotations_for_training";
    	
    	//Process phloem data
    	System.out.println("Binarize phloem");
    	SegmentationUtils.jsonToBinarySlices(jsonDir+"/via_regions_phloem.json",dirSourceIn,dirSegOut+"/Phloem",false);

    	//Process proto data
    	System.out.println("Binarize proto");
		SegmentationUtils.jsonToBinarySlices(jsonDir+"/via_regions_proto.json",dirSourceIn,dirSegOut+"/Proto",false);

		//Process meta data
    	System.out.println("Binarize meta");
		SegmentationUtils.jsonToBinarySlices(jsonDir+"/via_regions_meta.json",dirSourceIn,dirSegOut+"/Meta",false);
    	

		
		//Gather source images (and eventually prune)
    	System.out.println("Gather source");
		for (String img : new File(dirSourceIn).list(new FilenameFilter() {public boolean accept(File dir, String name) { return name.contains(".jpg");} }) ) {
						System.out.println(img);
						ImagePlus im=IJ.openImage(dirSourceIn+"/"+img);
						IJ.saveAsTiff(im,dirSegOut+"/Source/"+img);
			
		}

    	//Fuse proto and meta
    	System.out.println("Binarize xylem");
		for (String img : new File(dirSegOut+"/Meta").list()) {
						ImagePlus imgMeta=IJ.openImage(dirSegOut+"/Meta/"+img);
						ImagePlus imgProto=IJ.openImage(dirSegOut+"/Proto/"+img);
						ImagePlus imgXylem=VitimageUtils.binaryOperationBetweenTwoImages(imgMeta, imgProto, 1);
						IJ.saveAsTiff(imgXylem, dirSegOut+"/Xylem/"+img);
		}

		String []exceptListString=new String[] {"Segmentation_vessel_256.tif","Segmentation_vessel_288.tif"};
		int []exceptListVal=new int[]       {        6                       ,         6                   };
		
		//Make convex hull
		for (String img : new File(dirSegOut+"/Meta").list()) {
			System.out.println(img);
			ImagePlus xyl=IJ.openImage(dirSegOut+"/Xylem/"+img);
			ImagePlus phlo=IJ.openImage(dirSegOut+"/Phloem/"+img);
			ImagePlus imgVess=VitimageUtils.binaryOperationBetweenTwoImages(xyl,phlo, 1);
			IJ.saveAsTiff(imgVess, dirSegOut+"/Xylem_and_phloem/"+img);
			int dilateSpace=12;
			for(int i=0;i<exceptListString.length;i++) {
				if(exceptListString[i].equals(img))dilateSpace=exceptListVal[i];
			}
			imgVess=SegmentationUtils.getConvexHull(dilateSpace,imgVess,0,false);
			imgVess=VitimageUtils.invertBinaryMask(imgVess);
			IJ.run(imgVess,"Dilate","");
			IJ.run(imgVess,"Dilate","");
			imgVess=VitimageUtils.invertBinaryMask(imgVess);
			IJ.saveAsTiff(imgVess, dirSegOut+"/Vessel_convex_hull/"+img);
		}
		System.out.println("Ok");
		VitimageUtils.waitFor(10000);

		//TODO : prune data with vessels out from the center of extract
//		exceptListVal=new int[] {225};
		//or not todo : this means no problem for training, and this should be a useful stress test
		
		
    }
    

	//Collect json from Romain and Mathieu, prune vessels when not centered and transform it into a setup for training to segment vessel, phloem and xylem 
    //Input : json files
    //Output : training setup
    public void step_B_01_splitTrainValTest(boolean bothMathieuAndRomain){
       	String sorghoDir= getVesselsDir();
    	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Image_and_annotations_for_training/";
    	String dirInSource=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Selected_vessels/";
    	String dirOut=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Split_train_val_test/";
    	int index;
    	String rep;
    	int factor=bothMathieuAndRomain ? 2 : 1;
    	ImagePlus[]train=new ImagePlus[200*factor];
    	ImagePlus[]val=new ImagePlus[50*factor];
    	ImagePlus[]test=new ImagePlus[50*factor];
    	ImagePlus[]trainSource=new ImagePlus[200*factor];
    	ImagePlus[]valSource=new ImagePlus[50*factor];
    	ImagePlus[]testSource=new ImagePlus[50*factor];

    	for(String target : new String[] {"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"}) {
	    	for(int i=1;i<=300;i++) {
	    		if(i<=200)      { index=i-1;    train[index]=IJ.openImage(dirIn+target+"/Segmentation_vessel_"+i+".tif"); trainSource[index]=IJ.openImage(dirInSource+"vessel_"+i+".tif");}
	    		else if(i<=250) { index=i-201;	val[index]=IJ.openImage(dirIn+target+"/Segmentation_vessel_"+i+".tif");   valSource[index]=IJ.openImage(dirInSource+"vessel_"+i+".tif");}
	    		else            { index=i-251;  test[index]=IJ.openImage(dirIn+target+"/Segmentation_vessel_"+i+".tif");  testSource[index]=IJ.openImage(dirInSource+"vessel_"+i+".tif");}
	    	}
	    	if(bothMathieuAndRomain)for(int i=301;i<=600;i++) {
	    		if(i<=500)      { index=i-101;  train[index]=IJ.openImage(dirIn+target+"/Segmentation_vessel_"+i+".tif"); trainSource[index]=IJ.openImage(dirInSource+"vessel_"+i+".tif");}
	    		else if(i<=550) { index=i-451;  val[index]=IJ.openImage(dirIn+target+"/Segmentation_vessel_"+i+".tif");   valSource[index]=IJ.openImage(dirInSource+"vessel_"+i+".tif");}
	    		else            { index=i-501;  test[index]=IJ.openImage(dirIn+target+"/Segmentation_vessel_"+i+".tif");  testSource[index]=IJ.openImage(dirInSource+"vessel_"+i+".tif");}
	    	}
	    	ImagePlus imgTrain=VitimageUtils.slicesToStack(train);
	    	ImagePlus imgVal=VitimageUtils.slicesToStack(val);
	    	ImagePlus imgTest=VitimageUtils.slicesToStack(test);
	    	IJ.saveAsTiff(imgTrain, dirOut+"Train/"+target+".tif");
	    	IJ.saveAsTiff(imgVal, dirOut+"Val/"+target+".tif");
	    	IJ.saveAsTiff(imgTest, dirOut+"Test/"+target+".tif");

	    	ImagePlus imgTrainSource=VitimageUtils.slicesToStack(trainSource);
	    	ImagePlus imgValSource=VitimageUtils.slicesToStack(valSource);
	    	ImagePlus imgTestSource=VitimageUtils.slicesToStack(testSource);
	    	IJ.saveAsTiff(imgTrainSource, dirOut+"Train/Source.tif");
	    	IJ.saveAsTiff(imgValSource, dirOut+"Val/Source.tif");
	    	IJ.saveAsTiff(imgTestSource, dirOut+"Test/Source.tif");
    	}    	
    }

    
	//Make data augmentation to prepare training
    public void step_B_02_augmentTrainingData(){
    	String sorghoDir= getVesselsDir();
    	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Split_train_val_test/Train/";
    	String dirOut=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Train_augmented/";
    	ImagePlus sourceOut=IJ.openImage(dirIn+"Source.tif");
    	System.out.println("Color...");
    	sourceOut=SegmentationUtils.brightnessAugmentationStack(sourceOut,false,2,ratioStdBrightness,true);
    	System.out.println("Ok.");
    	IJ.saveAsTiff(sourceOut, dirOut+"Source.tif");
    	ImagePlus []chans=VitimageUtils.splitRGBStackHeadLess(sourceOut.duplicate());
    	IJ.saveAsTiff(chans[0], dirOut+"Source_Red.tif");
    	IJ.saveAsTiff(chans[1], dirOut+"Source_Green.tif");
    	IJ.saveAsTiff(chans[2], dirOut+"Source_Blue.tif");
    	chans=VitimageUtils.getHSB(sourceOut.duplicate());
    	IJ.saveAsTiff(chans[0], dirOut+"Source_Hue.tif");
    	IJ.saveAsTiff(chans[1], dirOut+"Source_Saturation.tif");
    	IJ.saveAsTiff(chans[2], dirOut+"Source_Brightness.tif");
    	
        for(String target : new String[] {"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"}) {
        	ImagePlus maskOut=IJ.openImage(dirIn+target+".tif");
        	maskOut=SegmentationUtils.brightnessAugmentationStack(maskOut,true,2,ratioStdBrightness,true);
        	IJ.saveAsTiff(maskOut, dirOut+target+".tif");
        }
    }
	
    	
	//Train 3 x 6 models (vessel, phlo, xy) X (R,G,B,H,S,B)
    public void step_B_03_trainModels(){
    	String sorghoDir= getVesselsDir();
    	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Train_augmented/";
    	String dirOut=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Models/";
    	int tot=30;
    	int incr=0;
    	String []targets=new String[] {"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"};
    	String []channels=new String[] {"Red","Green","Blue","Hue","Saturation","Brightness"};
		for(String channel : channels) {
			ImagePlus[]imgsMask=new ImagePlus[5];
			String[]modelNames=new String[5];
			incr++;
			for(int t=0;t<targets.length;t++) {
	    		System.out.println("\n----------  Processing "+incr+" / "+tot+" : "+channel+" ----------");
				String target=targets[t];
				imgsMask[t]=IJ.openImage(dirIn+target+".tif");
				modelNames[t]=""+dirOut+"model_"+target+"_"+channel;
	            Runtime. getRuntime(). gc();
	            ImagePlus source=IJ.openImage(dirIn+"Source_"+channel+".tif");
	            SegmentationUtils.wekaTrainModel(source,imgsMask[t],SegmentationUtils.getStandardRandomForestParamsVessels(incr),SegmentationUtils.getStandardRandomForestFeaturesVessels(),modelNames[t]);
			}        		
    	}    		
    }

	//Train 3 x 6 models (vessel, phlo, xy) X (R,G,B,H,S,B)
    public void step_B_03_trainModelsSub(){
    	String sorghoDir= getVesselsDir();
    	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Train_augmented/";
    	String dirOut=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Models/";
    	int tot=30;
    	int incr=0;
    	String []targets=new String[] {"Vessel_convex_hull"};
//    	String []targets=new String[] {"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"};
    	String []channels=new String[] {"Red","Green","Blue","Hue","Saturation","Brightness"};
		for(String channel : channels) {
			ImagePlus[]imgsMask=new ImagePlus[5];
			String[]modelNames=new String[5];
			incr++;
			for(int t=0;t<targets.length;t++) {
	    		System.out.println("\n----------  Processing "+incr+" / "+tot+" : "+channel+" ----------");
				String target=targets[t];
				imgsMask[t]=IJ.openImage(dirIn+target+".tif");
				modelNames[t]=""+dirOut+"model_"+target+"_"+channel;
	            Runtime. getRuntime(). gc();
	            ImagePlus source=IJ.openImage(dirIn+"Source_"+channel+".tif");
	            source=SegmentationUtils.resize(source, source.getWidth()/2, source.getHeight()/2, source.getNSlices());
	            imgsMask[t]=SegmentationUtils.resize(imgsMask[t], source.getWidth(), source.getHeight(), source.getNSlices());
	            imgsMask[t]=VitimageUtils.thresholdImage(imgsMask[t], 127.5, 256);
	            source=SegmentationUtils.brightnessAugmentationStackGrayScale(source, false, 2, 0.1, true);
	            imgsMask[t]=SegmentationUtils.brightnessAugmentationStackGrayScale(imgsMask[t], true, 2, 0.1, true);
	            ImagePlus []img=SegmentationUtils.rotationAugmentationStack(source, imgsMask[t], 0.5, 1, 1);

	            source=img[0];
	            imgsMask[t]=img[1];
	            source.show();
	            imgsMask[t].show();
	            SegmentationUtils.wekaTrainModel(source,imgsMask[t],SegmentationUtils.getStandardRandomForestParamsVesselsSub(incr),SegmentationUtils.getStandardRandomForestFeaturesVesselsSub(),modelNames[t]+"_sub");
	    	}    	
		}
    }

    
	//Train 3 x 6 models (vessel, phlo, xy) X (R,G,B,H,S,B)
    public void step_B_00_testTernalTrain(){
    	String sorghoDir= getVesselsDir();
    	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Train_augmented/";
    	String dirOut=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Models/";
    	int tot=30;
    	int incr=0;
    	String []targets=new String[] {"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"};
    	targets=new String[] {"XyPhlo"};
    	String []channels=new String[] {"Red","Green","Blue","Hue","Saturation","Brightness"};
		ImagePlus imgMaskVess=IJ.openImage(dirIn+"Vessel_convex_hull"+".tif");
		ImagePlus imgMaskXylem=IJ.openImage(dirIn+"Xylem"+".tif");
		ImagePlus imgMaskPhlo=IJ.openImage(dirIn+"Phloem"+".tif");


		
		for(String channel : channels) {
			String[]modelNames=new String[1];
			incr++;
            ImagePlus source=IJ.openImage(dirIn+"Source_"+channel+".tif");
    		//Prepare ternal image : out, vessel \ xylem, xylem
    		ImagePlus xyPhloMask=SegmentationUtils.generateLabelImageFromMasks(new ImagePlus[] {imgMaskVess,imgMaskXylem,imgMaskPhlo},true);

    		Runtime. getRuntime(). gc();
    		System.out.println("\n----------  Processing "+incr+" / "+tot+" : "+channel+" ----------");
			modelNames[0]=""+dirOut+"model_"+targets[0]+"_"+channel;
			
			source=SegmentationUtils.resize(source, source.getWidth()/2, source.getHeight()/2, source.getNSlices());
			xyPhloMask=SegmentationUtils.resizeNearest(xyPhloMask, source.getWidth(), source.getHeight(), source.getNSlices());
            
			source=SegmentationUtils.brightnessAugmentationStackGrayScale(source, false, 2, 0.1, true);
			xyPhloMask=SegmentationUtils.brightnessAugmentationStackGrayScale(xyPhloMask, true, 2, 0.1, true);
            ImagePlus []img=SegmentationUtils.rotationAugmentationStack(source, xyPhloMask, 0.5, 1, 1);
            source=img[0];
            xyPhloMask=img[1];
            source.show();
            xyPhloMask.show();
			
			SegmentationUtils.wekaTrainModelNary(source,xyPhloMask,SegmentationUtils.getStandardRandomForestParamsVesselsSubSub(incr),SegmentationUtils.getStandardRandomForestFeaturesVesselsSubSub(),modelNames[0],false);
		}    		
    }
	//Test segmentation models
    public void step_B_04_applyModels(int oneForTrainTwoForValThreeForTest){
    	String sorghoDir= getVesselsDir();
    	String rep=oneForTrainTwoForValThreeForTest==1 ? "Train" : oneForTrainTwoForValThreeForTest==2 ? "Val" : "Test";
    	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Split_train_val_test/"+rep+"/";
    	String dirModel=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Models/";
    	String []targets=new String[] {"PhloTer","XylemTer","Phloem","Vessel_convex_hull","Xylem"};//{"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"};
    	String []channels=new String[] {"Red","Green","Blue","Hue","Saturation","Brightness"};
    	int tot=30;
    	int incr=0;

    	//Split channels
    	boolean first=true;
    	for(String target : targets) {
    		VitimageUtils.garbageCollector();
    		ImagePlus []resultTab=new ImagePlus[channels.length];
    		ImagePlus img=IJ.openImage(dirIn+"Source.tif");
	    	ImagePlus []chans1=VitimageUtils.splitRGBStackHeadLess(img.duplicate());
	    	ImagePlus []chans2=VitimageUtils.getHSB(img.duplicate());
	    	ImagePlus []chans=new ImagePlus[] {chans1[0],chans1[1],chans1[2],chans2[0],chans2[1],chans2[2]};
    		for(int i=0;i<channels.length;i++) {
        		incr++;
        		VitimageUtils.printImageResume(chans[i]);
        		System.out.println("\n----------  Processing "+incr+" / "+tot+" : "+target+" "+channels[i]+" ----------");
    			resultTab[i]=SegmentationUtils.wekaApplyModel(chans[i],SegmentationUtils.getStandardRandomForestParamsVessels(i),SegmentationUtils.getStandardRandomForestFeaturesVessels(),dirModel+"model_"+target+"_"+channels[i]);
    			//if(!target.equals("PhloXylQuad"))resultTab[i]=new Duplicator().run(resultTab[i], 2, 2,1,resultTab[i].getNSlices(),1,1);
    			for(int c=0;c<resultTab[i].getNChannels();c++) {
    				resultTab[i].setC(1+c);
    				IJ.run(resultTab[i],"Fire","");
    			}
    			IJ.saveAsTiff(resultTab[i], dirIn+target+"_seg_"+channels[i]+".tif");
    		}
    		ImagePlus result=VitimageUtils.meanOfImageArray(resultTab);
			for(int c=0;c<result.getNChannels();c++) {
				result.setC(1+c);
				IJ.run(result,"Fire","");
			}
    		IJ.saveAsTiff(result, dirIn+target+"_seg_altogether.tif");  
    		if(!first)return;
    		else first=false;
    	}
    }

    public void step_B_05_extract_structures(int oneForTrainTwoForValThreeForTest) {
	   	String sorghoDir= getVesselsDir();
	   	String rep=oneForTrainTwoForValThreeForTest==1 ? "Train" : oneForTrainTwoForValThreeForTest==2 ? "Val" : "Test";
	   	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Split_train_val_test/"+rep+"/";
	   	String []targets=new String[] {"Phloem","Vessel_convex_hull","Xylem","MetaPhloQuad"};//{"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"};
		ImagePlus sourceRGB=IJ.openImage(dirIn+"Source.tif");    		
		ImagePlus source=IJ.openImage(dirIn+"Source.tif");    		
		ImagePlus source2=IJ.openImage(dirIn+"Source.tif");    		
		IJ.run(source,"8-bit","");
		IJ.run(source,"32-bit","");
		source=VitimageUtils.makeOperationOnOneImage(source, 2, 1.0/255, true);
		source.setDisplayRange(0, 1);
		ImagePlus vessel=IJ.openImage(dirIn+targets[1]+"_seg_altogether.tif");    		
		ImagePlus xylem=IJ.openImage(dirIn+targets[2]+"_seg_altogether.tif");    		
		ImagePlus phloem=IJ.openImage(dirIn+targets[0]+"_seg_altogether.tif");    		
		ImagePlus metaphloem=IJ.openImage(dirIn+targets[3]+"_seg_altogether.tif");    		
		boolean useTernary=true;
	    if(useTernary) {
	    	phloem=new Duplicator().run(metaphloem,3,3,1,phloem.getNSlices(),1,1);
	    	xylem=new Duplicator().run(metaphloem,2,2,1,phloem.getNSlices(),1,1);
	    }
		boolean debug=false;
		if(debug) {
			int z0=20;
			int zF=25;					
			sourceRGB=new Duplicator().run(sourceRGB,1,1,z0,zF,1,1);
			source=new Duplicator().run(source,1,1,z0,zF,1,1);
			vessel=new Duplicator().run(vessel,1,1,z0,zF,1,1);
			xylem=new Duplicator().run(xylem,1,1,z0,zF,1,1);
			phloem=new Duplicator().run(phloem,1,1,z0,zF,1,1);
		}
		int Z=xylem.getNSlices();
		IJ.run(xylem, "Median...", "radius=2 stack");
		IJ.run(phloem, "Median...", "radius=2 stack");
		IJ.run(vessel, "Median...", "radius=2 stack");
	
		IJ.run(vessel, "Median...", "radius=2 stack");

		//Step 1 : vessel segmentation

		//Get areas connected to a local maxima, and their influence zone upper to 0.5 proba
		ImagePlus vessTmp01=SegmentationUtils.getSegmentationFromProbaMap2D(vessel,0.5,2);
		ImagePlus debug01=VitimageUtils.makeOperationBetweenTwoImages(vessTmp01, source,2, true);debug01.setTitle("1 - Seg vessel from proba map");
		//.show();IJ.run(debug01,"Fire","");VitimageUtils.waitFor(2000);
		
		//Get most central component convex hull
		ImagePlus vessTmp02=SegmentationUtils.selectCentralMostRoi(vessTmp01);
		ImagePlus debug02=VitimageUtils.makeOperationBetweenTwoImages(vessTmp02, source,2, true);debug02.setTitle("2 - Vessels central Roi");
		//debug02.show();IJ.run(debug02,"Fire","");VitimageUtils.waitFor(2000);

		//Fill holes, and connect areas
		int nAct=4;
		for(int i=0;i<nAct;i++)IJ.run(vessTmp02, "Dilate", "stack");
		vessTmp02=VitimageUtils.invertBinaryMask(vessTmp02);
		IJ.run(vessTmp02, "Fill Holes", "stack");
		for(int i=0;i<nAct;i++)IJ.run(vessTmp02, "Dilate", "stack");
		vessTmp02=VitimageUtils.invertBinaryMask(vessTmp02);
		ImagePlus debug03=VitimageUtils.makeOperationBetweenTwoImages(vessTmp02, source,2, true);debug03.setTitle("3 - Fill holes of vessel");
		//debug03.show();IJ.run(debug03,"Fire","");VitimageUtils.waitFor(2000);
	
		//Get convex hull
		ImagePlus vessTmp04=SegmentationUtils.getConvexHull(0, vessTmp02, 0,false);vessTmp04.show();
		ImagePlus debug04=VitimageUtils.makeOperationBetweenTwoImages(vessTmp04, source,2, true);debug04.setTitle("4 - Final Vessel segmentation");
		ImagePlus vesselSeg=vessTmp04.duplicate();
		//.show();IJ.run(debug04,"Fire","");VitimageUtils.waitFor(2000);
		//phloem=SegmentationUtils.erosion(phloem, 11, false);
		//xylem=SegmentationUtils.erosion(xylem, 11, false);
		xylem=VitimageUtils.makeOperationBetweenTwoImages(xylem, vesselSeg, 2, true);
		phloem=VitimageUtils.makeOperationBetweenTwoImages(phloem, vesselSeg, 2, true);
		vessel.show();
		phloem.show();
		xylem.show();
		
		source2.setTitle("Source2");
		for(int z=0;z<Z;z++) {
			ImagePlus ves=new Duplicator().run(vesselSeg,1,1,z+1,z+1,1,1);
			double[]centers= SegmentationUtils.getMassCenter(ves);
			source2=SegmentationUtils.drawRectangleInRGBImage(source2,(int)centers[0]-1,(int)centers[1]-1,2,2,z,Color.yellow);
			double[]centers2= SegmentationUtils.massCenterIntensityWeighted(phloem, z);
			source2=SegmentationUtils.drawRectangleInRGBImage(source2,(int)centers2[0]-1,(int)centers2[1]-1,2,2,z,Color.green);
			double angle=java.lang.Math.atan2(centers2[1]-centers[1], centers2[0]-centers[0]);
			//System.out.println(angle);//positif de 0 a pi, negatif sinon. Cible = - pi / 2
			angle=angle*180.0/Math.PI;
			double deltaAngle= angle > 0  ? (270-angle) : (-90-angle);
			System.out.println(z+" : angle="+angle+"     delta="+deltaAngle);
			source2.setSlice(z+1);
			IJ.run(source2, "Rotate... ", "angle="+(-angle-90)+" grid=1 interpolation=Bilinear slice");
			
		}
		source2.show();
		source.show();
		VitimageUtils.waitFor(5000000);
		
		
		//Step 2 : xylem segmentation
		ImagePlus xylemTmp01=SegmentationUtils.getSegmentationFromProbaMap2D(xylem, 0.5, 1);
		xylemTmp01.show();	
		ImagePlus xylemTmp02=VitimageUtils.binaryOperationBetweenTwoImages(xylemTmp01, vesselSeg, 2);
		ImagePlus xylemSeg=xylemTmp02.duplicate();
		ImagePlus debug05=VitimageUtils.makeOperationBetweenTwoImages(xylemTmp02, source,2, true);debug05.setTitle("5 - Xylem segmentation");
		debug05.show();	IJ.run(debug05,"Fire","");VitimageUtils.waitFor(2000);
	
	
		//Step 3 : phloem segmentation. Exclude Xylem+1dil
		ImagePlus xylExclude=xylemSeg.duplicate();
		nAct=0;
		for(int i=0;i<nAct;i++)IJ.run(xylExclude, "Erode", "stack");
		xylExclude=VitimageUtils.invertBinaryMask(xylExclude);
		xylExclude=VitimageUtils.getBinaryMaskUnary(xylExclude, 0.5);
		ImagePlus phloemTmp01=VitimageUtils.makeOperationBetweenTwoImages(phloem, vesselSeg, 2, true);
		//phloemTmp01=VitimageUtils.makeOperationBetweenTwoImages(phloemTmp01, xylExclude, 2, true);
		phloemTmp01=VitimageUtils.makeOperationOnOneImage(phloemTmp01, 2, 1.0/255, true);
		phloemTmp01.show();phloemTmp01.setTitle("5 - 2 ProbaMap phlo without vess");IJ.run(phloemTmp01,"Fire","");VitimageUtils.waitFor(1000);		
		ImagePlus phloemTmp02=SegmentationUtils.getSegmentationFromProbaMap2D(phloemTmp01,0.5,-6);
		ImagePlus debug06=VitimageUtils.makeOperationBetweenTwoImages(phloemTmp02, source, 2, true);
		debug06.setTitle("6 - Temp Seg of Phlo");debug06.show();IJ.run(debug06,"Fire","");VitimageUtils.waitFor(2000);

		//Erode, take main, then dilate
		nAct=3;
		VitimageUtils.printImageResume(phloemTmp02);
		for(int i=0;i<nAct;i++)IJ.run(phloemTmp02, "Dilate", "stack");
		ImagePlus phloemTmp03=VitimageUtils.connexe2d(phloemTmp02, 1, 1E8, 0, 1E8, 6, 1, false);
		IJ.run(phloemTmp03,"8-bit","");
		phloemTmp03=VitimageUtils.invertBinaryMask(phloemTmp03);
		for(int i=0;i<nAct;i++)IJ.run(phloemTmp03, "Dilate", "stack");
		phloemTmp03=VitimageUtils.invertBinaryMask(phloemTmp03);
		ImagePlus phloemSeg=phloemTmp03.duplicate();
		ImagePlus debug07=VitimageUtils.makeOperationBetweenTwoImages(phloemTmp03, source, 2, true);
		debug07.setTitle("7 - Selected phloem area");debug07.show();IJ.run(debug07,"Fire","");VitimageUtils.waitFor(2000);
		ImagePlus deb1Xyl=xylemSeg.duplicate();
		ImagePlus deb2Phlo=phloemSeg.duplicate();		
		xylemSeg=VitimageUtils.binaryOperationBetweenTwoImages(xylemSeg, phloemSeg, 4);
		
		//Show a summary debug image
		//Outside at 0.1, Vessel at 0.4, phlo at 0.8 and xyl at 1
		ImagePlus vesUnary=VitimageUtils.getBinaryMaskUnary(vesselSeg, 0.5);
		ImagePlus xylUnary=VitimageUtils.getBinaryMaskUnary(xylemSeg, 0.5);
		ImagePlus phloUnary=VitimageUtils.getBinaryMaskUnary(phloemSeg, 0.5);
		IJ.run(vesUnary,"32-bit","");
		IJ.run(xylUnary,"32-bit","");
		IJ.run(phloUnary,"32-bit","");
		vesUnary=VitimageUtils.makeOperationOnOneImage(vesUnary, 2, 0.6, true);
		xylUnary=VitimageUtils.makeOperationOnOneImage(xylUnary, 2, 1.5, true);
		phloUnary=VitimageUtils.makeOperationOnOneImage(phloUnary, 2, 1.0, true);
		ImagePlus fullMask=VitimageUtils.makeOperationBetweenTwoImages(vesUnary, xylUnary, 1, true);
		fullMask=VitimageUtils.makeOperationBetweenTwoImages(fullMask, phloUnary, 1, true);

		ImagePlus[]sourceTabRGB=VitimageUtils.splitRGBStackHeadLess(sourceRGB);
		for(int i=0;i<3;i++)sourceTabRGB[i]=VitimageUtils.makeOperationBetweenTwoImages(sourceTabRGB[i], fullMask, 2, true);
		ImagePlus resultRGB=VitimageUtils.compositeRGBDouble(sourceTabRGB[0], sourceTabRGB[1], sourceTabRGB[2], 1,1,1,"resRGB");
		resultRGB.show();
		
		//TODO :		
		//Detect vessel center and vessel axis
		
		ImagePlus resCenter=resultRGB.duplicate();
		resCenter.show();
		for(int z=0;z<Z;z++) {
			double[][]centers=new double[][] {SegmentationUtils.getMassCenter(new Duplicator().run(vesselSeg,1,1,z+1,z+1,1,1)),
											SegmentationUtils.getMassCenter(new Duplicator().run(xylemSeg,1,1,z+1,z+1,1,1)), 
											SegmentationUtils.getMassCenter(new Duplicator().run(phloemSeg,1,1,z+1,z+1,1,1))};
			
			resCenter=SegmentationUtils.drawRectangleInRGBImage(resCenter,(int)centers[0][0]-3,(int)centers[0][1]-1,2,2,z,Color.pink);
			resCenter=SegmentationUtils.drawRectangleInRGBImage(resCenter,(int)centers[1][0]-3,(int)centers[1][1]-1,2,2,z,Color.yellow);
			resCenter=SegmentationUtils.drawRectangleInRGBImage(resCenter,(int)centers[2][0]-3,(int)centers[2][1]-1,2,2,z,Color.green);


		}
		resCenter.show();
		//For each slice :
			//Get center of vessel, of xylems, of phloems
			//Display it on RGB stuff
		
		//		VitimageUtils.massCenter
		//Test on train and test
	//ImagePlus vessTmp=VitimageUtils.connexe2d(vessel, 0, 1E8, oneForTrainTwoForValThreeForTest, oneForTrainTwoForValThreeForTest, oneForTrainTwoForValThreeForTest, oneForTrainTwoForValThreeForTest, debug)
		//	ImagePlus img,double threshLow,double threshHigh,double volumeLowSI,double volumeHighSI,int connexity,int selectByVolume,boolean noVerbose) {
	//TODO :
	   //Use segmentations to process : 
	   //Exclude slices when vessel not central
	   
	   //Vessel=convhull(Contocenter(bin(vessel)))
	   //Xylem=bin(xylem) AND vessel
	   //Phloem=conmax(seg( phlo \ closure (seg(xylem)) ) )
	   //If only one xylem or if no phloem, abort

	   //If only two xylems
	   		//Axis=orth(xyl1-xyl2) . direction center(xyl)->center(phlo)
		   //XylemL=leftmost(xylem,axis)
		   //XylemR=rightmost(xylem,axis)
	   	   //Proto=nothing
	   //Else
		   //Axis=Axis (vessel -> phlo)
		   //ListXyl=fromLeftToRight(xyls)
		   //XylemL=leftmost(xylem,axis)
		   //XylemR=rightmost(xylem,axis)
		   //Proto=If any (centralmost(xylem,axis)
		   //Others=nearerCat(L,C,R)
	   
	   //Verifications : axis coherent with position in stack ?
	   //Verifications : comparable vessel surface ?
	   //Verifications : phloem surface over xylem surface
	   //Verifications : 

    }    
    
    
    /** Third phase, from structure proba maps to output data  ---------------------------------------*/        		   
    public void step_B_05_getVesselSegmentation(){
    	
    }
    
    public void step_B_05_getPhloemSegmentation(){
    	
    }

    public void step_B_06_estimateVesselAxis() {
    	
    }
    
    
    //Train 3 x 6 models (vessel, phlo, xy) X (R,G,B,H,S,B)
    public void step_B_07_separateXylems(){
    	
    }
	
    public void step_B_08_estimateLikelihood(){
    	
    }
	
	
	
    /** First phase, from manual annotations to vessel contour segmentation  ---------------------------------------*/        		

	private static final double ratioStdBrightness=0.2;
	private static final double ratioStdColor=0.15;		
    private static final int targetResolutionVessel=512;
    private static final int resizeFactorVessel=8;
	private static final long serialVersionUID = 1L;
    public static double RATIO_CONTRAST=0.2;
    public static int MIN_VB_512 =14;
    public static int MAX_VB_512 =272+200;
    public static int MIN_VB_1024 =MIN_VB_512*4;
    public static int MAX_VB_1024 =MAX_VB_512*4;
    public static boolean NO_PRUNE=true;
    public static boolean CLEAN_VAL=false;
    public static boolean CLEAN_REF=false;
    public static int NSTEPS=4;
    public static int blabla=0; 
    Timer t;

    
    public void startFirstPhase (){
		   t=new Timer();
		   int startStep=4;
		   int lastStep=4;
		   IJ.log("Starting"); 
		   t.print("Starting step 0");
		   if(startStep<=0 && lastStep>=0)step_00_splitTrainValidTest();
		   t.print("Starting step 1");
	       if(startStep<=1 && lastStep>=1)step_01_augment_train_data();
		   t.print("Starting step 2");
	       if(startStep<=2 && lastStep>=2)step_02_train_model(true);
		   t.print("Starting step 3");
//	      if(startStep<=3 && lastStep>=3)step_03_apply_model("test",true);
//	       if(startStep<=3 && lastStep>=3)step_03_apply_model("validate",true);
	       if(startStep<=3 && lastStep>=3)step_03_apply_model("training",true);
		   t.print("Starting step 4");

		   if(startStep<=4 && lastStep>=4)step_04_measure_scores("validate",true);
		   t.print("Starting step 5");
	       if(startStep<=5 && lastStep>=5)step_05_display_results("test",true);
		   IJ.log("End test.");
		   System.out.println("End test.");
		}        
			
    public static void step_00_splitTrainValidTest() {
            String[]dirsToProcess=new String[] {
                            getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/Full",
                            getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/Train",
                            getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/Val",
                            getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/Test",
                            getVesselsDir()+"/Data/Insights_and_annotations/Interexpert_assessment/Interexpert_Mathieu",
                            getVesselsDir()+"/Data/Insights_and_annotations/Interexpert_assessment/Interexpert_Romain",
            };
            
            for(String s : dirsToProcess){
                    System.out.println("Processing "+s);
                    new File(s+"_subsampled").mkdirs();
                    SegmentationUtils.resampleJsonAndImageSet(s,s+"_subsampled",resizeFactorVessel);
            }
	        ImagePlus []imgs=SegmentationUtils.jsonToBinary(getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/Train_subsampled");
	        IJ.saveAsTiff(imgs[0],getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/Stack_source.tif");
	        IJ.saveAsTiff(imgs[1],getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations.tif");

	        imgs=SegmentationUtils.jsonToBinary(getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/Val_subsampled");
	        IJ.saveAsTiff(imgs[0],getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_validate/Stack_source.tif");
	        IJ.saveAsTiff(imgs[1],getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_validate/Stack_annotations.tif");

	        imgs=SegmentationUtils.jsonToBinary(getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/Test_subsampled");
	        IJ.saveAsTiff(imgs[0],getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_test/Stack_source.tif");
	        IJ.saveAsTiff(imgs[1],getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_test/Stack_annotations.tif");
    
    
    }
    
	public static void step_01_augment_train_data() {
        ImagePlus source=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/Stack_source.tif");
        ImagePlus mask=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations.tif");			

    	for(int i=0;i<6;i++) {
        	System.out.println("Processing augmentation "+i+"/"+NSTEPS);
        	ImagePlus sourceOut=source.duplicate();
        	ImagePlus maskOut=mask.duplicate();

        	sourceOut=SegmentationUtils.brightnessAugmentationStack(sourceOut,false,2,ratioStdBrightness,true);
    		sourceOut=SegmentationUtils.colorAugmentationStack(sourceOut,false,2,ratioStdColor,true);
    		maskOut=SegmentationUtils.brightnessAugmentationStack(maskOut,true,2,ratioStdBrightness,true);
    		maskOut=SegmentationUtils.colorAugmentationStack(maskOut,true,2,ratioStdBrightness,true);

    		ImagePlus[]tab=SegmentationUtils.rotationAugmentationStack(sourceOut,maskOut,0.5,1,i);
    		IJ.saveAsTiff(tab[0], getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/Stack_source"+("_AUGSET"+i)+".tif");
    		IJ.saveAsTiff(tab[1], getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations"+("_AUGSET"+i)+".tif"); 		        
            sourceOut=null;
            maskOut=null;
    		Runtime.getRuntime().gc();
    	}
	}
 		
    public static void step_02_train_model(boolean multiModel) {
        for(int i=0;i<(multiModel ?6:1);i++) {
            Runtime. getRuntime(). gc();
            ImagePlus []imgs=new ImagePlus[] {
        	        IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/Stack_source"+("_AUGSET"+i)+".tif"),
        	        IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations"+("_AUGSET"+i)+".tif"),
            };
            if(i<3)imgs[0]=VitimageUtils.splitRGBStackHeadLess(imgs[0])[(i)];
            else        imgs[0]=VitimageUtils.getHSB(imgs[0])[(i-3)];
            SegmentationUtils.wekaTrainModel(imgs[0],imgs[1],SegmentationUtils.getStandardRandomForestParams(i),SegmentationUtils.getStandardRandomForestFeatures(),getVesselsDir()+"/Data/Processing/Step_01_detection/Models/model_layer_1"+("_AUGSET"+i));
        }	
    }
   		
    public static void step_03_apply_model(String dataType,boolean multiModel) {
    	if(  (!dataType.equals("training") ) && (!dataType.equals("validate") ) &&(!dataType.equals("test") ) ) {
    		System.out.println("Wrong data set type :"+dataType);
    		System.exit(0);
    	}
    	ImagePlus[] resultTab=new ImagePlus[(multiModel ? 6 : 1 )];
    	for(int i=0;i<(multiModel ? 6 : 1 );i++) {
        	ImagePlus img=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_source.tif");
        	if(i<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(i)];
            else        img=VitimageUtils.getHSB(img)[(i-3)];
            IJ.log("Apply model aug "+i);
    		resultTab[i]=SegmentationUtils.wekaApplyModel(img,SegmentationUtils.getStandardRandomForestParams(i),SegmentationUtils.getStandardRandomForestFeatures(),getVesselsDir()+"/Data/Processing/Step_01_detection/Models/model_layer_1"+("_AUGSET"+i+""));
    		resultTab[i]=new Duplicator().run(resultTab[i],2,2,1,resultTab[i].getNSlices(),1,1);
            IJ.saveAsTiff(resultTab[i],getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_"+"STEP"+i+".tif");
            
    	}
		ImagePlus result=VitimageUtils.meanOfImageArray(resultTab);
        IJ.saveAsTiff(result,getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(multiModel ? "_multimodel" : "_monomodel")+".tif");
    }
              
	public static void step_04_measure_scores(String dataType,boolean multiModel) {
		boolean verbose=false;
    	if(  (!dataType.equals("training") ) && (!dataType.equals("validate") ) &&(!dataType.equals("test") ) ) {
    		System.out.println("Wrong data set type :"+dataType);
    		System.exit(0);
    	}
    	System.out.println(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba.tif");
        ImagePlus binaryRefT=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
		 ImagePlus []res=new ImagePlus[100];
		 int incr=0;
		 for(double d1=0.5;d1<=0.5;d1+=0.1){
            for(double d2=0.7;d2<=0.7;d2+=0.05){
        	System.out.println("\n "+d1+"  -  "+d2);
            ImagePlus binValT=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(multiModel ? "_multimodel" : "_STEP0")+".tif");
        	ImagePlus binaryValT=SegmentationUtils.getSegmentationFromProbaMap3D(binValT,d1,d2);
        	//IJ.saveAsTiff(result,getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(multiModel ? "_multimodel" : "_monomodel")+".tif");

        	SegmentationUtils.scoreComparisonSegmentations(binaryRefT,binaryValT,false);
			ImagePlus img=VitimageUtils.compositeNoAdjustOf(binaryRefT,binaryValT);
			img.setTitle(d1+" , "+d2);
			res[incr++]=img;
            }
        }
        for(int i=0;i<incr;i++) {
        	res[i].show();
        }
	}

	public static void step_05_display_results(String dataType,boolean multiModel) {
    	if(  (!dataType.equals("training") ) && (!dataType.equals("validate") ) &&(!dataType.equals("test") ) ) {
    		System.out.println("Wrong data set type :"+dataType);
    		System.exit(0);
    	}
        ImagePlus binaryRefT=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
		//ImagePlus binaryValT=SegmentationUtils.cleanVesselSegmentation(binaryValT,targetResolutionVessel,MIN_VB_512,MAX_VB_512);
        ImagePlus sourceValT=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_source.tif");
      //  SegmentationUtils.visualizeMaskEffectOnSourceData(sourceValT,binaryValT,3).show();
       // SegmentationUtils.visualizeMaskDifferenceOnSourceData(sourceValT,binaryRefT,binaryValT).show();
	}

   
	
	
   public static String getHighResTifDir() {
    	if(new File("/home/rfernandez").exists()) {
    		return "/media/rfernandez/DATA_RO_A/Sorgho_Slices_BFF/Img_lvl_1";
    	}
    	return null;
    }

	
   public static String getVesselsDir() {
	    	if(new File("/home/rfernandez").exists()) {
	    		return "/home/rfernandez/Bureau/A_Test/Vaisseaux";
	    	}
	    	else if(new File("/users/bionanonmri").exists()) {
	    		return "/users/bionanonmri/fernandez/DISTCOMP/VESSELS";
	    	}
	    	else if(new File("/home/fernandr").exists()) {
	    		return "/home/fernandr/Bureau/A_Test/Vaisseaux";
	    	}
	    	else if(new File("/linkhome/rech/gencir01/uol13er").exists() ) {
	    		return "/gpfswork/rech/qlb/uol13er/VESSELS";
	    	}
	    	else return "BUG !";
	    }
    
    public void test() {

    }

	public VesselSegmentation() {
		super();
		}
 
		/*       public static void step_03_apply_model_layer2(String dataType,boolean multiModel) {
    	if(  (!dataType.equals("training") ) && (!dataType.equals("validate") ) &&(!dataType.equals("test") ) ) {
    		System.out.println("Wrong data set type :"+dataType);
    		System.exit(0);
    	}
    	ImagePlus img=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(useRotAugValidate ? "_old" : "")+".tif");
		ImagePlus resultTab=wekaApplyModelSlicePerSlice(img,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_training/layer_2_512_pixSCENAUG3");
		resultTab=new Duplicator().run(resultTab,2,2,1,resultTab.getNSlices(),1,1);
        IJ.saveAsTiff(resultTab,getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_layer2"+(useRotAugValidate ? "_old" : "")+".tif");        	
    }
*/            
    public static void testMono(String dataType) {
   	   ImagePlus[]img=new ImagePlus[6];
   	   for(int i=0;i<6;i++) {
   		   img[i]=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_"+"STEP"+i+".tif");
   	   }
   	   ImagePlus in=VitimageUtils.compositeRGBByte(img[1], img[2], img[5], 1,1, 1);
   	  ImagePlus mask=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
        int[]classifierParams=	SegmentationUtils.getStandardRandomForestParams(0);
         boolean[]enableFeatures=	SegmentationUtils.getShortRandomForestFeatures();
   	   int numFeatures=classifierParams[1];
   	   int numTrees=classifierParams[0];
  	   int seed=classifierParams[2];
  	   int minSigma=classifierParams[3];
  	   int maxSigma=classifierParams[4];
  	   Runtime. getRuntime(). gc();
          long startTime = System.currentTimeMillis();
          WekaSegmentation seg = new WekaSegmentation(in);
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

          seg.addRandomBalancedBinaryData(in, mask, "class 2", "class 1", 10000);
          seg.saveFeatureStack(1, "/home/rfernandez/Bureau/test.tif","test.tif");
          System.out.println("Here 1");
          seg.trainClassifier();
          System.out.println("Here 2");
          seg.applyClassifier( in, 0, true).show();
          System.out.println("Here 4");
          // Add labeled samples in a balanced and random way
    //  	seg.saveFeatureStack(1, "/home/rfernandez/Bureau/tempdata/fsa",new File(modelName).getName());
          
  		Runtime.getRuntime().gc();

      }

   public static void testMono2(String dataType) {
  	   ImagePlus[]img=new ImagePlus[6];
  	   for(int i=0;i<6;i++) {
  		   img[i]=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_"+"STEP"+i+".tif");
  	   }
  	  ImagePlus mask=IJ.openImage(getVesselsDir()+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
       int[]classifierParams=	SegmentationUtils.getStandardRandomForestParams(0);
        boolean[]features=	SegmentationUtils.getShortRandomForestFeatures();
  	   int numFeatures=classifierParams[1];
  	   int numTrees=classifierParams[0];
 	   int seed=classifierParams[2];
 	   int minSigma=classifierParams[3];
 	   int maxSigma=classifierParams[4];
 	   Runtime. getRuntime(). gc();
         long startTime = System.currentTimeMillis();
         HyperWeka wekaSave=new HyperWeka();
         HyperWekaSegmentation seg = new HyperWekaSegmentation(img[0],wekaSave);
         seg.setMinimumSigma(1);
         seg.setMaximumSigma(8);
         seg.setFeatureStackArray(HyperWeka.buildFeatureStackArrayRGBSeparatedMultiThreadedV2(img,features,minSigma,maxSigma));
         seg.saveFeatureStack(1, "/home/rfernandez/Bureau/test.tif");
         // Classifier
         FastRandomForest rf = new FastRandomForest();
         rf.setNumTrees(numTrees);                  
         rf.setNumFeatures(3);  
         rf.setSeed( seed );    
         seg.setClassifier(rf);    
         // Parameters  
         seg.setMembranePatchSize(11);  
         seg.addRandomBalancedBinaryData(img[0], mask, "class 2", "class 1", 1000);
         seg.wekasave.tabHyperFeatures[0]=false;
         System.out.println("Here 1");
         seg.trainClassifier();
         System.out.println("Here 2");
		seg.setUpdateFeatures(false);
		seg.applyClassifier(false);//False means no probability maps
		seg.getClassifiedImage().show();
         System.out.println("Here 4");
         // Add labeled samples in a balanced and random way
   //  	seg.saveFeatureStack(1, "/home/rfernandez/Bureau/tempdata/fsa",new File(modelName).getName());
         
 		Runtime.getRuntime().gc();

     }

}