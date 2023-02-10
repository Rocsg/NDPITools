package fr.cirad.image.aerenchyme;
import java.io.File;
import java.util.ArrayList;

import io.github.rocsg.fijiyama.common.VitimageUtils;
import ij.IJ;
import ij.ImagePlus;
public class AerenchymeUtils {

	
	
//	/home/rfernandez/Bureau/A_Test/Aerenchyme/Dataset_Production_Justin_Juin_01/Raw_Data/DATA_SET_REMAKE_13.05.22_TRI
	
	public static void main(String[]args) {
		String s="/home/rfernandez/Bureau/A_Test/Aerenchyme/Dataset_Mutants_Juin/";
		formatBunchOfImageOneDepthDirsInSingleDir(s+"Raw_Data",s+"Source",null);
	}
	
	//Take a dir with dirs1 with dirs2 with dirs3, collect names and aggregate
	public static void formatBunchOfImageThreeDepthDirsInSingleDir(String dirIn,String dirOut) {
		if(!new File(dirOut).exists()) new File(dirOut).mkdirs();
		if(!new File(dirIn).exists()) {IJ.showMessage("Dir in does not exists : "+dirIn);return;}
		String[]tabNames1=new File(dirIn).list();
		for(String name1 : tabNames1) {
			String path1=new File(dirIn,name1).getAbsolutePath();
			String[]tabNames2=new File(path1).list();
			for(String name2 : tabNames2) {
				String path2=new File(path1,name2).getAbsolutePath();
				String[]tabNames3=new File(path2).list();
				for(String name3 : tabNames3) {
					String path3=new File(path2,name3).getAbsolutePath();
					String[]tabNames4=new File(path3).list();
					for(String name4 : tabNames4) {
						String path4=new File(path3,name4).getAbsolutePath();
						String finalNameImg=name1+"__"+name2+"__"+name3+"__"+name4;
						ImagePlus img=IJ.openImage(path4);
						IJ.saveAsTiff(img, new File(dirOut,finalNameImg).getAbsolutePath());
						System.out.println(finalNameImg);
					}
				}
			}
		}				
	}

	
	public static String findInfoInStringTab(String[][]tab,int targetColumn,String[]infos,int[]columns) {
		for(int i=0;i<tab.length;i++) {
			boolean goodLine=true;
			for(int j=0;j<columns.length;j++) {
				if(!tab[i][columns[j]].equals(infos[j]))goodLine=false;
			}
			if(goodLine)return tab[i][targetColumn];
		}
		return null;
	}
	
	//Take a dir with dirs1 with dirs2 with dirs3, collect names and aggregate
	public static void formatBunchOfImageOneDepthDirsInSingleDir(String dirIn,String dirOut,String selectionCsv) {
		String[][]tabs=null;
		if(selectionCsv!=null)tabs=VitimageUtils.readStringTabFromCsv(selectionCsv);
		if(!new File(dirOut).exists()) new File(dirOut).mkdirs();
		if(!new File(dirIn).exists()) {IJ.showMessage("Dir in does not exists : "+dirIn);return;}
		String[]tabNames1=new File(dirIn).list();
		ArrayList<String>ar=new ArrayList<String>();
		int N=0;
		int nExclud=0;
		for(String name1 : tabNames1) {
			String path1=new File(dirIn,name1).getAbsolutePath();
			String[]tabNames2=new File(path1).list();
			for(String name2 : tabNames2) {
				String path2=new File(path1,name2).getAbsolutePath();
				if(selectionCsv!=null) {
					String []infos=new String[] {name1.split("_")[0] , name1.split("_")[1] , name1.split("_")[2] , name2.split(".tif")[0]}; 
					int []indices=new int[] {             0          ,                  1  ,           2         ,          3         }; 
					String isOK=findInfoInStringTab(tabs, 4, infos, indices);
					if(isOK.contains("NON")) {
						System.out.println("Excluding "+name1.split("_")[0] +"_"+ name1.split("_")[1] +"_"+ name1.split("_")[2] +"_"+ name2.split(".tif")[0]);
						nExclud++;
						continue;
					}
				}
				
				ImagePlus img=IJ.openImage(path2);
				IJ.run(img,"8-bit","");
				String targetName=name1+"_"+name2;
				IJ.saveAsTiff(img, new File(dirOut,targetName).getAbsolutePath());
				//System.out.println(targetName);
				N++;
				ar.add(targetName);
			}
		}				
		ar.sort(null);
		String[][]tab=new String[494][];
		tab[0]=new String[] {"Genotype","Condition","Depth","Repetition"};
		for(int i=0;i<ar.size();i++) {
			tab[i+1]=ar.get(i).split(".tif")[0].split("_");
//			System.out.println( ar.get(i) );
		}
		//VitimageUtils.writeStringTabInCsv2(tab, "/home/rfernandez/Bureau/test.csv");
		System.out.println(N);
		System.out.println("Nexcluded="+nExclud);
	}


	
	//Take a dir with dirs1 with dirs2, collect names and aggregate
	public static void formatBunchOfImageTwoDepthDirsInSingleDir(String dirIn,String dirOut) {
		if(!new File(dirOut).exists()) new File(dirOut).mkdirs();
		if(!new File(dirIn).exists()) {IJ.showMessage("Dir in does not exists : "+dirIn);return;}
		String[]tabNames2=new File(dirIn).list();
		for(String name2 : tabNames2) {
			String path2=new File(dirIn,name2).getAbsolutePath();
			String[]tabNames3=new File(path2).list();
			for(String name3 : tabNames3) {
				String path3=new File(path2,name3).getAbsolutePath();
				String[]tabNames4=new File(path3).list();
				for(String name4 : tabNames4) {
					String path4=new File(path3,name4).getAbsolutePath();
					String finalNameImg=name2+"__"+name3+"__"+name4;
					ImagePlus img=IJ.openImage(path4);
					IJ.saveAsTiff(img, new File(dirOut,finalNameImg).getAbsolutePath());
				}
			}
		}
	}


}
