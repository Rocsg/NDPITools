//Le package n'est pas à recopier, sinon beanshell ne captera pas
package com.phenomen.sorghobff;

//En revanche, les import sont à recopier
import com.phenomen.common.VitimageUtils;
import com.phenomen.mlutils.SegmentationUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;


//Ca, ça ne sert pas dans un script
public class Matthieu_Test {
	public static void main(String []args) {

		//Par contre tout ce qu'il y a de START a END, il faut le copier, c'est le code
		/////////////////////////////// START ///////////////////////
		
		
		//On peut afficher des trucs
		IJ.log("Hello world !");
		
		//On peut créer et montrer des images
		ImagePlus imgTest=IJ.createImage("Image test", "8-bit", 512, 512, 1);
		imgTest.show();

		//On peut appeler des fonctions du package
		ImagePlus testField=SegmentationUtils.getAugmentationMap(imgTest,1,0.4,5);
		testField.setTitle("Mon image d augmentation");
		IJ.run(testField,"Fire","");
		testField.show();
		
		//Sinon, plus utile pour le soft de Marc, on peut ouvrir une image 
		String dir="...blablabla C:\\Bureau ou je sais pas quoi, tu peux t'en referer aux scripts precedents";
		ImagePlus img=IJ.openImage(dir+"/Test/imgMachin.tif");
		img.show();
		
		//Ensuite on peut ouvrir le Roiset que Marc a construit
		ImagePlus imgSource=IJ.openImage(dir+"/ResultatsroiMarch/imgMachin.tif");
		
		
		/////////////////////////////// END /////////////////////////

	//Pas besoin de copier ces accolades, puisque tu ne les a pas ouvertes dans ton script
	}	
}
