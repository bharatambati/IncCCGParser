/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.incderivation;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import edinburgh.ccg.deps.CCGcat;
import edinburgh.ccg.deps.DepList;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGDepInfo;
import ilcc.ccgparser.utils.CCGDepList;
import ilcc.ccgparser.utils.CCGJRuleInfo;
import ilcc.ccgparser.utils.CCGJRules;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.CCGNodeDepInfo;
import ilcc.ccgparser.utils.SCoNLLNode;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import ilcc.ccgparser.utils.DataTypes.GoldccgInfo;
import ilcc.ccgparser.utils.DepGraph;
import ilcc.ccgparser.utils.DerivDepInfo;
import ilcc.ccgparser.utils.Utils;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ccgCombinators;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 *
 * @author ambati
 *
 */
public abstract class SRParser {
    
    public Stack<CCGJTreeNode> stack;
    public List<CCGJTreeNode> input;
    public CCGJRules treebankRules;
    CCGJRules goldSentRules;
    
    public HashMap<String, CCGDepInfo> goldccgDeps;
    public HashMap<String, CCGDepInfo> sysccgDeps;
    HashMap<Integer, CCGNodeDepInfo> ccgNodeDeps;
    HashMap<Integer, String> nodeWaitInfo;
    HashMap<String, DerivDepInfo> drvDeps;
    
    public Map<SRAction, Integer> actionMap;
    public int uGold, uSys, uCorr, lGold, lSys, lCorr, lcCorr, lcSys, lcGold, totCat, corrCat;
    public int wordCount, sentCount, nodeCount, waitTime, parsedSents, compressNodeCount, connectness;
    double aveWaitTime, aveConnectness;
    public boolean ftrue, uncov, incalgo;
    public double UF, LF, UAS, LAS;
    
    public DepGraph depGraph;
    //public DepTree depGraph;
    public CCGJSentence sent;
    //ccgCombinators combinators;
    
    public SRParser() throws IOException{        
        init();
        treebankRules = new CCGJRules();
    }
    
    public void init() throws IOException{
        
        stack = new Stack<>();
        input = new ArrayList<>();
        goldSentRules = new CCGJRules();
        
        goldccgDeps = new HashMap<>();
        sysccgDeps = new HashMap<>();
        ccgNodeDeps = new HashMap<>();
        nodeWaitInfo = new HashMap<>();
        drvDeps = new HashMap<>();
        
        uGold = uSys = uCorr = lGold = lSys = lCorr = lcCorr = lcSys = lcGold = totCat = corrCat= 0;
        wordCount = sentCount = nodeCount = waitTime = parsedSents = compressNodeCount = connectness= 0;
        aveWaitTime = aveConnectness =0.0;
        
        uncov = false;
        actionMap = new HashMap<>();        
        actionMap.put(SRAction.SHIFT, 1); actionMap.put(SRAction.RU, 1); 
        actionMap.put(SRAction.RL, 1); actionMap.put(SRAction.RR, 1); 
        actionMap.put(SRAction.RREVEAL, 1); actionMap.put(SRAction.LREVEAL, 1);
        sent = new CCGJSentence();
    }
    
    
    abstract public List<ArcJAction> parse(CCGJSentence sent) throws Exception;
    abstract public CCGJTreeNode shift() throws Exception;
    
    public void addRules(String unaryRuleFile, String binaryRuleFile) throws IOException{
        //rules.addRules(unaryRuleFile, binaryRuleFile);
    }
    
    public List<CCGJTreeNode> fillData(String conllFile, String derivFile, String depsFile, HashMap<Integer, GoldccgInfo> goldDetails) throws IOException, Exception {
        
        BufferedReader conllReader = new BufferedReader(new FileReader(conllFile));
        BufferedReader derivReader = new BufferedReader(new FileReader(derivFile));
        //BufferedReader depReader = new BufferedReader(new FileReader(depsFile));
        String dLine;
        ArrayList<String> cLines;
        
        while (derivReader.readLine() != null) {
            updateSentCount();
            //getCCGDepsParg(depReader);
            cLines = getConll(conllReader);
            sent = new CCGJSentence();
            dLine = getccgDeriv(derivReader);
            CCGJTreeNode root = parseDrivString(dLine, sent);
            sent.updateCoNLL(cLines);
            getDerivDeps(root);
            updateSysDepsGold(root);
            List<ArcJAction> actslist = parse(sent);
            goldDetails.put(sentCount, new GoldccgInfo(actslist, goldccgDeps, sent));
            //evaluateParseDependenciesJulia();
            updateNodeCount();
            resetVars();
        }
        //printResults();
        List<CCGJTreeNode> trees = parseSents(goldDetails);
        printResults();
        return trees;
        //treebankRules.printRules();
        //printUnresolveMap2(nodeCount); //printCcgRules();
    }
    
    public List<CCGJTreeNode> parseSents(HashMap<Integer, GoldccgInfo> goldDetails) throws IOException{
        ftrue = true;
        List<CCGJTreeNode> trees = new ArrayList<>();
        for(int i = 0; i < goldDetails.size(); i++ ){
            sentCount = i+1;
            GoldccgInfo gccginfo = goldDetails.get(sentCount);
            List<ArcJAction> arcActList = gccginfo.getarcActs();
            sent = gccginfo.getccgSent();
            goldccgDeps = copyHashMap(gccginfo.getccgDeps());
            input = sent.getNodes();
            
            if(arcActList != null){
                //System.err.println((i+1)+"  "+gccginfo.getarcActs());
                getDerivDeps(sent.getDerivRoot());
                ccgNodeDeps = updateDepNodeDeps(goldccgDeps);
                depGraph = new DepGraph(sent.getLength());
                try{
                    boolean success = applyActions(arcActList);
                    if(success && stack.size()==1){                        
                        uncov=false;
                        evaluateParseDependenciesJulia();
                        trees.add(stack.get(0));
                    }
                    else{
                        goldDetails.put(sentCount, new GoldccgInfo(null, goldccgDeps, sent));
                        uncov=true;
                        trees.add(null);
                        //System.err.println("Error re-parsing "+sentCount);
                    }
                }
                catch(Exception ex){
                    goldDetails.put(sentCount, new GoldccgInfo(null, goldccgDeps, sent));
                    trees.add(null);
                    System.err.println("Error re-parsing "+sentCount);
                }
                resetVars();
            }
            else
                trees.add(null);
        }
        return trees;
    }
    
    public void initVars(CCGJSentence nsent) throws IOException{
        resetVars();
        sent = nsent;
        input = sent.getNodes();
        depGraph = new DepGraph(sent.getLength());
    }
    
    private boolean applyActions(List<ArcJAction> arcActList){
        for(int i = 0; i < arcActList.size(); i++){
            ArcJAction act = arcActList.get(i);
            if(applyAction(act) == null)
                return false;
        }
        return true;
    }
    
    public CCGJTreeNode applyAction(ArcJAction act){
        if(act.getAction() == SRAction.SHIFT)
            return applyShift(act);
        else if(act.getAction() == SRAction.RL || act.getAction() == SRAction.RR || act.getAction() == SRAction.RU)
            return applyReduce(act);
        else if(act.getAction() == SRAction.LREVEAL)
            return applyLReveal(act);
        else if(act.getAction() == SRAction.RREVEAL)
            return applyRReveal(act);
        return null;
    }
    
    private CCGJTreeNode applyShift(ArcJAction act){
        CCGJTreeNode inode = input.get(0);
        CCGJTreeNode result = Commons.applyShift(inode, act);
        connectness += stack.size();
        depGraph.addVertex(result.getNodeId(), result.getHeadcat());
        stack.push(result);
        input.remove(0);
        calculateWaitTime(result.getConllNode().getNodeId()-1, ccgNodeDeps);
        return result;
    }
    
    private CCGJTreeNode applyReduce(ArcJAction act) {
        
        CCGJTreeNode left = null, right, result;
        String rescatstr = act.getccgCat().toString();
        
        if(act.getAction() == SRAction.RU){
            right = stack.pop();
            result = Commons.applyUnary(right, rescatstr, act, sysccgDeps);
        }
        else {
            right = stack.pop();
            left = stack.pop();
            
            SRAction sract = act.getAction();
            boolean isHeadLeft = "left".equals((sract == SRAction.RR) ? "left" : "right");
            CCGcat lcat = left.getCCGcat();
            CCGcat rcat = right.getCCGcat();
            RuleType ruletype = act.getRuleType();
            
            CCGcat rescat = CCGcat.combine(lcat, rcat, rescatstr);
            if(rescat == null || !rescat.toString().equals(rescatstr))
                rescat = ccgCombinators.checkRule(lcat, rcat, ruletype);
            if(rescat == null){
                rescat = CCGcat.typeChangingRule(isHeadLeft ? lcat : rcat, rescatstr);
                rescat.catString = rescatstr;
            }
            HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
            Commons.getDepsMap(lcat, rcat, rescat, depsMap);
            if(incalgo)
                Commons.applyupdateDepTree(left, right, act.getAction(), depsMap, sysccgDeps, depGraph);
            Commons.updateSysDeps(depsMap, sysccgDeps);
            result = Commons.applyBinaryUpdate(left, right, rescat, act, ruletype, isHeadLeft);
            
            int lid = left.getConllNode().getNodeId(), rid = right.getConllNode().getNodeId();
            updateccgNodeDep(lid, rid, sract, ccgNodeDeps, depsMap);
            updateccgNodeDep(rid, lid, sract, ccgNodeDeps, depsMap);
        }
        
        stack.push(result);
        return result;
    }
    
    protected CCGJTreeNode applyLReveal(ArcJAction act) {
        CCGJTreeNode result;
        
        CCGJTreeNode right = stack.pop(), left = stack.pop();
        result = Commons.applyLReveal(left, right, act, sent.getNodes(), sysccgDeps, depGraph, true, ccgNodeDeps);
        stack.push(result);
        return result;
    }
    
    protected CCGJTreeNode applyRReveal(ArcJAction act) {
        CCGJTreeNode result;        
        CCGJTreeNode right = stack.pop(), left = stack.pop();
        result = Commons.applyRReveal(left, right, act, sent.getNodes(), sysccgDeps, depGraph, true, ccgNodeDeps);
        if(result == null)
            result = Commons.applyBinaryUpdate(left, right, CCGcat.ccgCatFromString(act.getccgCat().toString()), act, RuleType.rreveal, true);
        stack.push(result);
        return result;
    }
    
    public void postOrder(CCGJTreeNode root, ArrayList list){
        if(root == null)
            return;
        postOrder(root.getLeftChild(), list);
        postOrder(root.getRightChild(), list);
        list.add(root);
    }

    protected CCGJTreeNode applyBinary(CCGJTreeNode left, CCGJTreeNode right, String rescatstr, boolean isHeadLeft, SRAction sract, ArcJAction act){
        
        RuleType rtype = RuleType.other;
        CCGcat lcat = left.getCCGcat();
        CCGcat rcat = right.getCCGcat();
        CCGcat rescat = CCGcat.combine(lcat, rcat, rescatstr);
        if(rescat == null)
            rescat = CCGcat.ccgCatFromString(rescatstr);
        
        CCGJTreeNode result = null;
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        Commons.getDepsMap(lcat, rcat, rescat, depsMap);
        if(ftrue || checkWithGoldDeps(depsMap)){
            Commons.updateSysDeps(depsMap, sysccgDeps);
            result = Commons.applyBinaryUpdate(left, right, rescat, act, rtype, isHeadLeft);
            Commons.applyupdateDepTree(left, right, sract, depsMap, sysccgDeps, depGraph);
            //int lid = left.getConllNode().getNodeId(), rid = right.getConllNode().getNodeId();
            //updateccgNodeDep(lid, rid, sract, ccgNodeDeps, depsMap);
            //updateccgNodeDep(rid, lid, sract, ccgNodeDeps, depsMap);
        }
        return result;
    }
    
    protected void updateccgNodeDeps(CCGJTreeNode left, CCGJTreeNode right, SRAction act, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps, HashMap<String, CCGDepInfo> depsMap, boolean isconj){
        int lid = left.getConllNode().getNodeId(), rid = right.getConllNode().getNodeId();
        if(!isconj)
            updateDepTree(left, right, act, depsMap);
        updateccgNodeDep(lid, rid, act, nccgNodeDeps, depsMap);
        updateccgNodeDep(rid, lid, act, nccgNodeDeps, depsMap);
        updateccgNodeDep(lid, rid, act, ccgNodeDeps, depsMap);
        updateccgNodeDep(rid, lid, act, ccgNodeDeps, depsMap);
    }
    
    protected void updateccgNodeDep(int lid, int rid, SRAction act, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps, HashMap<String, CCGDepInfo> depsMap){
        
        CCGNodeDepInfo nodeDepinfo;
        
        if(( nodeDepinfo = nccgNodeDeps.get(lid)) != null){
            nodeDepinfo.remove(lid, rid);
            //if(nodeDepinfo.size() == 0)
            if(nodeDepinfo.ldepsize() == 0)
                nccgNodeDeps.remove(lid);
        }
        
        if(( nodeDepinfo = nccgNodeDeps.get(rid)) != null){
            nodeDepinfo.remove(rid, lid);
            //if(nodeDepinfo.size() == 0)
            if(nodeDepinfo.ldepsize() == 0)
                nccgNodeDeps.remove(rid);
        }
        
        if(depsMap == null) return;
        for(String key : depsMap.keySet()){
            String[] parts = key.split("--");
            int lida, rida, id;
            lida = Integer.parseInt(parts[0]); rida = Integer.parseInt(parts[1]);
            if(rida < lida){
                id = lida; lida = rida; rida = id;
            }
            
            if(( nodeDepinfo = nccgNodeDeps.get(lida)) != null){
                nodeDepinfo.remove(lida, rida);
                if(nodeDepinfo.ldepsize() == 0)
                    //if(nodeDepinfo.size() == 0)
                    nccgNodeDeps.remove(lida);
            }
            
            if(( nodeDepinfo = nccgNodeDeps.get(rida)) != null){
                nodeDepinfo.remove(rida, lida);
                if(nodeDepinfo.ldepsize() == 0)
                    //if(nodeDepinfo.size() == 0)
                    nccgNodeDeps.remove(rida);
            }
        }
        //*/
    }
    
    protected void updateDepTree(CCGJTreeNode left, CCGJTreeNode right, SRAction act, HashMap<String, CCGDepInfo> depsMap){
        int pid, cid, lid, rid;
        CCGCategory phcat, chcat;
        
        rid = right.getConllNode().getNodeId();
        lid = left.getConllNode().getNodeId();
        
        if (act == SRAction.RL) {
            pid = rid;
            cid = lid;
        }
        else {
            pid = lid;
            cid = rid;
        }
        
        //phcat = sent.getNode(pid-1).getConllNode().getccgCat();
        //chcat = sent.getNode(cid-1).getConllNode().getccgCat();
        
        //depGraph.addEdge(pid, phcat, cid, chcat);
        /*
        if(act == SRAction.RL || act == SRAction.RR) {
        if(act == SRAction.RL){
        pid = right.getConllNode().getNodeId();
        cid = left.getConllNode().getNodeId();
        }
        else{
        pid = left.getConllNode().getNodeId();
        cid = right.getConllNode().getNodeId();
        }
        String key = pid+"--"+cid;
        if(ftrue || drvDeps.containsKey(key)){
        phcat = sent.getNode(pid-1).getConllNode().getSuperTag();
        chcat = sent.getNode(cid-1).getConllNode().getSuperTag();
        depGraph.addEdge(pid, phcat, cid, chcat);
        }
        }
        */
        ///*
        for(String key : depsMap.keySet()){
            if(sysccgDeps.containsKey(key))
                continue;
            CCGDepInfo cdepinfo = depsMap.get(key);
            String key1 = cdepinfo.getHeadId()+"--"+cdepinfo.getArgId(), key2 = cdepinfo.getArgId()+"--"+cdepinfo.getHeadId();
            DerivDepInfo ddepinfo = drvDeps.get(key1);
            if(ddepinfo == null) ddepinfo = drvDeps.get(key2);
            if(ddepinfo != null){
                pid = ddepinfo.isHeadLeft() ? ddepinfo.getLeftId() : ddepinfo.getRightId();
                cid = ddepinfo.isHeadLeft() ? ddepinfo.getRightId() : ddepinfo.getLeftId();
            }
            else{
                pid = cdepinfo.getHeadId();
                cid = cdepinfo.getArgId();
            }
            
            //phcat = sent.getNode(pid-1).getConllNode().getccgCat();
            //chcat = sent.getNode(cid-1).getConllNode().getccgCat();
            
            //depGraph.addEdge(pid, phcat, cid, chcat);
            depGraph.addEdge(pid, cid);
        }
        //*/
    }
    
    
    protected boolean checkWithGoldDeps(HashMap<String, CCGDepInfo> depsMap){
        for(String key : depsMap.keySet()){
            CCGDepInfo sdinfo = depsMap.get(key);
            if(!goldccgDeps.containsKey(key))
                return false;
            else{
                CCGDepInfo gdinfo = goldccgDeps.get(key);
                return sdinfo.getCat().equals(gdinfo.getCat()) && sdinfo.getSlot()==gdinfo.getSlot();
            }
        }
        return true;
    }
    
    
    protected void updateSysDeps(DepList dep){
        while(dep!=null){
            String key = dep.headIndex+"--"+dep.argIndex;
            if(!sysccgDeps.containsKey(key)){
                CCGDepInfo dinfo = new CCGDepInfo(dep.headIndex, dep.argIndex, dep.argPos, dep.headCat, dep.extracted, 0.0);
                sysccgDeps.put(key, dinfo);
            }
            dep = dep.next();
        }
    }
    
    protected void updateSysDeps(CCGDepList dep){
        while(dep!=null){
            String key = dep.getHeadId()+"--"+dep.getArgId();
            if(!sysccgDeps.containsKey(key)){
                CCGDepInfo dinfo = new CCGDepInfo(dep.getHeadId(), dep.getArgId(), dep.getSlot(), dep.getCat(), dep.getExtract(), 0.0);
                sysccgDeps.put(key, dinfo);
            }
            dep = dep.getNext();
        }
    }
    
    protected void updateHeadDirection(CCGJTreeNode left, CCGJTreeNode right, CCGJRuleInfo info, String key){
        if(info == null) return;
        
        RuleType rule = info.getCombinator();
        boolean headIsLeft = info.getHeadDir();
        
        if(drvDeps.containsKey(key))
            headIsLeft = drvDeps.get(key).isHeadLeft();
        //else
        //    headIsLeft = rule != RuleType.ba && rule != RuleType.bc && rule != RuleType.gbc;
        
        info.setHeadDir(headIsLeft);
    }
    
    protected boolean canCompose(CCGcat lcat, CCGcat rcat){
        if(lcat.catString().equals("N"))
            lcat = CCGcat.typeChangingRule(lcat, "NP");
        return ccgCombinators.checkCCGRules(lcat, rcat) !=null;
    }
    
    protected CCGJRuleInfo checkGoldPunct(CCGJTreeNode left, CCGJTreeNode right){
        
        CCGcat lcat = left.getCCGcat();
        CCGcat rcat = right.getCCGcat();
        RuleType rule = null; boolean dir = true;
        int pnctid = -1, oid = -1;
        CCGJRuleInfo info = null;
        
        if(Utils.isPunct(lcat)) {
            pnctid = left.getConllNode().getNodeId();
            oid = right.getConllNode().getNodeId();
            rule = RuleType.lp; dir = false;
        }
        else if(Utils.isPunct(rcat)) {
            pnctid = right.getConllNode().getNodeId();
            oid = left.getConllNode().getNodeId();
            rule = RuleType.rp; dir = true;
        }
        
        if(nodeWaitInfo.containsKey(pnctid) && nodeWaitInfo.get(pnctid).equals(oid+"::"+rule)) {
            CCGcat rescat = ccgCombinators.checkRule(left.getCCGcat(), right.getCCGcat(), rule);
            info = new CCGJRuleInfo(lcat, rcat, rescat, dir, rule, 0, 0);
            nodeWaitInfo.remove(pnctid);
        }
        
        return info;
    }
    
    private void updateSentCount(){
        sentCount++;
        if(sentCount%1000 == 0)
            System.err.println(sentCount);
        else if(sentCount%100 == 0)
            System.err.print(sentCount+" ");
        //else
        //    System.err.print(sentCount+" ");
    }
    
    public ArrayList<String> getConll(BufferedReader conllReader) throws IOException{
        String cLine;
        ArrayList<String> cLines = new ArrayList<>();
        while (!(cLine = conllReader.readLine()).equals("")) {
            cLines.add(cLine);
        }
        return cLines;
    }
    
    public String getccgDeriv(BufferedReader derivReader) throws FileNotFoundException, IOException{
        String dLine;
        while (!(dLine = derivReader.readLine()).startsWith("ID=")){
            break;
        }
        return dLine;
    }
    
    private void updateNodeCount(){
        nodeCount += stack.size();
        int sentLength = sent.getLength();
        wordCount += sentLength;
    }
    
    public void updateSysDepsGold(CCGJTreeNode root){
        //sysccgDeps = root.getDeps();
        goldccgDeps = copyHashMap(root.getDeps());
        //ccgNodeDeps = updateCCGNodeDeps(goldccgDeps);
        ccgNodeDeps = updateDepNodeDeps(goldccgDeps);
        depGraph = new DepGraph(sent.getLength());
    }
    
    
    protected void updateNodeWaitInfo(CCGJTreeNode curnode, RuleType rtype){
        
        int pnctId, oid;
        if(rtype.equals(RuleType.lp)){
            pnctId = curnode.getLeftChild().getConllNode().getNodeId();
            oid = curnode.getRightChild().getConllNode().getNodeId();
        }
        else{
            pnctId = curnode.getRightChild().getConllNode().getNodeId();
            oid = curnode.getLeftChild().getConllNode().getNodeId();
        }
        nodeWaitInfo.put(pnctId, oid+"::"+rtype.toString());
    }
    
    
    private HashMap<String, CCGDepInfo> copyHashMap(HashMap<String, CCGDepInfo> map){
        HashMap<String, CCGDepInfo> nmap = new HashMap<>();
        for(String key : map.keySet())
            nmap.put(key, map.get(key).copy());
        return nmap;
    }
    
    private void resetVars() throws IOException{
        stack.clear();
        input.clear();
        ccgNodeDeps.clear();
        goldSentRules = new CCGJRules();
        nodeWaitInfo = new HashMap<>();
        uncov = false;
        goldccgDeps = new HashMap<>();
        sysccgDeps = new HashMap<>();
    }
    
    public void writeDeps(BufferedWriter opWriter) throws IOException{
        for(String key : sysccgDeps.keySet()){
            StringBuilder depstr = new StringBuilder("");
            CCGDepInfo sdinfo = sysccgDeps.get(key);
            int hid = sdinfo.getHeadId();
            int aid = sdinfo.getArgId();
            depstr.append(sent.getNode(hid-1).getWrdStr()); depstr.append("_");depstr.append(hid);
            depstr.append(" (");depstr.append(sdinfo.getCat());depstr.append(") ");
            depstr.append(sdinfo.getSlot());depstr.append(" ");
            depstr.append(sent.getNode(aid-1).getWrdStr()); depstr.append("_");depstr.append(aid);
            depstr.append(" 0");
            opWriter.write(depstr.toString()+"\n");
        }
        
        StringBuilder sb = new StringBuilder("<c> ");
        for(CCGJTreeNode node : sent.getNodes()){
            CCGCategory acat = depGraph.getVertex(node.getNodeId());
            sb.append(node.getWrdStr());sb.append("|");
            sb.append(node.getPOS());sb.append("|");
            sb.append(acat);sb.append(" ");
        }
        opWriter.write(sb.toString().trim()+"\n\n");
        opWriter.flush();
    }
    
    public void writeDeriv(int id, BufferedWriter odWriter) throws IOException{
        CCGJTreeNode left, right, result;
        while (stack.size() != 1) {
            right = stack.pop();
            left = stack.pop();

            CCGcat rescat = CCGcat.ccgCatFromString("X");
            RuleType ruletype = RuleType.frag;
            ArcJAction act = ArcJAction.make(SRAction.REDUCE, 0, rescat.toString(), ruletype);
            result = Commons.applyBinaryUpdate(left, right, rescat, act, ruletype, true);
            stack.push(result);
        }
        CCGJTreeNode root = stack.peek();
        ArrayList<CCGJTreeNode> list = new ArrayList<>();
        postOrder(root, list);
        Stack<String> sStack = new Stack<>();
        for(CCGJTreeNode node : list) {
            if(node.isLeaf()){
                StringBuilder sb = new StringBuilder();
                String wrd, pos, cat;
                wrd = node.getWrdStr();
                pos = node.getPOS().toString();
                cat = node.getCCGcat().toString();
                sb.append("(<L ");
                sb.append(cat);sb.append(" ");
                sb.append(pos);sb.append(" ");
                sb.append(pos);sb.append(" ");
                sb.append(wrd);sb.append(" ");
                sb.append(cat);sb.append(">)");
                sStack.push(sb.toString());
            }
            else if(node.getChildCount()==1){
                
                StringBuilder sb = new StringBuilder();
                String cat;
                cat = node.getCCGcat().toString();
                sb.append("(<T ");
                sb.append(" ");
                sb.append(cat);
                sb.append(" lex ");
                sb.append(" 0 1> ");
                sb.append(sStack.pop());
                sb.append(" )");
                sStack.push(sb.toString());
            }
            else {
                String rstr = sStack.pop();
                String lstr = sStack.pop();
                StringBuilder sb = new StringBuilder();
                String cat = node.getCCGcat().toString();
                String dir = (node.getHeadDir()==1)? "0" : "1";
                sb.append("(<T ");                
                sb.append(cat);
                sb.append(" ");
                sb.append(node.getArcAction().getRuleType());
                if(node.getArcAction().getRuleType().equals(RuleType.rreveal))
                    sb.append("-"+node.getArcAction().getLevel());
                sb.append(" ");
                sb.append(dir);
                sb.append(" 2> ");
                sb.append(lstr);
                sb.append(" ");
                sb.append(rstr);
                sb.append(" )");
                sStack.push(sb.toString());
            }
        }        
        odWriter.write("ID="+id+"\n");
        odWriter.write(sStack.pop().trim()+"\n");
        odWriter.flush();
    }
    
    public void printResults(){
        DecimalFormat df = new DecimalFormat(".00");
        System.err.println();
        //System.err.println("Total # Sentences, Words: "+sentCount+" , "+wordCount);
        //System.err.println("Total # Nodes: "+nodeCount);
        //System.err.println("Ave. nodes per stack: "+df.format(1.0*nodeCount/sentCount));
        System.err.println("Coverage: ( "+parsedSents+" / "+sentCount+" ) "+df.format(100.00*parsedSents/sentCount));
        System.err.println("goldccgDeps, sysDeps, corrDeps : "+uGold+" "+uSys+" "+uCorr);
        double UP = 100.00*uCorr/uSys;
        double UR = 100.00*uCorr/uGold;
        UF = (2.00*UP*UR)/(UP+UR);
        
        double LP = 100.00*lCorr/uSys;
        double LR = 100.00*lCorr/uGold;
        LF = (2.00*LP*LR)/(LP+LR);
        
        double cLP = 100.00*lcCorr/lcSys;
        double cLR = 100.00*lcCorr/lcGold;
        double cLF = (2.00*cLP*cLR)/(cLP+cLR);
        
        System.err.println(" Unlabelled Prec : "+df.format(UP)+" Rec : "+df.format(UR)+" F-score : "+df.format(UF));
        System.err.println(" Labelled Prec : "+df.format(LP)+"  Rec : "+df.format(LR)+" F-score : "+df.format(LF));
        System.err.println(" Category Accuracy : "+df.format(corrCat)+"/"+df.format(totCat)+" = "+df.format(100.00*corrCat/totCat));
        //System.err.println("cLabelled Prec : "+df.format(cLP)+"  Rec : "+df.format(cLR)+" F-score : "+df.format(cLF));
        System.err.println("Average Waiting Time : "+df.format(1.0*aveWaitTime/parsedSents));
        System.err.println("Average Connectedness : "+df.format(1.0*aveConnectness/parsedSents));
        int valCount = 0;
        //System.err.println("\n"+actionMap);
        for (int i : actionMap.values()) valCount += i;
        for(SRAction act : actionMap.keySet())
            System.err.print(act+"="+df.format(100.00*actionMap.get(act)/valCount)+"  ");        
        System.err.println();
        
        actionMap.clear();
    }
    
    public void evaluateParseDependenciesJulia(){
        int sGoldDeps, sSysDeps, sCorrDeps = 0, lsCorrDeps = 0;
        sGoldDeps = goldccgDeps.size();
        sSysDeps = sysccgDeps.size();
        //System.err.println(sentCount+"\t"+goldccgDeps+"\n"+sysccgDeps);
        for(String key : sysccgDeps.keySet()){
            CCGDepInfo sdinfo = sysccgDeps.get(key);
            String[] split = key.split("--");
            String key2 = split[1]+"--"+split[0];
            if(goldccgDeps.containsKey(key)){
                //System.err.println(sentCount+" : Deps in System out: "+sdinfo.ccgDepStr());
                sCorrDeps++;
                CCGDepInfo gdinfo = goldccgDeps.get(key);
                if(sdinfo.getCat().equals(gdinfo.getCat()) && sdinfo.getSlot()==gdinfo.getSlot()){
                    lsCorrDeps++;
                    //goldccgDeps.remove(key);
                }
            }
            //else if(goldccgDeps.containsKey(key2))
            //    sCorrDeps++;
            //else
            //    System.err.println(sentCount+" : Extra Deps in System out: "+key+" "+rel);
        }
        
        uGold += sGoldDeps;
        uSys += sSysDeps;
        uCorr += sCorrDeps;
        lCorr += lsCorrDeps;
            
        if(stack.size() == 1){
            parsedSents++;
            aveConnectness += (1.0*connectness/sent.getLength());
            aveWaitTime += (1.0*waitTime/sent.getLength());
        }
        
        waitTime = 0;
        connectness = 0;
        //goldccgDeps.clear();
        sysccgDeps.clear();
    }
    
    public void updateCatAccuray(CCGJSentence gsent){
        for(CCGJTreeNode node : gsent.getNodes()){
            CCGCategory acat = depGraph.getVertex(node.getNodeId());
            CCGCategory gcat = node.getConllNode().getccgCat();
            if(gcat.equals(acat))
                corrCat++;
        }
        //totCat += gsent.getNodes().size();
    }
    
    private HashMap<Integer, CCGNodeDepInfo> updateDepNodeDeps(HashMap<String, CCGDepInfo> gccgdeps){
        HashMap<Integer, CCGNodeDepInfo> nodeDepMap = new HashMap<>();
        for(String key : drvDeps.keySet()){
            CCGDepInfo info = gccgdeps.get(key);
            if(info == null){
                String[] parts = key.split("--");
                info = gccgdeps.get(parts[1]+"--"+parts[0]);
                if(info==null){
                    //continue;
                    ///*
                    DerivDepInfo ddepinfo = drvDeps.get(key);
                    int pid = ddepinfo.getLeftId(), cid = ddepinfo.getRightId();
                    if(!ddepinfo.isHeadLeft()) { cid = ddepinfo.getLeftId(); pid = ddepinfo.getRightId();}
                    info = new CCGDepInfo(pid, cid, 0, sent.getNode(pid-1).getConllNode().getccgCat().toString(), false, 0.0);
                    //*/
                }
            }
            updateNodeDepMap(info.getHeadId(), info, nodeDepMap);
            updateNodeDepMap(info.getArgId(), info, nodeDepMap);
        }
        return nodeDepMap;
    }
    
    private void getDerivDeps(CCGJTreeNode root){
        List<CCGJTreeNode> list  = new ArrayList<>();
        list.add(root);
        drvDeps.clear();
        CCGJTreeNode parNode, left, right;
        SCoNLLNode cleft, cright;
        while(!list.isEmpty()){
            parNode = list.get(0);
            if(parNode.getChildCount()==1){
                list.add(parNode.getLeftChild());
            }
            else if(parNode.getChildCount()==2){
                int id = parNode.getHeadDir();
                left = parNode.getLeftChild(); right= parNode.getRightChild();
                cleft = left.getConllNode(); cright = right.getConllNode();
                String drvKey = cleft.getNodeId()+"--"+cright.getNodeId();
                boolean isheadleft = (id==0);
                DerivDepInfo ddepinfo = new DerivDepInfo(cleft.getNodeId(), cright.getNodeId(), isheadleft, left.getCCGcat().toString(), right.getCCGcat().toString());
                drvDeps.put(drvKey, ddepinfo);
                list.add(left);
                list.add(right);
            }
            list.remove(0);
        }
    }
    
    public CCGJTreeNode parseDrivString(String treeString, CCGJSentence sent) {
        
        sent.setCcgDeriv(treeString);
        Stack<CCGJTreeNode> nodes = new Stack<>();
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
                    CCGcat rescat = CCGcat.lexCat(cnode.getWrd().toString(), cnode.getccgCat().toString(), cnode.getNodeId());
                    if(rescat.isAux())
                        rescat.handleAux();
                    CCGJTreeNode node = CCGJTreeNode.makeLeaf(rescat, cnode);
                    sent.addCCGJTreeNode(node);
                    nodes.add(node);
                    id++;
                }
                else if (nodeString.charAt(1) == 'T') {
                    // (<T S/S 0 2> (<L (S/S)/NP IN IN In (S_113/S_113)/NP_114>)
                    ArrayList<String> items = Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> ")).trimResults().omitEmptyStrings().split(nodeString));
                    CCGJTreeNode node;
                    CCGJRuleInfo info;
                    int childrenSize = Integer.parseInt(items.get(3));
                    int headDir = Integer.parseInt(items.get(2));
                    String key;
                    CCGJTreeNode left, right;
                    CCGcat lcat, rcat, rescat;
                    RuleType rtype;
                    int size = nodes.size();
                    String rescatstr = items.get(1);
                    if(childrenSize == 2){
                        left = nodes.get(size-2);
                        right = nodes.get(size-1);
                        lcat = left.getCCGcat();
                        rcat = right.getCCGcat();
                        
                        boolean headIsLeft = (headDir==0);
                        if(lcat.isAux()) headIsLeft = false;
                        
                        rtype = findCombinator(lcat, rcat, rescatstr);
                        rescat = CCGcat.combine(lcat, rcat, rescatstr);
                        
                        key = lcat.toString()+" "+rcat.toString()+"--"+left.getConllNode().getNodeId()+" "+right.getConllNode().getNodeId();
                        
                        info = new CCGJRuleInfo(lcat, rcat, rescat, headIsLeft, rtype, 0, 0);
                        goldSentRules.updateBinaryRuleInfo(info, key);
                        treebankRules.addBinaryRuleInfo(info, lcat.toString()+" "+rcat.toString());
                        node = CCGJTreeNode.makeBinary(rescat, null, rtype, headIsLeft, left, right);
                        left.setParent(node);
                        right.setParent(node);
                    }
                    else {
                        left = nodes.get(size-1);
                        lcat = left.getCCGcat();
                        
                        rescat = CCGcat.typeRaiseTo(lcat, rescatstr);
                        if (rescat == null) {
                            rescat = CCGcat.typeChangingRule(lcat, rescatstr);
                            rtype = RuleType.lex;
                        }
                        else
                            rtype = RuleType.tr;
                        
                        key = lcat.toString()+"--"+left.getConllNode().getNodeId();
                        info = new CCGJRuleInfo(lcat, null, rescat, true, rtype, 0, -1);
                        goldSentRules.updateUnaryRuleInfo(info, key);
                        treebankRules.addUnaryRuleInfo(info, lcat.toString());
                        node = CCGJTreeNode.makeUnary(rescat, null, rtype, left);
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
        CCGJTreeNode root = nodes.pop();
        sent.setccgDerivTree(root);
        return root;
    }
    
    protected RuleType findCombinator(CCGcat lcat, CCGcat rcat, String rescatstr){
        RuleType rtype = ccgCombinators.findCombinator(lcat, rcat, rescatstr);
        if(rtype.equals(RuleType.conj) && Utils.isPunct(lcat))
            rtype = RuleType.other;
        return rtype;
    }
    
    protected void calculateWaitTime(int id, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps){
        for(int i = 0; i<=id; i++)
            if(nccgNodeDeps.containsKey(i))
                waitTime += nccgNodeDeps.get(i).ldepsize();
    }
    
    private void updateNodeDepMap(int id, CCGDepInfo info, HashMap<Integer, CCGNodeDepInfo> nodeDepMap){
        CCGNodeDepInfo ndinfo;
        if((ndinfo = nodeDepMap.get(id)) == null)
            ndinfo = new CCGNodeDepInfo();
        //if(info.getExtract() && info.getSlot()==1 && ndinfo.getLeftArg().size() > 0 && (info.getHeadId()==id && info.getArgId()<info.getHeadId()))
        //        return;
        ndinfo.addtoDepInfo(id, info);
        nodeDepMap.put(id, ndinfo);
    }
    
    /*
    // Methods below are not used. Check before deleting
    
    private void getCCGDepsParg(BufferedReader depReader) throws IOException{
        goldccgDeps.clear();
        String dLine;
        while ((dLine = depReader.readLine()) != null) {
            if(dLine.startsWith("<s"))
                continue;
            if(dLine.startsWith("<\\s"))
                break;
            else{
                dLine = dLine.replace("\t", " ");
                dLine = dLine.replaceAll("  +", " ");
                String[] parts = dLine.split(" ");
                
                //goldccgDeps.put((Integer.parseInt(parts[1])+1)+"--"+(Integer.parseInt(parts[0])+1), parts[2]+"--"+parts[3]);
                int argId = Integer.parseInt(parts[0])+1;
                int nodeId = Integer.parseInt(parts[1])+1;
                String key, lkey, hid;
                if(nodeId > argId){
                    key = String.valueOf(nodeId);
                    lkey = String.valueOf(argId);
                    hid = "1";
                }
                else{
                    key = String.valueOf(argId);
                    lkey = String.valueOf(nodeId);
                    hid = "0";
                }
                CCGDepInfo dinfo = new CCGDepInfo(nodeId, argId, Integer.parseInt(parts[3]), parts[2], false, 0.0);
                goldccgDeps.put((Integer.parseInt(parts[1])+1)+"--"+(Integer.parseInt(parts[0])+1), dinfo);
                //addToCCGDepMap(dinfo);
                updateCCGNodeDep(dinfo, ccgNodeDeps);
            }
        }
        //System.err.println(ccgUnResDeps.toString());
    }
    
    private HashMap<Integer, CCGNodeDepInfo> updateDepNodeDeps(HashMap<String, CCGDepInfo> gccgdeps, CCGJSentence sent){
        HashMap<Integer, CCGNodeDepInfo> nodeDepMap = new HashMap<>();
        for(String key : drvDeps.keySet()){
            CCGDepInfo info = gccgdeps.get(key);
            if(info == null){
                String[] parts = key.split("--");
                info = gccgdeps.get(parts[1]+"--"+parts[0]);
                if(info==null){
                    //continue;
                    DerivDepInfo ddepinfo = drvDeps.get(key);
                    int pid = ddepinfo.getLeftId(), cid = ddepinfo.getRightId();
                    if(!ddepinfo.isHeadLeft()) { cid = ddepinfo.getLeftId(); pid = ddepinfo.getRightId();}
                    info = new CCGDepInfo(pid, cid, 0, sent.getNode(pid-1).getConllNode().getSuperTag(), false, 0.0);
                }
            }
            updateNodeDepMap(info.getHeadId(), info, nodeDepMap);
            updateNodeDepMap(info.getArgId(), info, nodeDepMap);
        }
        return nodeDepMap;
    }
    
    private HashMap<Integer, CCGNodeDepInfo> updateCCGNodeDeps(HashMap<String, CCGDepInfo> gccgdeps){
        HashMap<Integer, CCGNodeDepInfo> nodeDepMap = new HashMap<>();
        for(String key : gccgdeps.keySet()){
            CCGDepInfo info = gccgdeps.get(key);
            updateCCGNodeDep(info, nodeDepMap);
        }
        //updateExtracted(nodeDepMap);
        return nodeDepMap;
    }
    
    private void updateCCGNodeDep(CCGDepInfo info, HashMap<Integer, CCGNodeDepInfo> nodeDepMap){
        int hid = info.getHeadId(), aid = info.getArgId();
        String key = hid+"--"+aid;
        if(drvDeps.containsKey(hid+"--"+aid) || drvDeps.containsKey(aid+"--"+hid)){
            updateNodeDepMap(hid, info, nodeDepMap);
            updateNodeDepMap(aid, info, nodeDepMap);
        }
    }
    
    public ArrayList TreeCompress(CCGJTreeNode root){
        Map<Integer, CCGJTreeNode> map = new TreeMap<>();
        
        List<CCGJTreeNode> tlist = new ArrayList<>();
        tlist.add(root);
        
        while(!tlist.isEmpty()){
            CCGJTreeNode node = tlist.get(0);
            tlist.remove(0);
            RuleType rtype = node.getRuleType();
            if(rtype.equals(RuleType.other) || rtype.equals(RuleType.lex))
                map.put(node.getConllNode().getNodeId(), node);
            else{
                if(node.isUnary())
                    tlist.add(node.getLeftChild());
                else if(node.isLeaf())
                    map.put(node.getRSpan(), node);
                else {
                    tlist.add(node.getLeftChild());
                    tlist.add(node.getRightChild());
                }
            }
        }
        
        ArrayList<CCGJTreeNode> list = new ArrayList<>();
        for(CCGJTreeNode node : map.values())
            list.add(node);
        return list;
    }
    
    private void updateExtracted(HashMap<Integer, CCGNodeDepInfo> nodeDepMap){
        for(int id : nodeDepMap.keySet()){
            CCGNodeDepInfo ndinfo = nodeDepMap.get(id);
            HashMap<Integer, CCGDepInfo> ldepmap = ndinfo.getLeftAdj();
            HashMap<Integer, CCGDepInfo> rdepmap = ndinfo.getRightAdj();
            HashMap<Integer, CCGDepInfo> nrdepmap = new HashMap();
            if(ldepmap.size() > 0 && rdepmap.size() > 0){
                for(int nid : rdepmap.keySet()){
                    CCGDepInfo info = rdepmap.get(nid);
                    if(info.getExtract()){
                        String key1 = info.getHeadId()+"--"+info.getArgId();
                        String key2 = info.getArgId()+"--"+info.getHeadId();
                        if(drvDeps.containsKey(key1) || drvDeps.containsKey(key2)){
                            //String value = id+"--"+left.getCCGcat().toString()+"--"+right.getCCGcat().toString()+"--"+parNode.getCCGcat().toString();
                            DerivDepInfo ddepinfo = drvDeps.containsKey(key1) ? drvDeps.get(key1) : drvDeps.get(key2);
                            //if(val[2].equals("NP\\NP") || val[2].equals("(S\\NP)\\(S\\NP)")){
                            if(!info.getCat().contains(ddepinfo.getRightCat())){
                                nrdepmap.put(nid, info);
                            }
                        }
                    }
                    else
                        nrdepmap.put(nid, info);
                }
            }
            ndinfo.setRDAdj(nrdepmap);
            nodeDepMap.put(id, ndinfo);
        }
    }
    */
}