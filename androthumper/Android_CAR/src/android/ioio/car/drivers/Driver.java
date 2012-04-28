package android.ioio.car.drivers;

public interface Driver {
	/**Start the driver. By deafult, drivers do not start themselves. */
	public void start();
	/**Stop the driver. */
	public void stop();
	/**Restart the driver. */
	public void restart();
}
