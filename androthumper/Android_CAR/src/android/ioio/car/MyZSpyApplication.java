package android.ioio.car;

import com.zeemote.zc.Controller;
import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.event.IButtonListener;
import com.zeemote.zc.event.IJoystickListener;
import com.zeemote.zc.event.JoystickEvent;

import android.app.Application;
import android.ioio.car.drivers.ZeemoteDriver;

/**
 * An instance of the application, but with Zeemote libs. Provides access to controllers,
 * and does the actual listening for events, forwarding them onto the ZeemoteDriver.
 * @author Alex Flynn
 *
 */
public class MyZSpyApplication extends Application implements IButtonListener,IJoystickListener{

	/**The controllers. */
	private Controller leftController,rightController;
	/**The underlying activity. */
	private ZeemoteDriver driver;

	@Override
	public void onCreate() {

		leftController = new Controller(1);
		leftController.addButtonListener(this);
		leftController.addJoystickListener(this);

		rightController = new Controller(2);
		rightController.addButtonListener(this);
		rightController.addJoystickListener(this);

		super.onCreate();
	}

	/**Add a listener to receive controller events. */
	public void addMyListener(ZeemoteDriver driver){
		this.driver = driver;
		this.driver.start();
	}

	/**Get the controller specified by cont. 0=left, 1=right*/
	public Controller getController(int cont){
		if(cont == 0){
			return leftController;
		}else{
			return rightController;
		}
	}

	@Override
	public void buttonPressed(ButtonEvent arg0) {
		if(driver != null){
			if(arg0.getController() == leftController){
				driver.leftButtonDown(arg0);
			}else{
				driver.rightButtonDown(arg0);
			}
		}
	}

	@Override
	public void buttonReleased(ButtonEvent arg0) {
		if(driver != null){
			if(arg0.getController() == leftController){
				driver.leftButtonUp(arg0);
			}else{
				driver.rightButtonUp(arg0);
			}
		}
	}

	@Override
	public void joystickMoved(JoystickEvent arg0) {
		if(driver != null){
			if(arg0.getController() == leftController){
				driver.leftJoystick(arg0.getScaledX(-255, 255),arg0.getScaledY(-255, 255));
			}else{
				driver.rightJoystick(arg0.getScaledX(-255, 255),arg0.getScaledY(-255, 255));
			}
		}
	}

}
