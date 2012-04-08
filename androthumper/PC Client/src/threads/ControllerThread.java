package threads;

import ui.Window;

import net.java.games.input.Component;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Component.POV;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component.Identifier;

/**
 * This thread implements a thread that constantly pings an XBox 360 controller, and passes
 * the data back to the window.
 * @author Alex Flynn
 *
 */
public class ControllerThread implements Runnable{

	/**The controller to listen to. */
	private Controller controller = null;
	/**An event queue to hold the controller events. */
	private EventQueue eventQueue;
	/**A single event from the queue. */
	private Event event;
	/**A component linked to an event. */
	private Component comp;
	/**The value of the component. */
	private float val;
	private StringBuffer buff;
	private Identifier ident;
	
	private Window host;
	/**An array to hold which buttons are pressed. */
	private boolean[] buttons = new boolean[10];
	/**A array to hold the x/y values from the left stick. */
	private float[] LStick = new float[2];
	/**An array to hold the x/y values from the right stick. */
	private float[] RStick = new float[2];
	/**An array to hold the values from the triggers. */
	private float[] triggers = new float[2];
	
	private boolean debugButtons = false, debugSticks = false, debugTriggers = false, debugPOV = false, debugOutput = false;
	private boolean debugRightTrigger = false, debugLeftTrigger = false, debugRightStick = false, debugLeftStick = false;

	public ControllerThread(Window window2){
		this.host = window2;
		ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
		Controller[] controllers = ce.getControllers();

		//Find the controller, get its event queue, and start the thread to listen to it.
		for(Controller c:controllers){
			if(c.getName().equals("Controller (XBOX 360 For Windows)")){
				controller = c;
				eventQueue = controller.getEventQueue();
				break;
			}
		}

		if(controller != null){
			Thread t = new Thread(this);
			t.start();
		}
	}

	@Override
	public void run() {
		while(controller.poll()){
			event = new Event();
			
			while(eventQueue.getNextEvent(event)){
				buff = new StringBuffer(controller.getName());
				comp = event.getComponent();
				val = event.getValue();
				
				ident = comp.getIdentifier();
				
				if(comp.getName().equals("Hat Switch")){
					if(debugPOV){
						buff.append(" recieved event from POV val: "+getPov(val));
							
						Window.PrintToLog("Controller Thread: "+buff.toString());
					}
				}else{
					ident = comp.getIdentifier();
					
					if(ident instanceof Axis){
						
						if(ident == Axis.X){
							//buff.append("Left stick X");
							LStick[0] = val;
						}else if(ident == Axis.Y){
							//buff.append("Left stick Y");
							LStick[1] = val;
						}else if(ident == Axis.RX){
							//buff.append("Right stick X");
							RStick[0] = val;
						}else if(ident == Axis.RY){
							//buff.append("Right stick Y");
							RStick[1] = val;
						}else if(ident == Axis.Z){
							if(val > 0){
								//buff.append("Left trigger");
								triggers[0] = Math.abs(val);
							}else{
								//buff.append("Right trigger");
								triggers[1] = Math.abs(val);
							}
						}
						
					}else if(ident instanceof Button){
						
						//if(debugButtons){
							ident = (Button)ident;
							
							//buff.append(" recieved event from");
							//buff.append(" "+comp.getName()+" val: ");
							
							if(val == 1.0f){
								//buff.append("On");
								buttons[getButtonId(ident)]=true;
							}else{
								//buff.append("Off");
								buttons[getButtonId(ident)]=false;
							}
							//Window.PrintToLog(buff);
						//}
					}
				}
				host.recieveControllerInput(buttons, LStick, RStick, triggers);
			}
			if(debugSticks){
				Window.PrintToLog("Controller Thread: Left Stick X: "+LStick[0]+" Left Stick Y: "+LStick[1]);
				Window.PrintToLog("Controller Thread: Right stick X: "+RStick[0]+" Right Stick Y: "+RStick[1]);
			}
			if(debugTriggers){
				Window.PrintToLog("Controller Thread: Left Trigger: "+triggers[0]);
				Window.PrintToLog("Controller Thread: Right Trigger: "+triggers[1]);
			}
			if(debugButtons){
				System.out.print("Buttons pressed: ");
				
				for(int i = 0; i < 10; i++){
					if(buttons[i]){
						System.out.print(" "+i);
					}
				}
				Window.PrintToLog("");
			}
			if(debugPOV){
				
			}

			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Window.PrintToLog("Controller Thread: Controller polled false. Dropping.");
	}
	
	private String getPov(float val){
		if(val == POV.CENTER){
			return "CENTER";
		}else if(val == POV.DOWN){
			return "DOWN";
		}else if(val == POV.DOWN_LEFT){
			return "DOWN_LEFT";
		}else if(val == POV.DOWN_RIGHT){
			return "DOWN_RIGHT";
		}else if(val == POV.LEFT){
			return "LEFT";
		}else if(val == POV.OFF){
			return "OFF";
		}else if(val == POV.RIGHT){
			return "RIGHT";
		}else if(val == POV.UP){
			return "UP";
		}else if(val == POV.UP_LEFT){
			return "UP_LEFT";
		}else if(val == POV.UP_RIGHT){
			return "UP_RIGHT";
		}else{
			return "none";
		}
	}
	private int getButtonId(Identifier ident){	
		if(ident == Button._0){
			return 0;
		}else if(ident == Button._1){
			return 1;
		}else if(ident == Button._2){
			return 2;
		}else if(ident == Button._3){
			return 3;
		}else if(ident == Button._4){
			return 4;
		}else if(ident == Button._5){
			return 5;
		}else if(ident == Button._6){
			return 6;
		}else if(ident == Button._7){
			return 7;
		}else if(ident == Button._8){
			return 8;
		}else if(ident == Button._9){
			return 9;
		}else return -1;
	}
}
