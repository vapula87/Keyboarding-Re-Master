/* 
 * See LICENSE in top-level directory.
 */
package com.monkygames.kbmaster.driver;

/**
 * Contains information about a specific device.
 * @version 1.0
 */
public class DeviceInformation {

// ============= Class variables ============== //
    /**
     * The make of the device, ie the company name.
     */
    private String make;
    /**
     * The name of the device.
     */
    private String model;
    /**
     * The device name as seen by jinput.
     */
    private String jinputName;
    /**
     * The type of device, ie keyboard,mouse,etc.
     */
    private DeviceType deviceType;
    /**
     * The path to the device icon.
     */
    private String deviceIcon;
    /**
     * Contains information about the device.
     */
    private String deviceDescription;
    /**
     * The full name of the package.
     */
    private String packageName;
    /**
     * The path to the UI FXML file.
     */
    private String uiFXMLURL;
    /**
     * An template image used for printing out configuration.
     */
    private String imageBindingsTemplate;
    /**
     * The link to the amazon product page with the associate code embedded.
     */
    private String amazonLink;
    /**
     * True if this device has a mouse component and false otherwise.
     */
    private boolean hasMouse;

// ============= Constructors ============== //
    public DeviceInformation(String make, String model, String jinputName, 
		   DeviceType deviceType, String deviceIcon, String deviceDescription,
		   String packageName, String uiFXMLURL, String imageBindingsTemplate,
		   String amazonLink, boolean hasMouse) {
        this.make = make;
        this.model = model;
        this.jinputName = jinputName;
        this.deviceType = deviceType;
        this.deviceIcon = deviceIcon;
        this.deviceDescription = deviceDescription;
        this.packageName = packageName;
        this.uiFXMLURL = uiFXMLURL;
        this.imageBindingsTemplate = imageBindingsTemplate;
        this.amazonLink = amazonLink;
        this.hasMouse = hasMouse;
    }

// ============= Public Methods ============== //
    public String getMake() {
	return make;
    }

    public String getModel() {
	return model;
    }

    public String getJinputName() {
	return jinputName;
    }

    public DeviceType getDeviceType() {
	return deviceType;
    }

    public String getDeviceIcon() {
	return deviceIcon;
    }

    public String getDeviceDescription() {
	return deviceDescription;
    }

    public String getPackageName() {
	return packageName;
    }

    public String getUIFXMLURL(){
	return uiFXMLURL;
    }

    public String getImageBindingsTemplate() {
	return imageBindingsTemplate;
    }

    public String getAmazonLink(){
	return amazonLink;
    }

    public boolean hasMouse(){
	return hasMouse;
    }

    @Override
    public boolean equals(Object deviceInformation){
	if(!(deviceInformation instanceof DeviceInformation)){
	    return false;
	}
	DeviceInformation info = (DeviceInformation)deviceInformation;
	if(info.getMake().equals(make) &&
	   info.getModel().equals(model) &&
	   info.getDeviceIcon().equals(deviceIcon) &&
	   info.getDeviceDescription().equals(deviceDescription) &&
	   info.getUIFXMLURL().equals(uiFXMLURL) &&
	   info.getImageBindingsTemplate().equals(imageBindingsTemplate)){
	    return true;
	}
	return false;
    }
    @Override
    public String toString(){
	return "DeviceInformation["+deviceType+","+make+","+model+","+deviceDescription+"]";
    }
    /**
     * Returns the name of this device which is the make+:+model.
     * @return the make and model of the device.
     */
    public String getName(){
	return make+"_"+model;
    }
}

