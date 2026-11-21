package JavaKursu;

import java.math.BigDecimal;

/**
 * Kafedeki bir kutu oyunu (Okey, Tavla, Monopoly ...).
 * Her oyunun bir minimum sepet tutarı vardır: o oyunu oynamak için
 * siparişin (bahşiş hariç) en az bu tutarda olması gerekir.
 */
public class KutuOyunu {

    private final int id;
    private final String ad;
    private final BigDecimal minSepet;

    public KutuOyunu(int id, String ad, BigDecimal minSepet) {
        this.id = id;
        this.ad = ad;
        this.minSepet = minSepet;
    }

    /** Açılır listedeki "oyun istemiyorum" seçeneği. */
    public static KutuOyunu yok() {
        return new KutuOyunu(0, "Kutu oyunu istemiyorum", BigDecimal.ZERO);
    }

    public boolean oyunYok() {
        return id == 0;
    }

    public int getId() {
        return id;
    }

    public String getAd() {
        return ad;
    }

    public BigDecimal getMinSepet() {
        return minSepet;
    }

    /** Açılır listede böyle görünür: "Okey   (en az 300,00 TL sepet)" */
    @Override
    public String toString() {
        return oyunYok() ? ad : ad + "   (en az " + Tema.tl(minSepet) + " sepet)";
    }
}
