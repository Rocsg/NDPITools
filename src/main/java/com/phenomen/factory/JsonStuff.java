package com.phenomen.factory;

import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.json.*;

import com.phenomen.common.TransformUtils;
import com.phenomen.common.VitiDialogs;
//import javax.json.stream;
import com.phenomen.common.VitimageUtils;
import com.phenomen.registration.ItkTransform;

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
import com.phenomen.factory.JsonRoiSegmentationConverter;

public class JsonStuff extends PlugInFrame{
		
	 
		private static final long serialVersionUID = 1L;
		public static String vesselsDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";
		public static int NUM_TREES=300;
        public static int NUM_FEATS=14;//sqrt (#feat)
        public static int SEED=7;//lucky number
        public static int MIN_SIGMA=2;
        public static int MAX_SIGMA=32;
        public static int N_EXAMPLES=2000000;//1M
        public static double RATIO_CONTRAST=0.2;
        public static int MIN_VB_512 =14;
        public static int MAX_VB_512 =272+200;
        public static int MIN_VB_1024 =MIN_VB_512*4;
        public static int MAX_VB_1024 =MAX_VB_512*4;
        public static boolean NO_PRUNE=true;
        public static boolean CLEAN_VAL=false;
        public static boolean CLEAN_REF=false;
        public static int NSTEPS=4;
        public static boolean[]getFeatures(){
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
                false,  /* Median */
                false,  /* Anisotropic_diffusion */
                false,  /* Bilateral */
                false,  /* Lipschitz */
                false,  /* Kuwahara */
                true,  /* Gabor */
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
           int NSTEPS=4;
           int scenarioAug=3;//1=old scheme 2=rot+intGrey 3=rot+intGrey+intRGB
           Runtime. getRuntime(). gc();
           /**Data augmentation*/
	    	  // step_01_augment_train_data(512,false,scenarioAug);
             //	   step_01_augment_train_data(512,true,scenarioAug);
//	    	   step_01_augment_train_data(1024,true,scenarioAug);
//	    	   step_01_augment_train_data(1024,false,scenarioAug);

    	   
    	   /** Training and validation*/
    	   boolean makeTestData=false;
    	   int targetResolution=512;
    	   boolean useRotAug=false;
    	   boolean useContAug=false;
    	   boolean useValAug=false;
    	   int layer=1;
    	   IJ.log("Starting training New!");
    	   //step_02_train_model(512,false,false,scenarioAug,scenarioTrain);
          // step_03_apply_model_on_validation_data(512,false,false,false,false,scenarioAug,scenarioTrain);
          // step_03_apply_model_on_validation_data(512,false,false,false,true,scenarioAug,scenarioTrain);
           //step_03_apply_model_on_validation_data(512,false,true,false, makeTestData,scenarioAug,scenarioTrain);
    	   //test();
    	   step_04_measure_scores_on_validation_data(targetResolution,useRotAug,useContAug,useValAug, makeTestData,layer,scenarioAug,scenarioTrain);
    	   step_05_display_results_validation_data(targetResolution,useRotAug,useContAug,useValAug, makeTestData,layer,scenarioAug,scenarioTrain);
   		   //System.out.println("End.");
   }        

		
		public JsonStuff() {
			super("");
			}
		
		public void run(String arg) {
	    	   IJ.log("Starting training !");
			this.start();
		}	

		public static void main(String[]args) {
            ImageJ ij=new ImageJ();
            WindowManager.closeAllWindows();
			new JsonStuff().start();
		}
		


        /** Steps to run  --------------------------------------------------------------------------------------------*/        		
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
                        JsonRoiSegmentationConverter.resampleJsonAndImageSet(s,s+"_subsampled_"+targetInsightSize+"_pix",resampleFactor);
                }
    	        ImagePlus []imgs=JsonRoiSegmentationConverter.jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Train_subsampled_"+targetInsightSize+"_pix");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetInsightSize+"_pix.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetInsightSize+"_pix.tif");

    	        imgs=JsonRoiSegmentationConverter.jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Val_subsampled_"+targetInsightSize+"_pix");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_validate/Stack_source_"+targetInsightSize+"_pix.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_validate/Stack_annotations_"+targetInsightSize+"_pix.tif");

    	        imgs=JsonRoiSegmentationConverter.jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Test_subsampled_"+targetInsightSize+"_pix");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_test/Stack_source_"+targetInsightSize+"_pix.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_test/Stack_annotations_"+targetInsightSize+"_pix.tif");
        
        
        }
        
		public static void step_01_augment_train_data(int targetSize,boolean doMaskOrSource,int scenarioAug) {
	        ImagePlus source=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix.tif");
	        ImagePlus mask=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix.tif");			


	        if(scenarioAug==2) {
	        	System.out.println("Processing augmentation "+" at target size "+targetSize);
	        	ImagePlus sourceOut=source.duplicate();
	        	ImagePlus maskOut=mask.duplicate();
        		//if(!doMaskOrSource)sourceOut=rotationAugmentationStack(sourceOut);
        		if(!doMaskOrSource)sourceOut=brightnessAugmentationStack(sourceOut,doMaskOrSource,2,0.3,true);
        		if(!doMaskOrSource)IJ.saveAsTiff(sourceOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif");

        		//if(doMaskOrSource)maskOut=rotationAugmentationStack(maskOut);
        		if(doMaskOrSource)maskOut=brightnessAugmentationStack(maskOut,doMaskOrSource,2,0.3,true);
		        if(doMaskOrSource)IJ.saveAsTiff(maskOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif"); 		        
	        }	        	

	        if(scenarioAug==3) {
	        	System.out.println("Processing augmentation "+" at target size "+targetSize);
	        	ImagePlus sourceOut=source.duplicate();
	        	ImagePlus maskOut=mask.duplicate();
        		//if(!doMaskOrSource)sourceOut=rotationAugmentationStack(sourceOut);
        		if(!doMaskOrSource)sourceOut=brightnessAugmentationStack(sourceOut,doMaskOrSource,2,0.3,true);
        		if(!doMaskOrSource)sourceOut=colorAugmentationStack(sourceOut,doMaskOrSource,2,0.3,true);
        		if(!doMaskOrSource)IJ.saveAsTiff(sourceOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif");

        		//if(doMaskOrSource)maskOut=rotationAugmentationStack(maskOut);
        		if(doMaskOrSource)maskOut=brightnessAugmentationStack(maskOut,doMaskOrSource,2,0.3,true);
        		if(doMaskOrSource)maskOut=colorAugmentationStack(maskOut,doMaskOrSource,2,0.3,true);
		        if(doMaskOrSource)IJ.saveAsTiff(maskOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+".tif"); 		        
	        }
	        if(scenarioAug==4) {
	        	System.out.println("Processing augmentation "+" at target size "+targetSize);
	        	for(int i=0;i<NSTEPS;i++) {
		        	ImagePlus sourceOut=source.duplicate();
		        	ImagePlus maskOut=mask.duplicate();
	        		//if(!doMaskOrSource)sourceOut=rotationAugmentationStack(sourceOut);
	        		if(!doMaskOrSource)sourceOut=brightnessAugmentationStack(sourceOut,doMaskOrSource,2,0.3,i==0);
	        		if(!doMaskOrSource)sourceOut=colorAugmentationStack(sourceOut,doMaskOrSource,2,0.3,i==0);
	        		if((i%2)==1)IJ.run(sourceOut, "Rotate 90 Degrees Right", "");
	        		if(!doMaskOrSource)IJ.saveAsTiff(sourceOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+("STEP"+i)+".tif");
	        		
	        		//if(doMaskOrSource)maskOut=rotationAugmentationStack(maskOut);
	        		if(doMaskOrSource)maskOut=brightnessAugmentationStack(maskOut,doMaskOrSource,2,0.3,i==0);
	        		if(doMaskOrSource)maskOut=colorAugmentationStack(maskOut,doMaskOrSource,2,0.3,i==0);
	        		if((i%2)==1)IJ.run(maskOut, "Rotate 90 Degrees Right", "");
	        		if(doMaskOrSource)IJ.saveAsTiff(maskOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetSize+"_pix"+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+("STEP"+i)+".tif"); 		        
	        	}
        	}
		}
        		
        public static void step_02_train_model(int targetResolution,boolean useRotAug,boolean useContAug,int scenarioAug,int scenarioTrain) {
            for(int i=0;i<NSTEPS;i++) {
	            Runtime. getRuntime(). gc();
	            ImagePlus []imgs=new ImagePlus[] {
	        	        IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+("STEP"+i)+".tif"),
	        	        IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+("STEP"+i)+".tif"),
	            };
	            System.out.println(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+("STEP"+i)+".tif");
	            System.out.println(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+("STEP"+i)+".tif");
	            VitimageUtils.printImageResume(imgs[0]);
	            VitimageUtils.printImageResume(imgs[1]);
	            ImagePlus firstProbaMaps=wekaTrainModel(imgs[0],imgs[1],NUM_TREES,NUM_FEATS,SEED*(i+1),MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_1_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+("STEP"+i));
	            ImagePlus temp=new Duplicator().run(firstProbaMaps,2,2,1,firstProbaMaps.getNSlices(),1,1);
	            temp.show();
	            IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Result_proba_layer_1_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+("STEP"+i)+".tif");
	
	            /*ImagePlus secondProbaMaps=wekaTest(temp,imgs[1],NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_2_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
	            temp=new Duplicator().run(secondProbaMaps,2,2,1,secondProbaMaps.getNSlices(),1,1);
	            temp.show();
	            IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Result_proba_layer_2_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");                
	*/
            }	
        }
       		
        public static void step_03_apply_model_on_validation_data(int targetResolution,boolean useRotAugTrain,boolean useContAugTrain,boolean useRotAugValidate,boolean makeTest,int scenarioAug,int scenarioTrain) {
        	ImagePlus img=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Stack_source_"+targetResolution+"_pix.tif");
        	ImagePlus result=null;
        	if(! useRotAugValidate) {
	        	ImagePlus firstProbaMaps=wekaApplyModel(img,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_1_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
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
        			ImagePlus firstProbaMaps=wekaApplyModel(temp,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_1_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
        			temp.close();
        			temp=new Duplicator().run(firstProbaMaps,2,2,1,firstProbaMaps.getNSlices(),1,1);
		            IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_1_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(useRotAugValidate ? "_AUGVAL_part"+i+"" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
		
		            ImagePlus secondProbaMaps=wekaApplyModel(temp,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_2_"+targetResolution+"_pix"+extension(useRotAugTrain, useContAugTrain)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
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
            VitimageUtils.printImageResume(binaryValT);
			binaryValT=VitimageUtils.getBinaryMask(binaryValT, 0.5);
			if(CLEAN_VAL)binaryValT=JsonRoiSegmentationConverter.cleanVesselSegmentation(binaryValT,targetResolution, MIN_VB_512, MAX_VB_512);
			if(CLEAN_REF)binaryRefT=JsonRoiSegmentationConverter.cleanVesselSegmentation(binaryRefT,targetResolution, MIN_VB_512, MAX_VB_512);
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
	            double iouGlob=JsonRoiSegmentationConverter.IOU(binaryRef,binaryVal);
	            Roi[]rRef=JsonRoiSegmentationConverter.segmentationToRoi(binaryRef);
	            Roi[]rVal=JsonRoiSegmentationConverter.segmentationToRoi(binaryVal);
				nTotRef+=rRef.length;
				nTotVal+=rVal.length;
				Object[][]tab=JsonRoiSegmentationConverter.roiPairingHungarianMethod(rRef,rVal);
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
			JsonRoiSegmentationConverter.getSizeMap(binaryRefT,0,20,5,true);
		}

		public static void step_05_display_results_validation_data(int targetResolution,boolean useRotAug,boolean useContAug,boolean useRotAugVal,boolean makeTest,int layer,int scenarioAug,int scenarioTrain) {
			System.out.println(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_"+layer+"_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(useRotAugVal ? "_AUGVAL" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
            ImagePlus binaryValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Result_proba_layer_"+layer+"_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(useRotAugVal ? "_AUGVAL" : "")+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");
            ImagePlus binaryRefT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Stack_annotations_"+targetResolution+"_pix.tif");
			binaryValT=JsonRoiSegmentationConverter.cleanVesselSegmentation(binaryValT,targetResolution,MIN_VB_512,MAX_VB_512);
            ImagePlus sourceValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+(makeTest ? "test" : "validate")+"/Stack_source_"+targetResolution+"_pix.tif");
            JsonRoiSegmentationConverter.visualizeMaskEffectOnSourceData(sourceValT,binaryValT,3).show();
            JsonRoiSegmentationConverter.visualizeMaskDifferenceOnSourceData(sourceValT,binaryRefT,binaryValT).show();
		}

       

        /** Weka train and apply model --------------------------------------------------------------------------------------------*/        
       public static ImagePlus wekaTrainModel(ImagePlus img,ImagePlus mask,int numTrees,int numFeatures,int seed,int minSigma,int maxSigma,String modelName) {
           VitimageUtils.printImageResume(img); 
           VitimageUtils.printImageResume(mask); 
    	   Runtime. getRuntime(). gc();
            long startTime = System.currentTimeMillis();
            WekaSegmentation seg = new WekaSegmentation(img);
    
            // Classifier
            FastRandomForest rf = new FastRandomForest();
            rf.setNumTrees(300);                  
            rf.setNumFeatures(14);  
            rf.setSeed( seed );    
            seg.setClassifier(rf);    
            // Parameters  
            seg.setMembranePatchSize(11);  
            seg.setMinimumSigma(minSigma);
            seg.setMaximumSigma(maxSigma);
      
            // Selected attributes (image features)
            boolean[]enableFeatures = getFeatures();
        
            // Enable features in the segmentator
            seg.setEnabledFeatures( enableFeatures );

            // Add labeled samples in a balanced and random way

            int[]nbPix=JsonRoiSegmentationConverter.nbPixelsInClasses(mask);
            int min=Math.min(nbPix[0],nbPix[255]);
            int targetExamplesPerSlice=N_EXAMPLES/(2*img.getNSlices());
            System.out.println("Starting training on "+targetExamplesPerSlice+" examples per slice");
            seg.addRandomBalancedBinaryData(img, mask, "class 2", "class 1", targetExamplesPerSlice);
            Runtime. getRuntime(). gc();
            
            // Train classifier
            seg.trainClassifier();
            seg.saveClassifier(modelName+".model");
            
            // Apply trained classifier to test image and get probabilities
            ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
            probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
            // Print elapsed time
            long estimatedTime = System.currentTimeMillis() - startTime;
            IJ.log( "** Finished script in " + estimatedTime + " ms **" );
            Runtime. getRuntime(). gc();
            return probabilityMaps;
        }
                
        public static ImagePlus wekaApplyModel(ImagePlus img,int numTrees,int numFeatures,int seed,int minSigma,int maxSigma,String modelName) {
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

        
               
        /** Routines for data augmentation --------------------------------------------------------------------------------------------*/        
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
 		
		public static ImagePlus colorAugmentationStack(ImagePlus imgIn,boolean isMask,int nMult,double std,boolean keepOriginal){
			int N=imgIn.getNSlices();
			double mean=1;
			int frequency=5;
			ImagePlus []tabOut=new ImagePlus[nMult];
			ImagePlus []tabSlicesOut=new ImagePlus[N*nMult];
			int i=0;
			for(int iMult=0;iMult<nMult;iMult++) {
				System.out.println("Processing brightness "+iMult);
				tabOut[iMult]=imgIn.duplicate();
				if(isMask ||  (iMult==0 && keepOriginal)) {
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
         
              
       
        /** Helpers to set in Vitimage --------------------------------------------------------------------------------------------*/              
		public static String extension(boolean augRot,boolean augCont) {
			return (""+(augRot ? "_AUGROT" : "")+(augCont ? "_AUGCONT" : ""));
		}


}