# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_executable(searchcore_document_db_reference_registry_test_app TEST
    SOURCES
    document_db_reference_registry_test.cpp
    DEPENDS
    searchcore_reference
    searchcore_documentmetastore
    searchcore_bucketdb
    GTest::gtest
)
vespa_add_test(NAME searchcore_document_db_reference_registry_test_app COMMAND searchcore_document_db_reference_registry_test_app)
