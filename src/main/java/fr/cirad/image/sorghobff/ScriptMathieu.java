package fr.cirad.image.sorghobff;

import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.mlutils.SegmentationUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.util.*;
import java.util.LinkedList;

public class ScriptMathieu {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        //ImageJ im = new ImageJ();

        //faireTest0();
        //interexpertTest();
        listImgToProcess();

    }

    public static void faireTest0() {        
        System.out.println("Blabla");
    }

    public static void maCame() {
    	System.out.println("Test merge");
    }
    
    public static void faireTest1() {        
        ImagePlus impRef = IJ.openImage("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/FromRomain/To_Mat/Weka_test/Stack_annotations_512_pix.tif");
        ImagePlus impTest = IJ.openImage("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/FromRomain/To_Mat/Marc_test/Stack_annotations_512_pix.tif");

        impRef.show();
        impTest.show();

        IJ.log(""+SegmentationUtils.IOU(impRef, impTest));
        SegmentationUtils.scoreComparisonSegmentations(impRef, impTest);

        VitimageUtils.waitFor(10000);
        System.exit(0);
    }

    public static void interexpertTest() {
        ImagePlus[] imgMatthieu=SegmentationUtils.jsonToBinary("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/InterExpert/Interexpert_assessment/Interexpert_Mathieu_subsampled_512_pix");
        imgMatthieu[1].show();
        ImagePlus[] imgRomain=SegmentationUtils.jsonToBinary("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/InterExpert/Interexpert_assessment/Interexpert_Romain_subsampled_512_pix");
        imgRomain[1].show();

        ImagePlus imgBase = imgMatthieu[0];
        ImagePlus impRef = imgRomain[1];
        ImagePlus impTest = imgMatthieu[1];

        SegmentationUtils.scoreComparisonSegmentations(impRef, impTest);
        ImagePlus result1 = SegmentationUtils.visualizeMaskDifferenceOnSourceData(imgBase,impRef,impTest);
        //ImagePlus result2 = SegmentationUtils.visualizeMaskEffectOnSourceData(impRef,impTest,0);

        result1.show();
        //result2.show();

        VitimageUtils.waitFor(5000);
    }
    
    
    public static void listImgToProcess() {
    	String csvpath ="D:/DONNEES/Recap_echantillons_2017.csv";
    	String [][]baseSheet = SegmentationUtils.readStringTabFromCsv(csvpath);
    	//String [][]finalSheet = {{"Location","Side"}};
    	
    	
    	ArrayList<String> finalSheet = new ArrayList<String>(1);
    	
    	ArrayList[][] table = new ArrayList[1][1];
    	
    	
    	
    	
    	table[0][0] = new ArrayList(finalSheet); // add another ArrayList object to [0,0]
    	table[0][0].add(null); // add object to that ArrayList
    	
    	
    	
    	//LinkedList<String> finalSheet = new LinkedList<String>();
    	//String [][]finalSheet = {{"Location","Side"}};
    	//String imgPath = sheet[][];
    	//System.out.println(baseSheet[2][4]);
    	//System.out.println(finalSheet[0][0]);
    	
    	
    	//String [][]finalSheet = finalSheet.push({sheet[2][4],sheet[2][5]});
    	//finalSheet.push({sheet[2][4],sheet[2][5]});
    	
    	//if(baseSheet[2][11].equals("")) {
    		//String[] intermediarySheet = {baseSheet[2][4],baseSheet[2][6]};
			//System.out.println(intermediarySheet[0]);
			//finalSheet.add(intermediarySheet);
			
			
	}
}

