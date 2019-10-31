/**
 * -----------------------
 * ------ SMART APP ------
 * -----------------------
 *
 *  MyQ Lite
 *
 *  Copyright 2019 Jason Mok/Brian Beaird/Barry Burke/RBoy Apps
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
 */

String appVersion() { return "3.1.0" }
String appModified() { return "2019-10-18"}
String appAuthor() { return "Brian Beaird" }
String gitBranch() { return "brbeaird" }
String getAppImg(imgName) 	{ return "https://raw.githubusercontent.com/${gitBranch()}/SmartThings_MyQ/master/icons/$imgName" }

definition(
	name: "MyQ Lite",
	namespace: "brbeaird",
	author: "Jason Mok/Brian Beaird/Barry Burke",
	description: "Integrate MyQ with ${getIsST()?'SmartThings':'Hubitat'}",
	category: "SmartThings Labs",
	iconUrl:   "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq.png",
	iconX2Url: "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq@2x.png",
	iconX3Url: "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq@3x.png"
)

preferences {
	boolean ST = getIsST()
	boolean HE = !ST
	page(name: "mainPage", title: "${HE?'<h2>':''}MyQ Lite${HE?'</h2>':''}")
    page(name: "prefLogIn", title: "MyQ")
    page(name: "loginResultPage", title: "MyQ")
	page(name: "prefListDevices", title: "MyQ")
    page(name: "sensorPage", title: "MyQ")
    page(name: "noDoorsSelected", title: "MyQ")
    page(name: "summary", title: "MyQ")
    page(name: "prefUninstall", title: "MyQ")
}

def appInfoSect(sect=true)	{
	boolean ST = getIsST()
	boolean HE = !ST
	def str = ""
	str += "${app?.name}"
	str += "\nAuthor: ${appAuthor()}"
	section() { paragraph str, image: getAppImg("myq@2x.png") }
}

def mainPage() {

    if (state.previousVersion == null){
    	state.previousVersion = 0;
    }

    //Brand new install (need to grab version info)
    if (!state.latestVersion){
    	getVersionInfo(0, 0)
        state.currentVersion = [:]
        state.currentVersion['SmartApp'] = appVersion()
    }
    //Version updated
    else if (appVersion() != state.previousVersion){
    	state.previousVersion = appVersion()
        getVersionInfo(state.previousVersion, appVersion());
    }

    //If fresh install, go straight to login page
    if (!settings.username){
    	state.lastPage = "prefListDevices"
        return prefLogIn()
    }

    state.lastPage = "mainPage"

    dynamicPage(name: "mainPage", nextPage: "", uninstall: false, install: true) {
    	getVersionInfo(0,0)
        appInfoSect()
        def devs = refreshChildren()
        section("MyQ Account"){
            paragraph title: "", "Email: ${settings.username}\n"
            href "prefLogIn", title: "", description: "Tap to modify account", params: [nextPageName: "mainPage"]
        }
        section("Connected Devices") {
        	paragraph title: "", "${devs?.size() ? devs?.join("\n") : "No MyQ Devices Connected"}"
            href "prefListDevices", title: "", description: "Tap to modify devices"
        }
        section("App and Handler Versions"){
            state.currentVersion.each { device, version ->
            	paragraph title: "", "${device} ${version} (${versionCompare(device)})"
            }
            input "prefUpdateNotify", "bool", required: false, title: "Notify when new version is available"
        }
        section("Uninstall") {
            paragraph "Tap below to completely uninstall this SmartApp and devices (doors and lamp control devices will be force-removed from automations and SmartApps)"
            href(name: "", title: "",  description: "Tap to Uninstall", required: false, page: "prefUninstall")
        }
		section(""){
			input "isDebug", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: true, submitOnChange: true
			input "isInfo", "bool", title: "Enable Info Logging", required: false, multiple: false, defaultValue: true, submitOnChange: true
		}
    }
}

def versionCompare(deviceName){
    if (!state.currentVersion || !state.latestVersion){return 'checking...'}
    if (state.currentVersion[deviceName] == state.latestVersion[deviceName]){
    	return 'latest'
    }
    else{
   		return "${state.latestVersion[deviceName]} available"
    }
}

def logInfo(msg) {
	if (settings.isInfo) log.info(msg)
}

def refreshChildren(){
	state.currentVersion = [:]
    state.currentVersion['SmartApp'] = appVersion()
    def devices = []
    childDevices.each { child ->	
        def devName = child.name
        if (child.typeName == "MyQ Garage Door Opener"){
			def myQId = child.getMyQDeviceId() ? "ID: ${child.getMyQDeviceId()}" : 'Missing MyQ ID'
        	devName = devName + " (${child.currentContact})  ${myQId}"
            state.currentVersion['DoorDevice'] = child.showVersion()
        }
        else if (child.typeName == "MyQ Garage Door Opener-NoSensor"){
			def myQId = child.getMyQDeviceId() ? "ID: ${child.getMyQDeviceId()}" : 'Missing MyQ ID'
        	devName = devName + " (No sensor)   ${myQId}"
            state.currentVersion['DoorDeviceNoSensor'] = child.showVersion()
		}
        else if (child.typeName == "MyQ Light Controller"){
			def myQId = child.getMyQDeviceId() ? "ID: ${child.getMyQDeviceId()}" : 'Missing MyQ ID'
        	devName = devName + " (${child.currentSwitch})  ${myQId}"
            state.currentVersion['LightDevice'] = child.showVersion()
        }
        else{
        	return	//Ignore push-button devices
		}
        devices.push(devName)
    }
    return devices
}

/* Preferences */
def prefLogIn(params) {
    state.installMsg = ""
    def showUninstall = username != null && password != null
	return dynamicPage(name: "prefLogIn", title: "Connect to MyQ", nextPage:"loginResultPage", uninstall:false, install: false, submitOnChange: true) {
		section("Login Credentials"){
			input("username", "email", title: "Username", description: "MyQ Username (email address)")
			input("password", "password", title: "Password", description: "MyQ password")
		}		
	}
}

def loginResultPage(){
	if (isDebug) log.debug "login result next page: ${state.lastPage}"
    if (forceLogin()) {
    	if (state.lastPage == "prefListDevices")
        	return prefListDevices()
        else
        	return mainPage()
    }
    else{
    	return dynamicPage(name: "loginResultPage", title: "Login Error", install:false, uninstall:false) {
			section(""){
				paragraph "The username or password you entered is incorrect. Go back and try again. "
			}
		}
    }
}

def prefUninstall() {
    if (isDebug) log.debug "Removing MyQ Devices..."
    def msg = ""
    childDevices.each {
		try{
			deleteChildDevice(it.deviceNetworkId, true)
            msg = "Devices have been removed. Tap remove to complete the process."

		}
		catch (e) {
			log.warn "Error deleting ${it.deviceNetworkId}: ${e}"
            msg = "There was a problem removing your device(s). Check the IDE logs for details."
		}
	}

    return dynamicPage(name: "prefUninstall",  title: "Uninstall", install:false, uninstall:true) {
        section("Uninstallation"){
			paragraph msg
		}
    }
}

def getDeviceSelectionList(deviceType){
	def testing
}

def prefListDevices() {
	boolean ST = getIsST()
	boolean HE = !ST
    state.lastPage = "prefListDevices"
    if (login()) {
    	getMyQDevices()

        state.doorList = [:]
        state.lightList = [:]
        state.MyQDataPending.each { id, device ->
        	if (device.typeName == 'door'){
            	state.doorList[id] = device.name
            }
            else if (device.typeName == 'light'){
            	state.lightList[id] = device.name
            }
        }

		if ((state.doorList) || (state.lightList)){
        	def nextPage = "sensorPage"
            if (!state.doorList){nextPage = "summary"}  //Skip to summary if there are no doors to handle
                return dynamicPage(name: "prefListDevices",  title: "${HE?'<h3>':''}Devices${HE?'</h3>':''}", nextPage:nextPage, install:false, uninstall:false) {
                    if (state.doorList) {
                        section("Select which garage door(s)/gate(s) to use"){
                            input(name: "doors", type: "enum", title: "Garage Doors and Gates", required:false, multiple:true, options:state.doorList)
                        }
                    }
                    if (state.lightList) {
                        section("Select which lights to use"){
                            input(name: "lights", type: "enum", title: "Lights", required:false, multiple:true, options:state.lightList)
                        }
                    }
                    section("Advanced (optional)", hideable: true, hidden:true){
        	            paragraph "BETA: Enable the below option if you would like to force the Garage Doors to behave as Door Locks (sensor required)." +
                        			"This may be desirable if you only want doors to open up via PIN with Alexa voice commands. " +
                                    "Note this is still considered highly experimental and may break many other automations/apps that need the garage door capability."
        	            input "prefUseLockType", "bool", required: false, title: "Create garage doors as door locks?"
					}
                }

        }else {
			return dynamicPage(name: "prefListDevices",  title: "Error!", install:false, uninstall:true) {
				section(""){
					paragraph "Could not find any supported device(s). Please report to author about these devices: " +  state.unsupportedList
				}
			}
		}
	} else {
		return prefLogIn([nextPageName: "prefListDevices"])
	}
}


def sensorPage() {
	boolean ST = getIsST()
	boolean HE = !ST
    //If MyQ ID changes, the old stale ID will still be listed in the settings array. Let's get a clean count of valid doors selected
    state.validatedDoors = []
    if (doors instanceof List && doors.size() > 1){
        doors.each {
            if (state.MyQDataPending[it] != null){
                state.validatedDoors.add(it)
            }
        }
    }
    else{
    	state.validatedDoors = doors	//Handle single door
    }

	return dynamicPage(name: "sensorPage",  title: "${HE?'<h3>':''}Optional Sensors and Push Buttons${HE?'</h3>':''}", nextPage:"summary", install:false, uninstall:false/*, refreshInterval: 5*/) {
        def sensorCounter = 1
        state.validatedDoors.each{ door ->
            section("Setup options for " + state.MyQDataPending[door].name){
				if (HE) app.removeSetting("door${sensorCounter}Motion")	// Switching to use Activity instead of Motion
                if (!settings."door${sensorCounter}Sensor") app.updateSetting("door${sensorCounter}SensorActivity", [value: false, type: "bool"])
                //log.debug "0Activity: "+settings["door${sensorCounter}Activity"]?.id+", Sensor: "+settings["door${sensorCounter}Sensor"]?.id+", SensAct: "+settings."door${sensorCounter}SensorActivity"
                if (settings."door${sensorCounter}Sensor" && settings."door${sensorCounter}Activity") {
                	if (settings."door${sensorCounter}Sensor"?.id == settings."door${sensorCounter}Activity"?.id) {
                		if (isDebug) log.debug "${state.MyQDataPending[door].name}-> XYZZY"
                    	app.updateSetting("door${sensorCounter}SensorActivity", [value: true, type: "bool"])
                	} else {
                    	if (isDebug) log.debug "${state.MyQDataPending[door].name}-> YZZYX"
                        app.updateSetting("door${sensorCounter}SensorActivity", [value: false, type: "bool"])
                    }
                }
                if (isDebug) log.debug "${state.MyQDataPending[door].name}-> 1Activity: "+settings."door${sensorCounter}Activity"?.id+", Sensor: "+settings."door${sensorCounter}Sensor"?.id+", SensAct: "+settings."door${sensorCounter}SensorActivity"
				state."needASensor${sensorCounter}" = !(settings["door${sensorCounter}Sensor"] || (settings["door${sensorCounter}Activity"] && 
                																					(settings["door${sensorCounter}ActivityThreeD"] || settings["door${sensorCounter}ThreedD"]))) 
				if (settings."door${sensorCounter}Sensor" || !settings."door${sensorCounter}Activity" || (!settings."door${sensorCounter}ThreeD" && !settings."door${sensorCounter}ActivityThreeD")) {
                	input "door${sensorCounter}Sensor", "capability.contactSensor", required: (state."needASensor${sensorCounter}" ?: false) , multiple: false, title: state.MyQDataPending[door].name + " Contact/Tilt Sensor",
						  submitOnChange: true
					if (settings["door${sensorCounter}Sensor"]) {
                    	app.updateSetting("door${sensorCounter}ActivityThreeD", [value: false, type: "bool"])
						//if (isDebug) paragraph "Using " + settings["door${sensorCounter}Sensor"].displayName + " as the Contact Sensor (" + settings["door${sensorCounter}Sensor"].hasAttribute('acceleration') + ')'
						if (settings["door${sensorCounter}Sensor"].hasAttribute('acceleration')) {
							input "door${sensorCounter}SensorActivity", "bool", defaultValue: true, title: "Also use ${settings["door${sensorCounter}Sensor"].displayName} as the Activity Sensor?", 
								  submitOnChange: true
                            if (isDebug) log.debug "${state.MyQDataPending[door].name}-> 2Activity: "+settings."door${sensorCounter}Activity"?.id+", Sensor: "+settings."door${sensorCounter}Sensor"?.id+", SensAct: "+settings."door${sensorCounter}SensorActivity"
							if (settings."door${sensorCounter}SensorActivity") {
								//settings."door${sensorCounter}Activity" = settings."door${sensorCounter}Sensor"
                                if (isDebug) log.debug "${state.MyQDataPending[door].name}-> sillyme"
                                app.updateSetting("door${sensorCounter}Activity", "")
								//if (isDebug) paragraph "Using " + settings."door${sensorCounter}Activity".displayName + " as the Activity Sensor"
							} else {
								if (settings."door${sensorCounter}Activity" && (settings."door${sensorCounter}Activity"?.id == settings."door${sensorCounter}Sensor"?.id)) {
                                	if (isDebug) log.debug "${state.MyQDataPending[door].name}-> SAME"
                                	app.updateSetting("door${sensorCounter}Activity", "")
                                    app.updateSetting("door${sensorCounter}SensorActivity", [value: true, type: "bool"])
                                }
							}
						}
					} else {
                    	app.updateSetting("door${sensorCounter}SensorActivity", [value: false, type: "bool"])
                    }
				}
				//if (!settings."door${sensorCounter}SensorActivity" && (settings."door${sensorCounter}Activity" == settings."door${sensorCounter}Sensor")) {
				//	app.removeSetting("door${sensorCounter}Activity")
				//}
                if (isDebug) log.debug "${state.MyQDataPending[door].name}-> 3activity: "+settings."door${sensorCounter}Activity"?.id+", sensor: "+settings."door${sensorCounter}Sensor"?.id+", sensAct: "+settings."door${sensorCounter}SensorActivity"
				if ((settings."door${sensorCounter}Sensor" && !settings."door${sensorCounter}SensorActivity") ||
                	!settings."door${sensorCounter}Sensor") { // || !settings."door${sensorCounter}SensorActivity") {
                    if (isDebug) log.debug "${state.MyQDataPending[door].name}-> Activity should be enabled"
					input "door${sensorCounter}Activity", "capability.accelerationSensor", required: (state."needASensor${sensorCounter}" ?: false), multiple: false, title: state.MyQDataPending[door].name + " Activity Sensor",
						  submitOnChange: true
                    //state."door${sensorCounter}ActivityThreeD" = settings."door${sensorCounter}Activity" ? settings."door${sensorCounter}ActivityThreeD" : false
                    //settings."door${sensorCounter}ActivityThreeD" = state."door${sensorCounter}ActivityThreeD"
                    if (isDebug) log.debug "${state.MyQDataPending[door].name}-> 4activity: "+settings."door${sensorCounter}Activity"?.id+", sensor: "+settings."door${sensorCounter}Sensor"?.id+", sensAct: "+settings."door${sensorCounter}SensorActivity"
					if (settings."door${sensorCounter}Activity" 
						&& settings."door${sensorCounter}Sensor" 
						&& (settings."door${sensorCounter}Activity"?.id == settings."door${sensorCounter}Sensor"?.id)) {
                        if (isDebug) log.debug "${state.MyQDataPending[door].name}-> same"
                       	app.updateSetting("door${sensorCounter}SensorActivity", [value: true, type: "bool"])
                        app.updateSetting("door${sensorCounter}Activity", "")
                    } else {
                    	// app.updateSetting("door${sensorCounter}SensorActivity", false)
                    }
                    if (!settings."door${sensorCounter}Activity" ) app.updateSetting("door${sensorCounter}ActivityThreeD", [value: false, type: "bool"])
					//if ((settings."door${sensorCounter}Activity") && isDebug) paragraph "Using " + settings."door${sensorCounter}Activity".displayName + " as the Activity Sensor"
					if (!settings["door${sensorCounter}Sensor"]) {
						if (settings["door${sensorCounter}Activity"] && settings["door${sensorCounter}Activity"].hasAttribute('threeAxis')) {
							input "door${sensorCounter}ActivityThreeD", "bool", defaultValue: settings."door${sensorCounter}ActivityThreeD", 
                            	  title: "Also use ${settings["door${sensorCounter}Activity"].displayName} as the 3D Sensor?",
								  submitOnChange: true, required: (state."needASensor${sensorCounter}" ?: true)
                            //log.debug "Settings: " + settings."door${sensorCounter}ActivityThreeD"
                            //state."door${sensorCounter}ActivityThreeD" = (settings."door${sensorCounter}ActivityThreeD" != null) ? settings."door${sensorCounter}ActivityThreeD" : false
                            //log.debug "State: " + state."door${sensorCounter}ActivityThreeD"
							if (settings."door${sensorCounter}Activity" && !settings."door${sensorCounter}ActivityThreeD" && !settings."door${sensorCounter}Sensor") {
								paragraph "${HE?'<b>':''}WARNING: ${app.label} will not operate reliably without a Contact Sensor -OR- a 3D Sensor. Please select one or the other.${HE?'</b>':''}"
								state."needASensor${sensorCounter}" = true
							}
							if (settings."door${sensorCounter}ActivityThreeD") {
								settings."door${sensorCounter}ThreeD" = settings."door${sensorCounter}Activity"
								//if (isDebug) paragraph "Using " + settings."door${sensorCounter}ThreeD".displayName + " as the ThreeD Sensor"
							}
						} else {
                        	app.updateSetting("door${sensorCounter}ActivityThreeD", [value: false, type: "bool"])
                            app.updateSetting("door${sensorCounter}ThreeD", "")
                        }
					}
				}
                input "prefDoor${sensorCounter}PushButtons", "bool", required: false, title: "Create on/off push buttons?", submitOnChange: true
            }
            sensorCounter++
        }
        if (state.needASensor) state.remove('needASensor')
        section("Sensor setup"){
        	paragraph "For each door, you can specify an optional Contact/Tilt Sensor or a multi-sensor configured as a garage door sensor to recognize open/close more reliably. "+
            		  "You can also use an optional Activity Sensor for more accurate reporting when doors are opened manually or from another application. " +
                      "\nIf you don't have a Contact Sensor or a multi-sensor you can configure for use on a garage door, you can enable a multi-sensor's 3D Sensor to emulate a " +
                      "tilt-tracking Contact Sensor." +
					  "\n\nNOTE: Only 3 combinations are supported:" +
                      "\n * A Contact (or Tilt) Sensor, optionally with a separate Activity Sensor" +
					  "\n * A multi-sensor configured as a Contact Sensor, optionally also acting as an Activity Sensor" +
					  "\n * A multi-sensor acting as an Activity Sensor, and also using its 3D Sensor to emulate a Tilt/Contact Sensor"
			//paragraph "These also help the device function as a switch so that you can turn on (to open) and off " +
			//		  "(to close) in other automations and SmartApps."
           	paragraph "In addition, you can choose to create separate On/Open and Off/Close push button devices. This is recommened if you" +
					  "want a way to open/close the garage door from interfaces like Google Home that can't function with the built-in open/close or on/off capability."
			paragraph "See wiki for more details (need URL)"
        }
    }
}

def summary() {
	state.installMsg = ""
    try{
    	initialize()
    }

    //If error thrown during initialize, try to get the line number and display on installation summary page
    catch (e){
		def errorLine = "unknown"
        try{
        	if (isDebug) log.debug e.stackTrace
            def pattern = ( e.stackTrace =~ /groovy.(\d+)./   )
            errorLine = pattern[0][1]
        }
        catch(lineError){}

		log.error "Error at line number ${errorLine}: ${e}"
        state.installMsg = "There was a problem updating devices:\n ${e}.\nLine number: ${errorLine}\nLast successful step: ${state.lastSuccessfulStep}"
        throw e
    }

    return dynamicPage(name: "summary",  title: "Summary", install:true, uninstall:true) {
        section("Installation Details:"){
			paragraph state.installMsg
		}
    }
}

/* Initialization */
def installed() {
	logInfo "MyQ Lite installed; platform is ${getHubPlatform()}"    
}

def updated() {
	logInfo "MyQ Lite changes saved; platform is ${getHubPlatform()}"    
    unschedule()
    runEvery3Hours(updateVersionInfo)   //Check for new version every 3 hours
    
    if (door1Sensor && state.validatedDoors){
    	refreshAll()
    	runEvery30Minutes(refreshAll)
    }
    stateCleanup()
}

/* Version Checking */

//Called from scheduler every 3 hours
def updateVersionInfo(){
	getVersionInfo('versionCheck', '0')
}

//Get latest versions for SmartApp and Device Handlers
def getVersionInfo(oldVersion, newVersion){
    //Don't check for updates more 5 minutes
	return
	
    if (state.lastVersionCheck && (now() - state.lastVersionCheck) / 1000/60 < 5 ){
    	return
    }
    state.lastVersionCheck = now()
    log.info "Checking for latest version..."
    def params = [
        uri:  'http://www.brbeaird.com/getVersion/myq/' + oldVersion + '/' + newVersion,
        contentType: 'application/json'
    ]
    def callbackMethod = oldVersion == 'versionCheck' ? 'updateCheck' : 'handleVersionUpdateResponse'
    asynchttpGet(callbackMethod, params)
}

//When version response received (async), update state with the data
def handleVersionUpdateResponse(response, data) {
    if (response.hasError()) {
        log.error "Error getting version info: ${response.errorMessage}"
    }
    else {state.latestVersion = response.json}
}

//In case of periodic update check, also refresh installed versions and update the version warning message
def updateCheck(response, data) {
	handleVersionUpdateResponse(response,data)
    refreshChildren()
    updateVersionMessage()
}

def updateVersionMessage(){
	state.versionMsg = ""
    state.currentVersion.each { device, version ->
    	if (versionCompare(device) != 'latest'){
        	state.versionMsg = "MyQ Lite Updates are available."
    	}
    }

    //Notify if updates are available
    if (state.versionMsg != ""){

        //Send push notification if enabled
        if (prefUpdateNotify){

            //Don't notify if we've sent a notification within the last 1 day
            if (state.lastVersionNotification){
            	def timeSinceLastNotification = (now() - state.lastVersionNotification) / 1000
                if (timeSinceLastNotification < 60*60*23){
                	return
                }
            }
            state.lastVersionNotification = now()
    	}
    }
}


def uninstall(){
    logInfo "Removing MyQ Devices..."
    childDevices.each {
		try{
			deleteChildDevice(it.deviceNetworkId, true)
		}
		catch (e) {
			log.warn "Error deleting ${it.deviceNetworkId}: ${e}"
		}
	}
}

def uninstalled() {
	if (isDebug) log.debug "MyQ removal complete."
    getVersionInfo(state.previousVersion, 0);
}


def initialize() {
	boolean ST = ((atomicState?.isST != null) ? atomicState.isST : isST)
	boolean HE = !ST
	
    log.info "Initializing..."
    state.data = state.MyQDataPending
    state.lastSuccessfulStep = ""
    unsubscribe()

    //Check existing installed devices against MyQ data
    verifyChildDeviceIds()

    //Mark sensors onto state door data
    def doorSensorCounter = 1
	String doorSensorText = ""
	String warnings = ""
    state.validatedDoors.each{ door ->
    	if (state."needASensor${doorSensorCounter}") state.remove("needASensor${doorSensorCounter}")	// cleaup up after setup
		doorSensorText += ">> ${state.data[door].name} sensors: "
		boolean aSensor = false
		def verified = null
        if (settings["door${doorSensorCounter}Sensor"]){
            state.data[door].sensor = "door${doorSensorCounter}Sensor"
			String contactName = settings["door${doorSensorCounter}Sensor"].displayName
			doorSensorText += "${contactName} (contact"
			aSensor = true
			def currentContact = settings["door${doorSensorCounter}Sensor"].currentValue('contact')
			if (state.data[door].status != currentContact) {
				String warning = "DOOR STATUS MISMATCH: ${contactName}: ${currentContact}, MyQ: ${state.data[door].status}"
				log.warn warning
				warnings += warning + "\n"
			} else {
				verified = true					// True if we have ONLY a contact sensor
			}
			if (settings["door${doorSensorCounter}SensorActivity"]) {
				state.data[door].activity = "door${doorSensorCounter}Sensor"
				doorSensorText += ", activity)"
			} else if (settings["door${doorSensorCounter}Activity"]) {
				state.data[door].activity = "door${doorSensorCounter}Activity"
				String activityName = settings["door${doorSensorCounter}Activity"].displayName
				doorSensorText += "), ${activityName} (activity)"
			} else {
				doorSensorText += ")"
			}
		} else if (settings["door${doorSensorCounter}Activity"]){
			state.data[door].activity = "door${doorSensorCounter}Activity"
			String activityName = settings["door${doorSensorCounter}Activity"].displayName
			doorSensorText += "${aSensor?',':''} ${activityName} (activity"
			aSensor = true
			if (settings["door${doorSensorCounter}ActivityThreeD"]) {
				state.data[door].threed = "door${doorSensorCounter}Activity"
				//let's see if we can get the axis
				state.data[door].axis = ""	
				state.data[door].verified = false
				def command = state.data[door].status == "closed" ? "open" : (state.data[door].status == "open" ? "close" : null)
				find3DAxis(door, command)
				verified = state.data[door].verified
				doorSensorText += ", 3D: ${state.data[door].axis} ${verified?'verified':'tbd'})"
			} else {
				doorSensorText += ")"
				state.data[door].axis = "none"
				verified = true
			}
		}
		state.data[door].verified = (verified != null) ? verified : true // If no sensors, no need to verify the axis
		doorSensorText += "\n"
		doorSensorCounter++
    }
	if (doorSensorText != "") state.installMsg = state.installMsg + (doorSensorText + "\n")
    log.debug "DST: ${doorSensorText}"
    state.lastSuccessfulStep = "Sensor Indexing"

    //Create door devices
    def doorCounter = 1
    state.validatedDoors.each{ door ->
		// Either sensor will require the "Sensor" DTH/Driver
		def theSensor = settings[state.data[door].sensor] ?: settings[state.data[door].activity]
        createChilDevices(door, theSensor, state.data[door].name, settings["prefDoor${doorCounter}PushButtons"])
        doorCounter++
    }
    state.lastSuccessfulStep = "Door device creation"


    //Create light devices
    if (lights){
        state.validatedLights = []
        if (lights instanceof List && lights.size() > 1){
            lights.each { lightId ->
                if (state.data[lightId] != null){
                    state.validatedLights.add(lightId)
                }
            }
        }
        else{
            state.validatedLights = lights
        }
        state.validatedLights.each { light ->
            if (light){
                def myQDeviceId = state.data[light].myQDeviceId
                def DNI = [ app.id, "LightController", myQDeviceId ].join('|')
                def lightName = state.data[light].name
                def childLight = getChildDevice(state.data[light].child)
                
                if (!childLight) {
                    logInfo "Creating child light device: " + light

                    try{
                        childLight = addChildDevice("brbeaird", "MyQ Light Controller", DNI, getHubID(), ["name": lightName])
                        state.data[myQDeviceId].child = DNI
                        state.installMsg = state.installMsg + lightName + ": created light device. \r\n\r\n"
                    }
                    //catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
					catch (Exception e) {
						if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) { 
							log.error "Error! ${e}"
							state.installMsg = state.installMsg + lightName + ": problem creating light device. Check your IDE to make sure the brbeaird : MyQ Light Controller device handler is installed and published. \r\n\r\n"
						} else throw e
                    }
                }
                else{
                    log.warn "Light device already exists: " + lightName
                    state.installMsg = state.installMsg + lightName + ": light device already exists. \r\n\r\n"
                }
                logInfo "Setting ${lightName} status to ${state.data[light].status}"
                childLight.updateDeviceStatus(state.data[light].status)
            }
        }
        state.lastSuccessfulStep = "Light device creation"
    }

    // Remove unselected devices
    getChildDevices().each{ child ->
    	if (isDebug) log.debug "Checking ${child} for deletion"
		if (child.typeName != 'Momentary Button Tile'){		// unused pushbuttons were deleted in createChildDevices
			def myQDeviceId = child.getMyQDeviceId()
			if (myQDeviceId){
				if (!(myQDeviceId in state.validatedDoors) && !(myQDeviceId in state.validatedLights)){
					try{
						if (isDebug) log.debug "Child ${child} with ID ${myQDeviceId} not found in selected list. Deleting."
						if (ST) deleteChildDevice(child.deviceNetworkId, true)
						if (HE) deleteChildDevice(child.deviceNetworkId)
						if (isDebug) log.debug "Removed old device: ${child}"
						state.installMsg = state.installMsg + "Removed old device: ${child} \r\n\r\n"
					}
					catch (e)
					{
						log.error "Error trying to delete device: ${child} - ${e}\nDevice is likely in use in a Routine, or SmartApp (make sure and check Alexa, ActionTiles, etc.)."
					}
				}
			}
		}
    }
    state.lastSuccessfulStep = "Old device removal"

    //Set initial values
    if (state.validatedDoors){
    	syncDoorsWithSensors()
    }
    state.lastSuccessfulStep = "Setting initial values"

	//Subscribe to Sensor Events
    state.validatedDoors.each{ door ->
		if (state.data[door].sensor) 	subscribe(settings[state.data[door].sensor],	"contact",		sensorHandler)
		if (state.data[door].activity)	subscribe(settings[state.data[door].activity],	"acceleration", activityHandler)
		if (state.data[door].threed)	subscribe(settings[state.data[door].threed],	"threeAxis",	threeDHandler)
	}
	state.lastSuccessfulStep = "Subscribing to events"
}

def verifyChildDeviceIds(){
	//Try to match existing child devices with latest MyQ data
    childDevices.each { child ->        
        def matchingId
        if (child.typeName != 'Momentary Button Tile'){
            //Look for a matching entry in MyQ
            state.data.each { myQId, myQData ->
                if (child.getMyQDeviceId() == myQId){
                    logInfo "Found matching ID for ${child}"
                    matchingId = myQId
                }

                //If no matching ID, try to match on name
                else if (child.name == myQData.name || child.label == myQData.name){
                    logInfo "Found matching ID (via name) for ${child}"
                    child.updateMyQDeviceId(myQId)	//Update child to new ID
                    matchingId = myQId
                }
            }

            if (isDebug) log.debug "final matching ID for ${child.name} ${matchingId}"
            if (matchingId){
                state.data[matchingId].child = child.deviceNetworkId
            }
            else{
                log.warn "WARNING: Existing child ${child} does not seem to have a valid MyQID"
            }
        }
    }
}

def createChilDevices(door, sensor, doorName, prefPushButtons){
	boolean ST = ((atomicState?.isST != null) ? atomicState.isST : isST)
	boolean HE = !ST
	
    def sensorTypeName = "MyQ Garage Door Opener"
    def noSensorTypeName = "MyQ Garage Door Opener-NoSensor"
    def lockTypeName = "MyQ Lock Door"

    if (door){

    	def myQDeviceId = state.data[door].myQDeviceId
        def DNI = [ app.id, "GarageDoorOpener", myQDeviceId ].join('|')

        //Has door's child device already been created?
        def existingDev = getChildDevice(state.data[door].child)
        def existingType = existingDev?.typeName

        if (existingDev){
        	if (isDebug) log.debug "Child already exists for " + doorName + ". Sensor name is: " + sensor
            state.installMsg = state.installMsg + doorName + ": door device already exists. \r\n\r\n"

            if (prefUseLockType && existingType != lockTypeName){
                try{
                    if (isDebug) log.debug "Type needs updating to Lock version"
                    existingDev.deviceType = lockTypeName
                    state.installMsg = state.installMsg + doorName + ": changed door device to lock version." + "\r\n\r\n"
                }
                //catch(hubitat.exception.NotFoundException e)
				catch (Exception e) 
                {
					if ("${e}".startsWith("${ST?'physicalgraph':'hubitat'}.exception.NotFoundException")) { 
                    	log.error "Error! " + e
                    	state.installMsg = state.installMsg + doorName + ": problem changing door to no-sensor type. Check your IDE to make sure the brbeaird : " + lockTypeName + 
											" device handler is installed and published. \r\n\r\n"
					} else throw e
                }
            }
            else if ((!sensor) && existingType != noSensorTypeName){
            	try{
                    if (isDebug) log.debug "Type needs updating to no-sensor version"
                    existingDev.deviceType = noSensorTypeName
                    state.installMsg = state.installMsg + doorName + ": changed door device to No-sensor version." + "\r\n\r\n"
                }
                //catch(hubitat.exception.NotFoundException e)
				catch (Exception e) 
                {
					if ("${e}".startsWith("${ST?'physicalgraph':'hubitat'}.exception.NotFoundException")) { 
                    	log.error "Error! " + e
                    	state.installMsg = state.installMsg + doorName + ": problem changing door to no-sensor type. Check your IDE to make sure the brbeaird : " + noSensorTypeName + 
											" device handler is installed and published. \r\n\r\n"
					} else throw e
                }
            }

            else if (sensor && existingType != sensorTypeName && !prefUseLockType){
            	try{
                    if (isDebug) log.debug "Type needs updating to sensor version"
                    existingDev.deviceType = sensorTypeName
                    state.installMsg = state.installMsg + doorName + ": changed door device to sensor version." + "\r\n\r\n"
                }
                //catch(hubitat.exception.NotFoundException e)
				catch (Exception e) 
                {
					if ("${e}".startsWith("${ST?'physicalgraph':'hubitat'}.exception.NotFoundException")) { 
                    	log.error "Error! " + e
                    	state.installMsg = state.installMsg + doorName + ": problem changing door to sensor type. Check your IDE to make sure the brbeaird : " + sensorTypeName + 
											" device handler is installed and published. \r\n\r\n"
					} else throw e
                }
            }
        }
        else{
            log.debug "Creating child door device " + door
            def childDoor

            if (prefUseLockType){
                try{
                    if (isDebug) log.debug "Creating door with lock type"
                    childDoor = addChildDevice("brbeaird", lockTypeName, DNI, getHubID(), ["name": doorName])
                    childDoor.updateMyQDeviceId(myQDeviceId)
                    state.installMsg = state.installMsg + doorName + ": created lock device \r\n\r\n"
                }
                // catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
				catch (Exception e) {
					if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) { 
						log.error "Error! ${e}"
                    	state.installMsg = state.installMsg + doorName + ": problem creating door device (lock type). Check your IDE to make sure the brbeaird : " + sensorTypeName + 
											" device handler is installed and published. \r\n\r\n"
					} else throw e
                }
            }

            else if (sensor){
                try{
                    if (isDebug) log.debug "Creating door with sensor(s)"
                    log.trace sensorTypeName + " ${DNI}, ${getHubID()}, ${doorName}"
                    childDoor = addChildDevice("brbeaird", sensorTypeName, DNI, getHubID(), ["name": doorName])
                    childDoor.updateMyQDeviceId(myQDeviceId)
                    state.installMsg = state.installMsg + doorName + ": created door device (sensor version) \r\n\r\n"
                }
                //catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
				catch (Exception e) {
					if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) { 
						log.error "Error! ${e}"
                    	state.installMsg = state.installMsg + doorName + ": problem creating door device (sensor type). Check your IDE to make sure the brbeaird : " + sensorTypeName + 
											" device handler is installed and published. \r\n\r\n"
					} else throw e
                }
            }
            else{
                try{
                    if (isDebug) log.debug "Creating door with no sensor"
                    childDoor = addChildDevice("brbeaird", noSensorTypeName, DNI, getHubID(), ["name": doorName])
                    childDoor.updateMyQDeviceId(myQDeviceId)
                    state.installMsg = state.installMsg + doorName + ": created door device (no-sensor version) \r\n\r\n"
                }
                //catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
				catch (Exception e) {
					if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) { 
						log.error "Error! ${e}"
                    	state.installMsg = state.installMsg + doorName + ": problem creating door device (no-sensor type). Check your IDE to make sure the brbeaird : " + noSensorTypeName + 
							" device handler is installed and published. \r\n\r\n"
					} else throw e
                }
            }
            state.data[door].child = childDoor.deviceNetworkId
        }

        //Create push button devices
        if (prefPushButtons){
        	def existingOpenButtonDev = getChildDevice(door + " Opener")
            def existingCloseButtonDev = getChildDevice(door + " Closer")
            if (!existingOpenButtonDev){
                try{
                	def openButton = addChildDevice("smartthings", "Momentary Button Tile", door + " Opener", getHubID(), [name: doorName + " Opener", label: doorName + " Opener"])
                	state.installMsg = state.installMsg + doorName + ": created push button device. \r\n\r\n"
                	subscribe(openButton, "momentary.pushed", doorButtonOpenHandler)
					if (isDebug) log.debug "created and subscribed to button: ${openButton.displayName}"
                }
                //catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
				catch (Exception e) {
					if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) { 
						log.error "Error! ${e}"
                    	state.installMsg = state.installMsg + doorName + ": problem creating push button device. Check your IDE to make sure the smartthings : Momentary Button Tile device handler " +
											"is installed and published. \r\n\r\n"
					} else throw e
                }
            }
            else{
            	subscribe(existingOpenButtonDev, "momentary.pushed", doorButtonOpenHandler)
                state.installMsg = state.installMsg + doorName + ": push button device already exists. Subscription recreated. \r\n\r\n"
				if (isDebug) log.debug "subscribed to button: ${existingOpenButtonDev.displayName}"
            }

            if (!existingCloseButtonDev){
                try{
                    def closeButton = addChildDevice("smartthings", "Momentary Button Tile", door + " Closer", getHubID(), [name: doorName + " Closer", label: doorName + " Closer"])
                    subscribe(closeButton, "momentary.pushed", doorButtonCloseHandler)
					if (isDebug) log.debug "created and subscribed to button: ${closeButton.displayName}"
                }
                //catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
				catch (Exception e) {
					if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) { 
						log.error "Error! ${e}"
					} else throw e
                }
            }
            else{
                subscribe(existingCloseButtonDev, "momentary.pushed", doorButtonCloseHandler)
				if (isDebug) log.debug "subscribed to button: ${existingCloseButtonDev.displayName}"
            }
        }

        //Cleanup defunct push button devices if no longer wanted
        else{
        	def pushButtonIDs = [door + " Opener", door + " Closer"]
            def devsToDelete = getChildDevices().findAll { pushButtonIDs.contains(it.deviceNetworkId)}
            if (isDebug) log.debug "button devices to delete: " + devsToDelete
			devsToDelete.each{
            	if (isDebug) log.debug "deleting button: " + it
                try{
                	ST ? deleteChildDevice(it.deviceNetworkId, true) : deleteChildDevice(it.deviceNetworkId)
                } catch (e){
                    state.installMsg = state.installMsg + "Warning: unable to delete virtual on/off push button - you'll need to manually remove it. \r\n\r\n"
                    log.error "Error trying to delete button " + it + " - " + e + "\nButton  is likely in use in a Routine, or SmartApp (make sure and check SmarTiles!)."
                }
            }
        }
    }
}


def syncDoorsWithSensors(child){
	if (child && isDebug) log.debug "syncDoorsWithSensors: ${child.device}, ${child.device.deviceNetworkId}"
	if (child) {
		def door = child.device.deviceNetworkId.split("\\|").last()
		def sensor = settings[state.data[door].sensor] ?: (settings[state.data[door].threed] ?: null)
		logInfo "Synchronizing ${child.device.displayName} from ${sensor?.displayName}"
		updateDoorStatus(child.device.deviceNetworkId, sensor, child)
	} else {
		state.validatedDoors.each { door ->
			// Update door.contact
			def sensor = settings[state.data[door].sensor] ?: (settings[state.data[door].threed] ?: null)
			logInfo "Synchronizing ${getChildDevice(state.data[door].child)?.displayName} from ${sensor?.displayName}"
			updateDoorStatus(state.data[door].child, sensor, null)
		}
	}
}

def updateDoorStatus(doorDNI, sensor, child){
	def ST = atomicState.isST
    try{
        if (isDebug) log.debug "Updating door status: ${doorDNI} ${sensor} ${child}"

        if (!sensor){//If we got here somehow without a sensor, bail out
        	log.warn "Warning: no sensor found for ${doorDNI}"
            return 0
		}
		if (!doorDNI){
        	log.warn "Invalid doorDNI for sensor ${sensor} ${child}"
            return 0
        }

        //Get door to update and set the new value
        def doorToUpdate = getChildDevice(doorDNI)
        String doorName = doorToUpdate.displayName
		String door = doorDNI.split("\\|").last()
		
		// Get the current Acceleration sensor value (if we have one)
		String doorActivity = doorToUpdate.latestValue('acceleration')
		String currentActivity
		if (state.data[door].activity) {
			currentActivity = settings[state.data[door].activity].latestValue('acceleration') ?: 'unknown'
//			if (currentActivity == 'unknown') log.error "${doorName} - CURRENTACTIVITY IS UNKNOWN!!!"							   
//			if ((currentActivity != 'unknown') && (doorActivity != currentActivity)) {
//				if (isDebug) log.debug "${doorName} - Updating acceleration --> ${currentActivity}"
//				doorToUpdate.updateDeviceAcceleration(currentActivity)
//				doorActivity = currentActivity
//			}
		} else if (doorActivity != "unknown") {
			//No Activity Sensor, so we will never know what the acceleration is
			doorToUpdate.updateDeviceAcceleration('unknown')
			doorActivity = "unknown"
			if (isDebug) log.debug "${doorName} - Updating acceleration --> unknown"
		}
        //Get current Contact or 3D Emultated Tilt Sensor value (we will only have one or the other, not both)
        def currentSensorContact = getCurrentContact(door)
		def sensorName = sensor.displayName
		// if we're using contact as activity, or activity as threed contact, 
		//boolean sameSame = (state.data[door].sensor && (sensor == state.data[door].sensor)) ? (sensor == state.data[door].activity) 
		//																					: ((state.data[door].activity && (sensor == state.data[door].activity)) ? (sensor == state.data[door].threed) : false)
		if (isDebug) log.debug "${doorName} - Using ${sensorName} --> ${currentSensorContact}"
        def currentDoorContact = doorToUpdate.latestValue("contact")
		def currentDoorStatus = doorToUpdate.latestValue('door')

		if (isDebug) log.debug "${doorName} - door.contact: ${currentDoorContact}, sensor.contact: ${currentSensorContact}, door.door: ${currentDoorStatus}, door.acceleration: ${doorActivity}"
        //If sensor and door are out of sync, update the door
		if ((currentDoorContact != currentSensorContact) || (currentDoorStatus != currentSensorContact)){    	
			boolean moving = false
			if (currentSensorContact == 'open') {
				// Handle the special cases where we have an Activity Sensor
				if ((currentDoorStatus == 'opening') && (doorActivity == 'active')) {
					// the Contact is open, the door is opening, and the door is still moving
					logInfo "${doorName} - Updating door.contact from ${sensorName} --> opening"
					doorToUpdate.updateDeviceContact('open')
					doorToUpdate.updateDeviceSensor("${sensorName} is open (opening)")
					//door is still moving, so don't change to 'open' until it stops
					moving = true
				} else if ((currentDoorStatus == 'closing') && (doorActivity == 'active')) {
					// The Contact is open, the door is closing, and the door is still moving
					logInfo "${doorName} - Updating door.contact from ${sensorName} --> closing"
					doorToUpdate.updateDeviceContact('open')
					doorToUpdate.updateDeviceSensor("${sensorName} is open (closing)")
					// door is still moving, so don't change to 'closed' until it stops
					moving = true
				}
			}
			if (!moving) {
				if (currentSensorContact != 'unknown') {
					logInfo "${doorName} - Updating door.contact from ${sensorName} --> ${currentSensorContact}" // Door is open or closed only
					doorToUpdate.updateDeviceStatus(currentSensorContact)
					if (currentDoorContact != currentSensorContact) doorToUpdate.updateDeviceContact(currentSensorContact)
					doorToUpdate.updateDeviceSensor("${sensorName} is ${currentSensorContact}")
					state.data[door].status = currentSensorContact
				}
			}
			
            //Write to child log if this was initiated from one of the doors
            if (child){child.log("Updating as '${currentSensorContact}' from sensor ${sensorName}")}

            //Get latest activity timestamp for the sensor (data saved for up to a week)
            //def eventsSinceYesterday = sensor.eventsSince(new Date() - 7)
            //def latestEvent = eventsSinceYesterday[0]?.date
			def statesSinceLastWeek = doorToUpdate.statesSince("door", new Date() -7)
			def latestEvent = statesSinceLastWeek[0]?.date

            //Update timestamp
            if (latestEvent){
            	doorToUpdate.updateDeviceLastActivity(latestEvent)
				state.data[door].lastAction = latestEvent
            }
            //else{	//If the door has been inactive for more than a week, timestamp data will be null. Keep current value in that case.
            //	timeStampLogText = "Door: " + doorName + ": Null door action timestamp detected "  + /*" -  from sensor " + sensor + */ ". Keeping current value."
            //}
        }
    } catch (e) {
        log.error "Error updating door: ${getChildDevice(doorDNI).displayName}: ${e}"
		throw e
    }
}

def refresh(child){
	if (isDebug) log.debug "refresh: ${child.device}, ${child.device.deviceNetworkId}, ${child.device.displayName}"
    def door = child.device.deviceNetworkId
	def doorName = child.device.displayName
    child.log("refresh called from " + doorName + ' (' + door + ')')
    syncDoorsWithSensors(child)
}

def refreshAll(){
    syncDoorsWithSensors()
}

def refreshAll(evt){
	refreshAll()
}

def sensorHandler(evt) {
	if (isDebug) log.debug "Contact Sensor detected from device ${evt.device.displayName}, name: ${evt.name}, value: ${evt.value}, deviceID: ${evt.deviceId}"
    state.validatedDoors.each{ door ->
		if (settings[state.data[door].sensor]?.id.toString() == evt.deviceId.toString()) {
			boolean sameSame = (state.data[door].sensor && (state.data[door].sensor == state.data[door].activity))
			def theDoor = getChildDevice(state.data[door].child)
			theDoor.updateDeviceContact(evt.value)	// change the contact state - other events or timers will do the full update later
			if (evt.value == 'open') {
				// if using the same sensor for Contact and Activity, we can overload the Activity status
				theDoor.updateDeviceAcceleration('active')
				if (theDoor.latestValue('doorSensor').endsWith('closed (opening)')) theDoor.updateDeviceSensor("${evt.device.displayName} is open (opening)")
			} else if (evt.value == 'closed') {
				theDoor.updateDeviceAcceleration('inactive')
            	updateDoorStatus(state.data[door].child, settings[state.data[door].sensor], null)
			}
		}
    }
}

def threeDHandler(evt) {
	if (isDebug) log.debug "3D Sensor change from device ${evt.device.displayName}, name: ${evt.name}, value: ${evt.value}, deviceID: ${evt.deviceId}"
	state.validatedDoors.each{ door ->
		if (settings[state.data[door].threed]?.id.toString() == evt.deviceId.toString()) {
			updateDoorStatus(state.data[door].child, settings[state.data[door].threed], null)
		}
	}
}

String check3DContact(door, xyz = null) {
	String axis = state.data[door].axis
	def threed 
	if (axis && (axis != "none")) {
		if (xyz != null) {
			//A threeAxis event returns a string value, while a currentValue('threeAxis') returns the map we are creating here
			xyz = xyz[1..-2].split(',').collectEntries {
				entry -> def pair = entry.split(':')
				[(pair.first()): pair.last() as Integer]
			}
		} else {
			threed = settings[state.data[door].threed]
			try {
				xyz = threed?.latestValue('threeAxis')
			} catch (e) {
				log.warn "Could not read current xyzValues of ${motion.displayName}"
				return "unknown"
			}
		}
        // log.debug "xyz: ${xyz}, " + xyz."${axis}".toString() + " (${axis})"
		// 3D values aren't "closed" until the door stops moving (reports inactive)
		return ((Math.abs(xyz."${axis}") > 900) && (threed?.latestValue('acceleration') == 'inactive')) ? 'closed' : 'open'
	} else {
		return "unknown"		// we probably haven't calculated our axis yet...
	}
}

String getCurrentContact(door) {
	// Return the current contact value, from either the Contact Sensor or the 3D Sensor
	String contact = "unknown"
	
	if (state.data[door]?.sensor) {
		contact = settings[state.data[door]?.sensor].latestValue('contact')
	} else if (state.data[door]?.threed) {
		contact = check3DContact(door)
	}
	if (isDebug) log.debug "getCurrentContact(${getChildDevice(state.data[door].child)?.displayName}) --> ${contact}"
	return contact	  
}
	
def activityHandler(evt) {
	if (isDebug) log.debug "Activity change from device ${evt.device.displayName}, name: ${evt.name}, value: ${evt.value}, deviceID: ${evt.deviceId}"
	state.validatedDoors.each{ door ->
		if (settings[state.data[door].activity]?.id.toString() == evt.deviceId.toString()) {
			def theDoor = getChildDevice(state.data[door].child)
			if (theDoor.latestValue('acceleration') != evt.value) theDoor.updateDeviceAcceleration(evt.value)
			
			// update the door's contact status if we don't have an actual contact sensor, otherwise let the actual contact sensor do the update on its own
			// log.trace "activity: ${evt.value}, door.contact: ${theDoor.latestValue('contact')}, currentContact: ${getCurrentContact(door)}"
			//if (!state.data[door].sensor && evt.device.hasAttribute('threeAxis')) updateDoorStatus(state.data[door].child, evt.device, null)
			//log.trace "activity: ${evt.value}, door.contact: ${theDoor.latestValue('contact')}, check3D: ${check3DContact(door)}"
			
			// If the door's Activity Sensor is the same as its Contact Sensor, then we can safely update Contact based on Activity
			boolean sameSame = state.data[door].sensor ? (state.data[door].sensor == state.data[door].activity) 
            										   : (state.data[door].activity ? (state.data[door].activity == state.data[door].threed) : false)
			def doorName = theDoor.displayName
			def doorContact = theDoor.latestValue('contact')
			def currentDoor = theDoor.latestValue('door')
			def sensor = (settings[state.data[door].sensor] ?: (settings[state.data[door].threed] ?: null))
			def currentContact = getCurrentContact(door)
			if (isDebug) log.debug "activityHandler() - evt.value: ${evt.value}, doorContact: ${doorContact}, currentDoor: ${currentDoor}, sensor: ${sensor}"
			if (evt.value == 'active') {
				if (doorContact) {
					if (((currentDoor == 'open') || (currentDoor == 'waiting')) && (doorContact == 'open')) {
						theDoor.updateDeviceStatus("closing")
						logInfo "Updating ${doorName} from ${evt.device.displayName} --> closing"
						if (sensor) theDoor.updateDeviceSensor("${sensor.displayName} is open (closing)")
						state.data[door].status = "closing"
					} else if (((currentDoor == 'closed') || (currentDoor == 'opening')) && (doorContact == 'closed')) {
						if (currentDoor != 'opening') theDoor.updateDeviceStatus("opening")
						// if (sameSame && (doorContact != 'open') || !state.data[door].sensor) theDoor.updateDeviceContact("open")
						theDoor.updateDeviceContact("open")
						logInfo "Updating ${doorName} from ${evt.device.displayName} --> opening"
						//String cval = sameSame ? 'open' : 'closed'
						if (sensor) theDoor.updateDeviceSensor("${sensor.displayName} is open (opening)")
						state.data[door].status = "opening"
					} //else if ((currentDoor == 'opening') && (doorContact == 'open')) {
						//Door is already opening, just update the "doorSensor" attribute (it may have said "closed (opening)", from above)
						//if (sensor) theDoor.updateDeviceSensor("${sensor.displayName} is open (opening)")
						//state.data[door].status = "opening"
					//}
				} else {
					// We don't have a contact sensor of any type
					if (currentDoor == 'open') {
						theDoor.updateDeviceStatus("closing")
						logInfo "Updating ${doorName} from ${evt.device.displayName} --> closing"
						state.data[door].status = "closing"
					} else if (currentDoor == 'closed') {
						theDoor.updateDeviceStatus("opening")
						logInfo "Updating ${doorName} from ${evt.device.displayName} --> opening"
						if (sensor) theDoor.updateDeviceSensor("${sensor.displayName} is open")
						state.data[door].status = "opening"
					}
				}
			} else { // inactive
				if (doorContact) {
					if ((currentDoor == 'opening') && (currentContact == 'open')) {
						theDoor.updateDeviceStatus('open')
						if (sameSame || !state.data[door].sensor) theDoor.updateDeviceContact('open')
						if (sensor) theDoor.updateDeviceSensor("${sensor.displayName} is open")
						state.data[door].status = "open"
					} else if (((currentDoor == 'closing') || (currentDoor == 'waiting')) && (currentContact == 'closed')) {
						theDoor.updateDeviceStatus('closed')
						if (sameSame || !state.data[door].sensor) theDoor.updateDeviceContact('closed')
						if (sensor) theDoor.updateDeviceSensor("${sensor.displayName} is closed")
						state.data[door].status = "closed"
					}
				} else {
					if (currentDoor == 'closing') {
						theDoor.updateDeviceStatus('closed')
						if (sameSame || !state.data[door].sensor) theDoor.updateDeviceContact('closed')
						if (sensor) theDoor.updateDeviceSensor("${sensor.displayName} is closed")
						state.data[door].status = "closed"
					} else if (currentDoor == 'opening') {
						theDoor.updateDeviceStatus('open')
						if (!state.data[door].sensor) theDoor.updateDeviceContact('open')
						if (sensor) theDoor.updateDeviceSensor("${sensor.displayName} is open")
						state.data[door].status = "open"
					}
				}
			}
            if (isDebug) log.debug "activityHandler() - evt.value: ${evt.value}, doorContact: ${theDoor.latestValue('contact')}, currentDoor: ${theDoor.latestValue('door')}, sensor: ${sensor}"
		}
	}	
}

def doorButtonOpenHandler(evt) {
    try{
        if (isDebug) log.debug "Door open button push detected: Event name  " + evt.name + " value: " + evt.value   + " deviceID: " + evt.deviceId + " DNI: " + evt.device.deviceNetworkId
        def myQDeviceId = evt.device.deviceNetworkId.replace(" Opener", "")
        def doorDevice = getChildDevice(state.data[myQDeviceId].child)
		logInfo "Opening door ${doorDevice.displayName} (button)"
        sendCommand(myQDeviceId, "open")
    } catch(e) {
    	log.error "Warning: MyQ Open button command failed - ${e}"
    }
}

def doorButtonCloseHandler(evt) {
	try{
		if (isDebug) log.debug "Door close button push detected: Event name  " + evt.name + " value: " + evt.value   + " deviceID: " + evt.deviceId + " DNI: " + evt.device.deviceNetworkId
        def myQDeviceId = evt.device.deviceNetworkId.replace(" Closer", "")
        doorDevice = getChildDevice(state.data[myQDeviceId].child)
        logInfo "Closing door ${doorDevice.displayName} (button)"
        sendCommand(myQDeviceId, "close")
	} catch(e) {
    	log.error "Warning: MyQ Close button command failed - ${e}"
    }
}

def getSelectedDevices( settingsName ) {
	def selectedDevices = []
	(!settings.get(settingsName))?:((settings.get(settingsName)?.getAt(0)?.size() > 1)  ? settings.get(settingsName)?.each { selectedDevices.add(it) } : selectedDevices.add(settings.get(settingsName)))
	return selectedDevices
}

/* Access Management */
private forceLogin() {
	//Reset token and expiry
    log.warn "forceLogin: Refreshing login token"
	state.session = [ brandID: 0, brandName: settings.brand, securityToken: null, expiration: 0 ]
	return doLogin()
}

private login() {
	if (now() > state.session.expiration){
    	log.warn "Token has expired. Logging in again."
        doLogin()
    }
    else{
    	return true;
    }
}

private doLogin() {
	def result = false
    apiPostLogin("/api/v5/Login", "{\"username\":\"${settings.username}\",\"password\": \"${settings.password}\"}" ) { response ->
        if (response.data.SecurityToken != null) {
            state.session.securityToken = response.data.SecurityToken
            state.session.expiration = now() + (5*60*1000) // 5 minutes default
            //Now get account ID
            return apiGet(getAccountIdURL(), [expand: "account"]) { acctResponse ->
                if (acctResponse.status == 200) {                    
                    state.session.accountId = acctResponse.data.Account.Id
                    if (isDebug) log.debug "got accountid ${acctResponse.data.Account.Id}"
                    result = true
                }
                else{
                	log.warn "Failed to get AccountId, login unsuccessful"
                    result =  false
                }
            }
            result = true
        } else {
            log.warn "No security token found, login unsuccessful"
            state.session = [ brandID: 0, brandName: settings.brand, securityToken: null, expiration: 0 ] // Reset token and expiration
            result = false
        }
    }
	return result
}

//Get devices listed on your MyQ account
private getMyQDevices() {
	state.MyQDataPending = [:]
    state.unsupportedList = []

	apiGet(getDevicesURL(), [:]) { response ->
		if (response.status == 200) {
			response.data.items.each { device ->
                // 2 = garage door, 5 = gate, 7 = MyQGarage(no gateway), 9 = commercial door, 17 = Garage Door Opener WGDO
				//if (device.MyQDeviceTypeId == 2||device.MyQDeviceTypeId == 5||device.MyQDeviceTypeId == 7||device.MyQDeviceTypeId == 17||device.MyQDeviceTypeId == 9) {
                if (device.device_family == "garagedoor") {
					if (isDebug) log.debug "Found door: ${device.name}"
                    def dni = device.serial_number
					def description = device.name
                    def doorState = device.state.door_state
                    def updatedTime = device.last_update
  
                    //def dni = device.MyQDeviceId
					//def description = ''
                    //def doorState = ''
                    //def updatedTime = ''
                    /*device.Attributes.each {
                        if (it.AttributeDisplayName=="desc")
                        {
                        	description = it.Value
                        }

						if (it.AttributeDisplayName=="doorstate") {
                        	doorState = it.Value
                            updatedTime = it.UpdatedTime
						}
					}

                    //Sometimes MyQ has duplicates. Check and see if we've seen this door before
                        def doorToRemove = ""
                        state.MyQDataPending.each { doorDNI, door ->
                        	if (door.name == description){
                            	log.debug "Duplicate door detected. Checking to see if this one is newer..."

                                //If this instance is newer than the duplicate, pull the older one back out of the array
                                if (door.lastAction < updatedTime){
                                	log.debug "Yep, this one is newer."
                                    doorToRemove = door
                                }

                                //If this door is the older one, clear out the description so it will be ignored
                                else{
                                	log.debug "Nope, this one is older. Stick with what we've got."
                                    description = ""
                                }
                            }
                        }
                        if (doorToRemove){
                        	log.debug "Removing older duplicate."
                            state.MyQDataPending.remove(door)
                        }*/

                    //Ignore any doors with blank descriptions
                    if (description != ''){
                        if (isDebug) log.debug "Got valid door: ${description} type: ${device.device_family} status: ${doorState} type: ${device.device_type}"
                        //log.debug "Storing door info: " + description + "type: " + device.device_family + " status: " + doorState +  " type: " + device.device_type
                        state.MyQDataPending[dni] = [status: doorState, lastAction: updatedTime, name: description, typeId: device.MyQDeviceTypeId, typeName: 'door', 
													 sensor: '', activity: '', threed: '', axis: '', verified: '', myQDeviceId: device.serial_number]
                    }
                    else{
                    	log.warn "Door " + device.MyQDeviceId + " has blank desc field. This is unusual..."
                    }
				}

                //Lights
                else if (device.device_family == "lamp") {
                    def dni = device.serial_number
					def description = device.name
                    def lightState = device.state.lamp_state
                    def updatedTime = device.state.last_update
                    
                    
                    /*
                    device.Attributes.each {

                        if (it.AttributeDisplayName=="desc")
                        {
                        	description = it.Value
                        }

						if (it.AttributeDisplayName=="lightstate") {
                        	lightState = it.Value
                            updatedTime = it.UpdatedTime
						}
					}*/

                    //Ignore any lights with blank descriptions
                    if (description && description != ''){                    	
                        if (isDebug) log.debug "Got valid light: ${description} type: ${device.device_family} status: ${lightState} type: ${device.device_type}"
                        state.MyQDataPending[dni] = [ status: lightState, lastAction: updatedTime, name: description, typeName: 'light', type: device.MyQDeviceTypeId, myQDeviceId: device.serial_number ]
                    }
				}

                //Unsupported devices
                else{                    
                    state.unsupportedList.add([name: device.name, typeId: device.device_family, typeName: device.device_type])
                }
			}
		}
	}
}

def getHubID(){
    return getIsST() ? '' : 1234
}

/* API Methods */
private getDevicesURL(){
	return "/api/v5.1/Accounts/${state.session.accountId}/Devices"
}

private getAccountIdURL(){
	return "/api/v5/My"
}

import groovy.transform.Field

@Field final MAX_RETRIES = 1 // Retry count before giving up

// get URL
private getApiURL() {
	return "https://api.myqdevice.com"	
}

private getApiAppID() {	
    return "JVM/G9Nwih5BwKgNCjLxiFUQxQijAebyyg8QUHr7JOrP+tuPb8iHfRHKwTmDzHOu"	
}

private getMyQHeaders() {
	return [        
        "SecurityToken": state.session.securityToken,
        "MyQApplicationId": getApiAppID(),
        "Content-Type": "application/json"
    ]
}


// HTTP GET call (Get Devices)
private apiGet(apiPath, apiQuery = [], callback = {}) {
    if (!login()){
        log.error "Unable to complete GET, login failed"
        return
    }
    try {        
        if (isDebug) log.debug "API Callout: GET ${getApiURL()}${apiPath} headers: ${getMyQHeaders()}"
		
        httpGet([ uri: getApiURL(), path: apiPath, headers: getMyQHeaders(), query: apiQuery ]) { response ->            
			def result = isGoodResponse(response)
			if (isDebug) log.debug "result: ${result}, response.status: ${response.status}${response.status!=204?', response.data:' + response.data:''}"
            if (result == 0) {
            	callback(response)
            }
            /*else if (result == 1){
            	apiGet(apiPath, apiQuery, callback) // Try again
            }*/
        }
    }	catch (e)	{
        log.error "API GET Error: $e"
    }
}

// HTTP PUT call (Send commands)
private apiPut(apiPath, apiBody = [], actionText = "") {
    if (!login()){
        log.error "Unable to complete PUT, login failed"
        return
    }
	def result
    try {
        if (isDebug) log.debug "Calling out PUT ${getApiURL()}${apiPath}${apiBody} ${getMyQHeaders()}, actionText: ${actionText}"
        httpPut([ uri: getApiURL(), path: apiPath, contentType: "application/json; charset=utf-8", headers: getMyQHeaders(), body: apiBody ]) { response ->
            result = isGoodResponse(response)
			if (isDebug) log.debug "result: ${result}, response.status: ${response.status}${response.status!=204?', response.data:' + response.data:''}"
            if (result == 0) {
            	return 0
            }
            else if (result == 1){
            	apiPut(apiPath, apiBody, actionText) // Try again
            }            
        }
    } catch (e)	{
        log.error "API PUT Error: $e"
    }
}

//Check response and retry login if needed
def isGoodResponse(response){
    if (isDebug) log.debug "Got response: STATUS: ${response.status}"
    
    //Good response
    if (response.status == 200 || response.status == 204) {
        state.retryCount = 0 // Reset it
        return 0
    }
    
    //Bad token response
    else if(response.status == 401){
    	if (state.retryCount <= MAX_RETRIES) {
            state.retryCount = (state.retryCount ?: 0) + 1
            log.warn "GET: Login expired, logging in again"
            if (forceLogin()){
                returnCode = 1
                log.warn "GET: Re-login successful."
            }
            else{
                returnCode = -1
                log.warn "GET: Re-login failed."
            }
        } else {
            log.warn "Too many retries, dropping request"
        }
    }
    
    //Unknown response
    else{
    	log.error "Unknown status: ${response.status} ${response.data}"
        return -1
    }
    return returnCode
}

// HTTP POST call (Login)
private apiPostLogin(apiPath, apiBody = [], callback = {}) {
    try {
		def result = false
        if (isDebug) log.debug "Logging into ${getApiURL()}/${apiPath} headers: ${getMyQHeaders()}"
        return httpPost([ uri: getApiURL(), path: apiPath, headers: getMyQHeaders(), body: apiBody ]) { response ->
            if (isDebug) log.debug "Got LOGIN POST response.status: ${response.status}, response.data: ${response.data}"
            if (response.status == 200) {            	
                result = callback(response)
            } else {
                log.error "Unknown LOGIN POST response.status: ${response.status}, response.data: ${response.data}"
				result = false
            }
            result
        }
    } catch (e)	{
        log.error "LOGIN POST Error: $e"
    }
    return result
}

// Send command to start or stop
def sendCommand(myQDeviceId, command) {
	if (!state.data[myQDeviceId].verified) find3DAxis(myQDeviceId, command)
	def theDoor = getChildDevice(state.data[myQDeviceId].child)
	if (command == "close") {
		if (state.data[myQDeviceId].activity) {
			// We have an Activity Sensor, so we will be waiting until we see the door start moving
			theDoor.updateDeviceStatus('waiting')
			// Note that we always use the Contact Sensor's contact status if we have one
			String sensorName = state.data[myQDeviceId].sensor ? settings[state.data[myQDeviceId].sensor].displayName : (state.data[myQDeviceId].threed ? settings[state.data[myQDeviceId].threed].displayName : "")
			if (sensorName) theDoor.updateDeviceSensor( sensorName + " is open (waiting)" )
		} else {
			// Otherwise, we are closing until the contact says we are closed
			theDoor.updateDeviceStatus('closing')
			theDoor.updateDeviceSensor("${settings[state.data[myQDeviceId].sensor].displayName} is open (closing)")
		}
	} else if (command == "open") {
		theDoor.updateDeviceStatus("opening")
		if ((theDoor.latestValue('contact') == 'closed')) { // && (theDoor.latestValue('door') == 'opening')) {
			// Slightly odd, but we are closed (opening) until the contact sensor says we are open
			String sensorName = state.data[myQDeviceId].sensor ? settings[state.data[myQDeviceId].sensor].displayName : (state.data[myQDeviceId].threed ? settings[state.data[myQDeviceId].threed].displayName : "")
			if (sensorName) theDoor.updateDeviceSensor( sensorName + " is closed (opening)" )
		}
	}
	state.lastCommandSent = now()
  	apiPut("/api/v5.1/Accounts/${state.session.accountId}/Devices/${myQDeviceId}/actions", "{\"action_type\":\"${command}\"}", "${state.data[myQDeviceId].name}(${command})")
    //log.debug "MyQ Command for ${theDoor.displayName} --> ${command.toUpperCase()}"
    return true
}

// Identify which 3D Axis is open/close
def find3DAxis(door, command) {
	String axis = state.data[door].axis
	String other = state.data[door].other
	def verified = state.data[door].verified
    String doorName = state.data[door].child ? getChildDevice(state.data[door].child) : state.data[door].name
	if ((command == "open") && state.data[door].threed && (!axis || (axis && !verified))) {
		def currentThreeD = [:]
		try {
			currentThreeD = settings[state.data[door].threed].latestValue('threeAxis')
			if (axis && (axis != "none")) {
				// We have an axis that needs verifying - we're closed, so verify the sensor agrees
				if (currentThreeD."${axis}".abs() < 100) {
					// Oops - that axis isn't closed
					if (other && (currentThreeD."${other}".abs() > 900)) {
						// good - the other one IS closed
						axis = other
						other = ""
					} else {
						log.error "Can't get valid 3D axis from Activity Sensor ${settings[state.data[door].threed].displayName}"
						axis = "none"
						other = ""
					}
				}
			} else {
				//no axis yet - find the one(s) that are ~1000(abs)
				other = ""
				axis = ""
				currentThreeD.each { key, val ->
					if (val.abs() > 900) {
						if (!axis) { 
							axis = key
						} else {
							// there may be 2 axies with large values - save the other in case we pick the wrong one the first time
							other = key
			}	}	}	}
			// set it and forget it
			if (isDebug) log.debug "${doorName} 3D Sensor ${settings[state.data[door].threed].displayName} is currently using axis '${axis}'"
			state.data[door].axis = axis
			state.data[door].other = other
			state.data[door].verified = other ? false : true	// if only one axis had a large value, then we don't need to worry about the other
			return
		} catch (e) {
			log.warn "Error getting 3D coordinates for ${doorName} (will retry): ${e}"
			state.data[door].axis = ""
		}
	} else if ((command == "close") && state.data[door].threed && (!axis || (axis && !verified))) {
		def currentThreeD = [:]
		try {
			currentThreeD = settings[state.data[door].threed].latestValue('threeAxis')
			if (axis && (axis != "none")) {
				// We have an axis that needs verifying - we're open, so verify the sensor agrees
				if (currentThreeD."${axis}".abs() > 900) {
					// Oops - that axis isn't open
					if (other && (currentThreeD."${other}".abs() < 100)) {
						// good - the other one IS open
						axis = other
						other = ""
					} else {
						log.error "Can't get valid 3D axis from 3D Sensor ${settings[state.data[door].threed].displayName}"
						axis = "none"
						other = ""
					}
				}
			} else {
				//no axis yet - find the one(s) that are ~0(abs)
				other = ""
				axis = ""
				currentThreeD.each { key, val ->
					if (val.abs() < 100) {
						if (!axis) {
							axis = key
						} else {
							// there may be 2 axies with small values - save the other in case we pick the wrong one the first time
							other = key
			}	}	}	}
			if (isDebug) log.debug "${doorName} Activity 3D ${settings[state.data[door].threed].displayName} is currently using axis '${axis}'"
			state.data[door].axis = axis
			state.data[door].other = other
			state.data[door].verified = other ? false : true	// if only one axis had a small value, then we don't need to worry about the other
			return
		} catch (e) {
			log.warn "Error validating 3D axis for ${doorName} (will retry): ${e}"
			state.data[door].axis = ""
		}
	}
	
	if (!state.data[door].threed && ((command == "open") || (command == "close"))){
		// IE, we don't have a motion sensor
		state.data[door].verified = true
		state.data[door].axis = "none"
	}
}

//Remove old unused pieces of state
def stateCleanup(){
    if (state.latestDoorNoSensorVersion){state.remove('latestDoorNoSensorVersion')}
    if (state.latestDoorVersion){state.remove('latestDoorVersion')}
    if (state.latestLightVersion){state.remove('latestLightVersion')}
    if (state.latestSmartAppVersion){state.remove('latestSmartAppVersion')}
    if (state.thisDoorNoSensorVersion){state.remove('thisDoorNoSensorVersion')}
    if (state.thisDoorVersion){state.remove('thisDoorVersion')}
    if (state.thisLightVersion){state.remove('thisLightVersion')}
    if (state.thisSmartAppVersion){state.remove('thisSmartAppVersion')}
    if (state.versionWarning){state.remove('versionWarning')}
    if (state.polling){state.remove('polling')}
}

//Available to be called from child devices for special logging
def notify(message){
	if (isDebug) log.debug "From child: ${message}"
}

// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
String	getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST()	  { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
boolean getIsHE()	  { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...

String getHubPlatform() {
	def pf = getPlatform()
	atomicState?.hubPlatform = pf			// if (atomicState.hubPlatform == 'Hubitat') ... 
											// or if (state.hubPlatform == 'SmartThings')...
	atomicState?.isST = pf.startsWith('S')	// if (atomicState.isST) ...
	atomicState?.isHE = pf.startsWith('H')	// if (atomicState.isHE) ...
	return pf
}
boolean getIsSTHub() { return atomicState.isST }					// if (isSTHub) ...
boolean getIsHEHub() { return atomicState.isHE }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"	
}
