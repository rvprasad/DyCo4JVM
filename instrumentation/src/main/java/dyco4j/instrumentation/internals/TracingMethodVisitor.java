/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import dyco4j.instrumentation.LoggingHelper;
import dyco4j.logging.Logger;
import dyco4j.utility.ClassNameHelper;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

final class TracingMethodVisitor extends MethodVisitor {
    private final String methodId;
    private final Method method;
    private final boolean isStatic;
    private final TracingClassVisitor cv;
    private final Map<Label, Label> beginLabel2endLabel;
    private int callsiteId;
    private boolean thisInitialized;
    private Label outermostExceptionHandlerBeginLabel;

    TracingMethodVisitor(final int access, final String name, final String desc, final MethodVisitor mv,
                         final TracingClassVisitor owner, final boolean thisInitialized) {
        super(CLI.ASM_VERSION, mv);
        this.method = new Method(name, desc);
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.methodId = owner.getMethodId(name, desc);
        this.cv = owner;
        this.thisInitialized = thisInitialized;
        this.beginLabel2endLabel = new HashMap<>();
    }

    @Override
    public void visitCode() {
        beginOutermostExceptionHandler();
        emitLogMethodEntry();
        emitLogMethodArguments();
    }

    @Override
    public final void visitInsn(final int opcode) {
        if (opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN ||
                opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN) {
            if (this.cv.cmdLineOptions.traceMethodRetValue)
                LoggingHelper.emitLogReturn(this.mv, method.getReturnType());
            LoggingHelper.emitLogMethodExit(this.mv, this.methodId, LoggingHelper.ExitKind.NORMAL);
            super.visitInsn(opcode);
        } else if (opcode == Opcodes.AASTORE || opcode == Opcodes.BASTORE || opcode == Opcodes.CASTORE ||
                opcode == Opcodes.DASTORE || opcode == Opcodes.FASTORE || opcode == Opcodes.IASTORE ||
                opcode == Opcodes.LASTORE || opcode == Opcodes.SASTORE) {
            if (this.cv.cmdLineOptions.traceArrayAccess)
                visitArrayStoreInsn(opcode);
            else
                super.visitInsn(opcode);
        } else if (opcode == Opcodes.AALOAD || opcode == Opcodes.BALOAD || opcode == Opcodes.CALOAD ||
                opcode == Opcodes.DALOAD || opcode == Opcodes.FALOAD || opcode == Opcodes.IALOAD ||
                opcode == Opcodes.LALOAD || opcode == Opcodes.SALOAD) {
            if (this.cv.cmdLineOptions.traceArrayAccess)
                visitArrayLoadInsn(opcode);
            else
                super.visitInsn(opcode);
        } else
            super.visitInsn(opcode);
    }

    @Override
    public final void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        if (this.cv.cmdLineOptions.traceFieldAccess) {
            final Type _fieldType = Type.getType(desc);
            final String _fieldId = this.cv.getFieldId(name, owner, desc);
            final boolean _isFieldStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
            if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD) {
                if (_isFieldStatic)
                    super.visitInsn(Opcodes.ACONST_NULL);
                else if (this.thisInitialized)
                    super.visitInsn(Opcodes.DUP);
                else {
                    super.visitLdcInsn(LoggingHelper.UNINITIALIZED_THIS);
                    super.visitInsn(Opcodes.SWAP);
                }

                super.visitFieldInsn(opcode, owner, name, desc);
                LoggingHelper.emitLogField(this.mv, _fieldId, _fieldType, Logger.FieldAction.GETF);
            } else if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) {
                if (_isFieldStatic) {
                    super.visitInsn(Opcodes.ACONST_NULL);
                } else if (this.thisInitialized) {
                    LoggingHelper.emitSwapTwoWordsAndOneWord(this.mv, _fieldType);
                    final int _fieldSort = _fieldType.getSort();
                    if (_fieldSort == Type.LONG || _fieldSort == Type.DOUBLE)
                        super.visitInsn(Opcodes.DUP_X2);
                    else
                        super.visitInsn(Opcodes.DUP_X1);
                } else {
                    super.visitLdcInsn(LoggingHelper.UNINITIALIZED_THIS);
                }

                LoggingHelper.emitSwapOneWordAndTwoWords(this.mv, _fieldType);
                LoggingHelper.emitLogField(this.mv, _fieldId, _fieldType, Logger.FieldAction.PUTF);
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        } else
            super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public final void visitMaxs(final int maxStack, final int maxLocals) {
        endOutermostExceptionHandler();
        for (final Map.Entry<Label, Label> _e : beginLabel2endLabel.entrySet()) {
            final Label _handlerLabel = new Label();
            super.visitLabel(_handlerLabel);
            super.visitTryCatchBlock(_e.getKey(), _e.getValue(), _handlerLabel, "java/lang/Throwable");
            LoggingHelper.emitLogException(this.mv);
            LoggingHelper.emitLogMethodExit(this.mv, this.methodId, LoggingHelper.ExitKind.EXCEPTIONAL);
            super.visitInsn(Opcodes.ATHROW);
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
                                final boolean itf) {
        if (this.cv.cmdLineOptions.traceMethodCall)
            LoggingHelper.emitLogMethodCall(this.mv, this.cv.getMethodId(name, owner, desc), this.callsiteId++);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm,
                                       final Object... bsmArgs) {
        if (this.cv.cmdLineOptions.traceMethodCall)
            LoggingHelper.emitLogMethodCall(this.mv,
                    this.cv.getMethodId(name, ClassNameHelper.DYNAMIC_METHOD_OWNER, desc), this.callsiteId++);
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    void setThisInitialized() {
        this.thisInitialized = true;
    }

    void beginOutermostExceptionHandler() {
        this.outermostExceptionHandlerBeginLabel = new Label();
        super.visitLabel(this.outermostExceptionHandlerBeginLabel);
    }

    void endOutermostExceptionHandler() {
        assert this.outermostExceptionHandlerBeginLabel != null;
        final Label _l = new Label();
        super.visitLabel(_l);
        this.beginLabel2endLabel.put(this.outermostExceptionHandlerBeginLabel, _l);
        this.outermostExceptionHandlerBeginLabel = null;
    }

    private void emitLogMethodEntry() {
        super.visitCode();
        LoggingHelper.emitLogMethodEntry(this.mv, this.methodId);
    }

    private void emitLogMethodArguments() {
        if (this.cv.cmdLineOptions.traceMethodArgs) {
            // emit code to trace each arg
            int _position = 0;
            int _localVarIndex = 0;
            if (!this.isStatic) {
                final OptionalInt _tmp1 = this.thisInitialized ? OptionalInt.of(_localVarIndex) : OptionalInt.empty();
                _localVarIndex += LoggingHelper.emitLogArgument(this.mv, _position, _tmp1,
                        Type.getType(Object.class));
                _position++;
            }

            for (final Type _argType : this.method.getArgumentTypes()) {
                _localVarIndex += LoggingHelper.emitLogArgument(this.mv, _position, OptionalInt.of(_localVarIndex),
                        _argType);
                _position++;
            }
        }
    }

    private void visitArrayStoreInsn(final int opcode) {
        if (opcode == Opcodes.LASTORE || opcode == Opcodes.DASTORE) {
            super.visitInsn(Opcodes.DUP2_X2);
            super.visitInsn(Opcodes.POP2);
            super.visitInsn(Opcodes.DUP2_X2);
            super.visitInsn(Opcodes.DUP2_X2);
            super.visitInsn(Opcodes.POP2);
            super.visitInsn(Opcodes.DUP2_X2);
        } else {
            super.visitInsn(Opcodes.DUP_X2);
            super.visitInsn(Opcodes.POP);
            super.visitInsn(Opcodes.DUP2_X1);
            super.visitInsn(Opcodes.DUP2_X1);
            super.visitInsn(Opcodes.POP2);
            super.visitInsn(Opcodes.DUP_X2);
        }

        switch (opcode) {
            case Opcodes.AASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.getObjectType("java/lang/Object"));
                break;
            case Opcodes.BASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.BYTE_TYPE);
                break;
            case Opcodes.CASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.CHAR_TYPE);
                break;
            case Opcodes.FASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.FLOAT_TYPE);
                break;
            case Opcodes.IASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.INT_TYPE);
                break;
            case Opcodes.SASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.SHORT_TYPE);
                break;
            case Opcodes.DASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.DOUBLE_TYPE);
                break;
            case Opcodes.LASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.LONG_TYPE);
                break;
        }

        LoggingHelper.emitLogArray(this.mv, Logger.ArrayAction.PUTA);

        super.visitInsn(opcode);
    }

    private void visitArrayLoadInsn(final int opcode) {
        super.visitInsn(Opcodes.DUP2);

        super.visitInsn(opcode);

        if (opcode == Opcodes.LALOAD || opcode == Opcodes.DALOAD)
            super.visitInsn(Opcodes.DUP2_X2);
        else
            super.visitInsn(Opcodes.DUP_X2);

        switch (opcode) {
            case Opcodes.AALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.getObjectType("java/lang/Object"));
                break;
            case Opcodes.BALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.BYTE_TYPE);
                break;
            case Opcodes.CALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.CHAR_TYPE);
                break;
            case Opcodes.FALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.FLOAT_TYPE);
                break;
            case Opcodes.IALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.INT_TYPE);
                break;
            case Opcodes.SALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.SHORT_TYPE);
                break;
            case Opcodes.DALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.DOUBLE_TYPE);
                break;
            case Opcodes.LALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.LONG_TYPE);
                break;
        }

        LoggingHelper.emitLogArray(this.mv, Logger.ArrayAction.GETA);
    }

}
