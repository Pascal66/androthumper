package android.ioio.car.hardware;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import constants.Conts;

import android.graphics.AvoidXfermode;
import android.ioio.car.listeners.MyGPSListener;
import android.ioio.car.threads.ThreadManager;
import android.location.Location;
import android.util.Log;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;

public class GpsModule {
	/**Update rate. */
	public static final String PMTK_SET_NMEA_UPDATE_1HZ = "$PMTK220,1000*1F";
	/**Update rate. */
	public static final String PMTK_SET_NMEA_UPDATE_5HZ = "$PMTK220,200*2C";
	/**Update rate. */
	public static final String PMTK_SET_NMEA_UPDATE_10HZ = "$PMTK220,100*2F";

	/**Turn on only the second sentence (GPRMC). */
	public static final String PMTK_SET_NMEA_OUTPUT_RMCONLY = "$PMTK314,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0*29";
	/**Turn on GPRMC and GGA. */
	public static final String PMTK_SET_NMEA_OUTPUT_RMCGGA = "$PMTK314,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0*28";
	/**Turn on ALL THE DATA. */
	public static final String PMTK_SET_NMEA_OUTPUT_ALLDATA = "$PMTK314,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0*28";
	/**Turn off output. */
	public static final String PMTK_SET_NMEA_OUTPUT_OFF = "$PMTK314,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0*28";

	private IOIO ioio;
	public static final int GPS_PIN_RX = 3;
	public static final int GPS_PIN_TX = 4;
	public static final int GPS_BAUD = 9600;

	private InputStream is;
	private OutputStream os;
	private Uart uart;
	private String TAG = "GpsModule";
	private boolean listening = false,debug = true;
	private List<MyGPSListener> listeners;
	private ThreadManager manager;

	private String line = null;
	private String GPRMC = null, GPGGA = null;
	private GpsModuleData data = null;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();

	public GpsModule(ThreadManager manager, IOIO ioio){
		this.ioio = ioio;
		this.manager = manager;
		restart();
	}

	public void sendCommand(String command){
		command+="\r\n";
		try {
			os.write(command.getBytes("US-ASCII"));
		} catch (IOException e) {
			Log.e(TAG,"Failed to send command!");
			e.printStackTrace();
		}
	}

	public void startListening(){
		listening = true;
		if(debug){
			manager.getUtilitiesThread().sendMessage("GPS Module: Start listening.");
		}
	}
	public void stopListening(){
		listening = false;
		if(debug){
			manager.getUtilitiesThread().sendMessage("GPS Module: Stop listening.");
		}
	}
	public void abort(){
		try {
			if(debug){
				manager.getUtilitiesThread().sendMessage("GPS Module: aborting.");
			}

			listening = false;
			listeners.clear();
			is.close();
			os.close();
			uart.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void restart(){
		listeners = new LinkedList<MyGPSListener>();
		try {
			uart = this.ioio.openUart(GPS_PIN_RX, GPS_PIN_TX, GPS_BAUD, Uart.Parity.NONE, Uart.StopBits.ONE);
			is = uart.getInputStream();
			os = uart.getOutputStream();

			//Write default params
			sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);
			sendCommand(PMTK_SET_NMEA_UPDATE_5HZ);

			if(debug){
				manager.getUtilitiesThread().sendMessage("Restarting GPS module. Sent default params.");
				Log.e(TAG,"Restarting, sent params: ");
				Log.e(TAG,"param: "+PMTK_SET_NMEA_OUTPUT_RMCGGA);
				Log.e(TAG,"param: "+PMTK_SET_NMEA_UPDATE_5HZ);
			}
		} catch (ConnectionLostException e) {
			e.printStackTrace();
		}
	}

	public String read(){
		if(debug){
			Log.e(TAG,"reading");
		}
		if(listening){
			try {
				if(is.available()>0){
					int readChar;
					while((readChar = is.read())!= -1){
						if(readChar == 36){
							//hit the new line
							if(line != null){
								line = new String(baos.toByteArray(),"US-ASCII");
								Log.e(TAG,"done: "+line);
							}else{
								line = new String();
							}
							baos.reset();
							baos.write(readChar);
							break;
						}else{
							baos.write(readChar);
						}
						if(is.available()==0){
							break;
						}
					}

					return line;
				}
			} catch (IOException e) {
				Log.e(TAG,"Could not read data!");
				e.printStackTrace();
			}
		}
		return null;
	}

	public void tick(){
		if(debug){
			manager.getUtilitiesThread().sendMessage("tick");
			Log.e(TAG,"tick");
		}
		parse(read());
		if(GPGGA != null && GPRMC != null){
			if(debug && data != null){
				data.printDebug();
			}
			for(MyGPSListener listener:listeners){
				listener.gotNewGPSData(data);
			}
		}

	}

	private void parse(String input){
		if(input == null){return;}
		if(data == null){
			data = new GpsModuleData();
		}
		if(input.startsWith("$GPRMC")){
			//											  min		GMT		 active fix	    	mins/degs/secs		  speed(knots)	GPS angle	date
			//05-26 16:12:58.660: E/GpsModule(17420): done: $GPRMC, 151254.400,     A,      5225.1402,N,00404.8984,W,      0.00,      130.87,     260512     ,,,A*7B
			GPRMC = input;
			String[] details = input.split(",");
			data.setLocked(details[2]);
			data.setLatitude(details[3], details[4]);
			data.setLongitude(details[5], details[6]);
			data.setSpeed(details[7]);
			data.setHeading(details[8]);

		}else if(input.startsWith("$GPGGA")){
			//											extras	   GMT				mins/degs/secs			good fix(1)		num sats	horrizontal accuracy	altitude(M)		altitude of geiod
			//05-26 16:12:58.742: E/GpsModule(17420): done: $GPGGA,151254.600,	5225.1402,N,00404.8984,W, 		1,      	 6,					1.84,				15.5,M,  			51.3,M			,,*79
			GPGGA = input;

			String[] details = input.split(",");
			data.setLatitude(details[2], details[3]);
			data.setLongitude(details[4], details[5]);
			data.setLockStrength(details[6]);
			data.setNumSats(details[7]);
			data.setAccuracy(details[8]);
			data.setAltitude(details[9]);

		}else{

		}
	}

	public void addListener(MyGPSListener listener){
		listeners.add(listener);
	}
	public void removeListener(MyGPSListener listener){
		listeners.remove(listener);
	}

	public class GpsModuleData{
		public Location location = new Location("ME");
		public long latE6,lngE6;
		public int lockedSatelites,lockStrength;
		public boolean lock;
		public float accuracy,altitude,heading,speed;

		private char temp;
		private char[] chars;
		private StringBuilder stringBuilder = new StringBuilder();

		public void setLocked(String locked){
			if(locked.equals("A")){
				lock = true;
			}else{
				lock = false;
			}
		}
		public void setLatitude(String data, String about){
			stringBuilder.delete(0, stringBuilder.length());

			if(data.length() > 0){
		    	char[] chars = data.toCharArray();	    	
				int bigNum = Integer.valueOf(new String(chars, 0, 2));
				float rem = Float.valueOf(new String(chars,2,6));
				
				if(about.equals("N") || about.equals("n")){
					location.setLatitude(((bigNum)+(rem/60f)));
				}else{
					location.setLatitude((-(bigNum)+(rem/60f)));
				}
				
			}
		}
		public void setLongitude(String data, String about){
			stringBuilder.delete(0, stringBuilder.length());
			
			if(data.length()>0){
		    	char[] chars = data.toCharArray();
				int bigNum = Integer.valueOf(new String(chars, 0, 3));
				float rem = Float.valueOf(new String(chars,3,6));	
				if(about.equals("E") || about.equals("e")){
					location.setLongitude(((bigNum)+(rem/60f)));
				}else{
					location.setLongitude((-(bigNum)+(rem/60f)));
				}
				
			}
		}
		public void setSpeed(String speed){
			try{
				this.speed = Float.valueOf(speed);
			}catch(NumberFormatException e){}
		}
		public void setHeading(String heading){
			try{
				this.heading = Float.valueOf(heading);
			}catch(NumberFormatException e){}
		}
		public void setLockStrength(String strength){
			try{
				lockStrength = Integer.valueOf(strength);
			}catch(NumberFormatException e){}
		}
		public void setNumSats(String numSats){
			try{
				lockedSatelites = Integer.valueOf(numSats);
			}catch(NumberFormatException e){}
		}
		public void setAccuracy(String accuracy){
			try{
				this.accuracy = Float.valueOf(accuracy);
			}catch(NumberFormatException e){}
		}
		public void setAltitude(String altitude){
			try{
				this.altitude = Float.valueOf(altitude);
			}catch(NumberFormatException e){}
		}

		public void printDebug(){
			Log.e(TAG,"GPS DATA:");
			Log.e(TAG," Latitude: "+location.getLatitude()+" Longitude: "+location.getLongitude()+" altitude: "+altitude+" heading: "+heading);
			Log.e(TAG,"Trusted lock: "+lock+" on "+lockedSatelites+" with accuracy: "+accuracy+" and strength: "+lockStrength);
		}
	}
}
