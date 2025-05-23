// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#include "objectstore.h"
#include <vespa/vespalib/stllike/hash_map.hpp>

namespace search::fef {

ObjectStore::ObjectStore() :
    _objectMap()
{
}

ObjectStore::~ObjectStore()
{
    for(auto & it : _objectMap) {
        delete it.second;
        it.second = nullptr;
    }
}

void
ObjectStore::add(const std::string & key, Anything::UP value)
{
    auto found = _objectMap.find(key);
    if (found != _objectMap.end()) {
        delete found->second;
        found->second = nullptr;
    }
    _objectMap[key] = value.release();
}

const Anything *
ObjectStore::get(const std::string & key) const
{
    auto found = _objectMap.find(key);
    return (found != _objectMap.end()) ? found->second : nullptr;
}

Anything *
ObjectStore::get_mutable(const std::string& key)
{
    auto found = _objectMap.find(key);
    return (found != _objectMap.end()) ? found->second : nullptr;
}

}
