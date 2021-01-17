# WallPanel
WallPanel is an Android application for Web Based Dashboards and Home Automation Platforms. You can either side load the application to your Android device from the [release section](https://github.com/thanksmister/wallpanel-android/releases), install the application from  [Google Play](https://play.google.com/store/apps/details?id=com.thanksmister.iot.wallpanel) or get WallPanel from the [Amazon Appstore](https://www.amazon.com/dp/B08S8XZ7LN/). 

If you use WallPanel, please use my [community page](https://community.thanksmister.com/) to post some screenshots of your setup, I would love to see them.  Also, if you want to sponser this project or any of my other open source projects, please become a Sponser using the button at the tope of this page, thanks!

## Screenshots
![wall1](https://user-images.githubusercontent.com/142340/85577620-c9862d80-b60f-11ea-9c8e-8d0ab878c96e.png)
![wall2](https://user-images.githubusercontent.com/142340/85577633-cbe88780-b60f-11ea-98c5-e50f4f124def.png)
![wall3](https://user-images.githubusercontent.com/142340/85577640-cd19b480-b60f-11ea-8c04-14fdde2ba053.png)

## Support
For issues, feature requests, use the [Github issues tracker](https://github.com/thanksmister/wallpanel-android/issues). Join the [ThanksMister Discord](https://community.thanksmister.com/) to ask questions or share any helpful information about this project. 

## Features
- Web Based Dashboards and Home Automation Platforms support.
- Camera support for streaming video, motion detection, face detection, and QR Code reading.
- Google Text-to-Speech support to speak notification messages using MQTT or HTTP.
- MQTT or HTTP commands to remotely control device and application (url, brightness, wake, etc.).
- Sensor data reporting for the device (temperature, light, pressure, battery).
- Streaming MJPEG server support using the device camera.
- Screensaver feature that can be dismissed with motion or face detection.
- Support for Android 4.4 (API level 19) and greater devices.

## Hardware & Software 

- Android Device running Android OS 4.4 or greater.  Note: The WebView shipped with Android 4.4 (KitKat) is based on the same code as Chrome for Android version 30. This WebView does not have full feature parity with Chrome for Android and is given the version number 30.0.0.0.  

*** If you have need support for older Android 4.0 devices (those below Android 4.4), you want to use the [legacy](https://github.com/thanksmister/wallpanel-android-legacy) version of the application. Alternatively you can download an APK from the release section prior to release v0.8.8-beta.6 *** 

## Quick Start
You can either side load the application to your device from the [release section](https://github.com/thanksmister/wallpanel-android/releases)  or install the application from [Google Play](https://play.google.com/store/apps/details?id=com.thanksmister.iot.wallpanel). The application will open to the welcome page with a link to update the settings. Go to settings, and setup the link to your web page or home automation platform. You may also update additional settings for Motion, Face Detection, and for publishing device sensor data. 

## Building the Application 
To build the application locally, checkout the code from Github and load the project into Android Studio with Android API 27 or higher.  You will need to remove the Firebase dependency in the build.gradle file, this is not required.  Remove the following dependencies:

```
implementation 'com.google.firebase:firebase-core:17.2.0'
implementation 'com.google.firebase:firebase-analytics:17.2.0'
```
Remove this if you are building the application for devices that do not support Google Services.

```
apply plugin: 'com.google.gms.google-services'
```

The project should compile normally.

## Limitations
Android devices use WebView to render webpages, This WebView does not have full feature parity with Chrome for Android and therefore pages that render in Chrome may not render nicely in Wall Panel. For example, WebView that shipped with Android 4.4 (KitKat) devices is based on the same code as Chrome for Android version 30. This WebView does not have full feature parity with Chrome for Android and is given the version number 30.0.0.0.  If you find that you cannot render a webpage, it is most likely that the version of WebView on your device does not support the CSS/HTML of that page.  You have little recourse but to update the webpage, as there is nothing to be done to the WebView to make it compatible with your code. 

## MQTT Sensor and State Data
If MQTT is enabled in the settings and properly configured, the application can publish data and states for various device sensors, camera detections, and application states.

### Device Sensors
The application will post device sensors data per the API description and Sensor Reading Frequency. Currently device sensors for Pressure, Temperature, Light, and Battery Level are published. 

#### Sensor Data
Sensor | Keys | Example | Notes
-|-|-|-
battery | unit, value, charging, acPlugged, usbPlugged | ```{"unit":"%", "value":"39", "acPlugged":false, "usbPlugged":true, "charging":true}``` |
light | unit, value | ```{"unit":"lx", "value":"920"}``` |
magneticField | unit, value | ```{"unit":"uT", "value":"-1780.699951171875"}``` |
pressure | unit, value | ```{"unit":"hPa", "value":"1011.584716796875"}``` |
temperature | unit, value | ```{"unit":"°C", "value":"24"}``` |

*NOTE:* Sensor values are device specific. Not all devices will publish all sensor values.

* Sensor values are constructed as JSON per the above table
* For MQTT
  * WallPanel publishes all sensors to MQTT under ```[baseTopic]sensor```
  * Each sensor publishes to a subtopic based on the type of sensor
    * Example: basetopic: ```wallpanel/mywallpanel/``` battery sensor data is published to: ```wallpanel/mywallpanel/sensor/battery```
    
#### Home Assistant Examples
```YAML
sensor:
  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/battery"
    name: "WallPanel Battery Level"
    unit_of_measurement: "%"
    value_template: '{{ value_json.value }}'
    
 - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/temperature"
    name: "WallPanel Temperature"
    unit_of_measurement: "°C"
    value_template: '{{ value_json.value }}'

  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/light"
    name: "WallPanel Light Level"
    unit_of_measurement: "lx"
    value_template: '{{ value_json.value }}'
    
  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/magneticField"
    name: "WallPanel Magnetic Field"
    unit_of_measurement: "uT"
    value_template: '{{ value_json.value }}'

  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/pressure"
    name: "WallPanel Pressure"
    unit_of_measurement: "hPa"
    value_template: '{{ value_json.value }}'
```

### Camera Motion, Face, and QR Codes Detections
In additional to device sensor data publishing, the application can also publish states for Motion detection and Face detection, as well as the data from QR Codes derived from the device camera.  

Detection | Keys | Example | Notes
-|-|-|-
motion | value | ```{"value": false}``` | Published immediately when motion detected
face | value | ```{"value": false}``` | Published immediately when face detected
qrcode | value | ```{"value": data}``` | Published immediately when QR Code scanned

* For MQTT
  * WallPanel publishes all sensors to MQTT under ```[baseTopic]/sensor```
  * Each sensor publishes to a subtopic based on the type of sensor
    * Example: ```wallpanel/mywallpanel/sensor/motion```

#### Home Assistant Examples

```YAML
binary_sensor:
  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/motion"
    name: "Motion"
    payload_on: '{"value":true}'
    payload_off: '{"value":false}'
    device_class: motion 
    
binary_sensor:
  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/face"
    name: "Face Detected"
    payload_on: '{"value":true}'
    payload_off: '{"value":false}'
    device_class: motion 
  
sensor:
  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/qrcode"
    name: "QR Code"
    value_template: '{{ value_json.value }}'
    
```

### Application State Data
The application can also publish state data about the application such as the current dashboard url loaded or the screen state.

Key | Value | Example | Description
-|-|-|-
currentUrl | URL String | ```{"currentUrl":"http://hasbian:8123/states"}``` | Current URL the Dashboard is displaying
screenOn | true/false | ```{"screenOn":true}``` | If the screen is currently on.
brightness | true/false | ```{"brightness":100}``` | Current brightness value of the screen.

* State values are presented together as a JSON block
  * eg, ```{"currentUrl":"http://hasbian:8123/states","screenOn":true}```
* For REST
  * GET the JSON from URL ```http://[mywallpanel]:2971/api/state```
* For MQTT
  * WallPanel publishes state to topic ```[baseTopic]state```
    * Default Topic: ```wallpanel/mywallpanel/state```

## MJPEG Video Streaming

Use the device camera as a live MJPEG stream. Just connect to the stream using the device IP address and end point. Be sure to turn on the camera streaming options found under ```HTTP Settings``` and set the number of allowed streams and HTTP port number. Note that performance will depend upon your device (i.e. older devices will be slow).

#### Browser Example:

```
http://<the.device.ip.address>:2971/camera/stream
// where <the.device.ip.address> represents the address that the device has been assigned on your network, 
// for example something like 192.168.1.1
```
You will find the address under ```HTTP Settings > MJPEGStreaming```

#### Home Assistant Example:

```YAML
camera:
  - platform: mjpeg
    mjpeg_url: http://<the.device.ip.address>:2971/camera/stream
    name: WallPanel Camera
```

## MQTT and HTTP Commands
Interact and control the application and device remotely using either MQTT or HTTP (REST) commands, including using your device as an announcer with Google Text-To-Speach. 

### Commands
Key | Value | Example Payload | Description
-|-|-|-
clearCache | true | ```{"clearCache": true}``` | Clears the browser cache
eval | JavaScript | ```{"eval": "alert('Hello World!');"}``` | Evaluates Javascript in the dashboard
audio | URL | ```{"audio": "http://<url>"}``` | Play the audio specified by the URL immediately
relaunch | true | ```{"relaunch": true}``` | Relaunches the dashboard from configured launchUrl
reload | true | ```{"reload": true}``` | Reloads the current page immediately 
url | URL | ```{"url": "http://<url>"}``` | Browse to a new URL immediately
wake | true | ```{"wake": true, "wakeTime": 180}``` | Wakes the screen if it is asleep. Option wakeTime (in seconds) is optional, default is 30 sec. (Note: wakeTime cannot be shorter than Androids Display Timeout setting)
wake | false | ```{"wake": false}``` | Release screen wake (Note: screen will not turn off before Androids Display Timeout finished)
speak | data | ```{"speak": "Hello!"}``` | Uses the devices TTS to speak the message
brightness | data | ```{"brightness": 1}``` | Changes the screens brightness, value 1-255. 
camera | data | ```{"camera": true}``` | Turns on/off camera streaming, requires camera to be enabled.
volume | data | ```{"volume": 100}``` | Changes the audio volume, value 0-100 (in %. Does not effect TTS volume).

* The base topic value (default is "mywallpanel") should be unique to each device running the application unless you want all devices to receive the same command. The base topic and can be changed in the applications ```MQTT settings```.
* Commands are constructed via valid JSON. It is possible to string multiple commands together:
  * eg, ```{"clearCache":true, "relaunch":true}```
* For REST
  * POST the JSON to URL ```http://<the.device.ip.address>:2971/api/command```
* For MQTT
  * WallPanel subscribes to topic ```wallpanel/[baseTopic]/command```
    * Default Topic: ```wallpanel/mywallpanel/command```
  * Publish a JSON payload to this topic (be mindfula of quotes in JSON should be single quotes not double)


### Google Text-To-Speach Command
You can send a command using either HTTP or MQTT to have the device speak a message using Google's Text-To-Speach. Note that the device must be running Android Lollipop or above. 

Example format for the message topic and payload: 

```{"topic":"wallpanel/mywallpanel/command", "payload":"{'speak':'Hello!'}"}```

### Screensaver and Brigthness control
On some older devices, there is not screensaver such as Daydream that automatically dims the screen.  Therefore the application provides a screensaver feature (currently a clock animation).  This feature along with the screen brightness option, allows the screen to dim when the screensaver is active.  With the Camera and Motion feature, the device can be automatically awaken when motion is detected.  Optionally, you can send an MQTT command to wake the screen or just touch the screen to deactive the screensaver. 

There is setting to dim screen a set percentage when the screensaver is active, this requires the screen brightness setting be enabled. When set to a value above 0%, the screen will dim by the percent value set when the screensaver is active. So if the setting is 75%, the screen will dim to a vlaue that is 75% of the default device brightness levels.

Using the screen brightness option requires some extra permissions.  This is because controlling the devices screen brightness is considered a dangerous permission by Google and so users have to manually turn this on.  When you first select the screen brightness option, you will be taken to the setting on your device to enable the permission.  The screen brightness feature behaves in the following manner: 

- There is a general brightness setting that must be enabled (and permissions given) in order for the application to manually set the device brightness.  If at any time you revoke the permissions in the device settings for the application to control the brightness, this option will be disabled. 

- The brightness mode can be disabled in the settings, returning the device back to its automatic brightness control. If brightness is disabled the application will no longer be able to change the devices brightness level including via MQTT commands. 

- Brightness level is read at the time brightness control is enabled and permissions granted. However, there is also a new capture button in the settings to manually capture the devices current brightness level. To use this, first go into the app settings, then adjust your devices brightness level, then press the capture button to save the new brightness level.  The application will then use this brightness level to set the device brighntess level.   

- If you have brightness enabled, you can at any time manually set a new brightness level using the MQTT commands (see commands section above).  The device will then use the new stting as the default brightness level of the deivice. 


## Credits

WallPanel (Formerly HomeDash) is a fork from the [original WallPanel project](https://github.com/WallPanel-Project/wallpanel-android) developed by [quadportnick](https://github.com/quadportnick). Thanks to [allofmex](https://github.com/allofmex) for his contributions to the project. 

