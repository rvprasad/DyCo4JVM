/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package sindu.jvm.instrumentation.entry;

import org.apache.commons.io.FileUtils;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import static org.objectweb.asm.Opcodes.ASM5;

public class InstrumenterCLI {

    public final static int ASM_VERSION = ASM5;

    public static void main(final String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Please provide a list of"
                    + "fully-qualified paths to classes to instrument.");
        }


        Stream.of(args).parallel().forEach(_arg -> {
            try {
                final File _src = new File(_arg);
                final File _trg = new File(_arg + ".orig");
                final byte[] _b = FileUtils.readFileToByteArray(src);
                final ClassReader _cr = new ClassReader(_b);
                final ClassWriter _cw = new ClassWriter(_cr, 
                        ClassWriter.COMPUTE_MAXS);
                final ClassVisitor _cv = new InstrumentingClassVisitor(_cw);
                _cr.accept(_cv, 0);

                if (!_trg.exists()) {
                    FileUtils.moveFile(_src, _trg);
                }
                FileUtils.writeByteArrayToFile(_src, _cw.toByteArray());
            } catch (final IOException _ex) {
                final String _name = InstrumenterCLI.class.getName();
                final Logger _logger = Logger.getLogger(_name);
                _logger.log(Level.SEVERE, null, _ex);
            }
        });
    }
}
