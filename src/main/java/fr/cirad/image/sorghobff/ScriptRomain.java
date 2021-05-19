package fr.cirad.image.sorghobff;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.mlutils.SegmentationUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.frame.PlugInFrame;
import weka.core.Debug.Random;

public class ScriptRomain extends PlugInFrame{
	private static final long serialVersionUID = 1L;
	
	public ScriptRomain() {
		super("");
	}
	
	public ScriptRomain(String title) {
		super(title);
	}

	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		new ScriptRomain().run("");
	}
	
	public void run(String arg) {
	//	this.test();
	//	prepareExtractionOfVesselFromTestData();
		//extractVesselsFromTestData();
		randomVesselsFromTestData();
	}
	

	
	public void prepareExtractionOfVesselFromTestData() {
		System.out.println("Starting preparation of vessel test data");
		String baseDir=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/";
		SegmentationUtils.jsonToBinarySlices(baseDir+"/Full",baseDir+"/FullTiff",baseDir+"/Full_segmentations_subsampled"); 
	}
	
	
	public void extractVesselsFromTestData() {
		String baseDir=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Full_dataset/";
		ScriptMathieu.extractVessels(baseDir+"/FullTiff", baseDir+"/Full_segmentations_subsampled",baseDir+"/Full_vessels"); 
	}
	
	public void randomVesselsFromTestData() {
		int nTargetVessels=800;
		String inputDir=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Vessels_dataset/Full_vessels";
		String outputDir=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Vessels_dataset/Selected_vessels";
		String[]imgDirs=new File(inputDir).list();
		ArrayList<ImagePlus>imgList=new ArrayList<ImagePlus>();
		ArrayList<String[]>labels=new ArrayList<String[]>();
		labels.add(new String[] {"Slice","Source_vessel"});
		for(String dir : imgDirs) {
			String[]imgs=new File(inputDir,dir).list(ScriptMathieu.getFileNameFilterToExcludeCsvAndJsonFiles());
			String imgDir=new File(inputDir,dir).getAbsolutePath();
			for(String img : imgs) {
				ImagePlus im=IJ.openImage(new File(imgDir,img).getAbsolutePath());
				im.getStack().setSliceLabel(img, 1);
				imgList.add(im);
				labels.add(new String[] {""+(labels.size()+1),img});
			}
		}
		int nTot=imgList.size();
		int nSelected=0;
		ImagePlus[]tabImages=new ImagePlus[nTargetVessels];
		String[][]tabLabels=new String[nTargetVessels][2];
		Random rand=new Random(7);
		while(nSelected<nTargetVessels) {
			int sel=rand.nextInt(nTot);
			tabImages[nSelected]=imgList.get(sel).duplicate();
			IJ.saveAsTiff(tabImages[nSelected],new File(outputDir,"vessel_"+(nSelected+1)+".tif").getAbsolutePath());
			tabLabels[nSelected]=new String[] {""+(nSelected+1),tabImages[nSelected].getStack().getSliceLabel(1)};
			System.out.println("Iter "+nSelected+" , selected "+sel+"/"+nTot+" = "+labels.get(sel)[0]+" - "+labels.get(sel)[1]);
			imgList.remove(sel);
			labels.remove(sel);
			nTot--;
			nSelected++;
		}
		VitimageUtils.writeStringTabInCsv(tabLabels, new File(outputDir,"summary.csv").getAbsolutePath());
		ImagePlus img=VitimageUtils.slicesToStack(tabImages);
		IJ.saveAsTiff(img,new File(outputDir,"summary.tif").getAbsolutePath());
	}
	
	public void test() {
		System.out.println("Start processing");
		SegmentationUtils.batchVesselSegmentation( VesselSegmentation.getVesselsDir(),"/home/rfernandez/Bureau/A_Test/Vaisseaux/In","/home/rfernandez/Bureau/A_Test/Vaisseaux/Out");
	}

}
