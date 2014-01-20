//
//  WebviewController.m
//  Feedhenry
//
//  Created by FeedHenry
//

#import "WebviewController.h"
#import "UIColor+i7HexColor.h"
#import <Cordova/CDVCommandQueue.h>
#import <Cordova/CDVViewController.h>
#import <Availability.h>
#import <Cordova/CDVAvailability.h>

@implementation WebviewContainerProperties
@synthesize x,y,width,height,fullScreen,titleBarColor,showControls, title, showTitleBar, setupBridge;


@end

@interface WebviewController ()

@property (nonatomic, readwrite, strong) NSDictionary* settings;
@property (nonatomic, readwrite, strong) CDVWhitelist* whitelist;
@property (nonatomic, readwrite, strong) NSMutableDictionary* pluginObjects;
@property (nonatomic, readwrite, strong) NSDictionary* pluginsMap;
@property (readwrite, assign) BOOL initialized;


@end


@implementation WebviewController
@synthesize topView, titleBar, cleanWebView, activityView, props, webview, webView, wwwFolderName;

- (WebviewController *) initWithSettings:(WebviewContainerProperties *) settings webview:(CDVPlugin *)theView {
    self = [super init];
    if(self && !self.initialized){
      self.wantsFullScreenLayout = NO;
      self.wwwFolderName = @"www";
      self.props = settings;
      self.webview = theView;
      _commandQueue = [[CDVCommandQueue alloc] initWithViewController: self];
      _commandDelegate = [[CDVCommandDelegateImpl alloc] initWithViewController: self];
      // load Cordova.plist settings
      [self loadSettings];
      // set the whitelist
      self.whitelist = [[CDVWhitelist alloc] initWithArray:[self.settings objectForKey:@"ExternalHosts"]];
      NSLog(@"props set");
      self.initialized = YES;
    }
    return self; 
}


- (void)loadSettings
{
  self.pluginObjects = [[NSMutableDictionary alloc] initWithCapacity:4];
  
  // read from Cordova.plist in the app bundle
  NSString* appPlistName = @"Cordova";
  NSDictionary* cordovaPlist = [CDVViewController getBundlePlist:appPlistName];
  if (cordovaPlist == nil) {
    NSLog(@"WARNING: %@.plist is missing.", appPlistName);
    return;
  }
  self.settings = [[NSDictionary alloc] initWithDictionary:cordovaPlist];
  
  // read from Plugins dict in Cordova.plist in the app bundle
  NSString* pluginsKey = @"Plugins";
  NSDictionary* pluginsDict = [self.settings objectForKey:@"Plugins"];
  if (pluginsDict == nil) {
    NSLog(@"WARNING: %@ key in %@.plist is missing! Cordova will not work, you need to have this key.", pluginsKey, appPlistName);
    return;
  }
  
  self.pluginsMap = [pluginsDict dictionaryWithLowercaseKeys];
}


-(void) loadView
{
    NSLog(@"start to load webview");
    UIWindow *appWindow = [[[UIApplication sharedApplication] delegate] window];
    //create the titlebar
    
    float appHeight = appWindow.frame.size.height;
    if(self.props.height != 0){
        appHeight = self.props.height;
    }
    float appWidth = appWindow.frame.size.width;
    if(self.props.width != 0){
        appWidth = self.props.width;
    }
    NSLog(@"Width: %f, Height %f", appWidth, appHeight);
    topView = [[UIView alloc] initWithFrame: CGRectMake(self.props.x, self.props.y, appWidth, appHeight)];
    topView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight |UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleTopMargin;
    
    float titleBarHeight = 45;

    if (([[[self class] sdkVersion] floatValue] >= [[NSNumber numberWithInt:7] floatValue]) && [[self class] atLeastIOSVersion:@"7.0"]) {
      NSLog(@"Build for ios and running on io7, updating title bar height");
      titleBarHeight = 65;
    }

    if(self.props.showTitleBar){
        titleBar = [[UINavigationBar alloc] initWithFrame:CGRectMake(0, 0, appWidth, titleBarHeight)];
        titleBar.barStyle = UIBarStyleBlack;
        if (([[[self class] sdkVersion] floatValue] >= [[NSNumber numberWithInt:7] floatValue]) && [[self class] atLeastIOSVersion:@"7.0"]) {
          NSLog(@"Build for ios and running on io7, change title bar style");
          titleBar.barStyle = UIBarStyleDefault;
        }
        if(self.props.titleBarColor != @"default"){
            titleBar.tintColor = [UIColor colorWithHexString:self.props.titleBarColor];
        }
        titleBar.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    } else {
        titleBarHeight = 0;
    }
    
    //make a new webview
    UIWebView * mkView = [[UIWebView alloc] initWithFrame:CGRectMake(0, titleBarHeight, appWidth, appHeight - titleBarHeight)];
    webView = mkView;
    cleanWebView = mkView;
  
    cleanWebView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    cleanWebView.scalesPageToFit = YES;
    cleanWebView.delegate = self;
  
    activityView = [[UIActivityIndicatorView alloc] initWithFrame: CGRectMake(appWidth/2 - 12.5, appHeight/2 - 12.5, 25, 25)];
    activityView.activityIndicatorViewStyle = UIActivityIndicatorViewStyleGray;
    [activityView sizeToFit];
    activityView.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleBottomMargin;
    
    if(titleBar){
      [topView addSubview:titleBar];
    }
    
    [topView addSubview:activityView];
    //new webview is added now having been drawn at the right size
    [topView addSubview:cleanWebView];
    
    if(titleBar){
        UINavigationItem *titleBarItem = [[UINavigationItem alloc] initWithTitle:self.props.title];
        //create the close button
        UIBarButtonItem *done = [[UIBarButtonItem alloc] initWithTitle: @"Close" style: UIBarButtonSystemItemDone target: self action: @selector(close)];
        [titleBarItem setLeftBarButtonItem:done];
        
        if(self.props.showControls){
            NSArray *icons = [NSArray arrayWithObjects: [UIImage imageNamed:@"left_arrow.png"], [UIImage imageNamed:@"right_arrow.png"], [UIImage imageNamed:@"house_icon.png"], nil] ;
            UISegmentedControl *controls = [[UISegmentedControl alloc] initWithItems:icons];
            controls.segmentedControlStyle = UISegmentedControlStyleBar;
            controls.selectedSegmentIndex = -1;
            controls.momentary = YES;
            //controls.autoresizingMask = UIViewAutoresizingFlexibleWidth;
            [controls setEnabled:NO forSegmentAtIndex:0];
            [controls setEnabled:NO forSegmentAtIndex:1];
            [controls addTarget:self.webview action:@selector(navigateWebview:) forControlEvents:UIControlEventValueChanged];
            
            
            UIBarButtonItem *navigator = [[UIBarButtonItem alloc] initWithCustomView:controls];
            [titleBarItem setRightBarButtonItem:navigator];

        }
        
        [self.titleBar pushNavigationItem:titleBarItem animated:NO];
    }
    
    self.view = topView;
}

- (void)viewDidUnload
{
  // Release any retained subviews of the main view.
  // e.g. self.myOutlet = nil;
  
  self.cleanWebView.delegate = nil;
  self.cleanWebView = nil;
}

- (void) checkControls
{
  UISegmentedControl *controls = (UISegmentedControl *) self.titleBar.topItem.rightBarButtonItem.customView;
  if([cleanWebView canGoBack]){
    [controls setEnabled: YES forSegmentAtIndex:0];
  } else {
    [controls setEnabled:NO forSegmentAtIndex:0];
  }
  if ([cleanWebView canGoForward]) {
    [controls setEnabled:YES forSegmentAtIndex:1];
  } else {
    [controls setEnabled:NO forSegmentAtIndex:1];
  }

}


/**
 When web application loads Add stuff to the DOM, mainly the user-defined settings from the Settings.plist file, and
 the device's data such as device ID, platform version, etc.
 */
- (void)webViewDidStartLoad:(UIWebView*)theWebView
{
  [self showActivity];
  [_commandQueue resetRequestId];
}

/**
 Called when the webview finishes loading.  This stops the activity view and closes the imageview
 */
- (void)webViewDidFinishLoad:(UIWebView*)theWebView
{
  // The iOSVCAddr is used to identify the WebView instance when using one of the XHR js->native bridge modes.
  // The .onNativeReady().fire() will work when cordova.js is already loaded.
  // The _nativeReady = true; is used when this is run before cordova.js is loaded.
  if(self.props.setupBridge){
    NSString* nativeReady = [NSString stringWithFormat:@"cordova.iOSVCAddr='%lld';try{cordova.require('cordova/channel').onNativeReady.fire();}catch(e){window._nativeReady = true;}", (long long)self];
    [self.commandDelegate evalJs:nativeReady];
  }
  [self checkControls];
    [self hideActivity];
}

- (void)webView:(UIWebView*)webView didFailLoadWithError:(NSError*)error
{
  [self hideActivity];
  NSLog(@"Failed to load webpage with error: %@", [error localizedDescription]);
}

- (BOOL)webView:(UIWebView*)theWebView shouldStartLoadWithRequest:(NSURLRequest*)request navigationType:(UIWebViewNavigationType)navigationType
{
  NSURL* url = [request URL];
  
  /*
   * Execute any commands queued with cordova.exec() on the JS side.
   * The part of the URL after gap:// is irrelevant.
   */
  if ([[url scheme] isEqualToString:@"gap"]) {
    [_commandQueue fetchCommandsFromJs];
    return NO;
  }
  
  /*
   * If a URL is being loaded that's a file/http/https URL, just load it internally
   */
  else if ([url isFileURL]) {
    return YES;
  }
  
  /*
   * all tel: scheme urls we let the UIWebview handle it using the default behavior
   */
  else if ([[url scheme] isEqualToString:@"tel"]) {
    return YES;
  }
  
  /*
   * all about: scheme urls are not handled
   */
  else if ([[url scheme] isEqualToString:@"about"]) {
    return NO;
  }
  
  /*
   * all data: scheme urls are handled
   */
  else if ([[url scheme] isEqualToString:@"data"]) {
    return YES;
  }
  
  /*
   * Handle all other types of urls (tel:, sms:), and requests to load a url in the main webview.
   */
  else {
    // BOOL isIFrame = ([theWebView.request.mainDocumentURL absoluteString] == nil);
    
    if ([self.whitelist schemeIsAllowed:[url scheme]]) {
      NSLog(@"Start to load url %@", url);
      NSString* jscallback = [[NSString alloc] initWithFormat:@"(function(){var e = document.createEvent('Events');e.initEvent('webviewUrlChange');e.data = '%@';document.dispatchEvent(e);})();", [url absoluteURL]];
      [self.webview writeJavascript:jscallback];
      return YES;
    } else {
      if ([[UIApplication sharedApplication] canOpenURL:url]) {
        [[UIApplication sharedApplication] openURL:url];
      } else { // handle any custom schemes to plugins
        [[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:CDVPluginHandleOpenURLNotification object:url]];
      }
    }
    
    return NO;
  }
  
  return YES;
}


- (void)registerPlugin:(CDVPlugin*)plugin withClassName:(NSString*)className
{
  if ([plugin respondsToSelector:@selector(setViewController:)]) {
    [plugin setViewController:self];
  }
  
  if ([plugin respondsToSelector:@selector(setCommandDelegate:)]) {
    [plugin setCommandDelegate:_commandDelegate];
  }
  
  [self.pluginObjects setObject:plugin forKey:className];
}

/**
 Returns an instance of a CordovaCommand object, based on its name.  If one exists already, it is returned.
 */
- (id)getCommandInstance:(NSString*)pluginName
{
  // first, we try to find the pluginName in the pluginsMap
  // (acts as a whitelist as well) if it does not exist, we return nil
  // NOTE: plugin names are matched as lowercase to avoid problems - however, a
  // possible issue is there can be duplicates possible if you had:
  // "org.apache.cordova.Foo" and "org.apache.cordova.foo" - only the lower-cased entry will match
  NSString* className = [self.pluginsMap objectForKey:[pluginName lowercaseString]];
  
  if (className == nil) {
    return nil;
  }
  
  id obj = [self.pluginObjects objectForKey:className];
  if (!obj) {
    // attempt to load the settings for this command class
    NSDictionary* classSettings = [self.settings objectForKey:className];
    
    if (classSettings) {
      obj = [[NSClassFromString (className)alloc] initWithWebView:self.cleanWebView settings:classSettings];
    } else {
      obj = [[NSClassFromString (className)alloc] initWithWebView:self.cleanWebView];
    }
    
    if ((obj != nil) && [obj isKindOfClass:[CDVPlugin class]]) {
      [self registerPlugin:obj withClassName:className];
    } else {
      NSLog(@"CDVPlugin class %@ (pluginName: %@) does not exist.", className, pluginName);
    }
  }
  return obj;
}

- (void) loadUrl:(NSURLRequest*) request
{
  [cleanWebView loadRequest:request];
}

- (NSString *)pathForResource:(NSString *)resourcepath{
  return [_commandDelegate pathForResource:resourcepath];
}

- (void) showView
{
    if(self.props.fullScreen){
      [[self.webview viewController] presentModalViewController:self animated:YES];
    } else {
      [[self.webview viewController].view addSubview:self.view];
    }
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation) interfaceOrientation
{
    return [[self.webview viewController] shouldAutorotateToInterfaceOrientation:interfaceOrientation];
}

- (BOOL)canBecomeFirstResponder {
	return YES;
}

- (void) showActivity
{
    [topView bringSubviewToFront:activityView];
    [activityView startAnimating];
}

- (void) hideActivity
{
    [activityView stopAnimating];
    [topView sendSubviewToBack:activityView];
}

- (void) close
{
    NSLog(@"close webview");
    if(self.props.fullScreen){
      [[self.webview viewController]dismissModalViewControllerAnimated:YES];
    } else {
        [self.view removeFromSuperview];
    }
    NSString* jscallback = @"navigator.webview.close_success()";
    [self.webview writeJavascript:jscallback];
}

+ (NSNumber*)sdkVersion
{
  NSLog(@"SDK version is %d", __IPHONE_OS_VERSION_MAX_ALLOWED/10000);
  return [NSNumber numberWithFloat:__IPHONE_OS_VERSION_MAX_ALLOWED/10000];
}

+(BOOL) atLeastIOSVersion:(NSString*) version
{
  NSLog(@"Current systemVersion = %@", [[UIDevice currentDevice] systemVersion]);
  BOOL isTrue = [[[UIDevice currentDevice] systemVersion] compare:version options:NSNumericSearch] != NSOrderedAscending;
  NSLog(@"Runtime version at least %@ = %d", version, isTrue);
  return isTrue;
}




@end
