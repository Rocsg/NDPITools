package fr.cirad.image.test;

import java.util.ArrayList;

import ij.ImageJ;

public class Syr {

	public static void main(String[]args) {
		int F1=0;
		int F2=13;
		int N=(int)Math.pow(2,F2);
		int []hist=new int[100000000];
		
		for(int i=(int)Math.pow(2,F1); i<=(int)Math.pow(2,F2);i++) {
			Syr s=new Syr(i);
			s.computeSeq();
			System.out.println(s.shortString());
			hist[s.lastLastLast(9)]++;
		}
		for(int i=0;i<10000;i++)if(hist[i]>1)System.out.println("hist["+i+"]="+hist[i]);
	}
	
	
	
	int i;
	ArrayList<Integer>seq;
	public Syr(int i) {
		this.i=i;
		seq=new ArrayList<Integer>();
	}

	public int last() {return seq.get(seq.size()-1);}
	public int lastLast() {return seq.get(seq.size()-2);}
	public int lastLastLast(int n) {if(seq.size()<=n)return seq.get(0); return seq.get(seq.size()-n);}
	
	public void computeSeq() {
		int[]tabPow=new int[30];
		for(int i=0;i<30;i++)tabPow[i]=(int)Math.pow(2, i);
		boolean found=false;
		int n=i;
		seq.add(n);
		while(!found) {
			if(n%2==0)n=(n/2);
			else n=(3*n+1);
			seq.add(n);
			for(int p:tabPow)if(n==p || n==(p*10))found=true;
		}
	}
	
	public String toString() {
		String res="Syr("+i+").L="+seq.size()+" [";
		for(Integer i:seq)res+=" "+i;
		res+="]";
		return res;
	}
	public String shortString() {
		String res="Syr("+i+").L="+seq.size()+" ["+seq.get(seq.size()-1)+"]"+" -"+seq.get(seq.size()-2)+"-";
		return res;
	}
}
