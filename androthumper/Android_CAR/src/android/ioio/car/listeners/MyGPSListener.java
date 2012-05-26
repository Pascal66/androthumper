package android.ioio.car.listeners;

import android.ioio.car.hardware.GpsModule.GpsModuleData;

public interface MyGPSListener {

	public void gotNewGPSData(GpsModuleData newData);
}
