package fr.cirad.image.aerenchyme;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.BatchPredictor;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import fr.cirad.image.common.Timer;
import fr.cirad.image.common.VitimageUtils;

/* @author Gowtham Girithar Srirangasamy, adapted by Romain Fernandez*/
public class RandomForestCellClassification {
	/** plugin's name */
	static int Nfeatures=6;
	static boolean spectacular=false;
	public static void main(String[]args) throws Exception {
		
		ImageJ ij=new ImageJ();
		String dirExp="/home/rfernandez/Bureau/A_Test/Aerenchyme/Tests/PipeIJM";
//		FastRandomForest forest=trainAndApplyOnAerenchymeData(dirExp);
		//test();
		computeSummaryOfLacuneData("/home/rfernandez/Bureau/A_Test/Aerenchyme/Sylvie_Et_Justin_Work","/home/rfernandez/Bureau/A_Test/Aerenchyme/Sylvie_Et_Justin_Work/Results_aerenchyme_measurements.csv");
	}

	public static void test() {
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/A_Test/Aerenchyme/Sylvie_Et_Justin_Work/Source/APO_ctrl_2_2.tif");
		IJ.open("/home/rfernandez/Bureau/A_Test/Aerenchyme/Sylvie_Et_Justin_Work/CellRoi/APO_ctrl_2_2.tif.zip");
		RoiManager rm=RoiManager.getInstance();
		rm.select(1);
	}
	
	public static void computeSummaryOfLacuneData(String dirExp,String pathToOutputCsv) {
		String dirSource=new File(dirExp,"Source").getAbsolutePath();
		String dirRoi=new File(dirExp,"CellRoi").getAbsolutePath();
		String dirCortex=new File(dirExp,"CortexRoi").getAbsolutePath();
		String dirLac=new File(dirExp,"LacunesIndices").getAbsolutePath();
		String[]imgNames=new File(dirSource).list();
		ArrayList<String>ar=new ArrayList<String>();
		for(String s:imgNames)ar.add(s);
		ar.sort(null);
		int N=ar.size();
		String[][]finalTab=new String[N+1][4];
		finalTab[0]=new String[] {"0_PARAM_ImgName","1_PARAM_INFO","2_PARAM_Geno","3_PARAM_Management","4_PARAM_Depth","5_PARAM_Repetition","6_PARAM_Magnification","7_PHENE_cortex_surf(pix)","8_PHENE_stele_surf(pix)","9_PHENE_lacune_surf(pix)","10_PHENE_lacune_ratio(percent)"};
		Timer t=new Timer();
		for(int i=0;i<N;i++) {
			String[]tab=new String[11];
			tab[0]=ar.get(i);
			String baseName=ar.get(i).split(".tif")[0];
			t.print("Processing "+baseName+" ="+i+" / "+N);
			if(!new File(dirLac,baseName+".tif.csv").exists()) {
				if(baseName.split("_")[0].contains("SAHEL")||baseName.split("_")[0].contains("IRAT")||baseName.split("_")[0].contains("KINA")) {
					tab[1]="Genotype was not selected";
				}
				else  tab[1]="Lacune annotations were not provided";
				tab[2]=baseName.split("_")[0];
				tab[3]=baseName.split("_")[1];
				tab[4]=baseName.split("_")[2];
				tab[5]=baseName.split("_")[3];
				tab[6]=""+getMagnification(dirSource+"/"+baseName+".tif");
				for(int iii=7;iii<11;iii++)tab[iii]="NA";
				finalTab[i+1]=tab;
				continue;
			}
			tab[1]="No info";
			tab[2]=baseName.split("_")[0];
			tab[3]=baseName.split("_")[1];
			tab[4]=baseName.split("_")[2];
			tab[5]=baseName.split("_")[3];
			tab[6]=""+getMagnification(dirSource+"/"+baseName+".tif");
			

			double totCortex=0;
			double totLacune=0;
			
			ImagePlus img=IJ.openImage(dirSource+"/"+baseName+".tif");			
			IJ.open(dirRoi+"/"+baseName+".tif.zip");
			RoiManager rm=RoiManager.getInstance();
			int nR=rm.getCount();

			boolean []isLacune=new boolean[nR];
			String[][]tabLac=VitimageUtils.readStringTabFromCsv(dirLac+"/"+baseName+".tif.csv");
			for(int l=1;l<tabLac.length;l++) {
				int index=Integer.parseInt(tabLac[l][1])-1;
				if(index>=isLacune.length) {
					System.out.println("Alerte : "+index);
				}
				else isLacune[index]=true;
			}
			for(int r=0;r<nR;r++) {
				double surf=VitimageUtils.getRoiSurface(rm.getRoi(r));
				totCortex+=surf;
				if(isLacune[r])totLacune+=surf;
				//System.out.println("After roi "+r+" totCortex="+totCortex+" lacune="+totLacune);
			}

			
			rm.reset();
			IJ.open(dirCortex+"/"+baseName+".tifcortex_in.zip");
			double surfIn=VitimageUtils.getRoiSurface(rm.getRoi(0));
			IJ.open(dirCortex+"/"+baseName+".tifcortex_out.zip");
			//double surfOut=VitimageUtils.getRoiSurface(rm.getRoi(1));
			tab[7]=""+(totCortex);
			tab[8]=""+(surfIn);
			tab[9]=""+totLacune;
			tab[10]=""+VitimageUtils.dou(100*totLacune/(totCortex));
			finalTab[i+1]=tab;
			rm.reset();
		
		}
		VitimageUtils.writeStringTabInCsv2(finalTab, pathToOutputCsv);
	}
	
    public static void computeAerenchymeRatio(String dirExp,String[]imgNamesFull,String outputCsvPath) {
    	int N=imgNamesFull.length;
    	String[]imgNames=new String[N];
    	for(int i=0;i<N;i++)imgNames[i]=new File(imgNamesFull[i]).getName().split(".csv")[0];
    	String[][]tab=new String[N+1][4];
    	tab[0]=new String[] {"Image","Total cortex surface (pix)","Lacunes (pix)", "Ratio"};
    	for(int n=0;n<N;n++) {
    		tab[n+1][0]=imgNames[n];
    		System.out.println(imgNames[n]);
			String pathToInitCSV=new File(dirExp+"/CellMeasurements"+"/"+imgNames[n]+".csv").getAbsolutePath();
			String pathToInitUpgradedCSV=new File(dirExp+"/CellUpgradedMeasurements"+"/"+imgNames[n]+".csv").getAbsolutePath();
			String[][]tabImg=VitimageUtils.readStringTabFromCsv(pathToInitCSV);
			String[][]tabUpImg=VitimageUtils.readStringTabFromCsv(pathToInitUpgradedCSV);
			int R=tabImg.length-1;
			double totSurf=0;
			double aerSurf=0;
			for(int r=0;r<R;r++) {
				double area=Double.parseDouble(tabImg[r+1][2]);
				int classe=Integer.parseInt(tabUpImg[r+1][Nfeatures]);
				totSurf+=area;
				if(classe==1)aerSurf+=area;
			}
			tab[n+1][1]=""+totSurf;
			tab[n+1][2]=""+aerSurf;
			tab[n+1][3]=""+VitimageUtils.dou(aerSurf*100.0/totSurf);
    	}
    	VitimageUtils.writeStringTabInCsv2(tab, outputCsvPath);
    }
	     
	public static void setPredictionToCSV(double[]preds,String inputPath,String outputPath) {
		String[][]tab=VitimageUtils.readStringTabFromCsv(inputPath);
		int N=tab.length;
		for(int i=1;i<N;i++)tab[i][Nfeatures]=""+(int)Math.round(preds[i-1]);
		VitimageUtils.writeStringTabInCsv2(tab, outputPath);
	}

	public static FastRandomForest trainAndApplyOnAerenchymeData(String dirExp) throws Exception {
		//Retrieve the list of investigated images in LacunesIndices
		String dirLac=new File(dirExp+"/LacunesIndices").getAbsolutePath();
		String []imgNames=new File(dirLac).list();
		int N=imgNames.length;
		//Split train/valid/test
	
		ArrayList<String>listTrain=new ArrayList<String>();
		ArrayList<String>listValid=new ArrayList<String>();
		ArrayList<String>listTest=new ArrayList<String>();
		String pathToTrainCsv=new File(dirExp,"Assemble/CsvTrain.csv").getAbsolutePath();
		String pathToTrainCsvAug=new File(dirExp,"Assemble/CsvTrainAug.csv").getAbsolutePath();
		String pathToValidCsv=new File(dirExp,"Assemble/CsvValid.csv").getAbsolutePath();
		String pathToTestCsv=new File(dirExp,"Assemble/CsvTest.csv").getAbsolutePath();
		String pathToTrainRatioExpected=new File(dirExp,"Assemble/RatioTrainExpected.csv").getAbsolutePath();
		String pathToTrainRatioPredicted=new File(dirExp,"Assemble/RatioTrainPredicted.csv").getAbsolutePath();
		String pathToValidRatioExpected=new File(dirExp,"Assemble/RatioValidExpected.csv").getAbsolutePath();
		String pathToValidRatioPredicted=new File(dirExp,"Assemble/RatioValidPredicted.csv").getAbsolutePath();
		String pathToTestRatioExpected=new File(dirExp,"Assemble/RatioTestExpected.csv").getAbsolutePath();
		String pathToTestRatioPredicted=new File(dirExp,"Assemble/RatioTestPredicted.csv").getAbsolutePath();
		
		Random rand=new Random(0);
		for(int i=0;i<N;i++) {
			//Upgrade CSV adding other computations, a "actual class" and "predicted class" fields
			String pathToLacuneIndices=new File(dirExp+"/LacunesIndices"+"/"+imgNames[i]).getAbsolutePath();
			String pathToInitCSV=new File(dirExp+"/CellMeasurements"+"/"+imgNames[i]).getAbsolutePath();
			String pathToOutputCSV=new File(dirExp+"/CellUpgradedMeasurements"+"/"+imgNames[i]).getAbsolutePath();
			double magnification=getMagnification(dirExp+"/Source"+"/"+imgNames[i].split(".csv")[0]);
			upgradeCSV(pathToInitCSV,pathToOutputCSV,pathToLacuneIndices,magnification);

			//attribute to one among train/valid/test
			double d=rand.nextDouble();
			if(d<0.5)      listTrain.add(pathToOutputCSV);
			else if(d<0.75)listValid.add(pathToOutputCSV);
			else           listTest.add(pathToOutputCSV);				
		}
		
		
		computeAerenchymeRatio(dirExp,listTrain.toArray(new String[listTrain.size()]),pathToTrainRatioExpected);
		computeAerenchymeRatio(dirExp,listValid.toArray(new String[listValid.size()]),pathToValidRatioExpected);
		computeAerenchymeRatio(dirExp,listTest.toArray(new String[listTest.size()]),pathToTestRatioExpected);
		
		//Assemble multiples CSV into a single one and 
		assembleCSV(listTrain.toArray(new String[listTrain.size()]),pathToTrainCsv,false);
		assembleCSV(listValid.toArray(new String[listValid.size()]),pathToValidCsv,false);
		assembleCSV(listTest.toArray(new String[listTest.size()]),pathToTestCsv,false);

					
		//Make data augmentation and import as instances
		augmentCSVOfAerenchymesFeatures(pathToTrainCsv,pathToTrainCsvAug,50);
		Instances trainInstances=buildInstancesDataSetFromCSV(pathToTrainCsvAug);
		Instances validInstances=buildInstancesDataSetFromCSV(pathToValidCsv);
		Instances testInstances=buildInstancesDataSetFromCSV(pathToTestCsv);
		
		
		//Train a RandomForest with it
		FastRandomForest forest=new FastRandomForest();
		forest.setComputeImportances(true);
		forest.setNumTrees(200);
		forest.setNumFeatures(4); //4 is optimum
		forest.buildClassifier(trainInstances);
		Evaluation evalTree = new Evaluation(trainInstances);
		evalTree.evaluateModel(forest, validInstances);
		
		
		
		/* Print the results summary */
		System.out.println("**\n\n\n\n\n Decision Trees Evaluation with Datasets **");
		System.out.println(evalTree.toSummaryString());
		System.out.print(" the expression for the input data as per alogorithm is ");
		System.out.println(forest);
		System.out.println(evalTree.toMatrixString());
		System.out.println(evalTree.toClassDetailsString());
		applyOnAerenchymeData(forest,dirExp);
		computeAerenchymeRatio(dirExp,listTrain.toArray(new String[listTrain.size()]),pathToTrainRatioPredicted);
		computeAerenchymeRatio(dirExp,listValid.toArray(new String[listValid.size()]),pathToValidRatioPredicted);
		computeAerenchymeRatio(dirExp,listTest.toArray(new String[listTest.size()]),pathToTestRatioPredicted);
		
		return forest;
}
	
	public static void applyOnAerenchymeData(FastRandomForest forest,String dirExp) throws Exception {
		//Retrieve the list of investigated images in LacunesIndices
		String dirSource=new File(dirExp+"/Source").getAbsolutePath();
		String []imgNames=new File(dirSource).list();
		String pathToTrainCsv=new File(dirExp,"Assemble/CsvTrain.csv").getAbsolutePath();
		Instances trainInstances=buildInstancesDataSetFromCSV(pathToTrainCsv);
		Evaluation evalTree = new Evaluation(trainInstances);

		int N=imgNames.length;
		for(int i=1;i<N;i++) {
			System.out.println("Processing "+imgNames[i]);
			String pathToInitCSV=new File(dirExp+"/CellMeasurements"+"/"+imgNames[i]+".csv").getAbsolutePath();
			String pathToOutputCSV=new File(dirExp+"/CellUpgradedMeasurements"+"/"+imgNames[i]+".csv").getAbsolutePath();
			double magnification=getMagnification(dirExp+"/Source"+"/"+imgNames[i]);
			upgradeCSV(pathToInitCSV,pathToOutputCSV,null,magnification);
			Instances testInstances=buildInstancesDataSetFromCSV(pathToOutputCSV);			
			double[]preds=evalTree.evaluateModel(forest,testInstances);
			setPredictionToCSV(preds,pathToOutputCSV,pathToOutputCSV);
			ImagePlus img=IJ.openImage(new File(dirExp+"/"+"Source"+"/"+imgNames[i]).getAbsolutePath());
			IJ.run(img, "Enhance Contrast", "saturated=0.35");
			IJ.openImage(new File(dirExp+"/"+"CellRoi"+"/"+imgNames[i]+".zip").getAbsolutePath());
			img.show();
			int R=preds.length;
			RoiManager rm=RoiManager.getRoiManager();
			for(int r=0;r<R;r++) {
				rm.select(r);
				if(spectacular)VitimageUtils.waitFor(100);
				if(preds[r]==1) {
					IJ.run(img, "Invert", "");					
				}
			}
			IJ.saveAsTiff(img, new File(dirExp+"/"+"ResultImages"+"/"+imgNames[i]).getAbsolutePath());
			rm.reset();
			img.close();
		}
		String pathToAllRatioPredicted=new File(dirExp,"Assemble/AllRatioPredicted.csv").getAbsolutePath();
		computeAerenchymeRatio(dirExp,imgNames,pathToAllRatioPredicted);

	}
	
	
	
	
	public static void augmentCSVOfAerenchymesFeatures(String pathSource,String pathTarget,double percentAug) {
		String[][]tabIn=VitimageUtils.readStringTabFromCsv(pathSource);
		int nC=tabIn[0].length;
		int nL=tabIn.length-1;
		Random rand=new Random(0);
		int tot=nL;
		int n0=0;for(int i=0;i<nL;i++)if(tabIn[1+i][Nfeatures].equals("0"))n0++;
		System.out.println("Augmenting : tot="+tot+" , 0="+n0);
		int Nrep=(int)Math.ceil(n0*1.0/(tot-n0) );
		System.out.println("Factor="+Nrep);
		ArrayList<String[]>listOut=new ArrayList<String[]>();
		listOut.add(tabIn[0]);
		for(int l=0;l<nL;l++) {
			listOut.add(tabIn[l+1]);
			if(tabIn[l+1][Nfeatures].equals("1")) {
				for(int r=0;r<Nrep;r++) {
					int ran=rand.nextInt()%81;
					listOut.add(randomChangeInStringTabOfNumericalValues(tabIn[1+l],percentAug,0,Nfeatures-1,ran));
				}
			}
		}
		VitimageUtils.writeStringTabInCsv2(listOut.toArray(new String[listOut.size()][listOut.get(0).length]), pathTarget);		
	}
	
	public static void upgradeCSV(String pathToInitCSV,String pathToOutputCSV,String pathToLacuneIndices,double magnification) {
		String[][]initCsvStringTab=VitimageUtils.readStringTabFromCsv(pathToInitCSV);
		String[][]lacuneCsvStringTab=null;
		if(pathToLacuneIndices!=null)lacuneCsvStringTab=VitimageUtils.readStringTabFromCsv(pathToLacuneIndices);
		int N=initCsvStringTab.length;
			//0=area [2]
			//1=Rho [comp 3 et 4]
			//2=Major [G]
			//3=Minor [H]
			//4=Circ [J]
			//5=Feret[K]
			//6=AR[P]
			//7=Round[Q]
			//8=Solidity[R]
			//9=sqrt(area)/Rho
			//(10=AxeRadial/AxeTangentiel)
		String[][]outputCsvStringTab=new String[initCsvStringTab.length][Nfeatures+1];
		for(int i=0;i<N;i++) {
			outputCsvStringTab[i][0]=initCsvStringTab[i][9];//Circularity
			outputCsvStringTab[i][1]=initCsvStringTab[i][10];//Feret length
			outputCsvStringTab[i][2]=initCsvStringTab[i][15];//AR
			outputCsvStringTab[i][3]=initCsvStringTab[i][16];//Roundness
			outputCsvStringTab[i][4]=initCsvStringTab[i][17];//Solidity
			if(i==0) {
				outputCsvStringTab[i][Nfeatures]="Class";
				outputCsvStringTab[i][Nfeatures-1]="DeltAngleRadial";
			}
			else {
				outputCsvStringTab[i][1]=""+20.0/magnification*Double.parseDouble(initCsvStringTab[i][10]);//Feret length
				double angleEllipsis=Double.parseDouble(initCsvStringTab[i][8]);
				double dx=Double.parseDouble(initCsvStringTab[i][18]);
				double dy=Double.parseDouble(initCsvStringTab[i][19]);
				double rmaj=Double.parseDouble(initCsvStringTab[i][6]);
				double rmin=Double.parseDouble(initCsvStringTab[i][7]);
				double angleRadial=angleFromDxAndDy(dx,dy);
				double deltaAngle=diffAngle(angleEllipsis, angleRadial);
				double deltaTeta=Math.PI*deltaAngle/180;
				double ratio=((Math.cos(deltaTeta)*rmaj+Math.sin(deltaTeta)*rmin)/(Math.sin(deltaTeta)*rmaj+Math.cos(deltaTeta)*rmin));
/*				System.out.println("Rmaj="+rmaj);
				System.out.println("Rmin="+rmin);
				System.out.println("deltaAngle="+deltaAngle);
				System.out.println("Ratio="+ratio);
				System.exit(0);
	*/			outputCsvStringTab[i][Nfeatures-1]=""+ratio;
				//outputCsvStringTab[i][1]=""+Double.parseDouble(outputCsvStringTab[i][1])/Math.sqrt(dx*dx+dy*dy);
				if(pathToLacuneIndices!=null) {
					//Look for the eventual presence of this element in the lacune tab to assess the target class
					boolean found=false;
					for(int ii=1;ii<lacuneCsvStringTab.length;ii++)if( (Integer.parseInt(lacuneCsvStringTab[ii][1])) == i)found=true;
					outputCsvStringTab[i][Nfeatures]=""+( found  ? 1 : 0);
				}
				else outputCsvStringTab[i][Nfeatures]=""+(i%2);
			}
		}
		VitimageUtils.writeStringTabInCsv2(outputCsvStringTab, pathToOutputCSV);
	}

	public static Instances buildInstancesDataSetFromCSV(String fileName) throws Exception {
		CSVLoader cload=new weka.core.converters.CSVLoader();
		cload.setFile(new File(fileName));
		cload.setSource(new FileInputStream(new File(fileName)));
		Instances dataSet = cload.getDataSet();
		weka.filters.unsupervised.attribute.NumericToNominal nom=new weka.filters.unsupervised.attribute.NumericToNominal();
		nom.setInputFormat(dataSet);
		nom.setAttributeIndices("last");
		dataSet=weka.filters.unsupervised.attribute.NumericToNominal.useFilter(dataSet, nom);
		dataSet.setClassIndex(dataSet.numAttributes() -1);
		return dataSet;
	}

	
	
	
	
	
	public static void assembleCSV(String[]pathToCsvIn,String pathToCsvOut,boolean debug) {
		int N=pathToCsvIn.length;
		String[][][]tabsIn=new String[N][][];
		int totN=1;
		for(int i=0;i<N;i++) {
			if(debug)System.out.println(i+" / "+N);
			tabsIn[i]=VitimageUtils.readStringTabFromCsv(pathToCsvIn[i]);
			if(debug)System.out.println("Adding "+(tabsIn[i].length-1)+" lines from "+pathToCsvIn[i]);
			totN+=tabsIn[i].length-1;
		}
		String[][]tabOut=new String[totN][tabsIn[0][0].length];
		tabOut[0]=tabsIn[0][0];
		int incr=1;
		for(int i=0;i<N;i++) {
			int L=tabsIn[i].length;
			for(int l=1;l<L;l++) {
				tabOut[incr++]=tabsIn[i][l];
			}
		}
		VitimageUtils.writeStringTabInCsv2(tabOut, pathToCsvOut);
	}
		
	public static String[]randomChangeInStringTabOfNumericalValues(String[]tabIn,double percentAug,int firstIndexModif,int lastIndexModif,int seed){
		Random rand= new Random(seed);
		String []tabOut=new String[tabIn.length];
		for(int i=0;i<firstIndexModif;i++) tabOut[i]=tabIn[i];
		for(int i=firstIndexModif;i<=lastIndexModif;i++) {
			double d=Double.parseDouble(tabIn[i]);
			double var=rand.nextGaussian()*percentAug*0.01+1;
			tabOut[i]=""+d*var;
		}
		for(int i=lastIndexModif+1;i<tabIn.length;i++) tabOut[i]=tabIn[i];
		return tabOut;
	}
	
	//Return an angle between 0 (indicating east) and 180 (indicating west), towards 90 (indicating north)
	public static double angleFromDxAndDy(double dx,double dy) {
		if(dx==0)return 90;
		if(dy<0)return angleFromDxAndDy(-dx, -dy);
		if(dx<0)return 180-angleFromDxAndDy(-dx, dy);
		return Math.atan(dy/dx)*180/Math.PI;
	}
	
	public static double diffAngle(double angle1,double angle2) {
		if(Math.abs(angle1-angle2)<90)return Math.abs(angle1-angle2);
		if(angle1>angle2)angle1-=180;
		else angle2-=180;
		return Math.abs(angle1-angle2);		
	}
		
	public static double getMagnification(String path) {
		ImagePlus img=IJ.openImage(path);
		double d=0;
		if(img.getInfoProperty().split("Objective Correction").length>1) {
			d=Double.parseDouble( img.getInfoProperty().split("Objective Correction")[1].split("NominalMagnification")[1].split("\"")[1]);
			System.out.println("Detected : "+d);
			return d;
		}
		else d=20;
		System.out.println("Guessed : "+d);
		return d;
	}
		
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
