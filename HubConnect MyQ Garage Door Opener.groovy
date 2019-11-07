/**
 * -----------------------
 * --- DEVICE HANDLER ----
 * -----------------------
 *
 *  HubConnect Custom Driver: MyQ Garage Door Opener
 *
 *  Copyright 2019 Steve White & Barry Burke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 */
metadata {
	definition (name: "HubConnect MyQ Garage Door Opener", namespace: "shackrat", author: "Steve White/Barry Burke", vid: "generic-contact-4", ocfdevicetype: "oic.d.garagedoor", mnmn: "SmartThings") {
		capability "Door Control"
		capability "Garage Door Control"
		capability "Contact Sensor"
		capability "Refresh"
		capability "Polling"

		capability "Actuator"
		capability "Switch"
		capability "Momentary"
		capability "Sensor"
		capability "Acceleration Sensor"
		
        //capability "Health Check" Will be needed eventually for new app compatability but is not documented well enough yet
		
		attribute "lastActivity", "string"
        attribute "doorSensor", "string"
//      attribute "doorMoving", "string"
        attribute "OpenButton", "string"
        attribute "CloseButton", "string"
        attribute "myQDeviceId", "string"
		
		attribute "version", "string"
        
//		command "updateDeviceStatus", ["string"]
//		command "updateDeviceLastActivity", ["number"]
//		command "updateDeviceMoving", ["string"]
//		command "updateDeviceSensor", ["string"]
//		command "updateMyQDeviceId", ["string"]
//		command "updateDeviceAcceleration", ["string"]
		command "sync"
	}

	simulator {	}

	tiles {
		
		multiAttributeTile(name:"door", type: "lighting", width: 6, height: 4, canChangeIcon: false) {
			tileAttribute ("device.door", key: "PRIMARY_CONTROL") {
				attributeState "unknown", label:'${name}', icon:"st.doors.garage.garage-closed",    backgroundColor:"#ffa81e"
				attributeState "closed",  label:'${name}', action:"push",   icon:"st.doors.garage.garage-closed",  backgroundColor:"#00a0dc", nextState: "opening"
				attributeState "open",    label:'${name}', action:"push",  icon:"st.doors.garage.garage-open",    backgroundColor:"#e86d13", nextState: "closing"
				attributeState "opening", label:'${name}', action:"push",	 icon:"st.doors.garage.garage-opening", backgroundColor:"#cec236"
				attributeState "closing", label:'${name}', action:"push",	 icon:"st.doors.garage.garage-closing", backgroundColor:"#cec236"
				attributeState "waiting", label:'${name}', action:"push",	 icon:"st.doors.garage.garage-closing", backgroundColor:"#cec236"
				attributeState "stopped", label:'${name}', action:"push",  icon:"st.doors.garage.garage-closing", backgroundColor:"#1ee3ff"
			}
            tileAttribute("device.lastActivity", key: "SECONDARY_CONTROL") {
        		attributeState("lastActivity", label:'Last Activity: ${currentValue}', defaultState: true)
    		}
		}

		standardTile("refresh", "device.door", width: 3, height: 2, decoration: "flat") {
			state("default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh")
		}
		standardTile("contact", "device.contact") {
			state("open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#e86d13")
			state("closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#00a0dc")
		}
		standardTile("switch", "device.switch") {
			state("on", label:'${name}', action: "switch.on",  backgroundColor:"#ffa81e")
			state("off", label:'${name}', action: "switch.off", backgroundColor:"#79b821")
		}		
        standardTile("openBtn", "device.OpenButton", width: 3, height: 3) {
            state "normal", label: 'Open', icon: "st.doors.garage.garage-open", backgroundColor: "#e86d13", action: "open", nextState: "opening"
            state "opening", label: 'Opening', icon: "st.doors.garage.garage-opening", backgroundColor: "#cec236", action: "open"
		}
        standardTile("closeBtn", "device.CloseButton", width: 3, height: 3) {            
            state "normal", label: 'Close', icon: "st.doors.garage.garage-closed", backgroundColor: "#00a0dc", action: "close", nextState: "closing"
            state "closing", label: 'Closing', icon: "st.doors.garage.garage-closing", backgroundColor: "#cec236", action: "close"
		}
        valueTile("doorSensor", "device.doorSensor", width: 3, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'${currentValue}', backgroundColor:"#ffffff"
		}
//		valueTile("doorMoving", "device.doorMoving", width: 6, height: 2, inactiveLavel: false, decoration: "flat") {
//			state "default", label: '${currentValue}', backgroundColor:"#ffffff"
//		}        
        main "door"
		details(["door", "openBtn", "closeBtn", "doorSensor", "refresh"])
	}
}

def push() {	
    parent.sendDeviceEvent(device.deviceNetworkId, "push")
}

def open()  { 
	parent.sendDeviceEvent(device.deviceNetworkId, "open")
}
def close() { 
	parent.sendDeviceEvent(device.deviceNetworkId, "close")
}
def on()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "on")
}
def off()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "off")
}
def refresh() {	        
	parent.sendDeviceEvent(device.deviceNetworkId, "refresh")
}
def poll() { refresh() }
def log(msg){
	log.debug msg
}

def showVersion(){
	return "3.1.1bab"
}
def sync()
{
	// The server will respond with updated status and details
	parent.syncDevice(device.deviceNetworkId, "garagedoor")
	sendEvent([name: "version", value: "v${driverVersion.major}.${driverVersion.minor}.${driverVersion.build}"])
}
def getDriverVersion() {[platform: "Universal", major: 1, minor: 2, build: 1]}
