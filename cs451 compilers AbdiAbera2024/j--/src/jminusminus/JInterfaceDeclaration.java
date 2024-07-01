// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * A representation of an interface declaration.
 */
class JInterfaceDeclaration extends JAST implements JTypeDecl {
    // Interface modifiers.
    private final ArrayList<String> mods;

    // Interface name.
    private final String name;

    // This interface type.
    private Type thisType;

    // Super class type.
    private final Type superType;

    // Extended interfaces.
    private final ArrayList<TypeName> superInterfaces;

    // Interface block.
    private final ArrayList<JMember> interfaceBlock;

    // Context for this interface.
    private ClassContext context;


    private boolean hasExplicitConstructor;

    private final ArrayList<JFieldDeclaration> instanceFieldInit;
    private final ArrayList<JFieldDeclaration> staticFieldInit;


    /**
     * Constructs an AST node for an interface declaration.
     *
     * @param line            line in which the interface declaration occurs in the source file.
     * @param mods            class modifiers.
     * @param name            class name.
     * @param superInterfaces super class types.
     * @param interfaceBlock  interface block.
     */
    public JInterfaceDeclaration(int line, ArrayList<String> mods, String name,
                                 ArrayList<TypeName> superInterfaces,
                                 ArrayList<JMember> interfaceBlock) {
        super(line);
        this.mods = mods;
        this.name = name;
        this.superType = Type.OBJECT;
        this.superInterfaces = superInterfaces;
        this.interfaceBlock = interfaceBlock;
        this.instanceFieldInit = new ArrayList<>(); // Initialize instanceFieldInitialization
        this.staticFieldInit = new ArrayList<>(); // Initialize staticFieldInitialization

        // An interface must have the "abstract" and "interface" modifiers.
        if (!this.mods.contains("abstract")) {
            this.mods.add("abstract");
        }
        if (!this.mods.contains("interface")) {
            this.mods.add("interface");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void declareThisType(Context context) {
        // TODO
        String qualifiedName = JAST.compilationUnit.packageName().equals("") ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        CLEmitter partial = new CLEmitter(false);
        partial.addClass(mods, qualifiedName, Type.OBJECT.jvmName(), null, true);
        thisType = Type.typeFor(partial.toClass());
        context.addType(line, thisType);
    }

    /**
     * {@inheritDoc}
     */
    public void preAnalyze(Context context) {
        // TODO
        this.context = new ClassContext(this, context);

        // Resolve superInterfaces.
        if (superInterfaces != null) {
            superInterfaces.replaceAll(typeName -> (TypeName) typeName.resolve(this.context));
        }

        // Create the (partial) interface.
        CLEmitter partial = new CLEmitter(false);

        // Add the interface header to the partial interface
        String qualifiedName = JAST.compilationUnit.packageName().equals("") ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        partial.addClass(mods, qualifiedName, superType.jvmName(), null, true);

        // Pre-analyze the members and add them to the partial interface.
        for (JMember member : interfaceBlock) {
            member.preAnalyze(this.context, partial);
        }

        // Get the InterfaceRep for the (partial) interface and make it the representation for this type.
        Type id = this.context.lookupType(name);
        if (id != null && !JAST.compilationUnit.errorHasOccurred()) {
            id.setClassRep(partial.toClass());
        }
    }

    /**
     * {@inheritDoc}
     */
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Type superType() {
        return superType;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList<TypeName> superInterfaces() {
        return superInterfaces;
    }

    /**
     * {@inheritDoc}
     */
    public Type thisType() {
        // TODO
        return thisType;
    }

    public ArrayList<JFieldDeclaration> instanceFieldInitializations() {
        return instanceFieldInit;
    }

    public ArrayList<JFieldDeclaration> staticFieldInitializations() {
        return staticFieldInit;
    }

    /**
     * {@inheritDoc}
     */
    public JAST analyze(Context context) {
        // TODO
        // Analyze all members
        for (JMember member : interfaceBlock) {
            ((JAST) member).analyze(this.context);
        }
        // Copy declared fields for purposes of initialization.
        for (JMember member : interfaceBlock) {
            if (member instanceof JFieldDeclaration fieldDecl) {
                if (fieldDecl.mods().contains("static")) {
                    staticFieldInitializations().add(fieldDecl);
                } else {
                    instanceFieldInitializations().add(fieldDecl);
                }
            }
        }
        // Finally, ensure that a non-abstract class has
        // no abstract methods.
        if (!thisType.isAbstract() && thisType.abstractMethods().size() > 0) {
            StringBuilder methods = new StringBuilder();
            for (Method method : thisType.abstractMethods()) {
                methods.append("\n").append(method);
            }
            JAST.compilationUnit.reportSemanticError(line,
                    "Class must be declared abstract since it defines "
                            + "the following abstract methods: %s", methods.toString());
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        // TODO
        // The class header
        String qualifiedName = JAST.compilationUnit.packageName().equals("") ? name
                : JAST.compilationUnit.packageName() + "/" + name;
        output.addClass(mods, qualifiedName, superType.jvmName(), null, false);
        // The implicit empty constructor?
        if (!hasExplicitConstructor) {
            codegenImplicitConstructor(output);
        }
        // The members
        for (JMember member : interfaceBlock) {
            ((JAST) member).codegen(output);
        }
        // Generate a class initialization method?
        if (staticFieldInit.size() > 0) {
            codegenClassInit(output);
        }
    }

    private void codegenImplicitConstructor(CLEmitter output) {
        // Invoke super constructor
        ArrayList<String> mods = new ArrayList<>();
        mods.add("public");
        output.addMethod(mods, "<init>", "()V", null, false);
        output.addNoArgInstruction(ALOAD_0);
        output.addMemberAccessInstruction(INVOKESPECIAL, superType.jvmName(),
                "<init>", "()V");
        // If there are instance field initializations, generate
        // code for them
        for (JFieldDeclaration instanceField : staticFieldInit) {
            instanceField.codegenInitializations(output);
        }
        // Return
        output.addNoArgInstruction(RETURN);
    }

    private void codegenClassInit(CLEmitter output) {

        ArrayList<String> mods = new ArrayList<>();
        mods.add("public");
        mods.add("static");
        output.addMethod(mods, "<clinit>", "()V", null, false);

        // If there are instance initializations, generate code
        // for them
        for (JFieldDeclaration staticField : staticFieldInit) {
            staticField.codegenInitializations(output);
        }
        // Return
        output.addNoArgInstruction(RETURN);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JInterfaceDeclaration:" + line, e);
        if (mods != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (String mod : mods) {
                value.add(String.format("\"%s\"", mod));
            }
            e.addAttribute("modifiers", value);
        }
        e.addAttribute("name", name);
        e.addAttribute("super", superType == null ? "" : superType.toString());
        if (superInterfaces != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (TypeName impl : superInterfaces) {
                value.add(String.format("\"%s\"", impl.toString()));
            }
            e.addAttribute("extends", value);
        }
        if (context != null) {
            context.toJSON(e);
        }
        if (interfaceBlock != null) {
            for (JMember member : interfaceBlock) {
                ((JAST) member).toJSON(e);
            }
        }
    }
}
