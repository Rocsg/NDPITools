package fr.cirad.image.sorghobff;

import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import fr.cirad.image.common.Timer;
import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.mlutils.SegmentationUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
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

	
	public static void copyFile(String source, String target) {
		Path tar = Paths.get(target);
	    Path sour =  Paths.get(source);
	    try {Files.copy(sour,tar, StandardCopyOption.REPLACE_EXISTING);} catch (IOException e) {e.printStackTrace();}
	}
	
	public static void fuck() {
		String dir="/home/rfernandez/Bureau/DATA/Racines/Data_BPMP/Second_dataset_2021_07/Data_Tidy/TRAINING_COUPLES_SPLIT/";
		for (String s : new String[] {"train","test","valid"}) {
			System.out.println("Processing "+dir+s+"/");
			for (String s2 : new File(dir+s).list()){
				if(s2.contains(".jpg")) {
					System.out.println("Processing "+dir+s+"/"+s2);
					ImagePlus img=IJ.openImage(dir+s+"/"+s2);
					IJ.run(img,"RGB Color","");
					IJ.saveAsTiff(img,(dir+s+"/"+s2).replace(".jpg",".tif"));
				}
			}
		}
	}
	
	public static void fuck2() {
		String dir="/media/rfernandez/DATA_RO_A/Roots_systems/Data_BPMP/Second_dataset_2021_07/Data_Tidy/IMG_RGB2";
		for (String s1 : new File(dir).list()) {
			for (String s2 : new File(dir,s1).list()) {
				for (String s3 : new File(dir,s1+"/"+s2).list()) {					
					System.out.println("Processing "+dir+"/"+s1+"/"+s2+"/"+s3);
					ImagePlus img=IJ.openImage(dir+"/"+s1+"/"+s2+"/"+s3);
					ImagePlus img2=VitimageUtils.compositeRGBByte(img, img, img, 1, 1, 1);
					IJ.saveAsTiff(img2,(dir+"/"+s1+"/"+s2+"/"+s3));
					
				}
			}
		}
	}
	
	
	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		fuck2();
		System.exit(0);
		String input="/home/rfernandez/Bureau/DATA/Racines/Data_BPMP/Data_Tidy/TRAINING_COUPLES";
		String input2="/home/rfernandez/Bureau/DATA/Racines/Data_BPMP/Data_Tidy/TRAINING_COUPLES_2";
		String output="/home/rfernandez/Bureau/DATA/Racines/Data_BPMP/Data_Tidy/REGISTERED_COUPLES";
		//String output2="/home/rfernandez/Bureau/DATA/Racines/Data_BPMP/Data_Tidy/REGISTERED_COUPLES_2";
		String[]tabIn=new File(input).list();
		String[]tabOut=new File(output).list();
		System.out.println(tabIn.length);
		System.out.println(tabOut.length);
		int iterIn=0;
		int iterOut=0;
		int iterCopy=0;
		for(int i=0;i<tabIn.length;i++)if(tabIn[i].contains(".rsml")) if(! new File(output,tabIn[i]).exists()) {
			System.out.println(""+(iterCopy++));
			String basename=tabIn[i].replace(".rsml","");
			copyFile(input+"/"+basename+".rsml",input2+"/"+basename+".rsml");
			copyFile(input+"/"+basename+".jpg",input2+"/"+basename+".jpg");
		}
//		IJ.openImage(input).show();
/*		String dir="/home/rfernandez/Bureau/DATA/Racines/Data_BPMP/Data_Tidy/TRAINING_COUPLES";
		String[]files=new File(dir).list();
		for(int i=0;i<files.length;i++) {
			String imgName=new File(dir,files[i]).getAbsolutePath();
			if(imgName.contains(".jpg")) {
				ImagePlus img=IJ.openImage(imgName);
				System.out.println(i+" "+imgName);
				VitimageUtils.printImageResume(img);
				if(img.getHeight()==1024)continue;
				//img.duplicate().show();
				ImagePlus img2=img.resize(1024, 1024, "bilinear");
				IJ.save(img2, imgName);
			}
		}*/
	}
	
	public void run(String arg) {
	//	this.test();
		//prepareExtractionOfVesselFromTestData();
		//extractVesselsFromTestData();
//		randomVesselsFromTestData();
//		getSegImagesCorrespondantToVessels();
		String test="/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Test/03_slice_seg/Segmentation/2016_G80_P4_E12.tif";
		ImagePlus img=IJ.openImage(test);
		img.show();
		ImagePlus img2=SegmentationUtils.dilation(img, 150, false);
		img2.show();
	}
	
	public ImagePlus getSegImagesCorrespondantToVessels(){
		String dirVess="/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Vessels_dataset/Selected_vessels/";
		String dirSlice="/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Slices_dataset/Full_vessels_source/";
		String dirImgHighRes="/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Slices_dataset/Full_segmentations/";
		String dirOutput="/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Insights_and_annotations/Vessels_dataset/Image_and_annotations_for_training/Vessel_init";
		for(int i=1;i<600;i++) {
			System.out.println(i);
			String img="vessel_"+i+".tif";
			String[][]data=VitimageUtils.readStringTabFromCsv(dirVess+"summary.csv");
			int num=Integer.parseInt(img.replace("vessel_","").replace(".tif",""));
			String name=data[num-1][1];
			System.out.println("Checking vessel "+num+" : "+name);
			String nameImg="Img"+name.split("Img")[1].replace(" ","");
			int num2=Integer.parseInt(name.split("_Img")[0].replace("V",""));
			System.out.println("|"+nameImg.replace(".tif", "")+"|");
			data=VitimageUtils.readStringTabFromCsv(dirSlice+nameImg.replace(".tif", "")+"/"+nameImg.replace(".tif", "")+".csv");
			String []dataVaiss=data[num2];
			int xBase=Integer.parseInt(dataVaiss[2].replace(" ", ""));
			int yBase=Integer.parseInt(dataVaiss[3].replace(" ", ""));
			ImagePlus seg=IJ.openImage(dirImgHighRes+"Segmentation_"+nameImg);
			System.out.println(dirImgHighRes+nameImg);
		    seg.setRoi(new Roi(new Rectangle(xBase,yBase,200,200)));
		    ImagePlus vais=seg.crop();
		    //vais.show();
		    ImagePlus test=IJ.openImage(dirVess+img);
		    //test.show();
		    IJ.saveAsTiff(vais, dirOutput+"/Segmentation_vessel_"+i+".tif");
	    	ImagePlus imgg=VitimageUtils.compositeOf(vais, test, ""+i);
	    	imgg.show();
//	    	VitimageUtils.waitFor(1000);
	    	imgg.close();
		}
	    
	    return null;
	}
	

	
	public void prepareExtractionOfVesselFromTestData() {
		System.out.println("Starting preparation of vessel test data");
		String baseDir=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Slices_dataset/";
		SegmentationUtils.jsonToBinarySlices(baseDir+"/Full",baseDir+"/FullTiff",baseDir+"/Full_segmentations",false); 
		SegmentationUtils.jsonToBinarySlices(baseDir+"/Full",baseDir+"/FullTiff",baseDir+"/Full_segmentations_subsampled",true); 
	}
	
	
	public void extractVesselsFromTestData() {
		String baseDir=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Slices_dataset/";
		ScriptMathieu.extractVessels(baseDir+"/FullTiff", baseDir+"/Full_segmentations",baseDir+"/Full_vessels_source",1); 
	}
	
	
	public void randomVesselsFromTestData() {
		int nTargetVessels=600;
		String inputDirSource=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Slices_dataset/Full_vessels_source";
		String inputDirBin=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Slices_dataset/Full_vessels_binary";
		String outputDir=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Vessels_dataset/Selected_vessels_source";
		String outputDirBin=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Vessels_dataset/Selected_vessels_binary";
		String outputDirJpg=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Vessels_dataset/Selected_vessels_jpg";
		String outputDirRo=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Vessels_dataset/Dir_Ro_VIA";
		String outputDirMat=VesselSegmentation.getVesselsDir()+"/Data/Insights_and_annotations/Vessels_dataset/Dir_Mat_VIA";
		String[]imgDirs=new File(inputDirSource).list();
		ArrayList<ImagePlus>imgList=new ArrayList<ImagePlus>();
		ArrayList<ImagePlus>imgListBin=new ArrayList<ImagePlus>();
		ArrayList<String[]>labels=new ArrayList<String[]>();
		labels.add(new String[] {"Slice","Source_vessel"});
		for(String dir : imgDirs) {
			String[]imgs=new File(inputDirSource,dir).list(ScriptMathieu.getFileNameFilterToExcludeCsvAndJsonFiles());
			String imgDirSource=new File(inputDirSource,dir).getAbsolutePath();
			String imgDirBin=new File(inputDirBin,dir).getAbsolutePath();
			for(String img : imgs) {
				
				ImagePlus im=IJ.openImage(new File(imgDirSource,img).getAbsolutePath());
				ImagePlus imBin=IJ.openImage(new File(imgDirBin,img).getAbsolutePath());
				im.getStack().setSliceLabel(img, 1);
				imgList.add(im);
				imBin.getStack().setSliceLabel(img, 1);
				imgListBin.add(imBin);
				labels.add(new String[] {""+(labels.size()+1),img});
			}
		}
		int nTot=imgList.size();
		int nSelected=0;
		ImagePlus[]tabImages=new ImagePlus[nTargetVessels];
		ImagePlus[]tabImagesBin=new ImagePlus[nTargetVessels];
		String[][]tabLabels=new String[nTargetVessels][2];
		Random rand=new Random(7);
		while(nSelected<nTargetVessels) {
			int sel=rand.nextInt(nTot);
			tabImages[nSelected]=imgList.get(sel).duplicate();
			tabImagesBin[nSelected]=imgListBin.get(sel).duplicate();
			IJ.saveAsTiff(tabImages[nSelected],new File(outputDir,"vessel_"+(nSelected+1)+".tif").getAbsolutePath());
			IJ.saveAsTiff(tabImagesBin[nSelected],new File(outputDirBin,"vessel_"+(nSelected+1)+".tif").getAbsolutePath());
			IJ.save(tabImages[nSelected],new File(outputDirJpg,"vessel_"+(nSelected+1)+".jpg").getAbsolutePath());
			if(nSelected<300) {
				IJ.save(tabImages[nSelected],new File(outputDirRo,"vessel_"+(nSelected+1)+".jpg").getAbsolutePath());				
			}
			else {
				IJ.save(tabImages[nSelected],new File(outputDirMat,"vessel_"+(nSelected+1)+".jpg").getAbsolutePath());				
			}
			tabLabels[nSelected]=new String[] {""+(nSelected+1),tabImages[nSelected].getStack().getSliceLabel(1)};
			System.out.println("Iter "+nSelected+" , selected "+sel+"/"+nTot+" = "+labels.get(sel)[0]+" - "+labels.get(sel)[1]);
			imgList.remove(sel);
			imgListBin.remove(sel);
			labels.remove(sel);
			nTot--;
			nSelected++;
		}
		VitimageUtils.writeStringTabInCsv(tabLabels, new File(outputDir,"summary.csv").getAbsolutePath());
		VitimageUtils.writeStringTabInCsv(tabLabels, new File(outputDirBin,"summary.csv").getAbsolutePath());
		ImagePlus img=VitimageUtils.slicesToStack(tabImages);
		ImagePlus imgBin=VitimageUtils.slicesToStack(tabImagesBin);
		IJ.saveAsTiff(img,new File(outputDir,"summary.tif").getAbsolutePath());
		IJ.saveAsTiff(imgBin,new File(outputDirBin,"summary.tif").getAbsolutePath());
	}
	
	public void test() {
		System.out.println("Start processing");
		SegmentationUtils.batchVesselSegmentation( VesselSegmentation.getVesselsDir(),"/home/rfernandez/Bureau/A_Test/Vaisseaux/In","/home/rfernandez/Bureau/A_Test/Vaisseaux/Out");
	}

}
