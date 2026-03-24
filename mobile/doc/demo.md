mobile：
begin to device login {"imsi":"12","imei":"lzl","manufacturer":"default","model":"default","appType":"5","extendModel":"default","country":"default","platform":"1","width":"240","height":"320","mcc":"460","mnc":"00x","lac":"100","ci":"5.21","rxlev":"-72","totalKb":"1424122","freeKb":"1424122","clientLanguage":"en_US","deviceType":"1"}

1. gids : /app-api/devicetcp/app/login/v1/gridLoginAuth
reqbody:
{
  "imsi" : "10",
  "imei" : "lzl",
  "manufacturer" : "default",
  "model" : "default",
  "appType" : "5",
  "extendModel" : "default",
  "country" : "default",
  "platform" : "1",
  "width" : "240",
  "height" : "320",
  "mcc" : "460",
  "mnc" : "00x",
  "lac" : "100",
  "ci" : "5.21",
  "rxlev" : "-72",
  "totalKb" : "1424122",
  "freeKb" : "1424122",
  "clientLanguage" : "en_US",
  "deviceType" : "1"
}
resp:
{
  "code" : 200,
  "msg" : "success",
  "data" : {
    "token" : "8352b6da-0313-439f-9946-0a06e2fd7144",
    "expiresTime" : 1774321223548944,
    "timeAxis" : 1774317623,
    "nodeGateWayUrl" : "135.242.67.210:9090",
    "nodeIntranetWayUrl" : "135.242.67.210:9090"
  }
}

2. /app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser
reqbody
{
  "imsi" : "10",
  "imei" : "lzl",
  "manufacturer" : "default",
  "model" : "default",
  "appType" : "5",
  "extendModel" : "default",
  "country" : "default",
  "platform" : "1",
  "width" : "240",
  "height" : "320",
  "mcc" : "460",
  "mnc" : "00x",
  "lac" : "100",
  "ci" : "5.21",
  "rxlev" : "-72",
  "totalKb" : "1424122",
  "freeKb" : "1424122",
  "clientLanguage" : "en_US",
  "deviceType" : "1"
}
resp:
{
  "code" : 200,
  "msg" : "success",
  "data" : {
    "token" : "8352b6da-0313-439f-9946-0a06e2fd7144",
    "expiresTime" : 1774321223548944,
    "timeAxis" : 1774317623,
    "nodeGateWayUrl" : "135.242.67.210:9090",
    "nodeIntranetWayUrl" : "135.242.67.210:9090"
  }
}

3. /app-api/devicetcp/app/login/v1/deviceLoginAuth
reqbody
{
  "imsi" : "10",
  "imei" : "lzl",
  "manufacturer" : "default",
  "model" : "default",
  "appType" : "5",
  "extendModel" : "default",
  "country" : "default",
  "platform" : "1",
  "width" : "240",
  "height" : "320",
  "mcc" : "460",
  "mnc" : "00x",
  "lac" : "100",
  "ci" : "5.21",
  "rxlev" : "-72",
  "totalKb" : "1424122",
  "freeKb" : "1424122",
  "clientLanguage" : "en_US",
  "deviceType" : "1"
}
respbody
{
  "code" : 200,
  "msg" : "success",
  "data" : {
    "token" : "8352b6da-0313-439f-9946-0a06e2fd7144",
    "expiresTime" : 1774321223548944,
    "timeAxis" : 1774317623,
    "tcpAddr" : "127.0.0.1:30001",
    "videoMode" : 1,
    "shortAddr" : "135.242.67.210:9090",
    "nodeGateWayUrl" : "135.242.67.210:9090",
    "nodeIntranetWayUrl" : "135.242.67.210:9090"
  }
}

browser-gateway的一些参数，涉及路径不需要考虑，和本项目保持一致
remoteImpl.java/createChrome
InitBrowserRequest(factory=default, devType=default, extType=default, platType=1, lcdWidth=240, lcdHeight=320, appType=5, appID=5, innerMediaEndpoint=127.0.0.1:30002, imsi=10, imei=lzl, deviceType=1, clientLanguage=en_US, playMode=1)
MuenDriverCtx(imei1=null, imei2=null, imeiAndImsi=null, machineType=null, frameRate=null, bitRite=null, sampleRate=null, channels=null, chromeWidth=null, chromeHeight=null, appType=null, instanceConfig=null, clientLoginPacket=null, controlTcpConnectedTime=-1, hwCallback=com.moon.cloud.browser.sdk.core.HWCallbackWrapper@4dfd3684, hwContext=null, isControlJsInitialized=false, ability=null, chromeDriver=null, devTools=null, initUserAgent=null, lastCtrlType=0, widthScaleRatio=null, heightScaleRatio=null, webParams={}, touchStrategy=com.moon.cloud.browser.sdk.strategy.MouseWheelTouchStrategy@e65420c, extras=null)
HWCallbackImpl(redis=com.huawei.browsergateway.service.impl.RedisImpl@4a6ef712, controlClientSet=com.huawei.browsergateway.tcpserver.control.ControlClientSet@61288814, muenSessionManager=com.huawei.browsergateway.websocket.extension.MuenSessionManager@128a188a, userId=lzl_10, websocketAddr=127.0.0.1:30002, fileStorage=com.huawei.browsergateway.service.impl.S3FileStorageServiceImpl@4571cebe, localTmp=D:\Code\muen_10\tmp)
ChromeRecordConfig(codecMode=webcodecs, dataDealAddr=127.0.0.1:30002, imeiAndImsi=lzl_10, appType=5, width=240, height=320, bitRate=1000, frameRate=10, sampleRate=48000, channelCount=1, echoCancellation=true, noiseSuppression=true, controlExtensionId=jjndjgheafjngoipoacpjgeicjeomjli, controlExtensionPath=D:\Code\muen_10\extension/keys, limit=40)
ChromeParams(frameRate=10, bitRite=1000, sampleRate=48000, channels=1, chromeWidth=240, chromeHeight=320, controlExtentionId=jjndjgheafjngoipoacpjgeicjeomjli, controlExtentionPath=D:\Code\muen_10\extension/keys)

browser-proxy
/api/browsers
request_body:{'executable_path': 'C:/Users/h00613474/AppData/Local/Google/Chrome/Application/chrome.exe', 'base_data': 'D:\\Code\\muen_10\\basedata\\7d6b7292d5f4496586fa4b4ac157f1fd', 'extension_paths': ['D:\\Code\\muen_10\\extension\\record', 'D:\\Code\\muen_10\\extension/keys'], 'extension_ids': ['majikpeglnhefidjkmpeipdbikkfbmho', 'jjndjgheafjngoipoacpjgeicjeomjli'], 'allowlisted_extension_id': 'majikpeglnhefidjkmpeipdbikkfbmho', 'browser_type': 'KEYS', 'headless': False, 'language': 'en_US'}