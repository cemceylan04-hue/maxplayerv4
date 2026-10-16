# Google Giriş Entegrasyonunu Sorunsuz Hale Getirme Planı

Kullanıcı fiziksel Android telefona APK yükleyerek test etmekte ve "Cihazda kayıtlı Google hesabı bulunamadı" uyarısı almaktadır.
Telefonunuzda aktif Google hesabı bulunmasına rağmen bu hatanın alınmasının temel nedeni şudur:
Android Credential Manager (Google Kimlik Yöneticisi), yüklenen APK'nın imza parmak izi (SHA-1) ile Google Cloud Console'da kayıtlı SHA-1 uyuşmadığında veya `serverClientId` yetkilendirmesi başarısız olduğunda güvenlik sebebiyle `NoCredentialException` döndürür ve hesap listesini ekrana getirmez.

---

## 1. Tespit Edilen Kök Nedenler

1. **SHA-1 Uyuşmazlığı (Debug APK vs Google Cloud Console):**
   * Google Cloud Console'da kayıtlı SHA-1: `E0:7C:98:ED:18:6D:F2:ED:04:24:42:C6:1D:09:93:0B:39:41:71:99`
   * Telefona yüklenen Debug APK'yı imzalayan anahtar (`debug.keystore`) SHA-1:
     `13:5A:43:9A:DC:14:CC:02:81:B5:39:8D:56:09:58:51:57:0B:1A:F6`
   * Google Play Services, bu SHA-1 konsolda ekli olmadığı için oturum açma isteğini reddetmektedir.

2. **Web Client ID vs Android Client ID (Credential Manager Kuralı):**
   * Android Credential Manager `GetGoogleIdOption.Builder().setServerClientId(...)` fonksiyonuna gönderilen kimliğin türü Google standartlarına göre **Web Application (Web Uygulaması)** olmak zorundadır.
   * `65327632118-76n7d7o8brpa0f3do0s3jieivh7oqm6s` bir **Android (Installed)** istemcisidir; aynı projenin Web İstemcisi olan `65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc` ile kimlik belirteci (ID Token) alınması gerekir.

3. **Hata Mesajı Yönlendirmesi:**
   * Hata yakalama bloğu `NoCredentialException` durumunda yalnızca genel "Cihazda hesap bulunamadı" yazmak yerine, geliştiriciye SHA-1 veya OAuth durumunu açıkça raporlayan bir log/açıklama sunmalıdır.

---

## 2. Yapılacak Adımlar ve İyileştirmeler

### Adım 1: Kullanıcının Google Cloud Console'a Debug SHA-1 Eklenmesi
* Google Cloud Console > **APIs & Services > Credentials (Kimlik Bilgileri)** sekmesinde:
  * Mevcut `maxxplayer` Android OAuth istemcisine ikinci bir SHA-1 olarak veya yeni bir Android OAuth İstemcisi olarak şu parmak izi eklenmelidir:
    * **Paket Adı:** `com.maxxplayers.tvs`
    * **SHA-1:** `13:5A:43:9A:DC:14:CC:02:81:B5:39:8D:56:09:58:51:57:0B:1A:F6`
    * **SHA-256:** `B0:7C:EB:A1:52:63:62:5B:2F:CA:E0:43:61:F3:96:CE:9E:4B:AC:79:83:37:EB:D3:5F:ED:25:FF:62:DE:0F:E1`

### Adım 2: `GoogleAuthManager` Kodunun Optimize Edilmesi
* `GoogleAuthManager.kt` dosyası güncellenerek:
  * Önce doğrudan Web Client ID (`65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc`) ile `GetGoogleIdOption` ve `GetSignInWithGoogleOption` kombinasyonu çağrılacak.
  * Eğer `NoCredentialException` veya `GetCredentialException` alınırsa, gerçek hata mesajı (örneğin SHA-1 uyuşmazlığı veya Play Services durumu) ayrıntılı olarak loglanacak ve kullanıcı ekranında net bir rehber gösterilecek.
  * Test ortamında kullanıcıyı kilitlememek adına, Google girişi başarısız olduğunda doğrudan e-posta ile giriş veya tek tıkla test kullanıcısı olarak devam edebilme seçeneği (Bypass/Manuel opsiyonu) eklenecektir.

### Adım 3: Release İmzalamalı APK Seçeneği
* Kullanıcının Google Cloud Console'da tanımladığı `E0:7C:98:ED...` SHA-1'ine sahip imza anahtarıyla release APK derlenebilmesi için `app/build.gradle.kts` ve release betiği hazırlanacak.

---

## 3. Doğrulama ve Test
1. Kod değişikliklerinin derlenmesi (`compile_applet`).
2. Kullanıcının Google Cloud Console'a SHA-1 eklemesinden sonra telefonda Google hesap seçici penceresinin açıldığının ve Odoo senkronizasyonunun başarıyla tamamlandığının doğrulanması.
