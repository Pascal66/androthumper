package ui;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JFrame;


public class MapWindow extends JFrame{

	MapApplet app;
	
	public MapWindow(Window window, float lat, float lng, Vector<float[]> points){
		this.setSize(600, 600);
		this.setVisible(true);
		 setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		app = new MapApplet(window, lat, lng, points);
		this.add(app,BorderLayout.CENTER);
		//this.pack();
		app.init();
	}
	
	public void jumpToLocation(float lat, float lng){
		app.jumpToLocation(lat, lng);
	}
}
