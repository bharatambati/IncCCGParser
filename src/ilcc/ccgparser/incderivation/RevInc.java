/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.incderivation;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGDepInfo;
import ilcc.ccgparser.utils.CCGJRuleInfo;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.CCGNodeDepInfo;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import ilcc.ccgparser.utils.SCoNLLNode;
import ilcc.ccgparser.utils.Utils;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ccgCombinators;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author ambati
 */

public class RevInc extends SRParser{
    
    HashMap<String, Integer> auxModalMap;
    HashMap<Integer, List<ArcJAction>> actMap;
    HashMap<Integer, Integer> revealLevel;    
    HashMap<String, HashMap<String, Integer>> unResolveMap;
    HashMap<String, Integer> unResolveMap2;
    String str;
    
    public RevInc() throws IOException {
        init();
    }
    
    @Override
    public void init() throws IOException{
        super.init();        
        auxModalMap = new HashMap<>();
        actMap = new HashMap<>();
        revealLevel = new HashMap<>();        
        unResolveMap = new HashMap<>();
        unResolveMap2 = new HashMap<>();        
        incalgo = true;
    }
    
    @Override
    public List<ArcJAction> parse(CCGJSentence sent){
        actMap = new HashMap<>();
        //updateGoldDepsVP2S();
        sysccgDeps.clear();
        //input = TreeCompress(sent.getDerivRoot());
        //compressNodeCount += input.size();
        CCGJTreeNode root = sent.getDerivRoot();
        CCGJTreeNode fnode = null;
        ArrayList<CCGJTreeNode> list = new ArrayList<>();
        HashMap<Integer, CCGJTreeNode> map = new HashMap<>();
        HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps;
        postOrder(root, list);
        CCGJTreeNode finalnode = null;
        for(CCGJTreeNode curnode : list){
            if(curnode.isLeaf())
                map.put(curnode.getRSpan(), curnode);
            
            RuleType rtype = curnode.getRuleType();
            if(rtype.equals(RuleType.lp) || rtype.equals(RuleType.rp)){
                updateNodeWaitInfo(curnode, rtype);
            }
            if(rtype.equals(RuleType.other) || rtype.equals(RuleType.fa) || rtype.equals(RuleType.conj) //|| rtype.equals(RuleType.conjF)
                    ){
                CCGJTreeNode left = curnode.getLeftChild();
                CCGJTreeNode right = curnode.getRightChild();
                if(rtype.equals(RuleType.fa) && (!left.getCCGcat().matches("NP/N") || !right.getCCGcat().matches("N") ) ){
                        finalnode = curnode;                        
                        continue;
                    }
                nccgNodeDeps = updateStackInput(left, map);
                CCGJTreeNode nleft = Incrementize(nccgNodeDeps);
                nccgNodeDeps = updateStackInput(right, map);
                CCGJTreeNode nright = Incrementize(nccgNodeDeps);
                boolean headIsLeft = (curnode.getHeadDir()==0);
                ftrue = true;
                ArcJAction ajact = ArcJAction.make((curnode.getHeadDir()==0) ? SRAction.RR : SRAction.RL, 0, curnode.getCCGcat().toString(), rtype);
                CCGJTreeNode nnode = applyBinary(nleft, nright, curnode.getCCGcat().toString(), headIsLeft, (curnode.getHeadDir()==0) ? SRAction.RR : SRAction.RL, ajact);
                updateMap(curnode, nnode, map);
                ftrue = false;
                int lid = nleft.getConllNode().getNodeId(), rid = nright.getConllNode().getNodeId();
                List<ArcJAction> acts = actMap.get(lid);
                acts.addAll(actMap.get(rid)); acts.add(ajact);
                actMap.remove(lid);actMap.remove(rid);
                actMap.put(nnode.getConllNode().getNodeId(), acts);
                
                CCGcat lcat = nleft.getCCGcat(), rcat = right.getCCGcat(), rescat = nnode.getCCGcat();
                CCGJRuleInfo info = new CCGJRuleInfo(lcat, rcat, rescat, headIsLeft, rtype, 0, 0);
                treebankRules.addBinaryRuleInfo(info, lcat.toString()+" "+rcat.toString());
            }
            /*
            if(rtype.equals(RuleType.rp) && curnode.getRightChild().getCCGcat().toString().equals("RRB") ){
                CCGJTreeNode left = curnode.getLeftChild();
                CCGJTreeNode right = curnode.getRightChild();
                if(left.getChildCount()==2 && left.getLeftChild().getConllNode().getWrd().equals("-LRB-") && ( left.getCCGcat().matches("NP\\NP") || left.getCCGcat().matches("N\\N")) ){
                    CCGJTreeNode lleft = left.getLeftChild();
                    CCGJTreeNode rleft = left.getRightChild();
                    nccgNodeDeps = updateStackInput(rleft, map);
                    CCGJTreeNode nrleft = Incrementize(nccgNodeDeps);
                    String hdir = (left.getHeadDir()==0) ? "left" : "right";
                    ftrue = true;
                    CCGJTreeNode nleft = applyBinary(lleft, nrleft, left.getCCGcat().toString(), hdir);
                    hdir = (curnode.getHeadDir()==0) ? "left" : "right";
                    CCGJTreeNode nnode = applyBinary(nleft, right, curnode.getCCGcat().toString(), hdir);
                    updateMap(curnode, nnode, map);
                    ftrue = false;
                                    
                ArcJAction ajact = ArcJAction.make((curnode.getHeadDir()==0) ? SRAction.RR.toString() : SRAction.RL.toString(), 0, curnode.getCCGcat().toString());
                }
            }
            */
            
            else if(rtype.equals(RuleType.lex) || ( rtype.equals(RuleType.tr) && !curnode.getParent().getRuleType().equals(RuleType.gfc)) ) {
                nccgNodeDeps = updateStackInput(curnode.getLeftChild(), map);
                fnode = Incrementize(nccgNodeDeps);
                
                if(fnode.getNodeId() != curnode.getLeftChild().getNodeId()){
                    //System.err.println(sentCount+" Incrementize failed for unary rule");
                    return null;
                }
                
                String catStr = curnode.getCCGcat().catString();
                /*
                if(rtype.equals(RuleType.tr) && curnode.getCCGcat().catString().startsWith("(S\\NP)")){
                    if(!curnode.getParent().getRightChild().getCCGcat().catString().equals("(S\\NP)\\(S\\NP)"))
                        catStr = catStr.replace("(S\\NP)", "S");
                }*/
                
                ArcJAction ajact = ArcJAction.make(SRAction.RU, 0, catStr, rtype);
                CCGJTreeNode nnode = Commons.applyUnary(fnode, catStr, ajact, sysccgDeps);
                updateMap(curnode, nnode, map);
                actMap.get(fnode.getConllNode().getNodeId()).add(ajact);
                CCGcat lcat = fnode.getCCGcat(), rescat = nnode.getCCGcat();
                CCGJRuleInfo info = new CCGJRuleInfo(lcat, null, rescat, true, rtype, 0, -1);
                
                if(lcat.toString().equals(info.getResultCat().toString()))
                    System.err.println(sentCount+" Recursive unary rule"+lcat.toString()+" "+info.getResultCat().toString());
                else
                    treebankRules.addUnaryRuleInfo(info, lcat.toString());
                //actMap.put(nnode.getConllNode().getNodeId(), actMap.get(fnode.getConllNode().getNodeId()));
            }
            finalnode = curnode;
        }
        if(map.size() >1){
            nccgNodeDeps = updateStackInput(finalnode, map);
            fnode = Incrementize(nccgNodeDeps);
            if(stack.size()!=1){
                ftrue = true;
                for(int i = 0; i< stack.size(); i++)
                    input.add(stack.get(i));
                //Incrementize(ccgNodeDeps);
                ftrue = false;
            }
        }
        if(uncov || fnode == null)
            return null;
        else
            return actMap.get(fnode.getConllNode().getNodeId());
    }
    
    private void updateMap(CCGJTreeNode curnode, CCGJTreeNode fnode, HashMap<Integer, CCGJTreeNode> map){
        CCGJTreeNode node;
        int lspan, rspan;
        lspan = curnode.getLSpan();
        rspan = curnode.getRSpan();
        for(int i = lspan; i<= rspan; i++){
            if( (node = map.get(i)) != null){
                map.remove(i);
            }
        }
        map.put(rspan, fnode);
    }
    
    private HashMap<Integer, CCGNodeDepInfo> updateStackInput(CCGJTreeNode curnode, HashMap<Integer, CCGJTreeNode> map){
        
        CCGJTreeNode node;
        input.clear(); stack.clear();
        CCGNodeDepInfo depInfo;
        HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps = new HashMap<>();
        int lspan, rspan;
        lspan = curnode.getLSpan();
        rspan = curnode.getRSpan();
        for(int i = lspan; i<= rspan; i++){
            if( (node = map.get(i)) != null){
                input.add(node);
            }
            if( (depInfo = ccgNodeDeps.get(i)) != null){
                CCGNodeDepInfo ndepInfo = new CCGNodeDepInfo();
                ndepInfo.copyCCGNodeDepInfo(depInfo, lspan, rspan);
                nccgNodeDeps.put(i, ndepInfo);
            }
        }
        return nccgNodeDeps;
    }
    
    @Override
    public CCGJTreeNode shift() {
        CCGJTreeNode result = input.get(0);
        //CCGJTreeNode nresult = VP2SUnary(result);
        CCGJTreeNode nresult = result;
        stack.push(nresult);
        input.remove(0);
        depGraph.addVertex(nresult.getNodeId(), nresult.getConllNode().getccgCat());
        return result;
    }
    
    private void leftArc(CCGJTreeNode left, CCGJTreeNode right, CCGJRuleInfo rinfo, HashMap<String, CCGDepInfo> depsMap) {
        Commons.updateSysDeps(depsMap, sysccgDeps);
    }
    
    private void rightArc(CCGJTreeNode left, CCGJTreeNode right, CCGJRuleInfo rinfo, HashMap<String, CCGDepInfo> depsMap) {
        Commons.updateSysDeps(depsMap, sysccgDeps);
    }
    
    private CCGJTreeNode reduce(CCGJTreeNode left, CCGJTreeNode right, SRAction sract, CCGJRuleInfo rinfo) {
        boolean headIsLeft = rinfo.getHeadDir();
        ArcJAction act = ArcJAction.make(sract, 0, rinfo.getResultCat().toString(), rinfo.getCombinator());
        CCGJTreeNode result = Commons.applyBinaryUpdate(left, right, rinfo.getResultCat(), act, rinfo.getCombinator(), headIsLeft);
        return result;
    }
    
    private CCGJTreeNode Incrementize(HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        stack = new Stack<>();
        CCGJTreeNode fnode = null;
        List<ArcJAction> acts = new ArrayList<>();
        
        while(!input.isEmpty()){
            int cid = input.get(0).getNodeId();
            //calculateWaitTime(cid-1, nccgNodeDeps);
            List<ArcJAction> pacts;
            if((pacts = actMap.get(cid)) != null){
                    acts.addAll(pacts);
                    actMap.remove(cid);
            }
            else
                acts.add(ArcJAction.make(SRAction.SHIFT, 0, input.get(0).getCCGcat().toString(), RuleType.lexicon));
                
            actionMap.put(SRAction.SHIFT, actionMap.get(SRAction.SHIFT)+1);
            shift();
            while(stack.size()>=2){
                CCGJTreeNode right = stack.pop();
                CCGJTreeNode left = stack.pop();
                SCoNLLNode cleft = left.getConllNode();
                SCoNLLNode cright = right.getConllNode();
                int id = cright.getNodeId();
                String ccgKey = cleft.getNodeId()+"--"+cright.getNodeId();
                String ccgKey2 = cright.getNodeId()+"--"+cleft.getNodeId();
                /*
                if(specialcase(left, right)){
                    stack.push(left);
                    stack.push(right);
                    break;
                }
                */
                
                if ((nccgNodeDeps.containsKey(id) && nccgNodeDeps.get(id).ldepsize()>0) || drvDeps.containsKey(ccgKey) || drvDeps.containsKey(ccgKey2) 
                        || cright.getWrd().equals(cright.getPOS())){
                    Pair<CCGJTreeNode, ArcJAction> tuple = checkAndApplyAction(left, right, ccgKey, nccgNodeDeps);
                    if(tuple == null || tuple.getLeft() == null){
                        stack.push(left);
                        stack.push(right);
                        //fillUnResolveList(left, right, drvDeps.get(ccgKey));
                        break;
                    }
                    try{
                        ArcJAction act = tuple.getRight();
                        acts.add(act);
                        CCGJTreeNode result = tuple.getLeft();
                        CCGcat lcat = left.getCCGcat(), rcat = right.getCCGcat(), rescat = result.getCCGcat();
                        boolean headIsLeft = (act.getAction()!=SRAction.RL);
                        //if( (act.getRuleType() != RuleType.rreveal) && (act.getRuleType() != RuleType.lreveal) ){
                        CCGJRuleInfo info = new CCGJRuleInfo(lcat, rcat, rescat, headIsLeft, act.getRuleType(), act.getLevel(), 0);
                        if(act.getRuleType() == RuleType.lreveal)
                            treebankRules.addRevealRuleInfo(info, lcat.toString()+" "+rcat.toString());
                        else if(act.getRuleType() != RuleType.rreveal)
                            treebankRules.addBinaryRuleInfo(info, lcat.toString()+" "+rcat.toString());
                    }
                    catch(Exception ex){
                        System.err.println(ex);
                    }
                }
                else{
                    stack.push(left);
                    stack.push(right);
                    break;
                }
            }
        }
        if(!stack.empty()){
            fnode = stack.peek();
            actMap.put(fnode.getConllNode().getNodeId(), acts);
        }
        if(stack.size() != 1){
            //System.err.println(sentCount+" : "+sent.getLength()+" : Couldn't incrementalize completely : "+ stack.toString());
            uncov = true;
        }
        return fnode;
    }
    
    private Pair<CCGJTreeNode, ArcJAction> checkAndApplyAction(CCGJTreeNode left, CCGJTreeNode right, String ccgKey, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps){
        Pair<CCGJTreeNode, ArcJAction> pair;
        CCGJTreeNode result;
        SRAction sract;
        RuleType rule;
        
        String key1 = left.getConllNode().getNodeId()+"--"+right.getConllNode().getNodeId(), key2 = right.getConllNode().getNodeId()+"--"+left.getConllNode().getNodeId();
        CCGJRuleInfo info = checkRules(left, right, ccgKey);
        if(info != null) {
            updateHeadDirection(left, right, info, ccgKey);
            HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
            Commons.getDepsMap(left.getCCGcat(), right.getCCGcat(), info.getResultCat(), depsMap);
            if(ftrue || (checkWithGoldDeps(depsMap) && !depsMap.isEmpty()) || 
                    (depsMap.isEmpty() && (drvDeps.containsKey(key1) || drvDeps.containsKey(key2) || (Utils.isPunct(left.getCCGcat()) || Utils.isPunct(right.getCCGcat())) )) ){
                sract =  info.getHeadDir() ? SRAction.RR : SRAction.RL;
                updateccgNodeDeps(left, right, sract, nccgNodeDeps, depsMap, false);
                result = applyAction(left, right, info, depsMap, sract);
                actionMap.put(sract, actionMap.get(sract)+1);
                
                if(info.getCombinator() == RuleType.other)
                    rule = findCombinator(info.getLeftCat(), info.getRightCat(), info.getResultCat().toString());
                else
                    rule = info.getCombinator();
                ArcJAction act = ArcJAction.make(sract, 0, info.getResultCat().toString(), rule);
                pair = new ImmutablePair(result, act);
            }
            else{
                pair = checkSpecialRulesDep(left, right, ccgKey, nccgNodeDeps);
                if(pair != null && pair.getLeft() != null)
                    stack.push(pair.getLeft());
            }
        }
        else //if(right.getCCGcat().toString().endsWith("[conj]"))
        {
            pair = checkSpecialRulesDep(left, right, ccgKey, nccgNodeDeps);
            if(pair != null && pair.getLeft() != null)
                    stack.push(pair.getLeft());
        }
        return pair;
    }
    
    private CCGJTreeNode applyAction(CCGJTreeNode left, CCGJTreeNode right, CCGJRuleInfo info, HashMap<String, CCGDepInfo> depsMap, SRAction act){
        CCGJTreeNode result = null;
        if(act == SRAction.RL){
            leftArc(left, right, info, depsMap);
            result = reduce(left, right, act, info);
            stack.push(result);
        }
        else if(act == SRAction.RR){
            rightArc(left, right, info, depsMap);
            result = reduce(left, right, act, info);
            stack.push(result);
        }
        return result;
    }
    
    public CCGJRuleInfo checkRules(CCGJTreeNode left, CCGJTreeNode right, String key){
        CCGcat lcat, rcat;
        CCGJRuleInfo info = checkGoldPunct(left, right);
        lcat = left.getCCGcat(); rcat = right.getCCGcat();
        String key2 = lcat.toString()+" "+rcat.toString()+"--"+left.getConllNode().getNodeId()+" "+right.getConllNode().getNodeId();
        
        if (info == null){
            info = checkTreebankRulesCache(lcat, rcat, key2);
        }
        if (info == null){
            String ckey1 = left.getConllNode().getNodeId()+"--"+right.getConllNode().getNodeId(), ckey2 = right.getConllNode().getNodeId()+"--"+left.getConllNode().getNodeId();
            if(rcat.toString().endsWith("[conj]")) {
                if((goldccgDeps.containsKey(ckey1) || goldccgDeps.containsKey(ckey2) || drvDeps.containsKey(ckey1) || drvDeps.containsKey(ckey2)) && rcat.matches(lcat))
                    info = ccgCombinators.checkCCGRules(lcat, rcat);
            }
            else if(!rcat.matches("(S\\NP)\\(S\\NP)"))
                info = ccgCombinators.checkCCGRules(lcat, rcat);
            if(info == null){
                //result = checkSpecialRules(left, right, key);                
                String rcatStr = rcat.catString();
                if(rcatStr.startsWith("(S\\NP)\\((S\\NP)")){
                    rcatStr = rcatStr.replace("(S\\NP)", "S");
                    rcat = CCGcat.typeChangingRule(rcat, rcatStr);
                    info = ccgCombinators.checkCCGRules(lcat, rcat);
                }
            }
        }
        else{
            CCGcat rescat = CCGcat.combine(lcat, rcat, info.getResultCat().toString());
            info = new CCGJRuleInfo(lcat, rcat, rescat, info.getHeadDir(), info.getCombinator(), info.getLevel(), info.getRuleCount());
        }
        
        return info;
    }
    
    private CCGJRuleInfo checkTreebankRulesCache(CCGcat lcat, CCGcat rcat, String key){
        List<CCGJRuleInfo> infolist = goldSentRules.getBinRuleInfo(key);
        if(infolist == null)
            return null;
        else
            return infolist.get(0);
    }    
        
    private Pair<CCGJTreeNode, ArcJAction> checkSpecialRulesDep(CCGJTreeNode left, CCGJTreeNode right, String key, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        
        Pair<CCGJTreeNode, ArcJAction> pair;
        pair = checkLeftRevealDep(left, right, "(S/S)/NP", nccgNodeDeps);
        if(pair == null || pair.getLeft() == null)
            pair = checkRightRevealDep(left, right, "(S/S)/NP", nccgNodeDeps);
        //if(result == null)
        //    result = checkRightReveal(left, right, "(S/S)/NP", nccgNodeDeps);
        return pair;
    }
    
    private Pair<CCGJTreeNode, ArcJAction> checkLeftRevealDep(CCGJTreeNode left, CCGJTreeNode right, String rescatstr, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        CCGJTreeNode lleft, result = null;
        RuleType rule = RuleType.other;
        int lid = left.getConllNode().getNodeId(), rid = right.getConllNode().getNodeId();
        CCGcat llcat, rlcat, lcat, rcat;
        String key1 = lid+"--"+rid, key2 = rid+"--"+lid;
        boolean isconj = false;
        rcat = right.getCCGcat();
        if((drvDeps.containsKey(key1) || drvDeps.containsKey(key2))){
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
                return null;
            
            Integer vert = lid;
            Integer lvert = depGraph.getLeftMost(vert, "N|NP");
            
            if(lvert == null)
                llcat = CCGcat.ccgCatFromString("NP");
            else
                llcat = CCGcat.typeChangingRule(sent.getNode(lvert-1).getCCGcat(), "NP");
            
            HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
            
            CCGJRuleInfo tinfo = ccgCombinators.checkCCGRules(rlcat, rcat);
            if(tinfo != null){
                depsMap = Commons.getDepsMap(rlcat, rcat, tinfo.getResultCat(), depsMap);
                CCGJRuleInfo info = ccgCombinators.checkCCGRules(llcat, tinfo.getResultCat());
                if(info != null){
                    //depsMap = getDepsMap(llcat, tinfo.getResultCat(), info.getResultCat(), depsMap);
                    if(ftrue || checkWithGoldDeps(depsMap)){
                        Commons.updateDepTree(tinfo, left.getConllNode().getNodeId(), right.getConllNode().getNodeId(), depGraph);
                        ArcJAction act = ArcJAction.make(SRAction.LREVEAL, 0, (result==null) ? null : result.getCCGcat().toString(), rule);
                        if(isconj)
                            result = Commons.applyBinaryUpdate(left, right, info.getResultCat(), act, RuleType.lreveal, true);
                        else
                            result = Commons.applyBinaryUpdate(left, right, left.getCCGcat(), act, RuleType.lreveal, true);
                        updateccgNodeDeps(left, right, SRAction.LREVEAL, nccgNodeDeps, depsMap, false);
                        Commons.updateSysDeps(depsMap, sysccgDeps);
                        actionMap.put(SRAction.LREVEAL, actionMap.get(SRAction.LREVEAL)+1);
                        rule = RuleType.lreveal;
                    }
                }                
            }
        }
        return new ImmutablePair(result, ArcJAction.make(SRAction.LREVEAL, 0, (result==null) ? null : result.getCCGcat().toString(), rule));
    }    
    
    private Pair<CCGJTreeNode, ArcJAction> checkRightRevealDep(CCGJTreeNode left, CCGJTreeNode right, String rescatstr, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        CCGJTreeNode result = null;
        RuleType rule = RuleType.other;
        CCGcat llcat, rlcat =null, lcat, rcat, icat, revcat = null;
        int lid, rlid = 0, rpid =0, rid;
        lcat = left.getCCGcat().copy();
        rcat = right.getCCGcat().copy();
        boolean isconj = false;
        
        if(rcat.toString().endsWith("[conj]")){
            icat = CCGcat.ccgCatFromString(rcat.toString().replace("[conj]", ""));
            isconj = true;
        }
        else if((rcat.argDir()==CCGcat.BW))
            icat = rcat.argument().copy();
        else
            return null;
        
        lid = left.getConllNode().getNodeId();
        rid = right.getConllNode().getNodeId();        
        int level = 0;
        
        CCGCategory lhcat = left.getConllNode().getccgCat();
        Integer lvertex = lid;
        ArrayList<Integer> rightPerList = depGraph.getRightPer(lvertex);
        
        for(int i=rightPerList.size()-1; i>0; i--){
            Integer rmost = rightPerList.get(i);
            rlid = rmost;
            rpid = rightPerList.get(i-1);
            String key1 = rlid+"--"+rid, key2 = rid+"--"+rlid;
            CCGcat tcat = CCGcat.lexCat(sent.getNode(rlid-1).getWrdStr(), depGraph.getVertex(rmost).toString(), rlid);
            revcat = tcat;
            if(tcat.catString().equals("N"))
                tcat = CCGcat.typeChangingRule(tcat, "NP");
            level++;
            if(goldccgDeps.containsKey(key1) || goldccgDeps.containsKey(key2) || drvDeps.containsKey(key1) || drvDeps.containsKey(key2)){
                //if(canCompose(tcat, right.getCCGcat()) || CCGcat.noFeatures(tcat.catString()).contains(CCGcat.noFeatures(right.getCCGcat().catString())) ){
                rlcat = tcat;
                break;
            }
        }
        
        if(rlcat == null) return null;
        
        if(icat == null){
            return null;
            //llcat = CCGcat.lexCat(sent.getNode(lid-1).getConllNode().getWrd(), sent.getNode(lid-1).getConllNode().getSuperTag(), lid);
        }
        else{            
            rlcat = CCGcat.lexCat(sent.getNode(rlid-1).getWrdStr(), icat.catString(), rlid);
            //String rcatstr = lcat.isAtomic() ? lcat.toString()+"/"+rlcat.toString() : "("+lcat.toString()+")/"+rlcat.toString();
            String rlcatstr = (rlcat.isAtomic() ? rlcat.toString() :  "("+rlcat.toString()+")");
            String rcatstr = lcat.isAtomic() ? lcat.toString()+"/"+ rlcatstr : "("+lcat.toString()+")/" + rlcatstr;
            llcat = lcat.revealCat(lcat, rcatstr);
            //llcat = CCGcat.typeChangingRule(lcat, rcatstr);
        }
        
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        CCGJRuleInfo tinfo = ccgCombinators.checkCCGRules(rlcat, rcat);
        if(tinfo != null){
            depsMap = Commons.getDepsMap(rlcat, rcat, tinfo.getResultCat(), depsMap);
            CCGJRuleInfo info = ccgCombinators.checkCCGRules(llcat, tinfo.getResultCat());
            if(info != null){
                //depsMap = getDepsMap(llcat, tinfo.getResultCat(), info.getResultCat(), depsMap);
                Commons.updateDepTree(tinfo, rlid, rid, depGraph);
                for(int id : rcat.headIdList()){
                    Commons.updateDepTree(tinfo, rlid, id, depGraph);
                    Commons.updateRevealDeps(tinfo, rlid, id, rpid, depsMap, sysccgDeps, depGraph);
                }
                if(ftrue || checkWithGoldDeps(depsMap)){
                    //result = applyBinaryUpdate(left, right, info.getResultCat(), RuleType.reveal, true);
                    ArcJAction act = ArcJAction.make(SRAction.RREVEAL, level, (result==null) ? null : result.getCCGcat().toString(), rule);
                    result = Commons.applyBinaryUpdate(left, right, left.getCCGcat(), act, RuleType.rreveal, true);
                    updateccgNodeDeps(left, right, SRAction.RREVEAL, nccgNodeDeps, depsMap, isconj);                    
                    Commons.updateSysDeps(depsMap, sysccgDeps);
                    if(revealLevel.containsKey(level))
                        revealLevel.put(level, revealLevel.get(level)+1);
                    else
                        revealLevel.put(level, 1);
                    actionMap.put(SRAction.RREVEAL, actionMap.get(SRAction.RREVEAL)+1);
                    rule = RuleType.rreveal;
                    //if(level>5) level=5;
//                    CCGJRuleInfo ninfo = new CCGJRuleInfo(revcat, rcat, info.getRightCat(), true, rule, level, 0);
//                    treebankRules.addRevealRuleInfo(ninfo, revcat.toString()+" "+rcat.toString());
                    CCGJRuleInfo ninfo = new CCGJRuleInfo(lcat, rcat, result.getCCGcat(), true, rule, level, 0);
                    treebankRules.addRevealRuleInfo(ninfo, lcat.toString()+" "+rcat.toString());
                }
            }
        }
        //if(level>5) level=5;
        return new ImmutablePair(result, ArcJAction.make(SRAction.RREVEAL, level, (result==null) ? null : result.getCCGcat().toString(), rule));
    }
    
    @Override
    public void printResults(){
        super.printResults();
        /*        
        for (int i : revealLevel.values()) valCount += i;
        System.err.println("Reveal Levels: "+valCount);
        for(int i : revealLevel.keySet())
            System.err.print(i+"="+df.format(100.00*revealLevel.get(i)/valCount)+"  ");
        
        System.err.println("\n"+auxModalMap.size()+" "+auxModalMap);
        */
    }
    
    /*
    
    private CCGJTreeNode checkRightReveal(CCGJTreeNode left, CCGJTreeNode right, String rescatstr, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        CCGJTreeNode tlright = null, tlleft, result = null;
        CCGcat llcat, rlcat, lcat, rcat, icat = null, rescat;
        int lid, rlid, rid;
        boolean isconj = false;
        lcat = left.getCCGcat();
        rcat = right.getCCGcat();
        
        if(rcat.toString().endsWith("[conj]")){
            icat = CCGcat.ccgCatFromString(rcat.toString().replace("[conj]", ""));
            isconj = true;
        }
        else{
            while(rcat != null){
                CCGcat arg = rcat.argument();
                if((rcat.argDir()==CCGcat.BW)){
                    icat = arg.copy();
                    break;
                }
                rcat = rcat.result();
            }
        }
        if(icat == null) return null;
        
        rcat = right.getCCGcat();
        lid = left.getConllNode().getNodeId();
        rid = right.getConllNode().getNodeId();
        
        tlleft = left;
        int level = 0;
        
        CCGJTreeNode tlrighttmp;
        tlrighttmp = tlleft.getRightChild();
        while(tlrighttmp != null){
            if(canCompose(tlrighttmp.getCCGcat(), right.getCCGcat())){
                tlright = tlrighttmp;
            }
            tlrighttmp = tlrighttmp.getRightChild();
        }
        ///*
        while(tlleft != null){
            tlright = tlleft.getRightChild();
            if(tlright == null) return null;
            rlid = tlright.getConllNode().getNodeId();
            String key1 = rlid+"--"+rid, key2 = rid+"--"+rlid;
            tlleft = tlleft.getLeftChild();
            if(canCompose(tlright.getCCGcat(), right.getCCGcat())){
                level++;
                if(goldccgDeps.containsKey(key1) || goldccgDeps.containsKey(key2) || drvDeps.containsKey(key1) || drvDeps.containsKey(key2)){
                    rlcat = tlright.getCCGcat();
                    break;
                }
            }
        }
        
        if(tlright == null)
            return null;
        else{
            rlcat = tlright.getCCGcat();
            //rlcat = CCGcat.typeChangingRule(rlcat, icat.toString());
        }
        //if(level>=3) return null;
        
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        //llcat = tlleft.getCCGcat();
        lcat = left.getCCGcat();
        String rcatstr = lcat.isAtomic() ? lcat.toString()+"/"+rlcat.toString() : "("+lcat.toString()+")/"+rlcat.toString();
        //llcat = CCGcat.typeChangingRule(lcat, lcat.isAtomic() ? lcat.toString()+"/"+rlcat.toString() : "("+lcat.toString()+")/"+rlcat.toString());
        llcat = CCGcat.lexCat(left.getConllNode().getWrd(), rcatstr, left.getConllNode().getNodeId());
        llcat = tlleft.getCCGcat();
        CCGJRuleInfo tinfo = combinators.checkCCGRules(rlcat, right.getCCGcat());
        if(tinfo != null){
            depsMap = getDepsMap(rlcat, right.getCCGcat(), tinfo.getResultCat(), depsMap);
            CCGJRuleInfo info = combinators.checkCCGRules(llcat, tinfo.getResultCat());
            if(info != null){
                depsMap = getDepsMap(llcat, tinfo.getResultCat(), info.getResultCat(), depsMap);
                if(ftrue || checkWithGoldDeps(depsMap)){
                    result = applyBinaryUpdate(left, right, info.getResultCat(), RuleType.rreveal, true);
                    updateccgNodeDeps(left, right, SRAction.RREVEAL, nccgNodeDeps, depsMap, isconj);            
                    updateSysDeps(depsMap);
                    //if(info.getResultCat().matches(left.getCCGcat()))
                    //    result = applyBinaryUpdate(left, right, info.getResultCat(), RuleType.reveal, true);
                    //else
                    //    result = applyBinaryUpdate(left, right, left.getCCGcat(), RuleType.reveal, true);
                    if(revealLevel.containsKey(level))
                        revealLevel.put(level, revealLevel.get(level)+1);
                    else
                        revealLevel.put(level, 1);
                }
            }
        }
        return result;
    }
    
    private void fillUnResolveList(CCGJTreeNode left, CCGJTreeNode right, String value){
        String goldCombo="", curCombo;
        if(value != null){
            String[] gCatParts = value.split("--");
            goldCombo = gCatParts[1]+" -- "+gCatParts[2];
        }
        curCombo = left.getCCGcat().toString()+" -- "+right.getCCGcat().toString();
        HashMap<String, Integer> map;
        if(unResolveMap.containsKey(curCombo)) {
            map = unResolveMap.get(curCombo);
            map.put("total", map.get("total")+1);
            if(map.containsKey(goldCombo))
                map.put(goldCombo, map.get(goldCombo)+1);
            else
                map.put(goldCombo, 1);
        }
        else {
            map = new HashMap<>();
            map.put("total", 1);
            map.put(goldCombo, 1);
        }
        unResolveMap.put(curCombo, map);
        String key = curCombo + "  <--> " + goldCombo;
        if(unResolveMap2.containsKey(key))
            unResolveMap2.put(key, unResolveMap2.get(key)+1);
        else
            unResolveMap2.put(key, 1);
    }
    
    private void printUnresolveMap2(int nodeCount){
        DecimalFormat df = new DecimalFormat(".00");
        for (Iterator it = Utils.entriesSortedByValues(unResolveMap2).iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            System.err.println(key+"\t"+df.format(100.00*unResolveMap2.get(key)/nodeCount));
        }
    }
    
    private CCGJTreeNode checkVPSRule(CCGJTreeNode left, CCGJTreeNode right, String rescatstr, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        CCGJTreeNode lleft, result = null;
        int lid = left.getConllNode().getNodeId(), rid = right.getConllNode().getNodeId();
        CCGcat llcat, rlcat, lcat, rcat;
        String key1 = lid+"--"+rid, key2 = rid+"--"+lid;        
        boolean isconj = false;
        rcat = right.getCCGcat();
        if( (drvDeps.containsKey(key1) || drvDeps.containsKey(key2)) &&
            (rcat.matches("(S\\NP)\\(S\\NP)") || rcat.matches("(S\\NP)\\((S\\NP)/NP)")) ){
            lcat = left.getCCGcat().matches("S") ? left.getCCGcat() : CCGcat.ccgCatFromString("S");
            rcat = CCGcat.typeChangingRule(right.getCCGcat(), "(S\\NP)\\(S\\NP)");
            rlcat = CCGcat.lexCat(left.getConllNode().getWrd(), lcat.catString()+"\\NP", lid);
            
            Vertex vert = new Vertex(lid, left.getConllNode().getSuperTag());
            Vertex lvert = depGraph.getLeftMost(vert, "N|NP");
            
            if(lvert == null)
                llcat = CCGcat.ccgCatFromString("NP");
            else
                llcat = CCGcat.typeChangingRule(sent.getNodeAt(lvert.getIndex()-1).getCCGcat(), "NP");
            
            HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
            
            CCGJRuleInfo tinfo = combinators.checkCCGRules(rlcat, rcat);            
            if(tinfo != null){
                depsMap = getDepsMap(rlcat, rcat, tinfo.getResultCat(), depsMap);
                CCGJRuleInfo info = combinators.checkCCGRules(llcat, tinfo.getResultCat());
                if(info != null){
                //depsMap = getDepsMap(llcat, tinfo.getResultCat(), info.getResultCat(), depsMap);
                if(ftrue || checkWithGoldDeps(depsMap)){
                    updateDepTree(tinfo, left.getConllNode().getNodeId(), right.getConllNode().getNodeId());
                    if(isconj)
                        result = applyBinaryUpdate(left, right, info.getResultCat(), RuleType.reveal, true);
                    else
                        result = applyBinaryUpdate(left, right, left.getCCGcat(), RuleType.reveal, true);                    
                    updateccgNodeDeps(left, right, SRAction.LREVEAL, nccgNodeDeps, depsMap, false);
                    updateSysDeps(depsMap);
                    actionMap.put(SRAction.LREVEAL, actionMap.get(SRAction.LREVEAL)+1);
                }
            }
            
            }
        }    
        return result;
    }
    
    private CCGJTreeNode checkVPReveal(CCGJTreeNode left, CCGJTreeNode right, String rescatstr, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        CCGJTreeNode lleft, rleft = null, tleft, result = null;
        CCGcat llcat, rlcat, lcat, rcat, icat = null, rescat;
        int lid, rlid, rid;
        lcat = left.getCCGcat();
        rcat = right.getCCGcat();
        
        if(rcat.matches("(S\\NP)\\(S\\NP)") || (rcat.matches("S\\NP") && rcat.toString().endsWith("[conj]")))
            icat = CCGcat.ccgCatFromString("S\\NP");
        else return null;
        
        rcat = right.getCCGcat();
        lid = left.getConllNode().getNodeId();
        rid = right.getConllNode().getNodeId();
        
        ArrayList<Integer> list = ccgDepDeps.get(lid);
        if(list == null) return null;
        lleft = sent.getNodeAt(list.get(0)-1);
        llcat = CCGcat.typeChangingRule(lleft.getCCGcat(), "NP");
        rlcat = CCGcat.typeChangingRule(lcat, icat.toString());
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        
        CCGJRuleInfo tinfo = combinators.checkCCGRules(rlcat, right.getCCGcat());
        if(tinfo != null){
            depsMap = getDepsMap(rlcat, right.getCCGcat(), tinfo.getResultCat(), depsMap);
            CCGJRuleInfo info = combinators.checkCCGRules(llcat, tinfo.getResultCat());
            if(info != null){
                depsMap = getDepsMap(llcat, tinfo.getResultCat(), info.getResultCat(), depsMap);
                if(ftrue || checkWithGoldDeps(depsMap)){
                    result = applyBinaryUpdate(left, right, info.getResultCat(), RuleType.reveal, true);
                    updateccgNodeDeps(left, right, SRAction.LREVEAL, nccgNodeDeps, depsMap);                    
                    updateSysDeps(depsMap);
                }
            }
        }
        return result;
    }
    
    private CCGJTreeNode checkRightReveal(CCGJTreeNode left, CCGJTreeNode right, String rescatstr, HashMap<Integer, CCGNodeDepInfo> nccgNodeDeps) {
        CCGJTreeNode tlright = null, tlleft, result = null;
        CCGcat llcat, rlcat, lcat, rcat, icat = null, rescat;
        int lid, rlid, rid;
        lcat = left.getCCGcat();
        rcat = right.getCCGcat();
        
        if(rcat.toString().endsWith("[conj]"))
            icat = CCGcat.ccgCatFromString(rcat.toString().replace("[conj]", ""));
        else{
            while(rcat != null){
                CCGcat arg = rcat.argument();
                if((rcat.argDir()==CCGcat.BW)){
                    icat = arg.copy();
                    break;
                }
                rcat = rcat.result();
            }
        }
        if(icat == null) return null;
        
        rcat = right.getCCGcat();
        lid = left.getConllNode().getNodeId();
        rid = right.getConllNode().getNodeId();
        
        tlleft = left;
        int level = 0;
        
        CCGJTreeNode tlrighttmp;
        tlrighttmp = tlleft.getRightChild();
        while(tlrighttmp != null){
            if(canCompose(tlrighttmp.getCCGcat(), right.getCCGcat())){
                tlright = tlrighttmp;
            }
            tlrighttmp = tlrighttmp.getRightChild();
        }
        ///*
        while(tlleft != null){
            tlright = tlleft.getRightChild();
            if(tlright == null) return null;
            rlid = tlright.getConllNode().getNodeId();
            String key1 = rlid+"--"+rid, key2 = rid+"--"+rlid;
            tlleft = tlleft.getLeftChild();
            if(canCompose(tlright.getCCGcat(), right.getCCGcat())){
                level++;
                if(goldccgDeps.containsKey(key1) || goldccgDeps.containsKey(key2) || drvDeps.containsKey(key1) || drvDeps.containsKey(key2)){
                    rlcat = tlright.getCCGcat();
                    break;
                }
            }
        }
        
        if(tlright == null)
            return null;
        else{
            rlcat = tlright.getCCGcat();
            //rlcat = CCGcat.typeChangingRule(rlcat, icat.toString());
        }
        //if(level>=3) return null;
        
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        //llcat = tlleft.getCCGcat();
        lcat = left.getCCGcat();
        String rcatstr = lcat.isAtomic() ? lcat.toString()+"/"+rlcat.toString() : "("+lcat.toString()+")/"+rlcat.toString();
        //llcat = CCGcat.typeChangingRule(lcat, lcat.isAtomic() ? lcat.toString()+"/"+rlcat.toString() : "("+lcat.toString()+")/"+rlcat.toString());
        llcat = CCGcat.lexCat(left.getConllNode().getWrd(), rcatstr, left.getConllNode().getNodeId());
        llcat = tlleft.getCCGcat();
        CCGJRuleInfo tinfo = combinators.checkCCGRules(rlcat, right.getCCGcat());
        if(tinfo != null){
            depsMap = getDepsMap(rlcat, right.getCCGcat(), tinfo.getResultCat(), depsMap);
            CCGJRuleInfo info = combinators.checkCCGRules(llcat, tinfo.getResultCat());
            if(info != null){
                depsMap = getDepsMap(llcat, tinfo.getResultCat(), info.getResultCat(), depsMap);
                if(ftrue || checkWithGoldDeps(depsMap)){
                    result = applyBinaryUpdate(left, right, info.getResultCat(), RuleType.reveal, true);
                    updateccgNodeDeps(left, right, SRAction.RREVEAL, nccgNodeDeps, depsMap);                    
                    updateSysDeps(depsMap);
                    //if(info.getResultCat().matches(left.getCCGcat()))
                    //    result = applyBinaryUpdate(left, right, info.getResultCat(), RuleType.reveal, true);
                    //else
                    //    result = applyBinaryUpdate(left, right, left.getCCGcat(), RuleType.reveal, true);
                    if(revealLevel.containsKey(level))
                        revealLevel.put(level, revealLevel.get(level)+1);
                    else
                        revealLevel.put(level, 1);
                }
            }
        }
        return result;
    }
    
    private void updateGoldDepsVP2S(){
        HashMap<String, CCGDepInfo> depsMap = new HashMap<>();
        for(String key : goldccgDeps.keySet()){
            CCGDepInfo gdinfo = goldccgDeps.get(key);            
            CCGDepInfo ngdinfo = gdinfo;            
            String catstr = VP2S(CCGcat.ccgCatFromString(gdinfo.getCat())).toString();
            if(!gdinfo.getCat().equals(catstr)){
                ngdinfo = new CCGDepInfo(gdinfo.getHeadId(), gdinfo.getArgId(), gdinfo.getSlot()-1, catstr, gdinfo.getExtract(), gdinfo.getWaitTime());
            }
            depsMap.put(key, ngdinfo);            
        }
        goldccgDeps = depsMap;
    }
    
    private CCGcat VP2S(CCGcat nccgcat){
        String rcatstrtmp, rcatstr;
        rcatstrtmp = nccgcat.toString();
        rcatstrtmp = CCGcat.noFeatures(rcatstrtmp);
        //if(rcatstrtmp.startsWith("(S\\NP)\\(S\\NP)") || rcatstrtmp.startsWith("((S\\NP)\\(S\\NP))")){
        if(rcatstrtmp.contains("(S\\NP)\\(S\\NP)") ){
            rcatstr = rcatstrtmp.replace("(S\\NP)\\(S\\NP)", "S\\S");
            nccgcat = CCGcat.changeVPcat(nccgcat);
            nccgcat = CCGcat.ccgCatFromString(nccgcat.toString());
        }
        return nccgcat;
    }
    
    private CCGcat VP2S(CCGJTreeNode result){
        CCGcat nccgcat = result.getCCGcat();
        String rcatstrtmp, rcatstr;
        rcatstrtmp = result.getCCGcat().toString();
        rcatstrtmp = CCGcat.noFeatures(rcatstrtmp);
        //if(rcatstrtmp.startsWith("(S\\NP)\\(S\\NP)") || rcatstrtmp.startsWith("((S\\NP)\\(S\\NP))")){
        if(rcatstrtmp.contains("(S\\NP)\\(S\\NP)") ){
            rcatstr = rcatstrtmp.replace("(S\\NP)\\(S\\NP)", "S\\S");
            nccgcat = CCGcat.changeVPcat(result.getCCGcat());
            nccgcat = CCGcat.lexCat(result.getConllNode().getWrd(), nccgcat.toString(), result.getConllNode().getNodeId());            
        }
        return nccgcat;
    }
    
    private CCGJTreeNode VP2SUnary(CCGJTreeNode result){
        CCGJTreeNode nresult = result;
        //calculateWaitTimeParse(result.getConllNode().getNodeId());
        CCGcat nccgcat = VP2S(result);
        nresult = CCGJTreeNode.makeUnary(nccgcat, RuleType.lex, result);
        return nresult;
    }
    
    private void updateDepTree(CCGJTreeNode left, CCGJTreeNode right, SRAction act, HashMap<String, CCGDepInfo> depsMap){
        int pid, cid;
        String phcat, cat, hcat;
        CCGJTreeNode head, child;
        
        if(act == SRAction.RL){
            head = right;
            child = left;
        }
        else if(act == SRAction.RR){
            head = left;
            child = right;
        }
        else
            return;
        pid = head.getConllNode().getNodeId();
        phcat = head.getConllNode().getSuperTag();
        cid = child.getConllNode().getNodeId();
        cat = child.getCCGcat().toString();
        hcat = child.getConllNode().getSuperTag();
            
        DepInfo info = new DepInfo(cid, cat, hcat);
        depGraph.addChild(pid, phcat, info);
    }
    
    private boolean specialcase(CCGJTreeNode left, CCGJTreeNode right){
        boolean flag = false;
        if(left.getConllNode().getWrd().equals("Neither") && input.get(0).getCCGcat().toString().endsWith("[conj]"))
            flag = true;
        //if((right.getConllNode().getWrd().equals("-LRB-") && right.getCCGcat().toString().equals("(NP\\NP)/NP") )|| (input.size()>0 && input.get(0).getConllNode().getWrd().equals("-RRB-") && !left.getConllNode().getWrd().equals("-LRB-")) )
        //    flag = true;
        return flag;
    }
        */    
}