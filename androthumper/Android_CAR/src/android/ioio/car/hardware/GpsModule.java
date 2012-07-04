package android.ioio.car.hardware;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.ioio.car.providers.GPSProvider;
import android.ioio.car.threads.ThreadManager;
import android.location.Location;
import android.util.Log;
import constants.Conts;
import constants.GpsData;

/**Represents the facilities provided by the hardware GPS module. This is connected to the IOIO via serial. This provides
 * data to the GPS provider.
 * @author Alex
 *
 */
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

	/**The host IOIO. */
	private IOIO ioio;
	/**The receiving pin on the IOIO. */
	public static final int GPS_PIN_RX = 3;
	/**The transmitting pin on the module. */
	public static final int GPS_PIN_TX = 4;
	/**The baud rate of the serial connection. */
	public static final int GPS_BAUD = 9600;

	/**Input stream to read the data from the module. */
	private InputStream is;
	/**The output stream to write to the module. */
	private OutputStream os;
	/**The serial connection to the module. */
	private Uart uart;
	/**Debug tag. */
	private String TAG = "GpsModule";
	/**Boolean flag signifying whether the module is listening for GPS updates. */
	private boolean listening = false;
	/**A debug flag. */
	private boolean debug = false;
	private ThreadManager manager;

	private String line = null;
	private String GPRMC = null, GPGGA = null;
	private GpsModuleData data = null;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private GPSProvider provider;
	private float compassHeading;

	public GpsModule(ThreadManager manager, IOIO ioio, GPSProvider provider){
		this.ioio = ioio;
		this.manager = manager;
		this.provider = provider;
		provider.assignDevice(this);
		restart();
	}
	
	public void provideCompassHeading(float heading){
		compassHeading = heading;
	}

	/**Send a command to the module. 
	 * @param command - String - The command to send to the module.*/
	public void sendCommand(String command){
		command+="\r\n";
		try {
			os.write(command.getBytes("US-ASCII"));
		} catch (IOException e) {
			Log.e(TAG,"Failed to send command!");
			e.printStackTrace();
		}
	}

	/**Enable the module. Start it listening. */
	public void startListening(){
		listening = true;
		if(debug){
			manager.getUtilitiesThread().sendMessage("GPS Module: Start listening.");
		}
	}

	/**Disable the module. Stop is listening. */
	public void stopListening(){
		listening = false;
		if(debug){
			manager.getUtilitiesThread().sendMessage("GPS Module: Stop listening.");
		}
	}

	/**Abort the module. After this, the module must be reinitialized or restarted. */
	public void abort(){
		try {
			if(debug){
				manager.getUtilitiesThread().sendMessage("GPS Module: aborting.");
			}

			listening = false;
			is.close();
			os.close();
			uart.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**Restart the module and set its default parameters. */
	public void restart(){
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

	/**Read data from the module. Returns null if there is not a complete line to return.
	 * @return - String - The line that was read from the module.*/
	private String read(){
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
								if(debug){
									Log.e(TAG,"done: "+line);
								}
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

	/**Attempt to read data from the module, and if it is a complete line, provide the data to the GPS provider. */
	public void tick(){
		if(listening){
			if(debug){
				manager.getUtilitiesThread().sendMessage("tick");
				Log.e(TAG,"tick");
			}
			parse(read());
			if(GPGGA != null && GPRMC != null){
				if(debug && data != null){
					data.printDebug();
				}
				provider.provideData(this, data);
			}
		}
	}

	/**Parse a line into the GPS data. */
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

	public class GpsModuleData implements GpsData{
		private Location location = new Location("ME");
		private int lockedSatelites,lockStrength;
		private boolean lock;
		private float accuracy,heading,speed;

		private ByteArrayOutputStream baos = null;
		private DataOutputStream dos = null;

		private StringBuilder stringBuilder = new StringBuilder();

		public byte[] getRawData(){
			if(baos == null){
				baos = new ByteArrayOutputStream();
				dos = new DataOutputStream(baos);
			}
			baos.reset();

			try {
				//dos.write(Conts.UtilsCodes.DataType.GPS_POSITION_DATA);
				dos.write(GpsDataWrapper.PROVIDER_GPSMODULE);
				dos.writeBoolean(lock);
				dos.writeInt(lockedSatelites);
				dos.writeInt(lockStrength);
				dos.writeDouble(location.getLongitude());
				dos.writeDouble(location.getLatitude());
				dos.writeFloat(accuracy);
				dos.writeFloat(compassHeading);
				dos.writeDouble(location.getAltitude());
				dos.writeFloat(heading);
				dos.writeFloat(speed);
			} catch (IOException e) {
				e.printStackTrace();
			}

			return baos.toByteArray();
		}

		private void setLocked(String locked){
			if(locked.equals("A")){
				lock = true;
			}else{
				lock = false;
			}
		}
		private void setLatitude(String data, String about){
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

		private void setLongitude(String data, String about){
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
		private void setSpeed(String speed){
			try{
				this.speed = Float.valueOf(speed);
			}catch(NumberFormatException e){}
		}
		private void setHeading(String heading){
			try{
				this.heading = Float.valueOf(heading);
			}catch(NumberFormatException e){}
		}
		private void setLockStrength(String strength){
			try{
				lockStrength = Integer.valueOf(strength);
			}catch(NumberFormatException e){}
		}
		private void setNumSats(String numSats){
			try{
				lockedSatelites = Integer.valueOf(numSats);
			}catch(NumberFormatException e){}
		}
		private void setAccuracy(String accuracy){
			try{
				this.accuracy = Float.valueOf(accuracy);
			}catch(NumberFormatException e){}
		}
		private void setAltitude(String altitude){
			try{
				location.setAltitude(Double.valueOf(altitude));
			}catch(NumberFormatException e){}
		}

		public void printDebug(){
			Log.e(TAG,"GPS DATA:");
			Log.e(TAG," Latitude: "+location.getLatitude()+" Longitude: "+location.getLongitude()+" altitude: "+location.getAltitude()+" heading: "+heading);
			Log.e(TAG,"Trusted lock: "+lock+" on "+lockedSatelites+" with accuracy: "+accuracy+" and strength: "+lockStrength);
		}

		@Override
		public double getAccuracy() {
			return (double)this.accuracy;
		}

		@Override
		public double getLongitude() {
			return location.getLongitude();
		}

		@Override
		public double getLatitude() {
			return location.getLatitude();
		}

		@Override
		public double getAltitude() {
			return location.getAltitude();
		}

		@Override
		public float getSpeed() {
			return location.getSpeed();
		}

		@Override
		public boolean gotLock(){
			return lock;
		}
		public float getRealHeading(){
			return 0;//TODO
		}
	}
}
