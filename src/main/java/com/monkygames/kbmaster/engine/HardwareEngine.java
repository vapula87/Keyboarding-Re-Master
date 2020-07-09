/*
 * See LICENSE in top-level directory.
 */
package com.monkygames.kbmaster.engine;

// === jinput imports === //
import com.monkygames.kbmaster.driver.Device;
import com.monkygames.kbmaster.input.*;
import com.monkygames.kbmaster.input.OutputMouse.MouseType;
import com.monkygames.kbmaster.profiles.Profile;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.games.input.Component;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.Keyboard;
import net.java.games.input.LinuxEnvironmentPlugin;
import net.java.games.input.Mouse;

/**
 * Handles initializing and managing hardware.
 * Also is responsible for polling the devices (as a thread).
 * Some devices may not have a mouse.
 * @version 1.0
 */
public class HardwareEngine implements Runnable{

	// ============= Class variables ============== //
	private final Device device;
	private ArrayList<Keyboard> keyboards;
	private Mouse mouse;
	private ArrayList<PollEventQueue> keyboardEventQueues;
	private PollEventQueue mouseEventQueue;
	private boolean closing;
	/**
	 * Timer for checking hardware status.
	 */
	 private Timer timer;
	 private TimerTask timerTask;
	/**
	 * True if polling and false otherwise.
	 */
	private boolean isPolling = false;
	/**
	 * Controls the thread loop for polling.
	 */
	private boolean poll = false, pollFail = false;
	/**
	 * Used for polling the devices.
	 */
	private Thread thread;
	/**
	 * Used for getting events from the controllers.
	 */
	private Event event;
	/**
	 * True if the device hardware exists and false otherwise.
	 */
	private boolean doesHardwareExist;
	/**
	 * The profile used for polling.
	 */
	private Profile profile;
	/**
	 * The currently used keymap.
	 */
	private Keymap keymap;
	/**
	 * Used for when a keymap is being temporary used and switched back
	 * when a key is released.
	 */
	private Keymap previousKeymap;
	/**
	 * The previous name of the component for a temporary keymap switch.
	 */
	private String previousComponentName = "";
	/**
	 * True if should switch back on release and false otherwise.
	 */
	private boolean isKeymapOnRelease = false;
	/**
	 * Controls the forwarding of key presses and scroll wheel.
	 */
	private Robot robot;
	/**
	 * A list of listeners for hardware status change.
	 */
	private final HardwareManager hardwareManager;
	/**
	 * The amount of time to check for new devices.
	 */
	private static final long sleepPassive = 1000;

	private boolean isMouseEvent = false;
	/**
	 * True if the hardware should be "grabbed" or false otherwise.
	 */
	private boolean isEnabled = false;
	/**
	 * Mice are normally relative.
	 */
	private boolean isMouseRelative = true;
	private float unit_width, unit_height;

	/**
	 * Used for determining if a mouse should be polled.
	 */
	private boolean hasMouse = true;
	// ============= Constructors ============== //
	public HardwareEngine(Device device, HardwareManager hardwareManager){
		event = new Event();
		keyboards = new ArrayList<>();
		this.mouseEventQueue = null;
		this.hardwareManager = hardwareManager;
		this.keyboardEventQueues = new ArrayList<>();
		this.device = device;
		try { robot = new Robot();}
		catch (AWTException ex) {
			Logger.getLogger(HardwareEngine.class.getName()).log(Level.SEVERE, null, ex);
		}
		hasMouse = device.getDeviceInformation().hasMouse();
	}
// ============= Public Methods ============== //
	/**
	 * Begins hardware scanning.
	 */
	 public void startScanning() {
		 scanHardware();
		 timerTask = new HardwareScanScheduler();
		 timer = new Timer();
		 timer.schedule(timerTask,sleepPassive,sleepPassive);
	 }
	/**
	 * Halts hardware scanning.
	 */
	public void stopScanning() {
		timerTask.cancel();
		timer.cancel();
	}
	/**
	 * Sets that this hardware should be grabbed if already detected.
	 */
	public void grabHardware(boolean isEnabled){
		if(isEnabled){
			if(keyboards.size() > 0) {
				for(Keyboard keyboard:keyboards)
					keyboard.grab();
			}
			if(hasMouse && mouse != null) {
				//Wait for mouse button to be released before grabbing to prevent crash
				while (mouse.poll()) {
					if (mouseEventQueue.eventExists()) continue;
					break;
				}
				mouse.grab();
			}
		}else{
			if(keyboards.size() > 0) {
				for(Keyboard keyboard:keyboards)
					keyboard.ungrab();
			}
			if(hasMouse && mouse != null) mouse.ungrab();
		}
		this.isEnabled = isEnabled;
	}
	public Device getDevice(){ return device; }
	/**
	 * Returns true if the hardware is found.
	 * @return true if the hardware exists on the system and false otherwise.
	 */
	public boolean hardwareExist(){	return doesHardwareExist; }
	/**
	 * Starts polling the devices.
	 * Must check that hardware exists before starting.
	 * Note, null can be passed in as long as its not enabled!
	 */
	public void startPolling(Profile profile){
		if(isPolling) stopPolling();
		this.profile = profile;
		if(profile != null) {
			this.keymap = profile.getKeymap(profile.getDefaultKeymap());
			grabHardware(true);
		}
		else return;
		this.isKeymapOnRelease = false;
		poll = true;
		thread = new Thread(this);
		thread.start();
	}
	public void stopPolling(){
		poll = false;
		// wait for thread to die
		while(isPolling);
		grabHardware(false);
	}
// ============= Private Methods ============== //
	/**
	 * Poll the hardware and generate system key calls.
	 */
	private void pollNormalMode(){
		while(poll){
			// poll keyboard
			for(Keyboard keyboard: keyboards){
				if(!keyboard.poll()) {
					poll = false;
					pollFail = true;
					grabHardware(false);
				}
			}
			// poll mouse
			if(hasMouse) {
				if (mouse != null && !mouse.poll()){
					poll = false;
					pollFail = true;
					grabHardware(false);
				}
			}
			// Determines whether to process the output or not
			if (!isEnabled) continue;

			// handle keyboard events
			for(PollEventQueue keyboardEventQueue: this.keyboardEventQueues){
				for(Event event: keyboardEventQueue.getEvents()){
					Component component = event.getComponent();
					//System.out.println("component = "+component);
					String name = component.getIdentifier().getName();
					if (profile.getDefaultKeymap() != keymap.getID()-1)
						keymap = profile.getKeymap(profile.getDefaultKeymap());
					ButtonMapping mapping = keymap.getButtonMapping(name);
					if(mapping != null) processOutput(name, mapping.getOutput(), event.getValue());

				}

			}
			// handle mouse events

			if(hasMouse){
				if (mouseEventQueue == null) continue;
				for(Event event: mouseEventQueue.getEvents()){
					//System.out.println("===== New Event Queue =====");
					Component component = event.getComponent();
					//System.out.println("component = "+component);
					String name = component.getIdentifier().getName();
					if (profile.getDefaultKeymap() != keymap.getID()-1)
						keymap = profile.getKeymap(profile.getDefaultKeymap());
					WheelMapping mapping;
					if(component.getIdentifier() == Axis.X){
						Point point = MouseInfo.getPointerInfo().getLocation();
						float rel = component.getPollData();
						float x = point.x + rel;
						robot.mouseMove((int)x, point.y);
					}else if(component.getIdentifier() == Axis.Y){
						Point point = MouseInfo.getPointerInfo().getLocation();
						float rel = component.getPollData();
						float y = point.y + rel;
						robot.mouseMove(point.x, (int)y);
					}else if(component.getIdentifier() == Axis.Z && event.getValue() >= 1){
						mapping = keymap.getzUpWheelMapping();
						if(mapping.getOutput() instanceof OutputKey || mapping.getOutput() instanceof OutputKeymapSwitch){
							processOutput(name, mapping.getOutput(),1);
							robot.delay(10);
							processOutput(name, mapping.getOutput(),0);
						}else processOutput(name, mapping.getOutput(),event.getValue());
					}else if(component.getIdentifier() == Axis.Z && event.getValue() <= -1){
						mapping = keymap.getzDownWheelMapping();
						if(mapping.getOutput() instanceof OutputKey || mapping.getOutput() instanceof OutputKeymapSwitch){
							processOutput(name, mapping.getOutput(),1);
							robot.delay(10);
							processOutput(name, mapping.getOutput(),0);
						}else processOutput(name, mapping.getOutput(),event.getValue());
					}else if(component.getIdentifier() == Axis.Z && event.getValue() == 0){
						// on release, don't do anything
					}else{
						try {
							ButtonMapping bMapping = keymap.getButtonMapping(name);
							processOutput(name, bMapping.getOutput(), event.getValue());
						}catch (NullPointerException v) { }
					}
				}
			}
		}
	}
	/**
	 * Processes the proper response to the output.
	 * Checks for mouse, keyboard, or keymap events.
	 * @param name the name of the input component.
	 * @param output the output to process.
	 * @param eventValue the event's value.
	 */
	private void processOutput(String name, Output output, float eventValue){
		// test for a release on a switch on release keymap event.
		if(isKeymapOnRelease && name.equals(previousComponentName) && eventValue == 0){
			isKeymapOnRelease = false;
			profile.setDefaultKeymap(previousKeymap.getID()-1);
			try{
				if (hardwareManager.getProfileUIController().getCurrentProfile() == profile)
					hardwareManager.getProfileUIController().getKeymapTabPane().getSelectionModel().select(previousKeymap.getID()-1);
			}catch (NullPointerException e) { }
			return;
		}
		if(output instanceof OutputKey){
			if(eventValue == 1){
				// handle modifiers
				if(output.getModifier() != 0)
					robot.keyPress(output.getModifier());
				robot.keyPress(output.getKeycode());
			}else if(eventValue == 0){
				// note, don't do anything if
				// the value is 2 (which means repeat)
				robot.keyRelease(output.getKeycode());
				// release the modifier after the key has been released
				if(output.getModifier() != 0)
					robot.keyRelease(output.getModifier());
			}
		}else if(output instanceof OutputMouse){
			OutputMouse outputM = (OutputMouse)output;
			if(outputM.getMouseType() == MouseType.MouseClick) {
				if (eventValue == 1)
					robot.mousePress(outputM.getKeycode());
				else if(eventValue == 0)
					robot.mouseRelease(outputM.getKeycode());
			}else if(outputM.getMouseType() == MouseType.MouseDoubleClick){
				if(eventValue == 1){
					robot.mousePress(outputM.getKeycode());
					robot.delay(10);
					robot.mouseRelease(outputM.getKeycode());
					robot.delay(10);
					robot.mousePress(outputM.getKeycode());
					robot.delay(10);
					robot.mouseRelease(outputM.getKeycode());
				}
			}else if(outputM.getMouseType() == MouseType.MouseWheel)
				robot.mouseWheel(outputM.getKeycode());
		}else if(output instanceof OutputKeymapSwitch){
			if(output.getKeycode() > 0 && output.getKeycode() <= 8){
				OutputKeymapSwitch outputSwitch = (OutputKeymapSwitch)output;
				// don't allow new keymap events if keymap is switch on held
				if(!isKeymapOnRelease){
					if(eventValue == 1){
						if(outputSwitch.isIsSwitchOnRelease()){
							isKeymapOnRelease = true;
							previousKeymap = keymap;
							previousComponentName = name;
						}
						profile.setDefaultKeymap(output.getKeycode()-1);
						try {
							if (hardwareManager.getProfileUIController().getCurrentProfile() == profile)
								hardwareManager.getProfileUIController().getKeymapTabPane().getSelectionModel().select(output.getKeycode()-1);
						}catch (NullPointerException e) { }
					}
				}
			}
		}
		// don't do anything if output instanceof OutputDisabled
	}
	/**
	 * Attempt to find and initialize the device hardware.
	 * @return true if the hardware has been found and false otherwise.
	 */
	private void rescanHardware(){
		scanHardware(getControllers(false));
	}
	private void scanHardware(){
		doesHardwareExist = false;
		scanHardware(getControllers(true));
	}
	private void scanHardware(Controller[] controllers){
		//System.out.println("///---Scanning Controllers---\\\\\\");
		if (closing) return;
		boolean found = false;
		for(Controller controller: controllers){
			//System.out.println("Controller: "+controller);
			if (controller.getName().equals(device.getDeviceInformation().getJinputName())) {
				found = true;
				hardwareConnected(controller);
			}
		}
		if (!found || pollFail) {
			hardwareDisconnected();
			pollFail = false;
		}
	}
	/**
	 * Set if hardware has been disconnected.
	 */
	private void hardwareDisconnected(){
		if (!hardwareExist()) return;
		stopPolling();
		keyboards.clear();
		if(hasMouse) mouse = null;
		device.setEnabled(false);
		doesHardwareExist = false;
		synchronized(hardwareManager) {
			hardwareManager.hardwareStatusChange(hardwareExist(),device.getDeviceInformation().getJinputName());
		}
		//System.out.println(device.getDeviceInformation().getName()+" disconnected");
	}
	/**
	 * Set if hardware has connected.
	 */
	private void hardwareConnected(Controller controller) {
		Controller.Type type = controller.getType();
		if (hardwareExist()) {
			if (type == Controller.Type.KEYBOARD) {
				for (Keyboard keyboard : keyboards) {
					if (keyboard == controller) return;
				}
			}
			else if (mouse == controller) return;
		}
		if (type == Controller.Type.KEYBOARD) {
			Keyboard keyboard;
			keyboard = (Keyboard)controller;
			keyboards.clear();
			keyboards.add((Keyboard)controller);
			keyboardEventQueues.clear();
			keyboardEventQueues.add(new PollEventQueue(keyboard.getComponents()));
		}
		else if (type == Controller.Type.MOUSE) {
			mouse = (Mouse)controller;
			if(mouse.getX() != null && !mouse.getX().isRelative())
				isMouseRelative = false;
			else isMouseRelative = true;
			float width = MouseInfo.getPointerInfo().getDevice().getDisplayMode().getWidth();
			float height = MouseInfo.getPointerInfo().getDevice().getDisplayMode().getHeight();
			unit_width = 1f/width;
			unit_height = 1f/height;
			mouseEventQueue = new PollEventQueue(mouse.getComponents());
		}
		doesHardwareExist = true;
		synchronized(hardwareManager) {
			hardwareManager.hardwareStatusChange(hardwareExist(),device.getDeviceInformation().getJinputName());
		}
		if (device.isEnabled()) startPolling(device.getProfile());
		//System.out.println(device.getDeviceInformation().getName()+" connected");
	}
	public void close() {
		closing = true;
		for (PollEventQueue eventQueue : keyboardEventQueues)
			eventQueue.close();
		keyboardEventQueues.clear();
		mouseEventQueue.close();
		for (Keyboard keyboard : keyboards) keyboard = null;
		keyboards.clear();
		mouse = null;
	}

	// ============= Implemented Methods ============== //
	@Override
	public void run(){
		isPolling = true;
		pollNormalMode();
		isPolling = false;
	}
	// ============= Static Methods ============== //
	/**
	 * Returns a list of controllers. Uses native methods.
	 *
	 * @return
	 */
	public static synchronized Controller[] getControllers(boolean firstScan) {
		if (firstScan) return LinuxEnvironmentPlugin.getDefaultEnvironment().getControllers();
		else return LinuxEnvironmentPlugin.getDefaultEnvironment().rescanControllers();
	}

	// ============= Private Classes ============== //
	private class HardwareScanScheduler extends TimerTask {
		@Override
		public void run() { rescanHardware(); }
	}
}