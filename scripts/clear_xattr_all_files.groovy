#!/usr/bin/env filebot -script
//--- VERSION 1.0.0

// select xattr tagged files
def files = args.getFiles{ f -> f.xattr.size() > 0 }

// sanity checks
 if (files.size() == 0) {
   die "No xattr tagged files"
 }

files.each { f ->
    log.finest "--- xattr: [$f.name] => [$f.metadata]"
    tryQuietly { f.xattr.clear() }
}
