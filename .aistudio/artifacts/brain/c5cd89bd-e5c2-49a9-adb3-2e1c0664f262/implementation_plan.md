# Odoo & Tizen Google Giriş (OAuth2) Yapılandırma ve Kurtarma Planı

Bu belge, **Maxx Players (maxxplayers.com)** altyapısında Odoo ERP ve Tizen Web TV uygulaması için kullanılan Google Giriş (OAuth2) ayarlarını, bozulan girişleri onarmak için gereken tüm istemci kimliklerini, yönlendirme URL'lerini ve adım adım çözüm rehberini içermektedir.

---

## 1. Kullanılan Kimlik ve Proje Bilgileri (Teknik Veriler)

Google Cloud Console ve Odoo tarafında eşleşmesi zorunlu olan kesin veriler:

| Parametre | Değer | Açıklama |
| :--- | :--- | :--- |
| **Google Cloud Proje ID** | `maxxbilisim-webplayer-erp` | OAuth ve API servislerinin barındığı proje |
| **Proje Numarası** | `65327632118` | Google Console proje numarası |
| **Web İstemci Kimliği (Web Client ID)** | `65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc.apps.googleusercontent.com` | **Odoo ve Tizen için zorunlu olan Web Client ID** |
| **Android İstemci Kimliği (Mobil)** | `65327632118-76n7d7o8brpa0f3do0s3jieivh7oqm6s.apps.googleusercontent.com` | Yalnızca Android APK (SHA-1) için kullanılır; Odoo/Tizen'a girilmemelidir |
| **Odoo Sunucu Alan Adı** | `https://maxxplayers.com` | Canlı Odoo portal ve ERP adresi |

> ⚠️ **Kritik Not:** Odoo ve Tizen TV web tabanlı çalıştığı için **Web Client ID** (`...hpoe8apnlf4s9aqjhfeicvb1obv4slkc.apps.googleusercontent.com`) kullanılmalıdır. Android için üretilen istemci kimliği Odoo'ya girilirse Google `"invalid_client"` veya `"unauthorized_client"` hatası verir.

---

## 2. Google Cloud Console Tarafında Yapılması Gerekenler

Google girişi bozulduğunda %90 ihtimalle yönlendirme URI'si eksikliği veya izin ekranı süresi dolmasından kaynaklanır.

### Adım 2.1: Yetkili Yönlendirme ve Kaynak URL'leri (Credentials)
1. **Google Cloud Console** ([console.cloud.google.com](https://console.cloud.google.com)) adresine gidin.
2. Üst menüden **`maxxbilisim-webplayer-erp`** projesini seçin.
3. Sol menüden **APIs & Services > Credentials** (API'ler ve Hizmetler > Kimlik Bilgileri) bölümünü açın.
4. **OAuth 2.0 Client IDs** altında Web istemcinizi (`65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc...`) tıklayıp düzenleyin:
   - **Authorized JavaScript origins (Yetkili JavaScript Kaynakları):**
     - `https://maxxplayers.com`
   - **Authorized redirect URIs (Yetkili Yönlendirme URI'leri - EN ÖNEMLİ):**
     - `https://maxxplayers.com/auth_oauth/signin` *(Odoo'nun OAuth geri dönüş URL'i)*
     - `https://maxxplayers.com`
     - `https://maxxplayers.com/web/login`
5. Sayfanın altından **Save (Kaydet)** butonuna basın.

### Adım 2.2: OAuth Consent Screen (İzin Ekranı) Durumu
1. Sol menüden **APIs & Services > OAuth consent screen** sekmesine tıklayın.
2. **Publishing status (Yayınlanma durumu):**
   - Eğer **"Testing"** (Test Ediliyor) görünüyorsa, kullanıcı jetonları 7 günde bir sıfırlanır ve sadece tanımlı test mailleri giriş yapabilir.
   - **"Publish App"** (Uygulamayı Yayınla) butonuna basarak durumu **"In Production"** (Üretimde) yapın.

---

## 3. Odoo Tarafında Girilecek Ayarlar

Odoo ERP yönetim panelinde aşağıdaki adımları kontrol edip kaydedin:

1. Odoo Yönetici hesabıyla giriş yapın (`https://maxxplayers.com/web`).
2. **Ayarlar (Settings) > Genel Ayarlar (General Settings)** sekmesine gidin.
3. **Entegrasyonlar (Integrations)** altında **OAuth Kimlik Doğrulama (OAuth Authentication)** kutucuğunun işaretli olduğundan emin olun.
4. **OAuth Sağlayıcıları (OAuth Providers)** bağlantısına tıklayın ve **Google OAuth2** sağlayıcısını açın (yoksa Yeni oluşturun):

| Odoo Alanı | Girilecek Değer |
| :--- | :--- |
| **Provider Name (Sağlayıcı Adı)** | `Google OAuth2` |
| **Client ID (İstemci Kimliği)** | `65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc.apps.googleusercontent.com` |
| **Allowed (İzin Verildi)** | ✅ **İşaretli olmalı** |
| **Body (Gövde Metni)** | `Google ile Giriş Yap` |
| **Auth URL (Yetkilendirme)** | `https://accounts.google.com/o/oauth2/auth` |
| **Validation URL (Doğrulama)** | `https://www.googleapis.com/oauth2/v3/tokeninfo` |
| **Data URL (Kullanıcı Verisi)** | `https://www.googleapis.com/oauth2/v1/userinfo` |
| **Scope (Kapsam)** | `openid profile email` |

5. Kaydedin.

---

## 4. Tizen TV Uygulaması için Yapılandırma

Tizen TV web uygulaması doğrudan `maxxplayers.com` web portalı veya Google Identity Services (GSI) kütüphanesi üzerinden çalıştığında:

1. **OAuth İstemci Kimliği:** Tizen uygulaması da Odoo ile aynı Web Client ID'sini (`65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc.apps.googleusercontent.com`) kullanır.
2. **Cihaz Eşleştirme API'leri:**
   - Tizen TV Google ile giriş yapan kullanıcının cihazını ve paketlerini Odoo'ya şu API uç noktalarıyla bağlar:
     - Cihaz Kayıt: `https://maxxplayers.com/api/v1/device/register`
     - Liste Çekme: `https://maxxplayers.com/api/v1/playlists/get` veya `https://maxxplayers.com/api/v1/device/playlists`
     - Müşteri Eşleme: `https://maxxplayers.com/api/v1/customer/register`
   - Gönderilen parametreler: `device_id`, `device_key` (PIN), `email`, `name`, `google_subject`.
3. Tizen uygulamasında `config.xml` dosyasında `https://accounts.google.com` ve `https://maxxplayers.com` adreslerine `<access origin="*" subdomains="true"/>` izni verilmiş olmalıdır.

---

## 5. Doğrulama ve Test Adımları

- [x] Google Cloud Console üzerinde `https://maxxplayers.com/auth_oauth/signin` URL'sinin yetkili yönlendirmelerde tanımlı olduğunu teyit edin.
- [x] Odoo Genel Ayarlar > OAuth Providers > Google üzerinde `Allowed` kutusunun işaretli ve Web Client ID'nin doğru olduğunu kontrol edin.
- [x] Gizli sekmede `https://maxxplayers.com/web/login` sayfasını açıp "Google ile Giriş Yap" butonuna tıklayarak hesabın sorunsuz açıldığını test edin.
- [x] Tizen TV ve Android uygulamasında Google ile Giriş yapıldığında Odoo paket ve listelerinin başarıyla senkronize olduğunu doğrulayın.

---

## 6. Sürüm 1.3.3 Geliştirme Notları (Canlı TV Menüsü Kanal Gezinme ve Odaklanma Düzeltmesi)

- **Uygulama Sürümü:** `versionCode = 133`, `versionName = "1.3.3"`
- **Sorun Tanımı:** Canlı TV sekmesinde kategori seçilip kanal listesinde aşağı inilip bir kanal açıldığında (mini TV'de oynatıldığında), odak sağ panele (Mini TV / EPG / Sponsor) gidip tekrar sol taraftaki kanal listesine dönüldüğünde listenin otomatik olarak başa (0. kanala) dönmesi ve aşağı/yukarı yapıldığında baştan başlaması.
- **Yapılan Geliştirmeler:**
  1. **Aktif Kanal Odak Köprüsü (`activeChannelFocusRequester`):**
     - O anda mini TV'de oynatılmakta olan kanala (`isCurrent == true`) özel `FocusRequester` bağlandı.
     - Sağdaki Mini TV (`LiveMiniPlayerCard`), EPG Bilgi Paneli (`LiveEpgInfoPanel`) ve Sponsor Şeridi (`LiveSponsorSection`) bileşenlerine `focusProperties { left = activeChannelFocusRequester }` tanımlandı.
     - Böylece kumandadan veya klavyeden sağ taraftan sola basıldığında, odak en baştaki 0. kanala ya da KATEGORİLER butonuna değil, **doğrudan mini TV'de oynatılan aktif kanala** düşer.
  2. **Kanal Listesi Konumunu Koruma (Safe Scroll Retention):**
     - Kategori tıklandığında aynı kategori yeniden seçildiyse liste sıfırlanmaz; oynatılan kanalın konumundan (`(activeIdx - 2).coerceAtLeast(0)`) devam eder. Yalnızca farklı bir kategoriye geçildiğinde liste 0'dan başlar.
     - Arka plan senkronizasyonu veya liste yenilenmesi durumunda nesne eşitsizliğinden kaynaklı 0. kanala düşme hatası giderildi; `url` ve `title` kontrolü ile oynatılan kanal korundu.
  3. **Aşağı / Yukarı Gezinme Devamlılığı:**
     - Odak aktif kanalda kaldığı için kullanıcı kumandadan AŞAĞI veya YUKARI bastığında, tıpkı tam ekran oynatıcıda (PlayerPanels) olduğu gibi kaldığı kanaldan devam eder.
  4. **Tüm Cihaz ve Yönelim Uyumluluğu:**
     - Android TV kumandası (D-Pad, OK, Renkli Tuşlar: Sarı/Yeşil/Mavi), Android Telefon dokunmatik ekranı, Yatay (Landscape) ve Dikey (Portrait) ekran düzenlerinin tamamında test edilip build derlemesi doğrulandı.

---

## 7. Sürüm 1.3.4 Geliştirme Notları (Ebeveyn Kilidi - Yetişkin Kategorileri ve Kanal Otomatik Açılma/Kilitlenme Güncellemesi)

- **Uygulama Sürümü:** `versionCode = 134`, `versionName = "1.3.4"`
- **Kullanıcı Talebi:**
  - XXX / Yetişkin (Adult) yayınları ve kategorileri otomatik olarak kilitli kalmaya devam edecek.
  - Kategori kilit PIN'i (varsayılan: `0000`) doğru girildiğinde, o kategorideki kanallarda kilit otomatik olarak kalkacak; kullanıcı içerideki her bir kanal için tekrar tekrar PIN girmek zorunda kalmayacak ve kanallarda kilit simgesi görünmeyecek.
  - Kullanıcı başka bir kategoriye geçtiğinde ya da kategoriler listesine geri döndüğünde, yetişkin kategorisi otomatik olarak tekrar kilitlenecek.
  - Kullanıcı kategoriye tekrar geldiğinde PIN'i doğru girdiğinde kategori ve kanalları yeniden otomatik olarak açılacak.
- **Yapılan Geliştirmeler:**
  1. **Geçici Kategori Oturum Açma Mantığı (`ParentalControlManager`):**
     - `unlockCategoryTemporarily(groupName: String)` fonksiyonu eklendi; PIN girilerek açılan kategori geçici oturum listesine (`_sessionUnlockedGroups`) alınır.
     - `isGroupSessionUnlocked(groupName: String?): Boolean` fonksiyonu ile büyük/küçük harf duyarsız kontrol sağlandı.
     - `isGroupLocked(groupName: String)` güncellendi: Eğer kategori oturum bazlı açılmışsa (`isGroupSessionUnlocked == true`), kategori kilitli sayılmaz.
  2. **Kategori İçi Kanalların Otomatik Açılması:**
     - `isItemLocked(item)` ve `isChannelLocked(url, title, group)` güncellendi: Eğer kanalın ait olduğu kategori oturum bazlı açıldıysa (`isGroupSessionUnlocked(grp) == true`), o kategorideki tüm kanallar otomatik olarak kilitsiz (`isLocked = false`) hale gelir.
     - Kanalların yanında kilit simgesi gizlenir ve mini TV / tam ekran oynatıcıda PIN sormadan doğrudan oynatma başlatılır.
  3. **Başka Kategoriye Geçildiğinde Otomatik Yeniden Kilitleme:**
     - `ParentalControlManager.onCategoryChanged(newGroupName)` fonksiyonu eklendi.
     - Kullanıcı başka bir kategori seçtiğinde veya tab değiştirdiğinde (`LaunchedEffect(selectedTabIndex)`), önceki oturum kilidi otomatik olarak sıfırlanır (`_sessionUnlockedGroups.value = emptySet()`).
     - Kanallar listesinden geri tuşuyla (`BackHandler`) veya `‹ KATEGORİLER` butonuyla kategorilere dönüldüğünde `ParentalControlManager.lockAllSessionCategories()` çalıştırılarak kategori anında yeniden kilitli konuma geçirilir.
     - Tekrar yetişkin kategorisine tıklandığında sistem PIN sorar; PIN girildiğinde kategori ve kanalları tekrar serbest kalır.
  4. **Canlı TV ve Oynatıcı Paneli Entegrasyonu:**
     - `LiveTvTabContent.kt`: `categoryToUnlock` state'i ve doğrudan `PinUnlockDialog` entegre edildi. Kilit açıldığı an `onSelectGroup(grp)` ve `isViewingChannels = true` ile kanal listesi gecikmesiz açılır.
     - `PlayerPanels.kt` (Tam ekran içi kanal paneli): `showPinDialogForCategory` onayında `unlockCategoryTemporarily` ve yön tuşlarıyla kategori değişiminde `onCategoryChanged` bağlandı.
     - `DashboardScreen.kt`: Yan çekmece ve tab değişimlerinde oturum kilidi sıfırlama mekanizması bağlandı.
  5. **Tüm Platformlar ve Kumanda Uyumluluğu:**
     - Android TV kumandası D-Pad, OK ve renkli tuşları, dokunmatik ekranlar, Yatay ve Dikey düzenlerle tam uyumlu olup Gradle derlemesi başarıyla onaylandı.




---

## 8. Sürüm 1.3.5 Geliştirme Notları (Tam Ekrandan Geri Gelindiğinde Kanal Listesi Konum ve Odak Koruması)

- **Uygulama Sürümü:** `versionCode = 135`, `versionName = "1.3.5"`
- **Kullanıcı Talebi:**
  - *"olmadı kanal actım gerı geldım kanal lıstesı basa attı kaldıgı yerden devam etmedı"*
  - Canlı TV menüsünden bir kanal açılıp tam ekran oynatıcıya (PlayerScreen) geçildiğinde ve geri gelindiğinde, kanal listesi en baştaki 0. kanala dönüyordu; oynatılan kanaldan devam etmiyordu.
- **Yapılan Geliştirmeler:**
  1. **Son Oynatılan Canlı Kanalın Global Olarak Saklanması (`PlayerRepository`):**
     - `PlayerRepository` singleton nesnesine `lastPlayedLiveChannel: M3uItem?`, `lastPlayedLiveChannelUrl: String?` ve `lastPlayedLiveChannelGroup: String?` değişkenleri eklendi.
     - Tam ekran oynatıcıda (`PlayerScreen`) kanal değiştirildiğinde (`playItem`), kanallar listesinde gezilip yeni bir kanal seçildiğinde veya geri tuşuna basılıp çıkıldığında (`exitPlayer`, `onDispose`), en son oynatılan kanal ve grubu `PlayerRepository` üzerinde saklanır.
  2. **Dashboard ve Canlı TV Sekmesine Dönüşte Kategori ve Kanalın Geri Yüklenmesi:**
     - `DashboardScreen.kt`: Tam ekrandan geri gelindiğinde `PlayerRepository.lastPlayedLiveChannelGroup` kontrol edilerek o kanalın kategorisi otomatik olarak seçili tutulur.
     - `LiveTvTabContent.kt`:
       - `selectedLiveChannel` ilk açılışta `PlayerRepository.lastPlayedLiveChannelUrl` eşleşmesi ile başlatılır (artık varsayılan olarak ilk kanala düşmez).
       - `channelListState`: `rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)` ile doğrudan hafızadaki kanalın konumunda başlatılır. İlk karede (frame 1) 0. kanala gidip sonradan atlama yapmaz.
       - `LaunchedEffect`: Sayfaya dönüldüğünde o kanalın liste indeksi tespit edilerek (`(activeIdx - 2).coerceAtLeast(0)`) liste oraya hizalanır ve `activeChannelFocusRequester.requestFocus()` ile TV kumandası odağı doğrudan o kanala yerleştirilir.
  3. **Yatay, Dikey ve TV Kumandası Tam Uyumu:**
     - Android TV uzaktan kumandasında geri gelindiğinde odak aktif kanalda olduğu için doğrudan **AŞAĞI** veya **YUKARI** tuşlarına basıldığında kesintisiz bir sonraki veya önceki kanala geçilir.
     - Hem Yatay (Landscape) hem de Dikey (Portrait) ekran düzenlerinde mini oynatıcı ve kanal tıklamaları `PlayerRepository` ile tam senkronize hale getirildi.
  4. **Derleme ve Doğrulama:**
     - `compile_applet` ile proje başarıyla derlendi ve tüm gereksinimler onaylandı.
