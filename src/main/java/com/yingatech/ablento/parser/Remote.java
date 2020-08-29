package com.yingatech.ablento.parser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.codec.binary.Hex;

/**
 * Representation of a remote resource, identified by the URL that it can be fetched at, as well as a hash to verify the integrity
 */
public class Remote {
    @JsonProperty
    private String url;
    @JsonProperty
    private String sha2;
    @JsonProperty
    private String sha3;
    @JsonProperty
    private String destination;
    @JsonProperty
    private Boolean extract;
    @JsonProperty
    private Boolean strip;
    /**
     * Computes the identifier name
     * @return Filesystem-safe name which identifies this resource
     */
    private String getIdentifierName() throws InvalidSourceException {
        if(sha3 != null) {
            if(!Pattern.matches("^[0-9a-z]{64}$", sha3)) {
                throw new InvalidSourceException("Invalid SHA3 hash");
            }
            // Compute from SHA2
            return "sha3-" + sha3;
        }
        if(sha2 != null) {
            if(!Pattern.matches("^[0-9a-z]{64}$", sha2)) {
                throw new InvalidSourceException("Invalid SHA2 hash");
            }
            // Compute from SHA2
            return "sha2-" + sha2;
        }
        throw new InvalidSourceException("No hash specified");
    }
    /**
     * Downloads the remote to be stored locally
     */
    private File download(String identifier) throws IOException {
        ReadableByteChannel input = Channels.newChannel(new URL(url).openStream());
        File file = new File("output/"+identifier);
        if(!file.exists()) {
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.getChannel().transferFrom(input, 0, Long.MAX_VALUE);
            }
        }
        return file;
    }
    /**
     * Gets the identifier for the remote
     * @return Identifier for the remote, located in remotes/
     */
    public String getIdentifier() throws InvalidSourceException {
        String identifier = getIdentifierName();
        // Download URL 
        if(url == null) {
            throw new InvalidSourceException("No URL specified");
        }
        File file;
        try {
            file = download(identifier);
        } catch(IOException e) {
            throw new InvalidSourceException("Input/output exception: " + e.getMessage());
        }
        checkHashes(file);
        return identifier;
    }
    private String getHash(File file, String digest) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance(digest);
        try(FileInputStream fileStream = new FileInputStream(file); DigestInputStream digestStream = new DigestInputStream(fileStream, messageDigest)) {
            digestStream.readAllBytes();
        }
        byte[] bytes = messageDigest.digest();
        return Hex.encodeHexString(bytes);
    }

    private void checkHash(File file, String digestName, String hash) throws InvalidSourceException {
        String got;
        try {
            got = getHash(file, digestName).toLowerCase();
        } catch(NoSuchAlgorithmException e) {
            throw new InvalidSourceException("Could not check " + digestName + " hash: algorithm not available");
        } catch(IOException e) {
            throw new InvalidSourceException("Input/output error while checking " + digestName + " hash: " + e.getMessage());
        }
        String expected = hash.toLowerCase();
        if( !got.equals(expected) ) {
            throw new InvalidSourceException("Incorrect " + digestName + " hash: got " + got + " and expected " + expected);
        }
    }

    private void checkHashes(File file) throws InvalidSourceException {
        // Check SHA-2
        if(sha2 != null) {
            checkHash(file, "SHA-256", sha2);
        }
        // Check SHA-3
        if(sha3 != null) {
            checkHash(file, "SHA3-256", sha3);
        }
    }


    public void save(Appendable writer) throws InvalidSourceException, IOException {
        // Build remote as stage
        String identifier = getIdentifier();
        String basename;
        try {
            basename = Paths.get(new URI(url).getPath()).getFileName().toString();
        } catch(URISyntaxException e) {
            throw new InvalidSourceException("Invalid URI : " + e.getMessage());
        }
        if(extract == null) {
            // Compute default value from basename of URL
            extract = basename.endsWith(".tar.gz") || basename.endsWith(".tar.xz") || basename.endsWith(".tar.bz2") || basename.endsWith(".tar") || basename.endsWith(".zip");
        }
        if(destination == null) {
            destination = extract ? "/" : ("/" + basename);
        }
        if(extract) {
            writer.append("COPY ").append(identifier).append(" /context-compressed\n");
            writer.append("RUN mkdir -p /context/").append(destination).append(" && cd /context/").append(destination).append(" && tar xf /context-compressed || unzip /context-compressed\n");
        } else {
            writer.append("COPY ").append(identifier).append(" /context").append(destination).append("\n");
        }
        if(strip == null || strip) {
            // Detect whether to strip, and strip a directory if possible
            writer.append("RUN if test `ls -A /context|wc -l` -eq 1; then mv /context /_context; mv /_context/* /context; ");
            if(strip != null) {
                // Err on failure
                writer.append("else echo Failed to strip; exit 1; ");
            }
            writer.append("fi\n");
        }
    }
}