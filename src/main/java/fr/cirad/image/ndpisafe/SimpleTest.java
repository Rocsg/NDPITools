package fr.cirad.image.ndpisafe;


import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.tool.PlugInTool;

public class SimpleTest extends PlugInTool{
	private static final long serialVersionUID = 1L;

	public SimpleTest() {
		super();
	}
	
	public SimpleTest(String title) {
		super();
	}
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		SimpleTest st=new SimpleTest();
		st.run("");
	}

	public void run(String arg) {
//		IJ.showMessage("Toto");
		System.out.println("Tata");
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/test.tif");
		img.setDisplayRange(0, 1);
		IJ.run(img,"8-bit","");
		IJ.saveAsTiff(img,	"/home/rfernandez/Bureau/res.tif");
	}

}
