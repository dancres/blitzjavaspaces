package org.dancres.util;

import java.lang.reflect.Method;

/**
   Convenience methods for use with reflection
 */
public class ReflectUtil {
    public static Method findMethod(Class aType, String aName,
                                    Class[] aParameterTypes) {

        try {
            return aType.getMethod(aName, aParameterTypes);
        } catch (NoSuchMethodException aNSME) {
            Error myError = new NoSuchMethodError("Couldn't find method");

            // Damn, can't pass exception into constructor - why! :(
            myError.initCause(aNSME);

            throw myError;
        }
    }
}
