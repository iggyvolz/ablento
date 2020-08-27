package com.yingatech.ablento.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Package {
    @JsonProperty
    private String name;
    public String getName() {
        return name;
    }
    @JsonProperty
    private String from;
    @JsonProperty
    private String source;
    private Map<String,String> getSources(Collection<String> validSources) throws InvalidPackageException {
        if(source == null) {
            throw new InvalidPackageException("Source must be specified");
        }
        HashMap<String,String> sourceMap = new HashMap<>();
        String[] sources = source.split(",");
        for(String src : sources) {
            String destination = "/";
            if(src.contains(":")) {
                String[] spl = src.split(":");
                src = spl[0];
                destination = spl[1];
            }
            if(!validSources.contains(src)) {
                throw new InvalidPackageException("Invalid source " + src);
            }
            sourceMap.put(src, destination);
        }
        return sourceMap;
    }
    @JsonProperty
    private String[] script;
    @JsonProperty
    private String[] test;

    public static Package read(File file) throws InvalidPackageException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(file, Package.class);
        } catch(IOException e) {
            throw new InvalidPackageException("Input/output exception in source: " + e.getMessage());
        }
    }

    public void save(Appendable writer, Collection<String> validSources) throws IOException, InvalidPackageException {
        if(name == null || !Pattern.matches("^[a-zA-Z_]+$", name)) {
            throw new InvalidPackageException("Illegal name " + name);
        }
        // Set up package context
        writer.append("FROM ").append(from).append(" AS ").append(name).append("\n");
        // Copy in sources
        for(Map.Entry<String,String> entry : getSources(validSources).entrySet())
        {
            writer.append("COPY --chown=lfs:lfs --from=").append(entry.getKey()).append("-src / $BUILDDIR/").append(entry.getValue()).append("\n");
        }
        // Set working directory
        writer.append("WORKDIR $BUILDDIR\n");
        // Run install scripts
        if(script == null) {
            throw new InvalidPackageException("Script is required");
        }
        for(String line : script) {
            writer.append("RUN ").append(line).append("\n");
        }
        // Run test scripts
        if(test != null) {
            for(String line : test) {
                writer.append("RUN ").append(line).append("\n");
            }
        }
        writer.append("RUN rm -R $BUILDDIR;mkdir $BUILDDIR\n");
    }
}
