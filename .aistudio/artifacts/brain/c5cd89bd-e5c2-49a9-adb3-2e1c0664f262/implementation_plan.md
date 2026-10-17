# Samsung Tizen OS (React Web App) - Renkler, Intro ve Kumanda Entegrasyon Planı

Bu plan, Android projesindeki (**Maxx Player**) tüm tasarım öğelerini, intro video geçiş mekanizmasını ve kumanda odak (spatial navigation) sistemini Samsung TV (Tizen OS) React Web App platformuna 1:1 kopyalamak için hazırlanmıştır.

---

## 1. UI Renk Paleti ve Tasarım Tokenleri (1:1 Android Eşleşmesi)

Android uygulamasındaki `Color.kt`, `Theme.kt`, `DashboardScreen.kt` ve `AdSliderBanner.kt` kaynaklı kesin renk kodları ve kullanım alanları:

### Temel Renk Kodları (Hex & CSS)
| İsim | Hex Kodu | Kullanım Alanı | CSS Değişkeni |
| :--- | :--- | :--- | :--- |
| **RedPrimary** | `#E50914` | Marka ana kırmızısı (Netflix kırmızısı), aktif butonlar, seçim kenarlıkları, kategori vurguları | `--primary-red: #E50914;` |
| **DarkBackground** | `#0A0A0A` | Tam ekran ana zemin, en koyu arka plan | `--bg-main: #0A0A0A;` |
| **DarkSurface** | `#141414` | Sol kategori barı, kart arka planları, dialog ve pop-up zeminleri | `--surface-dark: #141414;` |
| **DarkSurfaceVariant** | `#262626` | Pasif kartlar, ayırıcı çizgiler, chip butonlar | `--surface-variant: #262626;` |
| **TextWhite** | `#FFFFFF` | Ana başlıklar, seçili kanal isimleri, yüksek kontrast metinler | `--text-primary: #FFFFFF;` |
| **TextGray** | `#B3B3B3` | İkincil metinler, kanal açıklamaları, EPG saatleri | `--text-secondary: #B3B3B3;` |
| **TextMuted** | `#757575` | Pasif ikonlar, versiyon bilgisi, etiketler | `--text-muted: #757575;` |
| **AccentBlue** | `#0071EB` | Bilgi bildirimleri, ikincil canlı yayın vurguları | `--accent-blue: #0071EB;` |

### Reklam ve Tanıtım Slayder Renkleri (AdSliderBanner)
- **VIP / 4K Banner:** Birincil `#E50914`, İkincil `#141926`
- **Spor / Maç Banner:** Birincil `#1E88E5`, İkincil `#0A192F`
- **Film & Dizi Banner:** Birincil `#8E24AA`, İkincil `#180A26`
- **Web Reklam / Sponsor:** Birincil `#FB8C00`, İkincil `#211510`

### TV Odak (Focus Ring) Stili
Samsung TV kumandasıyla odaklanan her eleman için standart odak görseli:
```css
:root {
  --focus-border: 3px solid #E50914;
  --focus-glow: 0 0 16px rgba(229, 9, 20, 0.7);
  --focus-scale: scale(1.04);
}

.focusable:focus,
.focusable.focused {
  outline: none;
  border: var(--focus-border) !important;
  box-shadow: var(--focus-glow) !important;
  transform: var(--focus-scale);
  transition: transform 0.18s ease-out, box-shadow 0.18s ease-out;
  z-index: 10;
}
```

---

## 2. Intro / Splash Ekranı Entegrasyonu

Android projesinde `SplashScreen.kt` dosyasında yer alan intro mantığı Tizen'e şu kurallarla taşınır:

### Video Dosyası & Format
- **Dosya:** Android'deki `app/src/main/res/raw/intro5.mp4` (1.3 MB, 1080p H.264 / AAC stereo). Tizen 2017+ tüm Samsung TV modelleriyle %100 yerel donanımsal uyumludur.
- **Konum:** React projesinde `public/assets/intro5.mp4`.

### İşleyiş & Kurallar
1. **Otomatik Başlama (Autoplay):** Uygulama ilk yüklendiğinde sesli/sessiz olarak doğrudan tam ekran `<video autoplay playsinline muted={false}>` etiketiyle başlar.
2. **Atlama (Skip) İmkanı:** Kullanıcı kumandadan herhangi bir tuşa (`Enter`, `Return / Back`, `Space`) bastığında intro derhal sonlandırılır.
3. **Zaman Aşımı Koruması (Watchdog):** Video bittiğinde (`onEnded`) veya en fazla 10 saniye sonra (`setTimeout`) otomatik olarak Ana Menüye / Dashboard'a yönlendirilir.
4. **Yedek (Fallback) Logo:** Video oynatılamazsa veya hata verirse (`onError`), uygulamanın logo animasyonu (nabız / pulse scale 0.95 ↔ 1.05) ve yükleme göstergesi devreye girer.

---

## 3. Samsung TV Kumanda (Remote Control) Yönetimi

Samsung Tizen TV'lerde kumanda tuşlarının yakalanması ve odaklanma (Spatial Navigation) mekanizması:

### Tizen Özel Tuş Kodları (KeyCodes)
| Tuş Adı | JavaScript `keyCode` / `key` | Görevi |
| :--- | :--- | :--- |
| **Yukarı (Up)** | `38` (`ArrowUp`) | Liste/Menü yukarı hareket |
| **Aşağı (Down)** | `40` (`ArrowDown`) | Liste/Menü aşağı hareket |
| **Sol (Left)** | `37` (`ArrowLeft`) | Kategorilere geçiş / Geri |
| **Sağ (Right)** | `39` (`ArrowRight`) | Kanal listesine geçiş / İleri |
| **Seç / Tamam (Enter)** | `13` (`Enter`) | Kanalı açma, seçim yapma |
| **Geri (Back / Return)** | `10009` | Önceki menüye dönme / Tam ekran oynatıcıdan çıkış |
| **Play (Oynat)** | `415` | Yayını başlat |
| **Pause (Duraklat)** | `19` | Yayını duraklat |
| **Stop (Durdur)** | `413` | Yayını durdur |
| **İleri Sar (FF)** | `417` | Film/Dizide 10 sn ileri |
| **Geri Sar (Rewind)** | `412` | Film/Dizide 10 sn geri |

### Tizen `config.xml` Zorunlu İzinleri
Samsung Tizen TV kumandasının geri ve medya tuşlarını dinleyebilmek için `config.xml` dosyasına eklenmesi gereken bildirimler:
```xml
<tizen:privilege name="http://tizen.org/privilege/tv.inputdevice"/>
<tizen:privilege name="http://developer.samsung.com/privilege/avplay"/>
<tizen:privilege name="http://developer.samsung.com/privilege/network.public"/>
```

### Kumanda Medya Tuşlarını Tizen'e Kaydetme Kodu
```javascript
if (window.tizen && window.tizen.tvinputdevice) {
  const supportedKeys = [
    'MediaPlay', 'MediaPause', 'MediaPlayPause', 
    'MediaStop', 'MediaFastForward', 'MediaRewind',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
  ];
  supportedKeys.forEach(keyName => {
    try {
      window.tizen.tvinputdevice.registerKey(keyName);
    } catch (e) {
      console.warn('Tuş kaydedilemedi:', keyName, e);
    }
  });
}
```

---

## 4. Video Oynatıcı Kuralları (Samsung AVPlay)

Android'deki ExoPlayer işleyişi Tizen AVPlay (`webapis.avplay`) ile birebir eşlenir:
1. **Canlı TV ve Filmlerde Otomatik Sonrakine Geçiş YOKTUR:** Canlı yayın akışı koptuğunda veya film bittiğinde sonraki videoya geçmez; oynatıcı durur ve kullanıcıya bilgi verir.
2. **Yalnızca Dizilerde Otomatik Sonrakine Geçiş VARDIR:** Dizi bölümü bittiğinde bir sonraki bölüme otomatik geçiş yapılır.
3. **Ekran Uyumu:** `webapis.avplay.setDisplayMethod('PLAYER_DISPLAY_MODE_AUTO_ASPECT_RATIO')` ile tam ekran 16:9 yayınlar ölçeklendirilir.

---

## Doğrulama ve Teslim Adımları
1. Hazırlanan renk tokenlerinin, CSS tanımlarının ve kumanda tuş dinleyicilerinin eksiksiz doğrulanması.
2. Kullanıcının React Tizen projesinde doğrudan kopyalayıp kullanabileceği yapı taşlarının hazır hale getirilmesi.
