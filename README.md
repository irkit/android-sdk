IRKitの機能をAndroidアプリに組み込むためのSDKです。

## 機能

- IRKitのセットアップ
- リモコン信号の学習
- リモコン信号の送信

## ダウンロード

Android Studioをお使いの場合、モジュールのbuild.gradleの`dependencies`内に以下の1行を追加してください。

    compile 'com.getirkit:irkit-android-sdk:1.0.0'

## サンプルアプリの動かし方

1. Android Studioを開く
2. "Open an existing Android Studio project" をクリック
3. このフォルダを選択
4. AndroidManifest.xmlを開いてYOUR_API_KEYを置き換える
5. "Run 'app'" をクリック

## サンプルコード

[app/src/main/java/com/getirkit/example/activity/MainActivity.java](app/src/main/java/com/getirkit/example/activity/MainActivity.java)を見てください。


## Features

- Set up IRKit devices
- Learn remote signals
- Send remote signals

## Download

If you are using Android Studio, add the following line to the `dependencies` section in your module-level build.gradle.

    compile 'com.getirkit:irkit-android-sdk:1.0.0'

## How to run the example app

1. Open Android Studio
2. Click "Open an existing Android Studio project"
3. Choose this directory
4. Open AndroidManifest.xml and replace YOUR_API_KEY with your IRKit apikey
5. Click "Run 'app'"

## Example source code

See [app/src/main/java/com/getirkit/example/activity/MainActivity.java](app/src/main/java/com/getirkit/example/activity/MainActivity.java).

## License

    Copyright 2015 Nao Iizuka

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

This project contains [JmDNS](http://jmdns.sourceforge.net/) library.

    Copyright 2003-2005 Arthur van Hoff, Rick Blair
    Licensed under Apache License version 2.0
