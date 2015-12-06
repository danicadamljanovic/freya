package org.freya.regex;

import org.apache.oro.text.awk.AwkCompiler;
import org.apache.oro.text.awk.AwkMatcher;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.StringSubstitution;
import org.apache.oro.text.regex.Substitution;
import org.apache.oro.text.regex.Util;
import org.freya.rdf.query.CATConstants;

/**
 * @author Danica Damljanovic
 */
public class ExpressionFinder {
    org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory
                    .getLog(ExpressionFinder.class);

    public ExpressionFinder() {}

    /**
     * This method finds camelCase words inside the input string and substitutes all occurencies with lowerCase
     * substitute (usually one space) upperCase Maching group is defined by regularExpression. Example:
     * inputString="detectCamelCaseWord" call method: String resultString = findAndSeparateCamelCases(inputString,
     * CloneQlConstants.REGEX_CAMEL_CASE, " "); Result: "detect Camel Case Word"
     * 
     * @param inputString
     * @param regularExpression
     * @return
     */
    public static String findAndSeparateCamelCases(String inputString,
                    String regularExpression, String substituteString) {
        String afterSubstitution = null;
        try {

            int matches = 0;
            Pattern pattern = null;
            PatternMatcherInput input;
            PatternCompiler compiler;
            PatternMatcher matcher;
            MatchResult result = null;
            // Create AwkCompiler and AwkMatcher instances.
            compiler = new AwkCompiler();
            matcher = new AwkMatcher();
            // Attempt to compile the pattern. If the pattern is not valid,
            // report the error and exit.
            try {
                pattern = compiler.compile(regularExpression);
            } catch (MalformedPatternException e) {
                System.err.println("Bad pattern.");
                System.err.println(e.getMessage());
                System.exit(1);
            }
            // remove all non-ascii chars as this awk library does support only
            // ascii
            // chars
            // TODO investigate whether it is important to handle non-ascii
            // chars
            // and if
            // yes use another library
            inputString = inputString.replaceAll("[^\\p{ASCII}]", "");
            input = new PatternMatcherInput(inputString);
            // System.out.println("\nPatternMatcherInput: " + input);
            // Loop until there are no more matches left.
            afterSubstitution = inputString;
            while (matcher.contains(input, pattern)) {
                // Since we're still in the loop, fetch match that was found.
                result = matcher.getMatch();
                ++matches;
                System.out.println("Input String " + inputString);
                System.out.println("Match " + matches + ": " + result);
                String foundMatch = result.toString();
                String[] chars = foundMatch.split("");
                String lowerCase = chars[0];
                String upperCase = chars[1];
                // adding space (substitute string) in between lower and upper
                // case
                StringBuffer substitute = new StringBuffer(lowerCase).append(
                                substituteString).append(upperCase);
                Substitution aSubstitution = new StringSubstitution(
                                substitute.toString());
                // Perform substitution and print result.
                String newString = Util.substitute(matcher, pattern,
                                aSubstitution, input.toString(), 1);
                afterSubstitution = newString;
                // System.out.println("newString: " + newString);
                input = new PatternMatcherInput(newString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // afterSubstitution = afterSubstitution.toLowerCase();
        return afterSubstitution;
    }

    /**
     * @param inputString
     * @param regularExpression
     * @return
     */
    public static boolean isCamelCase(String inputString,
                    String regularExpression) {
        boolean result = false;
        Pattern pattern = null;
        PatternMatcherInput input;
        PatternCompiler compiler;
        PatternMatcher matcher;
        // Create AwkCompiler and AwkMatcher instances.
        compiler = new AwkCompiler();
        matcher = new AwkMatcher();
        // Attempt to compile the pattern. If the pattern is not valid,
        // report the error and exit.
        try {
            pattern = compiler.compile(regularExpression);
        } catch (MalformedPatternException e) {
            System.err.println("Bad pattern.");
            System.err.println(e.getMessage());
            System.exit(1);
        }
        input = new PatternMatcherInput(inputString);
        // System.out.println("\nPatternMatcherInput: " + input);
        while (matcher.contains(input, pattern)) {
            result = true;
        }
        return result;
    }

    /**
     * Main method for testing.
     * 
     * @param args
     */
    public static final void main(String args[]) {
        String inputString = "camelCaseDetection";

        String inputString1 = "AI";
        String resultString = findAndSeparateCamelCases(inputString1,
                        CATConstants.REGEX_CAMEL_CASE, " ");
        System.out.println("input:" + inputString1 + " Output:" + resultString);

        // System.out.println(resultString);
    }
}
