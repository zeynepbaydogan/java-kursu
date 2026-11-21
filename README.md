# Java Kursu

Java kursu çalışmalarım (Eclipse projeleri).

| Klasör | İçerik |
|---|---|
| `KafeMenu/` | **Kafe Menü Atölyesi** — "Kahve Durağı" kafesi için Swing + JDBC (MySQL) uygulaması. Çalıştırılacak sınıf: `JavaKursu.KafeMenuEkrani` |
| `test/` | Deneme projesi. `JavaKursu.OgrenciEkle`: `ogrenciler` tablosuna PreparedStatement ile ad/soyad ekleme örneği |

## Çalıştırma

1. Eclipse: **File → Import → General → Existing Projects into Workspace** ile klasörü seç.
2. MySQL 8 sunucusu `localhost:3306` adresinde çalışıyor olmalı.
3. Ana sınıfa sağ tıkla → **Run As → Java Application**.
4. MySQL şifresi program açılınca sorulur. Şifre kodun içinde **yoktur**.

MySQL JDBC sürücüsü (`mysql-connector-j-9.7.0.jar`) her projenin `lib/` klasöründedir.
