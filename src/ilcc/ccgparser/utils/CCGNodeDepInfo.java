/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import java.util.HashMap;

/**
 *
 * @author ambati
 */
public class CCGNodeDepInfo {
    
    private HashMap<Integer, CCGDepInfo> leftDepArg;
    private HashMap<Integer, CCGDepInfo> leftDepAdj;
    private HashMap<Integer, CCGDepInfo> rightDepArg;
    private HashMap<Integer, CCGDepInfo> rightDepAdj;
    private HashMap<Integer, CCGDepInfo> resolvedDeps;
    
    public CCGNodeDepInfo(){
        leftDepArg = new HashMap<>();
        leftDepAdj = new HashMap<>();
        rightDepArg = new HashMap<>();
        rightDepAdj = new HashMap<>();
        resolvedDeps = new HashMap<>();        
    }
    
    public int size(){
        return leftDepArg.size()+leftDepAdj.size()+rightDepArg.size()+rightDepAdj.size();
    }
    
    public int ldepsize(){
        return leftDepArg.size()+leftDepAdj.size();
    }
    
    public int rdepsize(){
        return rightDepArg.size()+rightDepAdj.size();
    }
    
    public int RDAdjSize(){
        return rightDepAdj.size();
    }
    
    public HashMap<Integer, CCGDepInfo> getLeftArg(){
        return leftDepArg;
    }
        
    public HashMap<Integer, CCGDepInfo> getRightArg(){
        return rightDepArg;
    }
    
    public HashMap<Integer, CCGDepInfo> getLeftAdj(){
        return leftDepAdj;
    }
        
    public HashMap<Integer, CCGDepInfo> getRightAdj(){
        return rightDepAdj;
    }
    
    public void addtoDepInfo(int id, CCGDepInfo depinfo){
        int hid = depinfo.getHeadId(), aid = depinfo.getArgId();
        if(hid == id){
            if(aid < hid)
                leftDepArg.put(aid, depinfo);
            else
                rightDepArg.put(aid, depinfo);
        }
        else{
            if(aid < hid)
                rightDepAdj.put(hid, depinfo);
            else
                leftDepAdj.put(hid, depinfo);
        }
    }
    
    public void remove(int hid, int aid){
        if(aid < hid){
            leftDepArg.remove(aid);
            leftDepAdj.remove(aid);
        }
        else{
            rightDepArg.remove(aid);
            rightDepAdj.remove(aid);
        }
    }
    
    public void addtoLDArg(int id, CCGDepInfo depinfo){
        leftDepArg.put(id, depinfo);
    }
    
    public void addtoLDAdj(int id, CCGDepInfo depinfo){
        leftDepAdj.put(id, depinfo);
    }    
    
    public void addtoRDArg(int id, CCGDepInfo depinfo){
        rightDepArg.put(id, depinfo);
    }
    
    public void addtoRDAdj(int id, CCGDepInfo depinfo){
        rightDepAdj.put(id, depinfo);
    }
    
    public void addtoRDArg(HashMap<Integer, CCGDepInfo> rDepArg){
        for(int id : rDepArg.keySet()){
            rightDepArg.put(id, rDepArg.get(id));
        }
    }
    
    public void setRDAdj(HashMap<Integer, CCGDepInfo> rDepArg){
        rightDepAdj = rDepArg;
    }
    
    public void updateLDArg(int id){
        CCGDepInfo depinfo = leftDepArg.get(id);
        leftDepArg.remove(id);
        resolvedDeps.put(id, depinfo);
    }
    
    public void updateLDAdj(int id){
        CCGDepInfo depinfo = leftDepAdj.get(id);
        leftDepAdj.remove(id);
        resolvedDeps.put(id, depinfo);
    }    
    
    public void updateRDArg(int id){
        CCGDepInfo depinfo = rightDepArg.get(id);
        rightDepArg.remove(id);
        resolvedDeps.put(id, depinfo);
    }
    
    public void updateRDAdj(int id){
        CCGDepInfo depinfo = rightDepAdj.get(id);
        rightDepAdj.remove(id);
        resolvedDeps.put(id, depinfo);
    }
    
    public void copyCCGNodeDepInfo(CCGNodeDepInfo info, int lspan, int rspan){
        leftDepArg = copyCCGNodeDepInfo(info.leftDepArg, lspan, rspan);
        leftDepAdj = copyCCGNodeDepInfo(info.leftDepAdj, lspan, rspan);
        rightDepArg = copyCCGNodeDepInfo(info.rightDepArg, lspan, rspan);
        rightDepAdj = copyCCGNodeDepInfo(info.rightDepAdj, lspan, rspan);
    }
    
    public HashMap<Integer, CCGDepInfo> copyCCGNodeDepInfo(HashMap<Integer, CCGDepInfo> deps, int lspan, int rspan){
        HashMap<Integer, CCGDepInfo> ndeps = new HashMap<>();
        for(int id : deps.keySet()){
            if(id>= lspan && id <=rspan)
                ndeps.put(id, deps.get(id));
        }
        return ndeps;
    }
    
}
