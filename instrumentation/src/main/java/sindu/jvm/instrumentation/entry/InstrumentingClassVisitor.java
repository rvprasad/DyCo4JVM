/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package sindu.jvm.instrumentation.entry;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class InstrumentingClassVisitor extends ClassVisitor {

    InstrumentingClassVisitor(final ClassVisitor cv) {
        super(InstrumenterCLI.ASM_VERSION, cv);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, 
            final String desc, final String signature, 
            final String[] exceptions) {
        final MethodVisitor _mv = cv.visitMethod(access, name, desc, signature, 
                exceptions);
        if (_mv != null && !name.equals("<init>")
                && !name.equals("toString")
                && !name.equals("hashCode")) {
            return new InstrumentingMethodVisitor(_mv, name, desc);
        } else {
            return _mv;
        }
    }
}
