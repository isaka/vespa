// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <cstdint>
#include <string>

namespace vespalib { class GenericHeader; }

namespace search {

/*
 * Class to calculate logical file size of a file based on header tags
 * and physical file size.  Logical file size can be smaller than
 * physical file size due to padding for directio alignment
 * constraints.
 */
class FileSizeCalculator
{
public:
    static bool
    extractFileSize(const vespalib::GenericHeader &header, size_t headerLen,
                    std::string fileName, uint64_t &fileSize);
};

}
