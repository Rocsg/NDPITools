package fr.cirad.image.sorghobff;

import java.io.File;

import fr.cirad.image.common.Timer;
import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.mlutils.SegmentationUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.plugin.frame.PlugInFrame;
import trainableSegmentation.WekaSegmentation;

public class Matthieu_BatchProcessing extends PlugInFrame{
        private static final long serialVersionUID = 1L;

        
        /**  ---------------------------------------- Constructors and entry points -----------------------------------------------*/
        public Matthieu_BatchProcessing(String title) {
                super(title);
        }

    public Matthieu_BatchProcessing() {
            super("");
    }
    
        //This method is entry point when testing from Eclipse
    public static void main(String[] args) {
                @SuppressWarnings("unused")
                ImageJ ij=new ImageJ();        
                new Matthieu_BatchProcessing().run("");
            
            
    }
        
        //This method is entry point when testing from Fiji
        public void run(String arg) {
                //batchVesselDetection_Step1ProbaMap("D:/DONNEES/Sorgho_BFF/Data/Images_resamp8_test","D:/DONNEES/Sorgho_BFF/Data/ProbaMapMerged");
                batchVesselSegmentation("E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/Sorgho_BFF","E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/Sorgho_BFF/Data/Images_resampled8", "E:/DONNEES/Matthieu/Projet_VaisseauxSorgho/Sorgho_BFF/Data/ProbaMap_Segmentation");
        }
        
        
        public static void batchVesselSegmentation(String vesselsDir,String inputDir,String outputDir) {
                 String[]imgNames=new File(inputDir).list();
                 ImagePlus imgTest=IJ.openImage(new File(inputDir,imgNames[0]).getAbsolutePath());
                 IJ.run(imgTest,"8-bit","");
                 String[]modelPaths=new String[6];
                 for(int i=0;i<6;i++) {
                         modelPaths[i]=vesselsDir+"/Data/Processing/Step_01_detection/Models/model_layer_1"+("_AUGSET"+i+".model");
                 }
                 WekaSegmentation[]wekas=SegmentationUtils.initModels(imgTest,SegmentationUtils.getStandardRandomForestParams(1), SegmentationUtils.getStandardRandomForestFeatures(), modelPaths);

                 IJ.log("Starting batch processing ");        
                 Timer t= new Timer();
                 for(int indImg=0;indImg<imgNames.length;indImg++) {
                         t.print("Starting ML processing image "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
                         ImagePlus imgInit=IJ.openImage(new File(inputDir,imgNames[indImg]).getAbsolutePath());
                         ImagePlus[]results=new ImagePlus[6];
                         for(int m=0;m<6;m++) {
                                 t.print("--"+m);
                                 ImagePlus img=imgInit.duplicate();
                                 if(m<3)img=VitimageUtils.splitRGBStackHeadLess(img)[(m)];//RGB
                    else img=VitimageUtils.getHSB(img)[(m-3)];//HSB
                                 results[m]=wekas[m].applyClassifier(img,0,true);
                         }
                         ImagePlus result=VitimageUtils.meanOfImageArray(results);
                         result=new Duplicator().run(result,2,2,1,1,1,1);
                         results=null;
                         VitimageUtils.garbageCollector();
                         IJ.save(result, new File(outputDir,"ProbaMap_"+imgNames[indImg]).getAbsolutePath());
                         t.print("Starting segmentation image "+(indImg+1)+"/"+imgNames.length+" : "+imgNames[indImg]);
                         ImagePlus binary=SegmentationUtils.getSegmentationFromProbaMap3D(result,0.5,0.7);
                         IJ.save(binary, new File(outputDir,"Segmentation_"+imgNames[indImg]).getAbsolutePath());
                 }
        } 
} 