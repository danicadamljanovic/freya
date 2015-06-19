///**
// * 
// */
package org.freya.analyser;

import org.springframework.stereotype.Component;

//
//import gate.clone.ql.model.ClassElement;
//import gate.clone.ql.model.DatatypePropertyValueElement;
//import gate.clone.ql.model.GroupElement;
//import gate.clone.ql.model.InstanceElement;
//import gate.clone.ql.model.Interpretation;
//import gate.clone.ql.model.InterpretationElement;
//import gate.clone.ql.model.KeyPhraseElement;
//import gate.clone.ql.model.OntoResChunkElement;
//import gate.clone.ql.model.PropertyElement;
//import gate.clone.ql.utils.CloneQlConstants;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * This class transforms Interpretations with elementlist that contains
// * ClassElements, PropertyElements, InstanceElements into Interpretations with
// * elementslist that contains group of ClassElements, PropertyElements,
// * InstanceElements, etc.
// * 
// * @author danica
// * 
// */
@Component
public class GroupFinder{
	
}
//implements InterpretationTransformer {
//
//
//	/*
//	 * This method checks if the elements in elementsList of Interpretation are
//	 * of type OntoResElement or OntoResChunkElement
//	 * 
//	 * @see gate.clone.ql.transformer.InterpretationTransformer#canTransform(gate.clone.ql.model.Interpretation)
//	 */
//	public boolean canTransform(Interpretation interpretation) {
//		List<InterpretationElement> interpretationElementList = interpretation
//				.getElementsList();
//		boolean canTransform = false;
//		for (InterpretationElement element : interpretationElementList) {
//			if (element instanceof ClassElement
//					|| element instanceof InstanceElement
//					|| element instanceof PropertyElement
//					|| element instanceof DatatypePropertyValueElement
//					|| element instanceof OntoResChunkElement
//					|| element instanceof KeyPhraseElement) {
//				canTransform = true;
//			} else {
//				canTransform = false;
//				return canTransform;
//			}
//		}
//		return canTransform;
//	}
//
//	/*
//	 * transforms Interpretations with elementlist that contains ontology-aware
//	 * elements, keyphrase elements and ontoreschunk elements into
//	 * Interpretations with elementslist that contains groups of these elements
//	 * (mainly ontology-aware)
//	 * 
//	 * @see gate.clone.ql.transformer.InterpretationTransformer#transform(gate.clone.ql.model.Interpretation)
//	 */
//	public List<Interpretation> transform(Interpretation interpretation) {
//		List finalList = new ArrayList();
//		List<InterpretationElement> elementList = interpretation
//				.getElementsList();
//		Interpretation newInterpretation = new Interpretation();
//		newInterpretation.setSource(interpretation);
//		List<InterpretationElement> newElementList = new ArrayList();
//		StringBuffer originalString = new StringBuffer("");
//		// from list of elements create string
//		for (int i = 0; i < elementList.size(); i++) {
//			InterpretationElement element = elementList.get(i);
//			String keyWord = null;
//			if (element instanceof KeyPhraseElement) {
//				keyWord = (String) ((KeyPhraseElement) element).getData()
//						.getFeatures().get("majorType");
//			}
//			if (CloneQlConstants.CONJUNCTION.equals(keyWord)) {
//				originalString.append(keyWord).append("-");
//			} else if (CloneQlConstants.DISJUNCTION.equals(keyWord)) {
//				originalString.append(keyWord).append("-");
//			} else
//				originalString.append("i").append(i).append("-");
//		}
//		// call groupengine to find groups
//		String resultString = GroupEngine.findAndReplaceGroupOccurencies(
//				originalString.toString(),
//				CloneQlConstants.REGEX_GROUPS_SEPARATED_BY_OR,
//				CloneQlConstants.GROUP_PREFIX, CloneQlConstants.GROUP_SUFFIX,
//				null);
//		resultString = GroupEngine.findAndReplaceGroupOccurencies(resultString,
//				CloneQlConstants.REGEX_GROUPS_SEPARATED_BY_AND,
//				CloneQlConstants.GROUP_PREFIX, CloneQlConstants.GROUP_SUFFIX,
//				null);
//		String[] elements = resultString.split("-");
//		for (int j = 0; j < elements.length; j++) {
//			String element = elements[j];
//			// check if element is group or an individual
//			if (element.startsWith("i")) {
//				// get index and
//				int index = new Integer(element.substring(1, element.length()));
//				newElementList.add(elementList.get(index));
//			} else if (element.startsWith(CloneQlConstants.GROUP_PREFIX)) {
//				// create group
//				GroupElement group = new GroupElement();
//				String type = CloneQlConstants.DISJUNCTION;
//				group.setType(type);
//				List data = new ArrayList();
//				String allElementsInGroup = element.substring(
//						0 + CloneQlConstants.GROUP_PREFIX.length(), element
//								.length()
//								- CloneQlConstants.GROUP_SUFFIX.length());
//				// split this group
//				String[] elementsInGroup = allElementsInGroup.split(":");
//				for (int k = 0; k < elementsInGroup.length; k++) {
//					String anElement = elementsInGroup[k];
//					if (anElement.startsWith("i")) {
//						// find index and add element to the group
//						int index = new Integer(anElement.substring(1,
//								anElement.length()));
//						data.add(elementList.get(index));
//					} else if (anElement.equals(CloneQlConstants.CONJUNCTION)) {
//						group.setType(CloneQlConstants.CONJUNCTION);
//					} else if (anElement.equals(CloneQlConstants.DISJUNCTION)) {
//						group.setType(CloneQlConstants.DISJUNCTION);
//					}
//				}
//				group.setData(data);
//				group.setStartToken(((InterpretationElement) data.get(0))
//						.getStartToken());
//				group.setEndToken(((InterpretationElement) data
//						.get(data.size() - 1)).getEndToken());
//				newElementList.add(group);
//			}
//		}
//		// from string create new interpretation with group elements
//		newInterpretation.setElementsList(newElementList);
//		finalList.add(newInterpretation);
//		return finalList;
//	}
//}
