# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_library(searchlib_queryeval_test STATIC
    SOURCES
    mock_element_gap_inspector.cpp
    searchhistory.cpp
    DEPENDS
)
