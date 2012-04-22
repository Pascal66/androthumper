package android.ioio.client;

import com.zeemote.zc.Controller;
import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.event.IButtonListener;
import com.zeemote.zc.event.IJoystickListener;
import com.zeemote.zc.event.JoystickEvent;

import android.app.Application;

/**
 * An instance of the application, but with Zeemote libs. Provides access to controllers,
 * and does the actual listening for events.
 * @author Alex Flynn
 *
 */
public class MyZSpyApplication extends Application implements IButtonListener,IJoystickListener{

	/**The controllers. */
	private Controller controller;
	/**The underlying activity. */
	private Main me;
	
	@Override
	public void onCreate() {
		
		controller = new Controller(1);
		controller.addButtonListener(this);
		controller.addJoystickListener(this);
		
		super.onCreate();
	}
	
	public void addMyListener(Main me){
		this.me = me;
	}
	
	public Controller getController(){
		return controller;
	}

	@Override
	public void buttonPressed(ButtonEvent arg0) {
		me.buttonDown(arg0);
	}

	@Override
	public void buttonReleased(ButtonEvent arg0) {
		me.buttonUp(arg0);
	}

	@Override
	public void joystickMoved(JoystickEvent arg0) {
		me.moveJoystick(arg0.getScaledX(-100, 100),arg0.getScaledY(-100, 100));
	}

}
