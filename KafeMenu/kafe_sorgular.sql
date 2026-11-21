-- =========================================================
--  Kahve Durağı - MySQL'de her şeyi görmek için sorgular
--
--  MySQL Workbench: File > Open SQL Script... ile bu dosyayı açın,
--  şimşek simgesine basın. Her sorgunun sonucu altta ayrı bir
--  sekmede ("Result 1", "Result 2" ...) açılır.
--  Bu dosya hiçbir şeyi DEĞİŞTİRMEZ, sadece gösterir.
-- =========================================================

USE java_kursu;

-- 1) Menü (kategori sırasıyla)
SELECT u.urun_adi AS 'Ürün', u.kategori AS 'Kategori', u.fiyat AS 'Fiyat (TL)'
FROM menu_urunleri u
LEFT JOIN kategoriler k ON k.ad = u.kategori
ORDER BY COALESCE(k.sira, 999), u.kategori, u.urun_adi;

-- 2) Kategoriler
SELECT ad AS 'Kategori', sira AS 'Menüdeki sırası' FROM kategoriler ORDER BY sira, ad;

-- 3) Kutu oyunları ve minimum sepet tutarları
SELECT oyun_adi AS 'Kutu Oyunu', min_sepet_tutari AS 'Minimum Sepet (TL)'
FROM kutu_oyunlari ORDER BY min_sepet_tutari, oyun_adi;

-- 4) Siparişler (en yenisi üstte)
SELECT id AS 'Sipariş No', tarih AS 'Tarih', ara_toplam AS 'Ara Toplam', bahsis AS 'Bahşiş',
       toplam AS 'Toplam', kutu_oyunu AS 'Kutu Oyunu'
FROM siparisler ORDER BY id DESC;

-- 5) Her siparişte hangi üründen kaç adet alındı
SELECT siparis_id AS 'Sipariş No', urun_adi AS 'Ürün', adet AS 'Adet',
       birim_fiyat AS 'Birim Fiyat', tutar AS 'Tutar'
FROM siparis_kalemleri ORDER BY siparis_id DESC, id;

-- 6) Yapılan tüm işlemler: EKLEME, GÜNCELLEME, SİLME, İNDİRİM, ZAM, SİPARİŞ, KURULUM
SELECT tarih AS 'Tarih', islem AS 'İşlem', aciklama AS 'Açıklama'
FROM islem_gecmisi ORDER BY id DESC;

-- 7) Bonus: bugünkü ciro ve toplam bahşiş
SELECT COUNT(*) AS 'Sipariş Sayısı', COALESCE(SUM(toplam), 0) AS 'Ciro (TL)',
       COALESCE(SUM(bahsis), 0) AS 'Bahşiş (TL)'
FROM siparisler WHERE DATE(tarih) = CURDATE();
