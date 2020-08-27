package com.yingatech.ablento.parser;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

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
    private Remote[] remotes;

    public void save(Appendable writer) throws IOException, InvalidSourceException {
        if(name == null || !Pattern.matches("^[a-zA-Z_]+$", name)) {
            throw new InvalidSourceException("Illegal name " + name);
        }
        // Set up source building context
        writer.append("FROM source-env AS ").append(name).append("-src-build\n");
        writer.append("RUN mkdir /context\n");
        // Save all remotes
        for (Remote remote : remotes) {
            remote.save(writer);
        }
        // Take only /context dir for final package
        writer.append("FROM scratch AS ").append(name).append("-src\n");
        writer.append("COPY --from=").append(name).append("-src-build /context /\n");
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