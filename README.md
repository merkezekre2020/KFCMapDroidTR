# KFCMapDroidTR

MIT lisanslı, kök dizininde Android uygulaması bulunan bir repo.

## Ne yapıyor?

Bu uygulama:
- KFC Türkiye restoran sayfasını açar
- Sayfadaki `initialRestaurants` verisini ayrıştırır
- Şubeleri Android içindeki WebView + Leaflet haritasında gösterir
- Arama, liste ve mobil toggle sunar

## Yapı

- `app/` → Android uygulaması
- `app/src/main/assets/index.html` → Leaflet arayüzü
- `MainActivity.kt` → KFC verisini webden çekip WebView'a enjekte eder

## Çalıştırma

Android Studio ile aç:
1. `KFCMapDroidTR` klasörünü aç
2. Gradle sync yap
3. Emulator veya cihazda çalıştır

## Not

Harita verisi uygulama açıldığında canlı olarak `https://www.kfcturkiye.com/restoranlar` üzerinden çekilir.
