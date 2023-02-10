package fr.cirad.image.test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.LutLoader;
import io.github.rocsg.fijiyama.common.VitimageUtils;

public class Test4 {

	public static void main (String[]args) {
		String imgPath="/home/rfernandez/Bureau/test.tif";
		ImagePlus img=IJ.openImage(imgPath);
		WindowManager.setTempCurrentImage(img);
		new LutLoader().run("fire");
		img.show();
	}
}