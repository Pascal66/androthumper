package android.ioio.car;

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
	private Controller leftController,rightController;
	/**The underlying activity. */
	private MainActivity me;
	
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
	
	public void addMyListener(MainActivity me){
		this.me = me;
	}
	
	public Controller getLeftController(){
		return leftController;
	}
	public Controller getRightController(){
		return rightController;
	}
	
	public Controller getController(int cont){
		if(cont == 0){
			return leftController;
		}else{
			return rightController;
		}
	}

	@Override
	public void buttonPressed(ButtonEvent arg0) {
		if(arg0.getController() == leftController){
			me.buttonDown(arg0,0);
		}else{
			me.buttonDown(arg0,1);
		}
	}

	@Override
	public void buttonReleased(ButtonEvent arg0) {
		if(arg0.getController() == leftController){
			me.buttonUp(arg0, 0);
		}else{
			me.buttonUp(arg0, 1);
		}
	}

	@Override
	public void joystickMoved(JoystickEvent arg0) {
		if(arg0.getController() == leftController){
			me.leftJoystick(arg0.getScaledY(-127, 127));
		}else{
			me.rightJoystick(arg0.getScaledY(-127, 127));
		}
		
	}

}
