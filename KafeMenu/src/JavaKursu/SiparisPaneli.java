package JavaKursu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

/**
 * "Sipariş" sekmesi.
 *   Sol: menü (kategoriye göre filtrelenebilir, isimle aranabilir)
 *   Sağ: sepet, kutu oyunu seçimi, bahşiş ve toplam
 *
 * Kutu oyunu seçilirse sepet tutarı (bahşiş hariç) o oyunun minimum tutarına
 * ulaşmadan sipariş tamamlanamaz. Ör. Okey için en az 300 TL.
 */
public class SiparisPaneli extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color UYARI_RENGI = new Color(170, 60, 50);
    private static final Color TAMAM_RENGI = new Color(70, 120, 60);

    private final UrunIslemleri urunIslemleri;
    private final SiparisIslemleri siparisIslemleri;
    private final Consumer<String> durum;

    // ---- Sol: menü ----
    private final FiltreCubugu filtre;
    final UrunTabloModeli menuModeli = new UrunTabloModeli();
    final JTable menuTablosu = new JTable(menuModeli);
    final JSpinner adetKutusu = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));

    // ---- Sağ: sepet ----
    private final List<SepetKalemi> sepet = new ArrayList<>();
    private final SepetModeli sepetModeli = new SepetModeli();
    private final JTable sepetTablosu = new JTable(sepetModeli);
    final JComboBox<KutuOyunu> oyunKutusu = new JComboBox<>();
    private final JLabel oyunDurumu = new JLabel(" ");
    final JComboBox<String> bahsisKutusu = new JComboBox<>(
            new String[] { "Bahşiş yok", "%5", "%10", "%15", "Tutarı ben yazacağım" });
    final JTextField bahsisTutari = new JTextField(6);
    private final JLabel araToplamYazisi = new JLabel();
    private final JLabel bahsisYazisi = new JLabel();
    private final JLabel toplamYazisi = new JLabel();

    public SiparisPaneli(UrunIslemleri urunIslemleri, SiparisIslemleri siparisIslemleri, Consumer<String> durum) {
        super(new BorderLayout(14, 0));
        this.urunIslemleri = urunIslemleri;
        this.siparisIslemleri = siparisIslemleri;
        this.durum = durum;
        this.filtre = new FiltreCubugu(this::listele);

        setBackground(Tema.KREM);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        add(menuBolumu(), BorderLayout.CENTER);
        add(sepetBolumu(), BorderLayout.EAST);
        hesapla();
    }

    // =====================================================================
    //  EKRAN PARÇALARI
    // =====================================================================

    private JPanel menuBolumu() {
        JPanel bolum = new JPanel(new BorderLayout(0, 6));
        bolum.setOpaque(false);

        JPanel ust = new JPanel(new BorderLayout());
        ust.setOpaque(false);
        ust.add(Tema.baslik("Menü"), BorderLayout.NORTH);
        ust.add(filtre, BorderLayout.CENTER);
        bolum.add(ust, BorderLayout.NORTH);

        Tema.tabloyuSusle(menuTablosu);
        menuTablosu.getColumnModel().getColumn(0).setPreferredWidth(260);
        menuTablosu.getColumnModel().getColumn(1).setPreferredWidth(160);
        menuTablosu.getColumnModel().getColumn(2).setPreferredWidth(110);
        menuTablosu.getColumnModel().getColumn(2).setCellRenderer(Tema.paraGosterici());
        menuTablosu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent olay) {
                if (olay.getClickCount() == 2) { // çift tık = sepete ekle
                    sepeteEkle();
                }
            }
        });
        bolum.add(new JScrollPane(menuTablosu), BorderLayout.CENTER);

        JPanel alt = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        alt.setOpaque(false);
        alt.add(Tema.etiket("Adet:"));
        adetKutusu.setFont(Tema.YAZI);
        alt.add(adetKutusu);
        JButton ekleDugmesi = Tema.dugme("Sepete Ekle", Tema.YESIL);
        ekleDugmesi.addActionListener(olay -> sepeteEkle());
        alt.add(ekleDugmesi);
        alt.add(Tema.ipucu("  İpucu: Çift tıklama da sepete ekler."));
        bolum.add(alt, BorderLayout.SOUTH);
        return bolum;
    }

    private JPanel sepetBolumu() {
        JPanel bolum = new JPanel(new GridBagLayout());
        bolum.setBackground(Color.WHITE);
        bolum.setPreferredSize(new Dimension(420, 0));
        bolum.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.SUTLU, 2, true),
                new EmptyBorder(10, 12, 10, 12)));

        GridBagConstraints k = new GridBagConstraints();
        k.gridx = 0;
        k.gridwidth = 2;
        k.fill = GridBagConstraints.HORIZONTAL;
        k.insets = new Insets(3, 0, 3, 0);

        k.gridy = 0;
        bolum.add(Tema.baslik("Sepet"), k);

        // Sepet tablosu: Ürün | Adet | Tutar
        Tema.tabloyuSusle(sepetTablosu);
        sepetTablosu.getColumnModel().getColumn(0).setPreferredWidth(200);
        sepetTablosu.getColumnModel().getColumn(1).setPreferredWidth(55);
        sepetTablosu.getColumnModel().getColumn(2).setPreferredWidth(110);
        sepetTablosu.getColumnModel().getColumn(1).setCellRenderer(Tema.ortaGosterici());
        sepetTablosu.getColumnModel().getColumn(2).setCellRenderer(Tema.paraGosterici());
        k.gridy = 1;
        k.weighty = 1;
        k.fill = GridBagConstraints.BOTH;
        bolum.add(new JScrollPane(sepetTablosu), k);
        k.weighty = 0;
        k.fill = GridBagConstraints.HORIZONTAL;

        JPanel sepetDugmeleri = new JPanel(new GridLayout(1, 2, 8, 0));
        sepetDugmeleri.setOpaque(false);
        JButton cikarDugmesi = Tema.dugme("Seçileni Çıkar", Tema.GRI);
        cikarDugmesi.addActionListener(olay -> seciliyiCikar());
        JButton bosaltDugmesi = Tema.dugme("Sepeti Boşalt", Tema.GRI);
        bosaltDugmesi.addActionListener(olay -> sepetiBosalt());
        sepetDugmeleri.add(cikarDugmesi);
        sepetDugmeleri.add(bosaltDugmesi);
        k.gridy = 2;
        bolum.add(sepetDugmeleri, k);

        // Kutu oyunu
        k.gridy = 3;
        k.insets = new Insets(12, 0, 3, 0);
        bolum.add(Tema.etiket("Kutu oyunu"), k);
        k.insets = new Insets(3, 0, 3, 0);
        oyunKutusu.setFont(Tema.YAZI);
        oyunKutusu.addActionListener(olay -> hesapla());
        k.gridy = 4;
        bolum.add(oyunKutusu, k);
        oyunDurumu.setFont(new Font("Segoe UI", Font.BOLD, 13));
        k.gridy = 5;
        bolum.add(oyunDurumu, k);

        // Bahşiş
        k.gridy = 6;
        k.insets = new Insets(10, 0, 3, 0);
        bolum.add(Tema.etiket("Bahşiş"), k);
        k.insets = new Insets(3, 0, 3, 0);
        JPanel bahsisSatiri = new JPanel(new BorderLayout(8, 0));
        bahsisSatiri.setOpaque(false);
        bahsisKutusu.setFont(Tema.YAZI);
        bahsisTutari.setFont(Tema.YAZI);
        bahsisTutari.setEnabled(false);
        bahsisKutusu.addActionListener(olay -> {
            bahsisTutari.setEnabled(bahsisKutusu.getSelectedIndex() == 4);
            hesapla();
        });
        bahsisTutari.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { hesapla(); }
            @Override public void removeUpdate(DocumentEvent e)  { hesapla(); }
            @Override public void changedUpdate(DocumentEvent e) { hesapla(); }
        });
        bahsisSatiri.add(bahsisKutusu, BorderLayout.CENTER);
        JPanel tutarKutusu = new JPanel(new BorderLayout(4, 0));
        tutarKutusu.setOpaque(false);
        tutarKutusu.add(bahsisTutari, BorderLayout.CENTER);
        tutarKutusu.add(Tema.etiket("TL"), BorderLayout.EAST);
        bahsisSatiri.add(tutarKutusu, BorderLayout.EAST);
        k.gridy = 7;
        bolum.add(bahsisSatiri, k);

        k.gridy = 8;
        k.insets = new Insets(10, 0, 6, 0);
        bolum.add(new JSeparator(), k);

        // Toplamlar: solda yazı, sağda tutar
        k.insets = new Insets(2, 0, 2, 0);
        k.gridwidth = 1;
        k.weightx = 1;
        toplamSatiri(bolum, k, 9, "Ara toplam", araToplamYazisi, false);
        toplamSatiri(bolum, k, 10, "Bahşiş", bahsisYazisi, false);
        toplamSatiri(bolum, k, 11, "TOPLAM", toplamYazisi, true);

        k.gridx = 0;
        k.gridwidth = 2;
        k.gridy = 12;
        k.insets = new Insets(12, 0, 0, 0);
        JButton tamamlaDugmesi = Tema.dugme("Siparişi Tamamla", Tema.YESIL);
        tamamlaDugmesi.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tamamlaDugmesi.setBorder(new EmptyBorder(12, 14, 12, 14));
        tamamlaDugmesi.addActionListener(olay -> siparisiTamamla());
        bolum.add(tamamlaDugmesi, k);
        return bolum;
    }

    private void toplamSatiri(JPanel bolum, GridBagConstraints k, int satir, String yazi, JLabel tutar, boolean buyuk) {
        JLabel etiket = Tema.etiket(yazi);
        tutar.setHorizontalAlignment(JLabel.RIGHT);
        tutar.setForeground(Tema.KAHVE);
        if (buyuk) {
            etiket.setFont(new Font("Georgia", Font.BOLD, 18));
            tutar.setFont(new Font("Georgia", Font.BOLD, 20));
        } else {
            tutar.setFont(Tema.YAZI_KALIN);
        }
        k.gridy = satir;
        k.gridx = 0;
        bolum.add(etiket, k);
        k.gridx = 1;
        bolum.add(tutar, k);
    }

    // =====================================================================
    //  İŞLEMLER
    // =====================================================================

    /** Sekme açıldığında: kategorileri, oyunları ve menüyü veritabanından yeniden okur. */
    public void yenile() {
        try {
            filtre.kategorileriYukle(urunIslemleri.kategoriler());
            oyunlariYukle();
            listele();
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }

    /** "Listele": filtreye uyan ürünleri menü tablosuna getirir. */
    void listele() {
        try {
            List<Urun> urunler = urunIslemleri.listele(filtre.getKategori(), filtre.getArama());
            menuModeli.setUrunler(urunler);
            durum.accept(urunler.isEmpty() ? "Aramanıza uygun ürün bulunamadı."
                    : urunler.size() + " ürün listelendi.");
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }

    private void oyunlariYukle() throws SQLException {
        KutuOyunu onceki = (KutuOyunu) oyunKutusu.getSelectedItem();
        oyunKutusu.removeAllItems();
        oyunKutusu.addItem(KutuOyunu.yok());
        for (KutuOyunu oyun : siparisIslemleri.kutuOyunlari()) {
            oyunKutusu.addItem(oyun);
            if (onceki != null && oyun.getAd().equals(onceki.getAd())) {
                oyunKutusu.setSelectedItem(oyun);
            }
        }
    }

    /** Menüde seçili üründen, "Adet" kutusundaki kadar sepete ekler. */
    void sepeteEkle() {
        int satir = menuTablosu.getSelectedRow();
        if (satir < 0) {
            Tema.uyar(this, "Lütfen önce menüden bir ürün seçin.");
            return;
        }
        Urun urun = menuModeli.getUrun(satir);
        int adet = (Integer) adetKutusu.getValue();

        SepetKalemi mevcut = null;
        for (SepetKalemi kalem : sepet) {
            if (kalem.getUrun().getId() == urun.getId()) {
                mevcut = kalem;
            }
        }
        if (mevcut != null) {
            mevcut.adetArtir(adet);      // aynı ürün zaten sepetteyse adedi artır
        } else {
            sepet.add(new SepetKalemi(urun, adet));
        }
        sepetModeli.fireTableDataChanged();
        adetKutusu.setValue(1);
        hesapla();
        durum.accept(adet + " x " + urun.getAd() + " sepete eklendi.");
    }

    private void seciliyiCikar() {
        int satir = sepetTablosu.getSelectedRow();
        if (satir < 0) {
            Tema.uyar(this, "Çıkarmak için sepetten bir satır seçin.");
            return;
        }
        SepetKalemi cikan = sepet.remove(satir);
        sepetModeli.fireTableDataChanged();
        hesapla();
        durum.accept(cikan.getUrun().getAd() + " sepetten çıkarıldı.");
    }

    private void sepetiBosalt() {
        sepet.clear();
        sepetModeli.fireTableDataChanged();
        oyunKutusu.setSelectedIndex(0);
        bahsisKutusu.setSelectedIndex(0);
        bahsisTutari.setText("");
        hesapla();
    }

    /** Ara toplamı, bahşişi, toplamı ve kutu oyunu durumunu yeniden hesaplayıp ekrana yazar. */
    void hesapla() {
        BigDecimal ara = SiparisIslemleri.araToplam(sepet);
        BigDecimal bahsis = bahsisHesapla(ara);

        araToplamYazisi.setText(Tema.tl(ara));
        bahsisYazisi.setText(bahsis == null ? "geçersiz tutar" : Tema.tl(bahsis));
        toplamYazisi.setText(Tema.tl(bahsis == null ? ara : ara.add(bahsis)));

        KutuOyunu oyun = (KutuOyunu) oyunKutusu.getSelectedItem();
        if (oyun == null || oyun.oyunYok()) {
            oyunDurumu.setText(" ");
        } else if (ara.compareTo(oyun.getMinSepet()) >= 0) {
            oyunDurumu.setForeground(TAMAM_RENGI);
            oyunDurumu.setText("Sepet yeterli, " + oyun.getAd() + " oynayabilirsiniz.");
        } else {
            BigDecimal eksik = oyun.getMinSepet().subtract(ara);
            oyunDurumu.setForeground(UYARI_RENGI);
            oyunDurumu.setText("<html>" + oyun.getAd() + " için sepet en az " + Tema.tl(oyun.getMinSepet())
                    + " olmalı.<br>" + Tema.tl(eksik) + " daha ürün ekleyin.</html>");
        }
    }

    /** Seçilen bahşişi TL olarak döndürür. Elle yazılan tutar hatalıysa null. */
    private BigDecimal bahsisHesapla(BigDecimal ara) {
        switch (bahsisKutusu.getSelectedIndex()) {
            case 1:  return yuzdesi(ara, 5);
            case 2:  return yuzdesi(ara, 10);
            case 3:  return yuzdesi(ara, 15);
            case 4: {
                if (bahsisTutari.getText().isBlank()) {
                    return BigDecimal.ZERO;
                }
                BigDecimal tutar = Tema.sayiOku(bahsisTutari.getText());
                return (tutar == null || tutar.signum() < 0) ? null : tutar;
            }
            default: return BigDecimal.ZERO;
        }
    }

    private static BigDecimal yuzdesi(BigDecimal tutar, int yuzde) {
        return tutar.multiply(BigDecimal.valueOf(yuzde)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** "Siparişi Tamamla": kontrolleri yapar ve siparişi MySQL'e kaydeder. */
    private void siparisiTamamla() {
        if (sepet.isEmpty()) {
            Tema.uyar(this, "Sepet boş. Önce menüden ürün ekleyin.");
            return;
        }
        BigDecimal ara = SiparisIslemleri.araToplam(sepet);
        BigDecimal bahsis = bahsisHesapla(ara);
        if (bahsis == null) {
            Tema.uyar(this, "Bahşiş tutarı geçersiz. Örnek: 50 veya 37,50");
            return;
        }

        // Kutu oyunu kuralı: sepet (bahşiş hariç) en az oyunun minimum tutarı kadar olmalı
        KutuOyunu oyun = (KutuOyunu) oyunKutusu.getSelectedItem();
        if (oyun != null && !oyun.oyunYok() && ara.compareTo(oyun.getMinSepet()) < 0) {
            BigDecimal eksik = oyun.getMinSepet().subtract(ara);
            Tema.uyar(this, oyun.getAd() + " oynamak için sepet tutarı en az " + Tema.tl(oyun.getMinSepet()) + " olmalı.\n"
                    + "Şu anki sepet: " + Tema.tl(ara) + "\n"
                    + "Lütfen " + Tema.tl(eksik) + " daha ürün ekleyin (bahşiş bu tutara sayılmaz).");
            return;
        }

        BigDecimal toplam = ara.add(bahsis);
        String ozet = "Ara toplam: " + Tema.tl(ara) + "\nBahşiş: " + Tema.tl(bahsis) + "\nTOPLAM: " + Tema.tl(toplam)
                + ((oyun != null && !oyun.oyunYok()) ? "\nKutu oyunu: " + oyun.getAd() : "")
                + "\n\nSipariş kaydedilsin mi?";
        if (JOptionPane.showConfirmDialog(this, ozet, "Siparişi onayla", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int siparisNo = siparisIslemleri.siparisKaydet(new ArrayList<>(sepet), bahsis, oyun);
            Tema.bilgi(this, "Sipariş kaydedildi! (Sipariş no: " + siparisNo + ")\nToplam: " + Tema.tl(toplam)
                    + "\n\nMySQL'de 'siparisler' ve 'siparis_kalemleri' tablolarında görebilirsiniz.");
            sepetiBosalt();
            durum.accept("Sipariş #" + siparisNo + " kaydedildi, toplam " + Tema.tl(toplam) + ".");
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }

    /** Sepet tablosunun modeli: Ürün | Adet | Tutar */
    private class SepetModeli extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] sutunlar = { "Ürün", "Adet", "Tutar" };

        @Override public int getRowCount()    { return sepet.size(); }
        @Override public int getColumnCount() { return sutunlar.length; }
        @Override public String getColumnName(int sutun) { return sutunlar[sutun]; }

        @Override
        public Object getValueAt(int satir, int sutun) {
            SepetKalemi kalem = sepet.get(satir);
            switch (sutun) {
                case 0:  return kalem.getUrun().getAd();
                case 1:  return kalem.getAdet();
                default: return kalem.getTutar();
            }
        }
    }
}
