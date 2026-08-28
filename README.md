# 胖錢包計算機™ v2.1｜26合1 × 我的計算機

這一版把兩條線正式合併：

1. **App 內：微笑娜生活萬用計算機 26 合 1** 直接載入正式網站 `https://smilenalife1177.github.io/lina-calculator/`。
2. **桌面：我的計算機 Widget** 保留自選文字、圖片、字級、愛心與機身顏色。

## v2.1 修正重點

- App 一打開就是目前正式的 26 合 1 萬用生活計算機；網站日後 v2.9 / v3.0 更新，App 重新打開或重新整理即可跟上，不必重做 APK。
- 上方固定有：`🧮 26合1`、`🎨 我的桌面計算機`、`＋桌面`。
- 「我的桌面計算機」：
  - 0 顆 Widget → 引導新增。
  - 1 顆 Widget → 直接開啟該顆設計頁。
  - 多顆 Widget → 讓你選要改哪一顆。
- 桌面 Widget 不再只靠小愛心：**整個上方標題區、文字框、名胖圖片、愛心都可以點進編輯**。
- 每顆 Widget 的文字、圖片、機身色、字級、計算結果仍各自保存。
- 預設機身：鵝黃奶油 `#F6E29A`。
- 機身仍支援：鵝黃、玫瑰金、霧藍、鼠尾草、奶茶、薰衣草、粉霧與自填 HEX。
- 自選圖片只存手機本機，不上傳。

## APK 更新方式

v2.1 沿用 v2.0 的 applicationId：`tw.smilenalife.pangwallet.v2`。

因此手機已裝 v2.0 時，安裝 v2.1 APK 會以更新方式覆蓋程式，不需要兩套並存；既有 Widget 設定使用相同 SharedPreferences 名稱，設計資料可延續。

## GitHub 自動產 APK

1. 將整個專案上傳至 GitHub repository，**務必包含 `.github/workflows/build-apk.yml`**。
2. GitHub → Actions → `Build PangWalletCalculator v2.1 APK`。
3. Run workflow → main。
4. 綠勾後進該次執行紀錄，下載 Artifacts：`PangWalletCalculator-v2.1-APK`。
5. ZIP 解壓後即為 `PangWalletCalculator-v2.1.apk`。

## 網路說明

26 合 1 主頁使用正式 GitHub Pages 網站，因此該區需要網路；桌面四則運算 Widget 本身不需要網路。
