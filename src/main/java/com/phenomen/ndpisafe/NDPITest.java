package com.phenomen.ndpisafe;

import java.awt.event.ActionListener;

import com.phenomen.ndpilegacy.ExtractNDPI;
import com.phenomen.ndpilegacy.NDPIToolsPreviewPlugin;
import com.phenomen.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.frame.PlugInFrame;

public class NDPITest extends PlugInFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NDPITest() {
		super("");		// TODO Auto-generated constructor stub
	}
	public static void main(String[]args) {
	    	ImageJ ij=new ImageJ();
	    	
	    	testMatthieu();
	}	    	

	
	public static String[][]parseInsightDescriptorCSV(String pathToCSV){
		String content=VitimageUtils.readStringFromFile(pathToCSV);
		String[]contents=content.split("\n");
		String [][]ret=new String[contents.length-1][];
		for(int i=0;i<ret.length;i++) {
			ret[i]=contents[i+1].split(",");
		}
		return ret;
	}
	
	
	
	public static void testMatthieu() {
		boolean onlyStressTest=true;
		String pathRoiSource="PATHTOROI";
		String pathImgSource="PATHTOIMG";
		String xlsPath="/home/fernandr/Bureau/A_Test/Vaisseaux/InsightsDescriptor.csv";
		String[][]contents=parseInsightDescriptorCSV(xlsPath);
		for(int i=0;i<contents.length;i++) {
			System.out.println("\nProcessing insight number "+i+" : "+pathImgSource+"/"+contents[i][3]+"/Raw/"+contents[i][4]+" à extraire avec "+pathRoiSource+"/Img_insight_"+(i<10 ?"0" :"")+i+".roi");
			if(!onlyStressTest) {
				//WOrking phase
			}
		}
	}

	
	public static void testNDPI() {
		NDPI ndpi=new NDPI("/home/fernandr/Bureau/Traitements/Sylvie/NDPI_avec_bordel/G1P2E15_G1P2E17.ndpi");
    	ndpi.previewImage.show();
    	VitimageUtils.waitFor(5000);
    	ExtractNDPI.extractFromGUIBis("",false,"-x0.3125");	    	
	    }
	    
	 public void run(String arg) {
	    	IJ.showMessage("Nothing to test today !");
	 }
	 
	 
}
