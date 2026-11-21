package JavaKursu;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
 * MySQL bağlantısı ve veritabanı kurulumu.
 *
 * Şifre koda yazılmaz; program açılırken kullanıcıdan istenir.
 *
 * java_kursu veritabanındaki tablolar:
 *   menu_urunleri     : menüdeki ürünler (ad, kategori, fiyat)
 *   kategoriler       : açılır listede çıkan kategoriler
 *   kutu_oyunlari     : oyunlar ve minimum sepet tutarları
 *   siparisler        : tamamlanan her sipariş (ara toplam, bahşiş, toplam, oyun)
 *   siparis_kalemleri : her siparişte hangi üründen kaç adet alındığı
 *   islem_gecmisi     : ekleme, silme, güncelleme, indirim/zam, sipariş kayıtları
 */
public class Veritabani {

    // localhost = bu bilgisayar, 3306 = MySQL'in varsayılan portu
    private static final String SUNUCU_URL =
            "jdbc:mysql://localhost:3306/?useUnicode=true&characterEncoding=UTF-8";
    private static final String URL =
            "jdbc:mysql://localhost:3306/java_kursu?useUnicode=true&characterEncoding=UTF-8";
    private static final String KULLANICI = "root";

    // ---- Hazır veriler (ilk kurulumda eklenir) ----

    /** Kategoriler, menüde bu sırayla görünür. */
    private static final String[] KATEGORILER = {
            "Sıcak İçecekler", "Soğuk İçecekler", "Bitki Çayları", "Kahvaltılıklar",
            "Fast Food", "Atıştırmalıklar", "Tatlılar" };

    /** { ürün adı, kategori, fiyat } */
    private static final String[][] MENU = {
            { "Türk Kahvesi", "Sıcak İçecekler", "85.00" },
            { "Çay", "Sıcak İçecekler", "25.00" },
            { "Latte", "Sıcak İçecekler", "120.00" },
            { "Cappuccino", "Sıcak İçecekler", "115.00" },
            { "Americano", "Sıcak İçecekler", "100.00" },
            { "Filtre Kahve", "Sıcak İçecekler", "95.00" },
            { "Sıcak Çikolata", "Sıcak İçecekler", "110.00" },
            { "Salep", "Sıcak İçecekler", "100.00" },
            { "Limonata", "Soğuk İçecekler", "90.00" },
            { "Ice Americano", "Soğuk İçecekler", "110.00" },
            { "Ice Latte", "Soğuk İçecekler", "125.00" },
            { "Çilekli Milkshake", "Soğuk İçecekler", "140.00" },
            { "Frozen", "Soğuk İçecekler", "130.00" },
            { "Ayran", "Soğuk İçecekler", "40.00" },
            { "Soda", "Soğuk İçecekler", "35.00" },
            { "Ihlamur", "Bitki Çayları", "60.00" },
            { "Adaçayı", "Bitki Çayları", "60.00" },
            { "Kış Çayı", "Bitki Çayları", "70.00" },
            { "Yeşil Çay", "Bitki Çayları", "60.00" },
            { "Serpme Kahvaltı (1 kişilik)", "Kahvaltılıklar", "450.00" },
            { "Menemen", "Kahvaltılıklar", "160.00" },
            { "Sucuklu Yumurta", "Kahvaltılıklar", "170.00" },
            { "Omlet", "Kahvaltılıklar", "140.00" },
            { "Pankek", "Kahvaltılıklar", "150.00" },
            { "Hamburger", "Fast Food", "220.00" },
            { "Cheeseburger", "Fast Food", "240.00" },
            { "Tavuk Burger", "Fast Food", "210.00" },
            { "Hot Dog", "Fast Food", "150.00" },
            { "Tavuk Wrap", "Fast Food", "190.00" },
            { "Pizza Dilim", "Fast Food", "120.00" },
            { "Kaşarlı Tost", "Atıştırmalıklar", "130.00" },
            { "Patates Kızartması", "Atıştırmalıklar", "110.00" },
            { "Çıtır Tavuk", "Atıştırmalıklar", "160.00" },
            { "Nachos", "Atıştırmalıklar", "140.00" },
            { "Simit", "Atıştırmalıklar", "30.00" },
            { "San Sebastian", "Tatlılar", "180.00" },
            { "Brownie", "Tatlılar", "140.00" },
            { "Cheesecake", "Tatlılar", "170.00" },
            { "Tiramisu", "Tatlılar", "165.00" },
            { "Sufle", "Tatlılar", "150.00" },
            { "Waffle", "Tatlılar", "190.00" },
    };

    /** { oyun adı, minimum sepet tutarı } */
    private static final String[][] KUTU_OYUNLARI = {
            { "Okey", "300.00" },
            { "Tavla", "150.00" },
            { "Satranç", "150.00" },
            { "Kızma Birader", "150.00" },
            { "İskambil (Batak, Pişti)", "200.00" },
            { "UNO", "200.00" },
            { "Jenga", "200.00" },
            { "Tabu", "250.00" },
            { "Scrabble", "250.00" },
            { "Codenames", "250.00" },
            { "Dixit", "300.00" },
            { "Monopoly", "350.00" },
            { "Catan", "400.00" },
            { "Risk", "400.00" },
    };

    private final String sifre;

    public Veritabani(String sifre) {
        this.sifre = sifre;
    }

    /** java_kursu veritabanına bağlantı açar. Kullanan kod, işi bitince kapatmalıdır. */
    public Connection baglan() throws SQLException {
        return DriverManager.getConnection(URL, KULLANICI, sifre);
    }

    /** Veritabanı seçmeden MySQL sunucusuna bağlanır (java_kursu henüz yoksa da çalışır). */
    protected Connection sunucuyaBaglan() throws SQLException {
        return DriverManager.getConnection(SUNUCU_URL, KULLANICI, sifre);
    }

    /** Şifrenin doğru olup olmadığını anlamak için bir kez bağlanıp hemen kapatır. */
    public void baglantiyiDene() throws SQLException {
        try (Connection baglanti = sunucuyaBaglan()) {
            baglanti.isValid(2);
        }
    }

    // =====================================================================
    //  KURULUM
    // =====================================================================

    /**
     * Program her açıldığında çalışır ve eksik olanı tamamlar:
     *   - java_kursu veritabanı ve 6 tablo yoksa oluşturulur,
     *   - eski sürümden kalan tekrar eden ürünler temizlenir,
     *   - kategoriler ve kutu oyunları tabloları İLK KEZ oluşturulduğunda
     *     hazır menü, kategoriler ve oyunlar eklenir.
     * Sizin eklediğiniz, sildiğiniz veya değiştirdiğiniz kayıtlara dokunulmaz.
     *
     * @return yapılanların kısa listesi (hiçbir şey yapılmadıysa boş liste)
     */
    public List<String> kurulumYap() throws SQLException {
        List<String> yapilanlar = new ArrayList<>();

        // 1) Veritabanı
        try (Connection baglanti = sunucuyaBaglan();
             Statement komut = baglanti.createStatement()) {
            komut.executeUpdate("CREATE DATABASE IF NOT EXISTS java_kursu "
                    + "CHARACTER SET utf8mb4 COLLATE utf8mb4_turkish_ci");
        }

        try (Connection baglanti = baglan()) {
            // 2) Hangi tablolar zaten vardı? (Önce bakıyoruz, sonra oluşturuyoruz.)
            boolean menuVardi = tabloVarMi(baglanti, "menu_urunleri");
            boolean kategorilerVardi = tabloVarMi(baglanti, "kategoriler");
            boolean oyunlarVardi = tabloVarMi(baglanti, "kutu_oyunlari");

            // 3) Eksik tabloları oluştur
            tablolariOlustur(baglanti);

            // 4) Eski sürümden kalan tekrarlar
            if (menuVardi) {
                int silinen = tekrarlariTemizle(baglanti);
                if (silinen > 0) {
                    yapilanlar.add(silinen + " tekrar eden ürün kaydı temizlendi");
                }
            }

            // 5) Hazır kategoriler + hazır menü (kategoriler tablosu yeni oluştuysa)
            if (!kategorilerVardi) {
                try (PreparedStatement komut = baglanti.prepareStatement(
                        "INSERT IGNORE INTO kategoriler (ad, sira) VALUES (?, ?)")) {
                    for (int i = 0; i < KATEGORILER.length; i++) {
                        komut.setString(1, KATEGORILER[i]);
                        komut.setInt(2, i + 1);
                        komut.addBatch();
                    }
                    komut.executeBatch();
                }
                yapilanlar.add(KATEGORILER.length + " kategori hazırlandı");

                // INSERT IGNORE: menüde zaten olan ürün atlanır, tekrar eklenmez
                int eklenen = 0;
                try (PreparedStatement komut = baglanti.prepareStatement(
                        "INSERT IGNORE INTO menu_urunleri (urun_adi, kategori, fiyat) VALUES (?, ?, ?)")) {
                    for (String[] urun : MENU) {
                        komut.setString(1, urun[0]);
                        komut.setString(2, urun[1]);
                        komut.setBigDecimal(3, new BigDecimal(urun[2]));
                        komut.addBatch();
                    }
                    for (int sonuc : komut.executeBatch()) {
                        if (sonuc > 0) {
                            eklenen++;
                        }
                    }
                }
                if (eklenen > 0) {
                    yapilanlar.add(eklenen + " hazır ürün menüye eklendi");
                }
            }

            // 6) Hazır kutu oyunları (tablo yeni oluştuysa)
            if (!oyunlarVardi) {
                try (PreparedStatement komut = baglanti.prepareStatement(
                        "INSERT IGNORE INTO kutu_oyunlari (oyun_adi, min_sepet_tutari) VALUES (?, ?)")) {
                    for (String[] oyun : KUTU_OYUNLARI) {
                        komut.setString(1, oyun[0]);
                        komut.setBigDecimal(2, new BigDecimal(oyun[1]));
                        komut.addBatch();
                    }
                    komut.executeBatch();
                }
                yapilanlar.add(KUTU_OYUNLARI.length + " kutu oyunu eklendi");
            }

            // 7) Ürünlerde kullanılıp listede olmayan kategoriler de listeye eklensin
            try (Statement komut = baglanti.createStatement()) {
                komut.executeUpdate("INSERT IGNORE INTO kategoriler (ad) "
                        + "SELECT DISTINCT kategori FROM menu_urunleri");
            }

            if (!yapilanlar.isEmpty()) {
                gecmiseYaz(baglanti, "KURULUM", String.join(", ", yapilanlar));
            }
        }
        return yapilanlar;
    }

    /** CREATE TABLE IF NOT EXISTS: tablo varsa hiçbir şey yapmaz. */
    private void tablolariOlustur(Connection baglanti) throws SQLException {
        try (Statement komut = baglanti.createStatement()) {
            komut.executeUpdate("CREATE TABLE IF NOT EXISTS menu_urunleri ("
                    + " id        INT AUTO_INCREMENT PRIMARY KEY,"
                    + " urun_adi  VARCHAR(100) NOT NULL,"
                    + " kategori  VARCHAR(50)  NOT NULL,"
                    + " fiyat     DECIMAL(8,2) NOT NULL,"
                    + " UNIQUE KEY uk_urun_adi (urun_adi))");

            komut.executeUpdate("CREATE TABLE IF NOT EXISTS kategoriler ("
                    + " id    INT AUTO_INCREMENT PRIMARY KEY,"
                    + " ad    VARCHAR(50) NOT NULL,"
                    + " sira  INT NOT NULL DEFAULT 99,"
                    + " UNIQUE KEY uk_kategori_adi (ad))");

            komut.executeUpdate("CREATE TABLE IF NOT EXISTS kutu_oyunlari ("
                    + " id                INT AUTO_INCREMENT PRIMARY KEY,"
                    + " oyun_adi          VARCHAR(50)  NOT NULL,"
                    + " min_sepet_tutari  DECIMAL(8,2) NOT NULL,"
                    + " UNIQUE KEY uk_oyun_adi (oyun_adi))");

            komut.executeUpdate("CREATE TABLE IF NOT EXISTS siparisler ("
                    + " id          INT AUTO_INCREMENT PRIMARY KEY,"
                    + " tarih       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " ara_toplam  DECIMAL(10,2) NOT NULL,"
                    + " bahsis      DECIMAL(10,2) NOT NULL DEFAULT 0,"
                    + " toplam      DECIMAL(10,2) NOT NULL,"
                    + " kutu_oyunu  VARCHAR(50) NULL)");

            komut.executeUpdate("CREATE TABLE IF NOT EXISTS siparis_kalemleri ("
                    + " id           INT AUTO_INCREMENT PRIMARY KEY,"
                    + " siparis_id   INT NOT NULL,"
                    + " urun_adi     VARCHAR(100) NOT NULL,"
                    + " birim_fiyat  DECIMAL(8,2)  NOT NULL,"
                    + " adet         INT NOT NULL,"
                    + " tutar        DECIMAL(10,2) NOT NULL,"
                    + " FOREIGN KEY (siparis_id) REFERENCES siparisler(id) ON DELETE CASCADE)");

            komut.executeUpdate("CREATE TABLE IF NOT EXISTS islem_gecmisi ("
                    + " id        INT AUTO_INCREMENT PRIMARY KEY,"
                    + " tarih     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " islem     VARCHAR(30)  NOT NULL,"
                    + " aciklama  VARCHAR(255) NOT NULL)");
        }
    }

    private static boolean tabloVarMi(Connection baglanti, String tabloAdi) throws SQLException {
        try (ResultSet tablolar = baglanti.getMetaData()
                .getTables(baglanti.getCatalog(), null, tabloAdi, new String[] { "TABLE" })) {
            return tablolar.next();
        }
    }

    /**
     * Eski sürümde oluşmuş tekrar eden ürünleri temizler ve
     * "ürün adı benzersiz olsun" kuralını (UNIQUE) ekler. Kural zaten varsa 0 döner.
     */
    private static int tekrarlariTemizle(Connection baglanti) throws SQLException {
        try (ResultSet indeksler = baglanti.getMetaData()
                .getIndexInfo(baglanti.getCatalog(), null, "menu_urunleri", true, false)) {
            while (indeksler.next()) {
                if ("urun_adi".equalsIgnoreCase(indeksler.getString("COLUMN_NAME"))) {
                    return 0;
                }
            }
        }
        try (Statement komut = baglanti.createStatement()) {
            int silinen = komut.executeUpdate(
                    "DELETE FROM menu_urunleri WHERE id NOT IN ("
                            + " SELECT id FROM (SELECT MIN(id) AS id FROM menu_urunleri GROUP BY urun_adi) AS ilkler)");
            komut.executeUpdate("ALTER TABLE menu_urunleri ADD UNIQUE KEY uk_urun_adi (urun_adi)");
            return silinen;
        }
    }

    /**
     * islem_gecmisi tablosuna bir satır yazar.
     * Böylece yapılan her işlemi MySQL'de görebilirsiniz.
     */
    static void gecmiseYaz(Connection baglanti, String islem, String aciklama) throws SQLException {
        String metin = aciklama.length() > 255 ? aciklama.substring(0, 252) + "..." : aciklama;
        try (PreparedStatement komut = baglanti.prepareStatement(
                "INSERT INTO islem_gecmisi (islem, aciklama) VALUES (?, ?)")) {
            komut.setString(1, islem);
            komut.setString(2, metin);
            komut.executeUpdate();
        }
    }

    // =====================================================================
    //  GİRİŞ VE HATA MESAJLARI
    // =====================================================================

    /** Şifreyi yıldızlı bir kutuyla sorar. "İptal"e basılırsa null döner. */
    public static String sifreIste() {
        JPasswordField sifreKutusu = new JPasswordField(18);
        Object[] icerik = { "MySQL '" + KULLANICI + "' kullanıcısının şifresi:", sifreKutusu };

        int secim = JOptionPane.showConfirmDialog(
                null, icerik, Tema.KAFE_ADI + " - Giriş",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (secim != JOptionPane.OK_OPTION) {
            return null;
        }
        return new String(sifreKutusu.getPassword());
    }

    /** MySQL hata kodlarını yeni başlayan birinin anlayacağı mesaja çevirir. */
    public static String anlasilirHata(SQLException e) {
        switch (e.getErrorCode()) {
            case 1045:
                return "Şifre veya kullanıcı adı yanlış.";
            case 1062:
                return "Bu ad zaten kayıtlı; aynı kayıt ikinci kez eklenemez.\n"
                        + "Değiştirmek istiyorsanız listeden seçip 'Güncelle'yi kullanın.";
            case 1264:
                return "Sayı çok büyük. Fiyat en fazla 999.999,99 TL olabilir.";
            case 1406:
                return "Yazdığınız metin çok uzun.";
            case 1044:
                return "Bu MySQL kullanıcısının veritabanı/tablo oluşturma izni yok.\n"
                        + "'root' kullanıcısıyla deneyin.";
            case 1049:
            case 1146:
                return "Veritabanı veya tablo bulunamadı.\n"
                        + "Programı kapatıp yeniden açın; açılışta otomatik oluşturulur.";
            default:
                if ("08S01".equals(e.getSQLState())) {
                    return "MySQL sunucusuna ulaşılamadı.\n"
                            + "MySQL servisi çalışıyor mu? (Windows Hizmetler > MySQL80)";
                }
                return e.getMessage();
        }
    }
}
