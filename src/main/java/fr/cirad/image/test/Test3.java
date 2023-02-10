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

import io.github.rocsg.fijiyama.common.VitimageUtils;

public class Test3 {

	public static void main (String[]args) {
		String path="/media/rfernandez/DATA_RO_A/Roots_systems/Data_BPMP/Third_dataset_2022_11/Source_data/Inventory_of_221125-CC-CO2";
		String[]listImgs=new File(path).list(new FilenameFilter() {		
			@Override
			public boolean accept(File arg0, String arg1) {
				if(arg1.equals("A_main_inventory.csv"))return false;
				return true;
			}
		});
		for(String s : listImgs)System.out.println(s);
	}
	
	
	public static double hoursBetween(FileTime t1, FileTime t2) {
		return VitimageUtils.dou((t2.toMillis()-t1.toMillis())/(3600*1000.0));
	}

	public static FileTime getTime(String path) {
		try {
			return Files.readAttributes(Paths.get(path), BasicFileAttributes.class).lastModifiedTime();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} 
		//"Date du dernier accès: " + attr.lastAccessTime());
		//"Date derniere modification: " + attr.lastModifiedTime());
	}


	static String[]getRelativePathOfAllImageFilesInDir(String rootDir){		
		String rdt=new File(rootDir).getAbsolutePath();//Without the / at the end
		return Arrays.stream(searchImagesInDir(rootDir)).map( p -> p.replace(rdt,"").substring(1)).toArray(String[]::new);
	}

	static String[]getRelativePathOfAllImageFilesInDirByTimeOrder(String rootDir){		
		String rdt=new File(rootDir).getAbsolutePath();//Without the / at the end
		File[]init =Arrays.stream(searchImagesInDir(rootDir)).map(x -> new File(x) ).toArray(File[]::new);
		Arrays.sort(init,Comparator.comparingLong(File::lastModified));
		return (Stream.of(init).map(f -> f.getAbsolutePath()).map(s -> s.replace(rdt,"").substring(1) )).toArray(String[]::new);
	}

	
	static String[]searchImagesInDir(String rootDir){
	    try {
	    	Stream<Path> paths = Files.find(Paths.get(rootDir),Integer.MAX_VALUE, (path, file) -> file.isRegularFile());
	    	String[]tab=paths.map(p -> p.toString()).filter(s -> isImagePath(s)).toArray(String[]::new);
	    	return tab;
		} catch (IOException e) {
		    e.printStackTrace();
		    return null;
		}
	}

	public static boolean isImagePath(String x) {
		String[] okFileExtensions = new String[] { "jpg", "jpeg", "png", "tif","tiff"};
    	for (String extension : okFileExtensions) {
            if (x.toLowerCase().endsWith(extension))     return true;  
    	}
    	return false;
	}

}