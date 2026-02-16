package com.hyzer.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.hyzer.early.EarlyLogger.verbose;

/**
 * ASM ClassVisitor for BlockHealthSystem transformation.
 * Wraps tick() in a try-catch for NullPointerException.
 */
public class BlockHealthSystemVisitor extends ClassVisitor {

    private static final String TICK_METHOD = "tick";

    public BlockHealthSystemVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals(TICK_METHOD) && descriptor.endsWith(")V")) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying NPE guard...");
            return new BlockHealthSystemTickMethodVisitor(mv);
        }

        return mv;
    }
}
