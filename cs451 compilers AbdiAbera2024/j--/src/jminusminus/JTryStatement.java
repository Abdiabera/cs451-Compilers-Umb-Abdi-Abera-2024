// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas
///////
package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a try-catch-finally statement.
 */
class JTryStatement extends JStatement {
    // The try block.
    private JBlock tryBlock;

    // The catch parameters.
    private ArrayList<JFormalParameter> parameters;

    // The catch blocks.
    private ArrayList<JBlock> catchBlocks;

    // The finally block.
    private JBlock finallyBlock;

    /**
     * Constructs an AST node for a try-statement.
     *
     * @param line         line in which the while-statement occurs in the source file.
     * @param tryBlock     the try block.
     * @param parameters   the catch parameters.
     * @param catchBlocks  the catch blocks.
     * @param finallyBlock the finally block.
     */
    public JTryStatement(int line, JBlock tryBlock, ArrayList<JFormalParameter> parameters,
                         ArrayList<JBlock> catchBlocks, JBlock finallyBlock) {
        super(line);
        this.tryBlock = tryBlock;
        this.parameters = parameters;
        this.catchBlocks = catchBlocks;
        this.finallyBlock = finallyBlock;
    }

    /**
     * {@inheritDoc}
     */
    public JTryStatement analyze(Context context) {
        // TODO
        LocalContext tryContext = new LocalContext(context);
        tryBlock = (JBlock) tryBlock.analyze(tryContext);
        for (int i = 0; i < parameters.size(); i++) {
            LocalContext catchContext = new LocalContext(context);
            JFormalParameter param = parameters.get(i);
            param.setType(param.type().resolve(catchContext));
            Type type = param.type();
            int offset = catchContext.nextOffset();
            LocalVariableDefn defn = new LocalVariableDefn(type, offset);
            defn.initialize();
            catchContext.addEntry(param.line(), param.name(), defn);
            catchBlocks.set(i, ((JBlock) catchBlocks.get(i)).analyze(catchContext));
        }
        if (finallyBlock != null) {
            finallyBlock = (JBlock) finallyBlock.analyze(context);
        }
        return this;
    }


    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        // TODO

        String startTryLabel = output.createLabel();
        String endTryLabel = output.createLabel();
        String endCatchLabel = output.createLabel();
        String startFinallyLabel = output.createLabel();
        String startFinallyPlusOne = output.createLabel();
        String endFinallyLabel = output.createLabel();

        output.addLabel(startTryLabel);
        tryBlock.codegen(output);
        if (finallyBlock != null) {
            finallyBlock.codegen(output);
        }
        output.addBranchInstruction(GOTO, endFinallyLabel);
        output.addLabel(endTryLabel);

        ArrayList<String> catchLabels = new ArrayList<String>();
        for (int i = 0; i < catchBlocks.size(); i++) {
            String catchLabel = output.createLabel();
            catchLabels.add(catchLabel);
            output.addExceptionHandler(startTryLabel, endTryLabel, catchLabel, parameters.get(i).type().jvmName());
            JBlock catchBlock = catchBlocks.get(i);
            output.addLabel(catchLabel);
            output.addNoArgInstruction(ASTORE_1);
            catchBlock.codegen(output);
            if (finallyBlock != null) {
                finallyBlock.codegen(output);
            }
            output.addBranchInstruction(GOTO, endFinallyLabel);
        }
        output.addExceptionHandler(startTryLabel, endTryLabel, startFinallyLabel, null);

        output.addLabel(startFinallyLabel);
        if (finallyBlock != null) {
            output.addOneArgInstruction(ASTORE, catchLabels.size() + 2);
            output.addLabel(startFinallyPlusOne);
            finallyBlock.codegen(output);
            output.addOneArgInstruction(ALOAD, catchLabels.size() + 2);
            output.addNoArgInstruction(ATHROW);
        }
        output.addLabel(endFinallyLabel);
        for (int i = 0; i < catchLabels.size(); i++) {
            if (i < catchLabels.size() - 1) {
                output.addExceptionHandler(catchLabels.get(i), catchLabels.get(i + 1), startFinallyLabel, null);
            } else {
                output.addExceptionHandler(catchLabels.get(i), startFinallyLabel, startFinallyLabel, null);
            }
        }

        if (finallyBlock != null) {
            output.addExceptionHandler(startFinallyLabel, startFinallyPlusOne, startFinallyLabel, null);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JTryStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("TryBlock", e1);
        tryBlock.toJSON(e1);
        if (catchBlocks != null) {
            for (int i = 0; i < catchBlocks.size(); i++) {
                JFormalParameter param = parameters.get(i);
                JBlock catchBlock = catchBlocks.get(i);
                JSONElement e2 = new JSONElement();
                e.addChild("CatchBlock", e2);
                String s = String.format("[\"%s\", \"%s\"]", param.name(), param.type() == null ?
                        "" : param.type().toString());
                e2.addAttribute("parameter", s);
                catchBlock.toJSON(e2);
            }
        }
        if (finallyBlock != null) {
            JSONElement e2 = new JSONElement();
            e.addChild("FinallyBlock", e2);
            finallyBlock.toJSON(e2);
        }
    }
}
