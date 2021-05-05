package fr.cirad.image.ndpisafe;
import fr.cirad.image.common.VitiDialogs;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.PlugInFrame;

public class PluginRectangleExtract extends PlugInFrame{
	private static final long serialVersionUID = 1L;

	public PluginRectangleExtract() {		super("");	}
	
	public void run(String arg) {
		ImagePlus img=IJ.getImage();
		Roi roi=img.getRoi();
		double x0=roi.getXBase();
		double y0=roi.getYBase();
		double dx=roi.getFloatWidth();
		double dy=roi.getFloatHeight();
		NDPI ndpi=new NDPI((String) img.getProperty("PreviewOfNDPIPath"));
		int level=VitiDialogs.getIntUI("Choose a resolution level (0=high-res, "+ndpi.N+"=low-res"+"Level",ndpi.previewLevelN); 
		ImagePlus imgOut=ndpi.getExtract(level,x0,y0,dx,dy);
		imgOut.show();		
	}	

	public ImagePlus runHeadlessFromImagePlus(ImagePlus img,int level,double x0,double y0,double dx, double dy) {
		NDPI ndpi=new NDPI((String) img.getProperty("PreviewOfNDPIPath"));
		ImagePlus imgOut=ndpi.getExtract(level,x0,y0,dx,dy);
		return imgOut;		
	}	

	public ImagePlus runHeadlessFromNDPI(NDPI myNdpi,int level,double x0,double y0,double dx, double dy) {
		ImagePlus imgOut=myNdpi.getExtract(level,x0,y0,dx,dy);
		return imgOut;		
	}	


}
