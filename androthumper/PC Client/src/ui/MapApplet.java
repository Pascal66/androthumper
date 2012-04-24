package ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import codeanticode.glgraphics.GLConstants;
import de.fhpotsdam.unfolding.Map;
import de.fhpotsdam.unfolding.examples.fun.Circle;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.mapdisplay.MapDisplayFactory;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.utils.GeoUtils;
import de.fhpotsdam.unfolding.utils.MapUtils;
import processing.core.PApplet;

public class MapApplet extends PApplet{

	private static final List<Float> DISPLAY_DISTANCES = Arrays.asList(0.01f, 0.02f, 0.05f, 0.1f, 0.2f, 0.5f, 1f, 2f,
			5f, 10f, 20f, 50f, 100f, 200f, 500f, 1000f, 2000f, 5000f);
	private static final float MAX_DISPLAY_DISTANCE = 5000;

	// If false it uses the screen center, resulting in a bar scale depending on the north/south position of the map.
	boolean showDistanceAtEquator = true,pressedOnDot = false;

	Map map,map1,map2;
	private float lat = 52.41892f,lng = -4.081652f,accuracy=20;
	private Location currentLocation,pressedLocation;
	private Vector<Location> points = new Vector<Location>();
	private Window window;
	private Location highlightedLocation1, highlightedLocation2;

		public MapApplet(Window window, float lat, float lng, Vector<float[]> points){
			currentLocation = new Location(lat,lng);
			this.window = window;
			
			for(int i = 0; i < points.size(); i++){
				float[] pair = points.get(i);
				this.points.add(new Location(pair[0],pair[1]));
			}
		}

	public void setup() {
		size(800, 600, GLConstants.GLGRAPHICS);
		currentLocation = new Location(lat,lng);
		map1 = new Map(this, new Google.GoogleMapProvider());
		map2 = new Map(this, new Microsoft.AerialProvider());

		map1.zoomAndPanTo(currentLocation, 15);
		map2.zoomAndPanTo(currentLocation, 15);

		MapUtils.createDefaultEventDispatcher(this, map1, map2);

		map = map1;
	}

	public void draw() {
		background(0);
		map.draw();

		Location last = null;
		float[] lastPos = null;
		fill(0,0,0,255);
		for(int i = 0; i < points.size(); i++){
			Location l = points.get(i);
			float[] screenPos = map.getScreenPositionFromLocation(l);

			if(last != null){
				line(screenPos[0],screenPos[1],lastPos[0],lastPos[1]);
			}

			ellipse(screenPos[0], screenPos[1], 10, 10);
			last = l;
			lastPos = screenPos;
		}

		fill(48,139,206,127);
		float[] myPos = map.getScreenPositionFromLocation(currentLocation);
		float zoom = 45000/map.getZoom();
		ellipse(myPos[0], myPos[1], accuracy/zoom, accuracy/zoom);
		fill(48,139,206,255);
		zoom = 200000/map.getZoom();
		ellipse(myPos[0],myPos[1],accuracy/zoom,accuracy/zoom);

		if(highlightedLocation1 != null){
			fill(27,224,50,255);
			float[] screenPos = map.getScreenPositionFromLocation(highlightedLocation1);
			ellipse(screenPos[0], screenPos[1], 10, 10);
		}
		if(highlightedLocation2 != null){
			fill(28,139,206,255);
			float[] screenPos = map.getScreenPositionFromLocation(highlightedLocation2);
			ellipse(screenPos[0], screenPos[1], 10, 10);
		}
		drawBarScale(20, map.mapDisplay.getHeight() - 20);
	}

	public void jumpToLocation(float lat, float lng){
		currentLocation = new Location(lat,lng);

		map1.zoomAndPanTo(currentLocation, 15);
		map2.zoomAndPanTo(currentLocation, 15);
	}

	public static void main(String[] args) {
		PApplet.main(new String[] { "ui.MapApplet" });
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1){
			handleLeftClick(e.getX(), e.getY());
		}else if(e.getButton() == MouseEvent.BUTTON3){
			handleRightClick(e.getX(), e.getY());
		}
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		if(pressedOnDot){
			Location newLocation = map.getLocationFromScreenPosition(e.getX(), e.getY());
			pressedLocation.setLat(newLocation.getLat());
			pressedLocation.setLon(newLocation.getLon());
		}else{
			super.mouseDragged(e);
		}
	}
	@Override
	public void mousePressed(MouseEvent e) {
		for(int i = 0; i <points.size(); i++){
			Location l = points.get(i);
			float[] screenPos = map.getScreenPositionFromLocation(l);
			if(dist(screenPos[0],screenPos[1],e.getX(),e.getY()) < 10){
				pressedOnDot = true;
				pressedLocation = l;
				printt("on dot: "+i);
				return;
			}
		}
		super.mousePressed(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		pressedOnDot = false;
		super.mouseReleased(e);
	}

	private void handleLeftClick(int x, int y){
		if(highlightedLocation1 != null){
			float[] screenPos = map.getScreenPositionFromLocation(highlightedLocation1);
			if(dist(screenPos[0],screenPos[1],x,y) < 10){
				highlightedLocation1 = null;
				return;
			}
		}
		if(highlightedLocation2 != null){
			float[] screenPos = map.getScreenPositionFromLocation(highlightedLocation2);
			if(dist(screenPos[0],screenPos[1],x,y) < 10){
				highlightedLocation2 = null;
				return;
			}
		}

			for(int i = 0; i <points.size(); i++){
				Location l = points.get(i);
				float[] screenPos = map.getScreenPositionFromLocation(l);
				if(dist(screenPos[0],screenPos[1],x,y) < 10){
					if(highlightedLocation1 == null){
						highlightedLocation1 = l;
					}else if(highlightedLocation2 == null){
						highlightedLocation2 = l;
					}else{
						highlightedLocation1 = null;
						highlightedLocation2 = null;
					}
					return;
				}
			}

			if(highlightedLocation1 != null && highlightedLocation2 != null){
				
				int index = points.indexOf(highlightedLocation1);
				if(index > points.indexOf(highlightedLocation2)){
					points.insertElementAt(map.getLocationFromScreenPosition(x, y), index);
				}else{
					points.insertElementAt(map.getLocationFromScreenPosition(x, y), points.indexOf(highlightedLocation2));
				}
				highlightedLocation1 = null;
				highlightedLocation2 = null;
			}else{
				points.add(map.getLocationFromScreenPosition(x, y));
			}
			
			if(window != null){
				window.setPoints(floatsFromLocations());
			}
		

	}
	private void handleRightClick(int x, int y){
		for(int i = 0; i <points.size(); i++){
			Location l = points.get(i);
			float[] screenPos = map.getScreenPositionFromLocation(l);
			if(dist(screenPos[0],screenPos[1],x,y) < 10){
				points.remove(l);
				if(window != null){
					window.setPoints(floatsFromLocations());
				}
			}
		}
	}

	public void keyPressed() {
		if (key == '1') {
			map = map1;
		} else if (key == '2') {
			map = map2;
		}
	}

	private Vector<float[]> floatsFromLocations(){
		Vector<float[]> newPoints = new Vector<float[]>();
		for(Location l:points){
			float[] pair = {l.getLat(),l.getLon()};
			newPoints.add(pair);
		}
		return newPoints;
	}

	/**
	 * Draws a bar scale at given position according to current zoom level.
	 * 
	 * Calculates distance at equator (scale is dependent on Latitude). Uses a distance to display from fixed set of
	 * distance numbers, so length of bar may vary.
	 * 
	 * @param x
	 *            Position to display bar scale
	 * @param y
	 *            Position to display bar scale
	 */
	public void drawBarScale(float x, float y) {

		// Distance in km, appropriate to current zoom
		float distance = MAX_DISPLAY_DISTANCE / map.getZoom();
		distance = getClosestDistance(distance);

		Location startLocation = null;
		Location destLocation = null;
		if (showDistanceAtEquator) {
			// Gets destLocation (world center, on equator, with calculated distance)
			startLocation = new Location(0, 0);
			destLocation = GeoUtils.getDestinationLocation(startLocation, 90f, distance);
		} else {
			startLocation = map.getLocationFromScreenPosition(width / 2, height / 2);
			destLocation = GeoUtils.getDestinationLocation(startLocation, 90f, distance);
		}
		// Calculates distance between both locations in screen coordinates
		float[] destXY = map.getScreenPositionFromLocation(destLocation);
		float[] startXY = map.getScreenPositionFromLocation(startLocation);
		float dx = destXY[0] - startXY[0];

		// Display
		stroke(30);
		strokeWeight(1);
		line(x, y - 3, x, y + 3);
		line(x, y, x + dx, y);
		line(x + dx, y - 3, x + dx, y + 3);
		fill(30);
		text(nfs(distance, 0, 0) + " km", x + dx + 3, y + 4);
	}

	/**
	 * Returns the nearest distance to display as well as to use for calculation.
	 * 
	 * @param distance
	 *            The original distance
	 * @return A distance from the set of {@link DISPLAY_DISTANCES}
	 */
	public float getClosestDistance(float distance) {
		return closest(distance, DISPLAY_DISTANCES);
	}

	public float closest(float of, List<Float> in) {
		float min = Float.MAX_VALUE;
		float closest = of;

		for (float v : in) {
			final float diff = Math.abs(v - of);

			if (diff < min) {
				min = diff;
				closest = v;
			}
		}

		return closest;
	}

	public void printt(String print){
		System.out.println(print);
	}

	@Override
	public InputStream createInputRaw(String filename) {
		InputStream stream = null;

		if (filename == null) return null;

		if (filename.length() == 0) {
			// an error will be called by the parent function
			//System.err.println("The filename passed to openStream() was empty.");
			return null;
		}

		// safe to check for this as a url first. this will prevent online
		// access logs from being spammed with GET /sketchfolder/http://blahblah
		if (filename.indexOf(":") != -1) {  // at least smells like URL
			try {
				URL url = new URL(filename);

				URLConnection uc = url.openConnection();
				uc.addRequestProperty("User-Agent", 
						"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");

				uc.connect();

				stream = uc.getInputStream();
				return stream;

			} catch (MalformedURLException mfue) {
				// not a url, that's fine

			} catch (FileNotFoundException fnfe) {
				// Java 1.5 likes to throw this when URL not available. (fix for 0119)
				// http://dev.processing.org/bugs/show_bug.cgi?id=403

			} catch (IOException e) {
				// changed for 0117, shouldn't be throwing exception
				e.printStackTrace();
				//System.err.println("Error downloading from URL " + filename);
				return null;
				//throw new RuntimeException("Error downloading from URL " + filename);
			}
		}

		// Moved this earlier than the getResourceAsStream() checks, because
		// calling getResourceAsStream() on a directory lists its contents.
		// http://dev.processing.org/bugs/show_bug.cgi?id=716
		try {
			// First see if it's in a data folder. This may fail by throwing
			// a SecurityException. If so, this whole block will be skipped.
			File file = new File(dataPath(filename));
			if (!file.exists()) {
				// next see if it's just in the sketch folder
				file = new File(sketchPath, filename);
			}
			if (file.isDirectory()) {
				return null;
			}
			if (file.exists()) {
				try {
					// handle case sensitivity check
					String filePath = file.getCanonicalPath();
					String filenameActual = new File(filePath).getName();
					// make sure there isn't a subfolder prepended to the name
					String filenameShort = new File(filename).getName();
					// if the actual filename is the same, but capitalized
					// differently, warn the user.
					//if (filenameActual.equalsIgnoreCase(filenameShort) &&
					//!filenameActual.equals(filenameShort)) {
					if (!filenameActual.equals(filenameShort)) {
						throw new RuntimeException("This file is named " +
								filenameActual + " not " +
								filename + ". Rename the file " +
								"or change your code.");
					}
				} catch (IOException e) { }
			}

			// if this file is ok, may as well just load it
			stream = new FileInputStream(file);
			if (stream != null) return stream;

			// have to break these out because a general Exception might
			// catch the RuntimeException being thrown above
		} catch (IOException ioe) {
		} catch (SecurityException se) { }

		// Using getClassLoader() prevents java from converting dots
		// to slashes or requiring a slash at the beginning.
		// (a slash as a prefix means that it'll load from the root of
		// the jar, rather than trying to dig into the package location)
		ClassLoader cl = getClass().getClassLoader();

		// by default, data files are exported to the root path of the jar.
		// (not the data folder) so check there first.
		stream = cl.getResourceAsStream("data/" + filename);
		if (stream != null) {
			String cn = stream.getClass().getName();
			// this is an irritation of sun's java plug-in, which will return
			// a non-null stream for an object that doesn't exist. like all good
			// things, this is probably introduced in java 1.5. awesome!
			// http://dev.processing.org/bugs/show_bug.cgi?id=359
			if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
				return stream;
			}
		}

		// When used with an online script, also need to check without the
		// data folder, in case it's not in a subfolder called 'data'.
		// http://dev.processing.org/bugs/show_bug.cgi?id=389
		stream = cl.getResourceAsStream(filename);
		if (stream != null) {
			String cn = stream.getClass().getName();
			if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
				return stream;
			}
		}

		try {
			// attempt to load from a local file, used when running as
			// an application, or as a signed applet
			try {  // first try to catch any security exceptions
				try {
					stream = new FileInputStream(dataPath(filename));
					if (stream != null) return stream;
				} catch (IOException e2) { }

				try {
					stream = new FileInputStream(sketchPath(filename));
					if (stream != null) return stream;
				} catch (Exception e) { }  // ignored

				try {
					stream = new FileInputStream(filename);
					if (stream != null) return stream;
				} catch (IOException e1) { }

			} catch (SecurityException se) { }  // online, whups

		} catch (Exception e) {
			//die(e.getMessage(), e);
			e.printStackTrace();
		}
		return null;
	}
}
