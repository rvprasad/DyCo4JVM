/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package sindu.jvm.instrumentation.entry;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import sindu.jvm.instrumentation.logging.LoggingInstrumentationHelper;

class InstrumentingMethodVisitor extends MethodVisitor {

    private final String name;
    private final String desc;

    public InstrumentingMethodVisitor(final MethodVisitor mv,
            final String name, final String descriptor) {
        super(InstrumenterCLI.ASM_VERSION, mv);
        this.name = name;
        this.desc = descriptor;
    }

    @Override
    public void visitCode() {
        mv.visitCode();

        // create new StringBuffer
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", 
                "<init>", "()V");

        // load entry string onto operand stack
        StringBuilder msg = new StringBuilder();
        msg.append("entry point:").append(name).append(desc);
        mv.visitLdcInsn(msg.toString());
        
        // append entry string to StringBuilder onto operand stack
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", 
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        
        // invoke ToString() on StringBuilder
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", 
                "toString", "()Ljava/lang/String;");
        
        RuntimeLoggerHelper.emitInvokeLog(mv, RuntimeLoggerHelper.LOG_STRING);
    }
}
