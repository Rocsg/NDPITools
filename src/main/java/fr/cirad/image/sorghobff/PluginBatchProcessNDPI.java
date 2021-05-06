package fr.cirad.image.sorghobff;
import java.io.File;

import fr.cirad.image.common.VitiDialogs;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.frame.PlugInFrame;
import loci.plugins.LociImporter;


/** Plugin set in NDPI Safe tools > Batch process NDPI
 * One should try to manage calling LociImporter() to set this right :
 *         	String params="windowless=true " +
        "open=/home/rfernandez/Bureau/G1P3E10.ndpi "+
        "color_mode=Composite "+
        "view=Hyperstack "+
        "stack_order=XYCZT "+
        "c_begin_2=1 c_end_2=3 c_step_2=1"+
        "series=3";
        	params="pouet";
new LociImporter().run(params);
 * 
 * 
 * 
 * 
 * */
public class PluginBatchProcessNDPI extends PlugInFrame{
	private static final long serialVersionUID = 1L;

	public PluginBatchProcessNDPI() {		super("");	}
	
	public static void main(String []args) {
		ImageJ ij=new ImageJ();
		new PluginBatchProcessNDPI().run("");
	}
	
	
	
	public void run(String arg) {
		String inputDirectory="/home/rfernandez/Bureau/TestIn";
		String outputDirectory="/home/rfernandez/Bureau/TestOut";
		String[]names=new String[] {"unfichier.ndpi","deuxiemefichier.ndpi"};
		
		for(String nameImg : names) {
			String fileIn=new File(inputDirectory,nameImg).getAbsolutePath();
			String fileOut=new File(outputDirectory,nameImg).getAbsolutePath();
			
			IJ.log("Processing transformation : ");
			IJ.log(fileIn+" converted to "+fileOut);

			//Here, set operations to do (ndpi import and result saving)
		
		}		
	}	
}