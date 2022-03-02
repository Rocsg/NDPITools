package fr.cirad.image.sorghobff;

import fr.cirad.image.common.TransformUtils;
import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.registration.ItkTransform;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import math3d.Point3d;

public class Misc {
	public static void main(String []args) {
		ImageJ ij=new ImageJ();		

//		transformPartDown();
		transformPartUp();
	}
	public static void transformPartDown() {
		String dir="/home/rfernandez/Bureau/A_Test/Deform/";
		String pathInit=dir+"v2_down.tif";
		ImagePlus imgInit=IJ.openImage(pathInit);
		double[]vox=VitimageUtils.getVoxelSizes(imgInit);
		double imgSize=imgInit.getWidth()*vox[0];

		Point3d[][]corrDown=getCorrespondancesDown(vox[0], vox[1]);
		ImagePlus ref=imgInit.resize(1000, 1000, "bilinear");
		double sigma=imgSize/40;

		ItkTransform trDown=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(corrDown, ref, sigma);
		trDown.transformImage(imgInit, imgInit).show();
	}

	public static void transformPartUp() {
		String dir="/home/rfernandez/Bureau/A_Test/Deform/";
		String pathInit=dir+"v2_up.tif";
		ImagePlus imgInit=IJ.openImage(pathInit);
		double[]vox=VitimageUtils.getVoxelSizes(imgInit);
		double imgSize=imgInit.getWidth()*vox[0];

		Point3d[][]corrDown=getCorrespondancesUp(vox[0], vox[1]);
		ImagePlus ref=imgInit.resize(1000, 1000, "bilinear");
		double sigma=imgSize/40;

		ItkTransform trDown=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(corrDown, ref, sigma);
		trDown.transformImage(imgInit, imgInit).show();
	}

	
	
	public static Point3d [][] getCorrespondancesDown(double vx,double vy){
		int []yMove=new int[] {8556,7556,6556,5556,4556,3556,2556,1556,556};
		int []yNoMove=new int[] {9556,10556,11556,12556,13556,14556,15556};
		int targetYmove=8556;
		int deltaY=1000;
		int[][]vals= {
				{1280 ,8556 },
				{2200 ,8585 },
				{3150 ,8631 },
				{4100 ,8655  },
				{5050 ,8682 },
				{6000 ,8697  },
				{7000 ,8724 },
				{8000 ,8739  },
				{9000 ,8715  },
				{10000 ,8700 },
				{11000 ,8679 },
				{12000 ,8652 },
				{13000 ,8619 },
				{ 14000,8568 },
		};
		int nx=vals.length;
		int ny=yMove.length+yNoMove.length;
		Point3d[][]corrPoints=new Point3d[2][ny*nx];
		int iter=0;
		for(int x=0;x<nx;x++) {
			for(int y=0;y<yMove.length;y++) {
				deltaY=targetYmove-vals[x][1];
				corrPoints[0][iter]=new Point3d(vals[x][0]*vx,yMove[y]*vy+deltaY*vy,0);//origin
				corrPoints[1][iter]=new Point3d(vals[x][0]*vx,yMove[y]*vy,0);//origin
				iter++;
			}
			for(int y=0;y<yNoMove.length;y++) {
				corrPoints[0][iter]=new Point3d(vals[x][0]*vx,yNoMove[y]*vy,0);//origin
				corrPoints[1][iter]=new Point3d(vals[x][0]*vx,yNoMove[y]*vy,0);//origin
				iter++;
			}
		}
		return corrPoints;
	}

	
	
	public static Point3d [][] getCorrespondancesUp(double vx,double vy){
		int []yNoMove=new int[] {7474,6474,5474,4474,3474,2474,1474,474};
		int []yMove=new int[] {8474,9474,10474,11474,12474,13474,14474,15474};
		int targetYmove=8474;
		int deltaY=1000;
		int[][]vals= {
				{1294 ,8474  },
				{2200 ,8431  },
				{3150 ,8403  },
				{4100 ,8376   },
				{5050 ,8362   },
				{6000 ,8342   },
				{7000 ,8348  },
				{8000 ,8358   },
				{9000 ,8356    },
				{10000 ,8376  },
				{11000 ,8395  },
				{12000 ,8425  },
				{13000 ,8467  },
				{ 14000,8500  },
		};
		int nx=vals.length;
		int ny=yMove.length+yNoMove.length;
		Point3d[][]corrPoints=new Point3d[2][ny*nx];
		int iter=0;
		for(int x=0;x<nx;x++) {
			for(int y=0;y<yMove.length;y++) {
				deltaY=targetYmove-vals[x][1];
				corrPoints[0][iter]=new Point3d(vals[x][0]*vx,yMove[y]*vy+deltaY*vy,0);//origin
				corrPoints[1][iter]=new Point3d(vals[x][0]*vx,yMove[y]*vy,0);//origin
				iter++;
			}
			for(int y=0;y<yNoMove.length;y++) {
				corrPoints[0][iter]=new Point3d(vals[x][0]*vx,yNoMove[y]*vy,0);//origin
				corrPoints[1][iter]=new Point3d(vals[x][0]*vx,yNoMove[y]*vy,0);//origin
				iter++;
			}
		}
		return corrPoints;
	}

	
	
	
	
}
