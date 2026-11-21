package JavaKursu;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** "Kutu Oyunları" sekmesi: her oyun ve yanında minimum sepet tutarı. */
public class OyunlarPaneli extends JPanel {

    private static final long serialVersionUID = 1L;

    private final SiparisIslemleri islemler;
    private final Consumer<String> durum;
    private final DefaultTableModel model = Tema.saltOkunurModel("Kutu Oyunu", "Minimum Sepet Tutarı");
    private final JTable tablo = new JTable(model);

    public OyunlarPaneli(SiparisIslemleri islemler, Consumer<String> durum) {
        super(new BorderLayout(0, 8));
        this.islemler = islemler;
        this.durum = durum;
        setBackground(Tema.KREM);
        setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel ust = new JPanel(new GridLayout(0, 1));
        ust.setOpaque(false);
        ust.add(Tema.baslik("Kutu Oyunları"));
        ust.add(Tema.etiket("Bir kutu oyunu oynamak için siparişin (bahşiş hariç) en az yandaki tutarda olması gerekir."));
        ust.add(Tema.ipucu("Oyunu, Sipariş sekmesindeki sepetin altından seçebilirsiniz."));
        add(ust, BorderLayout.NORTH);

        Tema.tabloyuSusle(tablo);
        tablo.getColumnModel().getColumn(0).setPreferredWidth(400);
        tablo.getColumnModel().getColumn(1).setPreferredWidth(200);
        tablo.getColumnModel().getColumn(1).setCellRenderer(Tema.paraGosterici());
        add(new JScrollPane(tablo), BorderLayout.CENTER);
    }

    public void yenile() {
        try {
            List<KutuOyunu> oyunlar = islemler.kutuOyunlari();
            model.setRowCount(0);
            for (KutuOyunu oyun : oyunlar) {
                model.addRow(new Object[] { oyun.getAd(), oyun.getMinSepet() });
            }
            durum.accept(oyunlar.size() + " kutu oyunu listelendi.");
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }
}
