package fr.cirad.image.test;

import java.util.Properties;

import com.aparapi.Kernel;
import com.aparapi.Range;

import io.github.rocsg.fijiyama.common.Timer;

public class TestAparapi {
	public static void main(String[]args) {
		System.out.println(p);
		//p.toString()
		//for(pp : p)System.out.println(pp);
		Timer t=new Timer();
		int N1=1;
		int N2=50;
		int N=(int) 1E8;
		t.print("Start");
		final float inA[] = getFloats(N);
		t.print("After genesis");		
		for(int i=0;i<N1;i++)computeBasicMonoCPU(inA,N2);
		t.print("After basic");
		for(int i=0;i<N1;i++)computeBasicGPU(inA,N2);
		t.print("After gpu");
	}
	public static void computeBasicMonoCPU(final float[]inA,int N2) {
		final float []result = new float[inA.length];
		for(int j=0;j<N2;j++)for (int i = 0; i < result.length; i++) {
		    result[i] = inA[i]+inA[i];
//		    result[i] = (float) Math.exp(inA[i]);
		}
	}


	public static void computeBasicGPU(final float[]inA,int N2) {
		final float []result = new float[inA.length];
		Kernel kernel = new Kernel() {
		    @Override
		    public void run() {
		        int i = getGlobalId();
		        result[i] = inA[i]+inA[i];
//		        result[i] = (float) Math.exp(inA[i]);
		    }
		};

		Range range = Range.create(result.length);
		for(int i=0;i<N2;i++)kernel.execute(range);
	}

	
	public static float[]getFloats(int N){
		float delta=(float) (Math.PI/7);
		float[] tabF=new float[N];
		for(int i=0;i<N;i++)tabF[i]=i%5+delta;
		return tabF;
	}
}
