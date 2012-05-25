package android.ioio.car.hardware;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import android.ioio.car.listeners.MyGPSListener;
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
	public static final int GPS_PIN_RX = 0;
	public static final int GPS_PIN_TX = 0;
	public static final int GPS_BAUD = 9600;
	
	private InputStream is;
	private OutputStream os;
	private Uart uart;
	private String TAG = "GpsModule";
	private boolean listening = false;
	private List<MyGPSListener> listeners;
	
	public GpsModule(IOIO ioio){
		this.ioio = ioio;
		restart();
	}
	
	public void sendCommand(String command){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeChars(command);
			os.write(baos.toByteArray());
		} catch (IOException e) {
			Log.e(TAG,"Failed to send command!");
			e.printStackTrace();
		}
	}
	
	public void startListening(){
		listening = true;
	}
	public void stopListening(){
		listening = false;
	}
	public void abort(){
		try {
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
		} catch (ConnectionLostException e) {
			e.printStackTrace();
		}
	}
	
	public String read(){
		if(listening){
			try {
				if(is.available()>0){
					StringBuilder builder = new StringBuilder();
					int readChar;
					while((readChar = is.read())!= -1){
						builder.append((char)readChar);
					}
					return builder.toString();
				}
			} catch (IOException e) {
				Log.e(TAG,"Could not read data!");
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public void tick(){
		String line = read();
		if(line != null){
			
		}
	}
	
	private void parse(String data){
		//Find a checksum
		if(data.charAt(data.length()-4) == '*'){
			int sum = parseHex(data.charAt(data.length()-3)) * 16;
			sum += parseHex(data.charAt(data.length()-2));
			
		    for (int i=1; i < data.length()-4; i++) {
		        sum ^= data.charAt(i);
		      }
		      if (sum != 0) {
		        // bad checksum :(
		        //return false;
		      }
		}
		
		if(data.startsWith("$GPGGA")){
			//date,(2)N,(2)W,fix,num sats, XX, altitude,XX, XX, XX, XX, XX, checksum
			//(time, latitude/longitude, fix quality, num sats, HDOP?, altitude, geiodheight
			String[] details = data.split(",");
		}else if(data.startsWith("$GPRMC")){
			//(time, has fixed, lattitude/logitude, speed, angle, date
		}else{
			
		}
	}
	
	private int parseHex(char c) {
	    if (c < '0')
	      return 0;
	    if (c <= '9')
	      return c - '0';
	    if (c < 'A')
	       return 0;
	    if (c <= 'F')
	       return (c - 'A')+10;
	    return -1;
	}
	
	public void addListener(MyGPSListener listener){
		listeners.add(listener);
	}
	public void removeListener(MyGPSListener listener){
		listeners.remove(listener);
	}
}
