Change Log
==========

Version 1.2.1 *(2015-06-02)*
----------------------------

### 変更点

- HTTP API通信の安定性を向上し、IRKitが不安定な状態に陥ることが少なくなるようにしました。
- IRHTTPClientクラス
  - インスタンス変数internetAPIServiceとdeviceAPIServiceをprivateにしました。アクセスするにはGetterを使ってください。


### Changes

- Improve the stability of HTTP API. IRKit is less likely to get panic.
- IRHTTPClient class
  - Change internetAPIService and internetAPIService to private variables. Use getters to access them.


Version 1.2.0 *(2015-05-28)*
----------------------------

### 変更点

- mDNSでIRKitを発見した際に毎回modelNameとfirmwareVersionを取得するようにしました。
- Device HTTP APIのリクエスト送信前の待ち時間を調整しました。
- IRKitにWi-Fi情報を送信する場面でエラーが発生しセットアップが中断した場合、自動的に家のWi-Fiに接続し直すようにしました。
- appcompat-v7サポートライブラリを22.1.1に更新しました。
  - ActionBarActivityの代わりにAppCompatActivityを使うようにしました。
- IRKitSetupActivity
  - パスワード入力フィールドを2つともmonospaceフォントに変更しました。
- IRKitクラス
  - setupIRKit()をどのスレッドからでも呼べるようにしました。
- IRHTTPClientクラス
  - testIfIRKitWifiConnected()を追加しました。
- IRDeviceAPIServiceクラス
  - getHome()を追加しました。
- IRPeripheralクラス
  - parseServerHeaderValue()を追加しました。
- サンプルアプリ（appモジュール）
  - テーマカラーを変更しました。
  - Navigation Drawerで選択しているアイテムを強調表示するようにしました。


### Changes

- Retrieve modelName and firmwareVersion every time SDK finds an IRKit over mDNS.
- Modify wait times before sending requests to Device HTTP API.
- Reconnect to home Wi-Fi if IRKit setup has interrupted due to error sending Wi-Fi information to IRKit.
- Update appcompat-v7 version to 22.1.1.
  - Replace ActionBarActivity with AppCompatActivity.
- IRKitSetupActivity
  - Change the font of password fields to monospace.
- IRKit class
  - Make setupIRKit() callable from any thread.
- IRHTTPClient class
  - Add testIfIRKitWifiConnected().
- IRDeviceAPIService class
  - Add getHome().
- IRPeripheral
  - Add parseServerHeaderValue().
- Example app (app module)
  - Change the theme color.
  - Highlight the selected item in the navigation drawer.


Version 1.1.3 *(2015-05-16)*
----------------------------

### 変更点

- IRKitSetupActivity
  - 現在接続されているWi-Fi情報を使ってセキュリティを自動選択するようにしました。
  - Wi-Fiパスワードが空欄のまま次へ進もうとした際にエラー表示を出すようにしました。


### Changes

- IRKitSetupActivity
  - Automatically select Wi-Fi security based on currently connected Wi-Fi.
  - Show error when user attempts to proceed without entering Wi-Fi password.


Version 1.1.2 *(2015-05-11)*
----------------------------

### 変更点

- リソース名の競合を避けるため@dimen/activity_horizontal_marginと@dimen/activity_vertical_marginの名前を変更しました。
- @string/app_nameを削除しました。
- WaitSignalActivityで信号受信後の編集画面でBackボタンを押すと受信待機画面に戻るようにしました。
- IRInternetAPIServiceクラス
  - postApps()を追加しました。
  - GetClientsResponseからPostClientsResponseにクラス名を変更しました。
- IRSignal#setImageResourceId(int, Resources)を追加しました。
- com.getirkit.irkit.net.DownloadImageTaskクラスを削除しました。
- IRKitSetupManagerクラスをpackage-privateに変更しました。


### Changes

- Fix resource name conflict on @dimen/activity_horizontal_margin and @dimen/activity_vertical_margin.
- Remove @string/app_name.
- WaitSignalActivity: Go back to waiting screen when user presses back button on signal edit screen.
- IRInternetAPIService class
  - Add postApps().
  - Change class name from GetClientsResponse to PostClientsResponse.
- Add IRSignal#setImageResourceId(int, Resources).
- Delete com.getirkit.irkit.net.DownloadImageTask class.
- Make IRKitSetupManager class package-private.


Version 1.1.1 *(2015-03-19)*
----------------------------

### 変更点

- deviceidを取得できていないIRPeripheralが存在する際にNullPointerExceptionが発生するバグを修正しました。


### Changes

- Fix: IRPeripheral throws NullPointerException if deviceid has not been obtained.


Version 1.1.0 *(2015-03-18)*
----------------------------

### 変更点

- ベースとなるパッケージ名を`com.getirkit`から`com.getirkit.irkit`に変更しました。


### Changes

- Change base package name from `com.getirkit` to `com.getirkit.irkit`.


Version 1.0.1 *(2015-03-17)*
----------------------------

### 変更点

- `IRKit.SDK_VERSION`定数を削除しました。
- Retrofitを1.9.0に更新しました。
- Android SDK Build Toolsを22.0.0に更新しました。
- `onSaveInstanceState()`でNullPointerExceptionが起きないよう修正しました。


### Changes

- Remove IRKit.SDK_VERSION constant.
- Update Retrofit to 1.9.0.
- Update Android SDK Build Tools to 22.0.0.
- Fix: NullPointerException in `onSaveInstanceState()`.


Version 1.0.0 *(2015-03-09)*
----------------------------

最初のリリースです。


Initial release.
