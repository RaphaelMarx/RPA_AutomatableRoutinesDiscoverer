package au.edu.unimelb.rpadiscovery.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import au.edu.unimelb.rpadiscovery.SubPolygon;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Automaton;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Transition;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributes;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributesList;
import info.debatty.java.stringsimilarity.Levenshtein;
import info.debatty.java.stringsimilarity.LongestCommonSubsequence;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import info.debatty.java.stringsimilarity.SorensenDice;
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

    /**
     * Util function that converts an xml-Document into a String
     * @param doc
     * @return
     * @throws Exception
     */
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




    public static void exportResults(Automaton dafsa, HashMap<String, EventAttributesList> actionPayloadMap, TreeMap<String, LinkedList<SubPolygon>> automatablePolygonsCandidates, HashMap<String, String> trivialRules, 
                                    HashMap<String, String> activationRules, TreeMap<String, LinkedList<SubPolygon>> subPolygonMap, String path) {
        JSONObject actions = new JSONObject();
        
        for (Integer transID : dafsa.transitions().keySet()) {
            au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Transition t = dafsa.transitions().get(transID);
            String transLabel = "[" + t.source().label() + "->" + t.target().label() + "]";

            JSONObject jsAttributes;
            
            String add = "";
            for (EventAttributes eventAttr : actionPayloadMap.get(transLabel).getList()) {
                jsAttributes = new JSONObject();
                try {
                    jsAttributes.put("name", eventAttr.getActivityName()); // check if this attrname is already in the json
                } catch (JSONException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                for (Entry<String, LinkedList<String>> attributes : eventAttr     // dont peek! use every element in list!
                .getEventAttributesMap().entrySet()) {
                    try {
                        jsAttributes.put(attributes.getKey(), attributes.getValue().getFirst());
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                try {
                    actions.put(transLabel + add, jsAttributes);
                    //add += "l";
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
           
        }
        try {
            
            FileWriter fwActions = new FileWriter(path + "actions.json", StandardCharsets.UTF_8);
            actions.write(fwActions);
            fwActions.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Graph Export start
               try {
            JSONObject graph = new JSONObject();
            JSONArray starter = new JSONArray();
            graph.put("start", starter);
            for (Entry<Integer, au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.State> state : dafsa.states()
                    .entrySet()) {
                JSONArray transitionsArray = new JSONArray();
                for (Transition trans : state.getValue().outgoingTransitions()) {
                    // add transition object to transition array
                    JSONObject singleTransition = new JSONObject();

                    singleTransition.put("target", trans.target().label());

                    // check rules & regions

                    transitionsArray.put(singleTransition);
                }
                if(state.getValue().isFinal()) {
                    transitionsArray.put(new JSONObject().put("final", "true"));
                }
                if(state.getValue().isSource()) {
                    System.out.println("Source:  " + state.getKey().toString());
                    starter.put(state.getKey().toString());
                }
                graph.put(state.getKey().toString(), transitionsArray);
            }
            System.out.println(graph.toString());
            System.out.println(starter.toString());

            for (String polygon : automatablePolygonsCandidates.keySet()) {
                for (SubPolygon subPolygon : automatablePolygonsCandidates.get(polygon)) {
                    String beginState = subPolygon.getStart().getLabel().substring(1, subPolygon.getStart().getLabel().indexOf("-"));
                    String endState = subPolygon.getStart().getLabel().substring(subPolygon.getStart().getLabel().indexOf(">") + 1, subPolygon.getStart().getLabel().length() - 1);
                    // trivial Rules
                    if (trivialRules.get(subPolygon.getName()) != null) {
                        String[] ruleString = trivialRules.get(subPolygon.getName()).split(" ");
                        for (int i = 1; i < ruleString.length - 1; i++) {
                            String beginCondition = ruleString[i].substring(1, ruleString[i].indexOf("-"));
                            String endCondition = ruleString[i].substring(ruleString[i].indexOf(">") + 1,
                                    ruleString[i].length() - 1);
                            System.out.println("Start: " + beginCondition + " and  " + endCondition);
                            System.out.println(ruleString[i]);
                            for (int j = 0; j < graph.getJSONArray(beginCondition).length(); j++) {
                                JSONObject trans = graph.getJSONArray(beginCondition).getJSONObject(j);
                                if (trans.getString("target").equals(endCondition)) {
                                    trans.put("trivialStart", beginState);
                                    trans.put("trivialEnd", endState);
                                    
                                }
                            }
                        }
                    }

                    // activation Rules (not working with multiple conditions)
                    if(activationRules.get(subPolygon.getName()) != null) {
                        String ruleString = activationRules.get(subPolygon.getName());
                        String regexString = "\\((\\[\\d+\\-\\>\\d+\\])(\\w+):(\\w+) (\\W+) (.+)\\)";

                        
                        Pattern p = Pattern.compile(regexString, Pattern.MULTILINE);
                        Matcher m = p.matcher(ruleString);
                        if(m.find()) {
                            System.out.println(m.group(0));
                            // suche transition -> ähnlich zu oben
                            String beginCondition = m.group(1).substring(1, m.group(1).indexOf("-"));
                            String endCondition = m.group(1).substring(m.group(1).indexOf(">") + 1, m.group(1).length() - 1);
                            System.out.println("Condition: " + m.group(1));
                            for (int j = 0; j < graph.getJSONArray(beginCondition).length(); j++) {
                                JSONObject trans = graph.getJSONArray(beginCondition).getJSONObject(j);
                                
                                if (trans.getString("target").equals(endCondition)) {
                                    JSONArray actArr;
                                    if(!trans.has("activationRule")) {
                                        actArr = new JSONArray();
                                        trans.put("activationRule", actArr);
                                    }
                                    else {
                                        actArr = trans.getJSONArray("activationRule");
                                    }
                                    JSONObject actRule = new JSONObject();
                                    actRule.put("activityName", m.group(2));
                                    actRule.put("attributeName", m.group(3));
                                    actRule.put("operator", m.group(4));
                                    actRule.put("attributeValue", m.group(5));
                                    actRule.put("activatedStart", beginState);
                                    actRule.put("activatedEnd", endState);
                                    actArr.put(actRule);
                                }
                            }
                        }
                    }
                }
            }

            // can't use automatable, because it doesn't contain all SESE sequences (single actions are still not in this list)
            for(String polygonName : subPolygonMap.keySet()) {
                LinkedList<SubPolygon> spl = subPolygonMap.get(polygonName);
                for(SubPolygon sp : spl) {
                    System.out.println(sp.getStart().getLabel() + " to " + sp.getFinish().getLabel());
                    String beginTransState = sp.getStart().getLabel().substring(1, sp.getStart().getLabel().indexOf("-"));
                    String endTransState = sp.getStart().getLabel().substring(sp.getStart().getLabel().indexOf(">") + 1, sp.getStart().getLabel().length() - 1);


                    for (int j = 0; j < graph.getJSONArray(beginTransState).length(); j++) {
                        JSONObject trans = graph.getJSONArray(beginTransState).getJSONObject(j);
                        if (trans.getString("target").equals(endTransState)) {
                            trans.put("entry", sp.getName());
                        }
                    }

                    String beginEndState = sp.getFinish().getLabel().substring(1, sp.getFinish().getLabel().indexOf("-"));
                    String endEndState = sp.getFinish().getLabel().substring(sp.getFinish().getLabel().indexOf(">") + 1, sp.getFinish().getLabel().length() - 1);
                    for (int j = 0; j < graph.getJSONArray(beginEndState).length(); j++) {
                        JSONObject trans = graph.getJSONArray(beginEndState).getJSONObject(j);
                        if (trans.getString("target").equals(endEndState)) {
                            trans.put("exit", sp.getName());
                        }
                    }
                }
            }


            System.out.println(graph.toString());
            FileWriter graphWriter = new FileWriter(path + "graph.json", StandardCharsets.UTF_8);
            graph.write(graphWriter);
            graphWriter.flush();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
