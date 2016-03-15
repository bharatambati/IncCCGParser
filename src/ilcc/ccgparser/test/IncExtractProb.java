/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.test;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import edinburgh.ccg.deps.CCGcat;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Timing;
import ilcc.ccgparser.incderivation.*;
import ilcc.ccgparser.learn.AvePerceptron;
import ilcc.ccgparser.nnparser.Classifier;
import ilcc.ccgparser.nnparser.Config;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGJRuleInfo;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.CCGRTreeNode;
import ilcc.ccgparser.utils.CCGRTreeNode.Combinator;
import ilcc.ccgparser.utils.DataTypes.*;
import ilcc.ccgparser.utils.DepGraph;
import ilcc.ccgparser.utils.SCoNLLNode;
import ilcc.ccgparser.utils.Utils;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

/**
 *
 * @author ambati
 */
public class IncExtractProb {
    
    int sSize, sentCount;
    boolean lookAhead;
    String testAutoFile, testCoNLLFile, modelFile, outFile;
    
    AvePerceptron model;
    SRParser srparser;
    Map<Integer, CCGJSentence> gcdepsMap;
    List<ArcJAction> actsList;
    Map<ArcJAction, Integer> actsMap;
    
    private Classifier classifier;
    private List<Integer> preComputed;
    private final Config config;
    private Map<String, Integer> wordIDs, posIDs, ccgcatIDs;
    private List<String> knownWords, knownPos, knownCCGCats;    
    
    public IncExtractProb(Properties props) throws IOException {
        config = new Config(props);        
        model = new AvePerceptron();
        sSize = 0;
        testAutoFile = props.getProperty("testAuto");
        testCoNLLFile  = props.getProperty("testCoNLL");
        outFile = props.getProperty("outFile");
        modelFile = props.getProperty("model");
        lookAhead = PropertiesUtils.getBool(props, "lookAhead", false);
        srparser = new RevInc();
        srparser.incalgo = true;
    }
            
    public void postOrder(CCGRTreeNode root, List list){
        if(root == null)
            return;
        postOrder(root.getLeftChild(), list);
        postOrder(root.getRightChild(), list);
        list.add(root);
    }
        
    private void generateIDs() {
        wordIDs = new HashMap<>();
        posIDs = new HashMap<>();
        ccgcatIDs = new HashMap<>();
        
        int index = 0;
        for (String word : knownWords)
            wordIDs.put(word, (index++));
        for (String pos : knownPos)
            posIDs.put(pos, (index++));
        for (String label : knownCCGCats)
            ccgcatIDs.put(label, (index++));
    }
    
    public int getWordID(String s) {
        return wordIDs.containsKey(s) ? wordIDs.get(s) : wordIDs.get(Config.UNKNOWN);
    }
    
    public int getPosID(String s) {
        return posIDs.containsKey(s) ? posIDs.get(s) : posIDs.get(Config.UNKNOWN);
    }
    
    public int getCCGCatID(String s) {
        return ccgcatIDs.containsKey(s) ? ccgcatIDs.get(s) : ccgcatIDs.get("-NULL-");
        //return ccgcatIDs.get(s);
    }
    
    private void updateHeadFeatures(CCGJTreeNode node, List<Integer> fLabel){
        if(node != null){
            fLabel.add(getCCGCatID(node.getConllNode().getccgCat().toString()));
        }
        else{
            fLabel.add(getCCGCatID("-NULL-"));
        }
    }
    
    private void updateStackFeatures(CCGJTreeNode node, List<Integer> fWord, List<Integer> fPos, List<Integer> fLabel){
        if(node != null){
            fWord.add(getWordID(node.getWrdStr())); fPos.add(getPosID(node.getPOS().toString())); fLabel.add(getCCGCatID(node.getCCGcat().toString()));
        }
        else{
            fWord.add(getWordID("-NULL-")); fPos.add(getPosID("-NULL-")); fLabel.add(getCCGCatID("-NULL-"));
        }
    }
    
    private void updateInputFeatures(CCGJTreeNode node, List<Integer> fWord, List<Integer> fPos){
        if(node != null){
            fWord.add(getWordID(node.getWrdStr())); fPos.add(getPosID(node.getPOS().toString()));
        }
        else{
            fWord.add(getWordID("-NULL-")); fPos.add(getPosID("-NULL-"));
        }
    }
    
    private void fillGraphFeats(DepGraph depGraph, Integer ind, List<Integer> fLabel){
        if(ind == null)
            fLabel.add(getCCGCatID("-NULL-"));
        else{
            String cat = depGraph.getVertex(ind).toString();
            fLabel.add(getCCGCatID(cat));
        }
    }
    
    public List<Integer> getFeatures(SRParser srpar, ArrayList<Integer> rightPerList) {
        // Presize the arrays for very slight speed gain. Hardcoded, but so is the current feature list.
        List<Integer> fWord, fPos, fLabel, feature;
        if(lookAhead){
            fWord = new ArrayList<>(12);
            fPos = new ArrayList<>(12);
            fLabel = new ArrayList<>(15);
            feature = new ArrayList<>(39);
        }
        else{
            fWord = new ArrayList<>(9);
            fPos = new ArrayList<>(9);
            fLabel = new ArrayList<>(15);
            feature = new ArrayList<>(33);
        }
        
        CCGJTreeNode s0, s1, s2, s3;
        CCGJTreeNode s0l, s0r, s0u, s0h;
        CCGJTreeNode s1l, s1r, s1u, s1h;
        CCGJTreeNode q0, q1, q2, q3;
        Integer rmost1, rmost2, rmost3, rmost4, rmost5;
        DepGraph depGraph = srpar.depGraph;
        List<CCGJTreeNode> list = srpar.input;
        //input = list;
        int stacksize = srpar.stack.size();
        s0 = stacksize<1 ? null : srpar.stack.get(stacksize-1);
        s1 = stacksize<2 ? null : srpar.stack.get(stacksize-2);
        s2 = stacksize<3 ? null : srpar.stack.get(stacksize-3);
        s3 = stacksize<4 ? null : srpar.stack.get(stacksize-4);
        
        q0 = list.size() > 0 ? list.get(0) : null;
        q1 = list.size() > 1 ? list.get(1) : null;
        q2 = list.size() > 2 ? list.get(2) : null;
        q3 = list.size() > 3 ? list.get(3) : null;
        
        if(s0 != null){
            s0l = s0.getLeftChild(); s0r = s0.getRightChild();
            s0h = (s0.getHeadDir()==0) ? s0l : s0r;
        }
        else{
            s0l = null; s0r = null; s0h = null;
        }
        
        if(s1 != null){
            s1l = s1.getLeftChild(); s1r = s1.getRightChild();
            s1h = (s1.getHeadDir()==0) ? s1l : s1r;
        }
        else{
            s1l = null; s1r = null; s1h = null;
        }
        
        if(srpar.incalgo){
            List<Integer> vlist = getDepGraphFeats(rightPerList);
            rmost1 = vlist.get(0); rmost2 = vlist.get(1); rmost3 = vlist.get(2); rmost4 = vlist.get(3); rmost5 = vlist.get(4);
        }
        else{
            rmost1 = rmost2 = rmost3 = rmost4 = rmost5 = null;
        }
        
        updateStackFeatures(s0, fWord, fPos, fLabel);
        updateStackFeatures(s1, fWord, fPos, fLabel);
        updateStackFeatures(s2, fWord, fPos, fLabel);
        updateStackFeatures(s3, fWord, fPos, fLabel);
        updateStackFeatures(s0l, fWord, fPos, fLabel);
        updateStackFeatures(s0r, fWord, fPos, fLabel);
        updateStackFeatures(s1l, fWord, fPos, fLabel);
        updateStackFeatures(s1r, fWord, fPos, fLabel);
        updateHeadFeatures(s0h, fLabel);
        updateHeadFeatures(s1h, fLabel);
        updateInputFeatures(q0, fWord, fPos);
        
        if(lookAhead){
            updateInputFeatures(q1, fWord, fPos);
            updateInputFeatures(q2, fWord, fPos);
            updateInputFeatures(q3, fWord, fPos);
        }
        
        fillGraphFeats(depGraph, rmost1, fLabel);
        fillGraphFeats(depGraph, rmost2, fLabel);
        fillGraphFeats(depGraph, rmost3, fLabel);
        fillGraphFeats(depGraph, rmost4, fLabel);
        fillGraphFeats(depGraph, rmost5, fLabel);
        
        feature.addAll(fWord);
        feature.addAll(fPos);
        feature.addAll(fLabel);
        return feature;
    }
    
    private List getDepGraphFeats(List<Integer> rightPerList){
        List<Integer> vlist = Arrays.asList(new Integer[5]);
        if(rightPerList != null){
            int size = rightPerList.size();
            if(size>1)
                vlist.set(0, rightPerList.get(size-1));
            if(size>2)
                vlist.set(1, rightPerList.get(size-2));
            if(size>3)
                vlist.set(2, rightPerList.get(size-3));
            if(size>4)
                vlist.set(3, rightPerList.get(size-4));
            if(size>5)
                vlist.set(4, rightPerList.get(size-5));
        }
        return vlist;
    }
    
    private ArrayList<ArcJAction> getAction(SRParser state){
        ArrayList<ArcJAction> actions;
        CCGJTreeNode left, right, inode;
        left = right = inode = null;
        ArrayList<CCGCategory> rightPerList = new ArrayList<>();
        int stacksize = state.stack.size();
        if(state.input.size()>0)
            inode = state.input.get(0);
        if(stacksize>1){
            left = state.stack.get(stacksize-2);
            Integer lvertex = left.getConllNode().getNodeId();
            ArrayList<Integer> rightPeriList = state.depGraph.getRightPer(lvertex);
            for(int i : rightPeriList)
                rightPerList.add(state.depGraph.getVertex(i));
        }
        if(stacksize>0)
            right = state.stack.get(stacksize-1);
        actions = state.treebankRules.getActions(left, right, inode, rightPerList);
        return actions;
    }
    
    public void test(String tconllfile, String tderivfile, String ofile) throws Exception {
        testCoNLLFile = tconllfile;
        testAutoFile = tderivfile;
        outFile = ofile;
        System.err.println("Testing file: " + testAutoFile);
        BufferedReader derivReader = new BufferedReader(new FileReader(new File(testAutoFile)));
        BufferedReader conllReader = new BufferedReader(new FileReader(new File(testCoNLLFile)));
        BufferedWriter oWriter = new BufferedWriter(new FileWriter(new File(outFile)));
        String dLine;
        ArrayList<String> cLines;
        sentCount = 0;
        
        while ((dLine = derivReader.readLine()) !=null ){
            dLine = dLine.trim();
            if(dLine.startsWith("ID=") || dLine.startsWith("#") || dLine.isEmpty())
                continue;
            if(dLine.startsWith("Error")){
                cLines = srparser.getConll(conllReader);
                continue;
            }
            sentCount++;
            if(sentCount%100 == 0)
                System.err.print(sentCount+" ");
            if(sentCount%1000 == 0)
                System.err.println();
            //System.err.println("\n"+sentCount);
            
            cLines = srparser.getConll(conllReader);            
            CCGJSentence sent = new CCGJSentence();
            sent.fillCoNLL(cLines);
            ArrayList<ArcJAction> acts = getActs(dLine, sent);
            getSentProb(acts, sent, oWriter);
        }
        oWriter.close();
    }
    
    private ArrayList<ArcJAction> getActs(String line, CCGJSentence sent){
        sent.setCcgDeriv(line);
        CCGRTreeNode root = parseDrivString(line);
        ArrayList<CCGRTreeNode> list = new ArrayList<>();
        ArrayList<ArcJAction> gActs = new ArrayList<>();
        
        postOrder(root, list);
        for(CCGRTreeNode node : list) {
            Combinator comb = node.getCombinator();
            String combStr = comb.toString();
            if(node.isLeaf()){
                gActs.add(ArcJAction.make(SRAction.SHIFT, 0, node.getCCGcat().toString(), RuleType.valueOf(combStr)));
            }
            else if(node.getChildCount()==1)
                gActs.add(ArcJAction.make(SRAction.RU, 0, node.getCCGcat().toString(), RuleType.valueOf(combStr)));
            else {
                if(comb == Combinator.lreveal)
                    gActs.add(ArcJAction.make(SRAction.LREVEAL, 0, node.getCCGcat().toString(), RuleType.valueOf(combStr)));
                else if(combStr.startsWith("rreveal")){
                    int level = Integer.parseInt(combStr.replace("rreveal", ""));
                    gActs.add(ArcJAction.make(SRAction.RREVEAL, level, node.getCCGcat().toString(), RuleType.rreveal));
                }
                else if(comb == Combinator.frag)
                    gActs.add(ArcJAction.make(SRAction.REDUCE, 0, node.getCCGcat().toString(), RuleType.valueOf(combStr)));
                else if(node.getHeadDir()==1)
                    gActs.add(ArcJAction.make(SRAction.RL, 0, node.getCCGcat().toString(), RuleType.valueOf(combStr)));
                else
                    gActs.add(ArcJAction.make(SRAction.RR, 0, node.getCCGcat().toString(), RuleType.valueOf(combStr)));
            }
        }
        return gActs;
    }
    
    private void getSentProb(ArrayList<ArcJAction> gacts, CCGJSentence sent, BufferedWriter oWriter) throws IOException{
        DecimalFormat df = new DecimalFormat("0.0000");
        srparser.initVars(sent);
        for(ArcJAction act : gacts){            
            if(act.getccgCat().toString().equals("X")){
                //oWriter.write(" "+act.getAction().toString()+"  0.000001");
                oWriter.write(" "+act.toString2()+"  0.000001");
                //System.err.print(" "+act.getAction().toString()+"  "+df.format(0.0001));
                continue;
            }
            ArrayList<Integer> rightPerList = null;
            int stacksize = srparser.stack.size();
            if(srparser.incalgo && stacksize > 1){
                CCGJTreeNode left = srparser.stack.get(stacksize-2);
                Integer lvertex = left.getConllNode().getNodeId();
                rightPerList = srparser.depGraph.getRightPer(lvertex);
            }
            List<ArcJAction> acts, nacts;
            int[] fArray = Ints.toArray(getFeatures(srparser, rightPerList));
            acts = getAction(srparser);
            List<Integer> olist = new ArrayList<>(acts.size());
            nacts = new ArrayList<>();
            for(ArcJAction action : acts){
                if(actsMap.containsKey(action))
                    nacts.add(action);
            }
            int index = -1;
            for(int i=0;i<nacts.size();i++){
                ArcJAction action = nacts.get(i);
                olist.add(actsMap.get(action));
                if(action.equals(act)) index = i;
            }
            double[] scores = classifier.computeScores(fArray, olist);
            //scores = softmax(scores, olist);
            scores = softmax2(scores, olist);
            
            double score = (index!=-1) ? Math.exp(scores[index]) : 0.000001;
                        
            //System.err.println(" "+act.getAction().toString()+"  "+score);
            srparser.applyAction(act);
            
            //Print probabilities            
            String actStr = act.getAction().toString();
            if("SHIFT".equals(actStr))
                oWriter.write("\n"+srparser.stack.peek().getHeadWrd()+"\t");
                //System.err.print("\n"+srparser.stack.peek().getHeadWrd()+"\t");
            oWriter.write(" "+act.toString2()+"  "+score);
        }
        oWriter.write("\n");
        oWriter.flush();
        //System.err.println();
    }
    
    private double[] softmax3(double[] scores, List<ArcJAction> acts){
        
        if(acts.isEmpty())
            return scores;
        
        int optLabel = -1;
        Integer id;
        for(ArcJAction action : acts){
            id = actsMap.get(action);
            if (optLabel < 0 || scores[id] > scores[optLabel])
                optLabel = id;
        }
        
        double maxScore = scores[optLabel];
        double sum = 0.0;
        
        for(ArcJAction action : acts){
            id = actsMap.get(action);
            sum += Math.exp(scores[id] - maxScore);
        }
        
        for(ArcJAction action : acts){
            id = actsMap.get(action);
            //scores[id] = Math.exp(scores[id] - maxScore - Math.log(sum));
            scores[id] = (scores[id] - maxScore - Math.log(sum));
        }
        
        return scores;
    }
    
    private double[] softmax2(double[] scores, List<Integer> olist){
        
        if(olist.isEmpty())
            return scores;
        
        int optLabel = -1;
        for(int i=0; i< olist.size(); i++){
            if (optLabel < 0 || scores[i] > scores[optLabel])
                optLabel = i;
        }
        double maxScore = scores[optLabel];
        double sum = 0.0;
        
        for(int i=0; i< olist.size(); i++){
            sum += Math.exp(scores[i] - maxScore);
        }
        
        for(int i=0; i< olist.size(); i++){
            //scores[id] = Math.exp(scores[id] - maxScore - Math.log(sum));
            scores[i] = (scores[i] - maxScore - Math.log(sum));
        }
        
        return scores;
    }
    
    private double[] softmax(double[] scores, List<Integer> olist){
        
        if(olist.isEmpty())
            return scores;
        
        double total = 0.0;
        for(int i=0; i< olist.size(); i++){
            total += Math.exp(scores[i]);
        }
        
        for(int i=0; i< olist.size(); i++){
            scores[i] = scores[i] - Math.log(total);
        }
        
        return scores;
    }
    
    public CCGRTreeNode parseDrivString(String treeString) {
        
        Stack<CCGRTreeNode> nodes = new Stack<>();
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
                    SCoNLLNode cnode = Utils.scNodeFromString(id+1, nodeString);
                    CCGRTreeNode node = CCGRTreeNode.makeLeaf(cnode.getccgCat(), Combinator.lexicon, cnode);
                    nodes.add(node);
                    id++;
                }
                else if (nodeString.charAt(1) == 'T') {
                    // (<T S/S fa 0 2> (<L (S/S)/NP IN IN In (S_113/S_113)/NP_114>)
                    ArrayList<String> items = Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> ")).trimResults().omitEmptyStrings().split(nodeString));
                    CCGCategory rescat = CCGCategory.make(items.get(1));
                    Combinator comb = Combinator.valueOf(items.get(2));
                    boolean headIsLeft;
                    int childrenSize = Integer.parseInt(items.get(4));
                    CCGRTreeNode node;
                    CCGRTreeNode left;
                    CCGRTreeNode right;
                    int size = nodes.size();
                    
                    if(childrenSize == 2){
                        left = nodes.get(size-2);
                        right = nodes.get(size-1);
                        headIsLeft = (Integer.parseInt(items.get(3))==0);
                        node = CCGRTreeNode.makeBinary(rescat, comb, headIsLeft, left, right);
                        left.setParent(node);
                        right.setParent(node);
                    }
                    else {
                        left = nodes.get(size-1);
                        node = CCGRTreeNode.makeUnary(rescat, comb, left);
                        left.setParent(node);
                    }
                    while (childrenSize > 0) {
                        nodes.pop();
                        childrenSize--;
                    }
                    nodes.add(node);
                }
            } else {
                cStack.add(c);
            }
        }
        
        Preconditions.checkArgument(nodes.size() == 1, "Bad Tree");
        return nodes.pop();
    }
    
    public void loadModelFile(String modelFile) throws IOException {
        loadModelFile(modelFile, true);
    }
    
    private void loadModelFile(String modelFile, boolean verbose) throws IOException {
        Timing t = new Timing();
        try {
            
            System.err.println("Loading ccg parser model file: " + modelFile + " ... ");
            String s;
            BufferedReader input = IOUtils.readerFromString(modelFile);
            
            s = input.readLine();
            int nDict = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nPOS = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nccgCat = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int eSize = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int hSize = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nTokens = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nPreComputed = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int classes = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nuRules = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nbRules = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nrRules = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            
            actsMap = new HashMap<>();
            knownWords = new ArrayList<>();
            knownPos = new ArrayList<>();
            knownCCGCats = new ArrayList<>();
            
            double[][] E = new double[nDict + nPOS + nccgCat][eSize];
            String[] splits;
            int index = 0;
            
            for (int k = 0; k < classes; k++) {
                s = input.readLine().trim();
                splits = s.split("--");
                actsMap.put(ArcJAction.make(SRAction.valueOf(splits[0]), Integer.parseInt(splits[1]), splits[2], RuleType.valueOf(splits[3])), k);
            }
            
            for (int k = 0; k < nuRules; k++) {
                s = input.readLine().trim();
                splits = s.split("  ");
                String key = splits[0];
                for(int i = 1; i < splits.length; i++){
                    String[] parts = splits[i].split("--");
                    CCGJRuleInfo info = new CCGJRuleInfo(CCGcat.ccgCatFromString(parts[0]), null, CCGcat.ccgCatFromString(parts[2]), parts[3].equals("true"), RuleType.valueOf(parts[4]), Integer.parseInt(parts[5]), 0);
                    srparser.treebankRules.addUnaryRuleInfo(info, key);
                }
            }
            
            for (int k = 0; k < nbRules; k++) {
                s = input.readLine().trim();
                splits = s.split("  ");
                String key = splits[0];
                for(int i = 1; i < splits.length; i++){
                    String[] parts = splits[i].split("--");
                    CCGJRuleInfo info = new CCGJRuleInfo(CCGcat.ccgCatFromString(parts[0]), CCGcat.ccgCatFromString(parts[1]), CCGcat.ccgCatFromString(parts[2]), parts[3].equals("true"), RuleType.valueOf(parts[4]), Integer.parseInt(parts[5]), 0);
                    srparser.treebankRules.addBinaryRuleInfo(info, key);
                }
            }
            
            for (int k = 0; k < nrRules; k++) {
                s = input.readLine().trim();
                splits = s.split("  ");
                String key = splits[0];
                for(int i = 1; i < splits.length; i++){
                    String[] parts = splits[i].split("--");
                    CCGJRuleInfo info = new CCGJRuleInfo(CCGcat.ccgCatFromString(parts[0]), CCGcat.ccgCatFromString(parts[1]), CCGcat.ccgCatFromString(parts[2]), parts[3].equals("true"), RuleType.valueOf(parts[4]), Integer.parseInt(parts[5]), 0);
                    srparser.treebankRules.addRevealRuleInfo(info, key);
                }
            }
            
            for (int k = 0; k < nDict; ++k) {
                s = input.readLine();
                splits = s.split(" ");
                knownWords.add(splits[0]);
                for (int i = 0; i < eSize; ++i)
                    E[index][i] = Double.parseDouble(splits[i + 1]);
                index = index + 1;
            }
            for (int k = 0; k < nPOS; ++k) {
                s = input.readLine();
                splits = s.split(" ");
                knownPos.add(splits[0]);
                for (int i = 0; i < eSize; ++i)
                    E[index][i] = Double.parseDouble(splits[i + 1]);
                index = index + 1;
            }
            for (int k = 0; k < nccgCat; ++k) {
                s = input.readLine();
                splits = s.split(" ");
                knownCCGCats.add(splits[0]);
                for (int i = 0; i < eSize; ++i)
                    E[index][i] = Double.parseDouble(splits[i + 1]);
                index = index + 1;
            }
            generateIDs();
            
            double[][] W1 = new double[hSize][eSize * nTokens];
            for (int j = 0; j < W1[0].length; ++j) {
                s = input.readLine();
                splits = s.split(" ");
                for (int i = 0; i < W1.length; ++i)
                    W1[i][j] = Double.parseDouble(splits[i]);
            }
            
            double[] b1 = new double[hSize];
            s = input.readLine();
            splits = s.split(" ");
            for (int i = 0; i < b1.length; ++i)
                b1[i] = Double.parseDouble(splits[i]);
            
            double[][] W2 = new double[classes][hSize];
            for (int j = 0; j < W2[0].length; ++j) {
                s = input.readLine();
                splits = s.split(" ");
                for (int i = 0; i < W2.length; ++i)
                    W2[i][j] = Double.parseDouble(splits[i]);
            }
            
            preComputed = new ArrayList<Integer>();
            while (preComputed.size() < nPreComputed) {
                s = input.readLine();
                splits = s.split(" ");
                for (String split : splits) {
                    preComputed.add(Integer.parseInt(split));
                }
            }
            input.close();
            classifier = new Classifier(config, E, W1, b1, W2, preComputed);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        
        // initialize the loaded parser
        
        // Pre-compute matrix multiplications
        if (config.numPreComputed > 0) {
            classifier.preCompute();
        }
        t.done("Initializing ccg parser");
    }
    
    private static final Map<String, Integer> numArgs = new HashMap<>();
    static {
        numArgs.put("textFile", 1);
        numArgs.put("outFile", 1);
    }
    
    public static void main(String[] args) throws IOException, Exception {
        
        String testAutoFile, testConllFile, outFile, modelFile, embedFile;
        
        String home = "/home/ambati/ilcc/projects/parsing/experiments/english/ccg/useful/ccg-all/";
        modelFile = home+"models/nn.gincint.model.txt.gz";
        embedFile = "/home/ambati/ilcc/tools/neural-networks/embeddings/turian/embeddings.raw";
        testAutoFile = home+"/devel.per.b16incin.auto";
        testConllFile = home+"../../data/final/devel.innccg.conll";
        outFile = home+"/out1.txt";
        
        String eeg = "/home/ambati/ilcc/projects/parsing/experiments/eeg/data/";
        testAutoFile = eeg+"inc/wsj.eeg.inninc.auto";
        testConllFile = eeg+"inc/wsj.eeg.conll";
        outFile = eeg+"inc/wsj.eeg.inninc.out.txt";
        
        testAutoFile = eeg+"oracle/wsj.eeg.out.auto";        
        testConllFile = eeg+"oracle/wsj.eeg.inc.conll";
        outFile = eeg+"oracle/wsj.eeg.out.allacts.txt";
        testConllFile = eeg+"oracle/wsj.eeg.conll";
        outFile = eeg+"oracle/wsj.eeg.out.txt";
        
        
        if(args.length == 0){
            args = new String[] {
                "-testAuto", testAutoFile, "-testCoNLL", testConllFile, "-outFile", outFile, "-model", modelFile, "-embedFile", embedFile,
            };
        }
        
        Properties props = StringUtils.argsToProperties(args, numArgs);
        IncExtractProb incnnpar = new IncExtractProb(props);
        
        long start;               
        System.err.println("Loading Model: " + new Date(System.currentTimeMillis()) +"\n");
            incnnpar.loadModelFile(props.getProperty("model"));
            
        System.err.println("Started Parsing: " + new Date(System.currentTimeMillis()) +"\n");
        start = (long) (System.currentTimeMillis());
            incnnpar.test(props.getProperty("testCoNLL"), props.getProperty("testAuto"), props.getProperty("outFile"));
        System.err.println("Parsing Time: " + (System.currentTimeMillis() - start) / 1000.0 + " (s)");
    }
}
