// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;
import java.util.TreeMap;

import static jminusminus.CLConstants.LOOKUPSWITCH;
import static jminusminus.CLConstants.TABLESWITCH;

/**
 * The AST node for a switch-statement.
 */
public class JSwitchStatement extends JStatement {
    // Test expression.
    private JExpression condition;

    // List of switch-statement groups.
    private final ArrayList<SwitchStatementGroup> stmtGroup;

    private int lo;

    private int hi;
    private int nLabels;

    // Added for break statement support.
    public boolean hasBreak;
    public String breakLabel;


    /**
     * Constructs an AST node for a switch-statement.
     *
     * @param line      line in which the switch-statement occurs in the source file.
     * @param condition test expression.
     * @param stmtGroup list of statement groups.
     */
    public JSwitchStatement(int line, JExpression condition,
                            ArrayList<SwitchStatementGroup> stmtGroup) {
        super(line);
        this.condition = condition;
        this.stmtGroup = stmtGroup;
    }

    /**
     * {@inheritDoc}
     * this to analzye the switch statement
     */
    public JStatement analyze(Context context) {
        //for switchstatemnt within provided context
        condition = condition.analyze(context);
        condition.type().mustMatchExpected(line(), Type.INT);
        //intiailize and store
        ArrayList<JExpression> switchLabels = new ArrayList<>();
        nLabels = 0;
        //then iterate switchstatment group
        for (SwitchStatementGroup group : stmtGroup) {
            LocalContext switchContext = new LocalContext(context);
            for (int i = 0; i < group.getSwitchLabels().size(); i++) {
                JExpression label = group.getSwitchLabels().get(i);
                //this is to check if label is not null
                if (label != null) {
                    nLabels++;
                    //now analyze label in switch contect
                    group.getSwitchLabels().set(i, label.analyze(switchContext));
                    label.type().mustMatchExpected(line(), Type.INT);
                    switchLabels.add(label);
                }
            }
            //now iterate each statment in a gorup
            for (int i = 0; i < group.getStatements().size(); i++) {
                JStatement statement = group.getStatements().get(i);
                JMember.enclosingStatement.push(this); // Push the enclosing statement context for every statement.
                group.getStatements().set(i, (JStatement) statement.analyze(switchContext));
                JMember.enclosingStatement.pop(); // Pop the enclosing statement context for every statement.
            }
        }
        // then find findLowestAndHighest switchstatments
        findLowestAndHighest(switchLabels);
        return this;
    }

    /**
     * Finds the lowest and highest label in the entire switch
     */
    private void findLowestAndHighest(ArrayList<JExpression> switchLabels) {
        //initializing lo and hi
        lo = hi = ((JLiteralInt) switchLabels.get(0)).toInt();
        //now iterate through each switch label
        for (JExpression switchLabel : switchLabels) {
            int current = ((JLiteralInt) switchLabel).toInt();
            if (hi < current) {
                hi = current;
            }
            //then update lo if lo > current
            if (lo > current) {
                lo = current;
            }
        }
    }


    /**
     * Finds the correct operation code for the switch.
     *///this determines correct opertion for switch
    private int findCorrectOperation() {
        //this is to calculate space codt for table switch
        long tableSpaceCost = 5 + ((long) hi - lo + 1);
        long tableTimeCost = 3;
        long lookupSpaceCost = 3 + 2 * (long) nLabels;
        long lookupTimeCost = nLabels;
        //choose TABLESWITCH or  LOOKUPSWITCH to analysis
        return (nLabels > 0 && tableSpaceCost + 3 * tableTimeCost <= lookupSpaceCost + 3 * lookupTimeCost) ? TABLESWITCH : LOOKUPSWITCH;
    }

    // this is used to generate cod for switch
    private boolean codegenTableSwitch(CLEmitter output, String defaultLabel) {
        boolean containsDefault = false;
        ArrayList<String> labels = new ArrayList<>();
        //iterate for each group of switch label in the group
        for (SwitchStatementGroup group : stmtGroup) {
            ArrayList<JExpression> switchLabels = group.getSwitchLabels();
            for (JExpression switchLabel : switchLabels) {
                //this is to check switchLabel not null
                if (switchLabel != null) {
                    int literal = ((JLiteralInt) switchLabel).toInt();
                    labels.add("TableSwitchCase" + literal);
                }
            }
        }
        // this place to addTABLESWITCHInstruction to output
        output.addTABLESWITCHInstruction(defaultLabel, lo, hi, labels);
        int labelCounter = 0;
        for (SwitchStatementGroup group : stmtGroup) {
            ArrayList<JExpression> switchLabels = group.getSwitchLabels();
            ArrayList<JStatement> statements = group.getStatements();
            //iterate
            for (JExpression switchLabel : switchLabels) {
                output.addLabel(switchLabel != null ? labels.get(labelCounter++) : defaultLabel);
                if (switchLabel == null)
                    containsDefault = true;
            }
            for (JStatement statement : statements) {
                // now generate code for the statment
                statement.codegen(output);
            }
        }
        //return containsDefault
        return containsDefault;
    }

    // this  is to generate code LookupSwitch
    private boolean codgenLookupSwitch(CLEmitter output, String defaultLabel) {
        boolean containsDefault = false;
        TreeMap<Integer, String> matchLabelPairs = new TreeMap<>();
        // iterate each switch group
        for (SwitchStatementGroup group : stmtGroup) {
            ArrayList<JExpression> switchLabels = group.getSwitchLabels();
            // iterate each switch label
            for (JExpression switchLabel : switchLabels) {
                if (switchLabel != null) {
                    int literal = ((JLiteralInt) switchLabel).toInt();
                    matchLabelPairs.put(literal, "LookUpCase" + literal);
                }
            }
        }
        // then to addLOOKUPSWITCHInstruction
        output.addLOOKUPSWITCHInstruction(defaultLabel, matchLabelPairs.size(), matchLabelPairs);
        for (SwitchStatementGroup group : stmtGroup) {
            ArrayList<JExpression> switchLabels = group.getSwitchLabels();
            for (JExpression switchLabel : switchLabels) {
                if (switchLabel != null) {
                    int literal = ((JLiteralInt) switchLabel).toInt();
                    output.addLabel(matchLabelPairs.get(literal));
                } else {
                    output.addLabel(defaultLabel);
                    containsDefault = true;
                }
            }
            //
            for (JStatement statement : group.getStatements()) {
                statement.codegen(output);
            }
        }
        return containsDefault;
    }

    /**
     * {@inheritDoc}
     * code generate
     */
    public void codegen(CLEmitter output) {
        String defaultLabel = output.createLabel();
        breakLabel = output.createLabel();
        condition.codegen(output);
        int opCode = findCorrectOperation();
        boolean containsDefault = false;
        if (opCode == TABLESWITCH) {
            containsDefault = codegenTableSwitch(output, defaultLabel);
        } else if (opCode == LOOKUPSWITCH) {
            containsDefault = codgenLookupSwitch(output, defaultLabel);
        }

        if (!containsDefault) {
            output.addLabel(defaultLabel);
        }
        // Set the breakLabel and add it at the appropriate place if hasBreak is true.
        if (hasBreak) {
            output.addLabel(breakLabel);
        }


    }


    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JSwitchStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Condition", e1);
        condition.toJSON(e1);
        for (SwitchStatementGroup group : stmtGroup) {
            group.toJSON(e);
        }
    }


}

/**
 * A switch statement group consists of case labels and a block of statements.
 */
class SwitchStatementGroup {
    // Case labels.
    private final ArrayList<JExpression> switchLabels;

    // Block of statements.
    private final ArrayList<JStatement> block;

    /**
     * Constructs a switch-statement group.
     *
     * @param switchLabels case labels.
     * @param block        block of statements.
     */
    public SwitchStatementGroup(ArrayList<JExpression> switchLabels, ArrayList<JStatement> block) {
        this.switchLabels = switchLabels;
        this.block = block;
    }


    /**
     * Returns a list of switch labels
     *
     * @return switchLabels
     */
    public ArrayList<JExpression> getSwitchLabels() {
        return switchLabels;
    }

    /**
     * Returns a list of statements
     *
     * @return statements
     */
    public ArrayList<JStatement> getStatements() {
        return block;
    }


    /**
     * Stores information about this switch statement group in JSON format.
     *
     * @param json the JSON emitter.
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("SwitchStatementGroup", e);
        for (JExpression label : switchLabels) {
            JSONElement e1 = new JSONElement();
            if (label != null) {
                e.addChild("Case", e1);
                label.toJSON(e1);
            } else {
                e.addChild("Default", e1);
            }
        }
        if (block != null) {
            for (JStatement stmt : block) {
                stmt.toJSON(e);
            }
        }
    }
}
