# WallPanel
WallPanel is an Android application for Web Based Dashboards and Home Automation Platforms.

## Quick Start
You can either side load the application to your device from the release section or install the application from the Google Play store. The application will open to the welcome page with a link to update the settings. Go to settings, and setup the link to your web page or home automation platform. You may also update additional settings for Motion, Face Detection, and for publishing device sensor data. 

## MQTT Sensor and State Data
If MQTT is enabled in the settings and properly configured, the application can publish data and states for various device sensors, camera detections, and application states.

### Device Sensors
The application will post device sensors data per the API description and Sensor Reading Frequency. Curerntly device sensors for Pressure, Temperature, Light, and Battery Level are published. 

#### Sensor Data
Sensor | Keys | Example | Notes
-|-|-|-
battery | unit, value, charging, acPlugged, usbPlugged | ```{"unit":"%", "value":"39", "acPlugged":false, "usbPlugged":true, "charging":true}``` |
light | unit, value | ```{"unit":"lx", "value":"920"}``` |
magnetic_field | unit, value | ```{"unit":"uT", "value":"-1780.699951171875"}``` |
pressure | unit, value | ```{"unit":"hPa", "value":"1011.584716796875"}``` |
temperature | unit, value | ```{"unit":"°C", "value":"24"}``` |

*NOTE:* Sensor values are device specific. Not all devices will publish all sensor values.

* Sensor values are constructued as JSON per the above table
* For MQTT
  * WallPanel publishes all sensors to MQTT under ```[baseTopic]/sensor```
  * Each sensor publishes to a subtopic based on the type of sensor
    * Example: ```wallpanel/mywallpanel/sensor/battery```
    
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
    state_topic: "wallpanel/mywallpanel/sensor/magnetic_field"
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
In additional to device sensor data publishing. The application can also publish states for Motion detection and Face detection, as well as the data from QR Codes derived from the device camera.  

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
The application canl also publish state data about the application such as the current dashboard url loaded or the screen state.

Key | Value | Example | Description
-|-|-|-
currentUrl | URL String | ```{"currentUrl":"http://hasbian:8123/states"}``` | Current URL the Dashboard is displaying
screenOn | true/false | ```{"screenOn":true}``` | If the screen is currently on

* State values are presented together as a JSON block
  * eg, ```{"currentUrl":"http://hasbian:8123/states","screenOn":true}```
* For REST
  * GET the JSON from URL ```http://[mywallpanel]:2971/api/state```
* For MQTT
  * WallPanel publishes state to topic ```[baseTopic]/state```
    * Default Topic: ```wallpanel/mywallpanel/state```

## MJPEG Video Streaming

Use the device camera as a live MJPEG stream. Just connect to the stream using the device IP address and end point. Be sure to turn on the camera streaming options in the settings and set the number of allowed streams and HTTP port number. Note that performance depends upon your device (older devices will be slow).

#### Browser Example:

```http://192.168.1.1:2971/camera/stream```

#### Home Assistant Example:

```YAML
camera:
  - platform: mjpeg
    mjpeg_url: http://192.168.1.1:2971/camera/stream
```

## MQTT and HTTP Remote Control
You can control the app remotely via MQTT or HTTP (REST). 

### Commands
Key | Value | Example Payload | Description
-|-|-|-
clearCache | true | ```{"clearCache": true}``` | Clears the browser cache
eval | JavaScript | ```{"eval": "alert('Hello World!');"}``` | Evaluates Javascript in the dashboard
audio | URL | ```{"audio": "http://<url>"}``` | Play the audio specified by the URL immediately
relaunch | true | ```{"relaunch": true}``` | Relaunches the dashboard from configured launchUrl
reload | true | ```{"reload": true}``` | Reloads the current page immediately 
url | URL | ```{"url": "http://<url>"}``` | Browse to a new URL immediately
wake | true | ```{"wake": true}``` | Wakes the screen if it is asleep
speak | data | ```{"speak": "Hello!"}``` | Uses the devices TTS to speak the message

* The base topic value (default is "mywallpanel") should be unique to each device running the application unless you want all devices to receive the same command. The base topic and can be changed in the application settingssettings.
* Commands are constructed via valid JSON. It is possible to string multiple commands together:
  * eg, ```{"clearCache":true, "relaunch":true}```
* For REST
  * POST the JSON to URL ```http://[mywallpanel]:2971/api/command```
* For MQTT
  * WallPanel subscribes to topic ```wallpanel/[baseTopic]/command```
    * Default Topic: ```wallpanel/mywallpanel/command```
  * Publish a JSON payload to this topic (be mindfula of quotes in JSON should be single quotes not double)


## Google Text-To-Speach Command
You can send a command using either HTTP or MQTT to have the device speak a message using Google's Text-To-Speach. Note that the device must be running Android Lollipop or above. 

Example format for the message topic and payload: 

```{"topic":"wallpanel/mywallpanel/command", "payload":"{'speak':'Hello!'}"}```

<!-- ## Default Appication Configuration
Key | Value | Behavior | Default
-|-|-|-
app.deviceId | String | The unique identifier for this WallPanel device | mywallpanel
app.preventSleep | true/false | Prevents the screen from turning off | false
app.launchUrl | URL | The URL the Dashboard launches at | Tutorial Webpage 
app.showActivity | true/false | On-screen indication of browser activity | true
camera.cameraId | int | The camera ID to attach to | 0
camera.motionEnabled | true/false | If the device camera is used for motion detection | false
camera.motionCheckInterval | int | The interval the camera is polled for motion in milliseconds | 500
camera.motionLeniency | int | The leniency on changes in pictures between polls | 20
camera.motionMinLuma | int | The minimum light needed to perform motion detection | 1000
camera.motionWake | true/false | If motion activity should wake the device | true
PLANNED: camera.webcamEnabled | true/false | If the device camera is used as a webcam | false
http.enabled | true/false | Switch for REST(HTTP) being enabled | false
http.port | int | The port to listen on for REST(HTTP) | 2971
mqtt.enabled | true/false | Switch for MQTT being enabled | false 
mqtt.serverName | String | The hostname/IP of the MQTT server | mqtt 
mqtt.serverPort | Int | The port number for TCP MQTT | 1883 
mqtt.baseTopic | String | The root topic WallPanel will pub/sub under | wallpanel/{app.deviceId}/ 
mqtt.clientId | String | The client ID to connect to MQTT with | {app.deviceId}  
mqtt.username | String | The username to connect to MQTT with (or blank) |  
mqtt.password | String | The password to connect to MQTT with (or blank) | 
mqtt.sensorFrequency | Int | The frequency to post sensor data in seconds, or 0 to never post | 0 -->


## Credits

WallPanel (Formerly HomeDash) is a fork from the [original WallPanel project](https://github.com/WallPanel-Project/wallpanel-android) developed by [quadportnick](https://github.com/quadportnick).
