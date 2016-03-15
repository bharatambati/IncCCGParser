/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.incderivation;

import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.*;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author ambati
 */
public class PStateItem {
    
    private final CCGJTreeNode node;
    private final PStateItem statePtr;
    private final PStateItem stackPtr;
    private final int currentWord;
    private final ArcJAction action;
    private final double score;
    private final int id;
    //private final CCGDepList depList;
    HashMap<String, CCGDepInfo> sysccgDeps;
    //private final ImmutableMap<Integer, DepTree> depTreeMap;
    private final DepGraph depGraph;
    
    public PStateItem(){
        node = null;
        statePtr = null;
        stackPtr = null;
        currentWord = 0;
        action = null;
        score = 0.0;
        id = 0;
        //depList = null;
        sysccgDeps = new HashMap<>();
        depGraph = new DepGraph(25);
    }
    
    public PStateItem(int nid, CCGJTreeNode nNode, PStateItem nStatePtr, PStateItem nStackPtr, int nCurrentWrd, ArcJAction nAction, HashMap<String, CCGDepInfo> nsysccgDeps, double nScore, DepGraph ndepGraph){
        node = (nNode== null) ? nNode : nNode.copy();
        statePtr = nStatePtr;
        stackPtr = nStackPtr;
        currentWord = nCurrentWrd;
        action = nAction;
        score = nScore;
        sysccgDeps = nsysccgDeps;
        //depList = nDepList;
        id = nid;
        depGraph = ndepGraph;
    }
    
    public int getCurrentWrd(){
        return currentWord;
    }
   
    public PStateItem getStatePtr(){
        return statePtr;
    }    
   
    public PStateItem getStackPtr(){
        return stackPtr;
    }
       
    public CCGJTreeNode getNode(){
        return node;
    }
    
    public ArcJAction getArcAction(){
        return action;
   }
    
    public double getScore(){
        return score;
    }
    
    public int getId(){
        return id;
    }
    
    public HashMap<String, CCGDepInfo> getSysDeps(){
        return sysccgDeps;
    }
    
    public DepGraph getdepGraph(){
        return depGraph;
    }
    
    public int stacksize(){
        int size = 0;
        PStateItem current = this;
        while (current.node != null) {
            //if (current.node.valid()) 
            size++;// no node -> start/fini
            current = current.stackPtr;
        }
        return size;
   }
    
    public boolean isFinish(int size){        
        return currentWord == size && stacksize()==1;
    }
    
    public PStateItem copy() {
        return new PStateItem(id, node==null ? node : node.copy(), statePtr, stackPtr, currentWord, action, copyccgdeps(), score, depGraph==null ? depGraph : depGraph.copy());
    }
    
    public HashMap<String, CCGDepInfo> copyccgdeps(){
        return (HashMap<String, CCGDepInfo>) sysccgDeps.clone();
    }
    
    public PStateItem applyAction(ArcJAction act, List<CCGJTreeNode> input, boolean incalgo, double val){
        if(act.getAction() == SRAction.SHIFT)
            return applyShift(act, input, val, incalgo);
        else if(act.getAction() == SRAction.RL || act.getAction() == SRAction.RR || act.getAction() == SRAction.RU)
            return applyReduce(act, val, incalgo);
        else if(act.getAction() == SRAction.LREVEAL)
            return applyLReveal(act, input, val);
        else if(act.getAction() == SRAction.RREVEAL)
            return applyRReveal(act, input, val);
        return null;
    }
    
    public PStateItem applyShift(ArcJAction act, List<CCGJTreeNode> input, double val, boolean incalgo){        
        CCGJTreeNode inode = input.get(currentWord);
        CCGJTreeNode result = Commons.applyShift(inode, act);
        depGraph.addVertex(result.getNodeId(), result.getHeadcat());
        DepGraph nDepGraph = (incalgo) ? depGraph.copy(): depGraph;
        
        PStateItem retval = new PStateItem(id+1, result, this, this, currentWord+1, act, copyccgdeps(), val, nDepGraph);
        return retval;
    }
    
    public PStateItem applyReduce(ArcJAction act, double val, boolean incalgo) {
        
        CCGJTreeNode left = null, right, result;
        PStateItem retval;
        
        if(act.getAction() == SRAction.RU){
            right = node;
            result = Commons.applyReduce(left, right, act, sysccgDeps, depGraph);
            DepGraph nDepGraph = (incalgo) ? depGraph.copy(): depGraph;
            retval = new PStateItem(id+1, result, this, stackPtr, currentWord, act, copyccgdeps(), val, nDepGraph);
        }
        else {
            right = node;
            left = stackPtr.node;
            result = Commons.applyReduce(left, right, act, sysccgDeps, depGraph);
            DepGraph nDepGraph = (incalgo) ? depGraph.copy(): depGraph;
            retval = new PStateItem(id+1, result, this, stackPtr.stackPtr, currentWord, act, copyccgdeps(), val, nDepGraph);
        }
        return retval;
    }
    
    protected PStateItem applyLReveal(ArcJAction act, List<CCGJTreeNode> input, double val) {
        CCGJTreeNode left, right, result;
        left = stackPtr.node; right = node;
        result = Commons.applyLReveal(left, right, act, input, sysccgDeps, depGraph, false, null);
        PStateItem retval = new PStateItem(id+1, result, this, stackPtr.stackPtr, currentWord, act, copyccgdeps(), val, depGraph.copy());
        return retval;
    }
    
    protected PStateItem applyRReveal(ArcJAction act, List<CCGJTreeNode> input, double val) {
        CCGJTreeNode left, right, result;        
        left = stackPtr.node; right = node;
        result = Commons.applyRReveal(left, right, act, input, sysccgDeps, depGraph, false, null);        
        PStateItem retval = new PStateItem(id+1, result, this, stackPtr.stackPtr, currentWord, act, copyccgdeps(), val, depGraph.copy());
        return retval;
    }
    
    protected PStateItem applyFrag(ArcJAction act){
        CCGJTreeNode left, right, result;
        right = node;
        left = stackPtr.node;
        result = Commons.applyReduce(left, right, act, sysccgDeps, depGraph);
        return new PStateItem(id+1, result, this, stackPtr.stackPtr, currentWord, act, copyccgdeps(), 0.0, depGraph);
    }
    
    public void postOrder(CCGJTreeNode root, ArrayList list){
        if(root == null)
            return;
        postOrder(root.getLeftChild(), list);
        postOrder(root.getRightChild(), list);
        list.add(root);
    }
    
    public void writeDeriv(int id, BufferedWriter odWriter) throws IOException{
        CCGJTreeNode root = node;
        ArrayList<CCGJTreeNode> list = new ArrayList<>();
        postOrder(root, list);
        Stack<String> sStack = new Stack<>();
        for(CCGJTreeNode cNode : list) {
            if(cNode.isLeaf()){
                StringBuilder sb = new StringBuilder();
                String wrd, pos, cat;
                wrd = cNode.getWrdStr();
                pos = cNode.getPOS().toString();
                cat = cNode.getCCGcat().toString();
                sb.append("(<L ");
                sb.append(cat);sb.append(" ");
                sb.append(pos);sb.append(" ");
                sb.append(pos);sb.append(" ");
                sb.append(wrd);sb.append(" ");
                sb.append(cat);sb.append(">)");
                sStack.push(sb.toString());
            }
            else if(cNode.getChildCount()==1){
                
                StringBuilder sb = new StringBuilder();
                String cat;
                cat = cNode.getCCGcat().toString();
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
                String cat = cNode.getCCGcat().toString();
                String dir = (cNode.getHeadDir()==1)? "1" : "0";
                sb.append("(<T ");
                sb.append(cat);
                sb.append(" ");
                sb.append(cNode.getArcAction().getRuleType());
                if(cNode.getArcAction().getRuleType().equals(RuleType.rreveal))
                    sb.append(cNode.getArcAction().getLevel());
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
    
    /*
    private ImmutableMap<Integer, DepTree> addEdge(int pid, int cid, ImmutableMap<Integer, DepTree> deptreemap){
            
        Map<Integer, DepTree> map = new HashMap<>(); map.putAll(deptreemap);
        DepTree ptree = deptreemap.get(pid).copy();
        DepTree ctree = deptreemap.get(cid).copy();
        ptree.addChild(ctree);
        map.put(pid, ptree);
        ImmutableMap<Integer, DepTree> nDepTreeMap = new ImmutableMap.Builder<Integer, DepTree>().putAll(map).build();
        
        return nDepTreeMap;
    }
    
    
    public PStateItem applyAction(SRParser srparser, ArcJAction act, boolean isTrain, double val) throws Exception{
        CCGJTreeNode result = srparser.applyAction(act);
        if(result == null)
            return null;
        
        if(act.getAction() == SRAction.SHIFT)
            return shift(result, act, val);
        else 
            return reduce(result, action, val);
    }
    
    private PStateItem reduce(CCGJTreeNode result, ArcJAction act, double val) {

       boolean single_child = false, head_left = true, temporary = false;
       CCGCategory cat = action.getccgCat();
       SRAction sract = act.getAction();
        String headDir;
        if(sract == SRAction.RU)
            single_child = true;
        else if(sract == SRAction.RL)
            head_left = false;        
        
        PStateItem retval;
        CCGJTreeNode l, r;
        String rescatstr = cat.toString();
        
        if (single_child) {
            
            assert(head_left == false);
            assert(temporary == false);
            l = node;
            PStateNode sNode = new PStateNode(node.getNodeId()+1, PStateNode.NODE_TYPE.SINGLE_CHILD, result, l, new PStateNode(), l.getHeadId());
            retval = new PStateItem(id+1, sNode, this, stackPtr, currentWord, action, val);
        }
        else {
            
            r = node;
            l = stackPtr.node;
            
            //assert(stacksize()>=2);
            
            PStateNode sNode = new PStateNode(node.getNodeId()+1, (head_left?PStateNode.NODE_TYPE.HEAD_LEFT:PStateNode.NODE_TYPE.HEAD_RIGHT), result, l, r, (head_left?l.getHeadId():r.getHeadId()));
            retval = new PStateItem(id+1, sNode, this, stackPtr.stackPtr, currentWord, action, val);
        }
        return retval;
   }
    
   
   /*
   private void getCCGNode(PStateItem root){
       ArrayList<CCGJTreeNode> list = new ArrayList<>();
       list.add(root.node);
       while(!list.isEmpty()){
           CCGJTreeNode cnode = list.get(0);
           NODE_TYPE act = cnode.getNodeType();
           CCGJTreeNode left;
           CCGJTreeNode right;
           if(act == PStateNode.NODE_TYPE.HEAD_LEFT){
               left = cnode.getLeftChild();
               right = cnode.getRightChild();
               list.add(left);
               list.add(right);
           }
           if(act == PStateNode.NODE_TYPE.HEAD_RIGHT){               
               left = cnode.getLeftChild();
               right = cnode.getRightChild();
               list.add(left);
               list.add(right);
           }
           if(act == PStateNode.NODE_TYPE.SINGLE_CHILD)
               list.add(cnode.getLeftChild());
           list.remove(0);
       }
   }
   */
}