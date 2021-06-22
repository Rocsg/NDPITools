package fr.cirad.image.mlutils;

import java.util.Random;

import org.apache.commons.collections.TransformerUtils;

import fr.cirad.image.common.Timer;
import fr.cirad.image.common.TransformUtils;
import fr.cirad.image.common.VitimageUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.Duplicator;

public class SmallestEnclosingCircle {
	/* This macro creates a circular selection that is the smallest circle
	   enclosing the current selection.
	   Version: 2009-06-12 Michael Schmid

	   Restrictions:
	   - Does not work with composite selections
	   - Due to rounding errors, some selection points may be slightly outside the circle
	*/
	public static double standardThresholdOutlier=0.0025;

	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		testSmallest(5);
	}
	
	
	public static void testSmallest(int test) {
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/test.tif");
		img=new Duplicator().run(img,1,1,1,1,1,1);
		IJ.run(img,"8-bit","");
		img=VitimageUtils.nullImage(img);
		if(test==1) {//Test from a point cloud
			
			double[]xCoordinates=new double[] {90,100,110,115,116,120};
			double[]yCoordinates=new double[] {90,110,90,110,90,110};
			double[]circle=smallestEnclosingCircle(xCoordinates,	yCoordinates,standardThresholdOutlier,0,null)[0];
			img=VitimageUtils.drawCircleInImage(img, circle[2],(int)circle[0],(int)circle[1],0,255);
			for(int i=0;i<xCoordinates.length;i++) {				
				img=VitimageUtils.drawCircleNoFillInImage(img, 3,(int)xCoordinates[i],(int)yCoordinates[i],0,155,2);
			}
			img.show();
		}
		if(test==2) {//Test from a random binary image
			Random rand=new Random();
			int N=500;
			for(int n=0;n<N;n++) {
				double x0=rand.nextGaussian()*20+100;
				double y0=rand.nextGaussian()*20+100;
				img=VitimageUtils.drawCircleInImage(img, 3,(int)x0,(int)y0,0,180);
			}
			
			Timer t=new Timer();
			double[]circle=(double[]) smallestEnclosingCircle(img,true)[0];
			t.print("It s time");
			img=VitimageUtils.drawCircleNoFillInImage(img, circle[2],(int)circle[0],(int)circle[1],0,255,1);
			img.show();
		}

		if(test==3) {//Test from a random binary image
			Random rand=new Random();
			int N=50;
			for(int n=0;n<N;n++) {
				double x0=Math.abs(rand.nextGaussian())*20+100;
				double y0=rand.nextGaussian()*20+100;
				img=VitimageUtils.drawCircleInImage(img, 3,(int)x0,(int)y0,0,180);
			}
			
			Timer t=new Timer();
			double[]circle=(double[]) smallestEnclosingCircle(img,true)[0];
			t.print("It s time");
			img=VitimageUtils.drawCircleNoFillInImage(img, circle[2],(int)circle[0],(int)circle[1],0,255,1);
			img.show();
		}
		if(test==4) {//Test from a random binary image
			Random rand=new Random();
			int N=500;
			double angleMin=0;
			double angleMax=3.14;
			for(int n=0;n<N;n++) {
				double a0=Math.abs(rand.nextDouble())*angleMax;
				double d0=rand.nextDouble()*50;
				double x0=100+Math.cos(a0)*d0*1.1;
				double y0=100+Math.sin(a0)*d0;
				img=VitimageUtils.drawCircleInImage(img, 1,(int)x0,(int)y0,0,180);
			}
			
			Timer t=new Timer();
			double[]circle=(double[]) smallestEnclosingCircle(img,true)[0];
			t.print("It s time");
			img=VitimageUtils.drawCircleNoFillInImage(img, circle[2],(int)circle[0],(int)circle[1],0,255,1);
			img.show();
		}
		if(test==5) {//Test from an actual binary image
			int indexTest=4;
			//0.068  0.031  0.086  0.11  0.057
			String[]tests= {"2016_G80_P1_E11","2017_G1_P1_E17","2017_G26_P6_E17","2017_G80_P5_E20","2018_G80_P3_E15"};
			String[]otherTests= {"2018_G80_P2_E10","2017_G80_P5_E12","2018_G01_P3_E15","2019_G26_P83_E9","2019_G26_P29_E9"};
			//ImagePlus imgReal=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Test/03_slice_seg/Segmentation/"+tests[indexTest]+".tif");
			
			double tmp=standardThresholdOutlier;
			standardThresholdOutlier=0.25;
			//double[]circle=smallestEnclosingCircle(imgReal);
			//imgReal=VitimageUtils.drawCircleNoFillInImage(imgReal, circle[2],(int)circle[0],(int)circle[1],0,255,1);
			//imgReal.duplicate().show();
			standardThresholdOutlier/=2;
			for(int j=0;j<10;j++) {
				System.out.print("Going for "+standardThresholdOutlier);
				ImagePlus imgReal=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Test/03_slice_seg/Segmentation/"+tests[indexTest]+".tif");
				Object[]obj=smallestEnclosingCircle(imgReal,true);
				double[]circle=(double[]) obj[0];
				ImagePlus excluded=(ImagePlus) obj[1];
				double recIndex=circle[circle.length-1];
				imgReal=VitimageUtils.drawCircleNoFillInImage(excluded, circle[2],(int)circle[0],(int)circle[1],0,255,2);
				//ImagePlus t=imgReal.duplicate();
				imgReal.setTitle(""+standardThresholdOutlier);
				imgReal.duplicate().show();
				System.out.println(" index="+recIndex);
				VitimageUtils.waitFor(2000);
				standardThresholdOutlier/=2;
			}
		}

		
	}

	/* Assume there is multiple objects here, and that we look for the min circle that surround all the centers*/
	public static Object[]smallestEnclosingCircle(ImagePlus binary, boolean excludePointsThatDontLieOnAgoodCircle){
		if(!excludePointsThatDontLieOnAgoodCircle)standardThresholdOutlier=100;
		ImagePlus labels=VitimageUtils.connexeBinaryEasierParamsConnexitySelectvol(binary, 6, 0);
		double[][][]coordsArea=SegmentationUtils.inertiaComputation(labels,false,false,true);
		double[]xCoordinates=new double[coordsArea.length];
		double[]yCoordinates=new double[coordsArea.length];
		for(int i=0;i<coordsArea.length;i++) {
			xCoordinates[i]=coordsArea[i][0][1];
			yCoordinates[i]=coordsArea[i][0][2];
			//System.out.println("Point : "+xCoordinates[i]+" , "+yCoordinates[i]);
		}
		//VitimageUtils.waitFor(5000000);
		double[][]circleCoords=smallestEnclosingCircle(xCoordinates,yCoordinates,standardThresholdOutlier,0,null);
//		labels.duplicate().show();
		for(int i=0;i<circleCoords[1].length;i++) {
			System.out.println("Was excluded :  "+circleCoords[1][i]+" with coordinates = "+xCoordinates[(int) circleCoords[1][i]]+" , "+yCoordinates[(int) circleCoords[1][i]]);
			labels=VitimageUtils.switchValueInImage(labels, (int)Math.round(circleCoords[1][i])+1, 0);
		}
		System.out.println("Smallest circle computation. "+circleCoords[1].length +" connected components excluded");
		labels=VitimageUtils.thresholdImage(labels, 0.5, 1E100);
		IJ.run(labels,"8-bit","");			
		return new Object[] {circleCoords[0],labels};
	}
	
	public static double[][] smallestEnclosingCircle(double[]xC,double[]yC,double varThresholdOutlier,int recursivityIndex,boolean[]activePoints) {
		if(activePoints==null) {
			activePoints=new boolean[xC.length];
			for(int i=0;i<xC.length;i++)activePoints[i]=true;
		}
//		System.out.println("Debug at recIndex="+recursivityIndex+" : "+getIndexesFalse(activePoints).length);
		double[]xCoordinates=copyTabWithInclusionList(xC, activePoints);
		double[]yCoordinates=copyTabWithInclusionList(yC, activePoints);
		double[]vals=smallestEnclosingCircleNoOutlierCheck(xCoordinates,yCoordinates);
		double radiusAll=vals[2];
		int []indexes=new int[] {(int) Math.round(vals[3]),(int) Math.round(vals[4]),(int) Math.round(vals[5])};
		double [][]xCoordTmp=new double[3][];
		double [][]yCoordTmp=new double[3][];
		double[][]valsLess=new double[3][];

		double maxVar=0;
		int indexMaxVar=0;
		for(int i=0;i<3;i++){
			boolean[]tabTmp=copyTab(activePoints);
			tabTmp[indexes[i]]=false;
			xCoordTmp[i]=copyTabWithInclusionList(xC, tabTmp);
			yCoordTmp[i]=copyTabWithInclusionList(yC, tabTmp);
			valsLess[i]=smallestEnclosingCircleNoOutlierCheck(xCoordTmp[i], yCoordTmp[i]);
			double var=Math.abs(valsLess[i][2]-radiusAll)/radiusAll;
			if(var>maxVar) {maxVar=var;indexMaxVar=i;}
		}
		
		if(maxVar>varThresholdOutlier) {
//			System.out.println("goind one more, after hunting "+indexes[indexMaxVar]+" tot bef exc="+getIndexesFalse(activePoints).length);
			activePoints[indexes[indexMaxVar]]=false;
//			System.out.println("tot aft exc="+getIndexesFalse(activePoints).length+ TransformUtils.stringVectorN(getIndexesFalse(activePoints), ""));
			return smallestEnclosingCircle(xC,yC,varThresholdOutlier,recursivityIndex+1,activePoints);
		}
		else {
			double[]indexesFalse=getIndexesFalse(activePoints);
//			System.out.println("Finish : "+TransformUtils.stringVectorN(getIndexesFalse(activePoints), ""));
			return new double[][] {copyTabMoreOne(vals,recursivityIndex),indexesFalse};
		}
	}

	static boolean[]copyTab(boolean[]tab){
		boolean[]ret=new boolean[tab.length];
		for(int i=0;i<tab.length;i++)ret[i]=tab[i];
		return ret;
	}
	
	static double []getIndexesFalse(boolean[]list) {
		int nAct=0;
		for(int i=0;i<list.length;i++)if(!list[i])nAct++;
		double[]ret=new double[nAct];
		nAct=0;
		for(int i=0;i<list.length;i++)if(!list[i])ret[nAct++]=i;		
		return ret;	
	}
	
	static double[]copyTabMoreOne(double[]tab,double valInsertEnd){
		double[]ret=new double[tab.length+1];
		for(int i=0;i<tab.length;i++)ret[i]=tab[i];
		ret[tab.length]=valInsertEnd;
		return ret;		
	}
	
	static double[]copyTabWithInclusionList(double[]tab,boolean[]list){
		int nAct=0;
		for(int i=0;i<list.length;i++)if(list[i])nAct++;
		double[]ret=new double[nAct];
		nAct=0;
		for(int i=0;i<list.length;i++)if(list[i])ret[nAct++]=tab[i];		
		return ret;
	}
	
	
	static double[]copyTab(double[]tab){
		double[]ret=new double[tab.length];
		for(int i=0;i<tab.length;i++)ret[i]=tab[i];
		return ret;
	}
	
	static double[]copyTabLessOne(double[]tab,int indexToExclude){
		double[]ret=new double[tab.length-1];
		for(int i=0;i<ret.length;i++)ret[i]=( (i<indexToExclude) ? tab[i] : tab[i+1]);
		return ret;
	}
	
	public static double[]smallestEnclosingCircleNoOutlierCheck(double[]xCoordinates, double[]yCoordinates){
  	  int[] fourIndices = new int[4];
	  int n = xCoordinates.length;
/*	  if (n==1)
	    return newArray(xCoordinates[0], yCoordinates[0], 0);
	  else if (n==2)
	    return circle2(xCoordinates[0], yCoordinates[0], xCoordinates[1], yCoordinates[1]);
	  else if (n==3)
	    return circle3(xCoordinates[0], yCoordinates[0], xCoordinates[1], yCoordinates[1], xCoordinates[2], yCoordinates[2]);*/
	  //As starting point, find indices of min & max x & y
	  double xmin = 999999999; double ymin=999999999; 
	  double xmax=-1; double ymax=-1;
	  for (int i=0; i<n; i++) {
	    if (xCoordinates[i]<xmin) {xmin=xCoordinates[i]; fourIndices[0]=i;}
	    if (xCoordinates[i]>xmax) {xmax=xCoordinates[i]; fourIndices[1]=i;}
	    if (yCoordinates[i]<ymin) {ymin=yCoordinates[i]; fourIndices[2]=i;}
	    if (yCoordinates[i]>ymax) {ymax=yCoordinates[i]; fourIndices[3]=i;}
	  }
	  boolean retry=true;
	  double radius=0;
	  double xcenter=0;
	  double ycenter=0;
	  int badIndex=0;
	  do {
	    double[]tmp=circle4(xCoordinates, yCoordinates,fourIndices);  //get circle through points listed in fourIndices
	    badIndex=(int)Math.round(tmp[3]);
	    xcenter=tmp[0];
	    ycenter=tmp[1];
	    radius=tmp[2];
	    int newIndex = -1;
	    double largestRadius = -1;
	    for (int i=0; i<n; i++) {      //get point most distant from center of circle
	      double r = vecLength(xcenter-xCoordinates[i], ycenter-yCoordinates[i]);
	      if (r > largestRadius) {
	        largestRadius = r;
	        newIndex = i;
	      }
	    }
	    //print(largestRadius);
	    retry = (largestRadius > radius*1.0000000000001);
	    fourIndices[badIndex] = newIndex; //add most distant point
	  } while (retry);
	  //fourIndices has indices of three points making a englobant circle, and a bad index = -1;

	  int []tabInd=new int[3];
	  int incr=0;
	  for(int i=0;i<4;i++)if(i!=badIndex)tabInd[incr++]=fourIndices[i];
	  double diameter = Math.round(2*radius);
	  return new double[] {xcenter,ycenter,radius,tabInd[0],tabInd[1],tabInd[2]};
	}


	//circle spanned by diameter between two points.
	static double[] circle2(double xa,double ya,double xb,double yb) {
	  double xcenter = 0.5*(xa+xb);
	  double ycenter = 0.5*(ya+yb);
	  double radius = 0.5*vecLength(xa-xb, ya-yb);
	  return new double[] {xcenter,ycenter,radius};
	}
	
	//smallest circle enclosing 3 points.
	static double[] circle3(double xa,double ya,double xb,double yb,double xc,double yc) {
		double xab = xb-xa; double yab = yb-ya; double c = vecLength(xab, yab);
		double xac = xc-xa; double yac = yc-ya; double b = vecLength(xac, yac);
		double xbc = xc-xb; double ybc = yc-yb; double a = vecLength(xbc, ybc);
	  if (b==0 || c==0 || a*a>=b*b+c*c) return circle2(xb,yb,xc,yc);
	  if (b*b>=a*a+c*c) return circle2(xa,ya,xc,yc);
	  if (c*c>=a*a+b*b) return circle2(xa,ya,xb,yb);
	  double d = 2*(xab*yac - yab*xac);
	  double xcenter = xa + (yac*c*c-yab*b*b)/d;
	  double ycenter = ya + (xab*b*b-xac*c*c)/d;
	  double radius = vecLength(xa-xcenter, ya-ycenter);
	  return new double[] {xcenter,ycenter,radius};
	}
	
	
	//Get enclosing circle for 4 points of the x, y array and return which
	//of the 4 points we may eliminate
	//Point indices of the 4 points are in global array fourIndices
	static double[]circle4(double []x, double []y,int[]fourIndices) {
	  double[]rxy = new double[12]; //0...3 is r, 4...7 is x, 8..11 is y
	  double[]tmp=circle3(x[fourIndices[1]], y[fourIndices[1]], x[fourIndices[2]], y[fourIndices[2]], x[fourIndices[3]], y[fourIndices[3]]);
	  rxy[0] = tmp[2]; rxy[4] = tmp[0]; rxy[8] = tmp[1];
	  tmp=circle3(x[fourIndices[0]], y[fourIndices[0]], x[fourIndices[2]], y[fourIndices[2]], x[fourIndices[3]], y[fourIndices[3]]);
	  rxy[1] = tmp[2]; rxy[5] = tmp[0]; rxy[9] = tmp[1];
	  tmp=circle3(x[fourIndices[0]], y[fourIndices[0]], x[fourIndices[1]], y[fourIndices[1]], x[fourIndices[3]], y[fourIndices[3]]);
	  rxy[2] = tmp[2]; rxy[6] = tmp[0]; rxy[10] = tmp[1];
	  tmp=circle3(x[fourIndices[0]], y[fourIndices[0]], x[fourIndices[1]], y[fourIndices[1]], x[fourIndices[2]], y[fourIndices[2]]);
	  rxy[3] = tmp[2]; rxy[7] = tmp[0]; rxy[11] = tmp[1];
	  double radius = 0;
	  int badIndex=0;
	  for (int i=0; i<4; i++)
	    if (rxy[i]>radius) {
	      badIndex = i;
	      radius = rxy[badIndex];
	    }
	  double xcenter = rxy[badIndex + 4]; double ycenter = rxy[badIndex + 8];
	  return new double[] {xcenter,ycenter,radius,badIndex};
	}

	static double vecLength(double dx, double dy) {
	  return Math.sqrt(dx*dx+dy*dy);
	}

}
