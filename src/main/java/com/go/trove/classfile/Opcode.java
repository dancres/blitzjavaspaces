/* ====================================================================
 * Trove - Copyright (c) 1997-2000 Walt Disney Internet Group
 * ====================================================================
 * The Tea Software License, Version 1.1
 *
 * Copyright (c) 2000 Walt Disney Internet Group. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Walt Disney Internet Group (http://opensource.go.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Tea", "TeaServlet", "Kettle", "Trove" and "BeanDoc" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact opensource@dig.com.
 *
 * 5. Products derived from this software may not be called "Tea",
 *    "TeaServlet", "Kettle" or "Trove", nor may "Tea", "TeaServlet",
 *    "Kettle", "Trove" or "BeanDoc" appear in their name, without prior
 *    written permission of the Walt Disney Internet Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE WALT DISNEY INTERNET GROUP OR ITS
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * For more information about Tea, please see http://opensource.go.com/.
 */

package com.go.trove.classfile;

/******************************************************************************
 * A collection of opcode constants for creating class file byte code. These
 * opcodes are defined in chapter 6 of 
 * <i>The Java Virual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 * @see CodeAttr
 */
public class Opcode {
    public final static byte NOP = (byte)0;
    public final static byte ACONST_NULL = (byte)1;
    public final static byte ICONST_M1 = (byte)2;
    public final static byte ICONST_0 = (byte)3;
    public final static byte ICONST_1 = (byte)4;
    public final static byte ICONST_2 = (byte)5;
    public final static byte ICONST_3 = (byte)6;
    public final static byte ICONST_4 = (byte)7;
    public final static byte ICONST_5 = (byte)8;
    public final static byte LCONST_0 = (byte)9;
    public final static byte LCONST_1 = (byte)10;
    public final static byte FCONST_0 = (byte)11;
    public final static byte FCONST_1 = (byte)12;
    public final static byte FCONST_2 = (byte)13;
    public final static byte DCONST_0 = (byte)14;
    public final static byte DCONST_1 = (byte)15;
    public final static byte BIPUSH = (byte)16;
    public final static byte SIPUSH = (byte)17;
    public final static byte LDC = (byte)18;
    public final static byte LDC_W = (byte)19;
    public final static byte LDC2_W = (byte)20;
    public final static byte ILOAD = (byte)21;
    public final static byte LLOAD = (byte)22;
    public final static byte FLOAD = (byte)23;
    public final static byte DLOAD = (byte)24;
    public final static byte ALOAD = (byte)25;
    public final static byte ILOAD_0 = (byte)26;
    public final static byte ILOAD_1 = (byte)27;
    public final static byte ILOAD_2 = (byte)28;
    public final static byte ILOAD_3 = (byte)29;
    public final static byte LLOAD_0 = (byte)30;
    public final static byte LLOAD_1 = (byte)31;
    public final static byte LLOAD_2 = (byte)32;
    public final static byte LLOAD_3 = (byte)33;
    public final static byte FLOAD_0 = (byte)34;
    public final static byte FLOAD_1 = (byte)35;
    public final static byte FLOAD_2 = (byte)36;
    public final static byte FLOAD_3 = (byte)37;
    public final static byte DLOAD_0 = (byte)38;
    public final static byte DLOAD_1 = (byte)39;
    public final static byte DLOAD_2 = (byte)40;
    public final static byte DLOAD_3 = (byte)41;
    public final static byte ALOAD_0 = (byte)42;
    public final static byte ALOAD_1 = (byte)43;
    public final static byte ALOAD_2 = (byte)44;
    public final static byte ALOAD_3 = (byte)45;
    public final static byte IALOAD = (byte)46;
    public final static byte LALOAD = (byte)47;
    public final static byte FALOAD = (byte)48;
    public final static byte DALOAD = (byte)49;
    public final static byte AALOAD = (byte)50;
    public final static byte BALOAD = (byte)51;
    public final static byte CALOAD = (byte)52;
    public final static byte SALOAD = (byte)53;
    public final static byte ISTORE = (byte)54;
    public final static byte LSTORE = (byte)55;
    public final static byte FSTORE = (byte)56;
    public final static byte DSTORE = (byte)57;
    public final static byte ASTORE = (byte)58;
    public final static byte ISTORE_0 = (byte)59;
    public final static byte ISTORE_1 = (byte)60;
    public final static byte ISTORE_2 = (byte)61;
    public final static byte ISTORE_3 = (byte)62;
    public final static byte LSTORE_0 = (byte)63;
    public final static byte LSTORE_1 = (byte)64;
    public final static byte LSTORE_2 = (byte)65;
    public final static byte LSTORE_3 = (byte)66;
    public final static byte FSTORE_0 = (byte)67;
    public final static byte FSTORE_1 = (byte)68;
    public final static byte FSTORE_2 = (byte)69;
    public final static byte FSTORE_3 = (byte)70;
    public final static byte DSTORE_0 = (byte)71;
    public final static byte DSTORE_1 = (byte)72;
    public final static byte DSTORE_2 = (byte)73;
    public final static byte DSTORE_3 = (byte)74;
    public final static byte ASTORE_0 = (byte)75;
    public final static byte ASTORE_1 = (byte)76;
    public final static byte ASTORE_2 = (byte)77;
    public final static byte ASTORE_3 = (byte)78;
    public final static byte IASTORE = (byte)79;
    public final static byte LASTORE = (byte)80;
    public final static byte FASTORE = (byte)81;
    public final static byte DASTORE = (byte)82;
    public final static byte AASTORE = (byte)83;
    public final static byte BASTORE = (byte)84;
    public final static byte CASTORE = (byte)85;
    public final static byte SASTORE = (byte)86;
    public final static byte POP = (byte)87;
    public final static byte POP2 = (byte)88;
    public final static byte DUP = (byte)89;
    public final static byte DUP_X1 = (byte)90;
    public final static byte DUP_X2 = (byte)91;
    public final static byte DUP2 = (byte)92;
    public final static byte DUP2_X1 = (byte)93;
    public final static byte DUP2_X2 = (byte)94;
    public final static byte SWAP = (byte)95;
    public final static byte IADD = (byte)96;
    public final static byte LADD = (byte)97;
    public final static byte FADD = (byte)98;
    public final static byte DADD = (byte)99;
    public final static byte ISUB = (byte)100;
    public final static byte LSUB = (byte)101;
    public final static byte FSUB = (byte)102;
    public final static byte DSUB = (byte)103;
    public final static byte IMUL = (byte)104;
    public final static byte LMUL = (byte)105;
    public final static byte FMUL = (byte)106;
    public final static byte DMUL = (byte)107;
    public final static byte IDIV = (byte)108;
    public final static byte LDIV = (byte)109;
    public final static byte FDIV = (byte)110;
    public final static byte DDIV = (byte)111;
    public final static byte IREM = (byte)112;
    public final static byte LREM = (byte)113;
    public final static byte FREM = (byte)114;
    public final static byte DREM = (byte)115;
    public final static byte INEG = (byte)116;
    public final static byte LNEG = (byte)117;
    public final static byte FNEG = (byte)118;
    public final static byte DNEG = (byte)119;
    public final static byte ISHL = (byte)120;
    public final static byte LSHL = (byte)121;
    public final static byte ISHR = (byte)122;
    public final static byte LSHR = (byte)123;
    public final static byte IUSHR = (byte)124;
    public final static byte LUSHR = (byte)125;
    public final static byte IAND = (byte)126;
    public final static byte LAND = (byte)127;
    public final static byte IOR = (byte)128;
    public final static byte LOR = (byte)129;
    public final static byte IXOR = (byte)130;
    public final static byte LXOR = (byte)131;
    public final static byte IINC = (byte)132;
    public final static byte I2L = (byte)133;
    public final static byte I2F = (byte)134;
    public final static byte I2D = (byte)135;
    public final static byte L2I = (byte)136;
    public final static byte L2F = (byte)137;
    public final static byte L2D = (byte)138;
    public final static byte F2I = (byte)139;
    public final static byte F2L = (byte)140;
    public final static byte F2D = (byte)141;
    public final static byte D2I = (byte)142;
    public final static byte D2L = (byte)143;
    public final static byte D2F = (byte)144;
    public final static byte I2B = (byte)145;
    public final static byte I2C = (byte)146;
    public final static byte I2S = (byte)147;
    public final static byte LCMP = (byte)148;
    public final static byte FCMPL = (byte)149;
    public final static byte FCMPG = (byte)150;
    public final static byte DCMPL = (byte)151;
    public final static byte DCMPG = (byte)152;
    public final static byte IFEQ = (byte)153;
    public final static byte IFNE = (byte)154;
    public final static byte IFLT = (byte)155;
    public final static byte IFGE = (byte)156;
    public final static byte IFGT = (byte)157;
    public final static byte IFLE = (byte)158;
    public final static byte IF_ICMPEQ = (byte)159;
    public final static byte IF_ICMPNE = (byte)160;
    public final static byte IF_ICMPLT = (byte)161;
    public final static byte IF_ICMPGE = (byte)162;
    public final static byte IF_ICMPGT = (byte)163;
    public final static byte IF_ICMPLE = (byte)164;
    public final static byte IF_ACMPEQ = (byte)165;
    public final static byte IF_ACMPNE = (byte)166;
    public final static byte GOTO = (byte)167;
    public final static byte JSR = (byte)168;
    public final static byte RET = (byte)169;
    public final static byte TABLESWITCH = (byte)170;
    public final static byte LOOKUPSWITCH = (byte)171;
    public final static byte IRETURN = (byte)172;
    public final static byte LRETURN = (byte)173;
    public final static byte FRETURN = (byte)174;
    public final static byte DRETURN = (byte)175;
    public final static byte ARETURN = (byte)176;
    public final static byte RETURN = (byte)177;
    public final static byte GETSTATIC = (byte)178;
    public final static byte PUTSTATIC = (byte)179;
    public final static byte GETFIELD = (byte)180;
    public final static byte PUTFIELD = (byte)181;
    public final static byte INVOKEVIRTUAL = (byte)182;
    public final static byte INVOKESPECIAL = (byte)183;
    public final static byte INVOKESTATIC = (byte)184;
    public final static byte INVOKEINTERFACE = (byte)185;
    public final static byte UNUSED = (byte)186;
    public final static byte NEW = (byte)187;
    public final static byte NEWARRAY = (byte)188;
    public final static byte ANEWARRAY = (byte)189;
    public final static byte ARRAYLENGTH = (byte)190;
    public final static byte ATHROW = (byte)191;
    public final static byte CHECKCAST = (byte)192;
    public final static byte INSTANCEOF = (byte)193;
    public final static byte MONITORENTER = (byte)194;
    public final static byte MONITOREXIT = (byte)195;
    public final static byte WIDE = (byte)196;
    public final static byte MULTIANEWARRAY = (byte)197;
    public final static byte IFNULL = (byte)198;
    public final static byte IFNONNULL = (byte)199;
    public final static byte GOTO_W = (byte)200;
    public final static byte JSR_W = (byte)201;
    public final static byte BREAKPOINT = (byte)202;

    /**
     * @exception IllegalArgumentException if opcode is invalid
     */
    public final static String getMnemonic(byte opcode) 
        throws IllegalArgumentException {
        try {
            return Mnemonic.m[opcode & 0xff];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException
                ("Opcode not valid: " + opcode);
        }
    }

    /**
     * Reverses the condition for an "if" opcode. i.e. IFEQ is changed to
     * IFNE.
     */
    public final static byte reverseIfOpcode(byte opcode) {
        // Actually, because the numbers assigned to the "if" opcodes
        // were so cleverly chosen, all I really need to do is toggle
        // bit 0. I'm not going to do that because I still need to check if 
        // an invalid opcode was passed in.

        switch (opcode) {
        case IF_ACMPEQ:
            return IF_ACMPNE;
        case IF_ACMPNE:
            return IF_ACMPEQ;
        case IF_ICMPEQ:
            return IF_ICMPNE;
        case IF_ICMPNE:
            return IF_ICMPEQ;
        case IF_ICMPLT:
            return IF_ICMPGE;
        case IF_ICMPGE:
            return IF_ICMPLT;
        case IF_ICMPGT:
            return IF_ICMPLE;
        case IF_ICMPLE:
            return IF_ICMPGT;
        case IFEQ:
            return IFNE;
        case IFNE:
            return IFEQ;
        case IFLT:
            return IFGE;
        case IFGE:
            return IFLT;
        case IFGT:
            return IFLE;
        case IFLE:
            return IFGT;
        case IFNONNULL:
            return IFNULL;
        case IFNULL:
            return IFNONNULL;
        default:
            throw new IllegalArgumentException
                ("Opcode not an if instruction: " + getMnemonic(opcode));
        }
    }

    private static class Mnemonic {
        public static final String[] m =
        {
            "nop",
            "aconst_null",
            "iconst_m1",
            "iconst_0",
            "iconst_1",
            "iconst_2",
            "iconst_3",
            "iconst_4",
            "iconst_5",
            "lconst_0",
            "lconst_1",
            "fconst_0",
            "fconst_1",
            "fconst_2",
            "dconst_0",
            "dconst_1",
            "bipush",
            "sipush",
            "ldc",
            "ldc_w",
            "ldc2_w",
            "iload",
            "lload",
            "fload",
            "dload",
            "aload",
            "iload_0",
            "iload_1",
            "iload_2",
            "iload_3",
            "lload_0",
            "lload_1",
            "lload_2",
            "lload_3",
            "fload_0",
            "fload_1",
            "fload_2",
            "fload_3",
            "dload_0",
            "dload_1",
            "dload_2",
            "dload_3",
            "aload_0",
            "aload_1",
            "aload_2",
            "aload_3",
            "iaload",
            "laload",
            "faload",
            "daload",
            "aaload",
            "baload",
            "caload",
            "saload",
            "istore",
            "lstore",
            "fstore",
            "dstore",
            "astore",
            "istore_0",
            "istore_1",
            "istore_2",
            "istore_3",
            "lstore_0",
            "lstore_1",
            "lstore_2",
            "lstore_3",
            "fstore_0",
            "fstore_1",
            "fstore_2",
            "fstore_3",
            "dstore_0",
            "dstore_1",
            "dstore_2",
            "dstore_3",
            "astore_0",
            "astore_1",
            "astore_2",
            "astore_3",
            "iastore",
            "lastore",
            "fastore",
            "dastore",
            "aastore",
            "bastore",
            "castore",
            "sastore",
            "pop",
            "pop2",
            "dup",
            "dup_x1",
            "dup_x2",
            "dup2",
            "dup2_x1",
            "dup2_x2",
            "swap",
            "iadd",
            "ladd",
            "fadd",
            "dadd",
            "isub",
            "lsub",
            "fsub",
            "dsub",
            "imul",
            "lmul",
            "fmul",
            "dmul",
            "idiv",
            "ldiv",
            "fdiv",
            "ddiv",
            "irem",
            "lrem",
            "frem",
            "drem",
            "ineg",
            "lneg",
            "fneg",
            "dneg",
            "ishl",
            "lshl",
            "ishr",
            "lshr",
            "iushr",
            "lushr",
            "iand",
            "land",
            "ior",
            "lor",
            "ixor",
            "lxor",
            "iinc",
            "i2l",
            "i2f",
            "i2d",
            "l2i",
            "l2f",
            "l2d",
            "f2i",
            "f2l",
            "f2d",
            "d2i",
            "d2l",
            "d2f",
            "i2b",
            "i2c",
            "i2s",
            "lcmp",
            "fcmpl",
            "fcmpg",
            "dcmpl",
            "dcmpg",
            "ifeq",
            "ifne",
            "iflt",
            "ifge",
            "ifgt",
            "ifle",
            "if_icmpeq",
            "if_icmpne",
            "if_icmplt",
            "if_icmpge",
            "if_icmpgt",
            "if_icmple",
            "if_acmpeq",
            "if_acmpne",
            "goto",
            "jsr",
            "ret",
            "tableswitch",
            "lookupswitch",
            "ireturn",
            "lreturn",
            "freturn",
            "dreturn",
            "areturn",
            "return",
            "getstatic",
            "putstatic",
            "getfield",
            "putfield",
            "invokevirtual",
            "invokespecial",
            "invokestatic",
            "invokeinterface",
            "unused",
            "new",
            "newarray",
            "anewarray",
            "arraylength",
            "athrow",
            "checkcast",
            "instanceof",
            "monitorenter",
            "monitorexit",
            "wide",
            "multianewarray",
            "ifnull",
            "ifnonnull",
            "goto_w",
            "jsr_w",
            "breakpoint",
        };
    }
}
