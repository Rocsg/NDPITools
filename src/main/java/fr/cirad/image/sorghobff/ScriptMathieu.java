package fr.cirad.image.sorghobff;

import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.mlutils.SegmentationUtils;
import fr.cirad.image.ndpisafe.NDPI;
import fr.cirad.image.ndpisafe.PluginOpenPreview;
import fr.cirad.image.ndpisafe.PluginRectangleExtract;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.gui.Roi;

import java.io.File;
import java.util.*;

public class ScriptMathieu extends PlugInFrame{

    public ScriptMathieu(String title) {
		super(title);
	}

    public ScriptMathieu() {
    	super("");
    }
    
	//This method is entry point when testing from Eclipse
    public static void main(String[] args) {
		ImageJ ij=new ImageJ();	
		new ScriptMathieu().run("");
    }
	
	//This method is entry point when testing from Fiji
	public void run(String arg) {
        listImgToProcess();		
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
    	
    	// Inputs needed in line 83 / 101 / 102
    	// Possibility to make the soft more reliable by adding a step after Analyze particles, selecting the largest ROI, just in case there is several
    	// Possibility to remove the ndpisplit (?) logs and add some hints about where is the process at
    	
    	//Loading the summary CSV file
    	String csvpath ="D:/DONNEES/Recap_echantillons_2017_test.csv";
    	String [][]baseSheet = SegmentationUtils.readStringTabFromCsv(csvpath);
    	ArrayList<String[]> finalSheet = new ArrayList<String[]>();
    	
    	//Extracting useful data from the CSV - Year/Genotype/Plant/Node/File name/which slice to chose
    	for(int i=2;i<baseSheet.length;i++) {
    		if(baseSheet[i][11].equals("")) {
    			String[] intermediarySheet = {baseSheet[i][0],baseSheet[i][1],baseSheet[i][2],baseSheet[i][3],baseSheet[i][4],baseSheet[i][6]};
    			finalSheet.add(intermediarySheet);
     		}
    	}
    	String [][] finalTab = finalSheet.toArray(new String[finalSheet.size()][2]);
    	IJ.log("Initial list size was "+(baseSheet.length-2)+" and final list size is "+finalSheet.size());
    	IJ.log(finalSheet.size()*100/(baseSheet.length-2)+"% of the images are usable.");	
    	//for(String[]s:finalSheet)System.out.println(""+s[0]+s[1]+s[2]+s[3]+s[4]+s[5]);
    	
  
    	// Indicating places to pick the images up and store the resulting images
    	String inputDirectory="D:/DONNEES/Test/Input/";
		String outputDirectory="D:/DONNEES/Test/Output/";
				
		
		for(int j=0;j<finalTab.length;j++) {

			String fileIn=new File(inputDirectory,finalTab[j][4]).getAbsolutePath();			
			IJ.log("Processing transformation : ");
			// NDPI preview
			ImagePlus preview = PluginOpenPreview.runHeadlessAndGetImagePlus(fileIn);
	    	IJ.log(VitimageUtils.imageResume(preview));
	    	// Setting parameters for extraction
	    	String nameImg = finalTab[j][0]+"_"+finalTab[j][1]+"_"+finalTab[j][2]+"_"+finalTab[j][3];
	    	int targetHeight = preview.getHeight();
	    	int targetWidth = preview.getWidth();
	    	int resampleFactor = 8;
	    	double fact=1.0/resampleFactor;
	    	
	    	// Loops to sort the images according to where the slice of interest is (G, D or only once slice on the image)
	    	if(finalTab[j][5].equals("G")) {
	    		
	    		// Extract the left side
	    		ImagePlus img = PluginRectangleExtract.runHeadlessFromImagePlus(preview, 1, 0, 0, targetWidth/2, targetHeight);
	    		IJ.log(VitimageUtils.imageResume(img));
	    		img.show();
	    		img.setTitle("Extract");
	    		
	    		// Drawing a bounding box around the image to limit the amount of pixel that will be treated after
	    		ImagePlus imgDup = img.duplicate();
	    		IJ.run(imgDup, "8-bit", "");
	    		IJ.setThreshold(140, 255);
	    		IJ.run(imgDup, "Convert to Mask", "");
	    		IJ.run(imgDup, "Analyze Particles...", "size=50000-Infinity pixel include add");
	      	
		    	RoiManager rm = RoiManager.getRoiManager();
		    	Roi roi = rm.getRoi(0);
		    	img.setRoi(roi);
		    	IJ.run(img, "Enlarge...", "enlarge=20 pixel");
		    	IJ.run(img, "Crop", "");
		    	IJ.run(img, "Select None", "");
		    	rm.close();
		    	
		    	// Resampling and saving the resulting images
		    	int targetHeightExtract=img.getHeight()/resampleFactor;
		    	int targetWidthExtract=img.getWidth()/resampleFactor;
		        img.show();
		        IJ.save(img,outputDirectory+"/"+nameImg+".tif");
		        IJ.run("Scale...", "x="+fact+" y="+fact+" width="+targetWidthExtract+" height="+targetHeightExtract+" interpolation=Bilinear average create");//create
		        img=IJ.getImage();
		        IJ.save(img,outputDirectory+"/"+nameImg+"_resampled"+resampleFactor+".tif");
		        img.changes=false;
		        img.close();
		        img=IJ.getImage();
		        img.changes=false;
		        img.close();
		        IJ.log(fileIn+" converted.");
		        
	    	} else if(finalTab[j][5].equals("D")) {
	    		
	    		// Extract the left side
	    		ImagePlus img = PluginRectangleExtract.runHeadlessFromImagePlus(preview, 1, targetWidth/2, 0, targetWidth-targetWidth/2, targetHeight);
	    		IJ.log(VitimageUtils.imageResume(img));
	    		img.show();
	    		img.setTitle("Extract");

	    		// Drawing a bounding box around the image to limit the amount of pixel that will be treated after
	    		ImagePlus imgDup = img.duplicate();
	    		IJ.run(imgDup, "8-bit", "");
	    		IJ.setThreshold(140, 255);
	    		IJ.run(imgDup, "Convert to Mask", "");
	    		IJ.run(imgDup, "Analyze Particles...", "size=50000-Infinity pixel include add");
	      	
		    	RoiManager rm = RoiManager.getRoiManager();
		    	Roi roi = rm.getRoi(0);
		    	img.setRoi(roi);
		    	IJ.run(img, "Enlarge...", "enlarge=20 pixel");
		    	IJ.run(img, "Crop", "");
		    	IJ.run(img, "Select None", "");
		    	rm.close();

		    	// Resampling and saving the resulting images
		    	int targetHeightExtract=img.getHeight()/resampleFactor;
		    	int targetWidthExtract=img.getWidth()/resampleFactor;
		        img.show();
		        IJ.save(img,outputDirectory+"/"+nameImg+".tif");
		        IJ.run("Scale...", "x="+fact+" y="+fact+" width="+targetWidthExtract+" height="+targetHeightExtract+" interpolation=Bilinear average create");//create
		        img=IJ.getImage();
		        IJ.save(img,outputDirectory+"/"+nameImg+"_resampled"+resampleFactor+".tif");
		        img.changes=false;
		        img.close();
		        img=IJ.getImage();
		        img.changes=false;
		        img.close();
		        IJ.log(fileIn+" converted.");
		        
	    	} else{
	    		
	    		// Extract the left side
	    		ImagePlus img = PluginRectangleExtract.runHeadlessFromImagePlus(preview, 1, 0, 0, targetWidth, targetHeight);
	    		IJ.log(VitimageUtils.imageResume(img));
	    		img.show();
	    		img.setTitle("Extract");

	    		// Drawing a bounding box around the image to limit the amount of pixel that will be treated after
	    		ImagePlus imgDup = img.duplicate();
	    		IJ.run(imgDup, "8-bit", "");
	    		IJ.setThreshold(140, 255);
	    		IJ.run(imgDup, "Convert to Mask", "");
	    		IJ.run(imgDup, "Analyze Particles...", "size=50000-Infinity pixel include add");
	      	
		    	RoiManager rm = RoiManager.getRoiManager();
		    	Roi roi = rm.getRoi(0);
		    	img.setRoi(roi);
		    	IJ.run(img, "Enlarge...", "enlarge=20 pixel");
		    	IJ.run(img, "Crop", "");
		    	IJ.run(img, "Select None", "");
		    	rm.close();

		    	// Resampling and saving the resulting images
		    	int targetHeightExtract=img.getHeight()/resampleFactor;
		    	int targetWidthExtract=img.getWidth()/resampleFactor;
		        img.show();
		        IJ.save(img,outputDirectory+"/"+nameImg+".tif");
		        IJ.run("Scale...", "x="+fact+" y="+fact+" width="+targetWidthExtract+" height="+targetHeightExtract+" interpolation=Bilinear average create");//create
		        img=IJ.getImage();
		        IJ.save(img,outputDirectory+"/"+nameImg+"_resampled"+resampleFactor+".tif");
		        img.changes=false;
		        img.close();
		        img=IJ.getImage();
		        img.changes=false;
		        img.close();
		        IJ.log(fileIn+" converted.");
	    	}    	
    }
    
		IJ.log("THE END");
    }		
}
	


