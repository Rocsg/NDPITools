package com.phenomen.sorghobff;

import java.io.File;
import com.phenomen.common.VitimageUtils;
import com.phenomen.mlutils.SegmentationUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.Duplicator;
import ij.plugin.frame.PlugInFrame;

public class VesselSegmentation extends PlugInFrame{
		
	 	/**
	 	 * This class is more a script : it executes successive operations to :
	 	 * 1) prepare data from json and source images to ML-ready data into valid and train sets
	 	 * 
	 	 */
        public void start (){
    	   IJ.log("Starting training !"); 
           step_00_splitTrainValidTest();
	       step_01_augment_train_data();
    	   step_02_train_model(true);
           step_03_apply_model("test",true);
    	   step_04_measure_scores("test",true);
    	   step_05_display_results("test",true);
   		   IJ.log("End.");
        }        

		
		
		public void run(String arg) {
	    	   IJ.log("Starting training !");
			this.start();
		}	

		public static void main(String[]args) {
            ImageJ ij=new ImageJ();
            WindowManager.closeAllWindows();
			new VesselSegmentation().start();
		}
		
        /** Steps to run  --------------------------------------------------------------------------------------------*/        		
        public static void step_00_splitTrainValidTest() {
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
                        new File(s+"_subsampled").mkdirs();
                        SegmentationUtils.resampleJsonAndImageSet(s,s+"_subsampled",resizeFactorVessel);
                }
    	        ImagePlus []imgs=SegmentationUtils.jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Train_subsampled");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations.tif");

    	        imgs=SegmentationUtils.jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Val_subsampled");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_validate/Stack_source.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_validate/Stack_annotations.tif");

    	        imgs=SegmentationUtils.jsonToBinary(vesselsDir+"/Data/Insights_and_annotations/Full_dataset/Test_subsampled");
    	        IJ.saveAsTiff(imgs[0],vesselsDir+"/Data/Processing/Step_01_detection/Weka_test/Stack_source.tif");
    	        IJ.saveAsTiff(imgs[1],vesselsDir+"/Data/Processing/Step_01_detection/Weka_test/Stack_annotations.tif");
        
        
        }
        
		public static void step_01_augment_train_data() {
	        ImagePlus source=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source.tif");
	        ImagePlus mask=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations.tif");			

        	for(int i=0;i<NSTEPS;i++) {
            	System.out.println("Processing augmentation "+i+"/"+NSTEPS);
	        	ImagePlus sourceOut=source.duplicate();
	        	ImagePlus maskOut=mask.duplicate();
        		sourceOut=SegmentationUtils.brightnessAugmentationStack(sourceOut,false,2,0.3,i==0);
        		sourceOut=SegmentationUtils.colorAugmentationStack(sourceOut,false,2,0.3,i==0);
        		sourceOut=SegmentationUtils.rotationAugmentationStack(sourceOut,0.5,1);
        		IJ.saveAsTiff(sourceOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source"+("_AUGSET"+i)+".tif");
	            sourceOut=null;
        		Runtime. getRuntime(). gc();
       		
        		maskOut=SegmentationUtils.brightnessAugmentationStack(maskOut,true,2,0.3,i==0);
        		maskOut=SegmentationUtils.colorAugmentationStack(maskOut,true,2,0.3,i==0);
        		maskOut=SegmentationUtils.rotationAugmentationStack(maskOut,0.5,1);
        		IJ.saveAsTiff(maskOut, vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations"+("_AUGSET"+i)+".tif"); 		        
	            maskOut=null;
        		Runtime.getRuntime().gc();
        	}
    	}
        		
        public static void step_02_train_model(boolean multiModel) {
            for(int i=0;i<(multiModel ?4:1);i++) {
	            Runtime. getRuntime(). gc();
	            ImagePlus []imgs=new ImagePlus[] {
	        	        IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source"+("_AUGSET"+i)+".tif"),
	        	        IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations"+("_AUGSET"+i)+".tif"),
	            };
	            SegmentationUtils.wekaTrainModel(imgs[0],imgs[1],SegmentationUtils.getStandardRandomForestParams(i),SegmentationUtils.getStandardRandomForestFeatures(),vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/model_layer_1"+("_AUGSET"+i));
	            //ImagePlus temp=new Duplicator().run(firstProbaMaps,2,2,1,firstProbaMaps.getNSlices(),1,1);
	            //temp.show();
	            //IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Result_proba_layer_1_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+("STEP"+i)+".tif");
	
	            /*ImagePlus secondProbaMaps=wekaTest(temp,imgs[1],NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_2_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain));
	            temp=new Duplicator().run(secondProbaMaps,2,2,1,secondProbaMaps.getNSlices(),1,1);
	            temp.show();
	            IJ.saveAsTiff(temp,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Result_proba_layer_2_"+targetResolution+"_pix"+extension(useRotAug, useContAug)+(scenarioAug==0 ? "" : "SCENAUG"+scenarioAug)+(scenarioTrain==0 ? "" : "SCENTRAIN"+scenarioTrain)+".tif");                
	*/
            }	
        }
       		
        public static void step_03_apply_model(String dataType,boolean multiModel) {
        	if(  (!dataType.equals("training") ) && (!dataType.equals("validate") ) &&(!dataType.equals("test") ) ) {
        		System.out.println("Wrong data set type :"+dataType);
        		System.exit(0);
        	}
        	ImagePlus img=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_source.tif");
        	ImagePlus[] resultTab=new ImagePlus[(multiModel ? NSTEPS : 1 )];
        	for(int n=0;n<(multiModel ? NSTEPS : 1 );n++) {
    			IJ.log("Running step "+n);
        		resultTab[n]=SegmentationUtils.wekaApplyModelSlicePerSlice(img,SegmentationUtils.getStandardRandomForestParams(n),SegmentationUtils.getStandardRandomForestFeatures(),vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/model_layer_1"+("_AUGSET"+n+""));
        		resultTab[n]=new Duplicator().run(resultTab[n],2,2,1,resultTab[n].getNSlices(),1,1);
                IJ.saveAsTiff(resultTab[n],vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_"+"STEP"+n+".tif");
        	}
    		ImagePlus result=VitimageUtils.meanOfImageArray(resultTab);
            IJ.saveAsTiff(result,vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(multiModel ? "_multimodel" : "_monomodel")+".tif");
        }
 	
		public static void step_04_measure_scores(String dataType,boolean multiModel) {
			boolean verbose=false;
        	if(  (!dataType.equals("training") ) && (!dataType.equals("validate") ) &&(!dataType.equals("test") ) ) {
        		System.out.println("Wrong data set type :"+dataType);
        		System.exit(0);
        	}
        	System.out.println(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba.tif");
            ImagePlus binaryValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(multiModel ? "_multimodel" : "_monomodel")+".tif");
            ImagePlus binaryRefT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
            VitimageUtils.printImageResume(binaryValT);
            for(double d1=0.3;d1<0.9;d1+=0.1){
            	System.out.println("\n "+d1+"  -  "+d1);
            	binaryValT=SegmentationUtils.getSegmentationFromProbaMap3D(binaryValT,d1,d1);
            	SegmentationUtils.scoreComparisonSegmentations(binaryRefT,binaryValT);
            }
			VitimageUtils.compositeNoAdjustOf(binaryRefT,binaryValT).show();
		}

		public static void step_05_display_results(String dataType,boolean multiModel) {
        	if(  (!dataType.equals("training") ) && (!dataType.equals("validate") ) &&(!dataType.equals("test") ) ) {
        		System.out.println("Wrong data set type :"+dataType);
        		System.exit(0);
        	}
            ImagePlus binaryValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(multiModel ? "_multimodel" : "_monomodel")+".tif");
            ImagePlus binaryRefT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
		//	binaryValT=SegmentationUtils.cleanVesselSegmentation(binaryValT,targetResolutionVessel,MIN_VB_512,MAX_VB_512);
            ImagePlus sourceValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_source.tif");
            SegmentationUtils.visualizeMaskEffectOnSourceData(sourceValT,binaryValT,3).show();
            SegmentationUtils.visualizeMaskDifferenceOnSourceData(sourceValT,binaryRefT,binaryValT).show();
		}

       
		
		
	    private static final int targetResolutionVessel=512;
	    private static final int resizeFactorVessel=8;
		private static final long serialVersionUID = 1L;
		public static String vesselsDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";
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
    	ImagePlus img=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(useRotAugValidate ? "_old" : "")+".tif");
		ImagePlus resultTab=wekaApplyModelSlicePerSlice(img,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_2_512_pixSCENAUG3");
		resultTab=new Duplicator().run(resultTab,2,2,1,resultTab.getNSlices(),1,1);
        IJ.saveAsTiff(resultTab,vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_layer2"+(useRotAugValidate ? "_old" : "")+".tif");        	
    }
*/            
 
}