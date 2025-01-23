/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package io.openliberty.org.testcontainers.generate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

/**
 * Allows external contributors a convenient way to build the custom images we use in our build
 */
public class CustomImages {
    // Ensures when we look for cached images Docker doesn't attempt to reach out to docker.io
    public static final String LOCAL_REGISTRY = "localhost";

    // The repository where all Open Liberty images will be cached
    public static final String REPOSITORY = "openliberty/testcontainers/";
    
    // The --build-arg necessary to overwrite the default BASE_IMAGE in the Dockerfile
    // with the mirrored image in artifactory
    public static final String BASE_IMAGE = "BASE_IMAGE";
    
    public static void main(String[] args) {
          long start = System.currentTimeMillis();
          
          if(args == null || args[0] == null || args.length > 1) {
              throw new RuntimeException("CustomImages expects a single argument (projectPath) which is the path to the io.openliberty.org.testcontainers project.");
          }
          
          //Get data from calling script
          String projectPath = args[0];
          
          //Where to find instructions to build images
          Path commonPath = Paths.get(projectPath, "resources", REPOSITORY);
          
          // Construct a list of Dockerfiles
          List<Path> Dockerfiles = findDockerfiles(commonPath);
          
          // Construct map of docker image names to Dockerfiles
          Map<DockerImageName, Path> DockerfileMap = Dockerfiles.stream()
                  .collect(Collectors.toMap(Dockerfile -> constructImageName(Dockerfile, commonPath), Dockerfile -> Dockerfile));
          
          // TODO Construct map of docker image names to base image names
//          Map<DockerImageName, DockerImageName> baseImageMap = DockerfileMap.entrySet().stream()
//                  .collect(Collectors.toMap(entry -> entry.getKey(), entry -> findBaseImage(entry.getValue())));
          
          // Find or build all images
          for(DockerImageName name : DockerfileMap.keySet()) {
              Path Dockerfile = DockerfileMap.get(name);
//              DockerImageName baseImage = baseImageMap.get(name);
              
              ImageFromDockerfile img = new ImageFromDockerfile(name.asCanonicalNameString(), false)
                      .withDockerfile(Dockerfile);
//                      .withBuildArg(BASE_IMAGE, baseImage.asCanonicalNameString());
              
              try {
            	  System.out.println("Building image: " + name.asCanonicalNameString());
                  img.get();
              } catch (Exception e) {
                  throw new RuntimeException("Could not build or find image " + name.asCanonicalNameString(), e);
              }
          }
        
        long end = System.currentTimeMillis();
        System.out.println( "Execution time in ms: " + ( end - start ));
    }
    
    /**
     * Walk through all files nested within a shared path and find every Dockerfile.
     * 
     * @param commonPath the shared path within which all Dockerfiles are nested within
     * @return A list of paths to every Dockerfile
     */
    private static List<Path> findDockerfiles(Path commonPath) {
        final String FILE_NAME = "Dockerfile";
        final List<Path> Dockerfiles = new ArrayList<>();
        

        try (Stream<Path> paths = Files.walk(commonPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(FILE_NAME))
                .forEach(Dockerfiles::add);
        } catch (IOException e) {
            throw new RuntimeException("Error searching files: " + e.getMessage());
        }

        return Dockerfiles;
    }
    
    /**
     * Using the Dockerfile's path, parse the directory structure to construct a
     * fully qualified DockerImageName to be associated with this Dockerfile.
     * 
     * @param Dockerfile to be built
     * @param commonPath the shared path within which all Dockerfiles are nested within
     * @return The DockerImageName for this Dockerfile
     */
    private static DockerImageName constructImageName(Path Dockerfile, Path commonPath) {
        final String fullPath = Dockerfile.toString();
        
        // Find version
        int end = fullPath.lastIndexOf('/');
        int start = fullPath.substring(0, end).lastIndexOf('/') + 1;
        final String version = fullPath.substring(start, end);
        
        // Find repository
        end = fullPath.indexOf(version) - 1;
        start = commonPath.toString().length() + ( commonPath.toString().endsWith("/") ? 0 : 1);
        final String repository = fullPath.substring(start, end);
        
        // Construct and return name
        DockerImageName name = DockerImageName.parse(repository).withTag(version).withRegistry(LOCAL_REGISTRY);
        System.out.println("Found image to build: " + name.asCanonicalNameString());
        return name;
    }

    //TODO consider having a method to overwrite the BASE_IMAGE argument when running on a build system.
//    /**
//     * Read the Dockerfile and find the default value for the BASE_IMAGE argument.
//     * Return the substituted BASE_IMAGE when run on a build system.
//     * Otherwise, return the original BASE_IMAGE.
//     * 
//     * @param Dockerfile to be built
//     * @return the BASE_IMAGE (substituted or original pending environment)
//     */
//    private static DockerImageName findBaseImage(Path Dockerfile) {
//        final String prefix = "ARG BASE_IMAGE=\"";
//        
//        String argLine;
//        try {
//            argLine = Files.lines(Dockerfile)
//                .filter(line -> line.startsWith(prefix))
//                .findFirst()
//                .orElseThrow(() -> new NoSuchElementException("Could not find line that starts with " + prefix + " in file " + Dockerfile))
//                .trim();
//        } catch (IOException e) {
//            throw new RuntimeException("Could not read file: " + Dockerfile.toString());
//        }
//        
//        String fromImgString = argLine.substring(prefix.length(), argLine.length() -1);
//        
//        DockerImageName original = DockerImageName.parse(fromImgString);
//        DockerImageName substituted = ImageNameSubstitutor.instance().apply(original);
//        
//        if(!original.equals(substituted)) {
//            // Substitutor was used, also prepend the registry.
//            return substituted.withRegistry(System.getenv("IMAGE_NAME_PREFIX"));
//        } else {
//            return original;
//        }
//    }
}
