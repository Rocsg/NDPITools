package fr.cirad.image.sorghobff;

import io.github.rocsg.fijiyama.common.*;
import fr.cirad.image.mlutils.SegmentationUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class PaperValidation {
	
	
	public static double pixSize4096=1.842;//µm per pixel
	public static double pixSize512=14.736;//µm per pixel
	public static int thresholdSize=3000;//squared µm
	
	
	public static void main(String []args) {
		ImageJ ij=new ImageJ();
		//Interexpert assessment
		//test();
		//System.exit(0);
		if(false)doInterExpert();
		if(false)doStep1_part_1Train();
		if(false)doStep1_part_2Train();
		if(false)doStep1_part_1();
		if(false)doStep1_part_2();
		if(false)doStep1_part_1Valid();
		if(false)doStep1_part_2Valid();
		if(false)countStructuresStep2();
		if(false)segmentAndEvaluateVessels();
		if(true)fading();
	}
	
	
	
	public static void segmentAndEvaluateVessels() {
		//1)Constitute set
		ImagePlus imgSourceTest=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Vessels_dataset/"+
		"Split_train_val_test/Test/Source.tif");
		//100 slices, in which there is as a title : "V32_Img_insight_11_2"
		
		//In CSV such as
		String csv="/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Vessels_dataset/Full_vessels/Full_vessels_ordered_by_slice/Img_insight_11_2/Img_insight_11_2.csv";
		//There is Img_insight_11_2 	32	2765	1297	200	200

		
		//I just need to make a CSV with for each insight, coordinates of the center. Then I can hack the stuff
		//For each		

	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void fading() {
		String dir="/home/rfernandez/Bureau/A_Test/Vaisseaux/Figures";
		ImagePlus sourceRGB=IJ.openImage(dir+"/Fading_sourceBright.tif");
		ImagePlus segRGB=IJ.openImage(dir+"/Fading_seg.tif");
		ImagePlus targetRGB=VitimageUtils.fadeRGB(sourceRGB,segRGB,53,15);
		targetRGB.show();
	}
	

	
	
	public static void countStructuresStep2(){
		double nb=0;
		int[]stats=new int[20];
		String stuff="/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/";
		String dirContourVess=stuff+"Vessels_dataset/Image_and_annotations_for_training/Vessel_convex_hull";
		//Name under the form stuff/vessel_1.tif
		String dirXylAll=stuff+"Vessels_dataset/Image_and_annotations_for_training/Xylem";
		//Name under the form stuff/Segmentation_vessel_1.tif
		for(int i=554;i<=600;i++) {
			ImagePlus segXyl=IJ.openImage(dirXylAll+"/Segmentation_vessel_"+i+".tif");
			ImagePlus contourVess=IJ.openImage(dirContourVess+"/Segmentation_vessel_"+i+".tif");
			contourVess=SegmentationUtils.selectCentralMostRoi(contourVess);
			ImagePlus rest=VitimageUtils.binaryOperationBetweenTwoImages(segXyl, contourVess, 2);
			rest=VitimageUtils.connexeNoFuckWithVolume(rest, 1, 1000000, 1, 1000000, 4, 0, false);
			double delta=VitimageUtils.maxOfImage(rest);
			nb+=delta;
			stats[(int) delta]++;
			System.out.println("Processed : "+i+" nb="+nb+"   Delta="+delta);
			if(i==554) {
				segXyl.show();segXyl.setTitle("SegXyl");
				contourVess.show();contourVess.setTitle("ContourVess");
				rest.show();rest.setTitle("Rest");
			}
			if(delta==0)IJ.showMessage("Thats me "+i);
		}
		System.out.println("Nb="+nb);
		for(int i=0;i<20 ; i++)System.out.println("STATS["+i+"]="+stats[i]);
	}

		
	public static void doStep1_part_1Valid() {
		//Open the binary segmentation predicted and annotated, and compare it
		//La il y a toutes les annotations en json 
		///home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Slices_dataset/Splitted_annotation_dataset/Test
		String vesselDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";
		
		ImagePlus sourceImage=IJ.openImage("Data/Computation_and_results/Step_01/Weka_valid/Stack_source.tif");
		ImagePlus segRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_validate/Stack_annotations.tif");
		 
	  	//Prealable : vesselsDir,inputDir,outputDir, 
    	SegmentationUtils.batchVesselSegmentation(vesselDir,
    			vesselDir+"/Data/Computation_and_results/Step_01/Weka_validate/SourceBySlices",
    			vesselDir+"/Data/Computation_and_results/Step_01/Weka_validate/ResultBySlices");
	
		
	}
	
	public static void doStep1_part_1Train() {
		//Open the binary segmentation predicted and annotated, and compare it
		//La il y a toutes les annotations en json 
		///home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Slices_dataset/Splitted_annotation_dataset/Test
		String vesselDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";
		
		ImagePlus sourceImage=IJ.openImage("Data/Computation_and_results/Step_01/Weka_test/Stack_source.tif");
		ImagePlus segRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_test/Stack_annotations.tif");
		 
	  	//Prealable : vesselsDir,inputDir,outputDir, 
    	SegmentationUtils.batchVesselSegmentation(vesselDir,
    			vesselDir+"/Data/Computation_and_results/Step_01/Weka_training/SourceBySlices",
    			vesselDir+"/Data/Computation_and_results/Step_01/Weka_training/ResultBySlices");
	
		
	}
		
		
		public static void doStep1_part_1() {
			//Open the binary segmentation predicted and annotated, and compare it
			//La il y a toutes les annotations en json 
			///home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Slices_dataset/Splitted_annotation_dataset/Test
			String vesselDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";
			
			ImagePlus sourceImage=IJ.openImage("Data/Computation_and_results/Step_01/Weka_test/Stack_source.tif");
			ImagePlus segRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_test/Stack_annotations.tif");
			 
		  	//Prealable : vesselsDir,inputDir,outputDir, 
	    	SegmentationUtils.batchVesselSegmentation(vesselDir,
	    			vesselDir+"/Data/Computation_and_results/Step_01/Weka_test/SourceBySlices",
	    			vesselDir+"/Data/Computation_and_results/Step_01/Weka_test/ResultBySlices");
		
			
		}
		
		public static void doStep1_part_2() {
			//Open the binary segmentation predicted and annotated, and compare it
			//La il y a toutes les annotations en json 
			///home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Slices_dataset/Splitted_annotation_dataset/Test
			String vesselDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";			
			ImagePlus sourceImage=IJ.openImage("Data/Computation_and_results/Step_01/Weka_test/Stack_source.tif");
			ImagePlus segPred=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_test/Stack_result_bin.tif");
			ImagePlus segRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_test/Stack_annotations.tif");
			double threshVolume=thresholdSize/(pixSize512*pixSize512);
			double pixSurf=pixSize512*pixSize512;
	
			
			segPred=removeLittleVB(segPred, threshVolume);
			segRef=removeLittleVB(segRef, threshVolume);
			SegmentationUtils.scoreComparisonSegmentations_v2(segRef,segPred,pixSurf,0,49999,3000);
		}
		
		
		public static void doStep1_part_2Train() {
			//Open the binary segmentation predicted and annotated, and compare it
			//La il y a toutes les annotations en json 
			///home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Slices_dataset/Splitted_annotation_dataset/Test
			String vesselDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";			
			ImagePlus sourceImage=IJ.openImage("Data/Computation_and_results/Step_01/Weka_training/Stack_source.tif");
			ImagePlus segPred=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_training/Stack_result_bin.tif");
			ImagePlus segRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_training/Stack_annotations.tif");
			double threshVolume=thresholdSize/(pixSize512*pixSize512);
			double pixSurf=pixSize512*pixSize512;
	
			
			segPred=removeLittleVB(segPred, threshVolume);
			segRef=removeLittleVB(segRef, threshVolume);
			SegmentationUtils.scoreComparisonSegmentations_v2(segRef,segPred,pixSurf,0,49999,3000);
		}
	
		public static void doStep1_part_2Valid() {
			//Open the binary segmentation predicted and annotated, and compare it
			//La il y a toutes les annotations en json 
			///home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Slices_dataset/Splitted_annotation_dataset/Test
			String vesselDir="/home/rfernandez/Bureau/A_Test/Vaisseaux";			
			ImagePlus sourceImage=IJ.openImage("Data/Computation_and_results/Step_01/Weka_validate/Stack_source.tif");
			ImagePlus segPred=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_validate/Stack_result_bin.tif");
			ImagePlus segRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Computation_and_results/Step_01/Weka_validate/Stack_annotations.tif");
			double threshVolume=thresholdSize/(pixSize512*pixSize512);
			double pixSurf=pixSize512*pixSize512;
	
			
			segPred=removeLittleVB(segPred, threshVolume);
			segRef=removeLittleVB(segRef, threshVolume);
			SegmentationUtils.scoreComparisonSegmentations_v2(segRef,segPred,pixSurf,0,49999,3000);
		}
		
		
	
	public static void doInterExpert() {
		double threshVolume=thresholdSize/(pixSize4096*pixSize4096);
		double pixSurf=pixSize4096*pixSize4096;
		String expDir="/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Interexpert_dataset";
		String dirMat=expDir+"/Interexpert_Mathieu";
		ImagePlus []resultMat=SegmentationUtils.jsonToBinary(dirMat);
		resultMat[1]=removeLittleVB(resultMat[1], threshVolume);
		//resultMat[1].show();
		//resultMat[1].setTitle("BinaryMaskMat");		
//		resultMat[0].show();
		
		String dirRo=expDir+"/Interexpert_Romain";
		ImagePlus []resultRo=SegmentationUtils.jsonToBinary(dirRo);
		resultRo[1]=removeLittleVB(resultRo[1], threshVolume);
		//resultRo[1].show();
		//resultRo[1].setTitle("BinaryMaskRo");
		System.out.println("With Ro as Ref");
		SegmentationUtils.scoreComparisonSegmentations_v2(resultRo[1],resultMat[1],pixSurf,0,49999,3000);

		
		System.out.println("With Mat as Ref");
	//	SegmentationUtils.scoreComparisonSegmentations_v2(resultMat[1],resultRo[1],pixSurf,0,29999,3000);
		VitimageUtils.waitFor(5000000);
	}
	
	public static ImagePlus removeLittleVB(ImagePlus img,double threshold) {
		ImagePlus img2=VitimageUtils.connexe2dNoFuckWithVolume(img, 1, 256, threshold, 1E10, 8, 0, false);
		img2=VitimageUtils.thresholdImage(img2, 0.5, 1000000);
		img2.setDisplayRange(0, 1);
		IJ.run(img2,"8-bit","");
		return img2;
	}
	
	
}
