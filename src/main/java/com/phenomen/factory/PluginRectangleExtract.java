package com.phenomen.factory;
import com.phenomen.common.VitiDialogs;

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
}
