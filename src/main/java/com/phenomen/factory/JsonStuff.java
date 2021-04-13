package com.phenomen.factory;

import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.json.*;

import com.vitimage.common.TransformUtils;
import com.vitimage.common.VitiDialogs;
//import javax.json.stream;
import com.vitimage.common.VitimageUtils;
import com.vitimage.registration.ItkTransform;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.OpenDialog;
import ij.io.RoiDecoder;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import trainableSegmentation.*;
import hr.irb.fastRandomForest.FastRandomForest;


public class JsonStuff extends PlugInFrame{
	public static ImagePlus[]channelSplitter(ImagePlus imgRGB){
		ImagePlus[]tabRet;
		ImagePlus imgTemp=imgRGB.duplicate();
		String tit=imgTemp.getTitle();
		imgTemp.setTitle("temp");
		imgTemp.show();
//		VitimageUtils.waitFor(100);
		IJ.selectWindow(imgTemp.getTitle());
		IJ.run("Split Channels");
		if(imgTemp!=null)imgTemp.close();
//		VitimageUtils.waitFor(100);
		ImagePlus imgR=ij.WindowManager.getImage("temp (red)");
		ImagePlus imgG=ij.WindowManager.getImage("temp (green)");
		ImagePlus imgB=ij.WindowManager.getImage("temp (blue)");
		imgR.setTitle(tit+"_red");
		imgG.setTitle(tit+"_green");
		imgB.setTitle(tit+"_blue");
		tabRet=new ImagePlus[] {imgR.duplicate(),imgG.duplicate(),imgB.duplicate()};
		imgR.close();
		imgG.close();
		imgB.close();
		return tabRet;
	}


	 
		private static final long serialVersionUID = 1L;
		public static String vesselsDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";
		public static int NUM_TREES=500;
        public static int NUM_FEATS=14;//sqrt (#feat)
        public static int SEED=7;//lucky number
        public static int MIN_SIGMA=2;
        public static int MAX_SIGMA=64;
        public static int N_EXAMPLES=2000000;//1M
        public static double RATIO_CONTRAST=0.2;
        public static int MIN_VB_512 =14;
        public static int MAX_VB_512 =272+200;
        public static int MIN_VB_1024 =MIN_VB_512*4;
        public static int MAX_VB_1024 =MAX_VB_512*4;
        public static boolean NO_PRUNE=true;
        public static boolean CLEAN_VAL=false;
        public static boolean CLEAN_REF=false;
        public static boolean[]getFeatures(){
             	return new boolean[]{
                true,   /* Gaussian_blur */
                true,   /* Sobel_filter */
                true,   /* Hessian */
                true,   /* Difference_of_gaussians */
                false,   /* Membrane_projections */
                false,  /* Variance */
                true,  /* Mean */
                true,  /* Minimum */
                true,  /* Maximum */
                false,  /* Median */
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
      //Benchmark a realiser :
        //Interet augmentation contraste
        //Interet augmentation rotation
        //Interet 512 / 1024
        //Methode de selection 
        //Nombres d exemples utilises
        
        public static ImagePlus getAugmentationMap(ImagePlus imgIn,double mean,double std,int frequency) {
        	int Z=imgIn.getNSlices();
        	int X=imgIn.getWidth();
        	int Y=imgIn.getHeight();
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
        
        public void test() {
        	VitimageUtils.showWithParams(getAugmentationMap(IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Processing/Step_01_detection/Weka_test/Stack_annotations_512_pix.tif"),1.0,0.3,6),"",1,0.5,1.5);
        }
        
	       public void start (){
	    	   IJ.log("Starting training !");
	    	  /**Timing @16 cores
	    	   * 512 : 2s per FT, 1200s per Mexemples  , 2.4 s de classification
	    	   */
	    	   /**Data preparation*/
	    	   //step_00_prepareData(512,8);
	    	   //step_00_prepareData(1024,4);
	           int scenarioTrain=0;//5 5 contre 4 2
	           int scenarioAug=3;//1=old scheme 2=rot+intGrey 3=rot+intGrey+intRGB
               Runtime. getRuntime(). gc();
               /**Data augmentation*/
	    	  // step_01_augment_train_data(512,false,scenarioAug);
	    	   //step_01_augment_train_data(512,true,scenarioAug);
//	    	   step_01_augment_train_data(1024,true,scenarioAug);
//	    	   step_01_augment_train_data(1024,false,scenarioAug);

	    	   
	    	   /** Training and validation*/
	    	   boolean makeTestData=false;
	    	   int targetResolution=512;
	    	   boolean useRotAug=false;
	    	   boolean useContAug=false;
	    	   boolean useValAug=false;
	    	   int layer=1;
	    	   IJ.log("Starting training !");
	    	  // step_02_train_model(512,false,false,scenarioAug,scenarioTrain);
              // step_03_apply_model_on_validation_data(512,false,false,false,false,scenarioAug,scenarioTrain);
              // step_03_apply_model_on_validation_data(512,false,false,false,true,scenarioAug,scenarioTrain);
               //step_03_apply_model_on_validation_data(512,false,true,false, makeTestData,scenarioAug,scenarioTrain);
	    	   //test();
	    	   step_04_measure_scores_on_validation_data(targetResolution,useRotAug,useContAug,useValAug, makeTestData,layer,scenarioAug,scenarioTrain);
	    	   step_04_display_results_validation_data(targetResolution,useRotAug,useContAug,useValAug, makeTestData,layer,scenarioAug,scenarioTrain);
	   		   //System.out.println("End.");
       }        

		
		public JsonStuff() {		super("");	}
		
		public void run(String arg) {
	    	   IJ.log("Starting training !");
			this.start();
		}	

		public static void main(String[]args) {
            ImageJ ij=new ImageJ();
            WindowManager.closeAllWindows();
			new JsonStuff().start();
		}
		

		
		public static Roi[]pruneRoi(Roi[]roiTab,int targetResolution){
			if(NO_PRUNE)return roiTab;
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

			
		public static ImagePlus visualizeMaskEffectOnSourceData(ImagePlus imgSourceRGB,ImagePlus mask,int mode0VBOnly_1Enhance_2GreysOther_3greenout) {
			ImagePlus[]imgSource=channelSplitter(imgSourceRGB);

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
				ImagePlus[]imgSourceGreys=channelSplitter(imgSourceRGBGrey);
				ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
				ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(imgMask);
				imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
				imgMaskGreys=VitimageUtils.makeOperationOnOneImage(imgMaskGreys, 2, 1.7, true);
				for(int can=0;can<3;can++) {
					imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
					imgSourceGreys[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceGreys[can], imgMaskGreys, 2, false);
					ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can],imgSourceGreys[can],1,false);
				}
//				ImagePlus deb1=VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);
//				ImagePlus deb2=VitimageUtils.compositeRGBByte(imgSourceGreys[0], imgSourceGreys[1], imgSourceGreys[2],1,1,1);
				return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
			}
			else if(mode0VBOnly_1Enhance_2GreysOther_3greenout==3) {
				ImagePlus[]ret=new ImagePlus[3];
				ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
				IJ.run(imgSourceRGBGrey,"8-bit","");
				IJ.run(imgSourceRGBGrey,"RGB Color","");
				ImagePlus[]imgSourceGreys=channelSplitter(imgSourceRGBGrey);
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
//				ImagePlus deb1=VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);
//				ImagePlus deb2=VitimageUtils.compositeRGBByte(imgSourceGreys[0], imgSourceGreys[1], imgSourceGreys[2],1,1,1);
				return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
			}
			else  {
				ImagePlus imgSourceRGB2=imgSourceRGB.duplicate();
				ImagePlus[]imgSource2=channelSplitter(imgSourceRGB2);
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

		public static ImagePlus visualizeMaskDifferenceOnSourceData(ImagePlus imgSourceRGB,ImagePlus maskRef,ImagePlus maskVal) {
			ImagePlus[]imgSourceRef=channelSplitter(imgSourceRGB);
			ImagePlus[]imgSourceVal=channelSplitter(imgSourceRGB);
			ImagePlus[]imgSourceBoth=channelSplitter(imgSourceRGB);
			ImagePlus[]ret=new ImagePlus[3];
			ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
			IJ.run(imgSourceRGBGrey,"8-bit","");
			IJ.run(imgSourceRGBGrey,"RGB Color","");
			ImagePlus[]imgSourceGreys=channelSplitter(imgSourceRGBGrey);

			ImagePlus mRefAndVal=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 2);
			ImagePlus imgMaskRefAndVal=getBinaryMaskUnary(mRefAndVal, 0.5);
			
			ImagePlus mRefOnly=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 4);
			ImagePlus imgMaskRefOnly=getBinaryMaskUnary(mRefOnly, 0.5);

			ImagePlus mValOnly=VitimageUtils.binaryOperationBetweenTwoImages(maskVal, maskRef, 4);
			ImagePlus imgMaskValOnly=getBinaryMaskUnary(mValOnly, 0.5);

			ImagePlus mRefOrVal=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 1);			
			ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(mRefOrVal);
			imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
			
			
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


		
        public static void step_00_prepareData(int targetInsightSize,int resampleFactor) {
                String[]dirsToProcess=new String[] {
                                vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Full",
                                vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Train",
                                vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Val",
                                vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Test",
                                vesselsDir+"/Data/Insights_and_annotations/Interexpert_assessment/Interexpert_Mathieu",
                                vesselsDir+"/Data/Insights_and_annotations/Interexpert_assessment/Interexpert_Romain",
                };
                
                for(String s : dirsToProcess){
                        System.out.println("Processing "+s);
                        new File(s+"_subsampled_"+targetInsightSize+"_pix").mkdirs();
                        resampleJsonAndImageSet(s,s+"_subsampled_"+targetInsightSize+"_pix",resampleFactor);
                }
    	        ImagePlus []imgs=jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Train_subsampled_"+targetInsightSize+"_pix");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetInsightSize+"_pix.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetInsightSize+"_pix.tif");

    	        imgs=jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Val_subsampled_"+targetInsightSize+"_pix");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_validate/Stack_source_"+targetInsightSize+"_pix.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_validate/Stack_annotations_"+targetInsightSize+"_pix.tif");

    	        imgs=jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Test_subsampled_"+targetInsightSize+"_pix");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_test/Stack_source_"+targetInsightSize+"_pix.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_test/Stack_annotations_"+targetInsightSize+"_pix.tif");
        
        
        }
        
		public static void step_01_augment_train_data(int targetSize,boolean doMaskOrSource,int scenarioAug) {
	        ImagePlus source=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix.tif");
	        ImagePlus mask=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix.tif");			


	        if(scenarioAug==1) {
		        for(boolean augRot : new boolean[] {false,true}) {
			        for(boolean augCont : new boolean[] {false,true}) {
			        	System.out.println("Processing augmentation "+extension(augRot,augCont)+" at target size "+targetSize);
			        	ImagePlus sourceOut=source.duplicate();
			        	ImagePlus maskOut=mask.duplicate();
			        	if(augCont) {
			        		if(!doMaskOrSource)sourceOut=contrastAugmentationStack(sourceOut);
			        		if(doMaskOrSource)maskOut=contrastAugmentationStack(maskOut);
			        	}
			        	if(augRot) {
			        		if(!doMaskOrSource)sourceOut=rotationAugmentationStack(sourceOut);
			        		if(doMaskOrSource)maskOut=rotationAugmentationStack(maskOut);
			        	}
			        	if(!doMaskOrSource)IJ.saveAsTiff(sourceOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix"+extension(augRot,augCont)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif");
				        if(doMaskOrSource)IJ.saveAsTiff(maskOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix"+extension(augRot,augCont)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif"); 	
			        }
		        }
	        }
	        if(scenarioAug==2) {
	        	System.out.println("Processing augmentation "+" at target size "+targetSize);
	        	ImagePlus sourceOut=source.duplicate();
	        	ImagePlus maskOut=mask.duplicate();
        		//if(!doMaskOrSource)sourceOut=rotationAugmentationStack(sourceOut);
        		if(!doMaskOrSource)sourceOut=brightnessAugmentationStack(sourceOut,doMaskOrSource,2,0.3);
        		if(!doMaskOrSource)IJ.saveAsTiff(sourceOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif");

        		//if(doMaskOrSource)maskOut=rotationAugmentationStack(maskOut);
        		if(doMaskOrSource)maskOut=brightnessAugmentationStack(maskOut,doMaskOrSource,2,0.3);
		        if(doMaskOrSource)IJ.saveAsTiff(maskOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif"); 		        
	        }	        	

	        if(scenarioAug==3) {
	        	System.out.println("Processing augmentation "+" at target size "+targetSize);
	        	ImagePlus sourceOut=source.duplicate();
	        	ImagePlus maskOut=mask.duplicate();
        		//if(!doMaskOrSource)sourceOut=rotationAugmentationStack(sourceOut);
        		if(!doMaskOrSource)sourceOut=brightnessAugmentationStack(sourceOut,doMaskOrSource,2,0.3);
        		if(!doMaskOrSource)sourceOut=colorAugmentationStack(sourceOut,doMaskOrSource,2,0.3);
        		if(!doMaskOrSource)IJ.saveAsTiff(sourceOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif");

        		//if(doMaskOrSource)maskOut=rotationAugmentationStack(maskOut);
        		if(doMaskOrSource)maskOut=brightnessAugmentationStack(maskOut,doMaskOrSource,2,0.3);
        		if(doMaskOrSource)maskOut=colorAugmentationStack(maskOut,doMaskOrSource,2,0.3);
		        if(doMaskOrSource)IJ.saveAsTiff(maskOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif"); 		        
	        }
	        if(scenarioAug==4) {
	        	System.out.println("Processing augmentation "+" at target size "+targetSize);
	        	ImagePlus sourceOut=source.duplicate();
	        	ImagePlus maskOut=mask.duplicate();
        		//if(!doMaskOrSource)sourceOut=rotationAugmentationStack(sourceOut);
        		if(!doMaskOrSource)sourceOut=brightnessAugmentationStack(sourceOut,doMaskOrSource,2,0.3);
        		if(!doMaskOrSource)sourceOut=colorAugmentationStack(sourceOut,doMaskOrSource,2,0.3);
        		if(!doMaskOrSource)IJ.saveAsTiff(sourceOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif");

        		//if(doMaskOrSource)maskOut=rotationAugmentationStack(maskOut);
        		if(doMaskOrSource)maskOut=brightnessAugmentationStack(maskOut,doMaskOrSource,2,0.3);
        		if(doMaskOrSource)maskOut=colorAugmentationStack(maskOut,doMaskOrSource,2,0.3);
		        if(doMaskOrSource)IJ.saveAsTiff(maskOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif"); 		        
	        }
		}
        		
        public static void step_02_train_model(int targetResolution,boolean useRotAug,boolean useContAug,int scenarioAug,int scenarioTrain) {
            Runtime. getRuntime(). gc();
            ImagePlus []imgs=new ImagePlus[] {
        	        IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif"),
        	        IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif"),
            };
            ImagePlus firstProbaMaps=wekaTest(imgs[0],imgs[1],NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_1_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
            ImagePlus temp=new Duplicator().run(firstProbaMaps,2,2,1,firstProbaMaps.getNSlices(),1,1);
            temp.show();
            IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Result_proba_layer_1_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");

            /*ImagePlus secondProbaMaps=wekaTest(temp,imgs[1],NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_2_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
            temp=new Duplicator().run(secondProbaMaps,2,2,1,secondProbaMaps.getNSlices(),1,1);
            temp.show();
            IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Result_proba_layer_2_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");                
*/
        }
       		
        public static void step_03_apply_model_on_validation_data(int targetResolution,boolean useRotAugTrain,boolean useContAugTrain,boolean useRotAugValidate,boolean makeTest,int scenarioAug,int scenarioTrain) {
        	ImagePlus img=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Stack_source_"+targetResolution+"_pix.tif");
        	ImagePlus result=null;
        	if(! useRotAugValidate) {
	        	ImagePlus firstProbaMaps=wekaValidate(img,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_1_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
	            ImagePlus temp=new Duplicator().run(firstProbaMaps,2,2,1,firstProbaMaps.getNSlices(),1,1);
	            temp.show();
	            IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_1_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(useRotAugValidate ? "_AUGVAL" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
	
	            /*ImagePlus secondProbaMaps=wekaValidate(temp,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_2_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
	            temp=new Duplicator().run(secondProbaMaps,2,2,1,firstProbaMaps.getNSlices(),1,1);
	            temp.show();
	            result=temp.duplicate();
	            temp.close();*/
        	}
        	else {
        		ImagePlus[]tempTab=new ImagePlus[2];
        		for(int i=0;i<2;i++) {
        			ImagePlus temp=img.duplicate();
        			temp.show();
            		for(int j=0;j<i;j++) IJ.run("Rotate 90 Degrees Right");
        			ImagePlus firstProbaMaps=wekaValidate(temp,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_1_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
        			temp.close();
        			temp=new Duplicator().run(firstProbaMaps,2,2,1,firstProbaMaps.getNSlices(),1,1);
		            IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_1_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(useRotAugValidate ? "_AUGVAL_part"+i+"" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
		
		            ImagePlus secondProbaMaps=wekaValidate(temp,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_2_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
		            temp=new Duplicator().run(temp,2,2,1,firstProbaMaps.getNSlices(),1,1);
		            temp.show();
            		for(int j=0;j<i;j++) IJ.run("Rotate 90 Degrees Left");
            		tempTab[i]=temp.duplicate();
            		temp.close();
        		}
        		result=VitimageUtils.meanOfImageArray(tempTab);
        	}
            //IJ.saveAsTiff(result,vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_2_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(useRotAugValidate ? "_AUGVAL" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
        }
        
		public static void step_04_measure_scores_on_validation_data(int targetResolution,boolean useRotAug,boolean useContAug,boolean useRotAugVal,boolean makeTest,int layer,int scenarioAug,int scenarioTrain) {
			boolean verbose=false;
			System.out.println(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_"+layer+"_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(useRotAugVal ? "_AUGVAL" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
            ImagePlus binaryValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_"+layer+"_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(useRotAugVal ? "_AUGVAL" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
            ImagePlus binaryRefT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Stack_annotations_"+targetResolution+"_pix.tif");
//			binaryRefT=VitimageUtils.getBinaryMask(binaryRefT, 0.5);
            VitimageUtils.printImageResume(binaryValT);
			binaryValT=VitimageUtils.getBinaryMask(binaryValT, 0.5);
			if(CLEAN_VAL)binaryValT=cleanVesselSegmentation(binaryValT,targetResolution);
			if(CLEAN_REF)binaryRefT=cleanVesselSegmentation(binaryRefT,targetResolution);
			int nLost=0;
			int nFound=0;
			double accIOU=0;
			int nInMore=0;
			int nTotRef=0;
			int nTotVal=0;
			int Z=binaryValT.getNSlices();
			int[]nbFoundPerClass=new int[20];
			int[]nbTotPerClass=new int[20];
			for(int z=0;z<Z;z++) {
				ImagePlus binaryRef=new Duplicator().run(binaryRefT,1,1,z+1,z+1,1,1);
				ImagePlus binaryVal=new Duplicator().run(binaryValT,1,1,z+1,z+1,1,1);
	            double iouGlob=IOU(binaryRef,binaryVal);
				Roi[]rRef=pruneRoi(segmentationToRoi(binaryRef),targetResolution);
				Roi[]rVal=pruneRoi(segmentationToRoi(binaryVal),targetResolution);
				nTotRef+=rRef.length;
				nTotVal+=rVal.length;
				Object[][]tab=roiPairingHungarianMethod(rRef,rVal);
				for(int i=0;i<tab.length;i++) {					
					int index=(int)Math.floor(((Double)tab[i][2]/20.0));
					if(index>19)index=19;
					nbTotPerClass[index]++;
					if((Integer)tab[i][0]<=0) {nLost++;continue;}
					nbFoundPerClass[index]++;
					nFound++;
					accIOU+=(Double)(tab[i][1]);
				}
				
				nInMore=nTotVal-nFound;
				if(verbose) {
					System.out.println("\nAfter z="+z);
					System.out.println("nLost="+nLost);
					System.out.println("nFound="+nFound);
					System.out.println("nInMore="+nInMore);
	
					System.out.println("nThisRef="+rRef.length);
					System.out.println("nThisVal="+rVal.length);
					System.out.println("nTotRef="+nTotRef);
					System.out.println("nTotVal="+nTotVal);
					System.out.println("accIOU="+(accIOU/nFound));
				}
			}
			accIOU/=nFound;
			double percent=VitimageUtils.dou(nFound*100.0/nTotRef);
			double percentOut=VitimageUtils.dou(nInMore*100.0/nTotVal);
			System.out.println("Summary : #VB found="+nFound+" ("+percent+" %) with mean IOU="+accIOU+" . #VB not found="+nLost+" . #False positives="+nInMore+" (="+percentOut+"%)");
			for(int i=0;i<4;i++) {
				System.out.println();
				for(int j=0;j<5;j++) {
					int indBase=i*5+j;
					String percentInd=""+VitimageUtils.dou(nbFoundPerClass[indBase]*100.0/nbTotPerClass[indBase]);
					if(nbTotPerClass[indBase]==0)percentInd="N/A";
					System.out.print("Class["+(indBase*20)+" - "+((indBase+1)*20)+"]:"+percentInd+" , ");
				}
			}
			getSizeMap(binaryRefT,0,20,5,true);
		}

		public static void step_04_display_results_validation_data(int targetResolution,boolean useRotAug,boolean useContAug,boolean useRotAugVal,boolean makeTest,int layer,int scenarioAug,int scenarioTrain) {
			System.out.println(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_"+layer+"_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(useRotAugVal ? "_AUGVAL" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
            ImagePlus binaryValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_"+layer+"_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(useRotAugVal ? "_AUGVAL" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
            ImagePlus binaryRefT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Stack_annotations_"+targetResolution+"_pix.tif");
			binaryValT=cleanVesselSegmentation(binaryValT,targetResolution);
            ImagePlus sourceValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Stack_source_"+targetResolution+"_pix.tif");
			visualizeMaskEffectOnSourceData(sourceValT,binaryValT,3).show();
			visualizeMaskDifferenceOnSourceData(sourceValT,binaryRefT,binaryValT).show();
		}

       
       
		public static ImagePlus cleanVesselSegmentation(ImagePlus seg,int targetResolution) {
			double voxVol=VitimageUtils.getVoxelVolume(seg);
			double minVBsurface=voxVol*(targetResolution==512 ? MIN_VB_512 : MIN_VB_1024);
			double maxVBsurface=voxVol*(targetResolution==512 ? MAX_VB_512 : MAX_VB_1024);

			//Remplir les trous inutiles
			ImagePlus imgSeg=VitimageUtils.getBinaryMask(seg, 0.5);
			imgSeg.show();
			IJ.run("Fill Holes", "stack");
			ImagePlus test1=imgSeg.duplicate();
			imgSeg.hide();
			//Retirer les cellules plus petites que et plus grandes que
			imgSeg=VitimageUtils.connexe2d(imgSeg, 1,256, minVBsurface, maxVBsurface , 6,0,true);
			ImagePlus test2=imgSeg.duplicate();
			return VitimageUtils.getBinaryMask(imgSeg,0.5);
		}
       
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
    

        
		public static ImagePlus getSizeMap(ImagePlus img,int threshLow,int step,int nCat,boolean show) {

			ImagePlus imgRet=VitimageUtils.nullImage(img);
			IJ.run(imgRet,"8-bit","");
			imgRet.setDisplayRange(0, nCat*step);
			IJ.run(imgRet,"Fire","");
			for(int z=0;z<imgRet.getNSlices();z++) {
				ImagePlus temp=new Duplicator().run(img,1,1,z+1,z+1,1,1);
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
			VitimageUtils.showWithParams(imgRet,img.getTitle()+"_size_map",1,0,nCat*step);
/*			imgRet.show();
			imgRet.setDisplayRange(0, nCat*step);
			imgRet.updateAndDraw();
			IJ.run(imgRet,"Fire","");
			IJ.run("Brightness/Contrast...");
			selectWindow("B&C");
			*/
			if(!show)imgRet.hide();
			return imgRet;
		}
               
        public static Roi[]segmentationToRoi(ImagePlus seg){
        	ImagePlus imgSeg=VitimageUtils.getBinaryMask(seg, 0.5);
        	imgSeg.show();
//            VitimageUtils.waitFor(100);
            IJ.run("Create Selection");
            //VitimageUtils.printImageResume(IJ.getImage(),"getImage");
            Roi r=IJ.getImage().getRoi();
            //System.out.println(r);
            Roi[] rois = ((ShapeRoi)r).getRois();
            IJ.getImage().close();
            return rois;
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
        
        public static Object [][] roiPairingHungarianMethod(Roi[]roiTabRef,Roi[]roiTabTest){
            double[][]costMatrix=new double[roiTabRef.length][roiTabTest.length];
            for(int i=0;i<roiTabRef.length;i++) {
                for(int j=0;j<roiTabRef.length;j++) {
                	costMatrix[i][j]=1-IOU(roiTabRef[i],roiTabTest[j]);
                }
            }
            
    		HungarianAlgorithm hung=new HungarianAlgorithm(costMatrix);
    		com.vitimage.common.Timer t=new com.vitimage.common.Timer();
    		t.print("Starting hungarian over "+roiTabRef.length+" x "+roiTabTest.length);
    		int []solutions=hung.execute();
    		t.print("Finishing hungarian over "+roiTabRef.length+" x "+roiTabTest.length);

    		Object[][]ret=new Object[roiTabRef.length][];
            for(int i=0;i<roiTabRef.length;i++) {
            	double surface=getRoiSurface(roiTabRef[i]);
            	if(solutions[i]==-1)ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
            	else if(costMatrix[i][solutions[i]]>=1)ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
            	else ret[i]=new Object[] {solutions[i],IOU(roiTabRef[i],roiTabTest[solutions[i]]),new Double(surface)};
            }    		
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
//                        		System.out.println("Nouveau max : "+val);
                                max=val;
                                ret[i]=new Object[] {new Integer(j),new Double(val),new Double(surface)};
                        }
                    }
                }
                return ret;
        }
        
        

        public static ImagePlus wekaTest(ImagePlus img,ImagePlus mask,int numTrees,int numFeatures,int seed,int minSigma,int maxSigma,String modelName) {
            Runtime. getRuntime(). gc();
                long startTime = System.currentTimeMillis();
            System.out.print("weka step 1   ");
                WekaSegmentation seg = new WekaSegmentation(img);
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
          
                // Selected attributes (image features)
                boolean[]enableFeatures = getFeatures();
            
                // Enable features in the segmentator
                seg.setEnabledFeatures( enableFeatures );
            System.out.print("weka step 6  ");

                // Add labeled samples in a balanced and random way

            int[]nbPix=nbPixelsInClasses(mask);
            int min=Math.min(nbPix[0],nbPix[255]);
            int targetExamplesPerSlice=N_EXAMPLES/(2*img.getNSlices());
            System.out.println("Starting training on "+targetExamplesPerSlice+" examples per slice");
            seg.addRandomBalancedBinaryData(img, mask, "class 2", "class 1", targetExamplesPerSlice);
            Runtime. getRuntime(). gc();
            
            System.out.print("weka step 7  ");
                // Train classifier
                seg.trainClassifier();
            System.out.print("weka step 8  ");
            seg.saveClassifier(modelName+".model");
            
                // Apply trained classifier to test image and get probabilities
                ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
            System.out.print("weka step 9  ");
                probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
                // Print elapsed time
                long estimatedTime = System.currentTimeMillis() - startTime;
                IJ.log( "** Finished script in " + estimatedTime + " ms **" );
                Runtime. getRuntime(). gc();
                return probabilityMaps;
        }

        
        
        
        public static ImagePlus wekaValidate(ImagePlus img,int numTrees,int numFeatures,int seed,int minSigma,int maxSigma,String modelName) {
            long startTime = System.currentTimeMillis();
            System.out.print("weka step 1   ");
            WekaSegmentation seg = new WekaSegmentation(img);
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
      
            // Selected attributes (image features)
            boolean[]enableFeatures = getFeatures();
        
            // Enable features in the segmentator
            seg.setEnabledFeatures( enableFeatures );
            System.out.print("weka step 6  ");

            // Add labeled samples in a balanced and random way
            seg.updateWholeImageData();
            System.out.print("weka step 65  ");
            seg.loadClassifier(modelName+".model");
        
            System.out.print("weka step 7  ");
            // Train classifier

            // Apply trained classifier to test image and get probabilities
            ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
            System.out.print("weka step 9  ");
            probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
            // Print elapsed time
            long estimatedTime = System.currentTimeMillis() - startTime;
            IJ.log( "** Finished script in " + estimatedTime + " ms **" );
            Runtime. getRuntime(). gc();
            return probabilityMaps;
    }

        
        
        
        
        
		public static ImagePlus rotationAugmentationStack(ImagePlus imgIn){
			int N=imgIn.getNSlices();
			ImagePlus []tabSlicesOut=new ImagePlus[2*N];

			for(int i=0;i<2;i++) {
				System.out.println("Processing rotation "+i);
				ImagePlus temp=imgIn.duplicate();
				temp.show();
				for(int j=0;j<i;j++)IJ.run("Rotate 90 Degrees Right");
				ImagePlus[]tab2=VitimageUtils.stackToSlices(temp);
				temp.close();
				for(int j=0;j<N;j++) {
					System.out.println("Copy tab "+j+" to tabOut "+(i*N+j));
					tabSlicesOut[(i)*N+j]=tab2[j].duplicate();
				}
			}
			return VitimageUtils.slicesToStack(tabSlicesOut);
		}

		public static ImagePlus contrastAugmentationStack(ImagePlus imgIn){
			int N=imgIn.getNSlices();
			ImagePlus []tabOut=new ImagePlus[27];
			ImagePlus []tabSlicesOut=new ImagePlus[N*27];
			int i=0;
			for(int iR=0;iR<3;iR++) {
				for(int iG=0;iG<3;iG++) {
					for(int iB=0;iB<3;iB++) {
						int index=iR+iG*3+iB*3*3;
						System.out.print((++i)+" /27");
						tabOut[index]=imgIn.duplicate();
						if(imgIn.getType()==ImagePlus.COLOR_RGB) {
							tabOut[index].setTitle("temp");
							tabOut[index].show();
							VitimageUtils.waitFor(200);
							IJ.selectWindow("temp");
							IJ.run("Split Channels");
							tabOut[index].close();
							ImagePlus imgR=ij.WindowManager.getImage("temp (red)");
							ImagePlus imgG=ij.WindowManager.getImage("temp (green)");
							ImagePlus imgB=ij.WindowManager.getImage("temp (blue)");
	
							IJ.selectWindow("temp (red)");
							IJ.run("Multiply...", "value="+(1-RATIO_CONTRAST+RATIO_CONTRAST*iR)+" stack");
							IJ.selectWindow("temp (green)");
							IJ.run("Multiply...", "value="+(1-RATIO_CONTRAST+RATIO_CONTRAST*iG)+" stack");
							IJ.selectWindow("temp (blue)");
							IJ.run("Multiply...", "value="+(1-RATIO_CONTRAST+RATIO_CONTRAST*iB)+" stack");
	
							IJ.run("Merge Channels...", "c1=[temp (red)] c2=[temp (green)] c3=[temp (blue)] create");
							IJ.run("Stack to RGB", "slices keep");
							tabOut[index]=IJ.getImage();
						}
						ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[index]);
						for(int j=0;j<N;j++) {
							System.out.println("Copy tab "+j+" to tabOut "+(index*N+j));
							tabSlicesOut[(index)*N+j]=tab2[j].duplicate();
						}
						if(imgIn.getType()==ImagePlus.COLOR_RGB) {
							ij.WindowManager.getImage("Composite").close();
							ij.WindowManager.getImage("Composite-1").close();
						}
						tabOut[index].close();
						Runtime. getRuntime(). gc();
					}
				}
			}
			return VitimageUtils.slicesToStack(tabSlicesOut);
		}

 		
		public static ImagePlus colorAugmentationStack(ImagePlus imgIn,boolean isMask,int nMult,double std){
			int N=imgIn.getNSlices();
			double mean=1;
			int frequency=5;
			ImagePlus []tabOut=new ImagePlus[nMult];
			ImagePlus []tabSlicesOut=new ImagePlus[N*nMult];
			int i=0;
			for(int iMult=0;iMult<nMult;iMult++) {
				System.out.println("Processing brightness "+iMult);
				tabOut[iMult]=imgIn.duplicate();
				if(isMask || iMult==0) {
					for(int j=0;j<N;j++) {
						ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
						System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
						tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
					}
					continue;
				}
				ImagePlus augmentationMapR=getAugmentationMap(imgIn,mean,std,frequency);
				ImagePlus augmentationMapG=getAugmentationMap(imgIn,mean,std,frequency);
				ImagePlus augmentationMapB=getAugmentationMap(imgIn,mean,std,frequency);
				tabOut[iMult].setTitle("temp");
				tabOut[iMult].show();
				IJ.selectWindow("temp");
				IJ.run("Split Channels");
				tabOut[iMult].close();
				ImagePlus imgR=ij.WindowManager.getImage("temp (red)");
				ImagePlus imgG=ij.WindowManager.getImage("temp (green)");
				ImagePlus imgB=ij.WindowManager.getImage("temp (blue)");
				
				ImagePlus imgR2=VitimageUtils.makeOperationBetweenTwoImages(imgR, augmentationMapR, 2, false);
				ImagePlus imgG2=VitimageUtils.makeOperationBetweenTwoImages(imgG, augmentationMapG, 2, false);
				ImagePlus imgB2=VitimageUtils.makeOperationBetweenTwoImages(imgB, augmentationMapB, 2, false);
				imgR.close();imgG.close();imgB.close();imgR2.show();imgG2.show();imgB2.show();imgR2.setTitle("temp (red)");imgG2.setTitle("temp (green)");imgB2.setTitle("temp (blue)");

				IJ.run("Merge Channels...", "c1=[temp (red)] c2=[temp (green)] c3=[temp (blue)] create");
				IJ.run("Stack to RGB", "slices keep");
				tabOut[iMult]=IJ.getImage();
				ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
				for(int j=0;j<N;j++) {
					System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
					tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
				}
				WindowManager.getImage("Composite").close();
				WindowManager.getImage("Composite-1").close();
			}
			Runtime. getRuntime(). gc();
			System.out.println("Assembling tab of "+tabSlicesOut.length);
			return VitimageUtils.slicesToStack(tabSlicesOut);
		}
	
		
		public static ImagePlus brightnessAugmentationStack(ImagePlus imgIn,boolean isMask,int nMult,double std){
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
				if(isMask || iMult==0) {
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

        

        public static ImagePlus binaryProbaMapsToRGB(ImagePlus img) {
                ImagePlus canalR=new Duplicator().run(img,1,1,1,img.getNSlices(),1,1);
                ImagePlus canalG=new Duplicator().run(img,1,1,1,img.getNSlices(),1,1);
                ImagePlus canalB=new Duplicator().run(img,2,2,1,img.getNSlices(),1,1);

                return VitimageUtils.compositeRGBByte(canalR, canalG, canalB, 1, 1, 1);
        }
       

        public static int[]nbPixelsInClasses(ImagePlus img){
                int[]tab=img.getStack().getProcessor(1).getHistogram();
                return tab;
        }
        
        
        public static void roiPruning(boolean removeRoiOnTheBorder,double minCompacity,double minSurfaceInVoxels,double maxSurfaceInVoxels) {

        }
        
        public static int[]stringTabToDoubleTab(String[]tab){
                int[]ret=new int[tab.length];
                for(int i=0;i<tab.length;i++)ret[i]=Integer.parseInt(tab[i]);
                return ret;
        }
        
        public static Roi roiParser(String xcoords,String ycoords) {
                int[]tabX=stringTabToDoubleTab(xcoords.split(","));
                int[]tabY=stringTabToDoubleTab(ycoords.split(","));
                return new PolygonRoi(tabX, tabY, tabX.length,Roi.POLYGON);
        }

                
        
        
        public static ImagePlus []jsonToBinary(String dir) {
                System.out.println(dir);
                
                String[]listFiles=new File(dir).list();
                System.out.println(listFiles.length);
                int nImgs=listFiles.length-1;
                String s="";
                for(String s2 : listFiles) {
                        if(s2.contains(".json"))s=s2;
                }
                
                String[][][] tabData=convertJsonToRoi(new File(dir,s).getAbsolutePath());
                ImagePlus[]imgInit=new ImagePlus[nImgs];
                ImagePlus[]imgMask=new ImagePlus[nImgs];
                
                for(int indImg=0;indImg<tabData.length;indImg++) {
                        System.out.println("Opening image "+new File(dir,tabData[indImg][0][0]).getAbsolutePath());
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
        
        
        
        
        public static String[][][]convertJsonToRoi(String jsonPath){
                String bag="val";
                String conf="2048";
                int fact=2;
                String pathIn=jsonPath;
                JSONObject jo = new JSONObject(VitimageUtils.readStringFromFile(pathIn));
                Iterator <String>iter=jo.keys();
                String[][][] ret=new String[jo.length()][3][];
                int indexImg=-1;
                while(iter.hasNext()) {
                        String key=(String)iter.next();
                        System.out.println("Image name : "+key);
                        JSONObject jo2=jo.getJSONObject(key);
                        Iterator <String>iter2=jo2.keys();
                        
                        //Iterate over images
                        while(iter2.hasNext()) {
                                String key2=(String)iter2.next();
                                if(key2.equals("regions")){
                                        JSONArray jo3=jo2.getJSONArray(key2);
                                        ret[++indexImg]=new String[jo3.length()][3];

                                        
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
                                                                System.out.println();
                                                                while(iter5.hasNext()) {
                                                                        String key5=(String)iter5.next();
                                                                        if(key5.equals("all_points_x")){
                                                                                JSONArray tabX=(JSONArray) jo5.get(key5);
                                                                                String data=tabX.join(",");
                                                                                System.out.println("PtX="+data);
                                                                                ret[indexImg][indexReg][1]=(data);
                                                                        }
                                                                        if(key5.equals("all_points_y")){
                                                                                JSONArray tabY=(JSONArray) jo5.get(key5);
                                                                                String data=tabY.join(",");
                                                                                System.out.println("PtY="+data);
                                                                                ret[indexImg][indexReg][2]=(data);
                                                                        }
                                                                }        
                                                        }
                                                }
                                        }                                        
                                }
                        }                        
                }
                return ret;
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

    	
		public static String extension(boolean augRot,boolean augCont) {
			return (""+(augRot ? "_AUGROT" : "")+(augCont ? "_AUGCONT" : ""));
		}


}