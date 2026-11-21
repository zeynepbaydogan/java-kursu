package JavaKursu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Kahve Durağı - ana pencere. PROGRAMI BU DOSYADAN ÇALIŞTIRIN:
 * sağ tık > Run As > Java Application
 *
 * Dört sekme vardır:
 *   Sipariş        : menüden sepete ürün ekle, kutu oyunu seç, bahşiş bırak, siparişi tamamla
 *   Ürün Yönetimi  : ürün listele / ekle / güncelle / sil, indirim / zam
 *   Kutu Oyunları  : oyunlar ve minimum sepet tutarları
 *   Geçmiş         : son siparişler ve yapılan tüm işlemler
 */
public class KafeMenuEkrani extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JLabel durumEtiketi = new JLabel("Hoş geldiniz!");
    final JTabbedPane sekmeler = new JTabbedPane();
    final SiparisPaneli siparisPaneli;
    final UrunYonetimPaneli urunPaneli;
    final OyunlarPaneli oyunlarPaneli;
    final GecmisPaneli gecmisPaneli;

    public KafeMenuEkrani(Veritabani veritabani) {
        super(Tema.KAFE_ADI + " - Kafe Yönetimi");

        UrunIslemleri urunIslemleri = new UrunIslemleri(veritabani);
        SiparisIslemleri siparisIslemleri = new SiparisIslemleri(veritabani);
        Consumer<String> durum = durumEtiketi::setText; // panellerin en alta mesaj yazması için

        siparisPaneli = new SiparisPaneli(urunIslemleri, siparisIslemleri, durum);
        urunPaneli = new UrunYonetimPaneli(urunIslemleri, durum);
        oyunlarPaneli = new OyunlarPaneli(siparisIslemleri, durum);
        gecmisPaneli = new GecmisPaneli(siparisIslemleri, durum);

        sekmeler.setFont(Tema.YAZI_KALIN);
        sekmeler.addTab("  Sipariş  ", siparisPaneli);
        sekmeler.addTab("  Ürün Yönetimi  ", urunPaneli);
        sekmeler.addTab("  Kutu Oyunları  ", oyunlarPaneli);
        sekmeler.addTab("  Geçmiş  ", gecmisPaneli);
        // Bir sekmeye geçince o sekmenin bilgileri MySQL'den yeniden okunur
        sekmeler.addChangeListener(olay -> seciliSekmeyiYenile());

        JPanel ana = new JPanel(new BorderLayout());
        ana.setBackground(Tema.KREM);
        ana.add(ustBaslik(), BorderLayout.NORTH);
        ana.add(sekmeler, BorderLayout.CENTER);
        ana.add(durumCubugu(), BorderLayout.SOUTH);
        setContentPane(ana);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 760);
        setMinimumSize(new Dimension(1080, 660));
        setLocationRelativeTo(null);
    }

    private JPanel ustBaslik() {
        JPanel baslik = new JPanel(new GridLayout(2, 1));
        baslik.setBackground(Tema.KAHVE);
        baslik.setBorder(new EmptyBorder(12, 24, 12, 24));
        JLabel ad = new JLabel(Tema.KAFE_ADI);
        ad.setFont(new Font("Georgia", Font.BOLD, 30));
        ad.setForeground(Tema.KREM);
        JLabel alt = new JLabel("Sipariş  ·  Ürün Yönetimi  ·  Kutu Oyunları  ·  Geçmiş");
        alt.setFont(Tema.YAZI);
        alt.setForeground(Tema.SUTLU);
        baslik.add(ad);
        baslik.add(alt);
        return baslik;
    }

    private JPanel durumCubugu() {
        JPanel cubuk = new JPanel(new BorderLayout());
        cubuk.setBackground(Tema.SUTLU);
        cubuk.setBorder(new EmptyBorder(6, 16, 6, 16));
        durumEtiketi.setFont(Tema.YAZI);
        durumEtiketi.setForeground(Tema.KAHVE);
        cubuk.add(durumEtiketi, BorderLayout.CENTER);
        return cubuk;
    }

    void seciliSekmeyiYenile() {
        switch (sekmeler.getSelectedIndex()) {
            case 0: siparisPaneli.yenile(); break;
            case 1: urunPaneli.yenile(); break;
            case 2: oyunlarPaneli.yenile(); break;
            case 3: gecmisPaneli.yenile(); break;
            default: break;
        }
    }

    // =====================================================================
    //  PROGRAMIN BAŞLANGIÇ NOKTASI
    // =====================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Veritabani veritabani = girisYap();
            if (veritabani == null) {
                System.exit(0); // vazgeçildi veya bağlanılamadı
            } else {
                KafeMenuEkrani ekran = new KafeMenuEkrani(veritabani);
                ekran.setVisible(true);
                ekran.seciliSekmeyiYenile(); // açılışta menüyü getir
            }
        });
    }

    /**
     * Şifreyi sorar, bağlanır ve kurulumu yapar (eksik tablolar, hazır menü, oyunlar).
     * Şifre yanlışsa 3 deneme hakkı vardır.
     */
    private static Veritabani girisYap() {
        for (int deneme = 1; deneme <= 3; deneme++) {
            String sifre = Veritabani.sifreIste();
            if (sifre == null) {
                return null;
            }
            Veritabani veritabani = new Veritabani(sifre);
            try {
                veritabani.baglantiyiDene();
                List<String> yapilanlar = veritabani.kurulumYap();
                if (!yapilanlar.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "Veritabanı hazırlandı:\n- " + String.join("\n- ", yapilanlar),
                            Tema.KAFE_ADI, JOptionPane.INFORMATION_MESSAGE);
                }
                return veritabani;
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, Veritabani.anlasilirHata(e),
                        "Bağlantı kurulamadı (" + deneme + "/3)", JOptionPane.ERROR_MESSAGE);
                if (e.getErrorCode() != 1045) {
                    return null; // sorun şifrede değilse tekrar sormanın anlamı yok
                }
            }
        }
        return null;
    }
}
