IRKitの機能をAndroidアプリに組み込むためのSDKです。

## 機能

- IRKitのセットアップ
- リモコン信号の学習
- リモコン信号の送信

## アプリへの組み込み

### ダウンロード

Android Studioをお使いの場合、モジュールのbuild.gradleの`dependencies`内に以下の1行を追加してください。

    compile 'com.getirkit:irkit-android-sdk:1.1.1'

### apikeyをセットする

AndroidManifest.xmlの`<application>`内に以下の`<meta-data>`を追加します。`YOUR_API_KEY`の部分を取得したapikeyに置き換えてください。apikeyの取得方法は[POST /1/apps](http://getirkit.com/#IRKit-Internet-POST-1-apps)を参照してください。

    <meta-data android:name="com.getirkit.IRKIT_API_KEY" android:value="YOUR_API_KEY" />

### Activity

SDKには4つのActivityが用意されています。

Activity           | 用途
------------------ | -------------------------------
IRKitSetupActivity | IRKitをセットアップする
DeviceActivity     | IRKitデバイス詳細情報を表示・編集する
WaitSignalActivity | リモコン信号を学習する
SignalActivity     | ボタン（信号）情報を表示・編集する

それぞれの使い方を以下で説明します。

### Activity用の定数を定義する

各アクティビティから返される値を受け取るため、以下のように定数を定義しておきます。

    private static final int REQUEST_IRKIT_SETUP   = 1;
    private static final int REQUEST_SIGNAL_DETAIL = 2;
    private static final int REQUEST_WAIT_SIGNAL   = 3;
    private static final int REQUEST_DEVICE_DETAIL = 4;

### IRKitSetupActivity

IRKitSetupActivityを起動すると、IRKitのセットアップをユーザに行わせることができます。

[![IRKitSetupActivity](images/IRKitSetupActivity-w260.png)](images/IRKitSetupActivity.png)

    Intent intent = new Intent(this, IRKitSetupActivity.class);
    startActivityForResult(intent, REQUEST_IRKIT_SETUP);

セットアップが完了した場合はRESULT_OKが返ります。

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IRKIT_SETUP && resultCode == RESULT_OK) {
            // セットアップが完了した
        }
    }

### WaitSignalActivity

WaitSignalActivityを起動すると、リモコン信号の学習をユーザに行わせることができます。

[![WaitSignalActivity](images/WaitSignalActivity-w260.png)](images/WaitSignalActivity.png)

    Intent intent = new Intent(this, WaitSignalActivity.class);
    startActivityForResult(intent, REQUEST_WAIT_SIGNAL);

学習が完了した場合はRESULT_OKが返ります。

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_WAIT_SIGNAL && resultCode == RESULT_OK) {
            Bundle args = data.getExtras();
            IRSignal signal = args.getParcelable("signal"); // 受信した信号
            IRKit irkit = IRKit.sharedInstance();
            signal.setId(irkit.signals.getNewId()); // ランダムなidを割り当てる

            if (signal.hasBitmapImage()) { // アイコンに写真が指定された
                // signal.renameToSuggestedImageFilename() は setId() より後に呼ぶ
                if (!signal.renameToSuggestedImageFilename(this)) {
                    // ファイル名変更失敗
                }
            } else { // アイコンリストから選択された
                // signalのimageResourceIdを元にimageResourceNameを更新する
                signal.onUpdateImageResourceId(getResources());
            }

            // ボタンのリストに追加する
            irkit.signals.add(signal);
            irkit.signals.save();
        }
    }

### SignalActivity

学習済のボタンの詳細情報を表示、編集、削除する画面を表示します。

[![SignalActivity](images/SignalActivity-w260.png)](images/SignalActivity.png)

    Bundle args = new Bundle();
    args.putInt("mode", SignalActivity.MODE_EDIT); // 編集モード
    args.putParcelable("signal", signal); // IRSignalインスタンスを渡す
    Intent intent = new Intent(this, SignalActivity.class);
    intent.putExtras(args);
    startActivityForResult(intent, REQUEST_SIGNAL_DETAIL);

ユーザが「保存」または「削除」を押した場合はRESULT_OKが返ります。下のコードで変数actionには、保存の場合は`save`、削除の場合は`delete`という文字列が入ります。

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SIGNAL_DETAIL && resultCode == RESULT_OK) {
            Bundle args = data.getExtras();
            String action = args.getString("action");
            IRSignal signal = args.getParcelable("signal");
            int mode = args.getInt("mode");
            IRKit irkit = IRKit.sharedInstance();
            switch (action) {
                case "save": // 「保存」が押された
                    if (editingSignal != null) {
                        // signalのデータをeditingSignalにコピーする
                        editingSignal.copyFrom(signal, this);
                        irkit.signals.save();
                    }
                    break;
                case "delete": // 「削除」が押された
                    if (editingSignal != null) {
                        // IRSignalを削除する
                        irkit.signals.remove(editingSignal);
                        irkit.signals.save();
                    }
                    break;
                default:
                    break;
            }
        }
    }

### DeviceActivity

IRKitデバイスの詳細情報を表示、編集、削除する画面を表示します。

[![DeviceActivity](images/DeviceActivity-w260.png)](images/DeviceActivity.png)

    Bundle args = new Bundle();
    args.putParcelable("peripheral", peripheral); // 対象のIRPeripheralインスタンスを渡す
    Intent intent = new Intent(this, DeviceActivity.class);
    intent.putExtras(args);
    startActivityForResult(intent, REQUEST_DEVICE_DETAIL);

「保存」または「削除」ボタンが押された場合はRESULT_OKが返ります。下のコードで変数actionには、保存の場合は`save`、削除の場合は`delete`という文字列が入ります。

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DEVICE_DETAIL && resultCode == RESULT_OK) {
            Bundle args = data.getExtras();
            String action = args.getString("action");
            IRPeripheral peripheral = args.getParcelable("peripheral");
            IRKit irkit = IRKit.sharedInstance();
            switch (action) {
                case "save": // 「保存」が押された
                    if (editingPeripheral != null) {
                        // customizedNameの変更を保存する
                        editingPeripheral.setCustomizedName(
                            peripheral.getCustomizedName()
                        );
                        irkit.peripherals.save();
                    }
                    break;
                case "delete": // 「削除」が押された
                    if (editingPeripheral != null) {
                        // IRPeripheralを削除する
                        irkit.peripherals.remove(editingPeripheral);
                        irkit.peripherals.save();

                        // 削除したIRPeripheralに紐付いたIRSignalを削除する
                        irkit.signals.removeIRSignalsForDeviceId(
                            editingPeripheral.getDeviceId()
                        );
                        irkit.signals.save();
                    }
                    break;
                default:
                    break;
            }
        }
    }

### クラスの概要

クラス名      | 役割
------------- | -------------------------------
IRKit         | SDKの基本クラス
IRSignal      | 赤外線信号1個を表す
IRSignals     | IRSignalを格納するArrayList
IRPeripheral  | IRKitデバイス1個を表す
IRPeripherals | IRPeripheralを格納するArrayList

### SDKの初期化

SDKの基本となるIRKitインスタンスは`IRKit.sharedInstance()`で取得できます。IRKit SDKを使用するActivityのonCreate()内で以下のようにSDKを初期化（有効化）します。

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ...

        // ContextをセットしてSDKを初期化する。すでに初期化済みの場合は
        // Contextのセットのみ行われる。
        IRKit.sharedInstance().init(getApplicationContext());
    }

init()は初回呼び出し時のみデータ読み込みなどの初期化を行います。複数のActivityのonCreate()にinit()を入れて問題ありません。

IRSignalsとIRPeripheralsの各インスタンスは以下のように取得できます。

    IRKit irkit = IRKit.sharedInstance();

    // 保存されている赤外線信号一覧を取得
    IRSignals signals = irkit.signals;

    // 保存済のIRKitデバイス一覧を取得
    IRPeripherals peripherals = irkit.peripherals;

### IRKitデバイス発見イベントを受け取る

SDKはローカルネットワーク内のIRKitをmDNSで自動検出します。IRKitデバイスが見つかった際にイベントを受け取るには、IRKitEventListenerを実装して以下2つのメソッドをオーバーライドします。

    @Override
    public void onNewIRKitFound(IRPeripheral peripheral) {
        // 新しいIRKitデバイスを発見した
    }

    @Override
    public void onExistingIRKitFound(IRPeripheral peripheral) {
        // 既存のIRKitデバイスを発見した
    }

実装したIRKitEventListenerを引数として`setIRKitEventListener()`を呼ぶと、IRKit検出イベントを受け取れるようになります。

    IRKit.sharedInstance().setIRKitEventListener(this);

SDKが新しいIRKitデバイスを発見した場合、内部的な設定とIRKit.sharedInstance().peripheralsへの追加をSDKが自動的に行います。検出したIRKitが「新しいIRKit」として認識されるのは、IRKit.sharedInstance().peripheralsに含まれていないIRKitを発見した場合です。

### 信号を手動で登録する

    IRSignals signals = IRKit.sharedInstance().signals;

    IRSignal signal = new IRSignal();
    // format, freq, dataの仕様は http://getirkit.com/#toc_5 を参照
    signal.setFormat("raw");
    signal.setFrequency(38.0f);
    signal.setData(new int[]{
        18031, 8755, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 1190, 1190, 3341, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 3341, 1190, 65535, 0, 9379, 18031, 4400, 1190
    });

    signal.setId(signals.getNewId()); // 新しいidを割り振る
    signal.setName("暖房"); // 信号の名前

    // Drawableリソースをアイコンとして使う場合
    signal.setImageResourceId(R.drawable.btn_icon_256_aircon, getResources());

    // 画像ファイルをアイコンとして使う場合
    signal.setImageFilename("image.png"); // 内部ストレージ上の画像ファイルのパス

    signal.setDeviceId("testdeviceid"); // 対応するIRKitデバイスのdeviceid

    // IRSignalsに追加して保存
    signals.add(signal);
    signals.save();

### サンプルコード

その他の使い方については[app/src/main/java/com/getirkit/example/activity/MainActivity.java](app/src/main/java/com/getirkit/example/activity/MainActivity.java)を見てください。

## サンプルアプリの動かし方

1. Android Studioを開く
2. "Open an existing Android Studio project" をクリック
3. このフォルダを選択
4. AndroidManifest.xmlを開いて`YOUR_API_KEY`を置き換える
5. "Run 'app'" をクリック


## Features

- Set up IRKit devices
- Learn remote signals
- Send remote signals

## Download

If you are using Android Studio, add the following line to the `dependencies` section in your module-level build.gradle.

    compile 'com.getirkit:irkit-android-sdk:1.1.1'

## How to run the example app

1. Open Android Studio
2. Click "Open an existing Android Studio project"
3. Choose this directory
4. Open AndroidManifest.xml and replace `YOUR_API_KEY` with your IRKit apikey
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
