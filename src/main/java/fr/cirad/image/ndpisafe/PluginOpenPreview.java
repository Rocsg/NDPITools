package fr.cirad.image.ndpisafe;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.frame.PlugInFrame;

public class PluginOpenPreview extends PlugInFrame{
	private static final long serialVersionUID = 1L;

	public PluginOpenPreview() {super("");}
	
	public void run(String arg) {
		OpenDialog od=new OpenDialog("Choose a ndpi file");
		NDPI myndpi=new NDPI(od.getPath(),false);
		myndpi.previewImage.show();		
	}	

	public static ImagePlus runHeadlessAndGetImagePlus(String path) {
		System.out.println("Opening NDPI in "+path);
		NDPI myndpi=new NDPI(path,false);
		return myndpi.previewImage;	
	}	

	public static NDPI runHeadlessAndGetNDPI(String path) {
		System.out.println("Opening NDPI in "+path);
		NDPI myndpi=new NDPI(path,false);
		return myndpi;	
	}	

}