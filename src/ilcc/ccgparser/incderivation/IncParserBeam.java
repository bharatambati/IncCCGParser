/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.incderivation;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.*;
import ilcc.ccgparser.utils.DataTypes.*;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author ambati
 */
public class IncParserBeam extends IncParser {
    
    ccgCombinators combinators;
    List<CCGJTreeNode> input;
    List<PStateItem> agenda;
    
    public IncParserBeam(Properties props) throws IOException{
        super.init(props);
        agenda = new ArrayList<>(beamSize);
        Commons.setIfBeam(true);
    }
        
    @Override
    public void train() throws Exception{
        System.err.println("Iteration: ");
        double bestlf = 0.0, lf;
        
        if(new File(modelFile).exists())
            loadModel();
        for(int i = 1; i <= iters; i++){
            System.err.print(i+": ");
            for(int sentid = 1 ; sentid < goldDetails.size()+1; sentid++){
                sSize++;
                if(sentid%100 == 0)
                    System.err.print(" "+sentid);
                if(sentid%1000 == 0)
                    System.err.println();
                //else
                //    System.err.print(" "+sentid);
                
                if(debug >= 1)
                    System.err.println(sentid);
                //parse(goldDetails.get(sentid).getccgSent(), sentid);
                if(sentid == 10046 || sentid == 578)
                    continue;
                try{                    
                    parse(goldDetails.get(sentid).getccgSent(), sentid);
                }
                catch(Exception ex){
                    System.err.println("\n"+sentid+"\t"+ex);
                }
            }
            System.err.println();
            System.err.println("Parsing after iter: "+i);
            model.updateFinalWeights(sSize);
            saveModel(modelFile+"."+i);
            /*
            lf = parse();
            if(lf >= bestlf){
                saveModel(modelFile);
                bestlf = lf;
            }*/
        }
        //model.updateFinalWeights(sSize);
    }
    
    public PStateItem parse(CCGJSentence sent, int sentId) throws Exception {
        
        input = sent.getNodes();
        ArrayList<HashMap<Feature, Integer>> featMapArray = new ArrayList<>(beamSize);
        for(int i = 0 ; i<beamSize; i++)
            featMapArray.add(new HashMap<>());
        
        agenda.clear();
        PStateItem start = new PStateItem();
        agenda.add(start);
        
        PStateItem candidateOutput = null, output = null, correctState = start;
        
        int index = -1, goldId = 0;
        List<ArcJAction> gActList = null;
        HashMap<Feature, Integer> gfMap;
        
        if(isTrain){
            gActList = goldDetails.get(sentId).getarcActs();
            if(gActList == null) return null;
        }
        
        int thres = 1000 * sent.getLength();
        while(!agenda.isEmpty() && index < thres){
            List<PStateItem> list = new ArrayList<>(beamSize);
            ArcJAction parserAct = null, goldAct = null;
            double bestScore = 0.0;
            boolean corItem = false;
            ArrayList<ArcJAction> acts = null;
            
            List<TempAgenda> tempAgenda = new ArrayList<>();
            for(int itemId = 0; itemId < agenda.size(); itemId++){
                PStateItem item = agenda.get(itemId);
                
                List<Integer> rightPerList = null;
                int stacksize = item.stacksize();
                if(incalgo && stacksize > 1){
                    CCGJTreeNode left = item.getStackPtr().getNode();
                    Integer lvertex = left.getConllNode().getNodeId();
                    rightPerList = item.getdepGraph().getRightPer(lvertex);
                }
                Context context = new Context(item, input, rightPerList, incalgo);
                
                HashMap<Feature, Integer> fMap, sentFMap, newFMap;
                fMap  = context.getFeatureList(incalgo, lookAhead);
                sentFMap  = featMapArray.get(itemId);
                newFMap = fMap;
                acts = getAction(item);
                for(ArcJAction action : acts){
                    double score = item.getScore() + model.fv.getScore(newFMap, action, isTrain);
                    TempAgenda tmp = new TempAgenda(item, action, newFMap, sentFMap, score);
                    addToList(tempAgenda, tmp);
                    if(debug >= 2)
                        System.err.print(action+" "+score+" ");
                }
            }

            if(isTrain)
                goldAct = gActList.get(index+1);
            
            List<TempAgenda> newAgendaList = bestAgendaList(tempAgenda);
            
            //acts.clear();
            for(int i = 0; i< newAgendaList.size(); i++){
                TempAgenda newAgenda = newAgendaList.get(i);
                PStateItem item = newAgenda.getState();
                ArcJAction action = newAgenda.getAction();
                double score = newAgenda.getScore();
                
                PStateItem nItem = item.copy();                
                if(nItem == null) continue;
                
                nItem = nItem.applyAction(action, input, incalgo, score);
                
                if(nItem.isFinish(input.size())){
                    if(candidateOutput == null || nItem.getScore() > candidateOutput.getScore())
                        candidateOutput = nItem;
                }
                else{
                    list.add(nItem);
                }
                
                if(i == 0){
                    parserAct = action;
                    output = nItem;
                    bestScore = score;
                }
                
                if(isTrain){
                    if(item.getId() == goldId && action.equals(goldAct)){
                        goldId = nItem.getId();
                        corItem = true;
                    }
                }
                //acts.add(action);
            }
            index++;
            
            if(isTrain){
                if(parserAct == null)
                    return candidateOutput;
                
                if(debug >= 2)
                    System.err.println("\n"+corItem+" Gold Action: "+goldAct+" -- Parser Action: "+parserAct);
                
                List<Integer> rightPerList = null;
                int stacksize = correctState.stacksize();
                if(incalgo && stacksize > 1){
                    CCGJTreeNode left = correctState.getStackPtr().getNode();
                    Integer lvertex = left.getConllNode().getNodeId();
                    rightPerList = correctState.getdepGraph().getRightPer(lvertex);
                }
                
                Context context = new Context(correctState, input, rightPerList, incalgo);
                //Context context = new Context(correctState, input, null);
                gfMap  = context.getFeatureList(incalgo, lookAhead);
                double score = correctState.getScore() + model.fv.getScore(gfMap, goldAct, isTrain);
                correctState = correctState.applyAction(goldAct, input, incalgo, score);
                if(corItem == false) {
                    //updateScores(gfMap, goldAct, 1);
                    //updateScores(gfMap, output.getArcAction(), -1);
                    updateScores(correctState, 1);
                    updateScores(output, -1);
                    if(early_update){
                        agenda.clear();
                        return candidateOutput;
                    }
                    else{
                        agenda.clear();
                        if(correctState.isFinish(input.size()))
                            return candidateOutput;
                        agenda.add(correctState);
                        goldId = correctState.getId();
                    }
                }
                else{                    
                    agenda.clear();
                    agenda.addAll(list);
                }
                
                if(index==gActList.size()-1)
                    return candidateOutput;
            }
            else{
                agenda.clear();
                agenda.addAll(list);
                if(debug >= 1){
                    //System.err.println(parserAct+" "+acts);
                    if(parserAct.getAction().equals(SRAction.SHIFT))
                        System.err.print("\n"+output.getNode().getHeadWrd());
                    StringBuilder sb = new StringBuilder();
                    sb.append(" ");sb.append(parserAct.getAction());
                    sb.append(" ");sb.append(parserAct.getccgCat());
                    sb.append(" ");sb.append(bestScore);
                    System.err.print(sb.toString());
                }
            }
        }
        
        if(isTrain && candidateOutput.getId() != goldId){
            //Context context = new Context(correctState, input, null);
            //gfMap  = context.getFeatureList();
            //updateScores(gfMap, correctState.getArcAction(), 1);
            //updateScores(gfMap, candidateOutput.getArcAction(), 1);
            updateScores(correctState, 1);
            updateScores(candidateOutput, -1);
        }
        
        if(index >= thres)
            timeOut(sentId, index, sent);
        
        if(candidateOutput == null)
            return output;
        else
            return candidateOutput;
    }
    
    private void updateScores(PStateItem root, int update){
        PStateItem cur = root;
        PStateItem prevState = cur.getStatePtr();
        while(prevState != null){
            ArcJAction act = cur.getArcAction();
            Context context = new Context(prevState, input, null, incalgo);
            HashMap<Feature,Integer> featMap  = context.getFeatureList(incalgo, lookAhead);
            model.updateWeights(featMap, act, update, sSize);                
            cur = prevState;
            prevState = cur.getStatePtr();
        }
    }
    
    private void updateScores(HashMap<Feature,Integer> featMap, ArcJAction act, int update){
        
        //SortedSet<Feature> keys = new TreeSet<>(featMap.keySet());
        //System.err.println(keys);
        model.updateWeights(featMap, act, update, sSize);
    }
    
    @Override
    public double parse() throws IOException, Exception{        
        BufferedReader derivReader = new BufferedReader(new FileReader(new File(testPargFile)));
        BufferedReader conllReader = new BufferedReader(new FileReader(new File(testCoNLLFile)));
        BufferedWriter oPargWriter = new BufferedWriter(new FileWriter(new File(outPargFile)));
        BufferedWriter oDerivWriter = new BufferedWriter(new FileWriter(new File(outAutoFile)));
        oPargWriter.write("# Generated using \n# IncParserGreedy.java\n\n");
        
        HashMap<Integer, CCGJSentence> gcdepsMap = getccgDepMap(derivReader);
        
        ArrayList<CCGJSentence> sentences = new ArrayList<>();
        System.err.println("Processing: ");
        srparser.init();
        
        String cLine;
        ArrayList<String> cLines = new ArrayList<>();
        while ((cLine = conllReader.readLine()) != null) {
            if(cLine.equals("")){
                CCGJSentence sent = new CCGJSentence();
                sent.fillCoNLL(cLines);
                sentences.add(sent);
                cLines.clear();
            }
            cLines.add(cLine);
        }
        System.err.println();
        
        isTrain = false;
        //beamSize = 16;
        int id = 0;
        for(CCGJSentence sent: sentences){
            id++;
            if(id%100 == 0)
                System.err.print(" "+id);
            if(id%1000 == 0)
                System.err.println();
            //else
            //    System.err.print(" "+id);
            
            //PStateItem output = parse(sent, id);
            try{
                srparser.sent = sent;
                if(testCoNLLFile.contains("devel.") && (id == 1839 || id == 1843 || id == 1844 || id == 1848))
                    srparser.sysccgDeps = new HashMap<>();
                else{
                    PStateItem output = parse(sent, id);
                    srparser.sysccgDeps = output.sysccgDeps;
                    srparser.depGraph = output.getdepGraph();
                    if(output.stacksize() == 1)
                        srparser.parsedSents++;
                    srparser.sentCount = sentences.size();
                    //srparser.updateSysDeps(output.getSysDeps());
                    output = mergeFrags(output);
                    output.writeDeriv(id, oDerivWriter);
                    srparser.writeDeps(oPargWriter);
                }
                if(!gcdepsMap.isEmpty()){
                    CCGJSentence gsent = gcdepsMap.get(id);
                    srparser.goldccgDeps = gsent.getPargDeps();
                    srparser.evaluateParseDependenciesJulia();
                    srparser.totCat += gsent.getNodes().size();
                    srparser.updateCatAccuray(gsent);
                }
            }
            catch(Exception ex){
                System.err.println("\n"+id+"\t"+ex);
            }
        }
        isTrain = true;
        //beamSize = 1;
        System.err.println();
        srparser.printResults();
        oDerivWriter.close();
        oPargWriter.close();
        return srparser.LF;
    }
    
    private PStateItem mergeFrags(PStateItem output){
        int stacksize = output.stacksize();
        while(stacksize!=1){
            CCGcat rescat = CCGcat.ccgCatFromString("X");
            RuleType ruletype = RuleType.frag;
            ArcJAction act = ArcJAction.make(SRAction.REDUCE, 0, rescat.toString(), ruletype);
            output = output.applyFrag(act);
            stacksize = output.stacksize();
        }
        return output;
    }
    
    private ArrayList<ArcJAction> getAction(PStateItem state){
        ArrayList<ArcJAction> actions;
        CCGJTreeNode left, right, inode;
        left = right = inode = null;
        ArrayList<CCGCategory> rightPerList = new ArrayList<>();
        int stacksize = state.stacksize();
        if(state.getCurrentWrd() < input.size())
            inode = input.get(state.getCurrentWrd());
        if(stacksize > 1){
            left = state.getStackPtr().getNode();
            if(incalgo){
                int lid = left.getNodeId();
                ArrayList<Integer> rightPeriList = state.getdepGraph().getRightPer(lid);
                for(int i : rightPeriList)
                    rightPerList.add(state.getdepGraph().getVertex(i));
            }
        }        
        if(stacksize>=1)
            right = state.getNode();
        
        actions = srparser.treebankRules.getActions(left, right, inode, rightPerList);
        return actions;
    }
    
    private void addToList(List<TempAgenda> list, TempAgenda item){
        int i = 0;
        while(i < list.size()){
            if (item.getScore() > list.get(i).getScore())
                break;
            i++;
        }
        list.add(i, item);
    }
    
    private List<TempAgenda> bestAgendaList(List<TempAgenda> list){
        List<TempAgenda> nList = new ArrayList<>();
        for(int i = 0; i< beamSize && i<list.size(); i++){
            nList.add(list.get(i));
        }
        return nList;
    }
    
    private void timeOut(int sentId, int id, CCGJSentence sent){
        System.err.println("Timeout while parsing "+sentId+" of length "+sent.getLength()+" after #iterations: "+id);
        PStateItem state = agenda.get(0);
    }
}
