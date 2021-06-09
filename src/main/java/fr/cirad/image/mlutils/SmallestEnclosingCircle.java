package fr.cirad.image.mlutils;

import java.util.Random;

import fr.cirad.image.common.Timer;
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
			double[]circle=smallestEnclosingCircle(xCoordinates,	yCoordinates);
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
			double[]circle=smallestEnclosingCircle(img);
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
			double[]circle=smallestEnclosingCircle(img);
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
			double[]circle=smallestEnclosingCircle(img);
			t.print("It s time");
			img=VitimageUtils.drawCircleNoFillInImage(img, circle[2],(int)circle[0],(int)circle[1],0,255,1);
			img.show();
		}
		if(test==5) {//Test from an actual binary image
			ImagePlus imgReal=IJ.openImage("/home/rfernandez/Bureau/A_Test/Vaisseaux/Data/Test/Vessels/3_slice_seg/Segmentation_T1_WD_G01P22E8_lev1.tif");

			Timer t=new Timer();
			double[]circle=smallestEnclosingCircle(imgReal);
			t.print("It s time");
			imgReal=VitimageUtils.drawCircleNoFillInImage(imgReal, circle[2],(int)circle[0],(int)circle[1],0,255,1);
			imgReal.show();
		}

		
	}

	/* Assume there is multiple objects here, and that we look for the min circle that surround all the centers*/
	public static double[]smallestEnclosingCircle(ImagePlus binary){
		Roi[]roiTab=SegmentationUtils.segmentationToRoi(binary);
		double[]xCoordinates=new double[roiTab.length];
		double[]yCoordinates=new double[roiTab.length];
		for(int i=0;i<roiTab.length;i++) {
			double[]cord=roiTab[i].getContourCentroid();
			xCoordinates[i]=cord[0];
			yCoordinates[i]=cord[1];
		}
		return smallestEnclosingCircle(xCoordinates,yCoordinates);
	}
	
	public static double[]smallestEnclosingCircle(double[]xCoordinates, double[]yCoordinates){
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
	  do {
	    double[]tmp=circle4(xCoordinates, yCoordinates,fourIndices);  //get circle through points listed in fourIndices
	    int badIndex=(int)Math.round(tmp[3]);
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
	  double diameter = Math.round(2*radius);
	  return new double[] {xcenter,ycenter,radius};
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
