package com.pair.jsoper;

/**
 * 12-Step Cloud Project
 * Image File Processing
 * Currently in Java, port to Scala
 * Runs in pure Java project, but not when merged with Scala
 */

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageFileDecoding {
	
	private void processBitmap(int[][] bmap) {
		System.out.println("proecess bitmap got called");
		int w = bmap.length;
		int h = bmap[0].length;
		for (int j = 0; j < w; j++) {
		    for (int k = 0; k < h; k++) {
		    	System.out.printf("%2d", bmap[k][j]);
		    }
		    System.out.println();		    	
		}	
	}
	
	@SuppressWarnings("unused")
	private void printDecodedArray(int[][] arr, int h, int w) {
		System.out.println("got called");
		System.out.println("ww: " + w + " h: " + h);
	    for (int k = 0; k < 2 * h + 1; k++) 
	    	System.out.printf("-");
		for (int j = 0; j < w; j++) {
		    System.out.print("|\n|");
		    for (int k = 0; k < h; k++) {
		    	int pixel = arr[k][j];
		    	char c = ' ';
		    	if (pixel > 2)
		    		 c = '*';
		    	System.out.printf("%2c", c);
		    	//System.out.printf(" %2d", pixel);
		    }
		}
	    System.out.println("|");
	    for (int k = 0; k < 2 * h + 1; k++) 
	    	System.out.printf("-");	
	}
	
	private int[][] decodeImageFile() {
		//BufferedImage img = ImageIO.read(new File("11.png"));
		//BufferedImage img = ImageIO.read(new File("basn0g02.png"));
		//BufferedImage img = ImageIO.read(new File("from_paint_net_bw.bmp"));
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File("data/G_152.gif"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int w = img.getWidth();
		int h = img.getHeight();
		int[][] arr = new int[w][h];
		int[][] bmap = new int[w][h];

		Raster raster = img.getData();
		for (int j = 0; j < w; j++) {
		    for (int k = 0; k < h; k++) {
		        arr[j][k] = raster.getSample(j, k, 0);
		        if (arr[j][k] > 2)
		        	bmap[j][k] = 1;
		        else
		        	bmap[j][k] = 0;
		    }
		}
		//printDecodedArray(arr, h, w);
		return bmap;		
	}
	
	private void go() {
		int[][] bmap = decodeImageFile();
		processBitmap(bmap);		
	}
	
	public static void main(String[] args) {
		// get out of static land
		new ImageFileDecoding().go();		
		System.out.println("\nfinished running ReadBMP");
	}

}


