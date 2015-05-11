Change Log
==========

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
