# 水電表記錄

Android 原生 Jetpack Compose App，package name 為 `com.justplay.meterlog`。目前主流程是建物式抄表：一個帳號可建立多個建物，每個建物可設定樓層數與每層不固定數量的水表/電表，並支援整棟一次輸入讀數。

## 開發環境

- Android Studio
- JDK 17
- Android SDK 35
- Firebase 專案

## Firebase 設定

本專案使用正式的 Firebase Android 設定檔 `app/google-services.json`。此檔案包含專案識別資訊，請只放在本機，不要推上 GitHub；目前 `.gitignore` 已排除 `app/google-services.json`。

首次設定或需要重新下載時，請到 Firebase Console：

1. 建立 Android App，package name 填 `com.justplay.meterlog`。
2. 下載正式 `google-services.json`。
3. 將正式檔案放到本機路徑 `app/google-services.json`。
4. 啟用 Firebase Authentication：
   - Email/Password
   - Google
5. 建立 Cloud Firestore database。
6. 在本機 `local.properties` 加入 Google 登入用的 Web client ID：

```properties
defaultWebClientId=1234567890-xxxx.apps.googleusercontent.com
```

`app/google-services.json` 與 `local.properties` 都已被 `.gitignore` 排除，不應推上 GitHub。

Firestore 資料路徑：

```text
users/{uid}/buildings/{buildingId}
users/{uid}/buildings/{buildingId}/meters/{meterId}
users/{uid}/buildings/{buildingId}/meters/{meterId}/readings/{readingId}
```

## 功能

- 登入/註冊：
  - Email/密碼
  - Google 登入
  - 登出
- 建物清單：
  - 建物名稱
  - 樓層數
  - 每層不固定表具數量
- 建物新增：
  - 輸入建物名稱與樓層數
  - 可用快捷設定套用每層水表數與電表數
  - 也可逐層調整水表與電表數量
  - 自動建立表具，水表命名格式為 `{樓層}F-W{序號}`，電表命名格式為 `{樓層}F-E{序號}`
- 表具編輯：
  - 可改表具名稱
  - 可切換水表/電表
  - 水表顯示水滴 icon，電表顯示閃電 icon
- 整棟一次抄表：
  - 水表與電表分開抄表
  - 依樓層列出對應類型的全部表具
  - 空白欄位不新增讀數
  - 第一筆讀數可直接新增
  - 後續讀數需距離同一表具上次抄表至少 2 個月

## 分段燒錄檢查點

### 檢查點 1：專案初始化

Android Studio 開啟專案後，使用 **Build > Make Project** 或在 Terminal 執行：

```bash
gradle assembleDebug
```

確認：

- App 可啟動。
- package/applicationId 是 `com.justplay.meterlog`。
- Firebase 初始化不閃退。

### 檢查點 2：登入

燒錄後確認：

- Email 註冊可用。
- Email 登入可用。
- Google 登入可用。
- 重開 App 仍保持登入。
- 登出後回到登入畫面。

### 檢查點 3：建物與樓層表具

新增測試建物：

- 建物名稱：`A棟`
- 樓層數：`3`
- 快捷設定：每層水表 `1`、每層電表 `2`
- 套用到全部樓層

確認：

- 建物詳情依樓層顯示表具。
- 每層都有 1 顆水表、2 顆電表。
- 表具自動命名為 `1F-W1`、`1F-E1`、`1F-E2` 等。

### 檢查點 4：表具編輯

確認：

- 可編輯自動產生的表具名稱。
- 可切換水表/電表。
- 水表顯示水滴 icon。
- 電表顯示閃電 icon。
- 重新登入後資料仍存在。

### 檢查點 5：整棟一次抄表

確認：

- 點建物詳情右下角按鈕可進入整棟抄表頁。
- 每層表具依樓層分組。
- 可一次輸入多顆表具讀數。
- 空白欄位不會新增讀數。
- 儲存後 Firestore 會在各表具底下建立 `readings`。

### 檢查點 6：兩個月限制

確認：

- 無讀數表具可輸入第一筆讀數。
- 未滿 2 個月的表具欄位不可輸入，並顯示下次可抄表日期。
- 滿 2 個月的表具可輸入。
- 每顆表具獨立判斷，不會互相影響。

## 資料重建

新版本只讀新的 `buildings` 結構，不自動轉換舊的 `users/{uid}/meters` 測試資料。若要清空舊測試資料，請在 Firebase Console 手動刪除，不要在 App 裡加入批量刪除功能。
