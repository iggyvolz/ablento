<?php
if(!is_dir(__DIR__ . "/vendor")) shell_exec("composer install");
require_once __DIR__ . "/vendor/autoload.php";

// Ensure base directories exist
if(!is_dir(__DIR__ . "/sources")) mkdir(__DIR__ . "/sources");
if(!is_dir(__DIR__ . "/sources_built")) mkdir(__DIR__ . "/sources_built");
if(!is_dir(__DIR__ . "/packages")) mkdir(__DIR__ . "/packages");
if(!is_dir(__DIR__ . "/remotes")) mkdir(__DIR__ . "/remotes");
// Find all src
foreach(new RegexIterator(new RecursiveIteratorIterator(new RecursiveDirectoryIterator(__DIR__ . "/src")), "/\\.src\\.yaml$/") as $file => $_) {
    $data = Spyc::YAMLLoad($file);
    if(!is_array($data)) {
        throw new RuntimeException("Invalid file $file");
    }
    $name = is_string($data["name"] ?? null) ? $data["name"] : null;
    if(is_null($name)) {
        throw new RuntimeException("Invalid file $file, name must be specified");
    }
    echo "Processing $name...\n";
    // Process all remotes
    foreach(array_values($data["remotes"] ?? []) as $i => $remote) {
        if(!is_array($remote)) {
            throw new RuntimeException("Invalid file $file - remote #$i is not an array");
        }
        $url = is_string($remote["url"] ?? null) ? $remote["url"] : null;
        if(is_null($url)) {
            throw new RuntimeException("Invalid file $file - in remote #$i, url must be specified");
        }
        $sha3 = is_string($remote["sha3"] ?? null) ? strtolower($remote["sha3"]) : null;
        $sha2 = is_string($remote["sha2"] ?? null) ? strtolower($remote["sha2"]) : null;
        if(is_null($sha2) && is_null($sha3)) {
            throw new RuntimeException("Invalid file $file - in remote #$i, sha2 or sha3 must be specified");
        }
        $hash = is_null($sha3) ? "sha2-$sha2" : "sha3-$sha3";
        $file = __DIR__ . "/remotes/$hash";
        if(file_exists($file)) {
            echo "Already downloaded $hash, skipping...\n";
        } else {
            echo "Downloading $hash...\n";
            $f = fopen($file, "w+");
            $ch = curl_init($url);
            curl_setopt($ch, CURLOPT_FILE, $f); 
            curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
            // get curl response
            curl_exec($ch); 
            curl_close($ch);
            fclose($f);
        }
        echo "Verifying $hash...\n";
        if(!is_null($sha2)) {
            if(($real_sha2 = strtolower(hash_file("sha256", $file))) !== $sha2) {
                echo "FAILED: sha2 sum $real_sha2 does not match expected $sha3\n";
                unlink($file);
                exit(1);
            }
        }
        if(!is_null($sha3)) {
            if(($real_sha3 = strtolower(hash_file("sha3-256", $file))) !== $sha3) {
                echo "FAILED: sha3 sum $real_sha3 does not match expected $sha3\n";
                unlink($file);
                exit(1);
            }
        }
    }
    shell_exec("rm -Rf ".escapeshellarg(__DIR__ . "/sources/$name"));
    mkdir(__DIR__ . "/sources/$name");
    $write = fn(string $line) => file_put_contents(__DIR__ . "/sources/$name/Dockerfile", "$line\n", FILE_APPEND);
    $write("FROM alpine");
    $write("RUN mkdir /context");
    foreach($data["remotes"] as $remote) {
        $sha3 = is_string($remote["sha3"] ?? null) ? strtolower($remote["sha3"]) : null;
        $sha2 = is_string($remote["sha2"] ?? null) ? strtolower($remote["sha2"]) : null;
        if(is_null($sha2) && is_null($sha3)) {
            throw new RuntimeException("Invalid file $file - in remote #$i, sha2 or sha3 must be specified");
        }
        $hash = is_null($sha3) ? "sha2-$sha2" : "sha3-$sha3";
        copy(__DIR__ . "/remotes/$hash", __DIR__ . "/sources/$name/$hash");
        $extract = $remote["extract"] ?? null;
        if(!is_bool($extract)) {
            // Auto-detect from end of URL
            $extract = preg_match("/\\.(tar\\.gz|tar\\.xz|tar\\.bz2|tar|zip)$/", $remote["url"]) === 1;
        }
        $destination = $remote["destination"] ?? null;
        if(!is_string($destination)) {
            $destination = $extract ? "/" : "/" . basename($remote["url"]);
        }
        $write("COPY $hash /tmp/ablento_tmpfile");
        if($extract) {
            $write("RUN mkdir -p ".escapeshellarg("/context/$destination"));
            switch($mime=mime_content_type(__DIR__ . "/remotes/$hash")) {
                case "application/x-xz":
                case "application/x-bzip2":
                case "application/x-gzip":
                case "application/x-tar":
                    $write("RUN tar -xf /tmp/ablento_tmpfile -C ".escapeshellarg("/context".$destination));
                    break;
                case "application/x-zip-compressed":
                case "application/x-zip":
                    $write("RUN unzip /tmp/ablento_tmpfile -d ".escapeshellarg("/context".$destination));
                    break;
                default:
                    throw new RuntimeException("Invalid MIMe type for $hash: ".$mime);
            }
            $strip = $remote["strip"] ?? null;
            // Force to ?bool
            if(!is_bool($strip)) $strip = null;
            if($strip !== false) {
                if($strip) {
                    // Test for 3-files, otherwise exit
                    $write("RUN test \"`ls -A /context$destination|wc -l`\" -eq 1");
                } else {
                    // Test for 3-files, otherwise suppress stripping
                    $write("RUN test \"`ls -A /context$destination|wc -l`\" -eq 1 || touch /tmp/strip_suppress");
                }
                // Move the folder out of /context and replace /context with it
                $write("RUN test -f /tmp/strip_suppress || mv /context$destination/`ls -A /context$destination` /tmp/ablento_tmpcontext");
                $write("RUN test -f /tmp/strip_suppress || rmdir /context$destination");
                $write("RUN test -f /tmp/strip_suppress || mv /tmp/ablento_tmpcontext /context$destination");
                // Cleanup
                $write("RUN rm -f /tmp/strip_suppress");
                $write("RUN rm -f /tmp/ablento_tmpcontext");
            }
        } else {
            $write("RUN mv /tmp/ablento_tmpfile ".escapeshellarg("/context".$destination));
        }
    }
    $write("FROM scratch");
    $write("COPY --from=0 /context /");
    shell_exec("DOCKER_BUILDKIT=1 docker build -o ".escapeshellarg(__DIR__ . "/sources_built/$name")." ".escapeshellarg(__DIR__ . "/sources/$name"));
}
// Map of packages to their parents
$prereqs = [];
// Find all pkg
foreach(new RegexIterator(new RecursiveIteratorIterator(new RecursiveDirectoryIterator(__DIR__ . "/src")), "/\\.pkg\\.yaml$/") as $file => $_) {
    $data = Spyc::YAMLLoad($file);
    $from = $data["from"] ?? null;
    if(!is_string($from)) {
        throw new RuntimeException("Invalid file $file - from must be specified");
    }
    $name = $data["name"] ?? null;
    if(!is_string($name)) {
        throw new RuntimeException("Invalid file $name - from must be specified");
    }
    $prereqs[$name]=$from;
    $sources = $data["source"] ?? $data["sources"] ?? null;
    if(is_string($sources)) {
        $sources = [$sources];
    }
    if(!is_array($sources)) {
        throw new RuntimeException("Invalid file $name - source(s) must be a string, array of strings, or array of {name,destination}");
    }
    $sources = array_map(function($src):array {
        if(is_string($src)) {
            return [
                "name" => $src,
                "destination" => "/"
            ];
        }
        if(is_array($src) && is_string($src["name"] ?? null) && is_string($src["destination"] ?? null)) {
            return $src;
        }
        throw new RuntimeException("Invalid file $name - source(s) must be a string, array of strings, or array of {name,destination}");
    }, $sources);
    shell_exec("rm -Rf ".escapeshellarg(__DIR__ . "/packages/$name"));
    mkdir(__DIR__ . "/packages/$name");
    $write = fn(string $line) => file_put_contents(__DIR__ . "/packages/$name/Dockerfile", "$line\n", FILE_APPEND);
    $name = is_string($data["name"] ?? null) ? $data["name"] : null;
    if(is_null($name)) {
        throw new RuntimeException("Invalid file $file, name must be specified");
    }
    $write("FROM ablento/$from");
    foreach($sources as $source) {
        $sourceName = $source["name"];
        $sourceDest = $source["destination"];
        shell_exec("cp -R ".escapeshellarg(__DIR__ . "/sources_built/$sourceName") . " " . escapeshellarg(__DIR__ . "/packages/$name"));
        $write("RUN rm -Rf /lfs/src");
        $write("RUN mkdir -p /lfs/src");
        $write("COPY $sourceName /lfs/src$sourceDest");
        $write("USER root");
        $write("RUN chown -R lfs:lfs /lfs/src$sourceDest");
        $write("USER lfs");
    }
    $write("WORKDIR /lfs/src");
    $script = $data["script"] ?? null;
    if(!is_array($script)) {
        throw new RuntimeException("Invalid file $file, script must be an array of strings");
    }
    foreach($script as $scriptLine) {
        $write("RUN $scriptLine");
    }
    $test = $data["test"] ?? [];
    if(!is_array($test)) {
        throw new RuntimeException("Invalid file $file, test must be an array of strings");
    }
    foreach($test as $testLine) {
        $write("RUN $testLine");
    }
}
function build_pkg(string $destination) {
    global $prereqs;
    if($destination === "base") {
        echo "Building base image...\n";
        shell_exec("docker build -t ablento/base ".escapeshellarg(__DIR__ . "/base"));
        return;
    } else {
        if(!array_key_exists($destination, $prereqs)) {
            throw new RuntimeException("Unable to find needed package $destination");
        }
        build_pkg($prereqs[$destination]);
        echo "Building $destination...\n";
        shell_exec("docker build -t ablento/$destination ".escapeshellarg(__DIR__ . "/packages/$destination"));
    }
}

if($destination = ($argv[1] ?? null)) {
    build_pkg($destination);
}