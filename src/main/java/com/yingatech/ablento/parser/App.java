package com.yingatech.ablento.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Vector;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
public class App 
{
    public static void main( String[] args )
    {
        // Get all sources
        ArrayList<Source> sources = new ArrayList<Source>();
        try (Stream<Path> walk = Files.walk(Paths.get("src"))) {
            walk.filter(path->path.toFile().isFile()).filter(path->path.toFile().getName().endsWith(".src.yaml")).map(Path::toFile).forEach(file->{
                try {
                    System.out.println("Parsing " + file.getAbsolutePath());
                    sources.add(Source.read(file));
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
        try(FileWriter fw = new FileWriter(new File("output/Dockerfile"))) {
            fw.append("FROM alpine AS remote-env\n");
            for (Source source : sources) {
                try {
                    System.out.println("Saving " + source.getName());
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
    }
}
