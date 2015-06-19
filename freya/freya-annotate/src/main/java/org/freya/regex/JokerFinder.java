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
import org.freya.util.FreyaConstants;

public class JokerFinder {
  org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(JokerFinder.class);

  /**
   * This method finds groups of elements (as interpreted by regularExpression
   * parameter) inside the input string and substitutes all occurencies with
   * substitute:firstElement:secondElement:substitute. Maching group is defined
   * by regularExpression.
   * 
   * Example: inputString="c1-p12-i2-i3-i4-p7-i6-p9-i8" String resultString =
   * findAndReplaceOccurencies(inputString, CloneQlConstants.REGEX_FIND_JOKER,
   * "joker"); Result:
   * "c1-p12-i2-joker:i2:i3:joker-i3-joker:i3:i4:joker-i4-p7-i6-p9-i8"
   * 
   * @param inputString
   * @param regularExpression
   * @return
   */
  public static String findAndReplaceGroupOccurencies(String inputString,
    String regularExpression, String substituteString) {
    String afterSubstitution = null;
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
    }
    catch(MalformedPatternException e) {
      System.err.println("Bad pattern.");
      System.err.println(e.getMessage());
      System.exit(1);
    }
    input = new PatternMatcherInput(inputString);
    // System.out.println("\nPatternMatcherInput: " + input);
    // Loop until there are no more matches left.
    afterSubstitution = inputString;
    while(matcher.contains(input, pattern)) {
      // Since we're still in the loop, fetch match that was found.
      result = matcher.getMatch();
      ++matches;
      // System.out.println("Match " + matches + ": " + result);
      String foundMatch = result.toString();
      String[] aPairMember = foundMatch.split("-");
      StringBuffer substitute =
        new StringBuffer(aPairMember[0]).append("-").append(substituteString);

      String newMatch = foundMatch.replace("-", ":");

      substitute.append(":").append(newMatch);
      substitute.append(":").append(substituteString);
      substitute.append("-").append(aPairMember[1]);
      Substitution aSubstitution =
        new StringSubstitution(substitute.toString());
      // Perform substitution and print result.
      String newString =
        Util.substitute(matcher, pattern, aSubstitution, input.toString(), 1);
      afterSubstitution = newString;
      // System.out.println("newString: " + newString);
      input = new PatternMatcherInput(newString);
    }
    return afterSubstitution;
  }

  public static final void main(String args[]) {

    //String inputString = "c1-p12-i2-i3-i4-p7-i6-p9-i8";
   //String inputString = "c0-p1-p2-i3-";
   String inputString="c0-i1-";
    String resultString =
      findAndReplaceGroupOccurencies(inputString,
                      FreyaConstants.REGEX_FIND_JOKER, FreyaConstants.JOKER);
    System.out.println(resultString);
    resultString =
      findAndReplaceGroupOccurencies(resultString,
                      FreyaConstants.REGEX_FIND_CLASS_JOKER, FreyaConstants.JOKER);

    System.out.println(resultString);
  }
}
