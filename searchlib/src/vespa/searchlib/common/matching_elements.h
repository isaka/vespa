// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once
#include <map>
#include <memory>
#include <string>
#include <utility>
#include <vector>

namespace search {

/**
 * Keeps track of which elements matched the query for a set of fields
 * across multiple documents.
 **/
class MatchingElements
{
private:
    using key_t = std::pair<uint32_t, std::string>;
    using value_t = std::vector<uint32_t>;

    std::map<key_t, value_t> _map;

public:
    MatchingElements();
    ~MatchingElements();

    using UP = std::unique_ptr<MatchingElements>;

    void add_matching_elements(uint32_t docid, const std::string &field_name, const std::vector<uint32_t> &elements);
    const std::vector<uint32_t> &get_matching_elements(uint32_t docid, const std::string &field_name) const;
};

} // namespace search
