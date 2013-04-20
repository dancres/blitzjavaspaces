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

import java.util.*;
import java.io.*;

/******************************************************************************
 * The InstructionList class is used by the CodeBuilder to perform lower-level
 * bookkeeping operations and flow analysis.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
 * @see CodeBuilder
 */
class InstructionList implements CodeBuffer {
    private static final boolean DEBUG = false;

    Instruction mFirst;
    Instruction mLast;

    boolean mResolved = false;

    private List mExceptionHandlers = new ArrayList(4);
    private List mLocalVariables = new ArrayList();

    private int mMaxStack;
    private int mMaxLocals;

    private byte[] mByteCodes;
    private int mBufferLength;

    protected InstructionList() {
        super();
    }

    /**
     * Returns an immutable collection of all the instructions in this
     * InstructionList.
     */
    public Collection getInstructions() {
        return new AbstractCollection() {
            public Iterator iterator() {
                return new Iterator() {
                    private Instruction mNext = mFirst;

                    public boolean hasNext() {
                        return mNext != null;
                    }

                    public Object next() {
                        if (mNext == null) {
                            throw new NoSuchElementException();
                        }

                        Instruction current = mNext;
                        mNext = mNext.mNext;
                        return current;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            public int size() {
                int count = 0;
                for (Instruction i = mFirst; i != null; i = i.mNext) {
                    count++;
                }
                return count;
            }
        };
    }

    public int getMaxStackDepth() {
        resolve();
        return mMaxStack;
    }

    public int getMaxLocals() {
        resolve();
        return mMaxLocals;
    }

    public byte[] getByteCodes() {
        resolve();
        return mByteCodes;
    }

    public ExceptionHandler[] getExceptionHandlers() {
        resolve();

        ExceptionHandler[] handlers = 
            new ExceptionHandler[mExceptionHandlers.size()];
        return (ExceptionHandler[])mExceptionHandlers.toArray(handlers);
    }

    public void addExceptionHandler(ExceptionHandler handler) {
        mExceptionHandlers.add(handler);
    }

    public LocalVariable createLocalVariable(String name,
                                             TypeDescriptor type) {
        LocalVariable var = new LocalVariableImpl(name, type, -1);
        mLocalVariables.add(var);
        return var;
    }

    public LocalVariable createLocalParameter(String name,
                                              TypeDescriptor type,
                                              int number) {
        LocalVariableImpl var = new LocalVariableImpl(name, type, number);
        mLocalVariables.add(var);
        if (mFirst == null) {
            // Make sure there is an initial instruction. Create a pseudo one.
            LabelInstruction label = new LabelInstruction();
            label.setLocation();
            mFirst = label;
        }
        
        // Parameters are initialized first, so ensure flow analysis starts
        // at the beginning.
        var.addStoreInstruction(mFirst);

        return var;
    }

    private void resolve() {
        if (mResolved) {
            return;
        }

        if (!DEBUG) {
            resolve0();
        }
        else {
            try {
                resolve0();
            }
            finally {
                System.out.println("-- Instructions --");
                
                Iterator it = getInstructions().iterator();
                while (it.hasNext()) {
                    System.out.println(it.next());
                }
            }
        }
    }

    private void resolve0() {
        mMaxStack = 0;
        mMaxLocals = 0;

        Instruction instr;

        // Sweep through the instructions, marking them as not being
        // visted by flow analysis and set fake locations.
        int instrCount = 0;
        for (instr = mFirst; instr != null; instr = instr.mNext) {
            instr.mStackDepth = -1;
            instr.mLocation = instrCount++;
        }

        // Assign variable numbers using the simplest technique.

        int size = mLocalVariables.size();
        List activeLocationBits = new ArrayList(size);
        for (int i=0; i<size; i++) {
            LocalVariableImpl var = (LocalVariableImpl)mLocalVariables.get(i);
            if (var.getNumber() < 0) {
                var.setNumber(mMaxLocals);
            }
            
            int max = var.getNumber() + (var.isDoubleWord() ? 2 : 1);
            if (max > mMaxLocals) {
                mMaxLocals = max;
            }
        }

        /*
        // Perform variable flow analysis for each local variable, in order to
        // determine which register it should be assigned.

        int size = mLocalVariables.size();
        List activeLocationBits = new ArrayList(size);
        for (int i=0; i<size; i++) {
            LocalVariableImpl var = (LocalVariableImpl)mLocalVariables.get(i);
            BitList activeLocations = new BitList(instrCount);
            activeLocationBits.add(activeLocations);

            // Start flow analysis at store variable instructions.
            Iterator it = var.iterateStoreInstructions();
            while (it.hasNext()) {
                Instruction enter = (Instruction)it.next();
                variableResolve(var, enter, activeLocations,
                                new BitList(instrCount), false);
            }
            
            // Continue flow analysis into all exception handlers that wrap
            // the active locations.
            boolean passAgain;
            do {
                passAgain = false;
                it = mExceptionHandlers.iterator();
                while (it.hasNext()) {
                    ExceptionHandler handler = (ExceptionHandler)it.next();
                    if (!isIntersecting(activeLocations, handler)) {
                        continue;
                    }
                    Instruction enter = 
                        (Instruction)handler.getCatchLocation();
                    passAgain =
                        variableResolve(var, enter, activeLocations,
                                        new BitList(instrCount), false);
                    passAgain = false;
                }
            } while (passAgain);

            if (!var.isFixedNumber()) {
                var.setNumber(-1);

                // Assign variable number by checking first if it can be shared
                // with another variable of the same size.

                boolean[] conflicts = new boolean[mMaxLocals];

                for (int j=0; j<i; j++) {
                    LocalVariable av = (LocalVariable)mLocalVariables.get(j);
                    if (av.getNumber() < 0 ||
                        av.isDoubleWord() != var.isDoubleWord()) {
                        continue;
                    }
                    BitList alocs = (BitList)activeLocationBits.get(j);
                    if (isIntersecting(alocs, activeLocations)) {
                        conflicts[av.getNumber()] = true;
                        if (av.getNumber() == var.getNumber()) {
                            var.setNumber(-1);
                        }
                    }
                    else if (conflicts[av.getNumber()]) {
                        var.setNumber(-1);
                    }
                    else {
                        var.setNumber(av.getNumber());
                    }
                }
                
                if (var.getNumber() < 0) {
                    var.setNumber(mMaxLocals);
                }
            }

            int max = var.getNumber() + (var.isDoubleWord() ? 2 : 1);
            if (max > mMaxLocals) {
                mMaxLocals = max;
            }

            // TODO
            //var.setLocations(activeLocations);
        }

        activeLocationBits = null;
        */

        // Perform stack flow analysis to determine the max stack size.

        // Start the flow analysis at the first instruction.
        Map subAdjustMap = new HashMap(11);
        stackResolve(0, mFirst, subAdjustMap);

        // Continue flow analysis into exception handler entry points.
        Iterator it = mExceptionHandlers.iterator();
        while (it.hasNext()) {
            ExceptionHandler handler = (ExceptionHandler)it.next();
            Instruction enter = (Instruction)handler.getCatchLocation();
            stackResolve(1, enter, subAdjustMap);
        }

        // Okay, build up the byte code and set real instruction locations.
        // Multiple passes may be required because instructions may adjust
        // their size as locations are set. Changing size affects the
        // locations of other instructions, so that is why additional passes
        // are required.
        
        boolean passAgain;
        do {
            passAgain = false;

            mByteCodes = new byte[16];
            mBufferLength = 0;

            for (instr = mFirst; instr != null; instr = instr.mNext) {
                if (!instr.isResolved()) {
                    passAgain = true;
                }

                if (instr instanceof Label) {
                    if (instr.mLocation != mBufferLength) {
                        if (instr.mLocation >= 0) {
                            // If the location of this label is not where it
                            // should be, (most likely because an instruction
                            // needed to expand in size) then do another pass.
                            passAgain = true;
                        }

                        instr.mLocation = mBufferLength;
                    }
                }
                else {
                    instr.mLocation = mBufferLength;

                    byte[] bytes = instr.getBytes();
                    if (bytes != null) {
                        if (passAgain) {
                            // If there is going to be another pass, don't 
                            // bother collecting bytes into the array. Just 
                            // expand the the length variable.
                            mBufferLength += bytes.length;
                        }
                        else {
                            addBytes(bytes);
                        }
                    }
                }
            }
        } while (passAgain); // do {} while ();

        if (mBufferLength != mByteCodes.length) {
            byte[] newBytes = new byte[mBufferLength];
            System.arraycopy(mByteCodes, 0, newBytes, 0, mBufferLength);
            mByteCodes = newBytes;
        }

        // Set resolved at end because during resolution, this field gets
        // set false again while changes are being made to the list
        // of instructions.
        mResolved = true;
    }

    private void addBytes(byte[] code) {
        growBuffer(code.length);
        System.arraycopy(code, 0, mByteCodes, mBufferLength, code.length);
        mBufferLength += code.length;
    }

    private void growBuffer(int amount) {
        if ((mBufferLength + amount) > mByteCodes.length) {
            int newCapacity = mByteCodes.length * 2;
            if ((mBufferLength + amount) > newCapacity) {
                newCapacity = mBufferLength + amount;
            }

            byte[] newBuffer = new byte[newCapacity];
            System.arraycopy(mByteCodes, 0, newBuffer, 0, mBufferLength);
            mByteCodes = newBuffer;
        }
    }

    /*
    private boolean variableResolve(LocalVariableImpl var,
                                    Instruction instr,
                                    BitList activeLocations,
                                    BitList possibleLocations,
                                    boolean fork) {
        while (instr != null) {
            int instrLoc = instr.getLocation();

            if (activeLocations.get(instrLoc)) {
                activeLocations.or(possibleLocations);
                if (possibleLocations.isAllClear()) {
                    return false;
                }
                else {
                    possibleLocations.clear();
                    return true;
                }
            }

            if (possibleLocations.get(instrLoc)) {
                return false;
            }

            possibleLocations.set(instrLoc);

            if (instr instanceof LocalOperandInstruction &&
                ((LocalOperandInstruction)instr).getLocalVariable() == var) {

                if (instr instanceof StoreLocalInstruction) {
                    activeLocations.set(instrLoc);
                    if (fork) {
                        possibleLocations.clear(instrLoc);
                    }
                    else {
                        possibleLocations.clear();
                    }
                }
                else {
                    activeLocations.or(possibleLocations);
                    possibleLocations.clear();
                }
            }

            // Determine the next instruction to flow down to.
            Instruction next = null;

            if (instr.isFlowThrough()) {
                if ((next = instr.mNext) == null) {
                    throw new RuntimeException
                        ("Execution flows through end of method");
                }
            }

            Location[] targets = instr.getBranchTargets();
            if (targets != null) {
                for (int i=0; i<targets.length; i++) {
                    LabelInstruction targetInstr = 
                        (LabelInstruction)targets[i];

                    if (i == 0 && next == null) {
                        // Flow to the first target if instruction doesn't
                        // flow to its next instruction.
                        next = targetInstr;
                        continue;
                    }

                    variableResolve
                        (var, targetInstr, activeLocations,
                         possibleLocations, true);
                }
            }

            instr = next;
        }

        return true;
    }

    private boolean isIntersecting(BitList a, BitList b) {
        a = (BitList)a.clone();
        a.and(b);
        return !a.isAllClear();
    }

    private boolean isIntersecting(BitList bits, LocationRange range) {
        // TODO: how efficient is this?
        int start = range.getStartLocation().getLocation();
        int end = range.getEndLocation().getLocation();

        for (int i=start; i<end; i++) {
            if (bits.get(i)) {
                return true;
            }
        }

        return false;
    }
    */

    private int stackResolve(int stackDepth, 
                             Instruction instr, 
                             Map subAdjustMap) {
        while (instr != null) {
            // Set the stack depth, marking this instruction as being visited.
            // If already visited, break out of this flow.
            if (instr.mStackDepth < 0) {
                instr.mStackDepth = stackDepth;
            }
            else {
                if (instr.mStackDepth != stackDepth) {
                    throw new RuntimeException
                        ("Stack depth different at previously visited " +
                         "instruction: " + instr.mStackDepth + 
                         " != " + stackDepth);
                }

                break;
            }

            // Determine the next instruction to flow down to.
            Instruction next = null;

            if (instr.isFlowThrough()) {
                if ((next = instr.mNext) == null) {
                    throw new RuntimeException
                        ("Execution flows through end of method");
                }
            }

            stackDepth += instr.getStackAdjustment();
            if (stackDepth > mMaxStack) {
                mMaxStack = stackDepth;
            }
            else if (stackDepth < 0) {
                throw new RuntimeException("Stack depth is negative: " + 
                                           stackDepth);
            }

            Location[] targets = instr.getBranchTargets();
            if (targets != null) {
                for (int i=0; i<targets.length; i++) {
                    LabelInstruction targetInstr = 
                        (LabelInstruction)targets[i];

                    if (i == 0 && next == null) {
                        // Flow to the first target if instruction doesn't
                        // flow to its next instruction.
                        next = targetInstr;
                        continue;
                    }

                    if (!instr.isSubroutineCall()) {
                        stackResolve
                            (stackDepth, targetInstr, subAdjustMap);
                    }
                    else {
                        Integer subAdjust =
                            (Integer)subAdjustMap.get(targetInstr);
                        
                        if (subAdjust == null) {
                            int newDepth =
                                stackResolve(stackDepth, targetInstr,
                                             subAdjustMap);
                            subAdjust = new Integer(newDepth - stackDepth);
                            subAdjustMap.put(targetInstr, subAdjust);
                        }
                        
                        stackDepth += subAdjust.intValue();
                    }
                }
            }

            instr = next;
        }

        return stackDepth;
    }

    private class LocalVariableImpl implements LocalVariable {
        private String mName;
        private TypeDescriptor mType;
        private boolean mIsDoubleWord;
        
        private int mNumber;
        private boolean mFixed;

        private List mStoreInstructions;
        private SortedSet mLocationRangeSet;

        public LocalVariableImpl(String name, TypeDescriptor type,
                                 int number) {
            mName = name;
            mType = type;
            Class clazz = type.getClassArg();
            mIsDoubleWord = clazz == long.class || clazz == double.class;
            mNumber = number;
            if (number >= 0) {
                mFixed = true;
            }
            mStoreInstructions = new ArrayList();
        }
        
        /**
         * May return null if this LocalVariable is unnamed.
         */
        public String getName() {
            return mName;
        }
        
        public void setName(String name) {
            mName = name;
        }
        
        public TypeDescriptor getType() {
            return mType;
        }
        
        public boolean isDoubleWord() {
            return mIsDoubleWord;
        }
        
        public int getNumber() {
            return mNumber;
        }
        
        public void setNumber(int number) {
            mNumber = number;
        }

        public SortedSet getLocationRangeSet() {
            return mLocationRangeSet;
        }
        
        public void setLocations(Set locations) {
            /* TODO
            List sortedLocations = new ArrayList(locations);
            Collections.sort(sortedLocations);

            mLocationRangeSet = new TreeSet();
            
            Iterator it = sortedLocations.iterator();
            Instruction first = null;
            Instruction last = null;
            while (it.hasNext()) {
                Instruction instr = (Instruction)it.next();
                if (first == null) {
                    first = last = instr;
                }
                else if (last.mNext == instr) {
                    last = instr;
                }
                else {
                    if (last.mNext != null) {
                        last = last.mNext;
                    }
                    mLocationRangeSet.add(new LocationRangeImpl(first, last));
                    first = last = instr;
                }
            }

            if (first != null && last != null) {
                if (last.mNext != null) {
                    last = last.mNext;
                }
                mLocationRangeSet.add(new LocationRangeImpl(first, last));
            }

            mLocationRangeSet =
                Collections.unmodifiableSortedSet(mLocationRangeSet);
            */
        }

        public boolean isFixedNumber() {
            return mFixed;
        }

        public void addStoreInstruction(Instruction instr) {
            mStoreInstructions.add(instr);
        }

        public Iterator iterateStoreInstructions() {
            return mStoreInstructions.iterator();
        }

        public String toString() {
            if (getName() != null) {
                return String.valueOf(getType()) + ' ' + getName();
            }
            else {
                return String.valueOf(getType());
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Begin inner class definitions for instructions of the InstructionList.
    //
    /////////////////////////////////////////////////////////////////////////

    /**************************************************************************
     * An Instruction is an element in an InstructionList, and represents a 
     * Java byte code instruction.
     * 
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public abstract class Instruction implements Location {
        private int mStackAdjust;

        Instruction mPrev;
        Instruction mNext;
    
        // Indicates what the stack depth is when this instruction is reached.
        // Is -1 if not reached. Flow analysis sets this value.
        int mStackDepth = -1;

        // Indicates the address of this instruction is, or -1 if not known.
        int mLocation = -1;

        /**
         * Newly created instructions are automatically added to the
         * InstructionList.
         */
        public Instruction(int stackAdjust) {
            mStackAdjust = stackAdjust;
            add();
        }

        /**
         * This constructor allows sub-classes to disable auto-adding to the
         * InstructionList.
         */
        protected Instruction(int stackAdjust, boolean addInstruction) {
            mStackAdjust = stackAdjust;

            if (addInstruction) {
                add();
            }
        }

        /**
         * Add this instruction to the end of the InstructionList. If the
         * Instruction is already in the list, then it is moved to the end.
         */
        protected void add() {
            InstructionList.this.mResolved = false;

            if (mPrev != null) {
                mPrev.mNext = mNext;
            }

            if (mNext != null) {
                mNext.mPrev = mPrev;
            }

            mNext = null;
            
            if (InstructionList.this.mFirst == null) {
                mPrev = null;
                InstructionList.this.mFirst = this;
            }
            else {
                mPrev = InstructionList.this.mLast;
                InstructionList.this.mLast.mNext = this;
            }
            
            InstructionList.this.mLast = this;
        }

        /**
         * Insert an Instruction immediately following this one.
         */
        public void insert(Instruction instr) {
            InstructionList.this.mResolved = false;

            instr.mPrev = this;
            instr.mNext = mNext;

            mNext = instr;

            if (this == InstructionList.this.mLast) {
                InstructionList.this.mLast = instr;
            }
        }

        /**
         * Removes this Instruction from its parent InstructionList.
         */
        public void remove() {
            InstructionList.this.mResolved = false;
            
            if (mPrev != null) {
                mPrev.mNext = mNext;
            }
            
            if (mNext != null) {
                mNext.mPrev = mPrev;
            }
            
            if (this == InstructionList.this.mFirst) {
                InstructionList.this.mFirst = mNext;
            }
            
            if (this == InstructionList.this.mLast) {
                InstructionList.this.mLast = mPrev;
            }
            
            mPrev = null;
            mNext = null;
        }

        /**
         * Replace this Instruction with another one.
         */
        public void replace(Instruction replacement) {
            if (replacement == null) {
                remove();
                return;
            }

            InstructionList.this.mResolved = false;

            replacement.mPrev = mPrev;
            replacement.mNext = mNext;

            if (mPrev != null) {
                mPrev.mNext = replacement;
            }
            
            if (mNext != null) {
                mNext.mPrev = replacement;
            }
            
            if (this == InstructionList.this.mFirst) {
                InstructionList.this.mFirst = replacement;
            }
            
            if (this == InstructionList.this.mLast) {
                InstructionList.this.mLast = replacement;
            }
        }
        
        /**
         * Returns a positive, negative or zero value indicating what affect
         * this generated instruction has on the runtime stack.
         */
        public int getStackAdjustment() {
            return mStackAdjust;
        }
        
        /**
         * Returns the stack depth for when this instruction is reached. If the
         * value is negative, then this instruction is never reached.
         */
        public int getStackDepth() {
            return mStackDepth;
        }
        
        /**
         * Returns the address of this instruction or -1 if not known.
         */
        public int getLocation() {
            return mLocation;
        }
        
        /**
         * Returns all of the targets that this instruction may branch to. Not
         * all instructions support branching, and null is returned by default.
         */
        public Location[] getBranchTargets() {
            return null;
        }
        
        /**
         * Returns true if execution flow may continue after this instruction.
         * It may be a goto, a method return, an exception throw or a
         * subroutine return. Default implementation returns true.
         */
        public boolean isFlowThrough() {
            return true;
        }

        public boolean isSubroutineCall() {
            return false;
        }
        
        /**
         * Returns null if this is a pseudo instruction and no bytes are
         * generated.
         */
        public abstract byte[] getBytes();
        
        /**
         * An instruction is resolved when it has all information needed to
         * generate correct byte code.
         */
        public abstract boolean isResolved();

        public int compareTo(Object obj) {
            if (this == obj) {
                return 0;
            }
            Location other = (Location)obj;

            int loca = getLocation();
            int locb = other.getLocation();

            if (loca < locb) {
                return -1;
            }
            else if (loca > locb) {
                return 1;
            }
            else {
                return 0;
            }
        }

        /**
         * Returns a string containing the type of this instruction, the stack
         * adjustment and the list of byte codes. Unvisted instructions are
         * marked with an asterisk.
         */
        public String toString() {
            String name = getClass().getName();
            int index = name.lastIndexOf('.');
            if (index >= 0) {
                name = name.substring(index + 1);
            }
            index = name.lastIndexOf('$');
            if (index >= 0) {
                name = name.substring(index + 1);
            }
            
            StringBuffer buf = new StringBuffer(name.length() + 20);
            
            int adjust = getStackAdjustment();
            int depth = getStackDepth();

            if (depth >= 0) {
                buf.append(' ');
            }
            else {
                buf.append('*');
            }
            
            buf.append('[');
            buf.append(mLocation);
            buf.append("] ");
            
            buf.append(name);
            buf.append(" (");
            
            if (depth >= 0) {
                buf.append(depth);
                buf.append(" + ");
                buf.append(adjust);
                buf.append(" = ");
                buf.append(depth + adjust);
            }
            else {
                buf.append(adjust);
            }
            
            buf.append(") ");
            
            try {
                byte[] bytes = getBytes();
                boolean wide = false;
                if (bytes != null) {
                    for (int i=0; i<bytes.length; i++) {
                        if (i > 0) {
                            buf.append(',');
                        }
                        
                        byte code = bytes[i];
                        
                        if (i == 0 || wide) {
                            buf.append(Opcode.getMnemonic(code));
                            wide = code == Opcode.WIDE;
                        }
                        else {
                            buf.append(code & 0xff);
                        }
                    }
                }
            }
            catch (Exception e) {
            }

            return buf.toString();
        }
    }

    /**************************************************************************
     * Defines a psuedo instruction for a label. No byte code is ever generated
     * from a label. Labels are not automatically added to the list.
     * 
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class LabelInstruction extends Instruction implements Label {
        public LabelInstruction() {
            super(0, false);
        }

        /**
         * Set this label's branch location to be the current address
         * in this label's parent CodeBuilder or InstructionList.
         *
         * @return This Label.
         */
        public Label setLocation() {
            add();
            return this;
        }

        /**
         * @return -1 when not resolved yet
         */ 
        public int getLocation() throws IllegalStateException {
            int loc; 
            if ((loc = mLocation) < 0) {
                if (mPrev == null && mNext == null) {
                    throw new IllegalStateException
                        ("Label location is not set");
                }
            }

            return loc;
        }

        /**
         * Always returns null.
         */
        public byte[] getBytes() {
            return null;
        }
        
        public boolean isResolved() {
            return getLocation() >= 0;
        }
    }

    /**************************************************************************
     * Defines a code instruction and has storage for byte codes.
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class CodeInstruction extends Instruction {
        protected byte[] mBytes;
        
        public CodeInstruction(int stackAdjust) {
            super(stackAdjust);
        }
        
        protected CodeInstruction(int stackAdjust, boolean addInstruction) {
            super(stackAdjust, addInstruction);
        }

        public CodeInstruction(int stackAdjust, byte b) {
            super(stackAdjust);
            mBytes = new byte[] {b};
        }
        
        public CodeInstruction(int stackAdjust, byte[] bytes) {
            super(stackAdjust);
            mBytes = bytes;
        }
        
        public boolean isFlowThrough() {
            if (mBytes != null && mBytes.length > 0) {
                switch (mBytes[0]) {
                case Opcode.GOTO:
                case Opcode.GOTO_W:
                case Opcode.IRETURN:
                case Opcode.LRETURN:
                case Opcode.FRETURN:
                case Opcode.DRETURN:
                case Opcode.ARETURN:
                case Opcode.RETURN:
                case Opcode.ATHROW:
                    return false;
                }
            }
            
            return true;
        }
        
        public byte[] getBytes() {
            return mBytes;
        }
        
        public boolean isResolved() {
            return true;
        }
    }

    /**************************************************************************
     * Defines a branch instruction, like a goto, jsr or any conditional 
     * branch.
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class BranchInstruction extends CodeInstruction {
        private Location mTarget;
        private boolean mHasShortHop = false;
        private boolean mIsSub = false;

        public BranchInstruction(int stackAdjust, 
                                 byte opcode, Location target) {
            this(stackAdjust, true, opcode, target);
        }

        private BranchInstruction(int stackAdjust, boolean addInstruction,
                                  byte opcode, Location target) {
            super(stackAdjust, addInstruction);
            
            mTarget = target;
            
            switch (opcode) {
            case Opcode.GOTO_W:
            case Opcode.JSR_W:
                mIsSub = true;
                mBytes = new byte[5];
                mBytes[0] = opcode;
                break;
            case Opcode.JSR:
                mIsSub = true;
                // Flow through to next case.
            case Opcode.GOTO:
            case Opcode.IF_ACMPEQ:
            case Opcode.IF_ACMPNE:
            case Opcode.IF_ICMPEQ:
            case Opcode.IF_ICMPNE:
            case Opcode.IF_ICMPLT:
            case Opcode.IF_ICMPGE:
            case Opcode.IF_ICMPGT:
            case Opcode.IF_ICMPLE:
            case Opcode.IFEQ:
            case Opcode.IFNE:
            case Opcode.IFLT:
            case Opcode.IFGE:
            case Opcode.IFGT:
            case Opcode.IFLE:
            case Opcode.IFNONNULL:
            case Opcode.IFNULL:
                mBytes = new byte[3];
                mBytes[0] = opcode;
                break;
            default:
                throw new IllegalArgumentException
                    ("Opcode not a branch instruction: " + 
                     Opcode.getMnemonic(opcode));
            }
        }
        
        public Location[] getBranchTargets() {
            return new Location[] {mTarget};
        }
        
        public boolean isSubroutineCall() {
            return mIsSub;
        }

        public byte[] getBytes() {
            if (!isResolved() || mHasShortHop) {
                return mBytes;
            }
            
            int offset = mTarget.getLocation() - mLocation;
            byte opcode = mBytes[0];
            
            if (opcode == Opcode.GOTO_W || opcode == Opcode.JSR_W) {
                mBytes[1] = (byte)(offset >> 24);
                mBytes[2] = (byte)(offset >> 16);
                mBytes[3] = (byte)(offset >> 8);
                mBytes[4] = (byte)(offset >> 0);
            }
            else if (-32768 <= offset && offset <= 32767) {
                mBytes[1] = (byte)(offset >> 8);
                mBytes[2] = (byte)(offset >> 0);
            }
            else if (opcode == Opcode.GOTO || opcode == Opcode.JSR) {
                mBytes = new byte[5];
                if (opcode == Opcode.GOTO) {
                    mBytes[0] = Opcode.GOTO_W;
                }
                else {
                    mBytes[0] = Opcode.JSR_W;
                }
                mBytes[1] = (byte)(offset >> 24);
                mBytes[2] = (byte)(offset >> 16);
                mBytes[3] = (byte)(offset >> 8);
                mBytes[4] = (byte)(offset >> 0);
            }
            else {
                // The if branch requires a 32 bit offset.
                
                // Convert:
                //
                //           if <cond> goto target
                //           // reached if <cond> false
                // target:   // reached if <cond> true
                
                // to this:
                //
                //           if not <cond> goto shortHop
                //           goto_w target
                // shortHop: // reached if <cond> false
                // target:   // reached if <cond> true
                
                mHasShortHop = true;
                
                opcode = Opcode.reverseIfOpcode(opcode);
                
                mBytes[0] = opcode;
                mBytes[1] = (byte)0;
                mBytes[2] = (byte)8;
                
                // insert goto_w instruction after this one.
                insert
                    (new BranchInstruction(0, false, Opcode.GOTO_W, mTarget));
            }
            
            return mBytes;
        }
        
        public boolean isResolved() {
            return mTarget.getLocation() >= 0;
        }
    }

    /**************************************************************************
     * Defines an instruction that has a single operand which references a
     * constant in the constant pool.
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class ConstantOperandInstruction extends CodeInstruction {
        private ConstantInfo mInfo;
        
        public ConstantOperandInstruction(int stackAdjust,
                                          byte[] bytes,
                                          ConstantInfo info) {
            super(stackAdjust, bytes);
            mInfo = info;
        }
        
        public byte[] getBytes() {
            int index = mInfo.getIndex();
            
            if (index < 0) {
                throw new RuntimeException("Constant pool index not resolved");
            }
            
            mBytes[1] = (byte)(index >> 8);
            mBytes[2] = (byte)index;
            
            return mBytes;
        }
        
        public boolean isResolved() {
            return mInfo.getIndex() >= 0;
        }
    }

    /**************************************************************************
     * Defines an instruction that loads a constant onto the stack from the
     * constant pool.
     * 
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class LoadConstantInstruction extends CodeInstruction {
        private ConstantInfo mInfo;
        private boolean mWideOnly;
        
        public LoadConstantInstruction(int stackAdjust,
                                       ConstantInfo info) {
            this(stackAdjust, info, false);
        }

        public LoadConstantInstruction(int stackAdjust,
                                       ConstantInfo info,
                                       boolean wideOnly) {
            super(stackAdjust);
            mInfo = info;
            mWideOnly = wideOnly;
        }
        
        public boolean isFlowThrough() {
            return true;
        }
        
        public byte[] getBytes() {
            int index = mInfo.getIndex();
            
            if (index < 0) {
                throw new RuntimeException("Constant pool index not resolved");
            }
            
            if (mWideOnly) {
                byte[] bytes = new byte[3];
                bytes[0] = Opcode.LDC2_W;
                bytes[1] = (byte)(index >> 8);
                bytes[2] = (byte)index;
                return bytes;
            }
            else if (index <= 255) {
                byte[] bytes = new byte[2];
                bytes[0] = Opcode.LDC;
                bytes[1] = (byte)index;
                return bytes;
            }
            else {
                byte[] bytes = new byte[3];
                bytes[0] = Opcode.LDC_W;
                bytes[1] = (byte)(index >> 8);
                bytes[2] = (byte)index;
                return bytes;
            }
        }
        
        public boolean isResolved() {
            return mInfo.getIndex() >= 0;
        }
    }

    /**************************************************************************
     * Defines an instruction that contains an operand for referencing a
     * LocalVariable.
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class LocalOperandInstruction extends CodeInstruction {
        protected LocalVariable mLocal;
        
        public LocalOperandInstruction(int stackAdjust,
                                       LocalVariable local) {
            super(stackAdjust);
            mLocal = local;
        }
        
        public boolean isResolved() {
            return mLocal.getNumber() >= 0;
        }
        
        public LocalVariable getLocalVariable() {
            return mLocal;
        }
        
        public int getVariableNumber() {
            int varNum = mLocal.getNumber();
            
            if (varNum < 0) {
                throw new RuntimeException
                    ("Local variable number not resolved");
            }
            
            return varNum;
        }
    }

    /**************************************************************************
     * Defines an instruction that loads a local variable onto the stack.
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class LoadLocalInstruction extends LocalOperandInstruction {
        public LoadLocalInstruction(int stackAdjust,
                                    LocalVariable local) {
            super(stackAdjust, local);
        }
        
        public boolean isFlowThrough() {
            return true;
        }
        
        public byte[] getBytes() {
            int varNum = getVariableNumber();
            byte opcode;
            boolean writeIndex = false;

            TypeDescriptor type = mLocal.getType();
            Class clazz;
            if (type.getDimensions() > 0) {
                clazz = null;
            }
            else {
                clazz = type.getClassArg();
            }

            switch(varNum) {
            case 0:
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ALOAD_0;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LLOAD_0;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FLOAD_0;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DLOAD_0;
                }
                else {
                    opcode = Opcode.ILOAD_0;
                }
                break;
            case 1:
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ALOAD_1;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LLOAD_1;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FLOAD_1;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DLOAD_1;
                }
                else {
                    opcode = Opcode.ILOAD_1;
                }
                break;
            case 2:
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ALOAD_2;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LLOAD_2;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FLOAD_2;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DLOAD_2;
                }
                else {
                    opcode = Opcode.ILOAD_2;
                }
                break;
            case 3:
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ALOAD_3;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LLOAD_3;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FLOAD_3;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DLOAD_3;
                }
                else {
                    opcode = Opcode.ILOAD_3;
                }
                break;
            default:
                writeIndex = true;
                
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ALOAD;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LLOAD;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FLOAD;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DLOAD;
                }
                else {
                    opcode = Opcode.ILOAD;
                }
                break;
            }

            if (!writeIndex) {
                mBytes = new byte[] { opcode };
            }
            else {
                if (varNum <= 255) {
                    mBytes = new byte[] { opcode, (byte)varNum };
                }
                else {
                    mBytes = new byte[] 
                    {
                        Opcode.WIDE,
                        opcode,
                        (byte)(varNum >> 8),
                        (byte)varNum
                    };
                }
            }
            
            return mBytes;
        }
    }

    /**************************************************************************
     * Defines an instruction that stores a value from the stack into a local 
     * variable.
     * 
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class StoreLocalInstruction extends LocalOperandInstruction {
        public StoreLocalInstruction(int stackAdjust,
                                     LocalVariable local) {
            super(stackAdjust, local);
            ((LocalVariableImpl)local).addStoreInstruction(this);
        }
        
        public boolean isFlowThrough() {
            return true;
        }
        
        public byte[] getBytes() {
            int varNum = getVariableNumber();
            byte opcode;
            boolean writeIndex = false;

            TypeDescriptor type = mLocal.getType();
            Class clazz;
            if (type.getDimensions() > 0) {
                clazz = null;
            }
            else {
                clazz = type.getClassArg();
            }
            
            switch(varNum) {
            case 0:
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ASTORE_0;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LSTORE_0;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FSTORE_0;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DSTORE_0;
                }
                else {
                    opcode = Opcode.ISTORE_0;
                }
                break;
            case 1:
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ASTORE_1;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LSTORE_1;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FSTORE_1;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DSTORE_1;
                }
                else {
                    opcode = Opcode.ISTORE_1;
                }
                break;
            case 2:
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ASTORE_2;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LSTORE_2;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FSTORE_2;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DSTORE_2;
                }
                else {
                    opcode = Opcode.ISTORE_2;
                }
                break;
            case 3:
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ASTORE_3;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LSTORE_3;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FSTORE_3;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DSTORE_3;
                }
                else {
                    opcode = Opcode.ISTORE_3;
                }
                break;
            default:
                writeIndex = true;
                
                if (clazz == null || !clazz.isPrimitive()) {
                    opcode = Opcode.ASTORE;
                }
                else if (clazz == long.class) {
                    opcode = Opcode.LSTORE;
                }
                else if (clazz == float.class) {
                    opcode = Opcode.FSTORE;
                }
                else if (clazz == double.class) {
                    opcode = Opcode.DSTORE;
                }
                else {
                    opcode = Opcode.ISTORE;
                }
                break;
            }
            
            if (!writeIndex) {
                mBytes = new byte[] { opcode };
            }
            else {
                if (varNum <= 255) {
                    mBytes = new byte[] { opcode, (byte)varNum };
                }
                else {
                    mBytes = new byte[] 
                    {
                        Opcode.WIDE,
                        opcode,
                        (byte)(varNum >> 8),
                        (byte)varNum
                    };
                }
            }
            
            return mBytes;
        }
    }
    
    /**************************************************************************
     * Defines a ret instruction for returning from a jsr call. 
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class RetInstruction extends LocalOperandInstruction {
        public RetInstruction(LocalVariable local) {
            super(0, local);
        }
        
        public boolean isFlowThrough() {
            return false;
        }
        
        public byte[] getBytes() {
            int varNum = getVariableNumber();
            
            if (varNum <= 255) {
                mBytes = new byte[] { Opcode.RET, (byte)varNum };
            }
            else {
                mBytes = new byte[] 
                { 
                    Opcode.WIDE, 
                    Opcode.RET, 
                    (byte)(varNum >> 8),
                    (byte)varNum
                };
            }
            
            return mBytes;
        }
    }

    /**************************************************************************
     * Defines a specialized instruction that increments a local variable by
     * a signed 16-bit amount.
     * 
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class ShortIncrementInstruction extends LocalOperandInstruction {
        private short mAmount;
        
        public ShortIncrementInstruction(LocalVariable local, short amount) {
            super(0, local);
            mAmount = amount;
        }
        
        public boolean isFlowThrough() {
            return true;
        }
        
        public byte[] getBytes() {
            int varNum = getVariableNumber();
            
            if ((-128 <= mAmount && mAmount <= 127) && varNum <= 255) {
                mBytes = new byte[] 
                { Opcode.IINC, 
                  (byte)varNum, 
                  (byte)mAmount 
                };
            }
            else {
                mBytes = new byte[]
                {
                    Opcode.WIDE,
                    Opcode.IINC,
                    (byte)(varNum >> 8),
                    (byte)varNum,
                    (byte)(mAmount >> 8),
                    (byte)mAmount
                };
            }
            
            return mBytes;
        }
    }

    /**************************************************************************
     * Defines a switch instruction. The choice of which actual switch 
     * implementation to use (table or lookup switch) is determined 
     * automatically based on which generates to the smallest amount of bytes.
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
     */
    public class SwitchInstruction extends CodeInstruction {
        private int[] mCases;
        private Location[] mLocations;
        private Location mDefaultLocation;
        
        private byte mOpcode;
        
        private int mSmallest;
        private int mLargest;
        
        public SwitchInstruction(int[] casesParam,
                                 Location[] locationsParam,
                                 Location defaultLocation) {
            // A SwitchInstruction always adjusts the stack by -1 because it 
            // pops the switch key off the stack.
            super(-1);

            if (casesParam.length != locationsParam.length) {
                throw new IllegalArgumentException
                    ("Switch cases and locations sizes differ: " + 
                     casesParam.length + ", " + locationsParam.length);
            }
            
            mCases = new int[casesParam.length];
            System.arraycopy(casesParam, 0, mCases, 0, casesParam.length);
            
            mLocations = new Location[locationsParam.length];
            System.arraycopy(locationsParam, 0, mLocations, 
                             0, locationsParam.length);
            
            mDefaultLocation = defaultLocation;
            
            // First sort the cases and locations.
            sort(0, mCases.length - 1);
            
            // Check for duplicate cases.
            int lastCase = 0;
            for (int i=0; i<mCases.length; i++) {
                if (i > 0 && mCases[i] == lastCase) {
                    throw new RuntimeException("Duplicate switch cases: " +
                                               lastCase);
                }
                lastCase = mCases[i];
            }
            
            // Now determine which kind of switch to use.
            
            mSmallest = mCases[0];
            mLargest = mCases[mCases.length - 1];
            int tSize = 12 + 4 * (mLargest - mSmallest + 1);
            
            int lSize = 8 + 8 * mCases.length;
            
            if (tSize <= lSize) {
                mOpcode = Opcode.TABLESWITCH;
            }
            else {
                mOpcode = Opcode.LOOKUPSWITCH;
            }
        }   
        
        public Location[] getBranchTargets() {
            Location[] targets = new Location[mLocations.length + 1];
            System.arraycopy(mLocations, 0, targets, 0, mLocations.length);
            targets[targets.length - 1] = mDefaultLocation;
            
            return targets;
        }
        
        public boolean isFlowThrough() {
            return false;
        }
        
        public byte[] getBytes() {
            int length = 1;
            int pad = 3 - (mLocation & 3);
            length += pad;
            
            if (mOpcode == Opcode.TABLESWITCH) {
                length += 12 + 4 * (mLargest - mSmallest + 1);
            }
            else {
                length += 8 + 8 * mCases.length;
            }
            
            mBytes = new byte[length];
            
            if (!isResolved()) {
                return mBytes;
            }
            
            mBytes[0] = mOpcode;
            int cursor = pad + 1;
            
            int defaultOffset = mDefaultLocation.getLocation() - mLocation;
            mBytes[cursor++] = (byte)(defaultOffset >> 24);
            mBytes[cursor++] = (byte)(defaultOffset >> 16);
            mBytes[cursor++] = (byte)(defaultOffset >> 8);
            mBytes[cursor++] = (byte)(defaultOffset >> 0);
            
            if (mOpcode == Opcode.TABLESWITCH) {
                mBytes[cursor++] = (byte)(mSmallest >> 24);
                mBytes[cursor++] = (byte)(mSmallest >> 16);
                mBytes[cursor++] = (byte)(mSmallest >> 8);
                mBytes[cursor++] = (byte)(mSmallest >> 0);
                
                mBytes[cursor++] = (byte)(mLargest >> 24);
                mBytes[cursor++] = (byte)(mLargest >> 16);
                mBytes[cursor++] = (byte)(mLargest >> 8);
                mBytes[cursor++] = (byte)(mLargest >> 0);
                
                int index = 0;
                for (int case_ = mSmallest; case_ <= mLargest; case_++) {
                    if (case_ == mCases[index]) {
                        int offset = 
                            mLocations[index].getLocation() - mLocation;
                        mBytes[cursor++] = (byte)(offset >> 24);
                        mBytes[cursor++] = (byte)(offset >> 16);
                        mBytes[cursor++] = (byte)(offset >> 8);
                        mBytes[cursor++] = (byte)(offset >> 0);
                        
                        index++;
                    }
                    else {
                        mBytes[cursor++] = (byte)(defaultOffset >> 24);
                        mBytes[cursor++] = (byte)(defaultOffset >> 16);
                        mBytes[cursor++] = (byte)(defaultOffset >> 8);
                        mBytes[cursor++] = (byte)(defaultOffset >> 0);
                    }
                }
            }
            else {
                mBytes[cursor++] = (byte)(mCases.length >> 24);
                mBytes[cursor++] = (byte)(mCases.length >> 16);
                mBytes[cursor++] = (byte)(mCases.length >> 8);
                mBytes[cursor++] = (byte)(mCases.length >> 0);
                
                for (int index = 0; index < mCases.length; index++) {
                    int case_ = mCases[index];
                    
                    mBytes[cursor++] = (byte)(case_ >> 24);
                    mBytes[cursor++] = (byte)(case_ >> 16);
                    mBytes[cursor++] = (byte)(case_ >> 8);
                    mBytes[cursor++] = (byte)(case_ >> 0);
                    
                    int offset = mLocations[index].getLocation() - mLocation;
                    mBytes[cursor++] = (byte)(offset >> 24);
                    mBytes[cursor++] = (byte)(offset >> 16);
                    mBytes[cursor++] = (byte)(offset >> 8);
                    mBytes[cursor++] = (byte)(offset >> 0);
                }
            }
            
            return mBytes;
        }

        public boolean isResolved() {
            if (mDefaultLocation.getLocation() >= 0) {
                for (int i=0; i<mLocations.length; i++) {
                    if (mLocations[i].getLocation() < 0) {
                        break;
                    }
                }
                
                return true;
            }
            
            return false;
        }
        
        private void sort(int left, int right) {
            if (left >= right) {
                return;
            }
            
            swap(left, (left + right) / 2); // move middle element to 0
            
            int last = left;
            
            for (int i = left + 1; i <= right; i++) {
                if (mCases[i] < mCases[left]) {
                    swap(++last, i);
                }
            }
            
            swap(left, last);
            sort(left, last-1);
            sort(last + 1, right);
        }
        
        private void swap(int i, int j) {
            int tempInt = mCases[i];
            mCases[i] = mCases[j];
            mCases[j] = tempInt;
            
            Location tempLocation = mLocations[i];
            mLocations[i] = mLocations[j];
            mLocations[j] = tempLocation;
        }
    }
}
