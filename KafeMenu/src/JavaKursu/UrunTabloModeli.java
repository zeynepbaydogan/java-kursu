package JavaKursu;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Ürün listesini tabloda gösterir: Ürün | Kategori | Fiyat.
 * Ürünün id numarası (1, 2, 3 ...) ekranda GÖSTERİLMEZ;
 * sadece içeride tutulur, güncelleme/silme için kullanılır.
 */
public class UrunTabloModeli extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] SUTUNLAR = { "Ürün", "Kategori", "Fiyat" };

    private List<Urun> urunler = new ArrayList<>();

    public void setUrunler(List<Urun> yeniListe) {
        urunler = new ArrayList<>(yeniListe);
        fireTableDataChanged(); // tabloya "veriler değişti, yeniden çiz" der
    }

    public Urun getUrun(int satir) {
        return urunler.get(satir);
    }

    /** Adı verilen ürünün satır numarası; yoksa -1. */
    public int satirBul(String urunAdi) {
        for (int i = 0; i < urunler.size(); i++) {
            if (urunler.get(i).getAd().equalsIgnoreCase(urunAdi)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return urunler.size();
    }

    @Override
    public int getColumnCount() {
        return SUTUNLAR.length;
    }

    @Override
    public String getColumnName(int sutun) {
        return SUTUNLAR[sutun];
    }

    @Override
    public Object getValueAt(int satir, int sutun) {
        Urun urun = urunler.get(satir);
        switch (sutun) {
            case 0:  return urun.getAd();
            case 1:  return urun.getKategori();
            default: return urun.getFiyat();
        }
    }
}
