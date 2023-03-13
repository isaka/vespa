// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <vespa/searchlib/attribute/single_raw_attribute.h>
#include <vespa/searchlib/attribute/attributefactory.h>
#include <vespa/searchcommon/attribute/config.h>
#include <vespa/vespalib/gtest/gtest.h>
#include <filesystem>
#include <memory>

using search::AttributeFactory;
using search::AttributeVector;
using search::attribute::BasicType;
using search::attribute::CollectionType;
using search::attribute::Config;
using search::attribute::SingleRawAttribute;
using vespalib::ConstArrayRef;


std::vector<char> empty;
vespalib::string hello("hello");
vespalib::ConstArrayRef<char> raw_hello(hello.c_str(), hello.size());

std::filesystem::path attr_path("raw.dat");

std::vector<char> as_vector(vespalib::stringref value) {
    return {value.data(), value.data() + value.size()};
}

std::vector<char> as_vector(vespalib::ConstArrayRef<char> value) {
    return {value.data(), value.data() + value.size()};
}

void remove_saved_attr() {
    std::filesystem::remove(attr_path);
}

class RawAttributeTest : public ::testing::Test
{
protected:
    std::shared_ptr<AttributeVector> _attr;
    SingleRawAttribute*              _raw;

    RawAttributeTest();
    ~RawAttributeTest() override;
    std::vector<char> get_raw(uint32_t docid);
    void reset_attr(bool add_reserved);
};


RawAttributeTest::RawAttributeTest()
    : ::testing::Test(),
    _attr(),
    _raw(nullptr)
{
    reset_attr(true);
}

RawAttributeTest::~RawAttributeTest() = default;

std::vector<char>
RawAttributeTest::get_raw(uint32_t docid)
{
    return as_vector(_raw->get_raw(docid));
}

void
RawAttributeTest::reset_attr(bool add_reserved)
{
    Config cfg(BasicType::RAW, CollectionType::SINGLE);
    _attr = AttributeFactory::createAttribute("raw", cfg);
    _raw = &dynamic_cast<SingleRawAttribute&>(*_attr);
    if (add_reserved) {
        _attr->addReservedDoc();
    }
}

TEST_F(RawAttributeTest, can_set_and_clear_value)
{
    EXPECT_TRUE(_attr->addDocs(10));
    _attr->commit();
    EXPECT_EQ(empty, get_raw(1));
    _raw->set_raw(1, raw_hello);
    EXPECT_EQ(as_vector(hello), get_raw(1));
    _attr->clearDoc(1);
    EXPECT_EQ(empty, get_raw(1));
}

TEST_F(RawAttributeTest, implements_serialize_for_sort) {
    vespalib::string long_hello("hello, is there anybody out there");
    vespalib::ConstArrayRef<char> raw_long_hello(long_hello.c_str(), long_hello.size());
    uint8_t buf[8];
    memset(buf, 0, sizeof(buf));
    _attr->addDocs(10);
    _attr->commit();
    EXPECT_EQ(0, _attr->serializeForAscendingSort(1, buf, sizeof(buf)));
    EXPECT_EQ(0, _attr->serializeForDescendingSort(1, buf, sizeof(buf)));
    _raw->set_raw(1, raw_hello);
    EXPECT_EQ(5, _attr->serializeForAscendingSort(1, buf, sizeof(buf)));
    EXPECT_EQ(0, memcmp("hello", buf, 5));
    EXPECT_EQ(5, _attr->serializeForDescendingSort(1, buf, sizeof(buf)));
    uint8_t expected [] = {0xff-'h', 0xff-'e', 0xff-'l', 0xff-'l', 0xff-'o'};
    EXPECT_EQ(0, memcmp(expected, buf, 5));
    _raw->set_raw(1, raw_long_hello);
    EXPECT_EQ(-1, _attr->serializeForAscendingSort(1, buf, sizeof(buf)));
    EXPECT_EQ(-1, _attr->serializeForDescendingSort(1, buf, sizeof(buf)));
}

TEST_F(RawAttributeTest, save_and_load)
{
    auto mini_test = as_vector("mini test");
    remove_saved_attr();
    _attr->addDocs(10);
    _attr->commit();
    _raw->set_raw(1, raw_hello);
    _raw->set_raw(2, mini_test);
    _attr->setCreateSerialNum(20);
    _attr->save();
    reset_attr(false);
    _attr->load();
    EXPECT_EQ(11, _attr->getCommittedDocIdLimit());
    EXPECT_EQ(11, _attr->getStatus().getNumDocs());
    EXPECT_EQ(20, _attr->getCreateSerialNum());
    EXPECT_EQ(as_vector("hello"), as_vector(_raw->get_raw(1)));
    EXPECT_EQ(mini_test, as_vector(_raw->get_raw(2)));
    remove_saved_attr();
}

GTEST_MAIN_RUN_ALL_TESTS()