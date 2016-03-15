/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.incderivation;

import edinburgh.ccg.deps.CCGcat;
import edinburgh.ccg.deps.DepList;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGDepInfo;
import ilcc.ccgparser.utils.CCGJRuleInfo;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.CCGNodeDepInfo;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import ilcc.ccgparser.utils.DepGraph;
import ilcc.ccgparser.utils.SCoNLLNode;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ccgCombinators;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ambati
 */
public class Commons {
    
    public static int debug = 0;
    public static boolean incalgo = false;
    public static boolean ifbeam = false;
    
    public static void setDebug(int flag){
        debug = flag;
    }
    
    public static void setIncAlgo(boolean flag){
        incalgo = flag;
    }
    
    public static void setIfBeam(boolean flag){
        ifbeam = flag;
    }
    
    public static CCGJTreeNode applyShift(CCGJTreeNode inode, ArcJAction act){
        SCoNLLNode pcnode = inode.getConllNode();
        SCoNLLNode cnode = new SCoNLLNode(pcnode.getNodeId(), pcnode.getWrd().toString(), pcnode.getPOS().toString(), act.getccgCat().toString());
        CCGcat rescat = CCGcat.lexCat(cnode.getWrd().toString(), act.getccgCat().toString(), cnode.getNodeId());
        if(rescat.isAux())
            rescat.handleAux();
        CCGJTreeNode result = CCGJTreeNode.makeLeaf(rescat, cnode);
        return result;
    }
    
    public static CCGJTreeNode applyReduce(CCGJTreeNode left, CCGJTreeNode right, ArcJAction act, HashMap<String, CCGDepInfo> sysccgDeps, DepGraph depGraph) {
        
        CCGCategory cat = act.getccgCat();
        String rescatstr = cat.toString();
        SRAction sract = act.getAction();
        CCGJTreeNode result;
        
        if (sract == SRAction.RU)
            result = applyUnary(right, rescatstr, act, sysccgDeps);
        else
            result = applyReduceBinary(left, right, rescatstr, (sract == SRAction.RR) ? "left" : "right", act, sysccgDeps, depGraph);
        return result;
    }
    
    public static CCGJTreeNode applyLReveal(CCGJTreeNode left, CCGJTreeNode right, ArcJAction act, List<CCGJTreeNode> input, HashMap<String, CCGDepInfo> sysccgDeps, DepGraph depGraph, boolean flag, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        CCGJTreeNode lleft, result;
        CCGcat llcat, rlcat, lcat, rcat;      
        int lid = left.getNodeId(), rid = right.getNodeId();
        boolean isconj = false;
        rcat = right.getCCGcat();
        
        if( (right.getCCGcat().matches("(S\\NP)\\(S\\NP)") || right.getCCGcat().matches("(S\\NP)\\((S\\NP)/NP)")) ){
            lcat = left.getCCGcat().matches("S") ? left.getCCGcat() : CCGcat.ccgCatFromString("S");
            rcat = CCGcat.typeChangingRule(right.getCCGcat(), "(S\\NP)\\(S\\NP)");
            rlcat = CCGcat.lexCat(left.getWrdStr(), lcat.catString()+"\\NP", lid);
        }
        else if ( (rcat.matches("S\\NP") && rcat.toString().endsWith("[conj]")) ){
            lcat = left.getCCGcat().matches("S") ? left.getCCGcat() : CCGcat.ccgCatFromString("S");
            rlcat = CCGcat.typeChangingRule(lcat, "S\\NP");
            isconj = true;
        }
        else
            rlcat = CCGcat.typeChangingRule(left.getCCGcat(), "S\\NP");
        
        Integer lvert = depGraph.getLeftMost(lid, "N|NP");
        
        if(lvert == null )
            llcat = CCGcat.ccgCatFromString("NP");
        else {
            CCGcat tmpcat = CCGcat.lexCat(input.get(lvert-1).getWrdStr(), depGraph.getVertex(lvert).toString(), lvert);
            llcat = CCGcat.typeChangingRule(tmpcat, "NP");
        }
        
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        
        CCGJRuleInfo tinfo = ccgCombinators.checkCCGRules(rlcat, rcat);
        depsMap = getDepsMap(rlcat, rcat, tinfo.getResultCat(), depsMap);
        CCGJRuleInfo info = ccgCombinators.checkCCGRules(llcat, tinfo.getResultCat());
        //depsMap = getDepsMap(llcat, tinfo.getResultCat(), info.getResultCat(), depsMap);
        updateDepTree(tinfo, left.getNodeId(), right.getNodeId(), depGraph);
        if(isconj)
            result = applyBinaryUpdate(left, right, info.getResultCat(), act, RuleType.lreveal, true);
        else
            result = applyBinaryUpdate(left, right, left.getCCGcat(), act, RuleType.lreveal, true);
        applyupdateDepTree(left, right, SRAction.LREVEAL, depsMap, sysccgDeps, depGraph);
        updateSysDeps(depsMap, sysccgDeps);
        if(flag)
            updateccgNodeDeps(left, right, SRAction.LREVEAL, nccgNodeDeps, depsMap, false);
        return result;
    }
    
    public static CCGJTreeNode applyRReveal(CCGJTreeNode left, CCGJTreeNode right, ArcJAction act, List<CCGJTreeNode> input, HashMap<String, CCGDepInfo> sysccgDeps, DepGraph depGraph, boolean flag, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        CCGJTreeNode result;
        CCGcat llcat, rlcat, lcat, rcat, icat;
        int lid, rlid, rpid, rid;
        rcat = right.getCCGcat();
        boolean isconj = false;        
        
        if(rcat.toString().endsWith("[conj]")){
            icat = CCGcat.ccgCatFromString(rcat.toString().replace("[conj]", ""));
            isconj = true;
        }
        else if((rcat.argDir()==CCGcat.BW))
            icat = rcat.argument().copy();
        else
            //icat = rcat.copy();
            return applyBinaryUpdate(left, right, left.getCCGcat(), act, RuleType.rreveal, true);
        
        lid = left.getNodeId();
        rid = right.getNodeId();
        
        Integer lvertex = lid;
        ArrayList<Integer> rightPerList = depGraph.getRightPer(lvertex);
        
        int level = act.getLevel();
        int index = rightPerList.size()-level;
        if(index <=0)
            return null;
        
        Integer rmost = rightPerList.get(index);
        rlid = rmost;
        rpid = rightPerList.get(index-1);
        
        lcat = left.getCCGcat().copy();
        rcat = right.getCCGcat();
        rlcat = CCGcat.lexCat(input.get(rlid-1).getWrdStr(), icat.catString(), rlid);
        String rlcatstr = (rlcat.isAtomic() ? rlcat.toString() :  "("+rlcat.toString()+")");
        String rcatstr = lcat.isAtomic() ? lcat.toString()+"/"+ rlcatstr : "("+lcat.toString()+")/" + rlcatstr;
        llcat = lcat.revealCat(lcat, rcatstr);
        
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        CCGJRuleInfo tinfo = ccgCombinators.checkCCGRules(rlcat, rcat);
        depsMap = getDepsMap(rlcat, rcat, tinfo.getResultCat(), depsMap);
        CCGJRuleInfo info = ccgCombinators.checkCCGRules(llcat, tinfo.getResultCat());
        updateDepTree(tinfo, rlid, rid, depGraph);
        for(int id : rcat.headIdList()){
            updateDepTree(tinfo, rlid, id, depGraph);
            updateRevealDeps(tinfo, rlid, id, rpid, depsMap, sysccgDeps, depGraph);
        }
        result = applyBinaryUpdate(left, right, left.getCCGcat(), act, RuleType.rreveal, true);
        //result = CCGJTreeNode.makeUnary(left.getCCGcat(), RuleType.rreveal, left);
        //left.setParent(result);
        //result = applyUnary(left, left.getCCGcat(), sysccgDeps)Update(left, right, left.getCCGcat(), RuleType.rreveal, true);
        if(!isconj)
            applyupdateDepTree(left, right, SRAction.RREVEAL, depsMap, sysccgDeps, depGraph);
        updateSysDeps(depsMap, sysccgDeps);
        if(flag)
            updateccgNodeDeps(left, right, SRAction.RREVEAL, nccgNodeDeps, depsMap, false);
        return result;
    }
    
    public static void updateDepTree(CCGJRuleInfo info, int pid, int cid, DepGraph depGraph){
        depGraph.addEdge(pid, cid);
    }
    
    public static void updateRevealDeps(CCGJRuleInfo info, int rlid, int rid, int rpid, HashMap<String, CCGDepInfo> depsMap, HashMap<String, CCGDepInfo> sysccgDeps, DepGraph depGraph){        
        if(!info.getRightCat().catString().endsWith("[conj]")) return;
        info.getResultCat().filledDependencies = null;
        String key1 = rpid+"--"+rlid, key2 = rlid+"--"+rpid, nkey = rpid+"--"+rid;
        CCGDepInfo pccgdepinfo;
        String cat = null;
        if((pccgdepinfo = sysccgDeps.get(key1)) == null){
            pccgdepinfo = sysccgDeps.get(key2);
            nkey = rid+"--"+rpid;
            if(pccgdepinfo == null) return;
            cat = depGraph.getVertex(rid).toString();
        }
        if(pccgdepinfo != null){
            if(cat == null)
                cat = pccgdepinfo.getCat();
            CCGDepInfo nccgdepinfo = new CCGDepInfo(rpid, rid, pccgdepinfo.getSlot(), cat, false, 0.0);
            //sysccgDeps.put(nkey, nccgdepinfo);
            depsMap.put(nkey, nccgdepinfo);
        }
    }
    
    public static CCGJTreeNode applyUnary(CCGJTreeNode left, String rescatstr, ArcJAction act, HashMap<String, CCGDepInfo> sysccgDeps){
        
        CCGcat lcat, rescat;
        RuleType rtype;
        lcat = left.getCCGcat();
        rescat = CCGcat.typeRaiseTo(lcat, rescatstr);
        if (rescat == null) {
            rescat = CCGcat.typeChangingRule(lcat, rescatstr);
            rtype = RuleType.lex;
        }
        else
            rtype = RuleType.tr;
        
        CCGJTreeNode result = CCGJTreeNode.makeUnary(rescat, act, rtype, left);
        left.setParent(result);
        updateSysDeps(lcat.filledDependencies, sysccgDeps);
        updateSysDeps(rescat.filledDependencies, sysccgDeps);
        lcat.filledDependencies = rescat.filledDependencies = null;
        return result;
    }
    
    public static CCGJTreeNode applyReduceBinary(CCGJTreeNode left, CCGJTreeNode right, String rescatstr, String headDir, ArcJAction act, HashMap<String, CCGDepInfo> sysccgDeps, DepGraph depGraph){
        
        boolean isHeadLeft = "left".equals(headDir);
        CCGcat lcat = left.getCCGcat();
        CCGcat rcat = right.getCCGcat();
        RuleType ruletype = act.getRuleType();
        
        if(debug >=2)
            System.err.println(" Reduce : "+lcat.toString()+" "+rcat.toString());
        
        CCGcat rescat = null;
        
        if(ifbeam){
            /*rescat = ccgCombinators.checkRule(lcat, rcat, ruletype);
            if(rescat == null){
                rescat = CCGcat.typeChangingRule(isHeadLeft ? lcat : rcat, rescatstr);
                rescat.catString = rescatstr;
            }*/
            rescat = CCGcat.combine(lcat, rcat, rescatstr);
            if(rescat == null || !rescat.toString().equals(rescatstr))
                rescat = ccgCombinators.checkRule(lcat, rcat, ruletype);
            if(rescat == null){
                rescat = CCGcat.typeChangingRule(isHeadLeft ? lcat : rcat, rescatstr);
                //rescat = CCGcat.lexCat(isHeadLeft ? left.getWrdStr() : right.getWrdStr(), rescatstr, isHeadLeft ? left.getNodeId() : right.getNodeId());
                rescat.catString = rescatstr;
            }
        }
        else{
            rescat = CCGcat.combine(lcat, rcat, rescatstr);
            if(rescat == null || !rescat.toString().equals(rescatstr))
                rescat = ccgCombinators.checkRule(lcat, rcat, ruletype);
            if(rescat == null){
                rescat = CCGcat.typeChangingRule(isHeadLeft ? lcat : rcat, rescatstr);
                //rescat = CCGcat.lexCat(isHeadLeft ? left.getWrdStr() : right.getWrdStr(), rescatstr, isHeadLeft ? left.getNodeId() : right.getNodeId());
                rescat.catString = rescatstr;
            }
        }
        
        CCGJTreeNode result;
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        getDepsMap(lcat, rcat, rescat, depsMap);
        if(incalgo)
            applyupdateDepTree(left, right, act.getAction(), depsMap, sysccgDeps, depGraph);
        updateSysDeps(depsMap, sysccgDeps);
        result = applyBinaryUpdate(left, right, rescat, act, ruletype, isHeadLeft);
        return result;
    }
    
    protected static void applyupdateDepTree(CCGJTreeNode left, CCGJTreeNode right, SRAction act, HashMap<String, CCGDepInfo> depsMap, HashMap<String, CCGDepInfo> sysccgDeps, DepGraph depGraph){
        int pid, cid;
        int lid = left.getConllNode().getNodeId(), rid = right.getConllNode().getNodeId();
        
        for(String key : depsMap.keySet()){
            if(sysccgDeps.containsKey(key))
                continue;
            CCGDepInfo cdepinfo = depsMap.get(key);
            pid = cdepinfo.getHeadId();
            cid = cdepinfo.getArgId();
            
            if((act == SRAction.RL) && (lid == pid && rid == cid)){
                pid = rid;
                cid = lid;
            }
            if((act == SRAction.RR) && (lid == cid && rid == pid)){
            //else {
                pid = lid;
                cid = rid;
            }
            depGraph.addEdge(pid, cid);
        }
    }
    
    protected static HashMap getDepsMap(CCGcat lcat, CCGcat rcat, CCGcat rescat, HashMap<String, CCGDepInfo> depsMap){
        getDepsMap(lcat.filledDependencies, depsMap);
        getDepsMap(rcat.filledDependencies, depsMap);
        getDepsMap(rescat.filledDependencies, depsMap);
        lcat.filledDependencies = rcat.filledDependencies = rescat.filledDependencies = null;
        return depsMap;
    }
    
    protected static boolean getDepsMap(DepList dep, HashMap<String, CCGDepInfo> depsMap){
        while(dep!=null){
            String key = dep.headIndex+"--"+dep.argIndex;
            CCGDepInfo dinfo = new CCGDepInfo(dep.headIndex, dep.argIndex, dep.argPos, dep.headCat, dep.extracted, 0.0);
            depsMap.put(key, dinfo);
            dep = dep.next();
        }
        return true;
    }
        
    public static void updateSysDeps(DepList dep, HashMap<String, CCGDepInfo> sysccgDeps){
        while(dep!=null){
            String key = dep.headIndex+"--"+dep.argIndex;
            if(!sysccgDeps.containsKey(key)){
                CCGDepInfo dinfo = new CCGDepInfo(dep.headIndex, dep.argIndex, dep.argPos, dep.headCat, dep.extracted, 0.0);
                sysccgDeps.put(key, dinfo);
            }
            dep = dep.next();
        }
    }
    
    protected static void updateSysDeps(HashMap<String, CCGDepInfo> depsMap, HashMap<String, CCGDepInfo> sysccgDeps){
        if(depsMap == null) return;
        for(String key : depsMap.keySet()){
            CCGDepInfo dinfo = depsMap.get(key);
            if(!sysccgDeps.containsKey(key)){
                sysccgDeps.put(key, dinfo);
            }
        }
    }
    
    public static CCGJTreeNode applyBinaryUpdate(CCGJTreeNode left, CCGJTreeNode right, CCGcat rescat, ArcJAction ajact, ccgCombinators.RuleType rtype, boolean isHeadLeft){
        CCGJTreeNode result = CCGJTreeNode.makeBinary(rescat, ajact, rtype, isHeadLeft, left, right);
        left.setParent(result);
        right.setParent(result);
        return result;
    }
    
    public static void updateccgNodeDeps(CCGJTreeNode left, CCGJTreeNode right, SRAction act, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps, HashMap<String, CCGDepInfo> depsMap, boolean isconj){
        int lid = left.getConllNode().getNodeId(), rid = right.getConllNode().getNodeId();
        updateccgNodeDep(lid, rid, act, nccgNodeDeps, depsMap);
        updateccgNodeDep(rid, lid, act, nccgNodeDeps, depsMap);
    }
    
    public static void updateccgNodeDep(int lid, int rid, SRAction act, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps, HashMap<String, CCGDepInfo> depsMap){
        
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
    
}
