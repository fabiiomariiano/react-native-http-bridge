# react-native-http-bridge

Simple HTTP server for [React Native](https://github.com/facebook/react-native).
Created for [Status.im](https://github.com/status-im/status-react). 

Since 0.5.0 supports and handles GET, POST, PUT and DELETE requests.
The library can be useful for handling requests with `application/json` content type
(and this is the only content type we support at the current stage) and returning different responses.

Since 0.6.0 can handle millions of requests at the same time and also includes some very basic support for [React Native QT](https://github.com/status-im/react-native-desktop). 

Since 0.7.0 can delivery files too. The folder directory could be passed as parameter of the 'start' function. Additionally, can be passed a parameter when request files to try find it in root directory without it extension.

## Install

```shell
npm install --save react-native-http-bridge
```

## Automatically link

#### With React Native 0.27+

```shell
react-native link react-native-http-bridge
```

## Example

First import/require react-native-http-server:

```js

    var httpBridge = require('react-native-http-bridge');

```


Initalize the server in the `componentWillMount` lifecycle method. You need to provide a `port` and a callback. Optionally, you can provide a folder directory to delivery files from it.

```js

    componentWillMount() {
      // initalize the server (now accessible via localhost:1234)
      httpBridge.start(5561, 'http_service', 'folder_directory', request => {

          // you can use request.url, request.type and request.postData here
          if (request.type === "GET" && request.url.split("/")[1] === "users") {
            httpBridge.respond(request.requestId, 200, "application/json", "{\"message\": \"OK\"}");
          } else {
            httpBridge.respond(request.requestId, 400, "application/json", "{\"message\": \"Bad Request\"}");
          }

      });
    }

```

To check too with file exists in the root directory without it extension, just add the parameter `checkFileWithoutExtension` with `true` value. And the server will check at first if file exists with all name passed, and after, if it has extension, check if file exists without it. Example:

```js

  <video
    id="video-id" 
    alt="video-test"
    muted
    autoplay
    src={`/${fileName}.mp4?checkFileWithoutExtension=true`}
  />
```

Finally, ensure that you disable the server when your component is being unmounted.

```js

  componentWillUnmount() {
    httpBridge.stop();
  }

```
