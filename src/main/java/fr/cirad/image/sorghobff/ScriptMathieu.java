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
import java.io.FileOutputStream;
import java.io.PrintStream;
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
		String[][]tab=VitimageUtils.readStringTabFromCsv("/home/rfernandez/Téléchargements/vaisseaux_partie2.csv");
		System.out.println(tab.length);
		//listImgToProcess("D:/DONNEES/Sorgho_BFF/NDPI/2017/Recap_echantillons.csv", "D:/DONNEES/Sorgho_BFF/NDPI/2017/", "D:/DONNEES/Sorgho_BFF/ML1/");
	}
	
	
	public static ImagePlus resize(ImagePlus img, int targetX,int targetY,int targetZ) {
        return Scaler.resize(img, targetX,targetY, targetZ, " interpolation=Bilinear average create"); 		
	}
	
/*	public static void writeStringTabInExcelFile(String[][]tab,String fileName) {
		System.out.println("Impression de tableau de taille "+tab.length+" x "+tab[0].length);
		try { 
			PrintStream l_out = new PrintStream(new FileOutputStream(fileName)); 
			for(int i=0;i<tab.length;i++) {
				for(int j=0;j<tab[i].length;j++) {
					l_out.print(tab[i][j]+","); 
					System.out.print(tab[i][j]+",");
				}
				l_out.println(""); 
				System.out.println();
			}
			l_out.flush(); 
			l_out.close(); 
			l_out=null; 
		} 
		catch(Exception e){System.out.println(e.toString());} 
	}*/
	
	public static void testListImgToProcess() {
		//listImgToProcess(null,null,null);
		extractVessels(null, null);
	}
	
	
	
	
	
	/**  ---------------------------------------- Test functions -----------------------------------------------*/
    public static void listImgToProcess(String csvPath, String inputDirectory, String outputDirectory) {    	
    	// TODO : Possibility to make the soft more reliable by adding a step after Analyze particles, selecting the largest ROI, just in case there is several
    	
    	//General parameters of the function
    	int resampleFactor = 8;
    	    	
    	// Creating an arraylist to store the bounding boxes coordinates and later save them in a .csv
    	ArrayList<String[]> csvCoordinates = new ArrayList<String[]>();
    	String[] amorce = {"Sample", "Origin", "Xbase", "Ybase", "dX", "dY"};
    	csvCoordinates.add(amorce);
    	
    	// Indicating input dir (*.ndpi images) and output dir (*.tif images) )
    	if(inputDirectory==null) inputDirectory="D:/DONNEES/Test/Input/";
    	if(outputDirectory==null) outputDirectory="D:/DONNEES/Test/Output/";

    	//Extracting useful data from the CSV - Year/Genotype/Plant/Node/File name/which slice to chose
    	if(csvPath==null) csvPath ="D:/DONNEES/Recap_echantillons_2017_test.csv"; //Summary CSV file
    	String [][]baseSheet = VitimageUtils.readStringTabFromCsv(csvPath);
    	ArrayList<String[]> finalSheet = new ArrayList<String[]>();

    	for(int i=2;i<baseSheet.length;i++) {
    		if(baseSheet[i][11].equals("")) {//Image to be saved. If bad image, there is a marker in this case
    			String[] intermediarySheet = {baseSheet[i][0],baseSheet[i][1],baseSheet[i][2],baseSheet[i][3],baseSheet[i][4],baseSheet[i][6]};
    			finalSheet.add(intermediarySheet);
     		}
    	}
    	String [][] finalTab = finalSheet.toArray(new String[finalSheet.size()][2]);
    	IJ.log("Initial list size was "+(baseSheet.length-2)+" and final list size is "+finalSheet.size());
    	IJ.log(finalSheet.size()*100.0/(baseSheet.length-2)+"% of the images are usable.");	
    	
		// Loop over the selected input ndpi's
		//for(int j=0;j<finalTab.length;j++) {
    	for(int j=0;j<10;j++) {
			String fileIn=new File(inputDirectory,finalTab[j][4]).getAbsolutePath();			
			IJ.log("Processing extraction of image #"+(j+1)+" / "+(finalTab.length)+" : "+finalTab[j][4]);

			// Compute NDPI preview and set parameters for extraction
			ImagePlus preview = PluginOpenPreview.runHeadlessAndGetImagePlus(fileIn);
	    	String nameImgOut = finalTab[j][0]+"_"+finalTab[j][1]+"_"+finalTab[j][2]+"_"+finalTab[j][3];//Name uniformization
	    	int targetHeight = preview.getHeight();
	    	int targetWidth = preview.getWidth();
	    	preview.show();
	    	
	    	// Check whether the slice of interest is (G (left), D (right) or all the image)
	    	Roi areaRoi =null;
	    	if(finalTab[j][5].equals("G")) {// The interesting data is on the left part of the image	    		
	    		areaRoi = IJ.Roi(0, 0, targetWidth/2, targetHeight);
	     	}
	    	else if(finalTab[j][5].equals("D")) {// The interesting data is on the right part of the image
	    		areaRoi = IJ.Roi(targetWidth/2, 0, targetWidth-targetWidth/2, targetHeight);
			}
	    	else{		
	    		areaRoi = IJ.Roi(0, 0, targetWidth, targetHeight);
			}
	    	
	    	// Drawing a bounding box (divisible by 16 for later resampling) around the image to limit the amount of pixel that will be treated after
	    	ImagePlus dup = preview.duplicate();
    		IJ.run(dup, "8-bit", "");
    		IJ.setThreshold(140, 255);//Roughly get out the white part of the image
    		IJ.run(dup, "Convert to Mask", "");
	    	dup.setRoi(areaRoi);
	    	IJ.run(dup, "Analyze Particles...", "size=200-Infinity pixel include add");
	    	RoiManager rm = RoiManager.getRoiManager();
	    	Roi sampleRoi = rm.getRoi(0);
	    	preview.setRoi(sampleRoi);
	    	IJ.run(preview, "Enlarge...", "enlarge=2 pixel");
	    	IJ.run(preview, "To Bounding Box", "");
	    	Roi boundingBox = preview.getRoi();
	    	
	    	double x0=boundingBox.getXBase();
	    	double y0=boundingBox.getYBase();
	    	double dx=boundingBox.getFloatWidth();
	    	double dy=boundingBox.getFloatHeight();
			
			while(dx % 16 != 0) {
				dx++;
			}
			while(dy % 16 != 0) {
				dy++;
			}
	    	
			ImagePlus img = PluginRectangleExtract.runHeadlessFromImagePlus(preview, 1, x0, y0, dx, dy);
			rm.close();
			preview.close();
	    	img.hide();
	    	
	    	// Resample and save the results
	    	int targetHeightExtract=img.getHeight()/resampleFactor;
	    	int targetWidthExtract=img.getWidth()/resampleFactor;
	        IJ.save(img,outputDirectory+"Images_lvl1/"+nameImgOut+".tif");//save the level 1 version         
	        img=resize(img, targetWidthExtract, targetHeightExtract, 1);
	        IJ.save(img,outputDirectory+"Images_resampled8/"+nameImgOut+"_resampled"+resampleFactor+".tif");
	        img=null;
	        IJ.log(fileIn+" converted.");
	        
	        String xBase = String.valueOf(x0);
	        String yBase = String.valueOf(y0);
	        String width = String.valueOf(dx);
	        String height = String.valueOf(dy);
	        String[] coordinates = {nameImgOut, fileIn, xBase, yBase, width, height};
	        csvCoordinates.add(coordinates);
		}
	
		// Save the coordinates in .csv form
		String csv = outputDirectory+"Summary_coordinatesFromPreview.csv";	
		String [][] finalCoordinates = csvCoordinates.toArray(new String[csvCoordinates.size()][2]);	
		VitimageUtils.writeStringTabInCsv(finalCoordinates, csv);
		System.out.println(csv+" saved.");
	
		IJ.log("THE END");
    }	
    
    public static void extractVessels(String inputDirectory, String outputDirectory) {
    	
    	//General parameters of the function
    	int resampleFactor = 8;
    	
    	// Creating an arraylist to store the bounding boxes coordinates and later save them in a .csv
    	ArrayList<String[]> csvCoordinates = new ArrayList<String[]>();
    	String[] amorce = {"Sample", "Origin Image", "Vaisseau#", "Xbase", "Ybase", "dX", "dY"};
    	csvCoordinates.add(amorce);
    	    	
    	// Indicating input dir (*.tif source images and associated masks) and output dir (*.tif images)
    	if(inputDirectory==null) inputDirectory="D:/DONNEES/Test/Step2_Input/";
    	if(outputDirectory==null) outputDirectory="D:/DONNEES/Test/Step2_Output/";    	
    			
    	// Load segmented data from first ML step
    	//TODO : make a list of all images present in the input directory and make the loop go through them
    	ImagePlus mask = IJ.openImage(inputDirectory+"Mask_Img_insight_15_2.tif");
    	ImagePlus source = IJ.openImage(inputDirectory+"Source_Img_insight_15_2.jpg");
    			
    	// Load segmented image and transform in ROI[]
    	Roi[] vaisseauxRoiBase = SegmentationUtils.segmentationToRoi(mask);
    					
    	for(int i=0;i<vaisseauxRoiBase.length;i++) {
    		// Extract centroids information and resample it for source image
    		double[] centroid = vaisseauxRoiBase[i].getContourCentroid();
    				
    		int centroidXSource = (int) Math.round(centroid[0])*resampleFactor;
    		int centroidYSource = (int) Math.round(centroid[1])*resampleFactor;
    		
    		// Extract vessel on source image
    		Roi areaRoi = IJ.Roi(centroidXSource-100, centroidYSource-100, 200, 200);
    		source.setRoi(areaRoi);
    		ImagePlus vaisseauExtracted = source.crop();
    		
    		//Save the results
    		IJ.save(vaisseauExtracted,"D:/DONNEES/Test/Step2_Output/V"+(i+1)+"_Img_insight_15_2.tif");
    		
    	}
    			
    }
    
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
	


