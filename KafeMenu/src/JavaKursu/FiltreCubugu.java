package JavaKursu;

import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Menünün üstündeki filtre çubuğu:
 *   Kategori: [Tüm kategoriler v]   Ürün ara: [______]   [Listele] [Tümünü Göster]
 * Kategori seçilince, Enter'a basılınca veya Listele'ye tıklanınca liste yenilenir.
 */
public class FiltreCubugu extends JPanel {

    private static final long serialVersionUID = 1L;
    public static final String TUMU = "Tüm kategoriler";

    private final JComboBox<String> kategoriKutusu = new JComboBox<>();
    private final JTextField aramaKutusu = new JTextField(14);
    private boolean yukleniyor = false; // liste doldurulurken gereksiz yenilemeyi önler

    public FiltreCubugu(Runnable listele) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setOpaque(false);

        kategoriKutusu.setFont(Tema.YAZI);
        aramaKutusu.setFont(Tema.YAZI);

        add(Tema.etiket("Kategori:"));
        add(kategoriKutusu);
        add(Tema.etiket("   Ürün ara:"));
        add(aramaKutusu);

        JButton listeleDugmesi = Tema.dugme("Listele", Tema.KARAMEL);
        listeleDugmesi.addActionListener(olay -> listele.run());
        add(listeleDugmesi);

        JButton tumuDugmesi = Tema.dugme("Tümünü Göster", Tema.GRI);
        tumuDugmesi.addActionListener(olay -> {
            yukleniyor = true;
            kategoriKutusu.setSelectedIndex(0);
            aramaKutusu.setText("");
            yukleniyor = false;
            listele.run();
        });
        add(tumuDugmesi);

        aramaKutusu.addActionListener(olay -> listele.run()); // Enter tuşu
        kategoriKutusu.addActionListener(olay -> {
            if (!yukleniyor) {
                listele.run();
            }
        });
    }

    /** Açılır listeyi veritabanındaki kategorilerle doldurur; önceki seçimi korur. */
    public void kategorileriYukle(List<String> kategoriler) {
        yukleniyor = true;
        Object onceki = kategoriKutusu.getSelectedItem();
        kategoriKutusu.removeAllItems();
        kategoriKutusu.addItem(TUMU);
        for (String kategori : kategoriler) {
            kategoriKutusu.addItem(kategori);
        }
        if (onceki != null) {
            kategoriKutusu.setSelectedItem(onceki);
        }
        yukleniyor = false;
    }

    /** Seçili kategori; "Tüm kategoriler" seçiliyse null. */
    public String getKategori() {
        Object secili = kategoriKutusu.getSelectedItem();
        return (secili == null || TUMU.equals(secili)) ? null : secili.toString();
    }

    public String getArama() {
        return aramaKutusu.getText().trim();
    }
}
