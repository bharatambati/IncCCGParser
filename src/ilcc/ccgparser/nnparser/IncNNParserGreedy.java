/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.nnparser;

import com.google.common.primitives.Ints;
import edinburgh.ccg.deps.CCGcat;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Timing;
import ilcc.ccgparser.incderivation.*;
import ilcc.ccgparser.utils.DataTypes.GoldccgInfo;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGJRuleInfo;
import ilcc.ccgparser.utils.CCGJRules;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.DataTypes.*;
import ilcc.ccgparser.utils.DepGraph;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Stack;

/**
 *
 * @author ambati
 */
public class IncNNParserGreedy extends IncParser {
    
    private final CCGJRules rules;
    int sentCount;
    List<SRParser> agenda;
    
    Stack<CCGJTreeNode> stack;
    List<CCGJTreeNode> input;
    Map<Integer, CCGJSentence> gcdepsMap;
    List<ArcJAction> actsList;
    private Map<String, Integer> wordIDs, posIDs, ccgcatIDs;
    private Classifier classifier;
    private List<Integer> preComputed;
    private final Config config;
    private List<String> knownWords, knownPos, knownCCGCats;    
    
    public IncNNParserGreedy(Properties props) throws IOException {
        config = new Config(props);
        super.init(props);        
        rules = new CCGJRules();
        agenda = new ArrayList<>(beamSize);
    }
    
    public class TempNNAgenda {
        private final SRParser state;
        private final ArcJAction action;
        private final int[] featArray;
        private final double score;
        
        public TempNNAgenda(SRParser item, ArcJAction act, int[] feats, double val){
            state = item;
            action = act;
            featArray = feats;
            score = val;
        }
        
        public double getScore(){
            return score;
        }
        
        public SRParser getState(){
            return state;
        }
        
        public ArcJAction getAction(){
            return action;
        }
        
        public int[] getFeatArray(){
            return featArray;
        }
    }
    
    public void addRules(String unaryRuleFile, String binaryRuleFile) throws IOException{
        rules.addRules(unaryRuleFile, binaryRuleFile);
    }
    
    public void fillVars(String autoFile, String conllFile, String tPargFile, String tConllFile) {
        trainAutoFile = autoFile;
        trainCoNLLFile = conllFile;
        testPargFile = tPargFile;
        testCoNLLFile = tConllFile;
    }
    
    @Override
    public List<CCGJTreeNode> fillData() throws Exception {
        return srparser.fillData(trainCoNLLFile, trainAutoFile, trainAutoFile, goldDetails);
    }
    
    private void loadCCGFiles(String conllFile, String derivFile, String depsFile, List<CCGJSentence> sents, List<CCGJTreeNode> trees) throws IOException, Exception {
        srparser.fillData(trainCoNLLFile, trainAutoFile, trainAutoFile, goldDetails);
        for(CCGJTreeNode tree : srparser.parseSents(goldDetails))
            trees.add(tree);
        
        for(int i = 0; i < goldDetails.size(); i++ ){
            sentCount = i+1;
            GoldccgInfo gccginfo = goldDetails.get(sentCount);
            List<ArcJAction> arcActList = gccginfo.getarcActs();
            if(arcActList != null)
                addtoactlist(arcActList);
        }
        updateActMap(sents);
    }
    
    private void updateActMap(List<CCGJSentence> sents){
        HashMap<ArcJAction, Integer> map = new HashMap();
        int ninc, nact;
        ninc = nact = 0;
        
        for(ArcJAction act : actsMap.keySet()){
            if(actsMap.get(act) > 2)
                map.put(act, actsMap.get(act));
        }
        
        actsList = new ArrayList<>(map.size());
        actsList.addAll(map.keySet());
        Collections.sort(actsList);
        map.clear();
        for(int i=0; i< actsList.size(); i++)
            map.put(actsList.get(i), i);
        actsMap = map;
        
        for(int i = 0; i < goldDetails.size(); i++ ){
            GoldccgInfo gccginfo = goldDetails.get(i+1);
            List<ArcJAction> arcActList = gccginfo.getarcActs();
            if(arcActList == null){
                sents.add(null);
                ninc++;
                continue;
            }
            
            boolean flag = true;
            for(ArcJAction act : arcActList){
                if(!actsMap.containsKey(act)){
                    goldDetails.put(i+1, new GoldccgInfo(null, gccginfo.getccgDeps(), gccginfo.getccgSent()));
                    flag = false;
                    break;
                }
            }
            if(flag)
                sents.add(gccginfo.getccgSent());
            else{
                sents.add(null);
                nact++;
            }
        }
        System.err.println("\nSentences Total: "+goldDetails.size()+" inc-conversion errors: "+ninc + " action list pruning: " + nact);
    }
    
    private void addtoactlist(List<ArcJAction> gActs){
        for(ArcJAction act : gActs){
            Integer counter = actsMap.get(act);
            actsMap.put(act, counter==null ? 1 : counter+1);
        }
    }
    
    public void postOrder(CCGJTreeNode root, List list){
        if(root == null)
            return;
        postOrder(root.getLeftChild(), list);
        postOrder(root.getRightChild(), list);
        list.add(root);
    }
    
    private void genDictionaries(List<CCGJSentence> sents, List<CCGJTreeNode> trees) {
        List<String> word = new ArrayList<>();
        List<String> pos = new ArrayList<>();
        List<String> ccgcat = new ArrayList<>();
        
        for (CCGJSentence sentence : sents) {
            if(sentence==null) continue;
            for (CCGJTreeNode node : sentence.getNodes()) {
                word.add(node.getWrdStr());
                pos.add(node.getPOS().getPos());
            }
        }
        
        for (CCGJTreeNode tree : trees){
            if(tree==null) continue;
            ArrayList<CCGJTreeNode> list = new ArrayList<>();
            postOrder(tree, list);
            for (CCGJTreeNode node : list)
                ccgcat.add(node.getCCGcat().toString());
        }
        
        // Generate "dictionaries," possibly with frequency cutoff
        knownWords = Util.generateDict(word, config.wordCutOff);
        knownPos = Util.generateDict(pos);
        knownCCGCats = Util.generateDict(ccgcat);
        
        knownWords.add(0, Config.UNKNOWN);
        knownWords.add(1, Config.NULL);
        knownWords.add(2, Config.ROOT);
        
        knownPos.add(0, Config.UNKNOWN);
        knownPos.add(1, Config.NULL);
        knownPos.add(2, Config.ROOT);
        
        knownCCGCats.add(0, Config.NULL);
        generateIDs();
        
        System.err.println(Config.SEPARATOR);
        System.err.println("#Word: " + knownWords.size());
        System.err.println("#POS:" + knownPos.size());
        System.err.println("#CCGCats: " + knownCCGCats.size());
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
    
    private double[][] readEmbedFile(String embedFile, Map<String, Integer> embedID) {
        
        double[][] embeddings = null;
        if (embedFile != null) {
            BufferedReader input = null;
            try {
                input = IOUtils.readerFromString(embedFile);
                List<String> lines = new ArrayList<String>();
                for (String s; (s = input.readLine()) != null; ) {
                    lines.add(s);
                }
                
                int nWords = lines.size();
                String[] splits = lines.get(0).split("\\s+");
                
                int dim = splits.length - 1;
                embeddings = new double[nWords][dim];
                System.err.println("Embedding File " + embedFile + ": #Words = " + nWords + ", dim = " + dim);
                
                if (dim != config.embeddingSize)
                    throw new IllegalArgumentException("The dimension of embedding file does not match config.embeddingSize");
                
                for (int i = 0; i < lines.size(); ++i) {
                    splits = lines.get(i).split("\\s+");
                    embedID.put(splits[0], i);
                    for (int j = 0; j < dim; ++j)
                        embeddings[i][j] = Double.parseDouble(splits[j + 1]);
                }
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            } finally {
                IOUtils.closeIgnoringExceptions(input);
            }
        }
        return embeddings;
    }
    
    public int getWordID(String s) {
        return wordIDs.containsKey(s) ? wordIDs.get(s) : wordIDs.get(Config.UNKNOWN);
    }
    
    public int getPosID(String s) {
        return posIDs.containsKey(s) ? posIDs.get(s) : posIDs.get(Config.UNKNOWN);
    }
    
    public int getCCGCatID(String s) {
        //return ccgcatIDs.containsKey(s) ? ccgcatIDs.get(s) : ccgcatIDs.get(Config.UNKNOWN);
        return ccgcatIDs.get(s);
    }
    
    // Prepare a classifier for training with the given dataset.
    private void setupClassifierForTraining(List<CCGJSentence> trainSents, List<CCGJTreeNode> trainTrees, String embedFile, String preModel) throws IOException {
        double[][] E = new double[knownWords.size() + knownPos.size() + knownCCGCats.size()][config.embeddingSize];
        double[][] W1 = new double[config.hiddenSize][config.embeddingSize * config.numTokens];
        double[] b1 = new double[config.hiddenSize];
        double[][] W2 = new double[actsList.size()][config.hiddenSize];
        
        // Randomly initialize weight matrices / vectors
        Random random = Util.getRandom();
        for (int i = 0; i < W1.length; ++i)
            for (int j = 0; j < W1[i].length; ++j)
                W1[i][j] = random.nextDouble() * 2 * config.initRange - config.initRange;
        
        for (int i = 0; i < b1.length; ++i)
            b1[i] = random.nextDouble() * 2 * config.initRange - config.initRange;
        
        for (int i = 0; i < W2.length; ++i)
            for (int j = 0; j < W2[i].length; ++j)
                W2[i][j] = random.nextDouble() * 2 * config.initRange - config.initRange;
        
        // Read embeddings into `embedID`, `embeddings`
        Map<String, Integer> embedID = new HashMap<String, Integer>();
        double[][] embeddings = readEmbedFile(embedFile, embedID);
        
        // Try to match loaded embeddings with words in dictionary
        int foundEmbed = 0;
        for (int i = 0; i < E.length; ++i) {
            int index = -1;
            if (i < knownWords.size()) {
                String str = knownWords.get(i);
                //NOTE: exact match first, and then try lower case..
                if (embedID.containsKey(str)) index = embedID.get(str);
                else if (embedID.containsKey(str.toLowerCase())) index = embedID.get(str.toLowerCase());
            }
            if (index >= 0) {
                ++foundEmbed;
                for (int j = 0; j < E[i].length; ++j)
                    E[i][j] = embeddings[index][j];
            } else {
                for (int j = 0; j < E[i].length; ++j)
                    E[i][j] = random.nextDouble() * 0.02 - 0.01;
            }
        }
        System.err.println("Found embeddings: " + foundEmbed + " / " + knownWords.size());
        
        if (preModel != null) {
            try {
                System.err.println("Loading pre-trained model file: " + preModel + " ... ");
                String s;
                BufferedReader input = IOUtils.readerFromString(preModel);
                
                s = input.readLine();
                int nDict = Integer.parseInt(s.substring(s.indexOf('=') + 1));
                s = input.readLine();
                int nPOS = Integer.parseInt(s.substring(s.indexOf('=') + 1));
                s = input.readLine();
                int nLabel = Integer.parseInt(s.substring(s.indexOf('=') + 1));
                s = input.readLine();
                int eSize = Integer.parseInt(s.substring(s.indexOf('=') + 1));
                s = input.readLine();
                int hSize = Integer.parseInt(s.substring(s.indexOf('=') + 1));
                s = input.readLine();
                int nTokens = Integer.parseInt(s.substring(s.indexOf('=') + 1));
                s = input.readLine();
                
                String[] splits;
                for (int k = 0; k < nDict; ++k) {
                    s = input.readLine();
                    splits = s.split(" ");
                    if (wordIDs.containsKey(splits[0]) && eSize == config.embeddingSize) {
                        int index = getWordID(splits[0]);
                        for (int i = 0; i < eSize; ++i)
                            E[index][i] = Double.parseDouble(splits[i + 1]);
                    }
                }
                
                for (int k = 0; k < nPOS; ++k) {
                    s = input.readLine();
                    splits = s.split(" ");
                    if (posIDs.containsKey(splits[0]) && eSize == config.embeddingSize) {
                        int index = getPosID(splits[0]);
                        for (int i = 0; i < eSize; ++i)
                            E[index][i] = Double.parseDouble(splits[i + 1]);
                    }
                }
                
                for (int k = 0; k < nLabel; ++k) {
                    s = input.readLine();
                    splits = s.split(" ");
                    if (ccgcatIDs.containsKey(splits[0]) && eSize == config.embeddingSize) {
                        int index = getCCGCatID(splits[0]);
                        for (int i = 0; i < eSize; ++i)
                            E[index][i] = Double.parseDouble(splits[i + 1]);
                    }
                }
                
                boolean copyLayer1 = hSize == config.hiddenSize && config.embeddingSize == eSize && config.numTokens == nTokens;
                if (copyLayer1) {
                    System.err.println("Copying parameters W1 && b1...");
                }
                for (int j = 0; j < eSize * nTokens; ++j) {
                    s = input.readLine();
                    if (copyLayer1) {
                        splits = s.split(" ");
                        for (int i = 0; i < hSize; ++i)
                            W1[i][j] = Double.parseDouble(splits[i]);
                    }
                }
                
                s = input.readLine();
                if (copyLayer1) {
                    splits = s.split(" ");
                    for (int i = 0; i < hSize; ++i)
                        b1[i] = Double.parseDouble(splits[i]);
                }
                
                boolean copyLayer2 = (nLabel * 2 - 1 == actsList.size()) && hSize == config.hiddenSize;
                if (copyLayer2)
                    System.err.println("Copying parameters W2...");
                for (int j = 0; j < hSize; ++j) {
                    s = input.readLine();
                    if (copyLayer2) {
                        splits = s.split(" ");
                        for (int i = 0; i < nLabel * 2 - 1; ++i)
                            W2[i][j] = Double.parseDouble(splits[i]);
                    }
                }
                input.close();
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        }
        Dataset trainSet = genTrainExamples(trainSents, trainTrees);
        classifier = new Classifier(config, trainSet, E, W1, b1, W2, preComputed);
    }
    
    public Dataset genTrainExamples(List<CCGJSentence> sents, List<CCGJTreeNode> trees) throws IOException {
        int numTrans = actsList.size();
        Dataset ret = new Dataset(config.numTokens, numTrans);
        
        Counter<Integer> tokPosCount = new IntCounter<>();
        System.err.println(Config.SEPARATOR);
        System.err.println("Generate training examples...");
        System.err.println("With #transitions: "+numTrans);
        double start = (long) (System.currentTimeMillis()), end;
        System.err.println("Started at: " + new Date(System.currentTimeMillis()));
        
        for (int i = 0; i < sents.size(); ++i) {
            if (i > 0) {
                //System.err.print(i + " ");
                if (i % 1000 == 0)
                    System.err.print(i + " ");
                if (i % 10000 == 0 || i == sents.size() - 1)
                    System.err.println();
            }
            
            CCGJSentence sent = sents.get(i);
            if(sent==null)
                continue;
            srparser.initVars(sent);
            List<ArcJAction> gActList = goldDetails.get(i+1).getarcActs();
            for(ArcJAction gAct : gActList){
                ArrayList<ArcJAction> acts = getAction(srparser);
                ArrayList<Integer> rightPerList = null;
                int stacksize = srparser.stack.size();
                if(srparser.incalgo && stacksize > 1){
                    CCGJTreeNode left = srparser.stack.get(stacksize-2);
                    Integer lvertex = left.getConllNode().getNodeId();
                    rightPerList = srparser.depGraph.getRightPer(lvertex);
                }
                List<Integer> feature = getFeatures(srparser, rightPerList, sent);
                
                List<Integer> label = new ArrayList<>(Collections.nCopies(numTrans, -1));
                for(ArcJAction act : acts){
                    Integer id = actsMap.get(act);
                    if(id != null){
                        if(act.equals(gAct))
                            label.set(id, 1);
                        else
                            label.set(id, 0);
                    }
                }
                ret.addExample(feature, label);
                for (int j = 0; j < feature.size(); ++j)
                    tokPosCount.incrementCount(feature.get(j) * feature.size() + j);
                srparser.applyAction(gAct);
            }
        }
        System.err.println("#Train Examples: " + ret.n);
        end = (long) System.currentTimeMillis();
        System.err.println("Ended at : " + new Date(System.currentTimeMillis()) + " taking "+0.001*(end-start) + " secs");
        
        List<Integer> sortedTokens = Counters.toSortedList(tokPosCount, false);
        preComputed = new ArrayList<>(sortedTokens.subList(0, Math.min(config.numPreComputed, sortedTokens.size())));
        
        return ret;
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
    
    public List<Integer> getFeatures(SRParser srpar, ArrayList<Integer> rightPerList, CCGJSentence sent) {
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
    
    public void train(String trainFile, String devFile, String modelFile, String embedFile, String preModel) throws Exception {
        System.err.println("Train File: " + trainFile);
        System.err.println("Dev File: " + devFile);
        System.err.println("Model File: " + modelFile);
        System.err.println("Embedding File: " + embedFile);
        System.err.println("Pre-trained Model File: " + preModel);
        
        List<CCGJSentence> trainSents = new ArrayList<>();
        List<CCGJTreeNode> trainTrees = new ArrayList<>();
        loadCCGFiles(trainCoNLLFile, trainAutoFile, trainPargFile, trainSents, trainTrees);
        
        List<CCGJSentence> devSents = new ArrayList<>();
        List<CCGJTreeNode> devTrees = new ArrayList<>();
        if (devFile != null) {
            loadTestFiles(testCoNLLFile, testAutoFile, testPargFile, devSents, devTrees);
        }
        genDictionaries(trainSents, trainTrees);
        
        // Initialize a classifier; prepare for training
        setupClassifierForTraining(trainSents, trainTrees, embedFile, preModel);
        
        System.err.println(Config.SEPARATOR);
        config.printParameters();
        
        long startTime = System.currentTimeMillis();
        // Track the best UAS performance we've seen.
        double bestLF = 0;
        
        for (int iter = 0; iter < config.maxIter; ++iter) {
            System.err.println("##### Iteration " + iter);
            
            Classifier.Cost cost = classifier.computeCostFunction(config.batchSize, config.regParameter, config.dropProb);
            System.err.println("Cost = " + cost.getCost() + ", Correct(%) = " + cost.getPercentCorrect());
            classifier.takeAdaGradientStep(cost, config.adaAlpha, config.adaEps);
            
            System.err.println("Elapsed Time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " (s)");
            
            // evaluation
            if (devFile != null && iter % config.evalPerIter == 0) {
                // Redo precomputation with updated weights. This is only
                // necessary because we're updating weights -- for normal
                // prediction, we just do this once in #initialize
                classifier.preCompute();
                
                double lf = parse(devSents);
                System.err.println("LF: " + lf);
                
                if (config.saveIntermediate && lf > bestLF) {
                    System.err.printf("Exceeds best previous LF of %f. Saving model file..%n", bestLF);
                    bestLF = lf;
                    writeModelFile(modelFile);
                }
            }
            
            // Clear gradients
            if (config.clearGradientsPerIter > 0 && iter % config.clearGradientsPerIter == 0) {
                System.err.println("Clearing gradient histories..");
                classifier.clearGradientHistories();
            }
        }
        
        classifier.finalizeTraining();
        
        //*
        if (devFile != null) {
            // Do final UAS evaluation and save if final model beats the best intermediate one
            double lf = parse(devSents);
            if (lf > bestLF) {
                System.err.printf("Final model LF: %f%n", lf);
                System.err.printf("Exceeds best previous LF of %f. Saving model file..%n", bestLF);
                writeModelFile(modelFile);
            }
        } else {
            writeModelFile(modelFile);
        }
        //*/
    }
    
    public void test(String tconllfile, String tderivfile, String tpargfile) throws Exception {
        testCoNLLFile = tconllfile;
        testAutoFile = tderivfile;
        testPargFile = tpargfile;
        System.err.println("Testing file: " + testCoNLLFile);
        
        List<CCGJSentence> devSents = new ArrayList<>();
        List<CCGJTreeNode> devTrees = new ArrayList<>();
        loadTestFiles(testCoNLLFile, testAutoFile, testPargFile, devSents, devTrees);
        double lf = parse(devSents);
    }
    
    private void loadTestFiles(String conllFile, String derivFile, String pargFile, List<CCGJSentence> sents, List<CCGJTreeNode> trees) throws FileNotFoundException, IOException{
        BufferedReader derivReader = new BufferedReader(new FileReader(new File(pargFile)));
        BufferedReader conllReader = new BufferedReader(new FileReader(new File(conllFile)));
        gcdepsMap = getccgDepMap(derivReader);
        
        ArrayList<String> cLines;
        
        for(int i=1;i<=gcdepsMap.size();i++){
            cLines = srparser.getConll(conllReader);
            CCGJSentence sent = new CCGJSentence();
            sent.fillCoNLL(cLines);
            //sent.updateConllDeps(cLines);
            sents.add(sent);
        }
    }
    
    public double parse(List<CCGJSentence> sentences) throws IOException, Exception{
        
        BufferedWriter oPargWriter = new BufferedWriter(new FileWriter(new File(outPargFile)));
        oPargWriter.write("# Generated using \n# CCGParser.java\n\n");
        int id = 0;
        srparser.init();
        srparser.incalgo = true;
        for(CCGJSentence sent: sentences){
            id++;
            if(id%100 == 0)
                System.err.print(" "+id);
            if(id%1000 == 0)
                System.err.println();
            
            parse(sent, id);
            CCGJSentence gsent = gcdepsMap.get(id);
            srparser.goldccgDeps = gsent.getPargDeps();
            srparser.sent = sent;
            srparser.writeDeps(oPargWriter);
            srparser.evaluateParseDependenciesJulia();
            srparser.totCat += gsent.getNodes().size();
            srparser.updateCatAccuray(gsent);
        }
        srparser.sentCount = sentences.size();
        srparser.printResults();
        return srparser.LF;
    }
    
    public void parse(CCGJSentence sent, int sentId) throws Exception {
        if(debug >= 1)
            System.err.println("\n"+sentId);
        
        srparser.initVars(sent);
        double srscore = 0.0;
        
        agenda.clear();
        agenda.add(srparser);
        boolean flag = true;
        while(flag){
            ArcJAction parserAct;
            
            List<TempNNAgenda> tempAgenda = new ArrayList<>();
            SRParser item  = srparser;
            ArrayList<Integer> rightPerList = null;
            int stacksize = item.stack.size();
            if(srparser.incalgo && stacksize > 1){
                CCGJTreeNode left = item.stack.get(stacksize-2);
                Integer lvertex = left.getConllNode().getNodeId();
                rightPerList = item.depGraph.getRightPer(lvertex);
                if(debug >= 2)
                    System.err.print("\t"+left.getCCGcat().toString()+"  "+item.stack.get(stacksize-1).getCCGcat().toString());
            }
            List<ArcJAction> acts, nacts;
            int[] fArray = Ints.toArray(getFeatures(item, rightPerList, sent));
            acts = getAction(item);
            List<Integer> olist = new ArrayList<>(acts.size());
            nacts = new ArrayList<>();
            for(ArcJAction action : acts){
                if(actsMap.containsKey(action))
                    nacts.add(action);
            }
            
            /*
            double[] scores = classifier.computeScores(fArray);
            for(ArcJAction action : nacts){
                Integer id = actsMap.get(action);
                double score = (beamSize!=1) ? (srscore + scores[id]) : scores[id] ;
                TempNNAgenda tmp = new TempNNAgenda(item, action, fArray, score);
                addToList(tempAgenda, tmp);
            }*/
                       
            ///*
            for(ArcJAction action : nacts)
                olist.add(actsMap.get(action));
            double[] scores = classifier.computeScores(fArray, olist);
            
            if(beamSize!=1)
                scores = softmax(scores, olist);
                        
            for(int i=0; i<nacts.size();i++){
                ArcJAction action = nacts.get(i);
                double score = (beamSize!=1) ? (srscore + scores[i]) : scores[i];
                //double score = scores[i] ;
                //double score = item.getScore() * scores[id];
                TempNNAgenda tmp = new TempNNAgenda(item, action, fArray, score);
                addToList(tempAgenda, tmp);
            }
            //*/

            List<TempNNAgenda> newAgendaList = bestAgendaList(tempAgenda);
            
            if(newAgendaList.isEmpty()){
                flag = false;
                continue;
            }
            
            int i=0;
            TempNNAgenda newAgenda = newAgendaList.get(i);
            ArcJAction action = newAgenda.getAction();
            double score = newAgenda.getScore();
            srscore = score;
            
            SRParser nItem = item;
            parserAct = action;
            nItem.applyAction(parserAct);
            srparser.actionMap.put(parserAct.getAction(), srparser.actionMap.get(parserAct.getAction())+1);
            if(debug >= 1)
                System.err.println("\t"+parserAct+"\t"+nacts.toString());
            if(srparser.input.isEmpty() && srparser.stack.size()==1)
                flag = false;
        }
    }
    
    private void addToList(List<TempNNAgenda> list, TempNNAgenda item){
        int i = 0;
        while(i < list.size()){
            if (item.getScore() > list.get(i).getScore())
                break;
            i++;
        }
        list.add(i, item);
    }
    
    private List<TempNNAgenda> bestAgendaList(List<TempNNAgenda> list){
        List<TempNNAgenda> nList = new ArrayList<>();
        for(int i = 0; i< beamSize && i<list.size(); i++){
            nList.add(list.get(i));
        }
        return nList;
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
    
    @Override
    public void train(){        
    }
    
    @Override
    public double parse(){
        return 0.0;
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
            srparser = new NonInc();
            
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
    
    public void writeModelFile(String modelFile) {
        try {
            double[][] W1 = classifier.getW1();
            double[] b1 = classifier.getb1();
            double[][] W2 = classifier.getW2();
            double[][] E = classifier.getE();
            
            Writer output = IOUtils.getPrintWriter(modelFile);            
            
            HashMap<String, ArrayList<CCGJRuleInfo>> uRules = srparser.treebankRules.getUnaryRules();
            HashMap<String, ArrayList<CCGJRuleInfo>> bRules = srparser.treebankRules.getBinaryRules();
            HashMap<String, ArrayList<CCGJRuleInfo>> rRules = srparser.treebankRules.getRevealRules();
            
            output.write("dict=" + knownWords.size() + "\n");
            output.write("pos=" + knownPos.size() + "\n");
            output.write("ccg cats=" + knownCCGCats.size() + "\n");
            output.write("embeddingSize=" + E[0].length + "\n");
            output.write("hiddenSize=" + b1.length + "\n");
            output.write("numTokens=" + (W1[0].length / E[0].length) + "\n");
            output.write("preComputed=" + preComputed.size() + "\n");
            output.write("classes=" + actsMap.size() + "\n");
            output.write("UnaryRules=" + uRules.size() + "\n");
            output.write("BinaryRules=" + bRules.size() + "\n");
            output.write("RevealRules=" + rRules.size() + "\n");
            int index = 0;
            
            // Classes
            for (ArcJAction act : actsList)
                output.write(act.toString()+"\n");
            
            // Unary and Binary Rules
            for(String key : uRules.keySet()){
                ArrayList<CCGJRuleInfo> list = uRules.get(key);
                output.write(key);
                for(CCGJRuleInfo info : list){
                    output.write("  "+info.toString());
                }
                output.write("\n");
            }
            for(String key : bRules.keySet()){
                ArrayList<CCGJRuleInfo> list = bRules.get(key);
                output.write(key);
                for(CCGJRuleInfo info : list){
                    output.write("  "+info.toString());
                }
                output.write("\n");
            }
            for(String key : rRules.keySet()){
                ArrayList<CCGJRuleInfo> list = rRules.get(key);
                output.write(key);
                for(CCGJRuleInfo info : list){
                    output.write("  "+info.toString());
                }
                output.write("\n");
            }
            
            // First write word / POS / label embeddings
            for (String word : knownWords) {
                output.write(word);
                for (int k = 0; k < E[index].length; ++k)
                    output.write(" " + E[index][k]);
                output.write("\n");
                index = index + 1;
            }
            for (String pos : knownPos) {
                output.write(pos);
                for (int k = 0; k < E[index].length; ++k)
                    output.write(" " + E[index][k]);
                output.write("\n");
                index = index + 1;
            }
            for (String label : knownCCGCats) {
                output.write(label);
                for (int k = 0; k < E[index].length; ++k)
                    output.write(" " + E[index][k]);
                output.write("\n");
                index = index + 1;
            }
            
            // Now write classifier weights
            for (int j = 0; j < W1[0].length; ++j)
                for (int i = 0; i < W1.length; ++i) {
                    output.write("" + W1[i][j]);
                    if (i == W1.length - 1)
                        output.write("\n");
                    else
                        output.write(" ");
                }
            for (int i = 0; i < b1.length; ++i) {
                output.write("" + b1[i]);
                if (i == b1.length - 1)
                    output.write("\n");
                else
                    output.write(" ");
            }
            for (int j = 0; j < W2[0].length; ++j)
                for (int i = 0; i < W2.length; ++i) {
                    output.write("" + W2[i][j]);
                    if (i == W2.length - 1)
                        output.write("\n");
                    else
                        output.write(" ");
                }
            
            // Finish with pre-computation info
            for (int i = 0; i < preComputed.size(); ++i) {
                output.write("" + preComputed.get(i));
                if ((i + 1) % 100 == 0 || i == preComputed.size() - 1)
                    output.write("\n");
                else
                    output.write(" ");
            }
            
            output.close();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
    
    private static final Map<String, Integer> numArgs = new HashMap<>();
    static {
        numArgs.put("textFile", 1);
        numArgs.put("outFile", 1);
    }
    
    public static void main(String[] args) throws IOException, Exception {
        
        String trainAutoFile, trainConllFile, trainPargFile, testAutoFile, testPargFile, testConllFile, outAutoFile, outPargFile, modelFile, embedFile;
        
        String home = "/home/ambati/ilcc/projects/parsing/experiments/english/ccg/";
        trainAutoFile = home+"data/final/train.gccg.auto";
        trainConllFile = home+"data/final/train.accg.conll2";
        trainPargFile = home+"data/final/train.gccg.parg";
        trainAutoFile = home+"data/final/devel.gccg.auto";
        trainConllFile = home+"data/final/devel.accg.conll";
        trainPargFile = home+"data/final/devel.gccg.parg";
        //testAutoFile = home+"data/final/devel.gccg.auto";
        testAutoFile = "";
        testPargFile = home+"data/final/devel.gccg.pargx";
        testConllFile = home+"data/final/devel.accg.conllx";
        outAutoFile = home+"models/out1.txt";
        outPargFile = home+"models/out2.txt";
        modelFile = home+"models/nnccg.model.txt.gz";
        embedFile = "/home/ambati/ilcc/tools/neural-networks/embeddings/turian/embeddings.raw";
        
        if(args.length == 0){
            args = new String[] {
                "-trainCoNLL", trainConllFile, "-trainAuto", trainAutoFile, "-trainParg", trainPargFile,
                "-testCoNLL", testConllFile, "-testAuto", testAutoFile, "-testParg", testPargFile,
                "-outParg", outPargFile, "-model", modelFile, "-beam", "1", "-embedFile", embedFile,
                "-maxIter", "1",
                //"-isTrain", "true", "-beam", 1, "-debug", "false", "-early", false
            };
        }
        
        Properties props = StringUtils.argsToProperties(args, numArgs);
        IncNNParserGreedy incnnpar = new IncNNParserGreedy(props);
        
        long start;
        
        System.err.println("Started Training: " + new Date(System.currentTimeMillis()) +"\n");
        start = (long) (System.currentTimeMillis());
        if(props.getProperty("trainCoNLL") != null)
            incnnpar.train(props.getProperty("trainCoNLL"), props.getProperty("testCoNLL"), props.getProperty("model"),
                    props.getProperty("embedFile"), props.getProperty("preModel"));
        System.err.println("Training Time: " + (System.currentTimeMillis() - start) / 1000.0 + " (s)");
        
        System.err.println("Loading Model: " + new Date(System.currentTimeMillis()) +"\n");
        incnnpar.loadModelFile(props.getProperty("model"));
        
        System.err.println("Started Parsing: " + new Date(System.currentTimeMillis()) +"\n");
        start = (long) (System.currentTimeMillis());
        incnnpar.test(props.getProperty("testCoNLL"), props.getProperty("testAuto"), props.getProperty("testParg"));
        System.err.println("Parsing Time: " + (System.currentTimeMillis() - start) / 1000.0 + " (s)");
    }
}
