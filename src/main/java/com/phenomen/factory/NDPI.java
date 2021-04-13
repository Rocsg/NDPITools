package com.phenomen.factory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import com.phenomen.ndpilegacy.ExtractNDPI;
import com.phenomen.ndpilegacy.JAIReader;
import com.phenomen.ndpilegacy.NDPISplit;
import com.vitimage.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import jogamp.opengl.glu.mipmap.Image;

public class NDPI {
	int N;
	double[]magnifications;
	double[]fileSizeMB;
	int[][] sizes;
	public String sourcePath;
	private double factorFromPreviewToLargestImage;
	private String typeOfPreviewImage;
	public double previewMagnification;
	public ImagePlus previewImage;
	private double[][] voxelSizes;
	public int previewLevelN;

	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		NDPI ndpi=new NDPI("/home/fernandr/Bureau/NDPIBordel/G1P2E15_G1P2E17.ndpi");
		System.out.println(ndpi);
		ndpi.previewImage.show();
		ImagePlus img=ndpi.getExtract(0, 302, 125, 1, 1);
		img.show();
	}

	public NDPI() {
		OpenDialog od1=new OpenDialog("Select NDPI file");
		setupInfoAndPreview(od1.getPath());
		System.out.println(this);
 	}

	public NDPI(String path) {
		setupInfoAndPreview(path);
	}
	
	
	public String cleanDouStr(double d) {
		if(Math.abs((int)Math.round(d)-d)<0.00000001)return (""+((int)Math.round(d)));
		else return ""+d;
	}
	
	public ImagePlus getExtract(int resolution) {
		return getExtract(resolution,0,0,this.previewImage.getWidth(),this.previewImage.getHeight());
	}
		
	public ImagePlus getExtract(int resolution,double x0_pix,double y0_pix, double dx_pix, double dy_pix) {
    	if(resolution>=N)return null;
    	if((x0_pix<0) || (y0_pix<0))return null;
    	if((x0_pix+dx_pix>previewImage.getWidth()) || (y0_pix+dy_pix>previewImage.getHeight()))return null;

    	ImagePlus extract=null;
		String strOut=null;
		String[]args=new String[] {"-vv","-K","-x"+this.magnifications[resolution],
				"-e"+this.getEquivalentDoubleX(x0_pix)+","+this.getEquivalentDoubleY(y0_pix)+","+this.getEquivalentDoubleX(dx_pix)+","+this.getEquivalentDoubleY(dy_pix)+",TEMP",
				this.sourcePath};
		for(int i=0;i<args.length;i++) {
		}
		String[] res=null;
		try {res = NDPISplit.run(args,false);} catch (Exception e) {e.printStackTrace();}
        for(int i=0;i<res.length;i++) {
     		int pos=res[i].indexOf(':');
     		if(pos<0)continue;
         	if(res[i].contains("containing a TIFF scanned") ) {
         		strOut=res[i].substring(pos+1);
         	}
        }
        strOut=this.sourcePath.replace(".ndpi","_x"+cleanDouStr(this.magnifications[resolution])+"_z0_TEMP.tif");
        if(!new File(strOut).exists())strOut=this.sourcePath.replace(".ndpi","_x"+cleanDouStr(this.magnifications[resolution])+"_z0_1_TEMP.tif");
        
        JAIReader reader;
		try {
			File f=new File(strOut);
			reader = new JAIReader(f);
            extract = reader.readFirstImage();
            reader.close();
          
        } catch (Exception e) {e.printStackTrace();	}
        
        if(!strOut.contains(".tif")){IJ.log("Warning, return string "+strOut+" does not contain tif");return null;}
    	VitimageUtils.waitFor(150);
    	try {
			java.nio.file.Files.delete(new File(strOut).toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//        extract.setProperty("PreviewOfNDPIPath", this.sourcePath);
        extract.setTitle("Extract of "+new File(this.sourcePath).getName()+" - "+"Magnification = "+magnifications[resolution]+"x,  x0="+x0_pix+" y0="+y0_pix+" dx="+dx_pix+" dy="+dy_pix);
        extract.getStack().setSliceLabel("Magnification = x"+magnifications[resolution]+" x0="+x0_pix+" y0="+y0_pix+" dx="+dx_pix+" dy="+dy_pix , 1);
        return extract;
	}         
         	
	
	
	public void setupInfoAndPreview(String path) {
		this.sourcePath=path;
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		String[] args = new String[] {"-K",  "-p0," + screen.width + "x" + screen.height,  path};
		String[] res=null;
		try {res = NDPISplit.run(args, false);} catch (Exception e) {e.printStackTrace();}

		ArrayList<ImagePlus> imageList = new ArrayList<ImagePlus>();
        String pathOfFilePreview=null;
        String pathOfFileContainingMap=null;
		String availableMagnifications=null;
		String availableDimensions=null;
        
		int incr=0;
		for(String s : res) IJ.log("Line "+(incr++)+" : "+s);
		for (int i = 0 ; i < res.length ; i++)        {
            int pos = res[i].indexOf(':');
            if (pos < 0) continue;
			if (res[i].startsWith("Factor from preview image to largest image:"))
                factorFromPreviewToLargestImage = Double.parseDouble(res[i].substring(pos+1));
            else if (res[i].startsWith("Type of preview image:")) {
            	previewMagnification = Double.parseDouble(res[i].substring(pos+1).replace("x", ""));
            	typeOfPreviewImage = res[i].substring(pos+1);
            }
            else if (res[i].startsWith("File containing map:"))   pathOfFileContainingMap = res[i].substring(pos+1);
            else if (res[i].startsWith("Found images at magnifications:"))  availableMagnifications = res[i].substring(pos+1);
            else if (res[i].startsWith("Found images of sizes:")) availableDimensions = res[i].substring(pos+1);
            else if (res[i].startsWith("File containing a preview image:")) {
                pathOfFilePreview = res[i].substring(pos+1);
                File file = new File(res[i].substring(pos+1));
                try  {
                    JAIReader reader = new JAIReader(file);
                    ImagePlus image = reader.readFirstImage();
                    reader.close();
                    if (image != null) {
                        image.setProperty("PreviewOfNDPIType",typeOfPreviewImage);
                        image.setProperty("PreviewOfNDPIRatio", new Double(factorFromPreviewToLargestImage));
                        image.setProperty("PreviewOfNDPIAvailableMagnifications",availableMagnifications);
                        image.setProperty("PreviewOfNDPIAvailableDimensions", availableDimensions);
                        this.previewImage=image;
                        imageList.add(image);
                    }
                } catch (Exception ex) {  ex.printStackTrace();  }
            }
        }
        if (imageList.size() != 0)  previewImage= JAIReader.combineImagesIntoStack(imageList,"")[0];
        if( (pathOfFileContainingMap == null) || (pathOfFilePreview == null) || (!(pathOfFileContainingMap.contains(".tif"))) || (!(pathOfFilePreview.contains(".tif"))) ) {
        	IJ.showMessage("Something was wrong about get Info. No cleaning of maps will be done\n"+pathOfFileContainingMap+"\n"+pathOfFilePreview);
        	return;
        }
        
    	VitimageUtils.waitFor(150);
    	try {
			java.nio.file.Files.delete(new File(pathOfFileContainingMap).toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	VitimageUtils.waitFor(150);
    	try {
			java.nio.file.Files.delete(new File(pathOfFilePreview).toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        setNumberMagnificationsSizes(availableMagnifications,availableDimensions);
        this.previewImage.setProperty("PreviewOfNDPIPath", this.sourcePath);
        this.previewImage.setTitle("Extract of "+new File(this.sourcePath).getName()+" - "+"Magnification = "+magnifications[previewLevelN]+"x, fullsize");
        this.previewImage.getStack().setSliceLabel("Magnification = x"+magnifications[previewLevelN]+" fullsize", 1);
	}

	public void setNumberMagnificationsSizes(String magStr,String dimStr) {
		String[]mags=magStr.replace("x", "").split(",");
		this.magnifications=new double[mags.length];
		for(int i=0;i<magnifications.length;i++) {
			this.magnifications[i]=Double.parseDouble(mags[i]);
		}
		String[]dims=dimStr.split(",");
		this.sizes=new int[dims.length][2];
		this.fileSizeMB=new double[dims.length];
		for(int i=0;i<sizes.length;i++) {
			this.sizes[i]=new int[] {Integer.parseInt(dims[i].split("x")[0]),Integer.parseInt(dims[i].split("x")[1])};

			this.fileSizeMB[i]=this.sizes[i][0]*this.sizes[i][1]*0.000003;
		}
		if(this.sizes.length != this.magnifications.length) {
			IJ.showMessage("Informations tab mismatch. Abort");
			System.exit(0);
		}
		this.N=this.sizes.length;
		this.voxelSizes=new double[N][2];
		double[]voxelSizePreview=new double[] {this.previewImage.getCalibration().pixelWidth,this.previewImage.getCalibration().pixelHeight};
		this.previewLevelN=0;
		for(int i=0;i<N;i++) {
			if(this.previewMagnification==this.magnifications[i])this.previewLevelN=i;
		}
		for(int i=0;i<N;i++) {
			this.voxelSizes[i][0]=voxelSizePreview[0]*this.magnifications[this.previewLevelN]/this.magnifications[i];
			this.voxelSizes[i][1]=voxelSizePreview[0]*this.magnifications[this.previewLevelN]/this.magnifications[i];
		}
	}


	public int getLevelForTargetMagnitude(double target,boolean nearest) {
		int iBest=nearest ? 0: 0;
		double delta=10E80;
		double thisDelta=0;
		for(int i=0;i<N;i++) {
			if(nearest) {
				thisDelta=Math.abs(magnifications[i]-target)/target;
				if(thisDelta<delta) {iBest=i;delta=thisDelta;}
			}
			else {
				if(this.magnifications[i]>target)iBest=i;
			}
		}
		return iBest;
	}

	public int getLevelForTargetVoxelSize(double targetVox,boolean nearest) {
		int iBest=nearest ? 0: 0;
		double delta=10E8;
		for(int i=0;i<N;i++) {
			if(nearest) {
				double thisDelta=Math.abs(voxelSizes[i][0]-targetVox)/targetVox;
				if(thisDelta<delta) {iBest=i;delta=thisDelta;}
			}
			else {
				if(this.voxelSizes[i][0]<targetVox)iBest=i;
			}
		}
		return iBest;
	}

	
	public double getEquivalentDoubleX(double xInPixels) {
		return xInPixels/this.previewImage.getWidth();
	}
	
	public double getEquivalentDoubleY(double yInPixels) {
		return yInPixels/this.previewImage.getHeight();
	}

	public String toString() {
		String s="------ NDPI file "+this.sourcePath+" --------------- \n";
		s+="N resolutions="+N+"\n";
		for(int i=0;i<N;i++) {
			if(i==this.previewLevelN)s+="[[ ";			
			s+="Resolution "+i+": magnification="+this.magnifications[i]+"     Vx,Vy=["+IJ.d2s(this.voxelSizes[i][0],5,2)+","+IJ.d2s(this.voxelSizes[i][1],5,2)+"]"+this.previewImage.getCalibration().getUnit()+"     Dims=["+this.sizes[i][0]+" x "+this.sizes[i][1]+"]"+"   Target size="+IJ.d2s(this.fileSizeMB[i])+" MB";
			if(i==this.previewLevelN)s+=" ]] PREVIEW RESOLUTION";
			s+="\n";
		}
		return s;
	}
	
}
