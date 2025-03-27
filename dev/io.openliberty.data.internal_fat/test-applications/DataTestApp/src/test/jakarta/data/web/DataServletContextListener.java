/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Servlet context listener that uses Jakarta Data to pre-populate the table
 * that is used by the Prime entity.
 */
@WebListener
public class DataServletContextListener implements ServletContextListener {

    @Inject
    private Primes primes;

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.out.println("DataServletContextListener.contextInitialized:" +
                           " populate tables for " + primes.toString());

        // Do not add or remove from this data in tests.
        // Tests must be able to rely on this data always being present.
        primes.persist(new Prime(2, "2", "10", 1, "II", "two"),
                       new Prime(3, "3", "11", 2, "III", "three"),
                       new Prime(5, "5", "101", 2, "V", "five"),
                       new Prime(7, "7", "111", 3, "VII", "seven"),
                       new Prime(11, "B", "1011", 3, "XI", "eleven"),
                       new Prime(13, "D", "1101", 3, "XIII", "thirteen"),
                       new Prime(17, "11", "10001", 2, "XVII", "seventeen"),
                       new Prime(19, "13", "10011", 3, "XIX", "nineteen"),
                       new Prime(23, "17", "10111", 4, "XXIII", "twenty-three"),
                       new Prime(29, "1D", "11101", 4, "XXIX", "twenty-nine"),
                       new Prime(31, "1F", "11111", 5, "XXXI", "thirty-one"),
                       new Prime(37, "25", "100101", 3, "XXXVII", "thirty-seven"),
                       new Prime(41, "29", "101001", 3, "XLI", "forty-one"),
                       new Prime(43, "2B", "101011", 4, "XLIII", "forty-three"),
                       new Prime(47, "2F", "101111", 5, "XLVII", "forty-seven"),
                       // romanNumeralSymbols null:
                       new Prime(4001, "FA1", "111110100001", 7, null, "four thousand one"),
                       // romanNumeralSymbols null:
                       new Prime(4003, "FA3", "111110100011", 8, null, "four thousand three"),
                       // romanNumeralSymbols null:
                       new Prime(4007, "Fa7", "111110100111", 9, null, "four thousand seven"),
                       // empty list of romanNumeralSymbols:
                       new Prime(4013, "FAD", "111110101101", 9, "", "Four Thousand Thirteen"),
                       // empty list of romanNumeralSymbols:
                       new Prime(4019, "FB3", "111110110011", 9, "", "four thousand nineteen"),
                       // extra blank space at beginning and end:
                       new Prime(4021, "FB5", "111110110101", 9, "", " Four thousand twenty-one "));
    }

}
