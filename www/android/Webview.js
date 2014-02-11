var exec = require("cordova/exec");

function WebView()
{
  this.opened = false;
}

WebView.prototype.action=function(params, successCallback, errorCallback) {
  var operation= function (params) {
    // params['title'] ? ViewCtrl.display(params.url, params.title) : viewCtrl.display(params.url, undefined);
    var x = params.x ? params.x:0;
    var y = params.y ? params.y:0;
    var width = params.width ? params.width: 0;
    var height = params.height ? params.height : 0;
    var showTitleBar = typeof params.showTitleBar == "undefined" ? true : params.showTitleBar;
    var titleBarColor = params.titleBarColor ? params.titleBarColor : "default";
    var showControls = typeof params.showControls == "undefined" ? true : params.showControls;
    var setupBridge = typeof params.setupBridge == "undefined" ? false : params.setupBridge;
    var title = params.title ? params.title : "";
    exec(null, null, "ViewCtrl", "display", [params.url,title, x, y, width, height, showTitleBar, titleBarColor, showControls, setupBridge]);
  };

  if( !('act' in params) || params.act === 'open') {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    params['url']? operation(params): errorCallback("Data_Act: No Action defined") ;
  } else {
    if(params.act === 'close' && this.opened){
      this.opened = false;
      exec(null, null, "ViewCtrl", "close", []);
    }
  }
};

WebView.prototype.success = function(result) {
  if(result === 'closed'){
    this.opened = false;
  } else {
    this.opened = true;
  }
  this.successCallback(result);
};

WebView.prototype.failure = function(result) {
  this.errorCallback(result);
};

module.exports = new WebView();