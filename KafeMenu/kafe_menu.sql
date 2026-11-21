-- =========================================================
--  Kahve Durağı - Kafe Menü Atölyesi
--
--  Bu dosyayı MySQL Workbench'te istediğiniz kadar çalıştırabilirsiniz
--  (şimşek simgesi / Ctrl+Shift+Enter). Kaç kez çalışırsa çalışsın
--  aynı ürün İKİNCİ KEZ EKLENMEZ.
--
--  NOT: Java programı da ilk açılışta aynı şeyleri kendisi yapar;
--  bu dosyayı çalıştırmak zorunlu değildir.
-- =========================================================

-- 1) java_kursu veritabanı (zaten varsa dokunulmaz)
CREATE DATABASE IF NOT EXISTS java_kursu
    CHARACTER SET utf8mb4 COLLATE utf8mb4_turkish_ci;

USE java_kursu;

-- 2) Tablo: menu_urunleri
--    id       : her ürünün benzersiz numarası, MySQL kendisi verir (1, 2, 3 ...)
--    urun_adi : ürünün adı; UNIQUE = aynı ad iki kez olamaz
--    kategori : ürünün grubu, ör. "Sıcak İçecekler"
--    fiyat    : TL cinsinden fiyat; DECIMAL(8,2) = virgülden sonra tam 2 basamak
CREATE TABLE IF NOT EXISTS menu_urunleri (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    urun_adi  VARCHAR(100)  NOT NULL,
    kategori  VARCHAR(50)   NOT NULL,
    fiyat     DECIMAL(8,2)  NOT NULL,
    UNIQUE KEY uk_urun_adi (urun_adi)
);

-- 3) Daha önce oluşmuş TEKRARLARI temizle:
--    her ürün adından yalnızca ilk eklenen (en küçük id) kalır.
SET SQL_SAFE_UPDATES = 0;   -- Workbench'in "güvenli silme" korumasını geçici olarak kapat
DELETE FROM menu_urunleri
WHERE id NOT IN (
    SELECT id FROM (SELECT MIN(id) AS id FROM menu_urunleri GROUP BY urun_adi) AS ilkler
);
SET SQL_SAFE_UPDATES = 1;   -- korumayı geri aç

-- 4) Eski tabloda benzersizlik kuralı yoksa ekle (varsa hiçbir şey yapma)
SET @kural_var = (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = 'java_kursu'
                    AND table_name   = 'menu_urunleri'
                    AND index_name   = 'uk_urun_adi');
SET @komut = IF(@kural_var = 0,
                'ALTER TABLE menu_urunleri ADD UNIQUE KEY uk_urun_adi (urun_adi)',
                'SELECT ''Benzersizlik kurali zaten var''');
PREPARE adim FROM @komut;
EXECUTE adim;
DEALLOCATE PREPARE adim;

-- 5) Başlangıç menüsü: INSERT IGNORE, tabloda zaten olan ürünü sessizce atlar
INSERT IGNORE INTO menu_urunleri (urun_adi, kategori, fiyat) VALUES
    ('Türk Kahvesi',        'Sıcak İçecekler',  85.00),
    ('Latte',               'Sıcak İçecekler', 120.00),
    ('Çay',                 'Sıcak İçecekler',  25.00),
    ('Limonata',            'Soğuk İçecekler',  90.00),
    ('Ice Americano',       'Soğuk İçecekler', 110.00),
    ('San Sebastian',       'Tatlılar',        180.00),
    ('Brownie',             'Tatlılar',        140.00),
    ('Kaşarlı Tost',        'Atıştırmalıklar', 130.00);

-- 6) Kontrol
SELECT * FROM menu_urunleri ORDER BY id;
