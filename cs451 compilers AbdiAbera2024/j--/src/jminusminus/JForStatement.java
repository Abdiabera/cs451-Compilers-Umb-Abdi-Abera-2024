// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas
////
package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.GOTO;

/**
 * The AST node for a for-statement.
 */
class JForStatement extends JStatement {
    // Initialization.
    private final ArrayList<JStatement> init;

    // Test expression
    private JExpression condition;

    // Update.
    private final ArrayList<JStatement> update;

    // The body.
    private JStatement body;

    // Support for break statement
    public boolean hasBreak = false;
    public String breakLabel;

    // Support for continue statement
    public boolean hasContinue;
    public String continueLabel;

    /**
     * Constructs an AST node for a for-statement.
     *
     * @param line      line in which the for-statement occurs in the source file.
     * @param init      the initialization.
     * @param condition the test expression.
     * @param update    the update.
     * @param body      the body.
     */
    public JForStatement(int line, ArrayList<JStatement> init, JExpression condition,
                         ArrayList<JStatement> update, JStatement body) {
        super(line);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    public JForStatement analyze(Context context) {
        // TODO
        LocalContext FPContext = new LocalContext(context);
        JMember.enclosingStatement.push(this);
        if (init != null)
            init.replaceAll(jStatement -> (JStatement) jStatement.analyze(FPContext));
        if (condition != null) {
            condition = condition.analyze(FPContext);
            condition.type().mustMatchExpected(line(), Type.BOOLEAN);
        }
        if (update != null)
            update.replaceAll(jStatement -> (JStatement) jStatement.analyze(FPContext));

        if (body != null) {
            body.analyze(FPContext);
        }
        JMember.enclosingStatement.pop();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        // TODO
        String startFor = output.createLabel();
        String endFor = output.createLabel();
        breakLabel = output.createLabel();
        continueLabel = output.createLabel();
        // Generate code for init statements
        if (init != null) {
            for (JStatement statement : init) {
                statement.codegen(output);
            }
        }
        // Start of the loop
        output.addLabel(startFor);
        // Generate code for the condition and jump to the end of the loop if it's false
        if (condition != null)
            condition.codegen(output, endFor, false);
        output.addLabel(continueLabel);


        body.codegen(output);

        if (hasContinue) {
            output.addLabel(continueLabel);
        }
        // Generate code for update statements
        if (update != null) {
            for (JStatement statement : update) {
                statement.codegen(output);
            }
        }
        output.addBranchInstruction(GOTO, startFor);

        if (hasBreak)
            output.addLabel(breakLabel);


        // End of the loop
        output.addLabel(endFor);
    }


    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JForStatement:" + line, e);
        if (init != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Init", e1);
            for (JStatement stmt : init) {
                stmt.toJSON(e1);
            }
        }
        if (condition != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Condition", e1);
            condition.toJSON(e1);
        }
        if (update != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Update", e1);
            for (JStatement stmt : update) {
                stmt.toJSON(e1);
            }
        }
        if (body != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Body", e1);
            body.toJSON(e1);
        }
    }
}
