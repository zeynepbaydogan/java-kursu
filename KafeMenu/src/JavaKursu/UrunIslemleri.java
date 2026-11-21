package JavaKursu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Menü ürünleriyle ilgili tüm MySQL işlemleri:
 * listele (filtreli), ekle, güncelle, sil, indirim/zam.
 *
 * Hepsinde PreparedStatement kullanılır: değerlerin yerine '?' yazılır,
 * sonra setString / setBigDecimal / setInt ile doldurulur.
 * Her değişiklik islem_gecmisi tablosuna da kaydedilir.
 */
public class UrunIslemleri {

    private static final BigDecimal YUZ = new BigDecimal("100");

    private final Veritabani veritabani;

    public UrunIslemleri(Veritabani veritabani) {
        this.veritabani = veritabani;
    }

    /** Açılır listelerdeki kategoriler (menü sırasıyla). */
    public List<String> kategoriler() throws SQLException {
        List<String> liste = new ArrayList<>();
        try (Connection baglanti = veritabani.baglan();
             PreparedStatement komut = baglanti.prepareStatement("SELECT ad FROM kategoriler ORDER BY sira, ad");
             ResultSet sonuc = komut.executeQuery()) {
            while (sonuc.next()) {
                liste.add(sonuc.getString("ad"));
            }
        }
        return liste;
    }

    /** Tüm menü. */
    public List<Urun> listele() throws SQLException {
        return listele(null, null);
    }

    /**
     * Menüyü filtreleyerek getirir.
     *
     * @param kategori sadece bu kategori (null = hepsi)
     * @param arama    adında bu yazı geçen ürünler (boş = hepsi)
     */
    public List<Urun> listele(String kategori, String arama) throws SQLException {
        // SQL'i parça parça kuruyoruz; değerler yine '?' ile güvenli şekilde gönderiliyor
        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.urun_adi, u.kategori, u.fiyat FROM menu_urunleri u "
                        + "LEFT JOIN kategoriler k ON k.ad = u.kategori");
        List<String> kosullar = new ArrayList<>();
        List<String> degerler = new ArrayList<>();

        if (kategori != null) {
            kosullar.add("u.kategori = ?");
            degerler.add(kategori);
        }
        if (arama != null && !arama.isBlank()) {
            kosullar.add("u.urun_adi LIKE ?");
            degerler.add("%" + arama.trim() + "%");   // % = "önünde/arkasında ne olursa olsun"
        }
        if (!kosullar.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", kosullar));
        }
        // Menü sırası: önce kategori sırası, sonra ürün adı
        sql.append(" ORDER BY COALESCE(k.sira, 999), u.kategori, u.urun_adi");

        List<Urun> urunler = new ArrayList<>();
        try (Connection baglanti = veritabani.baglan();
             PreparedStatement komut = baglanti.prepareStatement(sql.toString())) {
            for (int i = 0; i < degerler.size(); i++) {
                komut.setString(i + 1, degerler.get(i));
            }
            try (ResultSet sonuc = komut.executeQuery()) {
                while (sonuc.next()) {
                    urunler.add(satirdanUrun(sonuc));
                }
            }
        }
        return urunler;
    }

    /** Yeni ürün ekler. Kategori listede yoksa listeye de eklenir. */
    public void ekle(Urun urun) throws SQLException {
        try (Connection baglanti = veritabani.baglan()) {
            kategoriyiKaydet(baglanti, urun.getKategori());

            try (PreparedStatement komut = baglanti.prepareStatement(
                    "INSERT INTO menu_urunleri (urun_adi, kategori, fiyat) VALUES (?, ?, ?)")) {
                komut.setString(1, urun.getAd());
                komut.setString(2, urun.getKategori());
                komut.setBigDecimal(3, urun.getFiyat());
                komut.executeUpdate();
            }
            Veritabani.gecmiseYaz(baglanti, "EKLEME",
                    urun.getAd() + " (" + urun.getKategori() + ") - " + Tema.tl(urun.getFiyat()));
        }
    }

    /** Ürünün adını, kategorisini ve fiyatını değiştirir. Ürün bulunduysa true döner. */
    public boolean guncelle(int id, String yeniAd, String yeniKategori, BigDecimal yeniFiyat) throws SQLException {
        try (Connection baglanti = veritabani.baglan()) {
            Urun eski = getir(baglanti, id);
            if (eski == null) {
                return false;
            }
            kategoriyiKaydet(baglanti, yeniKategori);

            try (PreparedStatement komut = baglanti.prepareStatement(
                    "UPDATE menu_urunleri SET urun_adi = ?, kategori = ?, fiyat = ? WHERE id = ?")) {
                komut.setString(1, yeniAd);
                komut.setString(2, yeniKategori);
                komut.setBigDecimal(3, yeniFiyat);
                komut.setInt(4, id);
                komut.executeUpdate();
            }
            Veritabani.gecmiseYaz(baglanti, "GÜNCELLEME",
                    eski.getAd() + " (" + eski.getKategori() + ", " + Tema.tl(eski.getFiyat()) + ")  ->  "
                            + yeniAd + " (" + yeniKategori + ", " + Tema.tl(yeniFiyat) + ")");
            return true;
        }
    }

    /** Ürünü siler. Ürün bulunduysa true döner. */
    public boolean sil(int id) throws SQLException {
        try (Connection baglanti = veritabani.baglan()) {
            Urun eski = getir(baglanti, id);
            if (eski == null) {
                return false;
            }
            try (PreparedStatement komut = baglanti.prepareStatement("DELETE FROM menu_urunleri WHERE id = ?")) {
                komut.setInt(1, id);
                komut.executeUpdate();
            }
            Veritabani.gecmiseYaz(baglanti, "SİLME",
                    eski.getAd() + " (" + eski.getKategori() + ", " + Tema.tl(eski.getFiyat()) + ")");
            return true;
        }
    }

    /**
     * Verilen ürünlere yüzde olarak indirim veya zam uygular.
     * Hepsi tek bir "işlem" (transaction) içinde yapılır: bir hata olursa hiçbiri değişmez.
     *
     * @return fiyatı değişen ürün sayısı
     */
    public int fiyatlariDegistir(boolean zam, BigDecimal yuzde, List<Urun> hedefler) throws SQLException {
        String islem = zam ? "ZAM" : "İNDİRİM";
        String yuzdeYazisi = "%" + yuzde.stripTrailingZeros().toPlainString();
        int degisen = 0;

        try (Connection baglanti = veritabani.baglan()) {
            baglanti.setAutoCommit(false); // değişiklikleri biriktir, en sonda birlikte kaydet
            try (PreparedStatement komut = baglanti.prepareStatement(
                    "UPDATE menu_urunleri SET fiyat = ? WHERE id = ?")) {
                for (Urun hedef : hedefler) {
                    Urun guncel = getir(baglanti, hedef.getId()); // en güncel fiyatı oku
                    if (guncel == null) {
                        continue;
                    }
                    BigDecimal yeni = yeniFiyat(guncel.getFiyat(), zam, yuzde);
                    komut.setBigDecimal(1, yeni);
                    komut.setInt(2, guncel.getId());
                    komut.executeUpdate();
                    Veritabani.gecmiseYaz(baglanti, islem, guncel.getAd() + ": "
                            + Tema.tl(guncel.getFiyat()) + "  ->  " + Tema.tl(yeni) + " (" + yuzdeYazisi + ")");
                    degisen++;
                }
                baglanti.commit();   // hepsini kalıcı yap
            } catch (SQLException e) {
                baglanti.rollback(); // hata olursa hiçbir fiyat değişmesin
                throw e;
            }
        }
        return degisen;
    }

    /** İndirim/zam sonrası fiyat: 120 TL'ye %10 zam -> 132 TL. En az 0,01 TL. */
    public static BigDecimal yeniFiyat(BigDecimal eskiFiyat, boolean zam, BigDecimal yuzde) {
        BigDecimal carpan = zam ? YUZ.add(yuzde) : YUZ.subtract(yuzde);
        BigDecimal yeni = eskiFiyat.multiply(carpan).divide(YUZ, 2, RoundingMode.HALF_UP);
        return yeni.max(new BigDecimal("0.01"));
    }

    // ---- yardımcılar ----

    private static Urun getir(Connection baglanti, int id) throws SQLException {
        try (PreparedStatement komut = baglanti.prepareStatement(
                "SELECT id, urun_adi, kategori, fiyat FROM menu_urunleri WHERE id = ?")) {
            komut.setInt(1, id);
            try (ResultSet sonuc = komut.executeQuery()) {
                return sonuc.next() ? satirdanUrun(sonuc) : null;
            }
        }
    }

    /** Kategori listede yoksa ekler (INSERT IGNORE: varsa sessizce atlar). */
    private static void kategoriyiKaydet(Connection baglanti, String kategori) throws SQLException {
        try (PreparedStatement komut = baglanti.prepareStatement("INSERT IGNORE INTO kategoriler (ad) VALUES (?)")) {
            komut.setString(1, kategori);
            komut.executeUpdate();
        }
    }

    private static Urun satirdanUrun(ResultSet sonuc) throws SQLException {
        return new Urun(sonuc.getInt("id"), sonuc.getString("urun_adi"),
                sonuc.getString("kategori"), sonuc.getBigDecimal("fiyat"));
    }
}
