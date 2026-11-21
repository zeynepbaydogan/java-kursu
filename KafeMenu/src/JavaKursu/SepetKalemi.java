package JavaKursu;

import java.math.BigDecimal;

/** Sepetteki bir satır: hangi üründen kaç adet. */
public class SepetKalemi {

    private final Urun urun;
    private int adet;

    public SepetKalemi(Urun urun, int adet) {
        this.urun = urun;
        this.adet = adet;
    }

    public Urun getUrun() {
        return urun;
    }

    public int getAdet() {
        return adet;
    }

    public void adetArtir(int eklenecek) {
        adet += eklenecek;
    }

    /** Birim fiyat x adet */
    public BigDecimal getTutar() {
        return urun.getFiyat().multiply(BigDecimal.valueOf(adet));
    }
}
