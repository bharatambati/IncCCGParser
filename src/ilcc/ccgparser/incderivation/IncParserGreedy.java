/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.incderivation;

import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.DataTypes.*;
import ilcc.ccgparser.utils.Feature;
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
public class IncParserGreedy extends IncParser {
    
    List<SRParser> agenda;
    
    public IncParserGreedy(Properties props) throws IOException{
        super.init(props);        
        agenda = new ArrayList<>(beamSize);
    }
        
    public void fillVars(String autoFile, String conllFile, String tPargFile, String tConllFile) {
        trainAutoFile = autoFile;
        trainCoNLLFile = conllFile;
        testPargFile = tPargFile;
        testCoNLLFile = tConllFile;
    }
    
    @Override
    public void train() throws Exception{
        System.err.println("Iteration: ");
        double bestlf = 0.0, lf;
        for(int i = 1; i <= iters; i++){
            System.err.print(i+": ");
            for(int sentid = 1 ; sentid < goldDetails.size()+1; sentid++){
                sSize++;
                if(sentid%100 == 0)
                    System.err.print(" "+sentid);
                if(sentid%1000 == 0)
                    System.err.println();
                if(debug > 0)
                    System.err.println(sentid);
                
                try{
                    srparser.init();
                    parse(goldDetails.get(sentid).getccgSent(), sentid);
                }
                catch(Exception ex){
                    System.err.println("\n"+sentid+"\t"+ex);
                }
            }
            System.err.println();
            System.err.println("Parsing after iter: "+i);
            model.updateFinalWeights(sSize);
            lf = parse();
            if(lf >= bestlf){
                saveModel(modelFile);
                bestlf = lf;
            }
        }
        //model.updateFinalWeights(sSize);
    }
    
    public void parse(CCGJSentence sent, int sentId) throws Exception {
        
        srparser.initVars(sent);
        double srscore = 0.0;
        ArrayList<HashMap<Feature, Integer>> featMapArray = new ArrayList<>(beamSize);
        for(int i = 0 ; i<beamSize; i++)
            featMapArray.add(new HashMap<>());
        
        int index = -1;
        List<ArcJAction> gActList = null;
        
        if(isTrain){
            gActList = goldDetails.get(sentId).getarcActs();
            if(gActList == null)
                return;
        }
        agenda.clear();
        agenda.add(srparser);
        boolean flag = true;
        while(flag){
            ArcJAction parserAct;
            ArcJAction goldAct = null;
            boolean corItem = false;
            HashMap<Feature, Integer> featMap;
            
            if(isTrain)
                goldAct = gActList.get(index+1);
            
            List<TempAgenda> tempAgenda = new ArrayList<>();
            int itemId = 0;
            SRParser item  = srparser;
            ArrayList<Integer> rightPerList = null;
            int stacksize = item.stack.size();
            if(incalgo && stacksize > 1){
                CCGJTreeNode left = item.stack.get(stacksize-2);
                Integer lvertex = left.getConllNode().getNodeId();
                rightPerList = item.depGraph.getRightPer(lvertex);
            }
            Context context = new Context(item.stack, item.input, sent, rightPerList, item.depGraph, incalgo);
            HashMap<Feature, Integer> fMap, sentFMap, newFMap;
            fMap = context.getFeatureList(incalgo, lookAhead);
            sentFMap  = featMapArray.get(itemId);
            newFMap = fMap;
            ArrayList<ArcJAction> acts = getAction(item);
            if(acts.isEmpty())
                flag = false;
            for(ArcJAction action : acts){
                double score = srscore + model.fv.getScore(newFMap, action, isTrain);
                TempAgenda tmp = new TempAgenda(null, action, newFMap, sentFMap, score);
                addToList(tempAgenda, tmp);
                if(debug >= 2)
                    System.err.print(action+" "+score+" ");
            }
            List<TempAgenda> newAgendaList = bestAgendaList(tempAgenda);
            
            if(newAgendaList.isEmpty()){
                flag = false;
                continue;
            }
            
            int i=0;
            TempAgenda newAgenda = newAgendaList.get(i);
            ArcJAction action = newAgenda.getAction();
            double score = newAgenda.getScore();
            srscore = score;
            
            SRParser nItem = item;
            parserAct = action;
            featMap = newAgenda.getFeatureList();
            
            if(isTrain){
                if(parserAct.equals(goldAct))
                    corItem = true;
            }
            index++;
            if(isTrain){
                nItem.applyAction(goldAct);
                
                if(debug >= 2)
                    System.err.println("\n"+corItem+" Gold Action: "+goldAct+" -- Parser Action: "+parserAct);
                if(corItem == false) {
                    updateScores(featMap, goldAct, 1);
                    updateScores(featMap, parserAct, -1);
                    if(early_update)
                        flag = false;
                }
            }
            else{
                nItem.applyAction(parserAct);
                srparser.actionMap.put(parserAct.getAction(), srparser.actionMap.get(parserAct.getAction())+1);
                if(debug >= 1)
                    System.err.println("\n"+parserAct);
            }
            if(srparser.input.isEmpty() && srparser.stack.size()==1)
                flag = false;
        }
    }
    
    private void updateScores(HashMap<Feature,Integer> featMap, ArcJAction act, int update){
        model.updateWeights(featMap, act, update, sSize);
    }
    
    @Override
    public double parse() throws IOException, Exception{
        
        BufferedReader derivReader = new BufferedReader(new FileReader(new File(testPargFile)));
        BufferedReader conllReader = new BufferedReader(new FileReader(new File(testCoNLLFile)));
        BufferedWriter oPargWriter = new BufferedWriter(new FileWriter(new File(outPargFile)));
        BufferedWriter oDerivWriter = new BufferedWriter(new FileWriter(new File(outAutoFile)));
        oPargWriter.write("# Generated using \n# IncParserGreedy.java\n\n");
        oDerivWriter.write("# Generated using \n# IncParserGreedy.java\n\n");
        
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
        int id = 0;
        for(CCGJSentence sent: sentences){
            id++;
            parse(sent, id);
            
            srparser.writeDeriv(id, oDerivWriter);
            srparser.writeDeps(oPargWriter);
            
            if(!gcdepsMap.isEmpty()){
                CCGJSentence gsent = gcdepsMap.get(id);
                srparser.goldccgDeps = gsent.getPargDeps();
                srparser.evaluateParseDependenciesJulia();
                srparser.totCat += gsent.getNodes().size();
                srparser.updateCatAccuray(gsent);
            }
            
            if(id%100 == 0)
                System.err.print(" "+id);
        }
        isTrain = true;
        System.err.println();
        srparser.sentCount = sentences.size();
        srparser.printResults();
        oDerivWriter.close();
        oPargWriter.close();
        return srparser.LF;
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
            ArrayList<Integer> rightPeriList = srparser.depGraph.getRightPer(lvertex);
            for(int i : rightPeriList)
                rightPerList.add(srparser.depGraph.getVertex(i));
        }
        if(stacksize>0)
            right = state.stack.get(stacksize-1);
        actions = state.treebankRules.getActions(left, right, inode, rightPerList);
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
}