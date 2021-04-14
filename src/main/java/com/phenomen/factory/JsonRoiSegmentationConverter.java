package com.phenomen.factory;

import java.awt.Polygon;
import java.io.File;
import java.util.Iterator;
import ij.plugin.ChannelSplitter;

import org.json.JSONArray;
import org.json.JSONObject;

import com.phenomen.common.VitiDialogs;
import com.phenomen.common.VitimageUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;
import com.phenomen.common.VitimageUtils;

public class JsonRoiSegmentationConverter {
    
	//IJ.run(imp, "Gaussian Blur...", "sigma=2");
	//IJ.run(imp, "Find Maxima...", "prominence=0.50 strict output=[Segmented Particles]");

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

    
    
	public static ImagePlus getSizeMap(ImagePlus img,int threshLow,int step,int nCat,boolean show) {

		ImagePlus imgRet=VitimageUtils.nullImage(img);
		IJ.run(imgRet,"8-bit","");
		imgRet.setDisplayRange(0, nCat*step);
		IJ.run(imgRet,"Fire","");
		for(int z=0;z<imgRet.getNSlices();z++) {
			ImagePlus temp=new Duplicator().run(img,1,1,z+1,z+1,1,1);
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
		VitimageUtils.showWithParams(imgRet,img.getTitle()+"_size_map",1,0,nCat*step);
/*			imgRet.show();
		imgRet.setDisplayRange(0, nCat*step);
		imgRet.updateAndDraw();
		IJ.run(imgRet,"Fire","");
		IJ.run("Brightness/Contrast...");
		selectWindow("B&C");
		*/
		if(!show)imgRet.hide();
		return imgRet;
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

		

		public static ImagePlus cleanVesselSegmentation(ImagePlus seg,int targetResolution,int minNbVox,int maxNbVox) {
			double voxVol=VitimageUtils.getVoxelVolume(seg);
			double minVBsurface=voxVol*minNbVox;
			double maxVBsurface=voxVol*maxNbVox;

			//Remplir les trous inutiles
			ImagePlus imgSeg=VitimageUtils.getBinaryMask(seg, 0.5);
			imgSeg.show();
			IJ.run("Fill Holes", "stack");
			ImagePlus test1=imgSeg.duplicate();
			imgSeg.hide();
			//Retirer les cellules plus petites que et plus grandes que
			imgSeg=VitimageUtils.connexe2d(imgSeg, 1,256, minVBsurface, maxVBsurface , 6,0,true);
			ImagePlus test2=imgSeg.duplicate();
			return VitimageUtils.getBinaryMask(imgSeg,0.5);
		}
    

    
    public static Object [][] roiPairingHungarianMethod(Roi[]roiTabRef,Roi[]roiTabTest){
        double[][]costMatrix=new double[roiTabRef.length][roiTabTest.length];
        for(int i=0;i<roiTabRef.length;i++) {
            for(int j=0;j<roiTabRef.length;j++) {
            	costMatrix[i][j]=1-IOU(roiTabRef[i],roiTabTest[j]);
            }
        }
        
		HungarianAlgorithm hung=new HungarianAlgorithm(costMatrix);
		com.phenomen.common.Timer t=new com.phenomen.common.Timer();
		t.print("Starting hungarian over "+roiTabRef.length+" x "+roiTabTest.length);
		int []solutions=hung.execute();
		t.print("Finishing hungarian over "+roiTabRef.length+" x "+roiTabTest.length);

		Object[][]ret=new Object[roiTabRef.length][];
        for(int i=0;i<roiTabRef.length;i++) {
        	double surface=getRoiSurface(roiTabRef[i]);
        	if(solutions[i]==-1)ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
        	else if(costMatrix[i][solutions[i]]>=1)ret[i]=new Object[] {new Integer(-1),new Double(0),new Double(surface)};
        	else ret[i]=new Object[] {solutions[i],IOU(roiTabRef[i],roiTabTest[solutions[i]]),new Double(surface)};
        }    		
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
    	imgSeg.show();
//        VitimageUtils.waitFor(100);
        IJ.run("Create Selection");
        //VitimageUtils.printImageResume(IJ.getImage(),"getImage");
        Roi r=IJ.getImage().getRoi();
        //System.out.println(r);
        Roi[] rois = ((ShapeRoi)r).getRois();
        IJ.getImage().close();
        return rois;
    }
    
    



}
