/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.incderivation;

import edinburgh.ccg.deps.CCGcat;
import edu.stanford.nlp.util.PropertiesUtils;
import ilcc.ccgparser.learn.AvePerceptron;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGDepInfo;
import ilcc.ccgparser.utils.CCGJRuleInfo;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.DataTypes.GoldccgInfo;
import ilcc.ccgparser.utils.Feature;
import ilcc.ccgparser.utils.SCoNLLNode;
import ilcc.ccgparser.utils.Utils;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ccgCombinators;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author ambati
 */
public abstract class IncParser {
        
    protected boolean ftrue, revelf, uncov, isTrain, early_update, lookAhead, incalgo;
    protected int beamSize, sSize, iters, debug;    
    protected String trainAutoFile, trainCoNLLFile, trainPargFile, testAutoFile, testPargFile, testCoNLLFile, modelFile, outAutoFile, outPargFile;
    
    protected AvePerceptron model;
    protected SRParser srparser;
    protected Map<ArcJAction, Integer> actsMap;
    protected Map<SRAction, Integer> actionMap;    
    protected HashMap<Integer, GoldccgInfo> goldDetails;
    
    public List<CCGJTreeNode> fillData() throws Exception{
        if(trainCoNLLFile == null)
            return null;
        else
            return srparser.fillData(trainCoNLLFile, trainAutoFile, trainAutoFile, goldDetails);
    }
    
    public abstract void train() throws Exception;
    
    public abstract double parse() throws Exception;
    
    protected void init(Properties props) throws IOException{
        model = new AvePerceptron();
        sSize = 0;
        goldDetails = new HashMap<>();
        trainAutoFile = props.getProperty("trainAuto");
        trainCoNLLFile = props.getProperty("trainCoNLL");
        trainPargFile = props.getProperty("trainParg");
        testAutoFile = props.getProperty("testAuto");
        testPargFile = props.getProperty("testParg");
        testCoNLLFile = props.getProperty("testCoNLL");
        outAutoFile = props.getProperty("outAuto");
        outPargFile = props.getProperty("outParg");        
        modelFile = props.getProperty("model");
        
        incalgo = props.getProperty("algo").equals("RevInc");
        isTrain = PropertiesUtils.getBool(props, "isTrain", false);
        beamSize = PropertiesUtils.getInt(props, "beam", 1);
        iters = PropertiesUtils.getInt(props, "iters", 10);
        early_update = PropertiesUtils.getBool(props, "early", false);
        lookAhead = PropertiesUtils.getBool(props, "lookAhead", true);
        debug = PropertiesUtils.getInt(props, "debug", 0);
        
        srparser = (incalgo) ? new RevInc(): new NonInc();        
        srparser.incalgo = incalgo;
        Commons.setDebug(debug);
        Commons.setIncAlgo(incalgo);
        actsMap = new HashMap<>();
    }
    
    private Feature String2Feature(String str){
        return Feature.make(str);
    }
    
    public void loadModel() throws FileNotFoundException, IOException{
        System.err.println("Loading Model: \n");

        String s;
        BufferedReader input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(modelFile))), "UTF-8"));
        HashMap<Feature, HashMap<ArcJAction, AvePerceptron.Weight>> featVector = new HashMap<>();
                
        s = input.readLine();
        int classes = Integer.parseInt(s.substring(s.indexOf('=') + 1));
        s = input.readLine();
        int nuRules = Integer.parseInt(s.substring(s.indexOf('=') + 1));
        s = input.readLine();
        int nbRules = Integer.parseInt(s.substring(s.indexOf('=') + 1));
        s = input.readLine();
        int nrRules = Integer.parseInt(s.substring(s.indexOf('=') + 1));
        
        String[] splits;        
        for (int k = 0; k < classes; k++) {
            s = input.readLine().trim();
            splits = s.split("--");
            actsMap.put(ArcJAction.make(Utils.SRAction.valueOf(splits[0]), Integer.parseInt(splits[1]), splits[2], ccgCombinators.RuleType.valueOf(splits[3])), k);
        }
        
        for (int k = 0; k < nuRules; k++) {
            s = input.readLine().trim();
            splits = s.split("  ");
            String key = splits[0];
            for(int i = 1; i < splits.length; i++){
                String[] parts = splits[i].split("--");
                CCGJRuleInfo info = new CCGJRuleInfo(CCGcat.ccgCatFromString(parts[0]), null, CCGcat.ccgCatFromString(parts[2]), parts[3].equals("true"), ccgCombinators.RuleType.valueOf(parts[4]), Integer.parseInt(parts[5]), 0);
                srparser.treebankRules.addUnaryRuleInfo(info, key);
            }
        }
        
        for (int k = 0; k < nbRules; k++) {
            s = input.readLine().trim();
            splits = s.split("  ");
            String key = splits[0];
            for(int i = 1; i < splits.length; i++){
                String[] parts = splits[i].split("--");
                CCGJRuleInfo info = new CCGJRuleInfo(CCGcat.ccgCatFromString(parts[0]), CCGcat.ccgCatFromString(parts[1]), CCGcat.ccgCatFromString(parts[2]), parts[3].equals("true"), ccgCombinators.RuleType.valueOf(parts[4]), Integer.parseInt(parts[5]), 0);
                srparser.treebankRules.addBinaryRuleInfo(info, key);
            }
        }
        
        for (int k = 0; k < nrRules; k++) {
            s = input.readLine().trim();
            splits = s.split("  ");
            String key = splits[0];
            for(int i = 1; i < splits.length; i++){
                String[] parts = splits[i].split("--");
                CCGJRuleInfo info = new CCGJRuleInfo(CCGcat.ccgCatFromString(parts[0]), CCGcat.ccgCatFromString(parts[1]), CCGcat.ccgCatFromString(parts[2]), parts[3].equals("true"), ccgCombinators.RuleType.valueOf(parts[4]), Integer.parseInt(parts[5]), 0);
                srparser.treebankRules.addRevealRuleInfo(info, key);
            }
        }
        
        while ((s = input.readLine()) != null) {
            s = s.trim();
            String[] parts = s.split("\t");
            if(parts.length!=2)
                continue;
            Feature key = String2Feature(parts[0]);
            String[] items = parts[1].split(" ");
            HashMap<ArcJAction, AvePerceptron.Weight> map = new HashMap<>();
            for(int i = 0; i<items.length; i+=5){
                String[] actstr = items[i].split("--");
                ArcJAction act = ArcJAction.make(Utils.SRAction.valueOf(actstr[0]), Integer.parseInt(actstr[1]), actstr[2], ccgCombinators.RuleType.valueOf(actstr[3]));
                AvePerceptron.Weight wt = new AvePerceptron.Weight(Double.parseDouble(items[i+2]), Double.parseDouble(items[i+4]), -1, 1);
                map.put(act, wt);
            }
            featVector.put(key, map);
        }
        input.close();
        model.fv = new AvePerceptron.FeatureVector(featVector);
    }
    
    public void saveModel(String modelFile) throws UnsupportedEncodingException, IOException{
                
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(modelFile))),"UTF-8"));
        //BufferedWriter output = new BufferedWriter(new FileWriter(new File(modelFile)));
        HashMap<Feature, HashMap<ArcJAction, AvePerceptron.Weight>> fv = model.fv.getFeatureVector();
        SortedSet<Feature> keys = new TreeSet<>(fv.keySet());
        
        
        HashMap<String, ArrayList<CCGJRuleInfo>> uRules = srparser.treebankRules.getUnaryRules();
        HashMap<String, ArrayList<CCGJRuleInfo>> bRules = srparser.treebankRules.getBinaryRules();
        HashMap<String, ArrayList<CCGJRuleInfo>> rRules = srparser.treebankRules.getRevealRules();
        output.write("classes=" + actsMap.size() + "\n");
        output.write("UnaryRules=" + uRules.size() + "\n");
        output.write("BinaryRules=" + bRules.size() + "\n");
        output.write("RevealRules=" + rRules.size() + "\n");
        
        // Classes
        for (ArcJAction act : actsMap.keySet())
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

        for(Feature key : keys){
            StringBuilder sb = new StringBuilder();
            HashMap<ArcJAction, AvePerceptron.Weight> map = fv.get(key);
            SortedSet<ArcJAction> keys2 = new TreeSet<>(map.keySet());
            for(ArcJAction act : keys2){
                AvePerceptron.Weight wt = map.get(act);
                if(wt.rawWeight != 0 || wt.totalWeight != 0){
                    sb.append(act.toString()); sb.append(" : ");
                    sb.append(map.get(act).rawWeight); sb.append(" / "); sb.append(map.get(act).totalWeight);
                    sb.append(" ");
                }
            }
            if(!sb.toString().isEmpty()){
                output.write(key+"\t");
                output.write(sb.toString().trim());
                output.write("\n");
            }
        }
        output.close();
    }

    public HashMap<Integer, CCGJSentence> getccgDepMap(BufferedReader iReader) throws IOException{
        String line;
        int sentcount = 0;
        HashMap<Integer, CCGJSentence> ccgDeps = new HashMap<>();
        HashMap<String, CCGDepInfo> tmpmap = new HashMap<>();
        CCGJSentence tmpsent = new CCGJSentence();
        while ( (line = iReader.readLine()) != null) {
            if(line.startsWith("# ") || line.trim().isEmpty())
                continue;
            if(line.startsWith("<s id")){
                tmpmap = new HashMap<>();
                tmpsent = getSentInfo(line);
                sentcount++;
            }
            else if(line.startsWith("<\\s")){
                //ccgDeps.put(sentcount, tmpmap);
                tmpsent.setpargdeps(tmpmap);
                ccgDeps.put(sentcount, tmpsent);
            }
            else{
                String parts[] = line.trim().replaceAll("[ \t]+","\t").split("\t");
                int hid = Integer.parseInt(parts[1])+1;
                int aid = Integer.parseInt(parts[0])+1;
                String key = hid+"--"+aid;
                CCGDepInfo info = new CCGDepInfo(hid, aid, Integer.parseInt(parts[3]), parts[2], false, 0.0);
                tmpmap.put(key, info);
            }
        }
        return ccgDeps;
    }
    
    private CCGJSentence getSentInfo(String line){
        String[] parts = line.split("<c>|</c>")[1].split(" ");
        CCGJSentence tmpsent = new CCGJSentence();
        for(int i=0;i<parts.length;i++){
             String[] wpc = parts[i].split("\\|");
             CCGJTreeNode lnode = CCGJTreeNode.makeLeaf(CCGcat.lexCat(wpc[0], wpc[2], i+1), new SCoNLLNode(i+1, wpc[0], wpc[1], wpc[2]));
             tmpsent.addCCGJTreeNode(lnode);
        }
        return tmpsent;
    }    
}