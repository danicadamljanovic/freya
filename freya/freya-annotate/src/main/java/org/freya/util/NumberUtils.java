package org.freya.util;

import java.text.DecimalFormat;
/**
 * 
 * @author danica
 *
 */
public class NumberUtils {
  public static double roundTwoDecimals(double d) {
    DecimalFormat twoDForm = new DecimalFormat("#.##");
    return Double.valueOf(twoDForm.format(d));
  }
}
