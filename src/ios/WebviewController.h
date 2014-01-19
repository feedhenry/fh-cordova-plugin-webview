//
//  WebviewController.h
//  Feedhenry
//
//  Created by FeedHenry.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVCommandQueue.h>
#import <Cordova/CDVCommandDelegateImpl.h>
#import <Cordova/CDVWhitelist.h>
#import <Cordova/NSDictionary+Extensions.h>


@interface WebviewContainerProperties : NSObject {
    BOOL fullScreen;
    CGFloat x;
    CGFloat y;
    CGFloat width;
    CGFloat height;
    NSString *titleBarColor;
    BOOL showControls;
    NSString *title;
    BOOL showTitleBar;
    BOOL setupBridge;
}
@property(nonatomic, assign) BOOL fullScreen;
@property(nonatomic, assign) CGFloat x;
@property(nonatomic, assign) CGFloat y;
@property(nonatomic, assign) CGFloat width;
@property(nonatomic, assign) CGFloat height;
@property(nonatomic, retain) NSString* titleBarColor;
@property(nonatomic, assign) BOOL showControls;
@property(nonatomic, retain) NSString* title;
@property(nonatomic, assign) BOOL showTitleBar;
@property(nonatomic, assign) BOOL setupBridge;

@end

@class WebviewController;
@class CDVCordovaView;

@interface WebviewController : UIViewController <UIWebViewDelegate> {
    UIView *topView;
    UINavigationBar *titleBar;
    UIWebView *cleanWebView;
    UIActivityIndicatorView *activityView;
    WebviewContainerProperties *props;
    CDVPlugin *webview;
@private
    CDVCommandDelegateImpl* _commandDelegate;
}

@property (nonatomic, retain) UIView *topView;
@property (nonatomic, retain) UINavigationBar *titleBar;
@property (nonatomic, retain) UIWebView *cleanWebView;
@property (nonatomic, retain) UIActivityIndicatorView *activityView;
@property (nonatomic, retain) WebviewContainerProperties *props;
@property (nonatomic, retain) CDVPlugin *webview; //The parent WebView plugin
@property (nonatomic, retain) UIWebView* webView; // The CordovaView(UIWevView) presented in this ViewController

@property (nonatomic, readonly, strong) NSMutableDictionary* pluginObjects;
@property (nonatomic, readonly, strong) NSDictionary* pluginsMap;
@property (nonatomic, readonly, strong) NSDictionary* settings;
@property (nonatomic, readonly, strong) CDVWhitelist* whitelist; // readonly for public
@property (nonatomic, readonly, strong) CDVCommandQueue* commandQueue;
@property (nonatomic, readonly, strong) CDVCommandDelegateImpl* commandDelegate;
@property (nonatomic, readwrite, copy) NSString* wwwFolderName;

- (id) initWithSettings:(WebviewContainerProperties *)settings 
                webview:(CDVPlugin *)theView;
- (void) loadView;
- (void) loadUrl:(NSURLRequest*) request;
- (NSString *)pathForResource:(NSString *)resourcepath;
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation) interfaceOrientation;
- (void) showActivity;
- (void) hideActivity;
- (void) showView;
- (void) close;

@end
