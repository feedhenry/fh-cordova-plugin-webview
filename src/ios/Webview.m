//Created by FeedHenry

#import "Webview.h"

@implementation Webview

@synthesize initialRequest;
@synthesize cleanWebviewController;
@synthesize setupAPIs;
@synthesize _data;

- (void) load:(CDVInvokedUrlCommand*)command
{
    NSDictionary *options = [command.arguments objectAtIndex:0];
    NSString *url = [options objectForKey:@"url"];
    NSString *title = [options objectForKey:@"title"];
    
    if(nil == url){
        NSString* jscallback = @"window.plugins.webview.fail('no_url')";
        [self writeJavascript:jscallback];
        return;
    }
    if(nil == title){
        title = @"";
    }
    WebviewContainerProperties *props = [self defaultProperties];
    props.title = title;
    
    NSString* xStr = [options objectForKey:@"x"];
    NSString* yStr = [options objectForKey:@"y"];
    NSString* widthStr = [options objectForKey:@"width"];
    NSString* heightStr = [options objectForKey:@"height"];
    NSString* titleBarColor = [options objectForKey:@"titleBarColor"];
    NSString* showControlsStr = [options objectForKey:@"showControls"];
    NSString* showTitleBarStr = [options objectForKey:@"showTitleBar"];
    self.setupAPIs = NO;
    if([options objectForKey:@"setupBridge"]){
        self.setupAPIs = [(NSString*) [options objectForKey:@"setupBridge"] boolValue];
    }
    props.setupBridge = self.setupAPIs;
    if(nil != xStr){
        props.x = [xStr floatValue];
    }
    if(nil != yStr){
        props.y = [yStr floatValue];
    }
    if(nil != widthStr){
        props.width = [widthStr floatValue];
    }
    if(nil != heightStr){
        props.height = [heightStr floatValue];
    }
    if(nil != titleBarColor){
        props.titleBarColor = titleBarColor;
    }
    if(nil != showControlsStr){
        props.showControls = [showControlsStr boolValue];
    }
    if(nil != showTitleBarStr){
        props.showTitleBar = [showTitleBarStr boolValue];
    }
    if(0 != props.x || 0 != props.y || 0 != props.width || 0 != props.height){
        props.fullScreen = NO;
    }
    //make a view controller
    WebviewController * wControl = [[WebviewController alloc] initWithSettings:props webview:self];
    cleanWebviewController = wControl;
    //the webViews view controller is called upon to show this Webview.. current view controller is CDVViewController
    [cleanWebviewController showView];
	
    NSURL *request = [NSURL URLWithString:url];
	if ([url hasPrefix:@"/"]){
		request = [NSURL fileURLWithPath:url];
	}
    self.initialRequest = [NSURLRequest requestWithURL:request cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:20.0];
    
    if(self.setupAPIs){
        [NSURLConnection connectionWithRequest:self.initialRequest delegate:self];
    } else {
        [cleanWebviewController loadUrl:self.initialRequest];
    }
    NSString* jscallback = @"window.plugins.webview.open_success()";
    [self writeJavascript:jscallback];
}

- (void) close:(CDVInvokedUrlCommand*)command 
{
    [cleanWebviewController close];
}

-(IBAction) navigateWebview: (id)sender
{
    UISegmentedControl *navigator = (UISegmentedControl *) sender;
    NSInteger selected = [navigator selectedSegmentIndex];
    UIWebView *newWebView = cleanWebviewController.cleanWebView;
    NSLog(@"selected: %d", selected);
    switch (selected) {
        case 0:
            [newWebView goBack];
            break;
        case 1:
            [newWebView goForward];
            break;
        case 2:
            [newWebView loadRequest: self.initialRequest];
            break;
    }
}

- (void) closeView
{
    NSString* jscallback = @"window.plugins.webview.close_success()";
    [self writeJavascript:jscallback];
}

- (WebviewContainerProperties *) defaultProperties {
    WebviewContainerProperties *ret = [WebviewContainerProperties new];
    ret.fullScreen = YES;
    ret.x = 0;
    ret.y = 0;
    ret.width = 0;
    ret.height = 0;
    ret.showControls = YES;
    ret.titleBarColor = @"default";
    ret.title = @"";
    ret.showTitleBar = YES;
    return ret;
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
    if(_data)
    {
        _data = nil;
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    if(!_data)
    {
        _data = [data mutableCopy];
    }
    else
    {
        [_data appendData:data];
    }
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    if(_data)
    {
        NSString* content = [[NSString alloc] initWithData:_data
                                                  encoding:NSUTF8StringEncoding];
        
        _data = nil;
        
        //load container.js and inject into the child webview
        NSString* containerFile = [self.cleanWebviewController pathForResource:@"fhext/js/container.js"];
        NSString* pgFile = [self.cleanWebviewController pathForResource:@"cordova.js"];
        NSString* iosFile = [self.cleanWebviewController pathForResource:@"cordova_plugins.js"];
        NSMutableString *script = [[NSMutableString alloc] initWithString:@"<script>"];
        if(nil != containerFile){
            [script appendString:[NSString stringWithContentsOfFile:containerFile encoding:NSUTF8StringEncoding error:NULL]];
        }
        if(nil != pgFile){
            [script appendString:[NSString stringWithContentsOfFile:pgFile encoding:NSUTF8StringEncoding error:NULL]];
        }
        if(nil != iosFile){
            [script appendString:[NSString stringWithContentsOfFile:iosFile encoding:NSUTF8StringEncoding error:NULL]];
        }
        [script appendString:@"cordova.require('cordova/exec').setJsToNativeBridgeMode(0);"];
        [script appendString:@"</script>"];
        [script appendString:content];
        [self.cleanWebviewController.cleanWebView loadHTMLString:script baseURL:self.initialRequest.URL];
    }
}


- (void) dealloc
{
    cleanWebviewController.cleanWebView.delegate = nil;
}

@end
