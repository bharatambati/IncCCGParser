/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.incderivation;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGJRuleInfo;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author ambati
 */
public class NonInc extends SRParser {
    
    public NonInc() throws IOException {
        incalgo = false;
    }
    
    @Override
    public List<ArcJAction> parse(CCGJSentence sent) throws Exception {
        return parseGold(sent);
    }
    
    @Override
    public CCGJTreeNode shift() {
        return null;
    }
        
    protected ArrayList<ArcJAction> parseGold(CCGJSentence sent) throws Exception{
        CCGJTreeNode root = sent.getDerivRoot();
        ArrayList<ArcJAction> gActs = new ArrayList<>();
        ArrayList<CCGJTreeNode> list = new ArrayList<>();
        postOrder(root, list);
        for(CCGJTreeNode node : list) {
            if(node.isLeaf()){
                gActs.add(ArcJAction.make(SRAction.SHIFT, 0, node.getCCGcat().toString(), RuleType.lexicon));
            }
            else if(node.getChildCount()==1){                
                CCGJTreeNode left = node.getLeftChild();
                CCGcat lcat = left.getCCGcat();
                String rescatstr = node.getCCGcat().toString();
                CCGcat rescat = CCGcat.typeRaiseTo(lcat, rescatstr);
                RuleType rtype = RuleType.lex;
                if (rescat == null)
                    rescat = CCGcat.typeChangingRule(lcat, rescatstr);
                else
                    rtype = RuleType.tr;                
                CCGJRuleInfo info = new CCGJRuleInfo(lcat, null, rescat, true, rtype, 0, -1);
                treebankRules.addUnaryRuleInfo(info, lcat.toString());
                if(lcat.toString().equals(info.getResultCat().toString()))
                    System.err.println(sentCount+" Recursive unary rule "+lcat.toString()+" "+info.getResultCat().toString());
                
                gActs.add(ArcJAction.make(SRAction.RU, 0, node.getCCGcat().toString(), rtype));
                
            }
            else {                
                CCGJTreeNode left = node.getLeftChild(), right = node.getRightChild();
                CCGcat lcat = left.getCCGcat(), rcat = right.getCCGcat(), rescat;
                String rescatstr = node.getCCGcat().toString();
                RuleType rtype = findCombinator(lcat, rcat, rescatstr);
                rescat = CCGcat.combine(lcat, rcat, rescatstr);
                CCGJRuleInfo info = new CCGJRuleInfo(lcat, rcat, rescat, (node.getHeadDir()==1), rtype, 0, 0);
                treebankRules.addBinaryRuleInfo(info, lcat.toString()+" "+rcat.toString());
                
                if(node.getHeadDir()==1)
                    gActs.add(ArcJAction.make(SRAction.RL, 0, node.getCCGcat().toString(), rtype));
                else
                    gActs.add(ArcJAction.make(SRAction.RR, 0, node.getCCGcat().toString(), rtype));
            }
        }
        shiftReduceGold(sent, gActs);
        return gActs;
    }
    
    public void shiftReduceGold(CCGJSentence sent, ArrayList<ArcJAction> gActs) {
        input = sent.getNodes();
        stack = new Stack<>();
        for(int i = 0; i < gActs.size(); i++){
            ArcJAction action = gActs.get(i);
            applyActionGold(action, false, 0);
        }
        if(stack.size()>1)
            System.err.println("More than one node in the stack");
    }
    
    public CCGJTreeNode applyActionGold(ArcJAction action, boolean isTrain, double val){
        String sract = action.getAction().toString();
        if(sract.equals("SHIFT"))
            return shiftGold(action, val);
        else
            return reduceGold(action, val);
    }
    
    public CCGJTreeNode shiftGold(ArcJAction action, double val) {
        
        CCGJTreeNode result = input.get(0);
        stack.push(result);
        input.remove(0);
        return result;
    }
    
    public CCGJTreeNode reduceGold(ArcJAction action, double val) {
        
        boolean single_child = false, head_left = false;
        CCGCategory cat = action.getccgCat();
        SRAction sract = action.getAction();
        if(sract == SRAction.RU)
            single_child = true;
        else if(sract == SRAction.RR)
            head_left = true;
        
        CCGJTreeNode left, right, result;
        String rescatstr = cat.toString();
        
        if (single_child) {
            left = stack.pop();
            result = Commons.applyUnary(left, rescatstr, action, sysccgDeps);
            stack.push(result);
        }
        else {
            right = stack.pop();
            left = stack.pop();
            result = applyBinary(left, right, rescatstr, head_left, sract, action);
            stack.push(result);
        }
        return result;
    }
}