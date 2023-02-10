package fr.cirad.image.test;

import ij.ImageJ;
import ij.plugin.frame.PlugInFrame;

public class ExamplePlugin extends PlugInFrame{
	private static final long serialVersionUID = 1L;

	public ExamplePlugin() {
		super("");
	}
	public ExamplePlugin(String arg) {
		super(arg);
	}

	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		ExamplePlugin val=new ExamplePlugin();
		val.run("");
	}

	public void run() {
		
	}
	
	
	
}
