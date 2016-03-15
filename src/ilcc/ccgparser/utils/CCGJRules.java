/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ambati
 * 
 */
public class CCGJRules {
    
    private final HashMap<String, ArrayList<CCGJRuleInfo>> unaryRules;
    private final HashMap<String, ArrayList<CCGJRuleInfo>> binaryRules;
    private final HashMap<String, ArrayList<CCGJRuleInfo>> revealRules;
    
    public CCGJRules() throws IOException{
        unaryRules = new HashMap<>();
        binaryRules = new HashMap<>();
        revealRules = new HashMap<>();
    }
    
    public CCGJRules(HashMap urules, HashMap brules, HashMap rrules){
        unaryRules = urules;
        binaryRules = brules;
        revealRules = rrules;
    }
    
    public HashMap<String, ArrayList<CCGJRuleInfo>> getUnaryRules(){
        return unaryRules;
    }
    
    public HashMap<String, ArrayList<CCGJRuleInfo>> getBinaryRules(){
        return binaryRules;
    }
    
    public HashMap<String, ArrayList<CCGJRuleInfo>> getRevealRules(){
        return revealRules;
    }
    
    public void addRules(String unaryRuleFile, String binaryRuleFile) throws FileNotFoundException, IOException{
        addUnaryRules(unaryRuleFile);
        addBinaryRules(binaryRuleFile);
    }
    
    private void addUnaryRules(String unaryRuleFile) throws FileNotFoundException, IOException{
        BufferedReader unaryReader = new BufferedReader(new FileReader(new File(unaryRuleFile)));
        String line;
        while ((line = unaryReader.readLine()) != null) {
            if(line.startsWith("#"))
                continue;
            //#childCat\trescat\tCombinator\tCount\n
            String[] parts = line.trim().split("\t");
            CCGJRuleInfo ruleInfo  = getRuleInfo(parts, true);
            String key = parts[0];
            updateUnaryRuleInfo(ruleInfo, key);
        }
    }
    
    private void addBinaryRules(String binaryRuleFile) throws FileNotFoundException, IOException{
        BufferedReader binaryReader = new BufferedReader(new FileReader(new File(binaryRuleFile)));
        String line;
        while ((line = binaryReader.readLine()) != null) {
            if(line.startsWith("#"))
                continue;
            //#lcat\trcat\trescat\theadDirection\tCombinator\tCount\n
            String[] parts = line.trim().split("\t");
            CCGJRuleInfo ruleInfo  = getRuleInfo(parts, false);
            String key = parts[0]+" "+parts[1];
            updateBinaryRuleInfo(ruleInfo, key);
        }
    }
    
    private void addNSort(ArrayList list, CCGJRuleInfo ruleInfo){
                    
        int i;
        for(i = 0; i < list.size(); i++){
            CCGJRuleInfo info = (CCGJRuleInfo) list.get(i);
            if(info.getRuleCount() < ruleInfo.getRuleCount())
                break;
        }
        list.add(i, ruleInfo);        
    }
    
    private void add(ArrayList list, CCGJRuleInfo ruleInfo){
        
        boolean found = false;
        for(int i = 0; i < list.size(); i++){
            CCGJRuleInfo info = (CCGJRuleInfo) list.get(i);
            if(info.toString().equals(ruleInfo.toString())) {
            //if(info.getResultCat().toString().equals(ruleInfo.getResultCat().toString()) &&
            //        info.getLevel() == ruleInfo.getLevel() && info.getHeadDir() == ruleInfo.getHeadDir()){
                info.setRuleCount(info.getRuleCount()+1);
                found = true;
            }
        }
        if(!found)
            list.add(ruleInfo);
    }

    private CCGJRuleInfo getRuleInfo(String[] rParts, boolean isunary){
        
        String lCat, rCat, resCat;
        RuleType comb;
        boolean dir;
        int count;
        CCGcat lcat, rcat, rescat;
        
        if(isunary){
            lCat = rParts[0];            
            resCat = rParts[1];
            dir = true;
            comb = RuleType.valueOf(rParts[2]);
            count = Integer.parseInt(rParts[3]);            
            rcat = null;
        }
        else{
            lCat = rParts[0];
            rCat = rParts[1];
            resCat = rParts[2];
            dir = rParts[3].equals("left");
            comb = RuleType.valueOf(rParts[4]);
            count = Integer.parseInt(rParts[5]);
            rcat = CCGcat.ccgCatFromString(rCat);
        }
        lcat = CCGcat.ccgCatFromString(lCat);
        rescat = CCGcat.ccgCatFromString(resCat);
        
        CCGJRuleInfo rinfo = new CCGJRuleInfo(lcat, rcat, rescat, dir, comb, 0, count);
        return rinfo;
    }
    
    
    public List<CCGJRuleInfo> getUnaryRuleInfo(String key){
        return unaryRules.get(key);
    }
    
    public List<CCGJRuleInfo> getBinRuleInfo(String key){
        return binaryRules.get(key);
    }
    
    public CCGJRuleInfo getBinRuleInfo(String key, String rescat){
        if(binaryRules.containsKey(key)){
            for(CCGJRuleInfo info : binaryRules.get(key)){
                String cat = info.getResultCat().catString;
                if(cat.equals(rescat))
                    return info;
            }
        }
        return null;
    }    
    
    public void updateBinaryRuleInfo(CCGJRuleInfo ruleInfo, String key){
        ArrayList<CCGJRuleInfo> list = new ArrayList<>();
        if (binaryRules.containsKey(key))
            list = binaryRules.get(key);
        addNSort(list, ruleInfo);
        binaryRules.put(key, list);
    }
    
    public void updateUnaryRuleInfo(CCGJRuleInfo ruleInfo, String key){
        ArrayList<CCGJRuleInfo> list = new ArrayList<>();
        if (unaryRules.containsKey(key))
            list = unaryRules.get(key);
        addNSort(list, ruleInfo);
        unaryRules.put(key, list);
    }
    
    public void addBinaryRuleInfo(CCGJRuleInfo ruleInfo, String key){
        addRuleInfo(ruleInfo, key, binaryRules);
    }
    
    public void addRevealRuleInfo(CCGJRuleInfo ruleInfo, String key){
        addRuleInfo(ruleInfo, key, revealRules);
    }
    
    public void addUnaryRuleInfo(CCGJRuleInfo ruleInfo, String key){
        addRuleInfo(ruleInfo, key, unaryRules);
    }
    
    private void addRuleInfo(CCGJRuleInfo ruleInfo, String key, HashMap<String, ArrayList<CCGJRuleInfo>> ruleMap){
        ArrayList<CCGJRuleInfo> list = new ArrayList<>();
        if (ruleMap.containsKey(key))
            list = ruleMap.get(key);
        add(list, ruleInfo);
        ruleMap.put(key, list);
    }
    
    public ArrayList<ArcJAction> getActions(CCGJTreeNode left, CCGJTreeNode right, CCGJTreeNode inode, ArrayList<CCGCategory> rightPerList){
        ArrayList<ArcJAction> actions = new ArrayList<>();        
        actions.addAll(shiftActions(inode));
        if(right != null)
            actions.addAll(unaryActions(right));
        if(left != null && right != null){
            actions.addAll(reduceActions(left, right));
            actions.addAll(revealActions(left, right, rightPerList));
            //actions.addAll(revealActions(left, right, rightPerList, true));
        }
        return actions;
    }
    
    public ArrayList<ArcJAction> shiftActions(CCGJTreeNode node){
        ArrayList<ArcJAction> actions = new ArrayList<>();
        ArrayList<CCGCategory> cats = getInputCatList(node);
        for(CCGCategory cat : cats){
            ArcJAction act = ArcJAction.make(SRAction.SHIFT, 0, cat.toString(), RuleType.lexicon);
            actions.add(act);
        }
        return actions;
    }
    
    private ArrayList<CCGCategory> getInputCatList(CCGJTreeNode node){
        ArrayList<CCGCategory> cats = new ArrayList<>();
        if(node == null)
            return cats;
        cats = node.getCCGcats();
        return cats;                
    }

    
    public ArrayList<ArcJAction> reduceActions(CCGJTreeNode left, CCGJTreeNode right){
        ArrayList<ArcJAction> actions = new ArrayList<>();
        String lCat = left.getCCGcat().toString();
        String rCat = right.getCCGcat().toString();
        String key = lCat+" "+rCat;
        ArrayList<CCGJRuleInfo> list;
        if((list = binaryRules.get(key)) != null)
            for(CCGJRuleInfo info : list){
                actions.add(ArcJAction.make(info.getHeadDir() ? SRAction.RR : SRAction.RL, info.getLevel(), info.getResultCat().toString(), info.getCombinator()));
            }
        return actions;
    }
    
    public ArrayList<ArcJAction> revealActions(CCGJTreeNode left, CCGJTreeNode right, ArrayList<CCGCategory> rightPerList, boolean flag){
        ArrayList<ArcJAction> actions = new ArrayList<>();
        String lCat = left.getCCGcat().toString();
        String rCat = right.getCCGcat().toString();
        
        String key = lCat+" "+rCat;
        ArrayList<CCGJRuleInfo> list;
        if((list = revealRules.get(key)) != null){
            for(CCGJRuleInfo info : list)
                if(info.getCombinator()==RuleType.lreveal)
                    actions.add(ArcJAction.make(SRAction.LREVEAL, info.getLevel(), info.getResultCat().toString(), info.getCombinator()));
        }
        
        int level = 0;
        for(int i=rightPerList.size()-1; i>0; i--){
            level++;
            CCGCategory lrCat = rightPerList.get(i);
            key = lrCat+" "+rCat;            
            if((list = revealRules.get(key)) != null)
                for(CCGJRuleInfo info : list){
                    if(info.getCombinator()==RuleType.rreveal)
                        actions.add(ArcJAction.make(SRAction.RREVEAL, level, info.getResultCat().toString(), info.getCombinator()));
                }
        }
        return actions;
    }
    
    public ArrayList<ArcJAction> revealActions(CCGJTreeNode left, CCGJTreeNode right, ArrayList<CCGCategory> rightPerList){
        ArrayList<ArcJAction> actions = new ArrayList<>();
        String lCat = left.getCCGcat().toString();
        String rCat = right.getCCGcat().toString();
        String key = lCat+" "+rCat;
        int depth = rightPerList.size()-1;
        ArrayList<CCGJRuleInfo> list;
        if((list = revealRules.get(key)) != null){
            for(CCGJRuleInfo info : list)
                if(info.getCombinator()==RuleType.lreveal)
                    actions.add(ArcJAction.make(SRAction.LREVEAL, info.getLevel(), info.getResultCat().toString(), info.getCombinator()));
                else if(info.getCombinator()==RuleType.rreveal && info.getLevel() <= depth)
                    actions.add(ArcJAction.make(SRAction.RREVEAL, info.getLevel(), info.getResultCat().toString(), info.getCombinator()));
        }
        return actions;
    }

    public ArrayList<ArcJAction> unaryActions(CCGJTreeNode top){
        
        ArrayList<ArcJAction> actions = new ArrayList<>();
        String catStr = top.getCCGcat().toString();
        ArrayList<CCGJRuleInfo> list;
        if((list = unaryRules.get(catStr)) != null)
            for(CCGJRuleInfo info : list)
                actions.add(ArcJAction.make(SRAction.RU, 0, info.getResultCat().toString(), info.getCombinator()));
        return actions;
    }
    
    public void printRules(){
        System.out.println("Binary Rules");
        printRules(binaryRules);
        System.out.println("Reveal Rules");
        printRules(revealRules);
    }
    
    public void printRules(HashMap<String, ArrayList<CCGJRuleInfo>> rules){
        for(String key : rules.keySet()){
            ArrayList<CCGJRuleInfo> list = rules.get(key);
            for(CCGJRuleInfo info : list){
                if(info.getCombinator() == RuleType.lreveal || info.getCombinator() == RuleType.rreveal)
                    System.out.println(key+"\t"+info.toString());
            }
        }
    }
}