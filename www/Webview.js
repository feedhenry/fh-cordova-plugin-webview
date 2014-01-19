function Webview(){
  this.success = null;
  this.fail = null;
  this.opened = false;
}

Webview.prototype.load = function(options, success, fail){
  cordova.exec(null, null, "Webview", "load", [options]);
  this.opened = true;
  this.success = success;
  this.fail = fail;
}

Webview.prototype.close = function(options, success, fail){
  if(this.opened){
    cordova.exec(null, null, "Webview", "close", [options]);
    this.opened = false;
  }
}

Webview.prototype.open_success = function(){
  if(this.success){
    this.success('opened');
  }
}

Webview.prototype.close_success = function(){
  this.opened = false;
  if(this.success){
    this.success('closed');
  }
}

Webview.prototype.fail = function(error){
  if(this.fail){
    this.fail(error);
  }
}

cordova.addConstructor(function() {
                        
    if(!window.plugins)        {
        window.plugins = {};
    }
        
    window.plugins.webview = new Webview();
});