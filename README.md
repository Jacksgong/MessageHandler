# MessageHandler

[![Download][bintray_svg]][bintray_url]
![][license_2_svg]

This is a simple Handler forwarding, for supporting `pause`、`resume`、`cancelAllMessage`、`killSelf` skills.

> [中文文档](https://github.com/Jacksgong/MessageHandler/blob/master/README-zh.md)

## Demo

![][demo_gif]

## Installation

MessageHandler is installed by adding the following dependency to your build.gradle file:

```
dependencies {
    compile 'cn.dreamtobe.messagehandler:messagehandler:0.1.1'
}
```

## How to use?

> All methods are Thread safe.

| method | description
| --- | ---
| pause(void) | Pause all messages(mark and calculate all message's delay from the moment of pause).
| resume(void) | Resume all messages.
| cancelAllMessage(void) | Cancel and clear all messages have already existed in message queue.
| killSelf(void) | Discard MessageHandler, and do not accept any messages.

> The following interface provides the same functionality as Handler

| method | description |
| --- | --- |
| sendEmptyMessage(what) | The same as `Handler#sendEmptyMessage`
| sendEmptyMessageDelayed(what, delayMillis) | The same as `Handler#sendEmptyMessageDelayed`
| sendMessage(msg) | The same as `Handler#sendMessage`
| sendMessageDelayed(msg, delayMillis) | The same as `Handler#sendMessageDelayed`
| sendMessageAtTime(msg, uptimeMillis) | The same as `Handler#sendMessageAtTime`
| sendMessageAtFrontOfQueue(msg) | The same as `Handler#sendMessageAtFrontOfQueue`
| post(runnable) | The same as `Hanler#post`
| postDelayed(runnable, delayMillis) | The same as `Hanler#postDelayed`
| removeMessages(what) | The same as `Handler#removeMessages`
| removeCallbacks(runnable) | The same as `Handler#removeCallbacks`
| obtainMessage(void):Message | The same as `Handler#obtainMessage`

## LICENSE

```
Copyright (c) 2016 Jacksgong(blog.dreamtobe.cn).

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[license_2_svg]: https://img.shields.io/hexpm/l/plug.svg
[bintray_svg]: https://api.bintray.com/packages/jacksgong/maven/MessageHandler/images/download.svg
[bintray_url]: https://bintray.com/jacksgong/maven/MessageHandler/_latestVersion
[demo_gif]: https://github.com/Jacksgong/MessageHandler/raw/master/art/demo.gif
