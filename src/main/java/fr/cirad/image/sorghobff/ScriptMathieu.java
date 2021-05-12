package fr.cirad.image.sorghobff;

import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.mlutils.SegmentationUtils;
import fr.cirad.image.ndpisafe.PluginOpenPreview;
import fr.cirad.image.ndpisafe.PluginRectangleExtract;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Scaler;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.gui.Roi;

import java.io.File;
import java.util.*;

public class ScriptMathieu extends PlugInFrame{
	private static final long serialVersionUID = 1L;

	
	/**  ---------------------------------------- Constructors and entry points -----------------------------------------------*/
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
		// N = 1200    n=700
		int n=7003 ; int N=8001;
		System.out.println( VitimageUtils.dou(n*100.0/N) );
		
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/test.tif");
		img.show();
		//testListImgToProcess();
	}
	
	
	public static ImagePlus resize(ImagePlus img, int targetX,int targetY,int targetZ) {
        return Scaler.resize(img, targetX,targetY, targetZ, " interpolation=Bilinear average create"); 		
	}
	

	public static void testListImgToProcess() {
		listImgToProcess(null,null,null);
	}
	
	/**  ---------------------------------------- Test functions -----------------------------------------------*/
    public static void listImgToProcess(String csvPath, String inputDirectory, String outputDirectory) {    	
    	// To use this function, set the input dir at lines 49 69 78
    	// TODO : Possibility to make the soft more reliable by adding a step after Analyze particles, selecting the largest ROI, just in case there is several
    	// TODO : Possibility to remove the ndpisplit (?) logs and add some hints about where is the process at
    	
    	//General parameters of the function
    	int resampleFactor = 8;
    	double fact=1.0/resampleFactor;

    	 	
    	//Extracting useful data from the CSV - Year/Genotype/Plant/Node/File name/which slice to chose
    	if(csvPath==null) csvPath ="D:/DONNEES/Recap_echantillons_2017_test.csv"; //Summary CSV file
    	String [][]baseSheet = SegmentationUtils.readStringTabFromCsv(csvPath);
    	ArrayList<String[]> finalSheet = new ArrayList<String[]>();
    	    	
    	for(int i=2;i<baseSheet.length;i++) {
    		if(baseSheet[i][11].equals("")) {
    			String[] intermediarySheet = {baseSheet[i][0],baseSheet[i][1],baseSheet[i][2],baseSheet[i][3],baseSheet[i][4],baseSheet[i][6]};
    			finalSheet.add(intermediarySheet);
     		}
    	}
    	String [][] finalTab = finalSheet.toArray(new String[finalSheet.size()][2]);
    	IJ.log("Initial list size was "+(baseSheet.length-2)+" and final list size is "+finalSheet.size());
    	IJ.log(finalSheet.size()*100.0/(baseSheet.length-2)+"% of the images are usable.");	
    	
    	// Indicating input dir (*.ndpi images) and output dir (*.tif images) )
    	if(inputDirectory==null) inputDirectory="D:/DONNEES/Test/Input/";
    	if(outputDirectory==null) outputDirectory="D:/DONNEES/Test/Output/";

    	
   	
		// Loop over the selected input ndpi's
		for(int j=0;j<finalTab.length;j++) {
			String fileIn=new File(inputDirectory,finalTab[j][4]).getAbsolutePath();			
			IJ.log("Processing extraction of image #"+(j+1)+" / "+(finalTab.length)+" : "+finalTab[j][4]);

			// Compute NDPI preview and set parameters for extraction
			ImagePlus preview = PluginOpenPreview.runHeadlessAndGetImagePlus(fileIn);
	    	String nameImgOut = finalTab[j][0]+"_"+finalTab[j][1]+"_"+finalTab[j][2]+"_"+finalTab[j][3];
	    	int targetHeight = preview.getHeight();
	    	int targetWidth = preview.getWidth();
	    		    	

	    	// Check whether the slice of interest is (G (left), D (right) or all the image)
	    	ImagePlus img=null;
	    	if(finalTab[j][5].equals("G")) {// The interesting data is on the left part of the image	    		
	    		img = PluginRectangleExtract.runHeadlessFromImagePlus(preview, 1, 0, 0, targetWidth/2, targetHeight);
	    	}
			else if(finalTab[j][5].equals("D")) {// The interesting data is on the right part of the image
	    		img = PluginRectangleExtract.runHeadlessFromImagePlus(preview, 1, targetWidth/2, 0, targetWidth-targetWidth/2, targetHeight);
			}
			else{		
	    		img = PluginRectangleExtract.runHeadlessFromImagePlus(preview, 1, 0, 0, targetWidth, targetHeight);
			}
	    	
  		 	// Drawing a bounding box around the image to limit the amount of pixel that will be treated after
    		img.show();
    		img.setTitle("Extract");
    		ImagePlus imgDup = img.duplicate();
    		IJ.run(imgDup, "8-bit", "");
    		IJ.setThreshold(140, 255);//Roughly get out the white part of the image
    		IJ.run(imgDup, "Convert to Mask", "");
    		IJ.run(imgDup, "Analyze Particles...", "size=50000-Infinity pixel include add");
	    	RoiManager rm = RoiManager.getRoiManager();
	    	Roi roi = rm.getRoi(0);
	    	img.setRoi(roi);
	    	//TODO : save ROi in a specified place, in order to struggle later with various geometries  
	    	IJ.run(img, "Enlarge...", "enlarge=20 pixel");
	    	IJ.run(img, "Crop", "");
	    	IJ.run(img, "Select None", "");
	    	rm.close();
	    	img.hide();
	    	
	    	// Resample and save the results
	    	int targetHeightExtract=img.getHeight()/resampleFactor;
	    	int targetWidthExtract=img.getWidth()/resampleFactor;
	        IJ.save(img,outputDirectory+"/"+nameImgOut+".tif");//save the level 1 version         
	        img=resize(img, targetWidthExtract, targetHeightExtract, 1);
	        IJ.save(img,outputDirectory+"/"+nameImgOut+"_resampled"+resampleFactor+".tif");
	        img=null;
	        IJ.log(fileIn+" converted.");		        
		}
		IJ.log("THE END");
    }	
    
//    IJ.run(img, "Scale...", "x="+fact+" y="+fact+" width="+targetWidthExtract+" height="+targetHeightExtract+" interpolation=Bilinear average create");//create
//    img=IJ.getImage();
//    img.hide();
    
    
    
    
    
 	/**  ---------------------------------------- Older test, not in use anymore -----------------------------------------------*/
    public static void faireTest1() {        
        ImagePlus impRef = IJ.openImage("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/FromRomain/To_Mat/Weka_test/Stack_annotations_512_pix.tif");
        ImagePlus impTest = IJ.openImage("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/FromRomain/To_Mat/Marc_test/Stack_annotations_512_pix.tif");

        impRef.show();
        impTest.show();

        IJ.log(""+SegmentationUtils.IOU(impRef, impTest));
        SegmentationUtils.scoreComparisonSegmentations(impRef, impTest,true);

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

        SegmentationUtils.scoreComparisonSegmentations(impRef, impTest,true);
        ImagePlus result1 = SegmentationUtils.visualizeMaskDifferenceOnSourceData(imgBase,impRef,impTest);
        //ImagePlus result2 = SegmentationUtils.visualizeMaskEffectOnSourceData(impRef,impTest,0);

        result1.show();
        //result2.show();

        VitimageUtils.waitFor(5000);
    }
    
    
}
	


