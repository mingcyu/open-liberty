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
package com.ibm.ws.kernel.server.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Stream;

import org.apache.felix.service.command.Descriptor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.wsspi.logging.Introspector;

/**
 * An 'inspect' command for Liberty's OSGi Console
 */
@Component(
           service = Object.class,
           configurationPolicy = IGNORE,
           property = {
                        "osgi.command.scope=kernel",
                        "osgi.command.function=inspect",
                        "service.vendor=IBM"
           })
public class InspectCommand {
    private final List<Introspector> introspectors;

    @Activate
    public InspectCommand(@Reference(name = "introspectors", policyOption = ReferencePolicyOption.GREEDY) List<Introspector> introspectors) {
        this.introspectors = introspectors;
    }

    @Descriptor("Lists or invokes the known introspections available in this server. Run `introspect help' for more information.")
    public void inspect(String... args) {
        if (args.length == 0) {
            listIntrospections();
        } else if ("help".equals(args[0])) {
            if (args.length == 1) {
                System.out.println("usage: inspect");
                System.out.println("       Lists available introspections.");
                System.out.println();
                System.out.println("usage: inspect <introspection>");
                System.out.println("       Displays the output of the named introspection.");
                System.out.println();
                System.out.println("usage: inspect help");
                System.out.println("       Displays this usage message.");
                System.out.println();
                System.out.println("usage: inspect help <introspection>");
                System.out.println("       Displays the description of the named introspection.");
            } else {
                Stream.of(args).skip(1).forEach(this::describeIntrospection);
            }
        } else {
            Stream.of(args).forEach(this::displayIntrospection);
        }
    }

    private Stream<Introspector> stream() {
        return introspectors.stream();
    }

    private Introspector[] findMatchingIntrospections(String name) {
        Introspector[] matches = stream().filter(i -> name.equals(i.getIntrospectorName())).toArray(Introspector[]::new);
        if (matches.length == 0) {
            System.err.println("error: could not find an introspector matching '" + name + "'");
        }
        return matches;
    }

    private void listIntrospections() {
        System.out.println("Available introspections: ");
        stream().map(Introspector::getIntrospectorName).sorted().map("\t"::concat).forEach(System.out::println);
    }

    private void displayIntrospection(String name) {
        try (PrintWriter sysout = new PrintWriter(System.out)) {
            for (Introspector ii : findMatchingIntrospections(name)) {
                System.out.println("Introspector: " + ii.getIntrospectorName());
                System.out.println("============= " + ii.getIntrospectorName().replaceAll(".", "="));
                try {
                    ii.introspect(sysout);
                } catch (Exception e) {
                    System.err.println("Failed generating introspection for " + ii.getIntrospectorName());
                    e.printStackTrace();
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.err.println("error: failed to create print writer" + e);
            e.printStackTrace();
        }
    }

    private void describeIntrospection(String name) {
        for (Introspector ii : findMatchingIntrospections(name)) {
            System.out.println("Introspector: " + ii.getIntrospectorName());
            System.out.println("============= " + ii.getIntrospectorName().replaceAll(".", "="));
            System.out.println(ii.getIntrospectorDescription());
            System.out.println();
        }
    }
}
