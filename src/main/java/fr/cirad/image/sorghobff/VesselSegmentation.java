package fr.cirad.image.sorghobff;

import java.io.File;

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
import ij.plugin.Duplicator;
import ij.plugin.frame.PlugInFrame;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;

public class VesselSegmentation extends PlugInFrame{
		
	 	/**
	 	 * This class is more a script : it executes successive operations to :
	 	 * 1) prepare data from json and source images to ML-ready data into valid and train sets
	 	 * 
	 	 */
		private static final double ratioStdBrightness=0.2;
		private static final double ratioStdColor=0.15;		
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
	    Timer t;
	    
	public void start (){
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
//      if(startStep<=3 && lastStep>=3)step_03_apply_model("test",true);
//       if(startStep<=3 && lastStep>=3)step_03_apply_model("validate",true);
       if(startStep<=3 && lastStep>=3)step_03_apply_model("training",true);
	   t.print("Starting step 4");

	   if(startStep<=4 && lastStep>=4)step_04_measure_scores("validate",true);
	   t.print("Starting step 5");
       if(startStep<=5 && lastStep>=5)step_05_display_results("test",true);
	   IJ.log("End test.");
	   System.out.println("End test.");
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

        	for(int i=0;i<6;i++) {
            	System.out.println("Processing augmentation "+i+"/"+NSTEPS);
	        	ImagePlus sourceOut=source.duplicate();
	        	ImagePlus maskOut=mask.duplicate();

	        	sourceOut=SegmentationUtils.brightnessAugmentationStack(sourceOut,false,2,ratioStdBrightness,true);
        		sourceOut=SegmentationUtils.colorAugmentationStack(sourceOut,false,2,ratioStdColor,true);
        		maskOut=SegmentationUtils.brightnessAugmentationStack(maskOut,true,2,ratioStdBrightness,true);
        		maskOut=SegmentationUtils.colorAugmentationStack(maskOut,true,2,ratioStdBrightness,true);

        		ImagePlus[]tab=SegmentationUtils.rotationAugmentationStack(sourceOut,maskOut,0.5,1,i);
        		IJ.saveAsTiff(tab[0], vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source"+("_AUGSET"+i)+".tif");
        		IJ.saveAsTiff(tab[1], vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations"+("_AUGSET"+i)+".tif"); 		        
	            sourceOut=null;
	            maskOut=null;
        		Runtime.getRuntime().gc();
        	}
    	}
        		
        public static void step_02_train_model(boolean multiModel) {
            for(int i=0;i<(multiModel ?6:1);i++) {
	            Runtime. getRuntime(). gc();
	            ImagePlus []imgs=new ImagePlus[] {
	        	        IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_source"+("_AUGSET"+i)+".tif"),
	        	        IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/Stack_annotations"+("_AUGSET"+i)+".tif"),
	            };
	            if(i<3)imgs[0]=VitimageUtils.splitRGBStackHeadLess(imgs[0])[(i)];
	            else        imgs[0]=VitimageUtils.getHSB(imgs[0])[(i-3)];
	            SegmentationUtils.wekaTrainModel(imgs[0],imgs[1],SegmentationUtils.getStandardRandomForestParams(i),SegmentationUtils.getStandardRandomForestFeatures(),vesselsDir+"/Data/Processing/Step_01_detection/Models/model_layer_1"+("_AUGSET"+i));
            }	
        }
       		
        public static void step_03_apply_model(String dataType,boolean multiModel) {
        	if(  (!dataType.equals("training") ) && (!dataType.equals("validate") ) &&(!dataType.equals("test") ) ) {
        		System.out.println("Wrong data set type :"+dataType);
        		System.exit(0);
        	}
        	ImagePlus[] resultTab=new ImagePlus[(multiModel ? 6 : 1 )];
        	for(int i=0;i<(multiModel ? 6 : 1 );i++) {
            	ImagePlus img=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_source.tif");
            	if(i<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(i)];
	            else        img=VitimageUtils.getHSB(img)[(i-3)];
	            IJ.log("Apply model aug "+i);
        		resultTab[i]=SegmentationUtils.wekaApplyModel(img,SegmentationUtils.getStandardRandomForestParams(i),SegmentationUtils.getStandardRandomForestFeatures(),vesselsDir+"/Data/Processing/Step_01_detection/Models/model_layer_1"+("_AUGSET"+i+""));
        		resultTab[i]=new Duplicator().run(resultTab[i],2,2,1,resultTab[i].getNSlices(),1,1);
                IJ.saveAsTiff(resultTab[i],vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_"+"STEP"+i+".tif");
                
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
            ImagePlus binaryRefT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
			 ImagePlus []res=new ImagePlus[100];
			 int incr=0;
            for(double d1=0.5;d1<=0.5;d1+=0.1){
                for(double d2=0.6;d2<=0.85;d2+=0.05){
            	System.out.println("\n "+d1+"  -  "+d2);
                ImagePlus binValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(multiModel ? "_multimodel" : "_STEP0")+".tif");
            	ImagePlus binaryValT=SegmentationUtils.getSegmentationFromProbaMap3D(binValT,d1,d2);
            	IJ.saveAsTiff(result,vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(multiModel ? "_multimodel" : "_monomodel")+".tif");

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
            ImagePlus binaryRefT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
			ImagePlus binaryValT=SegmentationUtils.cleanVesselSegmentation(binaryValT,targetResolutionVessel,MIN_VB_512,MAX_VB_512);
            ImagePlus sourceValT=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_source.tif");
            SegmentationUtils.visualizeMaskEffectOnSourceData(sourceValT,binaryValT,3).show();
            SegmentationUtils.visualizeMaskDifferenceOnSourceData(sourceValT,binaryRefT,binaryValT).show();
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
    	ImagePlus img=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba"+(useRotAugValidate ? "_old" : "")+".tif");
		ImagePlus resultTab=wekaApplyModelSlicePerSlice(img,NUM_TREES,NUM_FEATS,SEED,MIN_SIGMA,MAX_SIGMA,vesselsDir+"/Data/Processing/Step_01_detection/Weka_training/layer_2_512_pixSCENAUG3");
		resultTab=new Duplicator().run(resultTab,2,2,1,resultTab.getNSlices(),1,1);
        IJ.saveAsTiff(resultTab,vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_layer2"+(useRotAugValidate ? "_old" : "")+".tif");        	
    }
*/            
        public static void testMono(String dataType) {
       	   ImagePlus[]img=new ImagePlus[6];
       	   for(int i=0;i<6;i++) {
       		   img[i]=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_"+"STEP"+i+".tif");
       	   }
       	   ImagePlus in=VitimageUtils.compositeRGBByte(img[1], img[2], img[5], 1,1, 1);
       	  ImagePlus mask=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
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
      		   img[i]=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Result_proba_"+"STEP"+i+".tif");
      	   }
      	  ImagePlus mask=IJ.openImage(vesselsDir+"/Data/Processing/Step_01_detection/Weka_"+dataType+"/Stack_annotations.tif");
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