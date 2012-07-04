package android.ioio.car.providers;

public class ProviderManager {

	private static GPSProvider gpsProvider;
	private static String ipAddress;
	
	public static GPSProvider getGpsProvider(){
		if(gpsProvider == null){
			gpsProvider = new GPSProvider();
		}
		return gpsProvider;
	}
	
	public static String getIpAddress(){
		return ipAddress;
	}
	public static void setIpAddress(String ip){
		ipAddress = ip;
	}
}
