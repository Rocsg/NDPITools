package fr.cirad.image.sorghobff;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;

import fr.cirad.image.common.Timer;
import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.hyperweka.HyperWeka;
import fr.cirad.image.hyperweka.HyperWekaSegmentation;
import fr.cirad.image.mlutils.SegmentationUtils;
import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;

public class VesselSegmentation extends PlugInFrame{
		
 	/**
 	 * This class is more a script : it executes successive operations to :
 	 * 1) prepare data from json and source images to ML-ready data into valid and train sets
 	 * 
 	 */
	    
	    
	
	
	public void run(String arg) {
 	   IJ.log("Starting training !");
 	  //testPouet();
 	   this.startSecondPhase();
	}	

	public static void main(String[]args) {
        ImageJ ij=new ImageJ();
        WindowManager.closeAllWindows();
		new VesselSegmentation().run("");
	}
	
	
    /** Second phase, from contour segmentation to structure proba maps  ---------------------------------------*/        		
    public void startSecondPhase (){
		   t=new Timer();
		   int startStep=5;
		   int lastStep=5;
		   boolean bothMathieuAndRomain=true;
		   IJ.log("Starting"); 
		   t.print("Starting step 0");
		   if(startStep<=0 && lastStep>=0)step_B_00_collectJsonAndPrepareSourceAndTargetLabels(bothMathieuAndRomain);
		   t.print("Starting step 1");
		   if(startStep<=1 && lastStep>=1)step_B_01_splitTrainValTest(bothMathieuAndRomain);
		   t.print("Starting step 2");
		   if(startStep<=2 && lastStep>=2)step_B_02_augmentTrainingData();
		   t.print("Starting step 3");
		   if(startStep<=3 && lastStep>=3)step_B_03_trainModels();
		   t.print("Starting step 4");
		   if(startStep<=4 && lastStep>=4)step_B_04_applyModels(2);
		   t.print("Starting step 5");
		   if(startStep<=5 && lastStep>=5)step_B_05_extract_structures(2);

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

	//Test segmentation models
    public void step_B_04_applyModels(int oneForTrainTwoForValThreeForTest){
    	String sorghoDir= getVesselsDir();
    	String rep=oneForTrainTwoForValThreeForTest==1 ? "Train" : oneForTrainTwoForValThreeForTest==2 ? "Val" : "Test";
    	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Split_train_val_test/"+rep+"/";
    	String dirModel=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Models/";
    	String []targets=new String[] {"Phloem","Vessel_convex_hull","Xylem"};//{"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"};
    	String []channels=new String[] {"Red","Green","Blue","Hue","Saturation","Brightness"};
    	int tot=30;
    	int incr=0;

    	//Split channels
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
    			resultTab[i]=new Duplicator().run(resultTab[i],2,2,1,resultTab[i].getNSlices(),1,1);
    			IJ.run(resultTab[i],"Fire","");
    			IJ.saveAsTiff(resultTab[i], dirIn+target+"_seg_"+channels[i]+".tif");
    		}
    		ImagePlus result=VitimageUtils.meanOfImageArray(resultTab);
    		IJ.saveAsTiff(result, dirIn+target+"_seg_altogether.tif");    		
    	}
    }

    public void step_B_05_extract_structures(int oneForTrainTwoForValThreeForTest) {
	   	String sorghoDir= getVesselsDir();
	   	String rep=oneForTrainTwoForValThreeForTest==1 ? "Train" : oneForTrainTwoForValThreeForTest==2 ? "Val" : "Test";
	   	String dirIn=sorghoDir+"/Data/Insights_and_annotations/Vessels_dataset/Split_train_val_test/"+rep+"/";
	   	String []targets=new String[] {"Phloem","Vessel_convex_hull","Xylem"};//{"Meta","Phloem","Proto","Vessel_convex_hull","Xylem"};
		ImagePlus sourceRGB=IJ.openImage(dirIn+"Source.tif");    		
		ImagePlus source=IJ.openImage(dirIn+"Source.tif");    		
		IJ.run(source,"8-bit","");
		IJ.run(source,"32-bit","");
		source=VitimageUtils.makeOperationOnOneImage(source, 2, 1.0/255, true);
		source.setDisplayRange(0, 1);
		ImagePlus xylem=IJ.openImage(dirIn+targets[2]+"_seg_altogether.tif");    		
		ImagePlus phloem=IJ.openImage(dirIn+targets[0]+"_seg_altogether.tif");    		
		ImagePlus vessel=IJ.openImage(dirIn+targets[1]+"_seg_altogether.tif");    		
		IJ.run(xylem, "Gaussian Blur...", "sigma=2");
		IJ.run(phloem, "Gaussian Blur...", "sigma=2");
		ImagePlus[]tmp=SegmentationUtils.getMaxOfProba(xylem, phloem);
		xylem=VitimageUtils.makeOperationBetweenTwoImages(xylem, tmp[0], 2, true);
		phloem=VitimageUtils.makeOperationBetweenTwoImages(phloem, tmp[1], 2, true);
		IJ.run(vessel,"Grays","");
	
	   boolean debug=false;
		if(debug) {
			int z0=7;
			int zF=10;
					
			sourceRGB=new Duplicator().run(sourceRGB,1,1,z0,zF,1,1);
			source=new Duplicator().run(source,1,1,z0,zF,1,1);
			vessel=new Duplicator().run(vessel,1,1,z0,zF,1,1);
			xylem=new Duplicator().run(xylem,1,1,z0,zF,1,1);
			phloem=new Duplicator().run(phloem,1,1,z0,zF,1,1);
		}

		//Step 1 : vessel segmentation

		//Get areas connected to a local maxima, and their influence zone upper to 0.5 proba
		ImagePlus vessTmp01=SegmentationUtils.getSegmentationFromProbaMap2D(vessel,0.5,2);
		ImagePlus debug01=VitimageUtils.makeOperationBetweenTwoImages(vessTmp01, source,2, true);debug01.setTitle("1");
//		debug01.show();IJ.run(debug01,"Fire","");VitimageUtils.waitFor(5000);
		
		//Get most central component convex hull
		ImagePlus vessTmp02=SegmentationUtils.selectCentralMostRoi(vessTmp01);
		ImagePlus debug02=VitimageUtils.makeOperationBetweenTwoImages(vessTmp02, source,2, true);debug02.setTitle("2");
//		debug02.show();IJ.run(debug02,"Fire","");VitimageUtils.waitFor(5000);

		//Fill holes, and connect areas
		int nAct=4;
		for(int i=0;i<nAct;i++)IJ.run(vessTmp02, "Dilate", "stack");
		vessTmp02=VitimageUtils.invertBinaryMask(vessTmp02);
		IJ.run(vessTmp02, "Fill Holes", "stack");
		for(int i=0;i<nAct;i++)IJ.run(vessTmp02, "Dilate", "stack");
		vessTmp02=VitimageUtils.invertBinaryMask(vessTmp02);
		ImagePlus debug03=VitimageUtils.makeOperationBetweenTwoImages(vessTmp02, source,2, true);debug03.setTitle("3");
//		debug03.show();IJ.run(debug03,"Fire","");VitimageUtils.waitFor(5000);
	
		//Get convex hull
		ImagePlus vessTmp04=SegmentationUtils.getConvexHull(0, vessTmp02, 0,false);vessTmp04.show();
		ImagePlus debug04=VitimageUtils.makeOperationBetweenTwoImages(vessTmp04, source,2, true);debug04.setTitle("Vessel segmentation");
		ImagePlus vesselSeg=vessTmp04.duplicate();
//		debug04.show();IJ.run(debug04,"Fire","");VitimageUtils.waitFor(5000);
	

		//Step 2 : xylem segmentation
		ImagePlus xylemTmp01=SegmentationUtils.getSegmentationFromProbaMap2D(xylem, 0.5, 1);
		xylemTmp01.show();	
		ImagePlus xylemTmp02=VitimageUtils.binaryOperationBetweenTwoImages(xylemTmp01, vesselSeg, 2);
		ImagePlus xylemSeg=xylemTmp02.duplicate();
		ImagePlus debug05=VitimageUtils.makeOperationBetweenTwoImages(xylemTmp02, source,2, true);debug05.setTitle("Xylem segmentation");
		//debug05.show();	IJ.run(debug05,"Fire","");VitimageUtils.waitFor(5000);
	
	
		//Step 3 : phloem segmentation. Exclude Xylem+1dil
		ImagePlus xylExclude=xylemSeg.duplicate();
		nAct=5;
		for(int i=0;i<nAct;i++)IJ.run(xylExclude, "Erode", "stack");
		xylExclude=VitimageUtils.invertBinaryMask(xylExclude);
		xylExclude=VitimageUtils.getBinaryMaskUnary(xylExclude, 0.5);
		ImagePlus phloemTmp01=VitimageUtils.makeOperationBetweenTwoImages(phloem, vesselSeg, 2, true);
		phloemTmp01=VitimageUtils.makeOperationBetweenTwoImages(phloemTmp01, xylExclude, 2, true);
		phloemTmp01=VitimageUtils.makeOperationOnOneImage(phloemTmp01, 2, 1.0/255, true);
		phloemTmp01.show();phloemTmp01.setTitle("phlo5");IJ.run(phloemTmp01,"Fire","");//VitimageUtils.waitFor(5000);		
		ImagePlus phloemTmp02=SegmentationUtils.getSegmentationFromProbaMap2D(phloemTmp01,0.5,0);
		ImagePlus debug06=VitimageUtils.makeOperationBetweenTwoImages(phloemTmp02, source, 2, true);
		debug06.setTitle("debug06");debug06.show();IJ.run(debug06,"Fire","");//VitimageUtils.waitFor(2000);

		//Erode, take main, then dilate
		nAct=2;
		VitimageUtils.printImageResume(phloemTmp02);
		for(int i=0;i<nAct;i++)IJ.run(phloemTmp02, "Dilate", "stack");
		ImagePlus phloemTmp03=VitimageUtils.connexe2d(phloemTmp02, 1, 1E8, 0, 1E8, 6, 1, false);
		IJ.run(phloemTmp03,"8-bit","");
		phloemTmp03=VitimageUtils.invertBinaryMask(phloemTmp03);
		for(int i=0;i<nAct;i++)IJ.run(phloemTmp03, "Dilate", "stack");
		phloemTmp03=VitimageUtils.invertBinaryMask(phloemTmp03);
		ImagePlus phloemSeg=phloemTmp03.duplicate();
		ImagePlus debug07=VitimageUtils.makeOperationBetweenTwoImages(phloemTmp03, source, 2, true);
		debug07.show();IJ.run(debug07,"Fire","");//VitimageUtils.waitFor(2000);
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
		vesUnary=VitimageUtils.makeOperationOnOneImage(vesUnary, 2, 0.4, true);
		xylUnary=VitimageUtils.makeOperationOnOneImage(xylUnary, 2, 1.2, true);
		phloUnary=VitimageUtils.makeOperationOnOneImage(phloUnary, 2, 0.8, true);
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
		int Z=xylem.getNSlices();
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

   
	

	
   public static String getVesselsDir() {
	    	if(new File("/home/rfernandez").exists()) {
	    		return "/home/rfernandez/Bureau/A_Test/Vaisseaux";
	    	}
	    	else {
	    		return "D:/jesaispasquoi";
	    	}
	    }
    
    public void test() {

    }

	public VesselSegmentation() {
		super("");
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