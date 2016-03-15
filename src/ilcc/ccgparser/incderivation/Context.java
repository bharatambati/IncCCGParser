/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.incderivation;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.SCoNLLNode;
import ilcc.ccgparser.utils.DataTypes.*;
import ilcc.ccgparser.utils.DepGraph;
import ilcc.ccgparser.utils.Feature;
import ilcc.ccgparser.utils.Feature.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 *
 * @author ambati
 */

public class Context {
   
   private final CCGJTreeNode s0, s1, s2, s3;
   private final CCGJTreeNode s0l, s0r, s0u, s0h;
   private final CCGJTreeNode s1l, s1r, s1u, s1h;
   private final CCGJTreeNode q0, q1, q2, q3;
   private final int s0ld, s0rd;
   private final int s1ld, s1rd;
   private final Word s0wrd, s1wrd, s2wrd, s3wrd, q0wrd, q1wrd, q2wrd, q3wrd;
   private final POS s0pos, s1pos, s2pos, s3pos, q0pos, q1pos, q2pos, q3pos;
   private final CCGCategory s0cat, s1cat, s2cat, s3cat;
   
   private final Integer rmost1, rmost2, rmost3, rmost4, rmost5;
   
   private final List<CCGJTreeNode> sentNodes;
   private final DepGraph depGraph;
   private final HashMap<Feature, Integer> featList;
      
   public Context(PStateItem state, List<CCGJTreeNode> list, List<Integer> rightPerList, boolean incalgo){
        PStateItem curState = state;
        sentNodes = list;        
        depGraph = state.getdepGraph();
        
        int stacksize = curState.stacksize();
        s0 = stacksize<1 ? null : curState.getNode();
        s1 = stacksize<2 ? null : curState.getStackPtr().getNode();
        s2 = stacksize<3 ? null : curState.getStackPtr().getStackPtr().getNode();
        s3 = stacksize<4 ? null : curState.getStackPtr().getStackPtr().getStackPtr().getNode();
        
        
        q0 = curState.getCurrentWrd() >= sentNodes.size() ? null : list.get(curState.getCurrentWrd());
        q1 = curState.getCurrentWrd()+1 >= sentNodes.size() ? null : list.get(curState.getCurrentWrd()+1);
        q2 = curState.getCurrentWrd()+2 >= sentNodes.size() ? null : list.get(curState.getCurrentWrd()+2);
        q3 = curState.getCurrentWrd()+3 >= sentNodes.size() ? null : list.get(curState.getCurrentWrd()+3);
        //q0 = list.size() > 0 ? list.get(0) : null;
        //q1 = list.size() > 1 ? list.get(1) : null;
        //q2 = list.size() > 2 ? list.get(2) : null;
        //q3 = list.size() > 3 ? list.get(3) : null;
        
        CCGJTreeNode[] s0nodes = new CCGJTreeNode[4];
        CCGJTreeNode[] s1nodes = new CCGJTreeNode[4];
        
        String[] nvars;
        
        if(s0 != null)
            s0nodes = getNodeVariable(s0);
        
        s0l = s0nodes[0]; s0r = s0nodes[1]; s0u = s0nodes[2]; s0h = s0nodes[3];
        
        if(s1 != null)
            s1nodes = getNodeVariable(s1);
        
        s1l = s1nodes[0]; s1r = s1nodes[1]; s1u = s1nodes[2]; s1h = s1nodes[3];
        
        List<Word> wlst = Arrays.asList(new Word[8]);
        List<POS> plst = Arrays.asList(new POS[8]);
        List<CCGCategory> clst = Arrays.asList(new CCGCategory[4]);
        getStackVariables(wlst, plst, clst);        
        getInputVariables(wlst, plst);
        s0wrd = wlst.get(0); s1wrd = wlst.get(1); s2wrd = wlst.get(2); s3wrd = wlst.get(3);
        s0pos = plst.get(0); s1pos = plst.get(1); s2pos = plst.get(2); s3pos = plst.get(3);
        s0cat = clst.get(0); s1cat = clst.get(1); s2cat = clst.get(2); s3cat = clst.get(3);
        q0wrd = wlst.get(4); q1wrd = wlst.get(5); q2wrd = wlst.get(6); q3wrd = wlst.get(7);
        q0pos = plst.get(4); q1pos = plst.get(5); q2pos = plst.get(6); q3pos = plst.get(7);
        
        s0ld = s0rd = s1ld = s1rd = -1;
        featList = new HashMap<>();
        int size = calculateCapacity();
    
        
        if(incalgo){
            List<Integer> vlist = getDepGraphFeats(rightPerList);
            rmost1 = vlist.get(0); rmost2 = vlist.get(1); rmost3 = vlist.get(2); rmost4 = vlist.get(3); rmost5 = vlist.get(4);
        }
        else{
            rmost1 = rmost2 = rmost3 = rmost4 = rmost5 = null;
        }
   }
   
   public Context(List<CCGJTreeNode> stack, List<CCGJTreeNode> list, CCGJSentence sent, List<Integer> rightPerList, DepGraph depgraph, boolean incalgo){
       
        sentNodes = sent.getNodes();
        depGraph = depgraph;
        //input = list;
        int stacksize = stack.size();
        s0 = stacksize<1 ? null : stack.get(stacksize-1);
        s1 = stacksize<2 ? null : stack.get(stacksize-2);
        s2 = stacksize<3 ? null : stack.get(stacksize-3);
        s3 = stacksize<4 ? null : stack.get(stacksize-4);
        
        q0 = list.size() > 0 ? list.get(0) : null;
        q1 = list.size() > 1 ? list.get(1) : null;
        q2 = list.size() > 2 ? list.get(2) : null;
        q3 = list.size() > 3 ? list.get(3) : null;
        
        CCGJTreeNode[] s0nodes = new CCGJTreeNode[4];
        CCGJTreeNode[] s1nodes = new CCGJTreeNode[4];
        
        String[] nvars;
        
        if(s0 != null)
            s0nodes = getNodeVariable(s0);
        
        s0l = s0nodes[0]; s0r = s0nodes[1]; s0u = s0nodes[2]; s0h = s0nodes[3];
        
        if(s1 != null)
            s1nodes = getNodeVariable(s1);
        
        s1l = s1nodes[0]; s1r = s1nodes[1]; s1u = s1nodes[2]; s1h = s1nodes[3];
        
        List<Word> wlst = Arrays.asList(new Word[8]);
        List<POS> plst = Arrays.asList(new POS[8]);
        List<CCGCategory> clst = Arrays.asList(new CCGCategory[4]);
        getStackVariables(wlst, plst, clst);        
        getInputVariables(wlst, plst);
        s0wrd = wlst.get(0); s1wrd = wlst.get(1); s2wrd = wlst.get(2); s3wrd = wlst.get(3);
        s0pos = plst.get(0); s1pos = plst.get(1); s2pos = plst.get(2); s3pos = plst.get(3);
        s0cat = clst.get(0); s1cat = clst.get(1); s2cat = clst.get(2); s3cat = clst.get(3);
        q0wrd = wlst.get(4); q1wrd = wlst.get(5); q2wrd = wlst.get(6); q3wrd = wlst.get(7);
        q0pos = plst.get(4); q1pos = plst.get(5); q2pos = plst.get(6); q3pos = plst.get(7);
        
        s0ld = s0rd = s1ld = s1rd = -1;
        featList = new HashMap<>();
        int size = calculateCapacity();
    
        if(incalgo){
            List<Integer> vlist = getDepGraphFeats(rightPerList);
            rmost1 = vlist.get(0); rmost2 = vlist.get(1); rmost3 = vlist.get(2); rmost4 = vlist.get(3); rmost5 = vlist.get(4);
        }
        else{
            rmost1 = rmost2 = rmost3 = rmost4 = rmost5 = null;
        }
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
   
   private void getStackVariables(List<Word> wordlist, List<POS> poslist, List<CCGCategory> catlist){
       if(s0!=null){
           SCoNLLNode cnode = s0.getConllNode();
           wordlist.set(0, cnode.getWrd());
           poslist.set(0, cnode.getPOS());
           catlist.set(0, CCGCategory.make(s0.getCCGcat().toString()) );
       }
       
       if(s1!=null){
           SCoNLLNode cnode = s1.getConllNode();
           wordlist.set(1, cnode.getWrd());
           poslist.set(1, cnode.getPOS());
           catlist.set(1, CCGCategory.make(s1.getCCGcat().toString()) );
       }
       
       if(s2!=null){
           SCoNLLNode cnode = s2.getConllNode();
           wordlist.set(2, cnode.getWrd());
           poslist.set(2, cnode.getPOS());
           catlist.set(2, CCGCategory.make(s2.getCCGcat().toString()) );
       }
       
       if(s3!=null){
           SCoNLLNode cnode = s3.getConllNode();
           wordlist.set(3, cnode.getWrd());
           poslist.set(3, cnode.getPOS());
           catlist.set(3, CCGCategory.make(s3.getCCGcat().toString()) );
       }
   }
   
   private void getInputVariables(List<Word> wordlist, List<POS> poslist){
       if(q0!=null){
           SCoNLLNode cnode = q0.getConllNode();
           wordlist.set(4, cnode.getWrd());
           poslist.set(4, cnode.getPOS());
       }
       
       if(q1!=null){
           SCoNLLNode cnode = q1.getConllNode();
           wordlist.set(5, cnode.getWrd());
           poslist.set(5, cnode.getPOS());
       }
       
       if(q2!=null){
           SCoNLLNode cnode = q2.getConllNode();
           wordlist.set(6, cnode.getWrd());
           poslist.set(6, cnode.getPOS());
       }
       
       if(q3!=null){
           SCoNLLNode cnode = q3.getConllNode();
           wordlist.set(7, cnode.getWrd());
           poslist.set(7, cnode.getPOS());
       }
   }
   
   
   private int calculateCapacity(){
       int size = 0;
       if(s3!=null) size+=16;
       else if(s2!=null) size+=14;
       else if(s1!=null) size+=12;
       else if(s0!=null) size+=6;
       
       if(q3!=null) size+=12;
       else if(q2!=null) size+=9;
       else if(q1!=null) size+=6;
       else if(q0!=null) size+=3;
       
       return size;       
   }
   
   private CCGJTreeNode[] getNodeVariable(CCGJTreeNode node){
       CCGJTreeNode[] nodes = new CCGJTreeNode[4];
       if(node.isUnary()){
           nodes[2] = node.getLeftChild();
           nodes[3] = nodes[2];
       }
       else if(node.isBinary()){           
           nodes[0] = node.getLeftChild();
           nodes[1] = node.getRightChild();
           if(node.getHeadDir()==0)
               nodes[3] = nodes[0];
           else
               nodes[3] = nodes[1];
       }
       return nodes;
   }
    
    public HashMap<Feature, Integer> getFeatureList(boolean incalgo, boolean lookAhead){
        // 1st set
        fillStackBase();
        
        //2nd Set
        fillInputBase(lookAhead);
        
        //3rd Set
        if(s0l!=null)
            fillStackChild(FeatPrefix.s0Lwc, FeatPrefix.s0Lpc, s0l.getConllNode().getNodeId(), s0l.getCCGcat());
        if(s0r!=null)
            fillStackChild(FeatPrefix.s0Rwc, FeatPrefix.s0Rpc, s0r.getConllNode().getNodeId(), s0r.getCCGcat());
        if(s0u!=null)
            fillStackChild(FeatPrefix.s0Uwc, FeatPrefix.s0Upc, s0u.getConllNode().getNodeId(), s0u.getCCGcat());
        
        if(s1l!=null)
            fillStackChild(FeatPrefix.s1Lwc, FeatPrefix.s1Lpc, s1l.getConllNode().getNodeId(), s1l.getCCGcat());
        if(s1r!=null)
            fillStackChild(FeatPrefix.s1Rwc, FeatPrefix.s1Rpc, s1r.getConllNode().getNodeId(), s1r.getCCGcat());
        if(s1u!=null)
            fillStackChild(FeatPrefix.s1Uwc, FeatPrefix.s1Upc, s1u.getConllNode().getNodeId(), s1u.getCCGcat());
        
        //4th Set
        fills0s1InputBigrams(lookAhead);
        
        //5th Set
        fillSimpleTrigrams(lookAhead);

        //6th Set
        fillTrigrams(lookAhead);
        
        //7th Set
        if(incalgo)
            fillGraphFeats();
        
        //return Collections.unmodifiableList(featList);
        return featList;
    }
    
    private void fillGraphFeats(){
        if(rmost1 != null){            
            CCGCategory cat = CCGCategory.make(depGraph.getVertex(rmost1).toString());            
            featList.put(Feature.make(FeatPrefix.l1c, Arrays.asList(cat)), 1);
            featList.put(Feature.make(FeatPrefix.l1cs0c, Arrays.asList(cat, s0cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l1cs0cs1c, Arrays.asList(sentNodes.get(rmost1).getPOS(), s0pos)), 1);
            
//            featList.put(Feature.make(FeatPrefix.l1wcs0cs1c, Arrays.asList(sentNodes.get(rmost1).getWrdStr(), s0wrd, s1wrd)), 1);
//            featList.put(Feature.make(FeatPrefix.l1ps0ps1p, Arrays.asList(sentNodes.get(rmost1).getPOS(), s0pos, s1pos)), 1);
//            featList.put(Feature.make(FeatPrefix.s0wcs1cs2c, Arrays.asList(s0wrd, s0cat, s1cat, s2cat)), 1);
//            featList.put(Feature.make(FeatPrefix.s0cs1wcs2c, Arrays.asList(s0cat, s1wrd, s1cat, s2cat)), 1);
//            featList.put(Feature.make(FeatPrefix.s0cs1cs2wc, Arrays.asList(s0cat, s1cat, s2wrd, s2cat)), 1);
//            featList.put(Feature.make(FeatPrefix.s0cs1cs2c, Arrays.asList(s0cat, s1cat, s2cat)), 1);
//            featList.put(Feature.make(FeatPrefix.s0ps1ps2p, Arrays.asList(s0pos, s1pos, s2pos)), 1);
//            featList.put(Feature.make(FeatPrefix.l1p, Arrays.asList(sentNodes.get(rmost1).getPOS())), 1);
//            featList.put(Feature.make(FeatPrefix.l1cs1c, Arrays.asList(cat, s1cat)), 1);
//            featList.put(Feature.make(FeatPrefix.l1cs0cs1c, Arrays.asList(cat, s0cat, s1cat)), 1);
        }
        if(rmost2 != null){
            CCGCategory cat = CCGCategory.make(depGraph.getVertex(rmost2).toString());
            featList.put(Feature.make(FeatPrefix.l2c, Arrays.asList(cat)), 1);
            featList.put(Feature.make(FeatPrefix.l2cs0c, Arrays.asList(cat, s0cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l2cs0cs1c, Arrays.asList(sentNodes.get(rmost2).getPOS(), s0pos)), 1);
            
            //featList.put(Feature.make(FeatPrefix.l2cs1c, Arrays.asList(cat, s1cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l2ps0ps1p, Arrays.asList(sentNodes.get(rmost2).getPOS(), s0pos, s1pos)), 1);
            //featList.put(Feature.make(FeatPrefix.l2wcs0cs1c, Arrays.asList(sentNodes.get(rmost2).getWrdStr(), s0wrd, s1wrd)), 1);
        }
        if(rmost3 != null){
            CCGCategory cat = CCGCategory.make(depGraph.getVertex(rmost3).toString());
            featList.put(Feature.make(FeatPrefix.l3c, Arrays.asList(cat)), 1);
            featList.put(Feature.make(FeatPrefix.l3cs0c, Arrays.asList(cat, s0cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l3cs0cs1c, Arrays.asList(sentNodes.get(rmost3).getPOS(), s0pos)), 1);
            
            //featList.put(Feature.make(FeatPrefix.l3cs1c, Arrays.asList(cat, s1cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l3ps0ps1p, Arrays.asList(sentNodes.get(rmost3).getPOS(), s0pos, s1pos)), 1);
            //featList.put(Feature.make(FeatPrefix.l3wcs0cs1c, Arrays.asList(sentNodes.get(rmost3).getWrdStr(), s0wrd, s1wrd)), 1);
        }
        
        if(rmost4 != null){
            CCGCategory cat = CCGCategory.make(depGraph.getVertex(rmost4).toString());
            //featList.put(Feature.make(FeatPrefix.l4c, Arrays.asList(cat)), 1);
            featList.put(Feature.make(FeatPrefix.l4cs0c, Arrays.asList(cat, s0cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l4cs0cs1c, Arrays.asList(cat, s0cat, s1cat)), 1);
            
            //featList.put(Feature.make(FeatPrefix.l3cs1c, Arrays.asList(cat, s1cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l4ps0ps1p, Arrays.asList(sentNodes.get(rmost4).getPOS(), s0pos, s1pos)), 1); 
            //featList.put(Feature.make(FeatPrefix.l4wcs0cs1c, Arrays.asList(sentNodes.get(rmost4).getWrdStr(), cat, s0cat, s1cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l4wcs0cs1c, Arrays.asList(sentNodes.get(rmost4).getWrdStr(), s0wrd, s1wrd)), 1);
        }
        
        if(rmost5 != null){
            CCGCategory cat = CCGCategory.make(depGraph.getVertex(rmost5).toString());
            //featList.put(Feature.make(FeatPrefix.l5c, Arrays.asList(cat)), 1);
            featList.put(Feature.make(FeatPrefix.l5cs0c, Arrays.asList(cat, s0cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l5cs0cs1c, Arrays.asList(cat, s0cat, s1cat)), 1);
            
            //featList.put(Feature.make(FeatPrefix.l3cs1c, Arrays.asList(cat, s1cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l5ps0ps1p, Arrays.asList(sentNodes.get(rmost5).getPOS(), s0pos, s1pos)), 1);
            //featList.put(Feature.make(FeatPrefix.l5wcs0cs1c, Arrays.asList(sentNodes.get(rmost5).getWrdStr(), cat, s0cat, s1cat)), 1);
            //featList.put(Feature.make(FeatPrefix.l5wcs0cs1c, Arrays.asList(sentNodes.get(rmost5).getWrdStr(), s0wrd, s1wrd)), 1);

        }
    }
    
    private void fillTrigrams(boolean lookAhead){
        if(s0!=null){
            if(s0l!=null){
                CCGCategory s0lcat = CCGCategory.make(s0l.getCCGcat().toString());
                if(s0h!=null){
                    CCGCategory s0hcat = CCGCategory.make(s0h.getCCGcat().toString());
                    featList.put(Feature.make(FeatPrefix.s0cs0hcs0lc, Arrays.asList(s0cat, s0hcat, s0lcat)), 1);
                }
                if(s1!=null){
                    featList.put(Feature.make(FeatPrefix.s0cs0lcs1c, Arrays.asList(s0cat, s0lcat, s1cat)), 1);
                    featList.put(Feature.make(FeatPrefix.s0cs0lcs1w, Arrays.asList(s0cat, s0lcat, s1wrd)), 1);
                }
            }
            if(s0r!=null){
                CCGCategory s0rcat = CCGCategory.make(s0r.getCCGcat().toString());
                if(s0h!=null){
                    CCGCategory s0hcat = CCGCategory.make(s0h.getCCGcat().toString());
                    featList.put(Feature.make(FeatPrefix.s0cs0hcs0rc, Arrays.asList(s0cat, s0rcat, s0hcat)), 1);
                }
                if(q0!=null){
                    featList.put(Feature.make(FeatPrefix.s0cs0rcq0p, Arrays.asList(s0cat, s0rcat, q0pos)), 1);
                    featList.put(Feature.make(FeatPrefix.s0cs0rcq0w, Arrays.asList(s0cat, s0rcat, q0wrd)), 1);
                }
            }            
        }
        if(s1r!=null){
            CCGCategory s1rcat = CCGCategory.make(s1r.getCCGcat().toString());
            CCGCategory s1hcat = CCGCategory.make(s1h.getCCGcat().toString());
            featList.put(Feature.make(FeatPrefix.s0cs1cs1rc, Arrays.asList(s0cat, s1cat, s1rcat)), 1);
            featList.put(Feature.make(FeatPrefix.s0ws1cs1rc, Arrays.asList(s0wrd, s1cat, s1rcat)), 1);
            featList.put(Feature.make(FeatPrefix.s1cs1hcs1rc, Arrays.asList(s1cat, s1hcat, s1rcat)), 1);
        }
    }
    
    private void fillSimpleTrigrams(boolean lookAhead){
        if(s0!=null && s1!=null && s2!=null){
            featList.put(Feature.make(FeatPrefix.s0wcs1cs2c, Arrays.asList(s0wrd, s0cat, s1cat, s2cat)), 1);
            featList.put(Feature.make(FeatPrefix.s0cs1wcs2c, Arrays.asList(s0cat, s1wrd, s1cat, s2cat)), 1);
            featList.put(Feature.make(FeatPrefix.s0cs1cs2wc, Arrays.asList(s0cat, s1cat, s2wrd, s2cat)), 1);
            featList.put(Feature.make(FeatPrefix.s0cs1cs2c, Arrays.asList(s0cat, s1cat, s2cat)), 1);
            featList.put(Feature.make(FeatPrefix.s0ps1ps2p, Arrays.asList(s0pos, s1pos, s2pos)), 1);
        }
        if(s0!=null && s1!=null && q0!=null){
            featList.put(Feature.make(FeatPrefix.s0wcs1cq0p, Arrays.asList(s0wrd, s0cat, s1cat, q0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0cs1wcq0p, Arrays.asList(s0cat, s1wrd, s1cat, q0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0cs1cq0wp, Arrays.asList(s0cat, s1cat, q0wrd, q0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0cs1cq0p, Arrays.asList(s0cat, s1cat, q0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0ps1pq0p, Arrays.asList(s0pos, s1pos, q0pos)), 1);
        }
        
        if(lookAhead){
            if(s0!=null && q0!=null && q1!=null){
                featList.put(Feature.make(FeatPrefix.s0wcq0pq1p, Arrays.asList(s0wrd, s0cat, q0pos, q1pos)), 1);
                featList.put(Feature.make(FeatPrefix.s0cq0wpq1p, Arrays.asList(s0cat, q0wrd, q0pos, q1pos)), 1);
                featList.put(Feature.make(FeatPrefix.s0cq0pq1wp, Arrays.asList(s0cat, q0pos, q1wrd, q1pos)), 1);
                featList.put(Feature.make(FeatPrefix.s0cq0pq1p, Arrays.asList(s0cat, q0pos, q1pos)), 1);
                featList.put(Feature.make(FeatPrefix.s0pq0pq1p, Arrays.asList(s0pos, q0pos, q1pos)), 1);
            }
        }
    }
    
    private void fills0s1InputBigrams(boolean lookAhead){
        
        if(s0!=null && s1!=null){
            //featList.put(Feature.make(FeatPrefix.s0wcs1wc, Arrays.asList(s0wrd, s0cat, s1wrd, s1cat)), 1);
            featList.put(Feature.make(FeatPrefix.s0cs1w, Arrays.asList(s0cat, s1wrd)), 1);
            featList.put(Feature.make(FeatPrefix.s0ws1c, Arrays.asList(s0wrd, s1cat)), 1);
            featList.put(Feature.make(FeatPrefix.s0cs1c, Arrays.asList(s0cat, s1cat)), 1);
        }
        
        if(s0!=null && q0!=null){
            //featList.put(Feature.make(FeatPrefix.s0wcq0wp, Arrays.asList(s0wrd, s0cat, q0wrd, q0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0cq0wp, Arrays.asList(s0cat, q0wrd, q0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0wcq0p, Arrays.asList(s0wrd, s0cat, q0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0cq0p, Arrays.asList(s0cat, q0pos)), 1);
        }
        
        if(s1!=null && q0!=null){
            //featList.put(Feature.make(FeatPrefix.s1wcq0wp, Arrays.asList(s1wrd, s1cat, q0wrd, q0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s1cq0wp, Arrays.asList(q0wrd, q0pos, s1cat)), 1);
            featList.put(Feature.make(FeatPrefix.s1wcq0p, Arrays.asList(s1wrd, q0pos, s1cat)), 1);
            featList.put(Feature.make(FeatPrefix.s1cq0p, Arrays.asList(q0pos, s1cat)), 1);
        }
    }
        
    private void fillStackBase(){
        
        if(s0!=null){
            //featList.put(Feature.make(FeatPrefix.s0w, Arrays.asList(s0wrd)), 1);
            //featList.put(Feature.make(FeatPrefix.s0p, Arrays.asList(s0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0wp, Arrays.asList(s0wrd, s0pos)), 1);
            featList.put(Feature.make(FeatPrefix.s0c, Arrays.asList(s0cat)), 1);
            featList.put(Feature.make(FeatPrefix.s0pc, Arrays.asList(s0pos, s0cat)), 1);
            featList.put(Feature.make(FeatPrefix.s0wc, Arrays.asList(s0wrd, s0cat)), 1);
            //featList.put(new Feature("s0wp-"+s0wrd, s0pos), 1); featList.put(new Feature("s0c-"+s0cat), 1); 
            //featList.put(new Feature("s0w-"+s0wrd), 1); featList.put(new Feature("s0p-"+s0pos), 1); 
            //featList.put(new Feature("s0pc-"+s0pos, s0cat), 1); featList.put(new Feature("s0wc-"+s0wrd, s0cat), 1);
        }
        if(s1!=null){
            featList.put(Feature.make(FeatPrefix.s1wp, Arrays.asList(s1wrd, s1pos)), 1);
            featList.put(Feature.make(FeatPrefix.s1c, Arrays.asList(s1cat)), 1);
            featList.put(Feature.make(FeatPrefix.s1pc, Arrays.asList(s1pos, s1cat)), 1);
            featList.put(Feature.make(FeatPrefix.s1wc, Arrays.asList(s1wrd, s1cat)), 1);
        }
        if(s2!=null){            
            featList.put(Feature.make(FeatPrefix.s2pc, Arrays.asList(s2pos, s2cat)), 1);
            featList.put(Feature.make(FeatPrefix.s2wc, Arrays.asList(s2wrd, s2cat)), 1);
        }        
        if(s3!=null){            
            featList.put(Feature.make(FeatPrefix.s3pc, Arrays.asList(s3pos, s3cat)), 1);
            featList.put(Feature.make(FeatPrefix.s3wc, Arrays.asList(s3wrd, s3cat)), 1);
        }
    }
    
    private void fillInputBase(boolean lookAhead){
        
        if(q0!=null){
            featList.put(Feature.make(FeatPrefix.q0wp, Arrays.asList(q0wrd, q0pos)), 1);
        }
        if(lookAhead){
            if(q1!=null){
                featList.put(Feature.make(FeatPrefix.q1wp, Arrays.asList(q1wrd, q1pos)), 1);
            }
            if(q2!=null){
                featList.put(Feature.make(FeatPrefix.q2wp, Arrays.asList(q2wrd, q2pos)), 1);
            }
            if(q3!=null){
                featList.put(Feature.make(FeatPrefix.q3wp, Arrays.asList(q3wrd, q3pos)), 1);
            }
        }
    }
    
    private void fillStackChild(FeatPrefix pre1, FeatPrefix pre2, int id, CCGcat ccgCat){
        
        SCoNLLNode cnode = sentNodes.get(id-1).getConllNode();
        Word wrd = cnode.getWrd();
        POS pos = cnode.getPOS();
        CCGCategory cat = CCGCategory.make(ccgCat.toString());
        
        featList.put(Feature.make(pre1, Arrays.asList(wrd, cat)), 1);
        featList.put(Feature.make(pre2, Arrays.asList(pos, cat)), 1);
    }
}
