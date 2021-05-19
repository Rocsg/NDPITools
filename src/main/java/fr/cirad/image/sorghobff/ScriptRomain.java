package fr.cirad.image.sorghobff;

import fr.cirad.image.mlutils.SegmentationUtils;
import ij.ImageJ;
import ij.plugin.frame.PlugInFrame;

public class ScriptRomain extends PlugInFrame{
	private static final long serialVersionUID = 1L;
	public static String version="Handome_Honeysuckle";
	public int nbImagesToProcess=10;
	
	public ScriptRomain() {
		super("");
		this.nbImagesToProcess=50;
	}
	
	public ScriptRomain(String title) {
		super(title);
	}

	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		new ScriptRomain().run("");
	}
	
	public void run(String arg) {
		this.test();
	}
	
	
	
	public void test() {
		System.out.println("Start processing");
		SegmentationUtils.batchVesselSegmentation( VesselSegmentation.getVesselsDir(),"/home/rfernandez/Bureau/A_Test/Vaisseaux/In","/home/rfernandez/Bureau/A_Test/Vaisseaux/Out");
	}

}
