package com.yingatech.ablento.parser;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Source {
    @JsonProperty
    private String name;
    public String getName() {
        return name;
    }
    @JsonProperty
    private String version;
    @JsonProperty
    private Remote[] remotes;

    public void save(Appendable writer) throws IOException, InvalidSourceException {
        for (Remote remote : remotes) {
            writer.append(remote.getIdentifier() + "\n");
        }
    }
    public static Source read(File file) throws InvalidSourceException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(file, Source.class);
        } catch(IOException e) {
            throw new InvalidSourceException("Input/output exception in source: " + e.getMessage());
        }
    }
}