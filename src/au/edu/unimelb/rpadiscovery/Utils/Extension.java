package au.edu.unimelb.rpadiscovery.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.List;
import java.util.regex.*;

import info.debatty.java.stringsimilarity.Levenshtein;
import info.debatty.java.stringsimilarity.LongestCommonSubsequence;
import info.debatty.java.stringsimilarity.SorensenDice;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;

import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Automaton;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Transition;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributes;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributesList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;


import java.io.StringReader;
import java.io.StringWriter;

public class Extension {
    
    /**
     * Uses different similarity metrics to compare two selectors to decide if they can be combined.  
     * @param selector1
     * @param selector2
     * @param minimalSimilarity
     * @return
     */
    private static boolean similarityCheck(String selector1, String selector2, float minimalSimilarity) { // TODO use useful similarity metric
        Levenshtein l = new Levenshtein();
        int hardcodedMagicNumber = 75;
        System.out.println("Levenshtein: " + l.distance(selector1, selector2));
        NormalizedLevenshtein nl = new NormalizedLevenshtein();
        System.out.println("NormalizedLevenshtein: " + nl.distance(selector1, selector2) + " - - - Similarity: " +  nl.similarity(selector1, selector2));
        LongestCommonSubsequence lcs = new LongestCommonSubsequence();
        System.out.println("LongestCommonSubsequence: " + lcs.distance(selector1, selector2) + " - - - Length: " + lcs.length(selector1, selector2));
        SorensenDice sd = new SorensenDice();
        System.out.println("SorensonDice: " + sd.distance(selector1, selector2) + " - - - Similarity: " + sd.similarity(selector1, selector2));
        
        if(l.distance(selector1, selector2) <= hardcodedMagicNumber && nl.similarity(selector1, selector2) >= minimalSimilarity ){
            return true;
        }
        return false;
    }

    /**
     * Function to combine two selectors
     * @param selector1 
     * @param selector2
     * @return the combined selector, uses '*' as wildcards
     */
    private static String combineSelector(String selector1, String selector2) { // TODO: maybe change this to a list with all combinable selectors
        String regexString = "<.*?>";
        String fullCombined = "";
        
        Pattern p = Pattern.compile(regexString, Pattern.MULTILINE);
        Matcher m1 = p.matcher(selector1);
        Matcher m2 = p.matcher(selector2);
        List<String> m1Matches = new ArrayList<String>();

        while (m1.find()) {
            m1Matches.add(m1.group());
        }
        List<String> m2Matches = new ArrayList<String>(m1Matches.size() + 1);
        while (m2.find()) {
            m2Matches.add(m2.group());
        }
        if(m1Matches.size() != m2Matches.size()) {
            System.out.println("COMBINE: Not same length!");
            // make sure, the longer list comes first
            if(m2Matches.size() > m1Matches.size()) {
                List<String> tmp = m1Matches;
                m1Matches = m2Matches;
                m2Matches = tmp;
            }
            // TODO if they have different length
        }
        else {
            
            for (int i = 0; i < m1Matches.size(); i++) {
                String first = m1Matches.get(i);
                System.out.println(first);
                String second = m2Matches.get(i);
                System.out.println(second);


                try {
                    Document xml1 = loadXMLFromString(first);
                    Document xml2 = loadXMLFromString(second);
                    
                    if(xml1.isEqualNode(xml2)) {
                        System.out.println("they are same");
                        fullCombined += first;
                        continue;
                    }
                    System.out.println("not same ");


                    NamedNodeMap attributes1 = xml1.getFirstChild().getAttributes();
                    NamedNodeMap attributes2 = xml2.getFirstChild().getAttributes();
                    for (int j = 0; j < attributes1.getLength(); j++) {
                        Node firstAttr = attributes1.item(j);
                        String attrName = firstAttr.getNodeName();
                        Node secondAttr = attributes2.getNamedItem(attrName);

                        // if attributes are the same, we dont have to change them
                        if(!firstAttr.isEqualNode(secondAttr)) {
                            // if attribute exists in second node, we combine
                            if(secondAttr != null) {
                                System.out.println("TODO: combine");
                                System.out.println(firstAttr.toString() + " vs " + secondAttr.toString());

                                String generalized = recursiveStringCombining(firstAttr.getNodeValue(), secondAttr.getNodeValue(), 3);
                                System.out.println(generalized);
                                firstAttr.setNodeValue(generalized);
                                System.out.println(writeXMLToString(xml1));

                            }
                            else {
                                // remove this attribute, because second one doesn't have it
                                System.out.println(firstAttr.toString() + " is not found in second node." );
                            }
                        }
                        else {
                            System.out.println(firstAttr.toString() + " and " + secondAttr.toString() + " are the same. ");
                        }

                    }
                    fullCombined += writeXMLToString(xml1);
                }
                catch (Exception e) {
                    e.printStackTrace();

                }
                System.out.println(" - - - ");
            }

            System.out.println("FINAL OUTPUT: " + fullCombined);
        }
        
        return fullCombined;
    }


    /**
     * Util function that loads an xml-String into a xml-Document
     * @param xml
     * @return
     * @throws Exception
     */
    public static Document loadXMLFromString(String xml) throws Exception
    {
        System.out.println("LOADER: xml-string " + xml);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public static String writeXMLToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        return output;
    }

    /**
     * Computes the longest common substring in the given Strings s and t. 
     * If there are multiple longest substrings, only one is returned. 
     * @return the longest common substring contained in both strings 
     */
    public static String longestCommonSubstrings(String s, String t) {
        int[][] table = new int[s.length()][t.length()];
        int longest = 0;
        String result = null;
    
        for (int i = 0; i < s.length(); i++) {
            for (int j = 0; j < t.length(); j++) {
                if (s.charAt(i) != t.charAt(j)) {
                    continue;
                }
    
                table[i][j] = (i == 0 || j == 0) ? 1
                                                 : 1 + table[i - 1][j - 1];
                if (table[i][j] > longest) {
                    longest = table[i][j];
                    result = s.substring(i - longest + 1, i + 1);
                }
            }
        }
        return result;
    }


    /**
     * Recursively assimilates two strings, uses '*' as a Wildcard (equal to '.*' in Regex)
     * @param str1
     * @param str2
     * @param minLength minimal number of consecutive characters that are considered substrings 
     * @return
     */
    public static String recursiveStringCombining(String str1, String str2, int minLength){
        
        if(str1.length() == 0 && str2.length() == 0) {
            return "";
        }

        String same = longestCommonSubstrings(str1, str2);
         
        Pattern p = Pattern.compile("(.*)" + same + "(.*)");
        Matcher m1 = p.matcher(str1);
        Matcher m2 = p.matcher(str2);
        if(m1.find() && m2.find()) {

        }

        System.out.println("same: " + same);
        System.out.println("str1: " + str1 + " - - - str2: " + str2);     
        if (same != null && same.length() >= minLength) {
            //String[] input1 = str1.split(same);
            //String[] input2 = str2.split(same);
            //return recursiveStringCombining(input1[0], input2[0], minLength) + same + recursiveStringCombining(input1[1], input2[1], minLength);
        
            return recursiveStringCombining(m1.group(1), m2.group(1), minLength) + same + recursiveStringCombining(m1.group(2), m2.group(2), minLength);
        }
        return "*";
    }



    public static void graphAnalyzer(Automaton dafsa, HashMap<String, EventAttributesList> actionPayloadMap) {
        for (Entry<Integer, au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.State> state : dafsa.states()
                    .entrySet()) {
                
                // only look at possible decisions 
                int outgoingLength = state.getValue().outgoingTransitions().size();
                if(outgoingLength > 1) {
                    // double for 

                    System.out.println("Multiple outgoing at " + state.getValue().label() + " ( Count:  " + outgoingLength + " )");
                    boolean[] duplicates = new boolean[outgoingLength]; // default false, set to true after combining them (?)

                    for (int i = 0; i < outgoingLength - 1; i++) {
                        Transition transA = state.getValue().outgoingTransitions().get(i);
                        if(duplicates[i]) continue; // skip already incorporated duplicates
                        for (int j = i + 1; j < outgoingLength; j++) {
                            if(duplicates[j]) continue;
                            Transition transB = state.getValue().outgoingTransitions().get(j);
                            System.out.println("Vergleich: " + transA.id() + " - " + transB.id());
                            
                            System.out.println(dafsa.eventLabels().get(transA.eventID()) + " - " + dafsa.eventLabels().get(transB.eventID()));
                            
                            if(transA.target().id() == transB.target().id()) { // alternative uses actionpayloadmap - current version possibly more efficient
                                // must combine them to one Transition, multiple Transition to the same node are not allowed
                                System.out.println("Same target id between events " + transA.target().label() + " and " + transB.target().label() + " (starting: " + state.getValue().label() + ")");
                                
                                String RPSTLabel = "[" + state.getValue().label() + "->" + transA.target().label() + "]";
                                String src1 = ""; 
                                String src2 = "";
                                EventAttributes eventAttr = null;
                                EventAttributes eventAttr2 = null;
                                for (EventAttributes ea : actionPayloadMap.get(RPSTLabel).getList()) {
                                    if(ea.getActivityName().equals(dafsa.eventLabels().get(transA.eventID()))) {
                                        src1 = ea.getAttribute("source", 0);
                                        eventAttr = ea; // always take the first
                                    }
                                    if(ea.getActivityName().equals(dafsa.eventLabels().get(transB.eventID()))) {
                                        src2 = ea.getAttribute("source", 0);
                                        eventAttr2 = ea;
                                    }
                                }
                                //similarityCheck(src1, src2, 0.123f);


                                String generalized = combineSelector(src1, src2);
                                eventAttr.getAttributes("source").remove(src1);
                                eventAttr2.getAttributes("source").remove(src2);
                                //eventAttr.getAttributes("source").clear();   // TODO don't clear like that: if target.id() == target.id(): combine all selectors in eventAttr.getAttributes("source") at once
                                eventAttr.addAttributes("source", generalized);

                                //actionPayloadMap.get(RPSTLabel).getList().remove(eventAttr2);

                                // TODO remove the transition

                                
                                duplicates[j] = true;
                            } // if there is a split and the next activity for both is the same, we check if they can be combined
                            else if (transA.target().outgoingTransitions().size() == transB.target().outgoingTransitions().size()) {
                                HashSet<Integer> comparisonSet = new HashSet<>();
                                for(Transition futureTransA : transA.target().outgoingTransitions()){
                                    System.out.print("Starting at State " + state.getValue().label() + " --- ");
                                    System.out.print("Outgoing Trans of " + transA.target().label() + " with EventID: " + futureTransA.eventID() + " --- ");
                                    comparisonSet.add(futureTransA.eventID());
                                    // put all futureTransA.eventID in HashSet
                                    // then for each futureTransB check if in HashSet: true for all -> selektorabgleich durchführen
                                    
                                }
                                boolean sameFutureOutgoing = true;
                                for(Transition futureTransB : transB.target().outgoingTransitions()){
                                    if(!comparisonSet.contains(futureTransB.eventID())){
                                        sameFutureOutgoing = false;
                                    }
                                }

                                if(sameFutureOutgoing) {
                                    // do selektorabgleich
                                    String RPSTLabelA = "[" + state.getValue().label() + "->" + transA.target().label() + "]";
                                    String RPSTLabelB = "[" + state.getValue().label() + "->" + transB.target().label() + "]";
                                    System.out.println("ActionPayloadMapLength: " + actionPayloadMap.get(RPSTLabelA).getList().size() + " vs " + actionPayloadMap.get(RPSTLabelB).getList().size() + " -- for: " + RPSTLabelA + " vs " + RPSTLabelB);
                                    for(EventAttributes ea : actionPayloadMap.get(RPSTLabelA).getList()) {
                                        System.out.println(ea.getActivityName() + " with  " );
                                        for (String key : ea.getEventAttributesMap().keySet()) {
                                            System.out.println("Key: " + key + " and Vals: " + ea.getAttributes(key));
                                        }
                                        System.out.println("___");
                                    }
                                    actionPayloadMap.get(RPSTLabelB).getList().size();
                                    


                                    String src1 = ""; 
                                    String src2 = "";
                                    EventAttributes ea1 = null;
                                    EventAttributes ea2 = null;
                                    for (EventAttributes ea : actionPayloadMap.get(RPSTLabelA).getList()) {
                                         if(ea.getActivityName().equals(dafsa.eventLabels().get(transA.eventID()))) {
                                            src1 = ea.getAttribute("source", 0);
                                            ea1 = ea;
                                         }
                                    }
                                    for (EventAttributes ea : actionPayloadMap.get(RPSTLabelB).getList()) {
                                        if(ea.getActivityName().equals(dafsa.eventLabels().get(transB.eventID()))) {
                                           src2 = ea.getAttribute("source", 0);
                                           ea2 = ea;
                                        }
                                    }
                                    if(similarityCheck(src1, src2, 0.9f)) {
                                        // combine them
                                        String result = combineSelector(src1, src2);
                                        // ea1.getAttributes("source").clear();
                                        ea1.getAttributes("source").remove(src1);
                                        ea1.addAttributes("source", result);
                                        
                                        // TODO uncomment. Node has to be deleted, Transitions have to be reinstated
                                        //actionPayloadMap.get(RPSTLabelB).getList().remove(ea2);
                                        // ea2.getAttributes("source").clear();
                                        ea2.getAttributes("source").remove(src2);
                                        ea2.addAttributes("source", result);
                                        System.out.println("combined diff nodes");

                                        duplicates[j] = true;
                                    }


                                    System.out.println("SimCheck");
                                }
                                else {
                                    // if the following event is not the same
                                    // TODO only done for testing. Need more intelligent way to decide
                                    System.out.println("Normally no SimCheck.");
                                    
                                    
                                    String RPSTLabelA = "[" + state.getValue().label() + "->" + transA.target().label() + "]";
                                    String RPSTLabelB = "[" + state.getValue().label() + "->" + transB.target().label() + "]";
                                    System.out.println("ActionPayloadMapLength: " + actionPayloadMap.get(RPSTLabelA).getList().size() + " vs " + actionPayloadMap.get(RPSTLabelB).getList().size() + " -- for: " + RPSTLabelA + " vs " + RPSTLabelB);
                                    for(EventAttributes ea : actionPayloadMap.get(RPSTLabelA).getList()) {
                                        System.out.println(ea.getActivityName() + " with  " );
                                        for (String key : ea.getEventAttributesMap().keySet()) {
                                            System.out.println("Key: " + key + " and Vals: " + ea.getAttributes(key));
                                        }
                                        System.out.println("___");
                                    }
                                    actionPayloadMap.get(RPSTLabelB).getList().size();
                                    


                                    String src1 = ""; 
                                    String src2 = "";
                                    EventAttributes ea1 = null;
                                    EventAttributes ea2 = null;
                                    for (EventAttributes ea : actionPayloadMap.get(RPSTLabelA).getList()) {
                                         if(ea.getActivityName().equals(dafsa.eventLabels().get(transA.eventID()))) {
                                            src1 = ea.getAttribute("source", 0);
                                            ea1 = ea;
                                         }
                                    }
                                    for (EventAttributes ea : actionPayloadMap.get(RPSTLabelB).getList()) {
                                        if(ea.getActivityName().equals(dafsa.eventLabels().get(transB.eventID()))) {
                                           src2 = ea.getAttribute("source", 0);
                                           ea2 = ea;
                                        }
                                    }
                                    if(similarityCheck(src1, src2, 0.9f)) {
                                        System.out.println("Combining still possible.");
                                        String result = combineSelector(src1, src2);
                                        ea1.getAttributes("source").remove(src1);
                                        // ea1.getAttributes("source").clear();
                                        ea1.addAttributes("source", result);
                                        
                                        // TODO uncomment. Node has to be deleted, Transitions have to be reinstated
                                        // actionPayloadMap.get(RPSTLabelB).getList().remove(ea2);
                                        ea2.getAttributes("source").remove(src2);
                                        // ea2.getAttributes("source").clear();
                                        ea2.addAttributes("source", result);
                                        System.out.println("combined diff nodes");

                                        duplicates[j] = true;
                                    }


                                    // TODO end of ONLY FOR TESTING need more intelligent way to decide
                                }
                                
                            }
                            else {
                                System.out.println("Not combinable decision at state " + state.getValue().label());
                            }
                            // else do nothing
                        }
                    }

                    // TODO remove the transitions marked as duplicates
                    // TODO return the graph and/or actionPayloadMap
                }

        }
    }
}
