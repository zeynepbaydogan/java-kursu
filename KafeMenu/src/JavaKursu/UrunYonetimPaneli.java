package JavaKursu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * "Ürün Yönetimi" sekmesi: menüyü listele, ürün ekle, güncelle, sil, indirim/zam yap.
 * Ekleme ve güncelleme ayrı küçük bir pencerede yapılır; ekranın yanında form durmaz.
 */
public class UrunYonetimPaneli extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final BigDecimal EN_YUKSEK_FIYAT = new BigDecimal("999999.99");

    private final UrunIslemleri islemler;
    private final Consumer<String> durum;

    private final FiltreCubugu filtre;
    private final UrunTabloModeli model = new UrunTabloModeli();
    private final JTable tablo = new JTable(model);
    private List<String> kategoriler = List.of();

    public UrunYonetimPaneli(UrunIslemleri islemler, Consumer<String> durum) {
        super(new BorderLayout(0, 8));
        this.islemler = islemler;
        this.durum = durum;
        this.filtre = new FiltreCubugu(this::listele);

        setBackground(Tema.KREM);
        setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel ust = new JPanel(new BorderLayout());
        ust.setOpaque(false);
        ust.add(Tema.baslik("Ürün Yönetimi"), BorderLayout.NORTH);
        ust.add(filtre, BorderLayout.CENTER);
        add(ust, BorderLayout.NORTH);

        Tema.tabloyuSusle(tablo);
        tablo.getColumnModel().getColumn(0).setPreferredWidth(320);
        tablo.getColumnModel().getColumn(1).setPreferredWidth(200);
        tablo.getColumnModel().getColumn(2).setPreferredWidth(120);
        tablo.getColumnModel().getColumn(2).setCellRenderer(Tema.paraGosterici());
        tablo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent olay) {
                if (olay.getClickCount() == 2) {
                    urunGuncelle(); // çift tık = güncelle
                }
            }
        });
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        JPanel dugmeler = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        dugmeler.setOpaque(false);
        JButton ekle = Tema.dugme("Yeni Ürün Ekle", Tema.YESIL);
        ekle.addActionListener(olay -> urunEkle());
        JButton guncelle = Tema.dugme("Seçili Ürünü Güncelle", Tema.KAHVE);
        guncelle.addActionListener(olay -> urunGuncelle());
        JButton sil = Tema.dugme("Seçili Ürünü Sil", Tema.KIRMIZI);
        sil.addActionListener(olay -> urunSil());
        JButton indirimZam = Tema.dugme("İndirim / Zam Yap", Tema.KARAMEL);
        indirimZam.addActionListener(olay -> indirimZamYap());
        dugmeler.add(ekle);
        dugmeler.add(guncelle);
        dugmeler.add(sil);
        dugmeler.add(indirimZam);
        dugmeler.add(Tema.ipucu("  İpucu: Çift tıklama = güncelle"));
        add(dugmeler, BorderLayout.SOUTH);
    }

    /** Sekme açıldığında kategorileri ve listeyi yeniler. */
    public void yenile() {
        try {
            kategoriler = islemler.kategoriler();
            filtre.kategorileriYukle(kategoriler);
            listele();
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }

    void listele() {
        try {
            List<Urun> urunler = islemler.listele(filtre.getKategori(), filtre.getArama());
            model.setUrunler(urunler);
            durum.accept(urunler.isEmpty() ? "Aramanıza uygun ürün bulunamadı." : urunler.size() + " ürün listelendi.");
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }

    // =====================================================================
    //  EKLE / GÜNCELLE / SİL
    // =====================================================================

    private void urunEkle() {
        Urun yeni = urunPenceresi("Yeni Ürün Ekle", null);
        if (yeni == null) {
            return; // vazgeçildi
        }
        try {
            islemler.ekle(yeni);
            yenile();
            sec(yeni.getAd());
            durum.accept("\"" + yeni.getAd() + "\" menüye eklendi.");
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }

    private void urunGuncelle() {
        Urun secili = seciliUrun();
        if (secili == null) {
            return;
        }
        Urun yeni = urunPenceresi("Ürünü Güncelle: " + secili.getAd(), secili);
        if (yeni == null) {
            return;
        }
        try {
            if (islemler.guncelle(secili.getId(), yeni.getAd(), yeni.getKategori(), yeni.getFiyat())) {
                yenile();
                sec(yeni.getAd());
                durum.accept("\"" + yeni.getAd() + "\" güncellendi.");
            } else {
                Tema.uyar(this, "Bu ürün artık veritabanında yok; liste yenilendi.");
                yenile();
            }
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }

    private void urunSil() {
        Urun secili = seciliUrun();
        if (secili == null) {
            return;
        }
        int cevap = JOptionPane.showConfirmDialog(this,
                "\"" + secili.getAd() + "\" menüden silinsin mi?", "Silme onayı",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (cevap != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            islemler.sil(secili.getId());
            listele();
            durum.accept("\"" + secili.getAd() + "\" menüden silindi.");
        } catch (SQLException e) {
            Tema.hata(this, e);
        }
    }

    /**
     * Ürün bilgileri penceresi (ekleme ve güncelleme için ortak).
     * Hatalı bilgi girilirse uyarı gösterir ve pencereyi aynı bilgilerle tekrar açar.
     * "İptal" edilirse null döner.
     */
    private Urun urunPenceresi(String baslik, Urun mevcut) {
        JTextField adKutusu = new JTextField(mevcut == null ? "" : mevcut.getAd(), 22);
        JComboBox<String> kategoriKutusu = new JComboBox<>(kategoriler.toArray(new String[0]));
        kategoriKutusu.setEditable(true); // listede olmayan yeni bir kategori de yazılabilir
        if (mevcut != null) {
            kategoriKutusu.setSelectedItem(mevcut.getKategori());
        } else if (filtre.getKategori() != null) {
            kategoriKutusu.setSelectedItem(filtre.getKategori());
        }
        JTextField fiyatKutusu = new JTextField(
                mevcut == null ? "" : mevcut.getFiyat().toPlainString().replace('.', ','), 10);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints k = new GridBagConstraints();
        k.anchor = GridBagConstraints.WEST;
        k.fill = GridBagConstraints.HORIZONTAL;
        k.insets = new Insets(5, 4, 5, 4);
        formSatiri(form, k, 0, "Ürün adı:", adKutusu);
        formSatiri(form, k, 1, "Kategori:", kategoriKutusu);
        formSatiri(form, k, 2, "Fiyat (TL):", fiyatKutusu);
        k.gridx = 0;
        k.gridy = 3;
        k.gridwidth = 2;
        form.add(Tema.ipucu("Listede olmayan bir kategori yazarsanız yeni kategori olarak eklenir."), k);

        while (true) {
            int secim = JOptionPane.showConfirmDialog(this, form, baslik,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (secim != JOptionPane.OK_OPTION) {
                return null;
            }
            String ad = adKutusu.getText().trim();
            Object kategoriYazisi = kategoriKutusu.getEditor().getItem();
            String kategori = kategoriYazisi == null ? "" : kategoriYazisi.toString().trim();
            BigDecimal fiyat = Tema.sayiOku(fiyatKutusu.getText());

            if (ad.isEmpty() || ad.length() > 100) {
                Tema.uyar(this, "Ürün adı boş olamaz (en fazla 100 karakter).");
            } else if (kategori.isEmpty() || kategori.length() > 50) {
                Tema.uyar(this, "Lütfen bir kategori seçin veya yazın (en fazla 50 karakter).");
            } else if (fiyat == null || fiyat.signum() <= 0 || fiyat.compareTo(EN_YUKSEK_FIYAT) > 0) {
                Tema.uyar(this, "Fiyat 0'dan büyük bir sayı olmalı. Örnek: 85 veya 85,50");
            } else {
                return new Urun(mevcut == null ? 0 : mevcut.getId(), ad, kategori, fiyat);
            }
        }
    }

    // =====================================================================
    //  İNDİRİM / ZAM
    // =====================================================================

    private void indirimZamYap() {
        int satir = tablo.getSelectedRow();
        Urun secili = satir < 0 ? null : model.getUrun(satir);

        JRadioButton indirimSecenegi = new JRadioButton("İndirim (fiyat düşer)", true);
        JRadioButton zamSecenegi = new JRadioButton("Zam (fiyat artar)");
        ButtonGroup turGrubu = new ButtonGroup();
        turGrubu.add(indirimSecenegi);
        turGrubu.add(zamSecenegi);

        JTextField yuzdeKutusu = new JTextField("10", 5);

        JRadioButton tekUrun = new JRadioButton(secili == null
                ? "Sadece seçili ürün (önce listeden bir ürün seçin)"
                : "Sadece seçili ürün: " + secili.getAd());
        tekUrun.setEnabled(secili != null);
        JRadioButton kategoriSecenegi = new JRadioButton("Bir kategorinin tamamı:");
        JComboBox<String> kategoriKutusu = new JComboBox<>(kategoriler.toArray(new String[0]));
        if (secili != null) {
            kategoriKutusu.setSelectedItem(secili.getKategori());
        } else if (filtre.getKategori() != null) {
            kategoriKutusu.setSelectedItem(filtre.getKategori());
        }
        JRadioButton tumMenu = new JRadioButton("Tüm menü");
        ButtonGroup kapsamGrubu = new ButtonGroup();
        kapsamGrubu.add(tekUrun);
        kapsamGrubu.add(kategoriSecenegi);
        kapsamGrubu.add(tumMenu);
        (secili != null ? tekUrun : kategoriSecenegi).setSelected(true);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints k = new GridBagConstraints();
        k.anchor = GridBagConstraints.WEST;
        k.insets = new Insets(4, 4, 4, 4);
        k.gridx = 0;
        k.gridy = 0;
        k.gridwidth = 2;
        form.add(Tema.etiket("Ne yapılsın?"), k);
        k.gridy = 1;
        form.add(indirimSecenegi, k);
        k.gridy = 2;
        form.add(zamSecenegi, k);
        k.gridwidth = 1;
        formSatiri(form, k, 3, "Yüzde (%):", yuzdeKutusu);
        k.gridx = 0;
        k.gridwidth = 2;
        k.gridy = 4;
        k.insets = new Insets(12, 4, 4, 4);
        form.add(Tema.etiket("Hangi ürünlere?"), k);
        k.insets = new Insets(4, 4, 4, 4);
        k.gridy = 5;
        form.add(tekUrun, k);
        k.gridy = 6;
        k.gridwidth = 1;
        form.add(kategoriSecenegi, k);
        k.gridx = 1;
        form.add(kategoriKutusu, k);
        k.gridx = 0;
        k.gridy = 7;
        k.gridwidth = 2;
        form.add(tumMenu, k);

        while (true) {
            int secim = JOptionPane.showConfirmDialog(this, form, "İndirim / Zam",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (secim != JOptionPane.OK_OPTION) {
                return;
            }
            boolean zam = zamSecenegi.isSelected();
            BigDecimal yuzde = Tema.sayiOku(yuzdeKutusu.getText());
            BigDecimal ustSinir = new BigDecimal(zam ? "200" : "90");
            if (yuzde == null || yuzde.signum() <= 0 || yuzde.compareTo(ustSinir) > 0) {
                Tema.uyar(this, (zam ? "Zam" : "İndirim") + " yüzdesi 0 ile " + ustSinir + " arasında olmalı. Örnek: 10");
                continue;
            }

            try {
                List<Urun> hedefler;
                String kapsam;
                if (tekUrun.isSelected() && secili != null) {
                    hedefler = List.of(secili);
                    kapsam = "\"" + secili.getAd() + "\" ürününe";
                } else if (kategoriSecenegi.isSelected()) {
                    String kategori = (String) kategoriKutusu.getSelectedItem();
                    hedefler = islemler.listele(kategori, null);
                    kapsam = kategori + " kategorisindeki " + hedefler.size() + " ürüne";
                } else {
                    hedefler = islemler.listele();
                    kapsam = "tüm menüdeki " + hedefler.size() + " ürüne";
                }
                if (hedefler.isEmpty()) {
                    Tema.uyar(this, "Seçtiğiniz yerde hiç ürün yok.");
                    continue;
                }

                String yuzdeYazisi = "%" + yuzde.stripTrailingZeros().toPlainString();
                Urun ornek = hedefler.get(0);
                String soru = kapsam + " " + yuzdeYazisi + (zam ? " zam" : " indirim") + " uygulanacak.\n\n"
                        + "Örnek: " + ornek.getAd() + "  " + Tema.tl(ornek.getFiyat()) + "  ->  "
                        + Tema.tl(UrunIslemleri.yeniFiyat(ornek.getFiyat(), zam, yuzde)) + "\n\nOnaylıyor musunuz?";
                if (JOptionPane.showConfirmDialog(this, soru, "Onay", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }

                int degisen = islemler.fiyatlariDegistir(zam, yuzde, hedefler);
                listele();
                if (secili != null) {
                    sec(secili.getAd());
                }
                Tema.bilgi(this, degisen + " ürünün fiyatı güncellendi.\n"
                        + "Eski ve yeni fiyatlar MySQL'de 'islem_gecmisi' tablosuna kaydedildi.");
                durum.accept(degisen + " ürüne " + yuzdeYazisi + (zam ? " zam" : " indirim") + " uygulandı.");
            } catch (SQLException e) {
                Tema.hata(this, e);
            }
            return;
        }
    }

    // ---- yardımcılar ----

    private void formSatiri(JPanel form, GridBagConstraints k, int satir, String yazi, JComponent alan) {
        k.gridy = satir;
        k.gridx = 0;
        k.gridwidth = 1;
        JLabel etiket = Tema.etiket(yazi);
        form.add(etiket, k);
        k.gridx = 1;
        alan.setFont(Tema.YAZI);
        form.add(alan, k);
    }

    private Urun seciliUrun() {
        int satir = tablo.getSelectedRow();
        if (satir < 0) {
            Tema.uyar(this, "Lütfen önce listeden bir ürün seçin.");
            return null;
        }
        return model.getUrun(satir);
    }

    /** Adı verilen ürünü listede seçili hâle getirir ve görünür yapar. */
    private void sec(String urunAdi) {
        int satir = model.satirBul(urunAdi);
        if (satir >= 0) {
            tablo.setRowSelectionInterval(satir, satir);
            tablo.scrollRectToVisible(tablo.getCellRect(satir, 0, true));
        }
    }
}
