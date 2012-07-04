package constants;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
	
	public interface GpsData{
		public byte[] getRawData();
		public double getAccuracy();
		public double getLongitude();
		public double getLatitude();
		public double getAltitude();
		public boolean gotLock();
		public float getSpeed();
		public float getRealHeading();
	
	public class GpsDataWrapper implements GpsData{
		
		public static final byte PROVIDER_GPSMODULE = 1;
		public static final byte PROVIDER_DEVICE = 2;
		
		private String TAG = "GPS_DATA";
		private double longitude,latitude,altitude;
		private List<MyGpsSatellite> gpsSats;
		private int lockedSatelites,lockStrength;
		private boolean lock;
		private float accuracy,heading,speed,realHeading;
		private int provider;
		private byte[] data;

		public GpsDataWrapper(byte[] data){
			this.data = data;
			parseData(this.data);
		}
		
		private void parseModule(DataInputStream dis){
			try {
				lock = dis.readBoolean();
				lockedSatelites = dis.readInt();
				lockStrength = dis.readInt();
				longitude = dis.readDouble();			
				latitude = dis.readDouble();	
				accuracy = dis.readFloat();
				realHeading = dis.readFloat();
				altitude = dis.readDouble();
				heading = dis.readFloat();
				speed = dis.readFloat();
				
				gpsSats = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void parseDevice(DataInputStream dis){
			try{
				latitude = dis.readDouble();
				longitude = dis.readDouble();
				altitude = (float)dis.readDouble();
				speed = dis.readFloat();
				accuracy = dis.readFloat();
				realHeading = dis.readFloat();
				int hasSats = dis.read();
				if(hasSats == 1){
					lockedSatelites = dis.read();
					//We have GPS status data
					gpsSats = new LinkedList<GpsData.MyGpsSatellite>();
					
					for(int i = 0; i <lockedSatelites; i++){
						gpsSats.add(new MyGpsSatellite(dis.read(), dis.readFloat(), dis.read()));
					}
				}else{
					gpsSats = null;
				}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		/**
		 * Parse the whole data provided by either the module or the device
		 * @param data - byte[] - The whole data provided by either the module or the device
		 */
		private void parseData(byte[] data){
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			
			try {
				provider = dis.readByte(); 		//The provider code
				
				if(GpsDataWrapper.PROVIDER_DEVICE == provider){
					parseDevice(dis);
				}else if(GpsDataWrapper.PROVIDER_GPSMODULE == provider){
					parseModule(dis);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}							
		}

		public byte[] getRawData(){
			//FIXME implement
			return null;
		}
		
		public double getLatitude(){
			return latitude;
		}
		public double getLongitude(){
			return longitude;
		}
		public boolean gotLock(){
			return lock;
		}
		public double getAccuracy(){
			return accuracy;
		}
		public double getAltitude(){
			return altitude;
		}
		public float getHeading(){
			return heading;
		}
		public float getRealHeading(){
			return realHeading;
		}
		public float getSpeed(){
			return speed;
		}
		public int getSatsUsed(){
			return lockedSatelites;
		}
		public int getProvider(){
			return provider;
		}
		public List<MyGpsSatellite> getSats(){
			return gpsSats;
		}
		
		@Override
		public String toString(){
			StringBuilder buil = new StringBuilder();
			
			buil.append("Provider: ");if(getProvider() == PROVIDER_DEVICE){buil.append("Device");}else{buil.append("Module");}buil.append("\n");
			buil.append("Lock: ");		buil.append(lock);				buil.append("\n");
			buil.append("Accuracy: ");	buil.append(accuracy);			buil.append("\n");
			buil.append("Latitude: ");	buil.append(latitude);			buil.append("\n");
			buil.append("Longitude: ");	buil.append(longitude);			buil.append("\n");
			buil.append("Altitude: ");	buil.append(altitude); 			buil.append("\n");
			buil.append("Real heading: "); buil.append(realHeading); buil.append("\n");
			buil.append("Sats: ");		buil.append(lockedSatelites);	buil.append("\n");
			
			if(gpsSats != null){
				buil.append("Sat data: ");buil.append("\n");
				for(MyGpsSatellite sat:gpsSats){
					buil.append("PRN: ");buil.append(sat.prn);buil.append(" - SNR: ");buil.append(sat.snr);buil.append(" - USED: ");buil.append(sat.usedInFix);buil.append("\n");
				}
			}
			
			return buil.toString();
		}
	}
	
	public class MyGpsSatellite{
		private int prn;
		private float snr;
		private int usedInFix;
		
		public MyGpsSatellite(int prn, float snr, int inFix){
			this.prn = prn;
			this.snr = snr;
			this.usedInFix = inFix;
		}
		
		public int getPnr(){
			return prn;
		}
		public float getSnr(){
			return snr;
		}
		public int getUsedInFix(){
			return usedInFix;
		}
	}
}