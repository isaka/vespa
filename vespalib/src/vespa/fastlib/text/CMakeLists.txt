# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_library(vespalib_fastlib_text OBJECT
    SOURCES
    unicodeutil.cpp
    normwordfolder.cpp
    generate_hash_table.cpp
    DEPENDS
)
