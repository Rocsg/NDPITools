package com.phenomen.ndpisafe;
import ij.io.OpenDialog;
import ij.plugin.frame.PlugInFrame;

public class PluginOpenPreview extends PlugInFrame{
	private static final long serialVersionUID = 1L;

	public PluginOpenPreview() {super("");}
	
	public void run(String arg) {
		OpenDialog od=new OpenDialog("Choose a ndpi file");
		NDPI myndpi=new NDPI(od.getPath());
		myndpi.previewImage.show();		
	}	
}