package JavaKursu;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * MySQL'deki 'ogrenciler' tablosuna PreparedStatement ile
 * yeni bir öğrenci (ad, soyad) ekleyen basit örnek.
 */
public class OgrenciEkle {

    // 1) Bağlantı bilgileri
    //    "okul" yerine kendi veritabanınızın adını yazın.
    //    Şifre BURADA YOK; program çalışırken sizden istenir.
    private static final String URL =
            "jdbc:mysql://localhost:3306/okul?useUnicode=true&characterEncoding=UTF-8";
    private static final String KULLANICI = "root";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 2) Şifreyi kullanıcıdan al
        String sifre = sifreIste(scanner);

        // 3) Eklenecek öğrencinin bilgilerini al
        System.out.print("Öğrencinin adı: ");
        String ad = scanner.nextLine().trim();
        System.out.print("Öğrencinin soyadı: ");
        String soyad = scanner.nextLine().trim();

        // 4) SQL komutu: değerlerin yerine '?' koyuyoruz
        String sql = "INSERT INTO ogrenciler (ad, soyad) VALUES (?, ?)";

        // 5) try-with-resources: bağlantı ve komut, iş bitince kendiliğinden kapanır
        try (Connection baglanti = DriverManager.getConnection(URL, KULLANICI, sifre);
             PreparedStatement komut = baglanti.prepareStatement(sql)) {

            // 6) '?' işaretlerini sırayla dolduruyoruz (sayım 1'den başlar)
            komut.setString(1, ad);
            komut.setString(2, soyad);

            // 7) Komutu çalıştır; kaç satır eklendiğini döndürür
            int eklenenSatir = komut.executeUpdate();
            System.out.println(eklenenSatir + " öğrenci başarıyla eklendi: " + ad + " " + soyad);

        } catch (SQLException e) {
            // 8) Bir sorun olursa anlaşılır bir mesaj göster
            System.out.println("Veritabanı hatası: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    /**
     * Şifreyi ekrana yazdırmadan almaya çalışır.
     * Eclipse konsolunda bu genellikle mümkün olmadığından
     * (System.console() null döner) o durumda normal satır okumaya geçer.
     */
    private static String sifreIste(Scanner scanner) {
        Console konsol = System.console();
        if (konsol != null) {
            char[] karakterler = konsol.readPassword("MySQL şifresi: ");
            return new String(karakterler);
        }
        System.out.print("MySQL şifresi: ");
        return scanner.nextLine();
    }
}
