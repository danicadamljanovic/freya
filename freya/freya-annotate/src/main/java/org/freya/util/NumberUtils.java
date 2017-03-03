package org.freya.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
/**
 * 
 * @author danica
 *
 */
public class NumberUtils {
	  public static double roundTwoDecimals(double d) {
		DecimalFormatSymbols decimalSymbols = new DecimalFormatSymbols(Locale.getDefault());
		decimalSymbols.setDecimalSeparator('.');
		decimalSymbols.setGroupingSeparator(' ');
	    DecimalFormat twoDForm = new DecimalFormat("#.##");
	    twoDForm.setGroupingUsed(false);
	    twoDForm.setDecimalFormatSymbols(decimalSymbols);
	    return Double.valueOf(twoDForm.format(d));
	  }
}
