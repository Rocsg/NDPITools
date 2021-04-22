package com.phenomen.factory;

import com.phenomen.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class ScriptMathieu {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
						ImageJ im = new ImageJ();
			ImagePlus impRef = IJ.openImage("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/FromRomain/To_Mat/Weka_test/Stack_annotations_512_pix.tif");
			ImagePlus impTest = IJ.openImage("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/FromRomain/To_Mat/Marc_test/Stack_annotations_512_pix.tif");
		
			impRef.show();
			impTest.show();
			
			IJ.log(""+SegmentationUtils.IOU(impRef, impTest));
			SegmentationUtils.scoreComparisonSegmentations(impRef, impTest);
			
			VitimageUtils.waitFor(10000);
			System.exit(0);
	}

}
