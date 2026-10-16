package de.fabs.bounty;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Geld {

    private Geld() {
    }

    // Beträge werden intern als ganze Cent geführt, damit nichts durch Rundung verloren geht
    public static long parseCent(String text) {
        if (text == null || text.isBlank()) return -1;
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
        if (value.signum() <= 0) return -1;
        if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE / 100)) > 0) return -1;
        return value.setScale(2, RoundingMode.DOWN).movePointRight(2).longValueExact();
    }

    public static long toCent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) return 0;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.DOWN).movePointRight(2).longValue();
    }

    public static double toDouble(long cent) {
        return BigDecimal.valueOf(cent).movePointLeft(2).doubleValue();
    }
}
