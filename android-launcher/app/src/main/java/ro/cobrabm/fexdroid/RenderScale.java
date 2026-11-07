package ro.cobrabm.fexdroid;

/** Matches the guarded 01.00 patch generator; Android output size is independent. */
final class RenderScale {
    static final int MIN = 10, MAX = 100;
    static int units(int percent) {
        if (percent < MIN || percent > MAX) throw new IllegalArgumentException("Scale must be 10–100%");
        return (30 * percent + 50) / 100;
    }
    static int width(int percent) { return units(percent) * 64; }
    static int height(int percent) { return units(percent) * 36; }
    static String size(int percent) { return width(percent)+"x"+height(percent); }
    static String label(int percent) { return percent+"% · "+width(percent)+" × "+height(percent); }
}
