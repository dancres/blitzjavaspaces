/*
 * LineTooLongException.java
 * 
 * Copyright (c) 2000 Walt Disney Internet Group. All Rights Reserved.
 * 
 * Original author: Brian S O'Neill
 * 
 * $Workfile:: LineTooLongException.java                                      $
 *   $Author: dan $
 * $Revision: 1.1 $
 *     $Date: Mon, 13 Oct 2003 12:20:39 +0100 $
 */

package com.go.trove.net;

/******************************************************************************
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/12/06 <!-- $-->
 */
public class LineTooLongException extends java.io.IOException {
    public LineTooLongException(int limit) {
        super("> " + limit);
    }
}
