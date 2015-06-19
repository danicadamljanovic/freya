package org.freya.util;

import java.util.List;

import org.freya.model.SemanticConcept;

public class ProfilerUtil {
 public static String profileString(String sessionId, String query, long runTime, String[] voteIds){
    StringBuilder sb = new StringBuilder();
    sb.append("SESSION ID: ");
    sb.append(sessionId);
    sb.append("; QUERY: ");
    sb.append(query);
    if (voteIds!=null){
    sb.append("; VOTE ID: ");
    for (String id:voteIds){
    sb.append(id).append(" ");
    }
    }
    sb.append("; TIME: ");
    sb.append(runTime);
    return sb.toString();
  }
 
 public static String profileSelection(long runTime, String sessionId, String query, List<SemanticConcept> candidates, Integer[] ranks){
   StringBuilder sb = new StringBuilder();
   sb.append("SESSION ID: ");
   sb.append(sessionId);
   sb.append("; QUERY: ");
   sb.append(query);
   sb.append("; SELECTION: ");
   List list=OntologyElementsUtil.fromSemanticConceptsToOntologyElements(candidates);
  
   StringBuffer ranksString=new StringBuffer("");
   for (Integer rank:ranks){
     ranksString.append(rank.toString()).append(" ");
   }
   sb.append(OntologyElementsUtil.beautifyListOfOntologyElements(list));
   sb.append("; RANK: ");
   sb.append(ranksString.toString());
   sb.append("; TIME: ");
   sb.append(runTime);
   return sb.toString();
 }
 
 
 
}
