package JavaKursu;

import java.math.BigDecimal;

/**
 * Menüdeki tek bir ürünü temsil eder.
 * Alanlar, MySQL'deki menu_urunleri tablosunun sütunlarıyla birebir aynıdır.
 */
public class Urun {

    private final int id;              // tablodaki id sütunu
    private final String ad;           // urun_adi sütunu
    private final String kategori;     // kategori sütunu
    private final BigDecimal fiyat;    // fiyat sütunu (para için double yerine BigDecimal kullanılır)

    /** Veritabanından okunan, id'si belli olan ürün için. */
    public Urun(int id, String ad, String kategori, BigDecimal fiyat) {
        this.id = id;
        this.ad = ad;
        this.kategori = kategori;
        this.fiyat = fiyat;
    }

    /** Yeni eklenecek ürün için: id'yi MySQL kendisi verecek. */
    public Urun(String ad, String kategori, BigDecimal fiyat) {
        this(0, ad, kategori, fiyat);
    }

    public int getId() {
        return id;
    }

    public String getAd() {
        return ad;
    }

    public String getKategori() {
        return kategori;
    }

    public BigDecimal getFiyat() {
        return fiyat;
    }
}
