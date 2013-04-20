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

package com.go.trove;

import java.util.Date;

public final class PackageInfo {
    // The following fields are automatically assigned by PackageInfoCreator.
    private static final String BASE_DIRECTORY = null;
    private static final String REPOSITORY = null;
    private static final String USERNAME = null;
    private static final String BUILD_MACHINE = null;
    private static final String GROUP = null;
    private static final String PROJECT = null;
    private static final String BUILD_LOCATION = null;
    private static final String PRODUCT = "Trove";
    private static final String PRODUCT_VERSION = "1.0.x";
    private static final String BUILD_NUMBER = null;
    private static final Date BUILD_DATE = null;

    /**
     * Prints all the PackageInfo properties to standard out.
     */
    public static void main(String[] args) {
        System.out.println("Base Directory: " + getBaseDirectory());
        System.out.println("Repository: " + getRepository());
        System.out.println("Username: " + getUsername());
        System.out.println("Build Machine: " + getBuildMachine());
        System.out.println("Group: " + getGroup());
        System.out.println("Project: " + getProject());
        System.out.println("Build Location: " + getBuildLocation());
        System.out.println("Product: " + getProduct());
        System.out.println("Product Version: " + getProductVersion());
        System.out.println("Build Number: " + getBuildNumber());
        System.out.println("Build Date: " + getBuildDate());
        System.out.println();
        System.out.println("Specification Title: " + getSpecificationTitle());
        System.out.println("Specification Version: " + getSpecificationVersion());
        System.out.println("Specification Vendor: " + getSpecificationVendor());
        System.out.println("Implementation Title: " + getImplementationTitle());
        System.out.println("Implementation Version: " + getImplementationVersion());
        System.out.println("Implementation Vendor: " + getImplementationVendor());
    }

    public static String getSpecificationTitle() {
        return PRODUCT;
    }

    public static String getSpecificationVersion() {
        return PRODUCT_VERSION;
    }

    public static String getSpecificationVendor() {
        return "GO.com";
    }

    public static String getImplementationTitle() {
        return PRODUCT;
    }

    public static String getImplementationVersion() {
        if (BUILD_NUMBER != null) {
            if (getSpecificationVersion() != null) {
                return getSpecificationVersion() + '.' + BUILD_NUMBER;
            }
            else {
                return BUILD_NUMBER;
            }
        }
        else {
            return getSpecificationVersion();
        }
    }

    public static String getImplementationVendor() {
        if (GROUP != null) {
            return getSpecificationVendor() + ' ' + GROUP;
        }
        else {
            return getSpecificationVendor();
        }
    }

    public static String getBaseDirectory() {
        return BASE_DIRECTORY;
    }

    public static String getRepository() {
        return REPOSITORY;
    }

    public static String getUsername() {
        return USERNAME;
    }

    public static String getBuildMachine() {
        return BUILD_MACHINE;
    }

    public static String getGroup() {
        return GROUP;
    }

    public static String getProject() {
        return PROJECT;
    }

    public static String getBuildLocation() {
        return BUILD_LOCATION;
    }

    public static String getProduct() {
        return PRODUCT;
    }

    public static String getProductVersion() {
        return PRODUCT_VERSION;
    }

    public static String getBuildNumber() {
        return BUILD_NUMBER;
    }

    public static Date getBuildDate() {
        return BUILD_DATE;
    }
}
