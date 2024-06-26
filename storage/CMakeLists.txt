# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_define_module(
    DEPENDS
    vespadefaults
    vespa_metrics
    vespa_config
    vespa_configdefinitions
    vespalog
    vespa_messagebus
    vespa_documentapi
    vespa_document
    vespalib
    vespa_vdslib
    vespa_persistence

    EXTERNAL_DEPENDS
    ${VESPA_GLIBC_RT_LIB}

    LIBS
    src/vespa/storage
    src/vespa/storage/bucketdb
    src/vespa/storage/common
    src/vespa/storage/common/hostreporter
    src/vespa/storage/config
    src/vespa/storage/distributor
    src/vespa/storage/distributor/bucketdb
    src/vespa/storage/distributor/maintenance
    src/vespa/storage/distributor/operations
    src/vespa/storage/distributor/operations/external
    src/vespa/storage/distributor/operations/idealstate
    src/vespa/storage/frameworkimpl/component
    src/vespa/storage/frameworkimpl/status
    src/vespa/storage/frameworkimpl/thread
    src/vespa/storage/persistence
    src/vespa/storage/persistence/filestorage
    src/vespa/storage/storageserver
    src/vespa/storage/storageserver/rpc
    src/vespa/storage/storageutil
    src/vespa/storage/tools
    src/vespa/storage/visiting
    src/vespa/storageframework/defaultimplementation/clock
    src/vespa/storageframework/defaultimplementation/component
    src/vespa/storageframework/defaultimplementation/thread
    src/vespa/storageframework/generic/clock
    src/vespa/storageframework/generic/component
    src/vespa/storageframework/generic/metric
    src/vespa/storageframework/generic/status
    src/vespa/storageframework/generic/thread
    src/vespa/storageapi/app
    src/vespa/storageapi/buckets
    src/vespa/storageapi/mbusprot
    src/vespa/storageapi/message
    src/vespa/storageapi/messageapi

    TEST_DEPENDS
    vespa_messagebus-test
    TEST_EXTERNAL_DEPENDS
    ${VESPA_ATOMIC_LIB}

    TESTS
    src/tests
    src/tests/bucketdb
    src/tests/common
    src/tests/common/hostreporter
    src/tests/distributor
    src/tests/frameworkimpl/status
    src/tests/persistence
    src/tests/persistence/common
    src/tests/persistence/filestorage
    src/tests/storageapi
    src/tests/storageapi/buckets
    src/tests/storageapi/mbusprot
    src/tests/storageapi/messageapi
    src/tests/storageframework
    src/tests/storageframework/clock
    src/tests/storageframework/thread
    src/tests/storageserver
    src/tests/storageserver/rpc
    src/tests/visiting
)
