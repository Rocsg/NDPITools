package fr.cirad.image.test;

import java.io.File;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import io.github.rocsg.fijirelax.mrialgo.HyperMap;
import io.github.rocsg.fijirelax.mrialgo.MRUtils;
import io.github.rocsg.fijirelax.mrialgo.NoiseManagement;
import io.github.rocsg.fijiyama.common.VitimageUtils;

public class Test2 {
	public static void main(String []args) {
		ImageJ ij=new ImageJ();
		String path="/home/rfernandez/eclipse-workspace/MyIJPlugs/FijiRelax/src/test/resources/data/test_2";
		ImagePlus imgMap=IJ.openImage(new File(path,"TestDataHyperMap/smallSorghoSerieRaw.tif").getAbsolutePath());
		HyperMap map=new HyperMap(imgMap);
		
		int[]algTypes=new int[] {MRUtils.LM};
		boolean[]separateds=new boolean[] {false};
		NoiseManagement[]noises=new NoiseManagement[] {NoiseManagement.OFFSET};
		boolean[]forgets=new boolean[] {false};
		double[]nbStds=new double[] {5};
		for(int alg:algTypes)for(boolean separated:separateds)for(NoiseManagement noi: noises)for(boolean forget:forgets)for(double std:nbStds) {
		map.computeMapsAgainAndMask(alg, separated, noi, forget, null, std);
		}
	}
	
	
	
	
	/*exportToDicom();

		
		
		String path="/home/rfernandez/eclipse-workspace/MyIJPlugs/FijiRelax/src/test/resources/data/test_2";
		
		HyperMap hyp=HyperMap.importHyperMapFromRawDicomData(new File(path,"TestDataImportDicom").getAbsolutePath(),"testDicom");
		hyp.getEchoesImage().show();
	}
	*/
	public static void exportToDicom() {
		ImagePlus img=IJ.openImage("/home/rfernandez/eclipse-workspace/MyIJPlugs/FijiRelax/src/test/resources/data/test_2/TestDataHyperMap/smallSorghoSerieRaw.tif");
		String outputDir="/home/rfernandez/eclipse-workspace/MyIJPlugs/FijiRelax/src/test/resources/data/test_2/TestDataImportDicom/";
		HyperMap map=new HyperMap(img);
		map.getAsImagePlus().show();
		
		
		for(int c=0;c<img.getNChannels();c++) {
			ImagePlus imgSli=new Duplicator().run(img,c+1,c+1,1,1,1,1);
			String text=img.getStack().getSliceLabel(VitimageUtils.getCorrespondingSliceInHyperImage(img, c, 0, 0));
			int TR=(int)Double.parseDouble(text.split("TR=")[1].split("_TE=")[0]);
			int TE=(int)Double.parseDouble(text.split("TE=")[1].split("_SIGMARICE=")[0]);
			String completeTR="TR0"+(TR<10000 ? "0" : "")+(TR<1000 ? "0" : "")+TR;
			String completeTE="TE000"+(TE<100 ? "0" : "")+TE;
			File f=new File(outputDir+completeTR+"/"+completeTE);
			if(!f.exists())f.mkdirs();
			
			for(int z=0;z<img.getNSlices();z++) {
				imgSli=new Duplicator().run(img,c+1,c+1,1,1,z+1,z+1);
				IJ.saveAsTiff(imgSli,outputDir+completeTR+"/"+completeTE+"/slice0"+z+".tif");
			}
		}
	}		
	
	public static void exportToRaw() {
		ImagePlus img=IJ.openImage("/home/rfernandez/eclipse-workspace/MyIJPlugs/FijiRelax/src/test/resources/data/test_2/TestDataHyperMap/smallSorghoSerieRaw.tif");
		String outputDir="/home/rfernandez/eclipse-workspace/MyIJPlugs/FijiRelax/src/test/resources/data/test_2/TestDataImportRaw/";
		HyperMap map=new HyperMap(img);
		map.getAsImagePlus().show();
		
		
		for(int c=0;c<img.getNChannels();c++) {
			ImagePlus imgSli=new Duplicator().run(img,c+1,c+1,1,1,1,1);
			String text=img.getStack().getSliceLabel(VitimageUtils.getCorrespondingSliceInHyperImage(img, c, 0, 0));
			int TR=(int)Double.parseDouble(text.split("TR=")[1].split("_TE=")[0]);
			int TE=(int)Double.parseDouble(text.split("TE=")[1].split("_SIGMARICE=")[0]);
			String completeTR="TR0"+(TR<10000 ? "0" : "")+(TR<1000 ? "0" : "")+TR;
			String completeTE="TE000"+(TR<100 ? "0" : "")+TE;
			File f=new File(outputDir+completeTR+"/"+completeTE);
			
			imgSli=new Duplicator().run(img,c+1,c+1,1,1,1,1);
			IJ.saveAsTiff(imgSli,outputDir+"img_TR"+TR+"_TE"+TE+".tif");
		}
	}		
		
	
 }
