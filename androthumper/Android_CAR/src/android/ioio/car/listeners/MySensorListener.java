package android.ioio.car.listeners;

public interface MySensorListener {

	public void gotNewAccelerometerData(float x, float y, float z);
	public void gotNewOrientationData(float x, float y, float z);
}
