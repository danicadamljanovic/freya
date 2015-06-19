package org.freya.util;

import java.util.Comparator;

import org.freya.model.Vote;

public class VoteComparator implements Comparator {
    
    public int compare(Object o1, Object o2){
        Vote voteOne = (Vote)o1;
        Vote voteTwo = (Vote)o2;
        int result=0;
        // compare scores
        Double score1 = voteOne.getVote();
        Double score2 = voteTwo.getVote();
        if (score1==null)
            score1=new Double(0);
        if (score2==null)
            score2=new Double(0);
            result = score2.compareTo(
                score1);
        return result;
      }
}

