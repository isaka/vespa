// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <memory>
#include <string>

namespace search::docsummary {

class IQueryTermFilter;

/*
 * Interface class for creating an instance of IQueryTermFilter for a
 * specific input field.
 */
class IQueryTermFilterFactory
{
public:
    virtual ~IQueryTermFilterFactory() = default;

    virtual std::shared_ptr<const IQueryTermFilter> make(std::string_view input_field) const = 0;
};

}
