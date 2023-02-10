package fr.cirad.image.test;


import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.frame.PlugInFrame;

public class Simple_Test extends PlugInFrame{
	private static final long serialVersionUID = 1L;

	public Simple_Test() {
		super("");
	}
	
	public Simple_Test(String title) {
		super(title);
	}
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		int i=12;
		Syr s=new Syr(i);
		System.out.println(s);
	}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		Simple_Test st=new Simple_Test();
		st.run("");
	}

	public void run(String arg) {
		IJ.showMessage("Toto");
		System.out.println("Tata");
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/test.tif");
		img.setDisplayRange(0, 1);
		IJ.run(img,"8-bit","");
		IJ.saveAsTiff(img,	"/home/rfernandez/Bureau/res.tif");
	}

}
