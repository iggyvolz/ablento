package com.yingatech.ablento.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
public class App 
{
    public static void main( String[] args )
    {
        // Overwrite Dockerfile with base
        try {
            Files.copy(new File("base.Dockerfile").toPath(), new File("output/Dockerfile").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("Could not copy base dockerfile: " + e.getMessage());
            return;
        }
        // Get all sources
        HashMap<String, Source> sources = new HashMap<>();
        try (Stream<Path> walk = Files.walk(Paths.get("src"))) {
            walk.filter(path->path.toFile().isFile()).filter(path->path.toFile().getName().endsWith(".src.yaml")).map(Path::toFile).forEach(file->{
                try {
                    System.out.println("Parsing " + file.getAbsolutePath());
                    Source source = Source.read(file);
                    sources.put(source.getName(), source);
                } catch(InvalidSourceException e) {
                    System.out.println("Error in source " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            });
        } catch(IOException e) {
            System.out.println("I/O exception while searching for files: " + e.getMessage());
            return;
        }
        // System.out.println(ReflectionToStringBuilder.toString(sources.get(1),ToStringStyle.MULTI_LINE_STYLE));
        // Write all sources
        try(FileWriter fw = new FileWriter(new File("output/Dockerfile"), true)) {
            fw.append("FROM alpine AS source-env\n");
            for (Source source : sources.values()) {
                try {
                    System.out.println("Writing source " + source.getName());
                    source.save(fw);
                } catch(InvalidSourceException e) {
                    System.out.println("Invalid source: " + e.getMessage());
                    return;
                }
            }
        } catch(IOException e) {
            System.out.println("Input/output exception: " + e.getMessage());
        }
        // System.out.println(ReflectionToStringBuilder.toString(source,ToStringStyle.MULTI_LINE_STYLE));


        // Get all packages
        ArrayList<Package> packages = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Paths.get("src"))) {
            walk.filter(path->path.toFile().isFile()).filter(path->path.toFile().getName().endsWith(".pkg.yaml")).map(Path::toFile).forEach(file->{
                try {
                    System.out.println("Parsing " + file.getAbsolutePath());
                    Package pkg = Package.read(file);
                    packages.add(pkg);
                } catch(InvalidPackageException e) {
                    System.out.println("Error in source " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            });
        } catch(IOException e) {
            System.out.println("I/O exception while searching for files: " + e.getMessage());
            return;
        }

        Set<String> sourceNames = new HashSet<>(sources.keySet());
        // Add builtin sources
        sourceNames.add("base");

        ArrayList<Package> packagesSorted = new ArrayList<>();
        while(packages.size() > 0) {
            // loop detector
            boolean broken = false;
            for(Package pkg : packages) {
                // Check if packagesSorted has this object's parent
                String pkgFrom = pkg.getFrom();
                boolean hasParent = pkgFrom.equals("base");
                for(Package parent : packagesSorted) {
                    if(parent.getName().equals(pkgFrom)) {
                        hasParent = true;
                    }
                }
                if(hasParent) {
                    broken = true;
                    packagesSorted.add(pkg);
                    packages.remove(pkg);
                    break;
                }
            }
            if(!broken) {
                System.out.println("Infinite loop detected");
                return;
            }
        }

        System.out.print("Writing packages in order: ");
        boolean first = true;
        for(Package pkg : packagesSorted) {
            if(first) {
                first = false;
            } else {
                System.out.print(", ");
            }
            System.out.print(pkg.getName());
        }

        // Write all packages
        try(FileWriter fw = new FileWriter(new File("output/Dockerfile"), true)) {
            for (Package pkg: packagesSorted) {
                try {
                    System.out.println("Writing package " + pkg.getName());
                    pkg.save(fw, sourceNames);
                } catch(InvalidPackageException e) {
                    System.out.println("Invalid package: " + e.getMessage());
                    return;
                }
            }
        } catch(IOException e) {
            System.out.println("Input/output exception: " + e.getMessage());
        }

    }
}
