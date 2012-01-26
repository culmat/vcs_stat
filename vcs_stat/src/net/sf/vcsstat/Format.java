

package net.sf.vcsstat;


/**
 * @author mcullman
 *  Helper class for converting into human readable formats
 */

public class Format {
    /**
     * Only static methods.
     */
    private Format() {
    }

    /**
     * The byte units.
     */
    private static final String[] BYTE_UNITS = new String[]{"b", "Kb", "Mb",
            "Gb", };

    /**
     * Time units.
     */
    private static final String[] TIME_UNITS = new String[]{"msec", "sec",
            "min", "h", "day", "week", "year", };

    /**
     * Time steps.
     */
    private static final int[] TIME_STEPS = new int[]{1000, 60, 60, 24, 7, 52 };

    /**
     * Formats the given value into units separated by factors step.
     * 
     * @param unit The unit
     * @param step The step
     * @param value The value
     * @return The formated String
     */
    public static String format(String[] unit, int[] step, double value) {
        int i = 0;
        while (i < unit.length - 1 && value >= step[i]) {
            value = value / step[i++];
        }
        return String.format("%2.2f", value) + " " + unit[i];
    }

    /**
     * Formats the given value into units separated by a constant factor step.
     * 
     * @param unit The unit
     * @param step The step
     * @param value The value
     * @return The formated String
     */
    public static String format(String[] unit, int step, double value) {
        int i = 0;
        while (i < unit.length - 1 && value >= step) {
            value = value / step;
            i++;
        }
        return String.format("%2.2f", value) + " " + unit[i];
    }

    /**
     * Formats bytes.
     * 
     * @param value The value
     * @return The formated String
     */
    public static String bytes(double value) {
        return format(BYTE_UNITS, 1024, value);
    }

    /**
     * Formats time.
     * 
     * @param value the time in milliseconds
     * @return The formated String
     */
    public static String time(double value) {
        return format(TIME_UNITS, TIME_STEPS, value);
    }

    /**
     * Formats time.
     * 
     * @param value the time in nanoseconds
     * @return The formated String
     */
    public static String nanotime(double value) {
        return time(value / 1000);
    }

}
