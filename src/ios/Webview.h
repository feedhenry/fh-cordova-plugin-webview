
//Created by FeedHenry

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "WebviewController.h"

@interface Webview :  CDVPlugin
{
    WebviewController *cleanWebviewController;
    NSURLRequest *initialRequest;
}

@property (nonatomic, retain) WebviewController *cleanWebviewController;
@property (nonatomic, retain) NSURLRequest *initialRequest;
@property (nonatomic, readwrite) BOOL setupAPIs;
@property (nonatomic, readwrite, retain) NSMutableData * _data;


- (void) load: (CDVInvokedUrlCommand*)command;

- (void) close: (CDVInvokedUrlCommand*)command;

@end