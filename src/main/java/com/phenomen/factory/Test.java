package com.phenomen.factory;

import inra.ijpb.watershed.MarkerControlledWatershedTransform2D;
import com.phenomen.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;

public class Test {
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();


		//Ouvrir proba et seg reference
		ImagePlus probaMap=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Processing/Step_01_detection/Weka_training/Result_proba_layer_1_512_pixSCENAUG4SCENTRAIN1.tif");
		ImagePlus segRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Processing/Step_01_detection/Weka_training/Stack_annotations_512_pixSCENAUG4.tif");
		probaMap=new Duplicator().run(probaMap,1,1,1,10,1,1);
		segRef=new Duplicator().run(segRef,1,1,1,10,1,1);
		
		ImagePlus segTest=SegmentationUtils.getSegmentationFromProbaMap3D(probaMap);
		
		SegmentationUtils.scoreComparisonSegmentations(segRef, segTest);
		VitimageUtils.compositeNoAdjustOf(segRef, segTest).show();
	}
}
