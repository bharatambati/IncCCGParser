/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import edinburgh.ccg.deps.CCGcat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

/**
 *
 * @author ambati
 */
public class Utils {
        
    public enum SRAction {
        SHIFT, RL, RR, LA, RA, RU, LREVEAL, RREVEAL, RAR, REDUCE;
    }
    
    public static CCGJNode parseDrivString(String treeString, CCGJSentenceChart sent) {

        sent.setCcgDeriv(treeString);
        Stack<CCGJNode> nodes = new Stack<>();
        Stack<Character> cStack = new Stack<>();
        char[] cArray = treeString.toCharArray();
        boolean foundOpenLessThan = false;
        int id = 0;

        for (Character c : cArray) {
            if (c == '<') {
                foundOpenLessThan = true;
            } else if (c == '>') {
                foundOpenLessThan = false;
            }

            if (c == ')' && !foundOpenLessThan) {
                StringBuilder sb = new StringBuilder();
                Character cPop = cStack.pop();
                while (cPop != '<') {
                    sb.append(cPop);
                    cPop = cStack.pop();
                }
                sb.append(cPop);
                // pop (
                cStack.pop();
                sb.reverse();
                String nodeString = sb.toString();
                // (<L (S/S)/NP IN IN In (S_113/S_113)/NP_114>)
                if (nodeString.charAt(1) == 'L') {
                    CCGJNode node = new CCGJNode(id+1, nodeString);
                    sent.addCCGJNode(node);
                    nodes.add(node);
                    id++;
                } 
                else if (nodeString.charAt(1) == 'T') {
                    // (<T S/S 0 2> (<L (S/S)/NP IN IN In (S_113/S_113)/NP_114>)
                    ArrayList<String> items = Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> ")).trimResults().omitEmptyStrings().split(nodeString));
                    CCGJNode node = new CCGJNode();
                    int childrenSize = Integer.parseInt(items.get(3));
                    int headId = Integer.parseInt(items.get(2));
                    node.setHeadId(headId);                    
                    while (childrenSize > 0) {
                        node.addChild(nodes.pop(), 0);
                        childrenSize--;
                    }
                    String rescatstr = items.get(1);
                    CCGcat rescat;
                    childrenSize = Integer.parseInt(items.get(3));
                    if(childrenSize == 1){
                        CCGcat lcat = node.getChild(0).getCCGcat();
                        rescat = CCGcat.typeRaiseTo(lcat, rescatstr);
                        if (rescat == null)
                            rescat = CCGcat.typeChangingRule(lcat, rescatstr);
                    }
                    else{
                        CCGcat lcat = node.getChild(0).getCCGcat();
                        CCGcat rcat = node.getChild(1).getCCGcat();
                        rescat = CCGcat.combine(lcat, rcat, rescatstr);
                    }
                    node.setConllNode(node.getChild(headId).getConllNode());
                    node.setCCGcat(rescat);
                    nodes.add(node);
                }
            } else {
                cStack.add(c);
            }
        }
        Preconditions.checkArgument(nodes.size() == 1, "Bad Tree");
        CCGJNode root = nodes.pop();
        sent.setccgDerivTree(root);
        return root;
    }

    public static boolean isPunct(CCGcat pct){
        if (pct.catString.equals(",") || pct.catString.equals(".")
                || pct.catString.equals(";") || pct.catString.equals(":")
                || pct.catString.equals("RRB") || pct.catString.equals("LRB")
                || pct.catString.equals("``") || pct.catString.equals("\'\'") ){
            
            return true;
        }
        else
            return false;
    }
    
    public static boolean isPunct(String pct){
        return pct.equals(",") || pct.equals(".")
                || pct.equals(";") || pct.equals(":")
                || pct.equals("RRB") || pct.equals("LRB")
                || pct.equals("``") || pct.equals("\'\'");
    }
    
    public static CoNLLNode cNodeFromString(int wid, String lexString){
        
        CoNLLNode cNode;
        String wrd, lemma, pos, cpos, feats="";
        ArrayList<String> items = Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> ")).trimResults().omitEmptyStrings().split(lexString));
        String cat = items.get(1);
        pos = items.get(2);
        cpos = items.get(3);
	wrd = items.get(4);
	lemma = wrd;
        cNode = new CoNLLNode(wid, wrd, lemma, cpos, pos, feats, 0, "", cat);
        
        return cNode;
    }
    
    public static SCoNLLNode scNodeFromString(int wid, String lexString){
        
        SCoNLLNode cNode;
        String wrd, cpos, pos, cat;
        ArrayList<String> items = Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> ")).trimResults().omitEmptyStrings().split(lexString));
        cat = items.get(1);
        pos = items.get(2);
        cpos = items.get(3);
	wrd = items.get(4);
        cNode = new SCoNLLNode(wid, wrd, pos, cat);
        
        return cNode;
    }
    
    public static CCGJTreeNode ccgNodeFromString(int wid, String lexString){
        
        SCoNLLNode cNode;
        String wrd, cpos, pos, cat;
        ArrayList<String> items = Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> ")).trimResults().omitEmptyStrings().split(lexString));
        cat = items.get(1);
        pos = items.get(2);
        cpos = items.get(3);
	wrd = items.get(4);
        cNode = new SCoNLLNode(wid, wrd, pos, "");
        CCGcat rescat = CCGcat.lexCat(wrd, cat, wid);
        CCGJTreeNode node = CCGJTreeNode.makeLeaf(rescat, cNode);
        
        return node;
    }
    
    public static <K,V extends Comparable<? super V>>
    SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
        new Comparator<Map.Entry<K,V>>() {
            @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                int res = e2.getValue().compareTo(e1.getValue());
                return res != 0 ? res : 1;
            }
        }
    );
    sortedEntries.addAll(map.entrySet());
    return sortedEntries;
    }

    
}
