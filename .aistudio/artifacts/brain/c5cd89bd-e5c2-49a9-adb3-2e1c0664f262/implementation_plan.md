# Samsung Tizen OS (1920x1080) TV Tasarım ve Görünüm Şablon Planı

Bu plan, Samsung Smart TV (Tizen OS) üzerinde React ve Tailwind CSS ile geliştirilen IPTV uygulamasının ekran kaymalarını, taşmaları, orantısız kartları ve bozuk kumanda odaklama görünümlerini ortadan kaldırmak için **1920x1080 sabit çözünürlük mimarisi** ve hazır tasarım şablonlarını kapsar.

---

## 1. Temel TV Görüntü Standartları ve Reset CSS

Samsung TV web tarayıcı motorunda (Tizen Web Engine / Chromium tabanlı) taşmaları ve kaymaları önleyen katı kurallar:
- **Sabit Çözünürlük:** `width: 1920px; height: 1080px; overflow: hidden; position: fixed;`
- **Safe Zone (Güvenli Alan):** Eski veya overscan yapan TV panellerinde kenarlardan kesilmeyi önleyen `40px` iç boşluk (padding).
- **Kaydırma Çubukları (Scrollbars):** TV kumandasında scrollbar görünümü engellenmeli, içerik programatik olarak odaklanan karta doğru (`scrollIntoView({ behavior: 'smooth', block: 'nearest' })`) kaymalıdır.
- **İmleç ve Seçim:** `cursor: none; user-select: none;` kuralları ile mouse oku gizlenmeli ve metin seçimi engellenmelidir.

---

## 2. Tailwind TV Konfigürasyonu (`tailwind.config.js`)

Android uygulamasının birebir renk paleti ve TV ölçekleri için Tailwind konfigürasyonu:
- **Arka Planlar:**
  - Ana Zemin: `#0D0F15` (Derin Gece / True Dark)
  - Yüzey / Kart Zeminleri: `#141822` ve `#1C2230`
  - Ayırıcı ve Kenarlıklar: `#252D3D`
- **Vurgu (Primary) ve Odak:**
  - Marka Kırmızısı: `#F43F28`
  - Focus Ring: `border-4 border-[#F43F28]` + `shadow-[0_0_20px_rgba(244,63,40,0.6)]`
  - Focus Büyütme: `scale-105 transition-transform duration-200`
- **TV Tipografisi:**
  - TV mesafesinden okunabilirlik için minimum yazı boyutu 14px, başlıklar 20px - 28px.

---

## 3. Ekran ve Izgara (Grid) Oranları Şablonu

```
┌────────────────────────────────────────────────────────────────────────┐
│ ÜST TAB BAR (Yükseklik: 70px) - Logo + 5 Sekme + Arama + Menü          │
├───────────────────┬────────────────────────────────────────────────────┤
│ SOL KATEGORİ      │ SAĞ İÇERİK ALANI (Genişlik: 1540px)                │
│ PANELİ            │                                                    │
│ (Genişlik: 300px) │ • Canlı TV: 4 Kolonlu Grid (16:9 Kanal Kartları)   │
│                   │ • Film & Dizi: 6 Kolonlu Grid (2:3 Afişler)        │
│                   │ • Anasayfa: Vitrin Kartları & Yatay Sliderlar      │
└───────────────────┴────────────────────────────────────────────────────┘
```

1. **Üst Bar (Header):**
   - Genişlik: `100%` (1840px safe zone içinde), Yükseklik: `70px`.
   - Logo sol tarafta, 5 sekme ortada, arama ve profil sağ tarafta.
2. **Sol Kategori Paneli (Side Panel):**
   - Genişlik: Sabit `300px`, Yükseklik: `calc(1080px - 70px - 80px) = 930px`.
   - Dikey liste, her kategori butonu `height: 52px`, ikon + metin + kanal sayısı rozeti.
3. **Sağ İçerik Izgarası (Content Grid):**
   - Genişlik: Sabit `1540px`, Yükseklik: `930px`.
   - **Canlı TV Kartları:** `aspect-video` (16:9), 4 kolon (`grid-cols-4`, kart genişliği ~360px). Üzerinde kanal logosu, kanal adı ve canlı EPG bilgisi.
   - **Film & Dizi Afişleri:** `aspect-[2/3]` (Standart film afiş oranı), 6 kolon (`grid-cols-6`, afiş genişliği ~240px). Üzerinde TMDB afişi, IMDb puan rozeti ve başlık.

---

## 4. Kumanda Odak (Focus) Mekanizması ve CSS Sınıfları

TV kumandasının yön tuşlarıyla gezinirken görsel zenginlik ve kullanıcı geri bildirimi:
- Odaklanmayan Eleman: `opacity-80 scale-100 border border-transparent`
- **Odaklanan Eleman (Focused):**
  - `transform: scale(1.06)` (Hafif öne fırlama efekti)
  - `border: 3.5px solid #F43F28` (Canlı kırmızı çerçeve)
  - `box-shadow: 0 0 25px rgba(244, 63, 40, 0.55)` (Neon ışıma efekti)
  - `z-index: 50` (Yanındaki kartların üzerine taşarak net görünme)
  - `opacity-100`

---

## 5. Sağlanacak Kod ve Şablon Çıktıları

1. `index.css`: Tüm TV overscan, reset, scrollbar gizleme ve animasyon tanımları.
2. `tailwind.config.js`: IPTV TV renk ve gölge paleti.
3. `TVLayout.jsx`: 1920x1080 piksel bazlı, kaymayan ve bozulmayan ana React çerçevesi.
4. `LiveCard.jsx` & `VodCard.jsx`: 1:1 orantılı, focus durumlu kanal ve afiş bileşenleri.
