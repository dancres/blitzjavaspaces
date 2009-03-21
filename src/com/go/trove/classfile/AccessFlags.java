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

import java.lang.reflect.Modifier;

/******************************************************************************
 * The AccessFlags class is a wrapper around a Modifier bit mask. The
 * methods provided to manipulate the Modifier ensure that it is always
 * legal. i.e. setting it public automatically clears it from being
 * private or protected.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class AccessFlags extends Modifier implements Cloneable {
    private int mFlags;
    
    /** Construct with a modifier of 0. */
    public AccessFlags() {
        mFlags = 0;
    }

    public AccessFlags(int modifier) {
        mFlags = modifier;
    }
    
    public final int getModifier() {
        return mFlags;
    }
    
    public void setModifier(int flags) {
        mFlags = flags;
    }
    
    public boolean isPublic() {
        return isPublic(mFlags);
    }

    /**
     * When set public, it is cleared from being private or protected.
     */
    public void setPublic(boolean value) {
        if (value) {
            mFlags |= PUBLIC;
            mFlags &= ~PROTECTED & ~PRIVATE;
        }
        else {
            mFlags &= ~PUBLIC;
        }
    }
    
    public boolean isPrivate() {
        return isPrivate(mFlags);
    }

    /**
     * When set private, it is cleared from being public or protected.
     */
    public void setPrivate(boolean value) {
        if (value) {
            mFlags |= PRIVATE;
            mFlags &= ~PUBLIC & ~PROTECTED;
        }
        else {
            mFlags &= ~PRIVATE;
        }
    }

    public boolean isProtected() {
        return isProtected(mFlags);
    }
    
    /**
     * When set protected, it is cleared from being public or private.
     */
    public void setProtected(boolean value) {
        if (value) {
            mFlags |= PROTECTED;
            mFlags &= ~PUBLIC & ~PRIVATE;
        }
        else {
            mFlags &= ~PROTECTED;
        }
    }
    
    public boolean isStatic() {
        return isStatic(mFlags);
    }

    public void setStatic(boolean value) {
        if (value) {
            mFlags |= STATIC;
        }
        else {
            mFlags &= ~STATIC;
        }
    }

    public boolean isFinal() {
        return isFinal(mFlags);
    }

    /**
     * When set final, it is cleared from being an interface or abstract.
     */
    public void setFinal(boolean value) {
        if (value) {
            mFlags |= FINAL;
            mFlags &= ~INTERFACE & ~ABSTRACT;
        }
        else {
            mFlags &= ~FINAL;
        }
    }
    
    public boolean isSynchronized() {
        return isSynchronized(mFlags);
    }

    public void setSynchronized(boolean value) {
        if (value) {
            mFlags |= SYNCHRONIZED;
        }
        else {
            mFlags &= ~SYNCHRONIZED;
        }
    }
    
    public boolean isVolatile() {
        return isVolatile(mFlags);
    }

    public void setVolatile(boolean value) {
        if (value) {
            mFlags |= VOLATILE;
        }
        else {
            mFlags &= ~VOLATILE;
        }
    }
    
    public boolean isTransient() {
        return isTransient(mFlags);
    }

    public void setTransient(boolean value) {
        if (value) {
            mFlags |= TRANSIENT;
        }
        else {
            mFlags &= ~TRANSIENT;
        }
    }
    
    public boolean isNative() {
        return isNative(mFlags);
    }

    public void setNative(boolean value) {
        if (value) {
            mFlags |= NATIVE;
        }
        else {
            mFlags &= ~NATIVE;
        }
    }
    
    public boolean isInterface() {
        return isInterface(mFlags);
    }

    /**
     * When set as an interface, it is cleared from being final and set as
     * being abstract.
     */
    public void setInterface(boolean value) {
        if (value) {
            mFlags |= INTERFACE | ABSTRACT;
            mFlags &= ~FINAL;
        }
        else {
            mFlags &= ~INTERFACE;
        }
    }
    
    public boolean isAbstract() {
        return isAbstract(mFlags);
    }

    /**
     * When set abstract, it is cleared from being final. When cleared from
     * being abstract, it is also cleared from being an interface.
     */
    public void setAbstract(boolean value) {
        if (value) {
            mFlags |= ABSTRACT;
            mFlags &= ~FINAL;
        }
        else {
            mFlags &= ~ABSTRACT & ~INTERFACE;
        }
    }

    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
    
    /**
     * Returns the string value generated by the Modifier class.
     * @see java.lang.reflect.Modifier#toString()
     */
    public String toString() {
        return toString(mFlags);
    }
}
