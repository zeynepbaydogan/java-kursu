package JavaKursu;

import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * "Geçmiş" sekmesi: son siparişler ve yapılan tüm işlemler.
 * Aynı bilgiler MySQL'de siparisler, siparis_kalemleri ve islem_gecmisi tablolarındadır.
 */
public class GecmisPaneli extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int EN_FAZLA = 200;

    private final SiparisIslemleri islemler;
    private final Consumer<String> durum;

    private final DefaultTableModel siparisModeli = Tema.saltOkunurModel(
            "Tarih", "Ürünler", "Ara Toplam", "Bahşiş", "Toplam", "Kutu Oyunu");
    private final DefaultTableModel islemModeli = Tema.saltOkunurModel("Tarih", "İşlem", "Açıklama");

    public GecmisPaneli(SiparisIslemleri islemler, Consumer<String> durum) {
        super(new BorderLayout(0, 8));
        this.islemler = islemler;
        this.durum = durum;
        setBackground(Tema.KREM);
        setBorder(new EmptyBorder(12, 14, 12, 14));

        JTable siparisTablosu = new JTable(siparisModeli);
        Tema.tabloyuSusle(siparisTablosu);
        int[] genislik = { 130, 420, 110, 90, 110, 140 };
        for (int i = 0; i < genislik.length; i++) {
            siparisTablosu.getColumnModel().getColumn(i).setPreferredWidth(genislik[i]);
        }
        for (int i = 2; i <= 4; i++) {
            siparisTablosu.getColumnModel().getColumn(i).setCellRenderer(Tema.paraGosterici());
        }

        JTable islemTablosu = new JTable(islemModeli);
        Tema.tabloyuSusle(islemTablosu);
        islemTablosu.getColumnModel().getColumn(0).setPreferredWidth(130);
        islemTablosu.getColumnModel().getColumn(1).setPreferredWidth(110);
        islemTablosu.getColumnModel().getColumn(2).setPreferredWidth(760);

        JSplitPane bolme = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                bolum("Son Siparişler", siparisTablosu),
                bolum("İşlem Geçmişi (ekleme, silme, güncelleme, indirim / zam, sipariş)", islemTablosu));
        bolme.setResizeWeight(0.5);
        bolme.setBorder(null);
        bolme.setOpaque(false);
        add(bolme, BorderLayout.CENTER);

        JPanel alt = new JPanel(new BorderLayout());
        alt.setOpaque(false);
        alt.add(Tema.ipucu("MySQL Workbench'te görmek için: SELECT * FROM java_kursu.islem_gecmisi ORDER BY id DESC;"),
                BorderLayout.CENTER);
        JButton yenileDugmesi = Tema.dugme("Yenile", Tema.KARAMEL);
        yenileDugmesi.addActionListener(olay -> yenile());
        alt.add(yenileDugmesi, BorderLayout.EAST);
        add(alt, BorderLayout.SOUTH);
    }

    private JPanel bolum(String baslik, JTable tablo) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(Tema.baslik(baslik), BorderLayout.NORTH);
        panel.add(new JScrollPane(tablo), BorderLayout.CENTER);
        return panel;
    }

    public void yenile() {
        try {
            siparisModeli.setRowCount(0);
            for (Object[] satir : islemler.sonSiparisler(EN_FAZLA)) {
                siparisModeli.addRow(satir);
            }
            islemModeli.setRowCount(0);
            for (Object[] satir : islemler.islemGecmisi(EN_FAZLA)) {
                islemModeli.addRow(satir);
            }
            durum.accept(siparisModeli.getRowCount() + " sipariş, " + islemModeli.getRowCount() + " işlem kaydı gösteriliyor.");
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }
}
