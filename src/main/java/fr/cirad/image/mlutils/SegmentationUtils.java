package fr.cirad.image.mlutils;

import java.awt.List;
import java.awt.Point;
import java.awt.Polygon;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Random;

import ij.plugin.ChannelSplitter;

import org.json.JSONArray;
import org.json.JSONObject;


import fr.cirad.image.common.Timer;
import fr.cirad.image.common.VitiDialogs;
import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.hyperweka.HyperWekaSegmentation;
import fr.cirad.image.registration.ItkTransform;

import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import inra.ijpb.watershed.MarkerControlledWatershedTransform2D;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;

import fr.cirad.image.common.VitimageUtils;

public class SegmentationUtils {
    
	
	public static void testReadCsv() {
		String[][]tab=VitimageUtils.readStringTabFromCsv("/home/rfernandez/Téléchargements/Recap_echantillons.xlsx - Image_inventory_2019_T1_WW.csv");
		for(int i=2;i<tab.length;i++) {//From i=2 because two first lines are not images
			//For each line
			System.out.println("Line "+i+" =");
			for(int j=0;j<tab[i].length;j++) {
				System.out.print(tab[i][j]+" , ");//Display one element, without \n (endline)
			}
			System.out.println();//End of this line
			System.out.print("Is rejected ? ");
			if(tab[i][11].equals("")) {
				System.out.println("No");
			}
			else System.out.println("Yes");
			System.out.print("Total des defauts = ");
			System.out.println(  (Integer.parseInt(tab[i][7])+Integer.parseInt(tab[i][8])+Integer.parseInt(tab[i][9]) )  );
			System.out.println();//End of this line
			System.out.println();//End of this line
		}
	}
	
	
	
	
	
	/** Helpers for visualization of Roi and mask over source images --------------------------------------------*/
	public static ImagePlus visualizeMaskEffectOnSourceData(ImagePlus imgSourceRGB,ImagePlus mask,int mode0VBOnly_1Enhance_2GreysOther_3greenout) {
		ImagePlus[]imgSource=VitimageUtils.channelSplitter(imgSourceRGB);
	

		if(mode0VBOnly_1Enhance_2GreysOther_3greenout==0) {
			ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
			imgMask=VitimageUtils.makeOperationOnOneImage(imgMask, 2, 0.5, false);
			imgMask=VitimageUtils.makeOperationOnOneImage(imgMask, 1, 0.5, false);
			for(int can=0;can<3;can++)imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
			return VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);				
		}
		else if(mode0VBOnly_1Enhance_2GreysOther_3greenout==2) {
			ImagePlus[]ret=new ImagePlus[3];
			ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
			IJ.run(imgSourceRGBGrey,"8-bit","");
			IJ.run(imgSourceRGBGrey,"RGB Color","");
			ImagePlus[]imgSourceGreys=VitimageUtils.channelSplitter(imgSourceRGBGrey);
			ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
			ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(imgMask);
			imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
			imgMaskGreys=VitimageUtils.makeOperationOnOneImage(imgMaskGreys, 2, 1.7, true);
			for(int can=0;can<3;can++) {
				imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
				imgSourceGreys[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceGreys[can], imgMaskGreys, 2, false);
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can],imgSourceGreys[can],1,false);
			}
//			ImagePlus deb1=VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);
//			ImagePlus deb2=VitimageUtils.compositeRGBByte(imgSourceGreys[0], imgSourceGreys[1], imgSourceGreys[2],1,1,1);
			return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
		}
		
		else if(mode0VBOnly_1Enhance_2GreysOther_3greenout==3) {
			ImagePlus[]ret=new ImagePlus[3];
			ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
			IJ.run(imgSourceRGBGrey,"8-bit","");
			IJ.run(imgSourceRGBGrey,"RGB Color","");
			ImagePlus[]imgSourceGreys=VitimageUtils.channelSplitter(imgSourceRGBGrey);
			ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
			IJ.run(imgMask,"32-bit","");
			ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(imgMask);
			imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
			IJ.run(imgMaskGreys,"32-bit","");
			imgMaskGreys=VitimageUtils.makeOperationOnOneImage(imgMaskGreys, 2, 1.7, true);
			for(int can=0;can<3;can++) {
				imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
				imgSource[can]=VitimageUtils.makeOperationOnOneImage(imgSource[can], 2, 1.2, true);
				imgSourceGreys[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceGreys[can], imgMaskGreys, 2, false);
				imgSourceGreys[can]=VitimageUtils.makeOperationOnOneImage(imgSourceGreys[can], 2, (can==1) ? 0.85 : 0.7, true);
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can],imgSourceGreys[can],1,false);
			}
//			ImagePlus deb1=VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);
//			ImagePlus deb2=VitimageUtils.compositeRGBByte(imgSourceGreys[0], imgSourceGreys[1], imgSourceGreys[2],1,1,1);
			return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
		}
		else  {
			ImagePlus imgSourceRGB2=imgSourceRGB.duplicate();
			ImagePlus[]imgSource2=VitimageUtils.channelSplitter(imgSourceRGB2);
			ImagePlus imgMask=getBinaryMaskUnary(mask, 0.5);
			IJ.run(imgMask,"32-bit","");
			imgMask=VitimageUtils.makeOperationOnOneImage(imgMask, 2, 1.4, false);

			ImagePlus imgMask2=VitimageUtils.invertBinaryMask(mask);
			imgMask2=getBinaryMaskUnary(imgMask2, 0.5);
			IJ.run(imgMask2,"32-bit","");
			imgMask2=VitimageUtils.makeOperationOnOneImage(imgMask2, 2, 0.4, false);

			for(int can=0;can<3;can++) {
				imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can], imgMask, 2, false);
				imgSource2[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource2[can], imgMask2, 2, false);

				imgSource[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSource[can],imgSource2[can],1,false);
			}
			return VitimageUtils.compositeRGBByte(imgSource[0], imgSource[1], imgSource[2],1,1,1);				
		}
	}

	public static ImagePlus visualizeMaskDifferenceOnSourceData(ImagePlus imgSourceRGB,ImagePlus maskRef,ImagePlus maskVal) {
		ImagePlus[]imgSourceRef=VitimageUtils.channelSplitter(imgSourceRGB);
		ImagePlus[]imgSourceVal=VitimageUtils.channelSplitter(imgSourceRGB);
		ImagePlus[]imgSourceBoth=VitimageUtils.channelSplitter(imgSourceRGB);
		ImagePlus[]ret=new ImagePlus[3];
		ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
		IJ.run(imgSourceRGBGrey,"8-bit","");
		IJ.run(imgSourceRGBGrey,"RGB Color","");
		ImagePlus[]imgSourceGreys=VitimageUtils.channelSplitter(imgSourceRGBGrey);

		ImagePlus mRefAndVal=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 2);
		ImagePlus imgMaskRefAndVal=getBinaryMaskUnary(mRefAndVal, 0.5);
		
		ImagePlus mRefOnly=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 4);
		ImagePlus imgMaskRefOnly=getBinaryMaskUnary(mRefOnly, 0.5);

		ImagePlus mValOnly=VitimageUtils.binaryOperationBetweenTwoImages(maskVal, maskRef, 4);
		ImagePlus imgMaskValOnly=getBinaryMaskUnary(mValOnly, 0.5);

		ImagePlus mRefOrVal=VitimageUtils.binaryOperationBetweenTwoImages(maskRef, maskVal, 1);			
		ImagePlus imgMaskGreys=VitimageUtils.invertBinaryMask(mRefOrVal);
		imgMaskGreys=getBinaryMaskUnary(imgMaskGreys, 0.5);
		
		//la
		for(int can=0;can<3;can++) {
			imgSourceBoth[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceBoth[can], imgMaskRefAndVal, 2, false);
			imgSourceBoth[can]=VitimageUtils.makeOperationOnOneImage(imgSourceBoth[can], 2, 1.4, true);
			
			imgSourceGreys[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceGreys[can], imgMaskGreys, 2, false);
			imgSourceGreys[can]=VitimageUtils.makeOperationOnOneImage(imgSourceGreys[can], 2, (can==1) ? 0.5 : 0.5, true);

			imgSourceRef[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceRef[can], imgMaskRefOnly, 2, false);
			imgSourceRef[can]=VitimageUtils.makeOperationOnOneImage(imgSourceRef[can], 2, (can!=2) ? 0.7 : 1, true);
			imgSourceRef[can]=VitimageUtils.makeOperationOnOneImage(imgSourceRef[can], 1, (can!=2) ? 160 : 0, true);
			imgSourceRef[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceRef[can], imgMaskRefOnly, 2, false);

			imgSourceVal[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceVal[can], imgMaskValOnly, 2, false);
			imgSourceVal[can]=VitimageUtils.makeOperationOnOneImage(imgSourceVal[can], 2, (can==1) ? 0.7 : 1, true);
			imgSourceVal[can]=VitimageUtils.makeOperationOnOneImage(imgSourceVal[can], 1, (can==1) ? 160 : 0, true);
			imgSourceVal[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceVal[can], imgMaskValOnly, 2, false);

			
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceBoth[can],imgSourceGreys[can],1,false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can],imgSourceRef[can],1,false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can],imgSourceVal[can],1,false);
		}
		return VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
	}

	public static ImagePlus getSizeMap(ImagePlus img,int threshLow,int step,int nCat) {

		ImagePlus imgRet=VitimageUtils.nullImage(img);
		IJ.run(imgRet,"8-bit","");
		imgRet.setDisplayRange(0, nCat*step);
		IJ.run(imgRet,"Fire","");
		for(int z=0;z<imgRet.getNSlices();z++) {
			ImagePlus temp=new Duplicator().run(img,1,1,z+1,z+1,1,1);
			IJ.run(temp,"8-bit","");
			Roi[]roiTab=segmentationToRoi(temp);
			ImageProcessor ip=imgRet.getStack().getProcessor(z+1);
			for(Roi r : roiTab) {
				int index=(int)Math.floor(getRoiSurface(r))/step+1;
				if(index>nCat)index=nCat;
				ip.setValue(index*step);
				ip.fill(r);
			}
			imgRet.getStack().setProcessor(ip, z+1);
		}
		//VitimageUtils.showWithParams(imgRet,img.getTitle()+"_size_map",1,0,nCat*step);
/*			imgRet.show();
		imgRet.setDisplayRange(0, nCat*step);
		imgRet.updateAndDraw();
		IJ.run(imgRet,"Fire","");
		IJ.run("Brightness/Contrast...");
		selectWindow("B&C");
		*/
		//if(!show)imgRet.hide();
		return imgRet;
	}
           

	


	/** Comparison of segmentations and computation of similarity scores --------------------------------------------*/    
    public static double IOU(ImagePlus imgRef,ImagePlus imgVal) {
    	ImagePlus ref=VitimageUtils.getBinaryMask(imgRef, 0.5);
    	ImagePlus val=VitimageUtils.getBinaryMask(imgVal, 0.5);
    	ImagePlus imgOR=VitimageUtils.binaryOperationBetweenTwoImages(ref, val, 1);
    	ImagePlus imgAND=VitimageUtils.binaryOperationBetweenTwoImages(ref, val, 2);
    	int nbIm1=nbPixelsInClasses(imgRef)[255];
    	int nbIm2=nbPixelsInClasses(imgVal)[255];
    	int nbAND=nbPixelsInClasses(imgAND)[255];
    	int nbOR=nbPixelsInClasses(imgOR)[255];
    	return(nbAND*1.0/nbOR);
    }

    public static Object [][] roiPairingHungarianMethod(Roi[]roiTabRef,Roi[]roiTabTest){
		fr.cirad.image.common.Timer t=new fr.cirad.image.common.Timer();
        double[][]costMatrix=new double[roiTabRef.length][roiTabTest.length];
        for(int i=0;i<roiTabRef.length;i++) {
            for(int j=0;j<roiTabTest.length;j++) {
            	costMatrix[i][j]=1-IOU(roiTabRef[i],roiTabTest[j]);
            }
        }

		HungarianAlgorithm hung=new HungarianAlgorithm(costMatrix);
		int []solutions=hung.execute();
		Object[][]ret=new Object[roiTabRef.length][];
        for(int i=0;i<roiTabRef.length;i++) {
        	double surface=getRoiSurface(roiTabRef[i]);
        	if(solutions[i]==-1)ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
        	else if(IOU(roiTabRef[i],roiTabTest[solutions[i]])<=0) ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
        	else ret[i]=new Object[] {solutions[i],IOU(roiTabRef[i],roiTabTest[solutions[i]]),new Double(surface)};
        }    		
//		t.print("Hungarian");
        return ret;
    }

    public static Object [][] roiPairing(Roi[]roiTabRef,Roi[]roiTabTest){
            boolean []hasBeenPaired=new boolean[roiTabTest.length];
            Object[][]ret=new Object[roiTabRef.length][2];
            for(int i=0;i<roiTabRef.length;i++) {
            	double surface=getRoiSurface(roiTabRef[i]);
            	ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
                double max=0.000001;
                int indmax=0;
                for(int j=0;j<roiTabTest.length;j++) {
            		if(hasBeenPaired[j])continue;
                    double val=IOU(roiTabRef[i],roiTabTest[j]);
                    if(val>max) {
//                    		System.out.println("Nouveau max : "+val);
                            max=val;
                            ret[i]=new Object[] {new Integer(j),new Double(val),new Double(surface)};
                    }
                }
            }
            return ret;
    }
    
    public static double IOU(Roi r1,Roi r2) {
        if(r1.getBounds().getMinX()>r2.getBounds().getMaxX())return 0;
        if(r1.getBounds().getMinY()>r2.getBounds().getMaxY())return 0;
        if(r2.getBounds().getMinX()>r1.getBounds().getMaxX())return 0;
        if(r2.getBounds().getMinY()>r1.getBounds().getMaxY())return 0;

        int x0=(int)Math.floor(Math.min(r1.getBounds().getMinX(), r2.getBounds().getMinX()));
        int x1=(int)Math.ceil(Math.max(r1.getBounds().getMaxX(), r2.getBounds().getMaxX()));
        int y0=(int)Math.floor(Math.min(r1.getBounds().getMinY(), r2.getBounds().getMinY()));
        int y1=(int)Math.ceil(Math.max(r1.getBounds().getMaxY(), r2.getBounds().getMaxY()));
        //System.out.println("Bbox both="+x0+","+x1+","+y0+","+y1);
        int inter=0;
        int union=0;
        for(int x=x0;x<=x1;x++) {
                for(int y=y0;y<=y1;y++) {
                        if((r1.contains(x, y)) && (r2.contains(x, y)))inter++;
                        if((r1.contains(x, y)) || (r2.contains(x, y)))union++;
                }
                //System.out.println("Result : "+inter+" , "+union);
        }
        return (1.0*inter)/union;
}

	public static void scoreComparisonSegmentations(ImagePlus segRef,ImagePlus segTest,boolean verbose) {
		double accIOU=0;
		int nTotReal=0;
		int nTotPred=0;
		int totMatch=0;
		int []totMatchClass=new int[20];
		double []iouPerClass=new double[20];
		int Z=segTest.getNSlices();
		int[]TP=new int[20];
		int TPFull=0;
		int FNFull=0;
		int FPFull=0;
		int[]FN=new int[20];
		int[]FP=new int[20];
		int[]nPred=new int[20];
		int[]nReal=new int[20];
		for(int z=0;z<Z;z++) {
			ImagePlus binaryReal=new Duplicator().run(segRef,1,1,z+1,z+1,1,1);
			ImagePlus binaryPred=new Duplicator().run(segTest,1,1,z+1,z+1,1,1);
            double iouGlob=SegmentationUtils.IOU(binaryReal,binaryPred);
            Roi[]rReal=SegmentationUtils.segmentationToRoi(binaryReal);
            Roi[]rPred=SegmentationUtils.segmentationToRoi(binaryPred);
            for(int i=0;i<rReal.length;i++) {
            	double surf=getRoiSurface(rReal[i]);
				int index=(int)Math.floor(((Double)surf/20.0));
				if(index>19)index=19;
				nReal[index]++;
            }
            for(int i=0;i<rPred.length;i++) {
            	double surf=getRoiSurface(rPred[i]);
				int index=(int)Math.floor(((Double)surf/20.0));
				if(index>19)index=19;
				nPred[index]++;
            }

            if(rPred!=null) {
				nTotReal+=rReal.length;
				nTotPred+=rPred.length;
				Object[][]tab=SegmentationUtils.roiPairingHungarianMethod(rReal,rPred);
				boolean []checkedPred=new boolean[rPred.length];
				for(int i=0;i<tab.length;i++) {	
//					System.out.println(tab[i][0]+" "+tab[i][1]+" "+tab[i][2]);
					int index=(int)Math.floor(((Double)tab[i][2]/20.0));
					if(index>19)index=19;
					//nReal[index]++;
					if((Integer)tab[i][0]<0) {FN[index]++;FNFull++;}
					else {
						checkedPred[(Integer)tab[i][0]]=true;
						int index2=(int)Math.floor(((Double)getRoiSurface(rPred[ (Integer)tab[i][0] ] )/20.0));
						if(index2>19)index2=19;
						nPred[index2]--;
						nPred[index]++;
						
						
						TP[index]++;
						TPFull++;
						accIOU+=(Double)(tab[i][1]);
						iouPerClass[index]+=(Double)(tab[i][1]);
						totMatch++;
						totMatchClass[index]++;
					}
				}
				for(int i=0;i<rPred.length;i++) {
					if(!checkedPred[i]) {
						int index2=(int)Math.floor(((Double)getRoiSurface(rPred[ i ] )/20.0));
						if(index2>19)index2=19;
						FP[index2]++;
						
					}
				}
            }
		}
		accIOU/=totMatch;

		//Compute False positive
		FPFull=nTotPred-TPFull;
		int TPpti=0;
		int TPgrand=0;
		int FNpti=0;
		int FNgrand=0;
		int FPpti=0;
		int FPgrand=0;
		double ioupti=0;
		double iougrand=0;
		int countPti=0;
		int countGrand=0;
		String[]codesPython=new String[] {"prec=[","rec=[","iou=["};
		for(int i=0;i<20;i++) {
			String precision=""+VitimageUtils.dou(TP[i]*1.0/(TP[i]+FP[i]));
			String recall=""+VitimageUtils.dou(TP[i]*1.0/(TP[i]+FN[i]));
			String iou=""+(VitimageUtils.dou(iouPerClass[i]/TP[i]));
			if(TP[i]==0) precision=recall=iou="inf";
			codesPython[0]+=""+precision+(i==19 ? "]" : ",");
			codesPython[1]+=""+recall+(i==19 ? "]" : ",");
			codesPython[2]+=""+iou+(i==19 ? "]" : ",");
			if(verbose)System.out.println("Classe ["+(i*20)+" - "+((i+1)*20)+"]: pr,rec,iou "+precision+" , "+recall+" , "+iou+"    nReal"+nReal[i]+" nPred="+nPred[i]+" nMatch="+totMatchClass[i]+" TP="+TP[i]+" FP="+FP[i]+" FN="+FN[i]);
			if(i<5) {
				TPpti+=TP[i];
				FNpti+=FN[i];
				FPpti+=FP[i];
				ioupti+=iouPerClass[i];
				countPti+=TP[i];
			}
			else {
				TPgrand+=TP[i];
				FNgrand+=FN[i];
				FPgrand+=FP[i];
				iougrand+=iouPerClass[i];
				countGrand+=TP[i];
			}
		}
		ioupti/=countPti;
		iougrand/=countGrand;
//		System.out.println("Total real="+nTotReal+" total pred="+nTotPred);
		double globPrec=VitimageUtils.dou(TPFull*1.0/(nTotPred));
		double globRec=VitimageUtils.dou(TPFull*1.0/(nTotReal));
		double globPrecPti=VitimageUtils.dou(TPpti*1.0/(TPpti+FPpti));
		double globRecPti=VitimageUtils.dou(TPpti*1.0/(TPpti+FNpti));
		double globPrecGrand=VitimageUtils.dou(TPgrand*1.0/(TPgrand+FPgrand));
		double globRecGrand=VitimageUtils.dou(TPgrand*1.0/(TPgrand+FNgrand));
		System.out.println("Summary : Prec="+globPrec+" , Rec="+globRec+" , mean IOU="+accIOU+" . ");
		System.out.println("Little's: Prec="+globPrecPti+" , Rec="+globRecPti+" , mean IOU="+ioupti+" . ");
		System.out.println("Large 's: Prec="+globPrecGrand+" , Rec="+globRecGrand+" , mean IOU="+iougrand+" . ");
		SegmentationUtils.getSizeMap(segRef,0,20,5).show();
		for(String c : codesPython)System.out.println(c);
		
	}

	public static ImagePlus getWatershed(ImagePlus in,ImagePlus marker,ImagePlus mask) {
		MarkerControlledWatershedTransform2D mark=new MarkerControlledWatershedTransform2D(in.getStack().getProcessor(1), marker.getStack().getProcessor(1), mask.getStack().getProcessor(1),4);
		ImageProcessor ip=mark.applyWithPriorityQueueAndDams();
		return new ImagePlus("Results",ip);
	}

	public static ImagePlus getSegmentationFromProbaMap3D(ImagePlus probaMap,double thresh1,double thresh2) {
		ImagePlus debug=probaMap.duplicate();
		ImagePlus []tab=new ImagePlus[probaMap.getNSlices()];
		for(int z=0;z<probaMap.getNSlices();z++) {
			ImagePlus temp=new Duplicator().run(probaMap,1,1,z+1,z+1,1,1);
			tab[z]=getSegmentationFromProbaMap2D(temp,thresh1,thresh2);
		}
		return VitimageUtils.slicesToStack(tab);
	}
	
	public static ImagePlus getPointRoiImageOfMaximaFromProbaMap2D(ImagePlus probaMap,double thresh) {
		//Lisser l'image de probabilité
		ImagePlus probaGauss=probaMap.duplicate();
		IJ.run(probaGauss, "Median...", "sigma=2 stack");

		//Extraire les maxima
		IJ.run(probaGauss, "Find Maxima...", "prominence="+thresh+" output=[Single Points]");
		VitimageUtils.waitFor(100);
		ImagePlus tmp=IJ.getImage();
		ImagePlus pts=tmp.duplicate();
		tmp.changes=false;
		tmp.close();
		pts.setTitle("Points");
		probaGauss.changes=false;
		ImagePlus ret=pts.duplicate();
		probaGauss.close();
		pts.close();
		return ret;
	}
		
	
	public static Point[] getCoordinatesOfMaximaFromProbaMap2D(ImagePlus probaMap,double thresh) {
		probaMap.show();
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(probaMap, "Find Maxima...", "prominence="+thresh+" output=[Point Selection]");
		rm.addRoi(probaMap.getRoi());
		PointRoi pr=(PointRoi) rm.getRoi(0);
		return pr.getContainedPoints();
	}
	
	public static ImagePlus getSegmentationFromProbaMap2D(ImagePlus probaMap,double thresh1,double thresh2) {
		//Get image of maxima
		ImagePlus pts=getPointRoiImageOfMaximaFromProbaMap2D(probaMap,thresh1);

		//Extraire le masque de la zone de proba interessante
		ImagePlus probaGauss2=probaMap.duplicate();
		IJ.run(probaGauss2, "Median...", "sigma=2 stack");
		ImagePlus mask=VitimageUtils.getBinaryMask(probaGauss2, thresh2);

		//Calculer le watershed et les résultats
		ImagePlus result=getWatershed(probaGauss2, pts, mask);
		pts.changes=false;
		pts.close();
		ImagePlus temp=result.duplicate();
		temp= cleanVesselSegmentation(result,512,2,5000);
		return temp;
	}
	

	
    /** Weka train and apply model --------------------------------------------------------------------------------------------*/        
    public static void wekaTrainModel(ImagePlus imgTemp,ImagePlus maskTemp,int[]classifierParams,boolean[]enableFeatures,String modelName) {
   	 ImagePlus img=new Duplicator().run(imgTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
   	 ImagePlus mask=new Duplicator().run(maskTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
   	 int numTrees=classifierParams[0];
	   int numFeatures=classifierParams[1];
	   int seed=classifierParams[2];
	   int minSigma=classifierParams[3];
	   int maxSigma=classifierParams[4];
	   VitimageUtils.printImageResume(img); 
       VitimageUtils.printImageResume(mask); 
	   Runtime. getRuntime(). gc();
        long startTime = System.currentTimeMillis();
        WekaSegmentation seg = new WekaSegmentation(img);

        // Classifier
        FastRandomForest rf = new FastRandomForest();
        rf.setNumTrees(numTrees);                  
        rf.setNumFeatures(numFeatures);  
        rf.setSeed( seed );    
        seg.setClassifier(rf);    
        // Parameters  
        seg.setMembranePatchSize(11);  
        seg.setMinimumSigma(minSigma);
        seg.setMaximumSigma(maxSigma);
  
    
        // Enable features in the segmentator
        seg.setEnabledFeatures( enableFeatures );

        // Add labeled samples in a balanced and random way
  //  	seg.saveFeatureStack(1, "/home/rfernandez/Bureau/tempdata/fsa",new File(modelName).getName());
        
        int[]nbPix=SegmentationUtils.nbPixelsInClasses(mask);
        int min=Math.min(nbPix[0],nbPix[255]);
        int targetExamplesPerSlice=N_EXAMPLES/(2*img.getNSlices());
        System.out.println("Starting training on "+targetExamplesPerSlice+" examples per slice");
        seg.addRandomBalancedBinaryData(img, mask, "class 2", "class 1", targetExamplesPerSlice);
        Runtime. getRuntime(). gc();
        //seg.saveFeatureStack(1,"/home/rfernandez/Bureau/tempdata","test");
        //IJ.showMessage("Done!");
        // Train classifier
        seg.trainClassifier();
        seg.saveClassifier(modelName+".model");
        // Apply trained classifier to test image and get probabilities
    //    ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
     //   probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
     //   IJ.saveAsTiff(probabilityMaps, "/home/rfernandez/Bureau/tempdata/test.tif");
        // Print elapsed time
        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished script in " + estimatedTime + " ms **" );
        seg=null;
		Runtime.getRuntime().gc();
    }
            
    public static ImagePlus wekaApplyModel(ImagePlus imgTemp,int []classifierParams,boolean[]enableFeatures,String modelName) {
    	ImagePlus img=new Duplicator().run(imgTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
  	    IJ.log("weka set params  ");
	    int numTrees=classifierParams[0];
	    int numFeatures=classifierParams[1];
	    int seed=classifierParams[2];
	    int minSigma=classifierParams[3];
	    int maxSigma=classifierParams[4];
	    long startTime = System.currentTimeMillis();
	    WekaSegmentation seg = new WekaSegmentation(imgTemp);
        IJ.log("weka set features  ");
        // Classifier
        FastRandomForest rf = new FastRandomForest();
        rf.setNumTrees(numTrees);                  
        rf.setNumFeatures(numFeatures);  
        rf.setSeed( seed );    
        seg.setClassifier(rf);    

        // Parameters  
        seg.setMembranePatchSize(11);  
        seg.setMinimumSigma(minSigma);
        seg.setMaximumSigma(maxSigma);
        seg.setEnabledFeatures( enableFeatures );

        VitimageUtils.garbageCollector();
        IJ.log("weka load model  ");
        seg.loadClassifier(modelName+".model");

        // Apply trained classifier to test image and get probabilities
        IJ.log("Computing maps ");        
    	ImagePlus probabilityMaps=seg.applyClassifier(img,0,true);
        probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished apply model in " + estimatedTime + " ms **" );
        seg=null;
        VitimageUtils.garbageCollector();
        return probabilityMaps;
}

    public static ImagePlus wekaApplyModelSlicePerSlice(ImagePlus img,int[]classifierParams,boolean[]enableFeatures,String modelName) {
    	System.out.println("SU 1");
    	int numTrees=classifierParams[0];
 	   int numFeatures=classifierParams[1];
 	   int seed=classifierParams[2];
 	   int minSigma=classifierParams[3];
 	   int maxSigma=classifierParams[4];
       long startTime = System.currentTimeMillis();
   	System.out.println("SU 2");
       HyperWekaSegmentation seg = new HyperWekaSegmentation(new Duplicator().run(img,1,1,1,1,1,1),null);
        // Classifier
        FastRandomForest rf = new FastRandomForest();
        rf.setNumTrees(numTrees);                  
        rf.setNumFeatures(numFeatures);  
        rf.setSeed( seed );    
        seg.setClassifier(rf);    
    	System.out.println("SU 3");
        // Parameters  
        seg.setMembranePatchSize(11);  
        seg.setMinimumSigma(minSigma);
        seg.setMaximumSigma(maxSigma);
      
        // Enable features in the segmentator
        seg.setEnabledFeatures( enableFeatures );

        // Add labeled samples in a balanced and random way
        seg.updateWholeImageData();
        System.out.println("Loading model");
        seg.loadClassifier(modelName+".model");
        System.out.println("Loaded");

        
        // Apply trained classifier to test image and get probabilities
        ImagePlus []inTab=VitimageUtils.stackToSlices(img);
        ImagePlus [][]outTab=new ImagePlus[seg.getNumOfClasses()][inTab.length];
        ImagePlus []outTabChan=new ImagePlus[seg.getNumOfClasses()];
        for(int i=0;i<inTab.length;i++) {
        	System.out.println("Applying Classifier to slice number "+i);
        	ImagePlus temp=seg.applyClassifier( inTab[i], 0, true);//Sortie : C2 Z1
        	for(int c=0;c<seg.getNumOfClasses();c++)outTab[c][i] = new Duplicator().run(temp,c+1,c+1,1,1,1,1); // sortie C x Z cases
        }
    	System.out.println("Ok");
        for(int i=0;i<seg.getNumOfClasses();i++)outTabChan[i]=VitimageUtils.slicesToStack(outTab[i]);//sortie C cases de Z stacks
        ImagePlus probabilityMaps=VitimageUtils.hyperStackingChannels(outTabChan);
        System.out.print("weka step 9  ");
        probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
        // Print elapsed time
        long estimatedTime = System.currentTimeMillis() - startTime;
        IJ.log( "** Finished script in " + estimatedTime + " ms **" );
        inTab=null;
        outTab=null;
        outTabChan=null;
        Runtime. getRuntime(). gc();
        return probabilityMaps;
}
   
           
     public static void hyperWekaTrainModel(ImagePlus imgTemp,ImagePlus maskTemp,int[]classifierParams,boolean[]enableFeatures,String modelName) {
    	 
    	 ImagePlus img=new Duplicator().run(imgTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
    	 ImagePlus mask=new Duplicator().run(maskTemp,1,1,1,debugTrain ? 5 : imgTemp.getNSlices(),1,1);
    	 int numTrees=classifierParams[0];
 	   int numFeatures=classifierParams[1];
 	   int seed=classifierParams[2];
 	   int minSigma=classifierParams[3];
 	   int maxSigma=classifierParams[4];
 	   VitimageUtils.printImageResume(img); 
       VitimageUtils.printImageResume(mask); 
 	   Runtime. getRuntime(). gc();
         long startTime = System.currentTimeMillis();
         HyperWekaSegmentation seg = new HyperWekaSegmentation(img,null);

         // Classifier
         FastRandomForest rf = new FastRandomForest();
         rf.setNumTrees(numTrees);                  
         rf.setNumFeatures(numFeatures);  
         rf.setSeed( seed );    
         seg.setClassifier(rf);    
         // Parameters  
         seg.setMembranePatchSize(11);  
         seg.setMinimumSigma(minSigma);
         seg.setMaximumSigma(maxSigma);
   
     
         // Enable features in the segmentator
         seg.setEnabledFeatures( enableFeatures );

         // Add labeled samples in a balanced and random way

         int[]nbPix=SegmentationUtils.nbPixelsInClasses(mask);
         int min=Math.min(nbPix[0],nbPix[255]);
         int targetExamplesPerSlice=N_EXAMPLES/(2*img.getNSlices());
         System.out.println("Starting training on "+targetExamplesPerSlice+" examples per slice");
         seg.addRandomBalancedBinaryData(img, mask, "class 2", "class 1", targetExamplesPerSlice);
         seg.saveFeatureStack(1, "/home/rfernandez/Bureau/tempdata/"+new Timer().hashCode());
         
         Runtime. getRuntime(). gc();
         
         // Train classifier
         seg.trainClassifier();
         seg.saveClassifier(modelName+".model");
         // Apply trained classifier to test image and get probabilities
         //ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
         //probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
         // Print elapsed time
         long estimatedTime = System.currentTimeMillis() - startTime;
         IJ.log( "** Finished script in " + estimatedTime + " ms **" );
         seg=null;
 		Runtime.getRuntime().gc();
     }
             
     public static ImagePlus hyperWekaApplyModel(ImagePlus img,int []classifierParams,boolean[]enableFeatures,String modelName) {
  	   int numTrees=classifierParams[0];
  	   int numFeatures=classifierParams[1];
  	   int seed=classifierParams[2];
  	   int minSigma=classifierParams[3];
  	   int maxSigma=classifierParams[4];
         long startTime = System.currentTimeMillis();
         System.out.print("weka step 1   ");
         HyperWekaSegmentation seg = new HyperWekaSegmentation(img,null);
         System.out.print("weka step 2   ");

         // Classifier
         FastRandomForest rf = new FastRandomForest();
         rf.setNumTrees(300);                  
         rf.setNumFeatures(14);  
         rf.setSeed( seed );    
         System.out.print("weka step 3  ");
         seg.setClassifier(rf);    
         // Parameters  
         System.out.print("weka step 4  ");
         seg.setMembranePatchSize(11);  
         seg.setMinimumSigma(minSigma);
         seg.setMaximumSigma(maxSigma);
         System.out.print("weka step 5  ");
   
      
         // Enable features in the segmentator
         seg.setEnabledFeatures( enableFeatures );
         System.out.print("weka step 6  ");

         // Add labeled samples in a balanced and random way
         seg.updateWholeImageData();
         System.out.print("weka step 65  ");
         seg.loadClassifier(modelName+".model");
         VitimageUtils.garbageCollector();
         System.out.print("weka step 7  ");
         // Train classifier

         // Apply trained classifier to test image and get probabilities
         ImagePlus probabilityMaps = seg.applyClassifier( img, 0, true);
         System.out.print("weka step 9  ");
         probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
         // Print elapsed time
         long estimatedTime = System.currentTimeMillis() - startTime;
         IJ.log( "** Finished script in " + estimatedTime + " ms **" );
         seg=null;
         Runtime.getRuntime().gc();
         return probabilityMaps;
 }

     public static ImagePlus hyperWekaApplyModelSlicePerSlice(ImagePlus img,int[]classifierParams,boolean[]enableFeatures,String modelName) {
  	   int numTrees=classifierParams[0];
  	   int numFeatures=classifierParams[1];
  	   int seed=classifierParams[2];
  	   int minSigma=classifierParams[3];
  	   int maxSigma=classifierParams[4];
        long startTime = System.currentTimeMillis();
        HyperWekaSegmentation seg = new HyperWekaSegmentation(new Duplicator().run(img,1,1,1,1,1,1),null);
         // Classifier
         FastRandomForest rf = new FastRandomForest();
         rf.setNumTrees(numTrees);                  
         rf.setNumFeatures(numFeatures);  
         rf.setSeed( seed );    
         seg.setClassifier(rf);    
         // Parameters  
         seg.setMembranePatchSize(11);  
         seg.setMinimumSigma(minSigma);
         seg.setMaximumSigma(maxSigma);
       
         // Enable features in the segmentator
         seg.setEnabledFeatures( enableFeatures );

         // Add labeled samples in a balanced and random way
         seg.updateWholeImageData();
         System.out.println("Loading model");
         seg.loadClassifier(modelName+".model");
         System.out.println("Loaded");

         
         // Apply trained classifier to test image and get probabilities
         ImagePlus []inTab=VitimageUtils.stackToSlices(img);
         ImagePlus [][]outTab=new ImagePlus[seg.getNumOfClasses()][inTab.length];
         ImagePlus []outTabChan=new ImagePlus[seg.getNumOfClasses()];
         for(int i=0;i<inTab.length;i++) {
         	System.out.println("Applying Classifier to slice number "+i);
         	ImagePlus temp=seg.applyClassifier( inTab[i], 0, true);//Sortie : C2 Z1
         	for(int c=0;c<seg.getNumOfClasses();c++)outTab[c][i] = new Duplicator().run(temp,c+1,c+1,1,1,1,1); // sortie C x Z cases
         }
     	System.out.println("Ok");
         for(int i=0;i<seg.getNumOfClasses();i++)outTabChan[i]=VitimageUtils.slicesToStack(outTab[i]);//sortie C cases de Z stacks
         ImagePlus probabilityMaps=VitimageUtils.hyperStackingChannels(outTabChan);
         System.out.print("weka step 9  ");
         probabilityMaps.setTitle( "Probability maps of " + img.getTitle() );
         // Print elapsed time
         long estimatedTime = System.currentTimeMillis() - startTime;
         IJ.log( "** Finished script in " + estimatedTime + " ms **" );
         inTab=null;
         outTab=null;
         outTabChan=null;
         Runtime. getRuntime(). gc();
         return probabilityMaps;
 }
   
    
    
    /** Routines for data augmentation --------------------------------------------------------------------------------------------*/        
	public static ImagePlus[] rotationAugmentationStack(ImagePlus imgIn,ImagePlus maskIn,double probaRotation,int repetitions,int seed){
		int N=imgIn.getNSlices();
		ImagePlus []tabSlicesOut=new ImagePlus[repetitions*N];
		ImagePlus[]tab2=VitimageUtils.stackToSlices(imgIn);
		ImagePlus []tabSlicesOutMask=new ImagePlus[repetitions*N];
		ImagePlus[]tab2Mask=VitimageUtils.stackToSlices(maskIn);
		for(int i=0;i<repetitions;i++) {
			for(int j=0;j<N;j++) {
				tabSlicesOut[(i)*N+j]=tab2[j].duplicate();
				tabSlicesOutMask[(i)*N+j]=tab2Mask[j].duplicate();
			}
		}
		
		Random rand=new Random(seed);
		for(int i=0;i<tabSlicesOut.length;i++) {
			double p=rand.nextDouble();
			tabSlicesOut[i].show();
			if(p<probaRotation)IJ.run("Rotate 90 Degrees Right");
			tabSlicesOut[i].hide();
			tabSlicesOutMask[i].show();
			if(p<probaRotation)IJ.run("Rotate 90 Degrees Right");
			tabSlicesOutMask[i].hide();
		}
		return new ImagePlus[] {VitimageUtils.slicesToStack(tabSlicesOut),VitimageUtils.slicesToStack(tabSlicesOutMask)};
	}
		
	public static ImagePlus colorAugmentationStack(ImagePlus imgIn,boolean isMask,int nMult,double std,boolean keepOriginal){
		int N=imgIn.getNSlices();
		double mean=1;
		int frequency=5;
		ImagePlus []tabOut=new ImagePlus[nMult];
		ImagePlus []tabSlicesOut=new ImagePlus[N*nMult];
		int i=0;
		for(int iMult=0;iMult<nMult;iMult++) {
			System.out.println("Processing brightness "+iMult);
			tabOut[iMult]=imgIn.duplicate();
			if(isMask ||  (iMult==0 && keepOriginal)) {
				for(int j=0;j<N;j++) {
					ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
					System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
					tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
				}
				continue;
			}
			ImagePlus augmentationMapR=getAugmentationMap(imgIn,mean,std,frequency);
			ImagePlus augmentationMapG=getAugmentationMap(imgIn,mean,std,frequency);
			ImagePlus augmentationMapB=getAugmentationMap(imgIn,mean,std,frequency);
			tabOut[iMult].setTitle("temp");
			tabOut[iMult].show();
			IJ.selectWindow("temp");
			IJ.run("Split Channels");
			tabOut[iMult].changes=false;
			tabOut[iMult].close();
			ImagePlus imgR=ij.WindowManager.getImage("temp (red)");
			ImagePlus imgG=ij.WindowManager.getImage("temp (green)");
			ImagePlus imgB=ij.WindowManager.getImage("temp (blue)");
			
			ImagePlus imgR2=VitimageUtils.makeOperationBetweenTwoImages(imgR, augmentationMapR, 2, false);
			ImagePlus imgG2=VitimageUtils.makeOperationBetweenTwoImages(imgG, augmentationMapG, 2, false);
			ImagePlus imgB2=VitimageUtils.makeOperationBetweenTwoImages(imgB, augmentationMapB, 2, false);
			imgR.close();imgG.close();imgB.close();imgR2.show();imgG2.show();imgB2.show();imgR2.setTitle("temp (red)");imgG2.setTitle("temp (green)");imgB2.setTitle("temp (blue)");

			IJ.run("Merge Channels...", "c1=[temp (red)] c2=[temp (green)] c3=[temp (blue)] create");
			IJ.run("Stack to RGB", "slices keep");
			tabOut[iMult]=IJ.getImage();
			ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
			for(int j=0;j<N;j++) {
				System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
				tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
			}
			WindowManager.getImage("Composite").close();
			WindowManager.getImage("Composite-1").close();
		}
		Runtime. getRuntime(). gc();
		System.out.println("Assembling tab of "+tabSlicesOut.length);
		return VitimageUtils.slicesToStack(tabSlicesOut);
	}
		
	public static ImagePlus brightnessAugmentationStack(ImagePlus imgIn,boolean isMask,int nMult,double std,boolean keepOriginal){
		int N=imgIn.getNSlices();
		double mean=1;
		int frequency=5;
		ImagePlus []tabOut=new ImagePlus[nMult];
		ImagePlus []tabSlicesOut=new ImagePlus[N*nMult];
		int i=0;
		for(int iMult=0;iMult<nMult;iMult++) {
			System.out.println("T1");VitimageUtils.waitFor(1000);
			System.out.println("Processing brightness "+iMult);
			tabOut[iMult]=imgIn.duplicate();
			if(isMask || (iMult==0 && keepOriginal)) {
				for(int j=0;j<N;j++) {
					ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
					System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
					tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
				}
				continue;
			}
			System.out.println("T2");VitimageUtils.waitFor(1000);
			ImagePlus augmentationMap=getAugmentationMap(imgIn,mean,std,frequency);
			tabOut[iMult].setTitle("temp");
			tabOut[iMult].show();
			IJ.selectWindow("temp");
			IJ.run("Split Channels");
			System.out.println("T3");VitimageUtils.waitFor(1000);
			tabOut[iMult].close();
			ImagePlus imgR=ij.WindowManager.getImage("temp (red)");
			ImagePlus imgG=ij.WindowManager.getImage("temp (green)");
			ImagePlus imgB=ij.WindowManager.getImage("temp (blue)");
			
			ImagePlus imgR2=VitimageUtils.makeOperationBetweenTwoImages(imgR, augmentationMap, 2, false);
			ImagePlus imgG2=VitimageUtils.makeOperationBetweenTwoImages(imgG, augmentationMap, 2, false);
			ImagePlus imgB2=VitimageUtils.makeOperationBetweenTwoImages(imgB, augmentationMap, 2, false);
			imgR.close();imgG.close();imgB.close();imgR2.show();imgG2.show();imgB2.show();imgR2.setTitle("temp (red)");imgG2.setTitle("temp (green)");imgB2.setTitle("temp (blue)");
			System.out.println("T4");VitimageUtils.waitFor(1000);

			IJ.run("Merge Channels...", "c1=[temp (red)] c2=[temp (green)] c3=[temp (blue)] create");
			IJ.run("Stack to RGB", "slices keep");
			tabOut[iMult]=IJ.getImage();
			ImagePlus[]tab2=VitimageUtils.stackToSlices(tabOut[iMult]);
			System.out.println("T5");VitimageUtils.waitFor(1000);
			for(int j=0;j<N;j++) {
				System.out.println("Copy tab "+j+" to tabOut "+(iMult*N+j));
				tabSlicesOut[(iMult)*N+j]=tab2[j].duplicate();
			}
			WindowManager.getImage("Composite").close();
			WindowManager.getImage("Composite-1").close();
		}
		System.out.println("T6");VitimageUtils.waitFor(1000);
		Runtime. getRuntime(). gc();
		System.out.println("Assembling tab of "+tabSlicesOut.length);
		return VitimageUtils.slicesToStack(tabSlicesOut);
	}

    public static ImagePlus getAugmentationMap(ImagePlus imgIn,double mean,double std,int frequency) {
    	int Z=imgIn.getNSlices();
    	int X=imgIn.getWidth();
    	int Y=imgIn.getHeight();
    	ImagePlus []slices=new ImagePlus[Z];
    	ImagePlus sliceExample=new Duplicator().run(imgIn,1,1,1,1,1,1);
    	Random rand=new Random();
    	int[][]coordinates=new int[frequency*frequency][3];
    	double []values=new double[frequency*frequency];
    	for(int x=0;x<frequency;x++)for(int y=0;y<frequency;y++) {
    		coordinates[x*frequency+y]=new int[] {
    				(int)Math.round(((x+0.5)*X*1.0)/frequency),
    				(int)Math.round(((y+0.5)*Y*1.0)/frequency),
    				0
    		};
    		//System.out.println(TransformUtils.stringVector(coordinates[x*frequency+y],""));
    	}
    	for(int z=0;z<Z;z++) {
    		for(int x=0;x<frequency;x++)for(int y=0;y<frequency;y++)values[x*frequency+y]=rand.nextGaussian()*std+mean;
    		slices[z]=ItkTransform.smoothImageFromCorrespondences(coordinates,values, sliceExample,X/frequency,false);
    		VitimageUtils.waitFor(100);
    	}
    	return VitimageUtils.slicesToStack(slices);
    	
    }
     
          

	
	
	
	
	
	

    /** Helpers for conversion to / from Json , to / from Roi[] , to / from binary segmentation  -------------- */
    public static ImagePlus []jsonToBinary(String dir) {
            System.out.println(dir);
            
            String[]listFiles=new File(dir).list();
            System.out.println(listFiles.length);
            int nImgs=listFiles.length-1;
            String s="";
            for(String s2 : listFiles) {
                if(s2.contains(".json"))s=s2;
            }
            
            String[][][] tabData=convertJsonToRoi(new File(dir,s).getAbsolutePath());
            ImagePlus[]imgInit=new ImagePlus[nImgs];
            ImagePlus[]imgMask=new ImagePlus[nImgs];
            
            for(int indImg=0;indImg<tabData.length;indImg++) {
                    System.out.println("Opening image "+new File(dir,tabData[indImg][0][0]).getAbsolutePath());
                    imgInit[indImg]=IJ.openImage(new File(dir,tabData[indImg][0][0]).getAbsolutePath());
                    imgMask[indImg]=imgInit[indImg].duplicate();
                    IJ.run(imgMask[indImg],"8-bit","");
                    imgMask[indImg]=VitimageUtils.nullImage(imgMask[indImg]);
                    imgMask[indImg].show();
                    for(int indRoi=0;indRoi<tabData[indImg].length;indRoi++) {
                            Roi r=roiParser(tabData[indImg][indRoi][1],tabData[indImg][indRoi][2]);
                            imgMask[indImg].setRoi(r);
                            IJ.run("Fill", "slice");
                    }
                    imgMask[indImg].hide();
            }
            ImagePlus imgInitFull=VitimageUtils.slicesToStack(imgInit);
            ImagePlus imgMaskFull=VitimageUtils.slicesToStack(imgMask);
            return new ImagePlus[] {imgInitFull,imgMaskFull};
    }
                    
    public static String[][][]convertJsonToRoi(String jsonPath){
            String bag="val";
            String conf="2048";
            int fact=2;
            String pathIn=jsonPath;
            JSONObject jo = new JSONObject(VitimageUtils.readStringFromFile(pathIn));
            Iterator <String>iter=jo.keys();
            String[][][] ret=new String[jo.length()][3][];
            int indexImg=-1;
            while(iter.hasNext()) {
                    String key=(String)iter.next();
                    System.out.println("Image name : "+key);
                    JSONObject jo2=jo.getJSONObject(key);
                    Iterator <String>iter2=jo2.keys();
                    
                    //Iterate over images
                    while(iter2.hasNext()) {
                            String key2=(String)iter2.next();
                            if(key2.equals("regions")){
                                    JSONArray jo3=jo2.getJSONArray(key2);
                                    ret[++indexImg]=new String[jo3.length()][3];

                                    
                                    //Iterate over regions
                                    int indexReg=-1;
                                    for(int i=0;i<jo3.length();i++) {
                                            ret[indexImg][++indexReg][0]=key.split(".jpg")[0]+".jpg";//img name
                                            JSONObject jo4=jo3.getJSONObject(i);
                                            Iterator <String>iter4=jo4.keys();
                                            while(iter4.hasNext()) {
                                                    String key4=(String)iter4.next();
                                                    if(key4.equals("shape_attributes")){
                                                            JSONObject jo5=jo4.getJSONObject(key4);
                                                            Iterator <String>iter5=jo5.keys();
                                                            System.out.println();
                                                            while(iter5.hasNext()) {
                                                                    String key5=(String)iter5.next();
                                                                    if(key5.equals("all_points_x")){
                                                                            JSONArray tabX=(JSONArray) jo5.get(key5);
                                                                            String data=tabX.join(",");
                                                                            System.out.println("PtX="+data);
                                                                            ret[indexImg][indexReg][1]=(data);
                                                                    }
                                                                    if(key5.equals("all_points_y")){
                                                                            JSONArray tabY=(JSONArray) jo5.get(key5);
                                                                            String data=tabY.join(",");
                                                                            System.out.println("PtY="+data);
                                                                            ret[indexImg][indexReg][2]=(data);
                                                                    }
                                                            }        
                                                    }
                                            }
                                    }                                        
                            }
                    }                        
            }
            return ret;
    }                

    public static Roi roiParser(String xcoords,String ycoords) {
        int[]tabX=stringTabToDoubleTab(xcoords.split(","));
        int[]tabY=stringTabToDoubleTab(ycoords.split(","));
        return new PolygonRoi(tabX, tabY, tabX.length,Roi.POLYGON);
	  }
    
    public static int[]stringTabToDoubleTab(String[]tab){
        int[]ret=new int[tab.length];
        for(int i=0;i<tab.length;i++)ret[i]=Integer.parseInt(tab[i]);
        return ret;
    }

    public static Roi[]segmentationToRoi(ImagePlus seg){
    	ImagePlus imgSeg=VitimageUtils.getBinaryMask(seg, 0.5);
    	RoiManager rm=RoiManager.getRoiManager();
    	rm.close();
    	imgSeg.show();
    	IJ.setRawThreshold(imgSeg, 127, 255, null);
        VitimageUtils.waitFor(100);
    	
    	rm=RoiManager.getRoiManager();
        VitimageUtils.waitFor(100);
        IJ.run("Create Selection");
        //VitimageUtils.printImageResume(IJ.getImage(),"getImage");
        Roi r=IJ.getImage().getRoi();
       // System.out.println(r);
        if(r==null)return null;
        Roi[] rois = ((ShapeRoi)r).getRois();
        IJ.getImage().close();
        rm.close();
        return rois;
    }
    

    
    
	/*** Helpers for preparation of data (Roi, images) ---------------------------------------------------*/
	public static Roi[]pruneRoi(Roi[]roiTab,int targetResolution){
		int maxPossible=targetResolution-3;
		int minPossible=2;
		boolean []take=new boolean[roiTab.length];
		int select=0;
		for(int i=0;i<roiTab.length;i++) {
			take[i]=true;
			Roi r=roiTab[i];
			Polygon p=r.getPolygon();
			for(int val:p.xpoints)if(val<minPossible || val>maxPossible)take[i]=false;
			for(int val:p.ypoints)if(val<minPossible || val>maxPossible)take[i]=false;
			if(take[i])select++;
		}
		Roi[]tabOut=new Roi[select];
		int incr=0;
		for(int i=0;i<roiTab.length;i++) {
			if(take[i]) {
				tabOut[incr]=roiTab[i];
				incr++;
			}
		}
		//System.out.println("Prune : in="+roiTab.length+" , out="+tabOut.length);
		return tabOut;			
	}

    public static void resampleJsonAndImageSet(String dirIn,String dirOut,int resampleFactor) {
        String pathJsonIn=new File(dirIn,"via_region_data.json").getAbsolutePath();                                
        double fact=1.0/resampleFactor;

        String[]listImages=new File(dirIn).list();
        for(String s:listImages) {
                if(s.contains(".jpg")) {
                        ImagePlus img=IJ.openImage(new File(dirIn,s).getAbsolutePath());
                        int targetSize=img.getWidth()/resampleFactor;
                        img.show();
                        IJ.run("Scale...", "x="+fact+" y="+fact+" width="+targetSize+" height="+targetSize+" interpolation=Bilinear average create");//create
                        img=IJ.getImage();
                        IJ.save(img,new File(dirOut,s).getAbsolutePath());
                        img.changes=false;
                        img.close();
                        img=IJ.getImage();
                        img.changes=false;
                        img.close();
                }
        }
        String pathJsonOut=new File(dirOut,"via_region_data.json").getAbsolutePath();
        String dataIn=VitimageUtils.readStringFromFile(pathJsonIn);
        String data2=dataIn.replace("\"all_points_x\":[","PPTTXX\n").replace("\"all_points_y\":[","PPTTYY\n");
        String[]data3=data2.split("\n");
        String data4=data3[0];
        for(int lig=1;lig<data3.length;lig++) {
                int indCar=data3[lig].indexOf("]");
                String toReplace=data3[lig].substring(0, indCar);
                String[]numbers=toReplace.split(",");
                String replacing="";
                for(int ind=0;ind<numbers.length;ind++) {
                        int nb=(int)Math.round(( (Integer.parseInt(numbers[ind]))*1.0)/resampleFactor);
                        replacing+=""+nb;
                        if(ind<numbers.length-1)replacing+=",";
                }
                data3[lig].replace(toReplace, replacing);
                data4+=replacing+data3[lig].substring(indCar)+(lig<data3.length-1 ? "\n" : "");
        }
        String data5=data4.replace("PPTTXX", "\"all_points_x\":[").replace("PPTTYY", "\"all_points_y\":[").replace("\n","");
        VitimageUtils.writeStringInFile(data5, pathJsonOut);
}

	public static ImagePlus cleanVesselSegmentation(ImagePlus seg,int targetResolution,int minNbVox,int maxNbVox) {
			double voxVol=VitimageUtils.getVoxelVolume(seg);
			double minVBsurface=voxVol*minNbVox;
			double maxVBsurface=voxVol*maxNbVox;

			//Remplir les trous inutiles
			ImagePlus imgSeg=VitimageUtils.getBinaryMask(seg, 0.5);
			imgSeg.show();
			//VitimageUtils.waitFor(3000);
			//IJ.run("Fill Holes", "stack");
			//VitimageUtils.waitFor(3000);
			ImagePlus test1=imgSeg.duplicate();
			imgSeg.hide();
			//Retirer les cellules plus petites que et plus grandes que
			imgSeg=VitimageUtils.connexe2d(imgSeg, 1,256, minVBsurface, maxVBsurface , 6,0,true);
			ImagePlus test2=imgSeg.duplicate();
			return VitimageUtils.getBinaryMask(imgSeg,0.5);
		}
    
    public static double getRoiSurface(Roi r) {
        int x0=(int)Math.floor(r.getBounds().getMinX());
        int x1=(int)Math.floor(r.getBounds().getMaxX());
        int y0=(int)Math.floor(r.getBounds().getMinY());
        int y1=(int)Math.floor(r.getBounds().getMaxY());
        int inter=0;
        for(int x=x0;x<=x1;x++) {
            for(int y=y0;y<=y1;y++) {
                if(r.contains(x, y))inter++;
            }
        }
        return inter;
   	
    }
        
	public static int[]nbPixelsInClasses(ImagePlus img){
        int[]tab=img.getStack().getProcessor(1).getHistogram();
        return tab;
    }
       
	public static ImagePlus getBinaryMaskUnary(ImagePlus img,double threshold) {
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		int type=(img.getType()==ImagePlus.GRAY8 ? 8 : img.getType()==ImagePlus.GRAY16 ? 16 : img.getType()==ImagePlus.GRAY32 ? 32 : 24);
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		if(type==8) {
			for(int z=0;z<dimZ;z++) {
				byte []tabImg=(byte[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x] & 0xff) >= (byte)(((int)Math.round(threshold)) & 0xff)  )tabRet[dimX*y+x]=(byte)(1 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else if(type==16) {
			for(int z=0;z<dimZ;z++) {
				short []tabImg=(short[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x] & 0xffff) >= (short)(((int)Math.round(threshold)) & 0xffff)  )tabRet[dimX*y+x]=(byte)(1 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else if(type==32) {
			for(int z=0;z<dimZ;z++) {
				float []tabImg=(float[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x]) >= threshold )tabRet[dimX*y+x]=(byte)(1 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else VitiDialogs.notYet("getBinary Mask type "+type);
		return ret;
	}
	

	public static int NUM_TREES=200;
    public static int NUM_FEATS=14;//sqrt (#feat)
    public static int MIN_SIGMA=2;
    public static int MAX_SIGMA=16;//32
    public static int N_EXAMPLES=200000;//1M
    public static boolean debugTrain=false;
    
    public static int[]getStandardRandomForestParams(int seed){
	   return new int[] {NUM_TREES,NUM_FEATS,seed,MIN_SIGMA,MAX_SIGMA};
    }
    public static boolean[]getShortRandomForestFeatures(){
     	return new boolean[]{
        true,   /* Gaussian_blur */
        true,   /* Sobel_filter */
        false,   /* Hessian */
        false,   /* Difference_of_gaussians */
        false,   /* Membrane_projections */
        true,  /* Variance */
        false,  /* Mean */
        true,  /* Minimum */
        true,  /* Maximum */
        true,  /* Median */
        false,  /* Anisotropic_diffusion */
        false,  /* Bilateral */
        false,  /* Lipschitz */
        false,  /* Kuwahara */
        false,  /* Gabor */
        true,  /* Derivatives */
        true,  /* Laplacian */
        false,  /* Structure */
        false,  /* Entropy */
        false   /* Neighbors */
	};
    }

    public static boolean[]getStandardRandomForestFeatures(){
     	return new boolean[]{
        true,   /* Gaussian_blur */
        true,   /* Sobel_filter */
        true,   /* Hessian */
        true,   /* Difference_of_gaussians */
        false,   /* Membrane_projections */
        true,  /* Variance */
        true,  /* Mean */
        true,  /* Minimum */
        true,  /* Maximum */
        true,  /* Median */
        false,  /* Anisotropic_diffusion */
        false,  /* Bilateral */
        false,  /* Lipschitz */
        false,  /* Kuwahara */
        false,  /* Gabor */
        true,  /* Derivatives */
        true,  /* Laplacian */
        false,  /* Structure */
        false,  /* Entropy */
        false   /* Neighbors */
	};
}

}
