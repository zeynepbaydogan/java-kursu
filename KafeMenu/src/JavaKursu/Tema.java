package JavaKursu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Uygulamanın ortak görünümü: kafe adı, renkler, yazı tipleri,
 * düğme/tablo süsleme ve kısa mesaj pencereleri.
 * Kafenin adını veya renklerini değiştirmek için sadece bu dosyayı düzenleyin.
 */
public final class Tema {

    public static final String KAFE_ADI = "Kahve Durağı";

    // ---- Renk paleti ----
    public static final Color KAHVE      = new Color(78, 52, 46);
    public static final Color KARAMEL    = new Color(161, 110, 70);
    public static final Color SUTLU      = new Color(215, 180, 140);
    public static final Color KREM       = new Color(255, 248, 231);
    public static final Color SATIR_ACIK = new Color(252, 245, 235);
    public static final Color YESIL      = new Color(85, 125, 70);
    public static final Color KIRMIZI    = new Color(170, 60, 50);
    public static final Color GRI        = new Color(140, 125, 115);

    // ---- Yazı tipleri ----
    public static final Font YAZI       = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font YAZI_KALIN = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font BASLIK     = new Font("Georgia", Font.BOLD, 19);

    private Tema() {
        // Bu sınıftan nesne oluşturulmaz; sadece hazır metotları kullanılır.
    }

    /** Renkli, düz bir düğme. */
    public static JButton dugme(String yazi, Color renk) {
        JButton dugme = new JButton(yazi);
        dugme.setFont(YAZI_KALIN);
        dugme.setBackground(renk);
        dugme.setForeground(Color.WHITE);
        dugme.setFocusPainted(false);
        dugme.setBorderPainted(false);
        dugme.setOpaque(true);
        dugme.setBorder(new EmptyBorder(8, 14, 8, 14));
        dugme.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return dugme;
    }

    public static JLabel baslik(String yazi) {
        JLabel etiket = new JLabel(yazi);
        etiket.setFont(BASLIK);
        etiket.setForeground(KAHVE);
        return etiket;
    }

    public static JLabel etiket(String yazi) {
        JLabel etiket = new JLabel(yazi);
        etiket.setFont(YAZI);
        etiket.setForeground(KAHVE);
        return etiket;
    }

    public static JLabel ipucu(String yazi) {
        JLabel etiket = new JLabel(yazi + "   "); // sondaki boşluk: italik harfin kesilmemesi için
        etiket.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        etiket.setForeground(new Color(120, 100, 90));
        return etiket;
    }

    /** 1250.5 -> "1.250,50 TL" */
    public static String tl(BigDecimal tutar) {
        return String.format("%,.2f TL", tutar);
    }

    /**
     * Kullanıcının yazdığı sayıyı okur: "85", "85,50" ve "85.50" kabul edilir.
     * Boşsa veya sayı değilse null döner.
     */
    public static BigDecimal sayiOku(String metin) {
        if (metin == null) {
            return null;
        }
        String temiz = metin.trim().replace("TL", "").replace("%", "").replace(" ", "").replace(',', '.');
        if (temiz.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(temiz).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Tüm tablolara aynı görünümü verir: satır yüksekliği, renkler, başlık. */
    public static void tabloyuSusle(JTable tablo) {
        tablo.setFont(YAZI);
        tablo.setRowHeight(30);
        tablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablo.setSelectionBackground(SUTLU);
        tablo.setSelectionForeground(KAHVE);
        tablo.setShowVerticalLines(false);
        tablo.setGridColor(new Color(235, 222, 205));
        tablo.setFillsViewportHeight(true);
        tablo.setDefaultRenderer(Object.class, new ZebraGosterici(SwingConstants.LEFT, false));

        JTableHeader baslik = tablo.getTableHeader();
        baslik.setFont(YAZI_KALIN);
        baslik.setBackground(KARAMEL);
        baslik.setForeground(Color.WHITE);
        baslik.setPreferredSize(new Dimension(0, 32));
        baslik.setReorderingAllowed(false);
    }

    /** Para sütunları için: "85,00 TL", sağa yaslı. */
    public static DefaultTableCellRenderer paraGosterici() {
        return new ZebraGosterici(SwingConstants.RIGHT, true);
    }

    /** Ortaya hizalı sütunlar için (ör. adet). */
    public static DefaultTableCellRenderer ortaGosterici() {
        return new ZebraGosterici(SwingConstants.CENTER, false);
    }

    /** Hücreleri doğrudan düzenlenemeyen basit tablo modeli. */
    public static DefaultTableModel saltOkunurModel(String... sutunlar) {
        return new DefaultTableModel(sutunlar, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int satir, int sutun) {
                return false;
            }
        };
    }

    public static void uyar(Component ust, String mesaj) {
        JOptionPane.showMessageDialog(ust, mesaj, KAFE_ADI, JOptionPane.WARNING_MESSAGE);
    }

    public static void bilgi(Component ust, String mesaj) {
        JOptionPane.showMessageDialog(ust, mesaj, KAFE_ADI, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void hata(Component ust, SQLException e) {
        JOptionPane.showMessageDialog(ust, Veritabani.anlasilirHata(e), "Veritabanı hatası", JOptionPane.ERROR_MESSAGE);
    }

    /** Satırları sırayla beyaz / krem boyayan hücre çizicisi. */
    private static class ZebraGosterici extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final boolean para;

        ZebraGosterici(int hiza, boolean para) {
            this.para = para;
            setHorizontalAlignment(hiza);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tablo, Object deger, boolean secili,
                boolean odakta, int satir, int sutun) {
            Object yazi = (para && deger instanceof BigDecimal) ? tl((BigDecimal) deger) : deger;
            Component hucre = super.getTableCellRendererComponent(tablo, yazi, secili, false, satir, sutun);
            if (!secili) {
                hucre.setBackground(satir % 2 == 0 ? Color.WHITE : SATIR_ACIK);
            }
            setBorder(new EmptyBorder(0, 10, 0, 12));
            return hucre;
        }
    }
}
