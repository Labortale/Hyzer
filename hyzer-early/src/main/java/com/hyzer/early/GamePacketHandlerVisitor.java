package com.hyzer.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.hyzer.early.EarlyLogger.verbose;

/**
 * ASM ClassVisitor for GamePacketHandler transformation.
 * Wraps the ClientMovement lambda in a try-catch for NullPointerException.
 */
public class GamePacketHandlerVisitor extends ClassVisitor {

    private static final String CLIENT_MOVEMENT_DESC =
            "Lcom/hypixel/hytale/protocol/packets/player/ClientMovement;";

    public GamePacketHandlerVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.startsWith("lambda$handle$")
                && descriptor.endsWith(")V")
                && descriptor.contains(CLIENT_MOVEMENT_DESC)) {
            verbose("Found ClientMovement lambda: " + name + descriptor);
            verbose("Applying NPE guard...");
            return new GamePacketHandlerLambdaMethodVisitor(mv);
        }

        return mv;
    }
}
