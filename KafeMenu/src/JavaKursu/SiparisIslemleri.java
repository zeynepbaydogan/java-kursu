package JavaKursu;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Sipariş, kutu oyunu ve geçmiş ile ilgili MySQL işlemleri.
 */
public class SiparisIslemleri {

    private static final DateTimeFormatter TARIH = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Veritabani veritabani;

    public SiparisIslemleri(Veritabani veritabani) {
        this.veritabani = veritabani;
    }

    /** Kutu oyunları, minimum sepet tutarına göre sıralı. */
    public List<KutuOyunu> kutuOyunlari() throws SQLException {
        List<KutuOyunu> liste = new ArrayList<>();
        try (Connection baglanti = veritabani.baglan();
             PreparedStatement komut = baglanti.prepareStatement(
                     "SELECT id, oyun_adi, min_sepet_tutari FROM kutu_oyunlari ORDER BY min_sepet_tutari, oyun_adi");
             ResultSet sonuc = komut.executeQuery()) {
            while (sonuc.next()) {
                liste.add(new KutuOyunu(sonuc.getInt("id"), sonuc.getString("oyun_adi"),
                        sonuc.getBigDecimal("min_sepet_tutari")));
            }
        }
        return liste;
    }

    /** Sepetteki ürünlerin toplamı (bahşiş hariç). */
    public static BigDecimal araToplam(List<SepetKalemi> sepet) {
        BigDecimal toplam = BigDecimal.ZERO;
        for (SepetKalemi kalem : sepet) {
            toplam = toplam.add(kalem.getTutar());
        }
        return toplam;
    }

    /**
     * Siparişi MySQL'e kaydeder: siparisler tablosuna 1 satır,
     * siparis_kalemleri tablosuna her ürün için 1 satır, islem_gecmisi'ne 1 satır.
     * Hepsi tek işlemde (transaction) yapılır: ya hepsi kaydedilir ya hiçbiri.
     *
     * @return yeni siparişin numarası
     */
    public int siparisKaydet(List<SepetKalemi> sepet, BigDecimal bahsis, KutuOyunu oyun) throws SQLException {
        BigDecimal ara = araToplam(sepet);
        boolean oyunVar = oyun != null && !oyun.oyunYok();
        if (oyunVar && ara.compareTo(oyun.getMinSepet()) < 0) {
            throw new IllegalArgumentException(oyun.getAd() + " için sepet en az " + Tema.tl(oyun.getMinSepet()) + " olmalı.");
        }
        BigDecimal toplam = ara.add(bahsis);

        try (Connection baglanti = veritabani.baglan()) {
            baglanti.setAutoCommit(false);
            try {
                // 1) Sipariş satırı; MySQL'in verdiği yeni id'yi geri istiyoruz
                int siparisNo;
                try (PreparedStatement komut = baglanti.prepareStatement(
                        "INSERT INTO siparisler (ara_toplam, bahsis, toplam, kutu_oyunu) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    komut.setBigDecimal(1, ara);
                    komut.setBigDecimal(2, bahsis);
                    komut.setBigDecimal(3, toplam);
                    if (oyunVar) {
                        komut.setString(4, oyun.getAd());
                    } else {
                        komut.setNull(4, Types.VARCHAR);
                    }
                    komut.executeUpdate();
                    try (ResultSet anahtar = komut.getGeneratedKeys()) {
                        anahtar.next();
                        siparisNo = anahtar.getInt(1);
                    }
                }

                // 2) Siparişteki her ürün
                try (PreparedStatement komut = baglanti.prepareStatement(
                        "INSERT INTO siparis_kalemleri (siparis_id, urun_adi, birim_fiyat, adet, tutar) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    for (SepetKalemi kalem : sepet) {
                        komut.setInt(1, siparisNo);
                        komut.setString(2, kalem.getUrun().getAd());
                        komut.setBigDecimal(3, kalem.getUrun().getFiyat());
                        komut.setInt(4, kalem.getAdet());
                        komut.setBigDecimal(5, kalem.getTutar());
                        komut.addBatch();
                    }
                    komut.executeBatch();
                }

                // 3) İşlem geçmişi
                Veritabani.gecmiseYaz(baglanti, "SİPARİŞ", "Sipariş #" + siparisNo + ": "
                        + sepet.size() + " çeşit ürün, ara toplam " + Tema.tl(ara)
                        + ", bahşiş " + Tema.tl(bahsis) + ", toplam " + Tema.tl(toplam)
                        + (oyunVar ? ", kutu oyunu: " + oyun.getAd() : ""));

                baglanti.commit();
                return siparisNo;
            } catch (SQLException e) {
                baglanti.rollback();
                throw e;
            }
        }
    }

    /** Son siparişler: { tarih, ürünler, ara toplam, bahşiş, toplam, kutu oyunu } */
    public List<Object[]> sonSiparisler(int enFazla) throws SQLException {
        String sql = "SELECT s.tarih, "
                + "GROUP_CONCAT(CONCAT(k.adet, ' x ', k.urun_adi) SEPARATOR ', ') AS urunler, "
                + "s.ara_toplam, s.bahsis, s.toplam, s.kutu_oyunu "
                + "FROM siparisler s LEFT JOIN siparis_kalemleri k ON k.siparis_id = s.id "
                + "GROUP BY s.id, s.tarih, s.ara_toplam, s.bahsis, s.toplam, s.kutu_oyunu "
                + "ORDER BY s.id DESC LIMIT ?";
        List<Object[]> satirlar = new ArrayList<>();
        try (Connection baglanti = veritabani.baglan();
             PreparedStatement komut = baglanti.prepareStatement(sql)) {
            komut.setInt(1, enFazla);
            try (ResultSet sonuc = komut.executeQuery()) {
                while (sonuc.next()) {
                    String oyun = sonuc.getString("kutu_oyunu");
                    satirlar.add(new Object[] {
                            sonuc.getTimestamp("tarih").toLocalDateTime().format(TARIH),
                            sonuc.getString("urunler"),
                            sonuc.getBigDecimal("ara_toplam"),
                            sonuc.getBigDecimal("bahsis"),
                            sonuc.getBigDecimal("toplam"),
                            oyun == null ? "-" : oyun });
                }
            }
        }
        return satirlar;
    }

    /** Son işlemler: { tarih, işlem, açıklama } */
    public List<Object[]> islemGecmisi(int enFazla) throws SQLException {
        List<Object[]> satirlar = new ArrayList<>();
        try (Connection baglanti = veritabani.baglan();
             PreparedStatement komut = baglanti.prepareStatement(
                     "SELECT tarih, islem, aciklama FROM islem_gecmisi ORDER BY id DESC LIMIT ?")) {
            komut.setInt(1, enFazla);
            try (ResultSet sonuc = komut.executeQuery()) {
                while (sonuc.next()) {
                    satirlar.add(new Object[] {
                            sonuc.getTimestamp("tarih").toLocalDateTime().format(TARIH),
                            sonuc.getString("islem"),
                            sonuc.getString("aciklama") });
                }
            }
        }
        return satirlar;
    }
}
