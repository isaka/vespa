// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.query.profile.types.test;

import com.yahoo.component.ComponentId;
import com.yahoo.container.jdisc.HttpRequest;
import com.yahoo.language.Language;
import com.yahoo.language.process.Embedder;
import com.yahoo.tensor.Tensor;
import com.yahoo.tensor.TensorType;
import com.yahoo.yolean.Exceptions;
import com.yahoo.search.Query;
import com.yahoo.processing.request.CompoundName;
import com.yahoo.search.query.profile.QueryProfile;
import com.yahoo.search.query.profile.QueryProfileRegistry;
import com.yahoo.search.query.profile.compiled.CompiledQueryProfile;
import com.yahoo.search.query.profile.QueryProfileProperties;
import com.yahoo.search.query.profile.compiled.CompiledQueryProfileRegistry;
import com.yahoo.search.query.profile.types.FieldDescription;
import com.yahoo.search.query.profile.types.FieldType;
import com.yahoo.search.query.profile.types.QueryProfileType;
import com.yahoo.search.query.profile.types.QueryProfileTypeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests query profiles with/and types
 *
 * @author bratseth
 */
public class QueryProfileTypeTestCase {

    private QueryProfileRegistry registry;

    private QueryProfileType testtype, emptyInheritingTesttype, testtypeStrict, user, userStrict;

    @BeforeEach
    public void setUp() {
        registry = new QueryProfileRegistry();

        testtype = new QueryProfileType(new ComponentId("testtype"));
        testtype.inherited().add(registry.getTypeRegistry().getComponent(new ComponentId("native")));
        emptyInheritingTesttype = new QueryProfileType(new ComponentId("emptyInheritingTesttype"));
        emptyInheritingTesttype.inherited().add(testtype);
        testtypeStrict = new QueryProfileType(new ComponentId("testtypeStrict"));
        testtypeStrict.setStrict(true);
        user = new QueryProfileType(new ComponentId("user"));
        userStrict = new QueryProfileType(new ComponentId("userStrict"));
        userStrict.setStrict(true);

        registry.getTypeRegistry().register(testtype);
        registry.getTypeRegistry().register(emptyInheritingTesttype);
        registry.getTypeRegistry().register(testtypeStrict);
        registry.getTypeRegistry().register(user);
        registry.getTypeRegistry().register(userStrict);

        addTypeFields(testtype, registry.getTypeRegistry());
        testtype.addField(new FieldDescription("myUserQueryProfile", FieldType.fromString("query-profile:user", registry.getTypeRegistry())));
        addTypeFields(testtypeStrict, registry.getTypeRegistry());
        testtypeStrict.addField(new FieldDescription("myUserQueryProfile", FieldType.fromString("query-profile:userStrict", registry.getTypeRegistry())));
        addUserFields(user, registry.getTypeRegistry());
        addUserFields(userStrict, registry.getTypeRegistry());

    }

    private void addTypeFields(QueryProfileType type, QueryProfileTypeRegistry registry) {
        type.addField(new FieldDescription("myString", FieldType.fromString("string", registry)), registry);
        type.addField(new FieldDescription("myInteger", FieldType.fromString("integer", registry),"int"), registry);
        type.addField(new FieldDescription("myLong", FieldType.fromString("long", registry)), registry);
        type.addField(new FieldDescription("myFloat", FieldType.fromString("float", registry)), registry);
        type.addField(new FieldDescription("myDouble", FieldType.fromString("double", registry)), registry);
        type.addField(new FieldDescription("myBoolean", FieldType.fromString("boolean", registry)), registry);
        type.addField(new FieldDescription("myBoolean", FieldType.fromString("boolean", registry)), registry);
        type.addField(new FieldDescription("ranking.features.query(myTensor1)", FieldType.fromString("tensor(a{},b{})", registry)), registry);
        type.addField(new FieldDescription("ranking.features.query(myTensor2)", FieldType.fromString("tensor(x[2],y[2])", registry)), registry);
        type.addField(new FieldDescription("ranking.features.query(myTensor3)", FieldType.fromString("tensor<float>(x{})",registry)), registry);
        type.addField(new FieldDescription("ranking.features.query(myTensor4)", FieldType.fromString("tensor<float>(x[5])",registry)), registry);
        type.addField(new FieldDescription("myQuery", FieldType.fromString("query", registry)), registry);
        type.addField(new FieldDescription("myQueryProfile", FieldType.fromString("query-profile", registry),"qp"), registry);
    }

    private void addUserFields(QueryProfileType user, QueryProfileTypeRegistry registry) {
        user.addField(new FieldDescription("myUserString",FieldType.fromString("string",registry)), registry);
        user.addField(new FieldDescription("myUserInteger",FieldType.fromString("integer",registry),"uint"), registry);
    }

    @Test
    void testTypedOfPrimitivesAssignmentNonStrict() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);
        registry.register(profile);

        profile.set("myString", "anyValue", registry);
        profile.set("nontypedString", "anyValueToo", registry); // legal because this is not strict
        assertWrongType(profile, "integer", "myInteger", "notInteger");
        assertWrongType(profile, "integer", "myInteger", "1.5");
        profile.set("myInteger", 3, registry);
        assertWrongType(profile, "long", "myLong", "notLong");
        assertWrongType(profile, "long", "myLong", "1.5");
        profile.set("myLong", 4000000000000L, registry);
        assertWrongType(profile, "float", "myFloat", "notFloat");
        profile.set("myFloat", 3.14f, registry);
        assertWrongType(profile, "double", "myDouble", "notDouble");
        profile.set("myDouble", 2.18, registry);
        profile.set("myBoolean", true, registry);

        String tensorString1 = "{{a:a1, b:b1}:1.0, {a:a2, b:b1}:2.0}";
        profile.set("ranking.features.query(myTensor1)", tensorString1, registry);
        String tensorString2 = "{{x:0, y:0}:1.0, {x:0, y:1}:2.0}";
        profile.set("ranking.features.query(myTensor2)", tensorString2, registry);
        String tensorString3 = "{{x:x1}:1.0, {x:x2}:2.0}";
        profile.set("ranking.features.query(myTensor3)", tensorString3, registry);

        profile.set("myQuery", "...", registry); // TODO
        profile.set("myQueryProfile.anyString", "value1", registry);
        profile.set("myQueryProfile.anyDouble", 8.76, registry);
        profile.set("myUserQueryProfile.myUserString", "value2", registry);
        profile.set("myUserQueryProfile.anyString", "value3", registry); // Legal because user is not strict
        assertWrongType(profile, "integer", "myUserQueryProfile.myUserInteger", "notInteger");
        profile.set("myUserQueryProfile.uint", 1337, registry); // Set using alias
        profile.set("myUserQueryProfile.anyDouble", 9.13, registry); // Legal because user is not strict

        CompiledQueryProfileRegistry cRegistry = registry.compile();
        QueryProfileProperties properties = new QueryProfileProperties(cRegistry.findQueryProfile("test"));

        assertEquals("anyValue", properties.get("myString"));
        assertEquals("anyValueToo", properties.get("nontypedString"));
        assertEquals(3, properties.get("myInteger"));
        assertEquals(3, properties.get("Int"));
        assertEquals(4000000000000L, properties.get("myLong"));
        assertEquals(3.14f, properties.get("myFloat"));
        assertEquals(2.18, properties.get("myDouble"));
        assertEquals(true, properties.get("myBoolean"));
        assertEquals(Tensor.from(tensorString1), properties.get("ranking.features.query(myTensor1)"));
        assertEquals(Tensor.from("tensor(x[2],y[2])", tensorString2), properties.get("ranking.features.query(myTensor2)"));
        assertEquals(Tensor.from("tensor<float>(x{})", tensorString3), properties.get("ranking.features.query(myTensor3)"));
        // TODO: assertEquals(..., cprofile.get("myQuery"));
        assertEquals("value1", properties.get("myQueryProfile.anyString"));
        assertEquals("value1", properties.get("QP.anyString"));
        assertEquals(8.76, properties.get("myQueryProfile.anyDouble"));
        assertEquals(8.76, properties.get("qp.anyDouble"));
        assertEquals("value2", properties.get("myUserQueryProfile.myUserString"));
        assertEquals("value3", properties.get("myUserQueryProfile.anyString"));
        assertEquals(1337, properties.get("myUserQueryProfile.myUserInteger"));
        assertEquals(1337, properties.get("myUserQueryProfile.uint"));
        assertEquals(9.13, properties.get("myUserQueryProfile.anyDouble"));
        assertNull(properties.get("nonExisting"));

        properties.set("INt", 51);
        assertEquals(51, properties.get("InT"));
        assertEquals(51, properties.get("myInteger"));
    }

    @Test
    void testTypedOfPrimitivesAssignmentStrict() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtypeStrict);

        profile.set("myString", "anyValue", registry);
        assertNotPermitted(profile, "nontypedString", "anyValueToo"); // Illegal because this is strict
        assertWrongType(profile, "integer", "myInteger", "notInteger");
        assertWrongType(profile, "integer", "myInteger", "1.5");
        profile.set("myInteger", 3, registry);
        assertWrongType(profile, "long", "myLong", "notLong");
        assertWrongType(profile, "long", "myLong", "1.5");
        profile.set("myLong", 4000000000000L, registry);
        assertWrongType(profile, "float", "myFloat", "notFloat");
        profile.set("myFloat", 3.14f, registry);
        assertWrongType(profile, "double", "myDouble", "notDouble");
        profile.set("myDouble", 2.18, registry);
        profile.set("myQueryProfile.anyString", "value1", registry);
        profile.set("myQueryProfile.anyDouble", 8.76, registry);
        profile.set("myUserQueryProfile.myUserString", "value2", registry);
        assertNotPermitted(profile, "myUserQueryProfile.anyString", "value3"); // Illegal because this is strict
        assertWrongType(profile, "integer", "myUserQueryProfile.myUserInteger", "notInteger");
        profile.set("myUserQueryProfile.myUserInteger", 1337, registry);
        assertNotPermitted(profile, "myUserQueryProfile.anyDouble", 9.13); // Illegal because this is strict

        CompiledQueryProfile cprofile = profile.compile(null);

        assertEquals("anyValue", cprofile.get("myString"));
        assertNull(cprofile.get("nontypedString"));
        assertEquals(3, cprofile.get("myInteger"));
        assertEquals(4000000000000L, cprofile.get("myLong"));
        assertEquals(3.14f, cprofile.get("myFloat"));
        assertEquals(2.18, cprofile.get("myDouble"));
        assertEquals("value1", cprofile.get("myQueryProfile.anyString"));
        assertEquals(8.76, cprofile.get("myQueryProfile.anyDouble"));
        assertEquals("value2", cprofile.get("myUserQueryProfile.myUserString"));
        assertNull(cprofile.get("myUserQueryProfile.anyString"));
        assertEquals(1337, cprofile.get("myUserQueryProfile.myUserInteger"));
        assertNull(cprofile.get("myUserQueryProfile.anyDouble"));
    }

    /** Tests assigning a subprofile directly */
    @Test
    void testTypedAssignmentOfQueryProfilesNonStrict() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);

        QueryProfile map1 = new QueryProfile("myMap1");
        map1.set("key1", "value1", registry);

        QueryProfile map2 = new QueryProfile("myMap2");
        map2.set("key2", "value2", registry);

        QueryProfile myUser = new QueryProfile("myUser");
        myUser.setType(user);
        myUser.set("myUserString", "userValue1", registry);
        myUser.set("myUserInteger", 442, registry);

        assertWrongType(profile, "reference to a query profile", "myQueryProfile", "aString");
        profile.set("myQueryProfile", map1, registry);
        profile.set("someMap", map2, registry); // Legal because this is not strict
        assertWrongType(profile, "reference to a query profile of type 'user'", "myUserQueryProfile", map1);
        profile.set("myUserQueryProfile", myUser, registry);

        CompiledQueryProfile cprofile = profile.compile(null);

        assertEquals("value1", cprofile.get("myQueryProfile.key1"));
        assertEquals("value2", cprofile.get("someMap.key2"));
        assertEquals("userValue1", cprofile.get("myUserQueryProfile.myUserString"));
        assertEquals(442, cprofile.get("myUserQueryProfile.myUserInteger"));
    }

    /** Tests assigning a subprofile directly */
    @Test
    void testTypedAssignmentOfQueryProfilesStrict() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtypeStrict);

        QueryProfile map1 = new QueryProfile("myMap1");
        map1.set("key1", "value1", registry);

        QueryProfile map2 = new QueryProfile("myMap2");
        map2.set("key2", "value2", registry);

        QueryProfile myUser = new QueryProfile("myUser");
        myUser.setType(userStrict);
        myUser.set("myUserString", "userValue1", registry);
        myUser.set("myUserInteger", 442, registry);

        assertWrongType(profile, "reference to a query profile", "myQueryProfile", "aString");
        profile.set("myQueryProfile", map1, registry);
        assertNotPermitted(profile, "someMap", map2);
        assertWrongType(profile, "reference to a query profile of type 'userStrict'", "myUserQueryProfile", map1);
        profile.set("myUserQueryProfile", myUser, registry);

        CompiledQueryProfile cprofile = profile.compile(null);

        assertEquals("value1", cprofile.get("myQueryProfile.key1"));
        assertNull(cprofile.get("someMap.key2"));
        assertEquals("userValue1", cprofile.get("myUserQueryProfile.myUserString"));
        assertEquals(442, cprofile.get("myUserQueryProfile.myUserInteger"));
    }

    /** Tests assigning a subprofile as an id string */
    @Test
    void testTypedAssignmentOfQueryProfileReferencesNonStrict() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);

        QueryProfile map1 = new QueryProfile("myMap1");
        map1.set("key1", "value1", registry);

        QueryProfile map2 = new QueryProfile("myMap2");
        map2.set("key2", "value2", registry);

        QueryProfile myUser = new QueryProfile("myUser");
        myUser.setType(user);
        myUser.set("myUserString", "userValue1", registry);
        myUser.set("myUserInteger", 442, registry);

        registry.register(profile);
        registry.register(map1);
        registry.register(map2);
        registry.register(myUser);

        assertWrongType(profile, "reference to a query profile", "myQueryProfile", "aString");
        registry.register(map1);
        profile.set("myQueryProfile", "myMap1", registry);
        registry.register(map2);
        profile.set("someMap", "myMap2", registry); // NOTICE: Will set as a string because we cannot know this is a reference
        assertWrongType(profile, "reference to a query profile of type 'user'", "myUserQueryProfile", "myMap1");
        registry.register(myUser);
        profile.set("myUserQueryProfile", "myUser", registry);

        CompiledQueryProfileRegistry cRegistry = registry.compile();
        CompiledQueryProfile cprofile = cRegistry.getComponent("test");

        assertEquals("value1", cprofile.get("myQueryProfile.key1"));
        assertEquals("myMap2", cprofile.get("someMap"));
        assertNull(cprofile.get("someMap.key2"), "Asking for an value which cannot be completely resolved returns null");
        assertEquals("userValue1", cprofile.get("myUserQueryProfile.myUserString"));
        assertEquals(442, cprofile.get("myUserQueryProfile.myUserInteger"));
    }

    /**
     * Tests overriding a subprofile as an id string through the query.
     * Here there exists a user profile already, and then a new one is overwritten
     */
    @Test
    void testTypedOverridingOfQueryProfileReferencesNonStrictThroughQuery() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);

        QueryProfile myUser = new QueryProfile("myUser");
        myUser.setType(user);
        myUser.set("myUserString", "userValue1", registry);
        myUser.set("myUserInteger", 442, registry);

        QueryProfile newUser = new QueryProfile("newUser");
        newUser.setType(user);
        newUser.set("myUserString", "newUserValue1", registry);
        newUser.set("myUserInteger", 845, registry);

        QueryProfileRegistry registry = new QueryProfileRegistry();
        registry.register(profile);
        registry.register(myUser);
        registry.register(newUser);
        CompiledQueryProfileRegistry cRegistry = registry.compile();
        CompiledQueryProfile cprofile = cRegistry.getComponent("test");

        Query query = new Query(HttpRequest.createTestRequest("?myUserQueryProfile=newUser",
                        com.yahoo.jdisc.http.HttpRequest.Method.GET),
                cprofile);

        assertEquals(0, query.errors().size());

        assertEquals("newUserValue1", query.properties().get("myUserQueryProfile.myUserString"));
        assertEquals(845, query.properties().get("myUserQueryProfile.myUserInteger"));
    }

    /**
     * Tests overriding a subprofile as an id string through the query.
     * Here no user profile is set before it is assigned in the query
     */
    @Test
    void testTypedAssignmentOfQueryProfileReferencesNonStrictThroughQuery() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);

        QueryProfile newUser = new QueryProfile("newUser");
        newUser.setType(user);
        newUser.set("myUserString", "newUserValue1", registry);
        newUser.set("myUserInteger", 845, registry);

        registry.register(profile);
        registry.register(newUser);
        CompiledQueryProfileRegistry cRegistry = registry.compile();
        CompiledQueryProfile cprofile = cRegistry.getComponent("test");

        Query query = new Query(HttpRequest.createTestRequest("?myUserQueryProfile=newUser",
                        com.yahoo.jdisc.http.HttpRequest.Method.GET),
                cprofile);

        assertEquals(0, query.errors().size());

        assertEquals("newUserValue1", query.properties().get("myUserQueryProfile.myUserString"));
        assertEquals(845, query.properties().get("myUserQueryProfile.myUserInteger"));
    }

    /**
     * Tests overriding a subprofile as an id string through the query.
     * Here no user profile is set before it is assigned in the query
     */
    @Test
    void testTypedAssignmentOfQueryProfileReferencesStrictThroughQuery() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtypeStrict);

        QueryProfile newUser = new QueryProfile("newUser");
        newUser.setType(userStrict);
        newUser.set("myUserString", "newUserValue1", registry);
        newUser.set("myUserInteger", 845, registry);

        registry.register(profile);
        registry.register(newUser);

        CompiledQueryProfileRegistry cRegistry = registry.compile();

        Query query = new Query(HttpRequest.createTestRequest("?myUserQueryProfile=newUser", com.yahoo.jdisc.http.HttpRequest.Method.GET), cRegistry.getComponent("test"));
        assertEquals(0, query.errors().size());

        assertEquals("newUserValue1", query.properties().get("myUserQueryProfile.myUserString"));
        assertEquals(845, query.properties().get("myUserQueryProfile.myUserInteger"));

        try {
            query.properties().set("myUserQueryProfile.someKey", "value");
            fail("Should not be allowed to set this");
        }
        catch (IllegalArgumentException e) {
            assertEquals("Could not set 'myUserQueryProfile.someKey' to 'value': 'someKey' is not declared in query profile type 'userStrict', and the type is strict",
                    Exceptions.toMessageString(e));
        }

    }

    @Test
    void testTensorRankFeatureInRequest() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);
        registry.register(profile);

        CompiledQueryProfileRegistry cRegistry = registry.compile();
        String tensorString = "{{a:a1, b:b1}:1.0, {a:a2, b:b1}:2.0}";
        Query query = new Query(HttpRequest.createTestRequest("?" + urlEncode("ranking.features.query(myTensor1)") +
                        "=" + urlEncode(tensorString),
                        com.yahoo.jdisc.http.HttpRequest.Method.GET),
                cRegistry.getComponent("test"));
        assertEquals(0, query.errors().size());
        assertEquals(Tensor.from(tensorString), query.properties().get("ranking.features.query(myTensor1)"));
        assertEquals(Tensor.from(tensorString), query.getRanking().getFeatures().getTensor("query(myTensor1)").get());
    }

    @Test
    void testTensorRankFeatureSetProgrammatically() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);
        registry.register(profile);

        CompiledQueryProfileRegistry cRegistry = registry.compile();
        String tensorString = "{{a:a1, b:b1}:1.0, {a:a2, b:b1}:2.0}";
        Query query = new Query(HttpRequest.createTestRequest("?", com.yahoo.jdisc.http.HttpRequest.Method.GET),
                cRegistry.getComponent("test"));
        query.properties().set("ranking.features.query(myTensor1)", Tensor.from(tensorString));
        assertEquals(Tensor.from(tensorString), query.getRanking().getFeatures().getTensor("query(myTensor1)").get());
    }

    @Test
    void testTensorRankFeatureSetProgrammaticallyWithWrongType() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);
        registry.register(profile);

        CompiledQueryProfileRegistry cRegistry = registry.compile();
        String tensorString = "tensor(x[3]):[0.1, 0.2, 0.3]";
        Query query = new Query(HttpRequest.createTestRequest("?", com.yahoo.jdisc.http.HttpRequest.Method.GET),
                cRegistry.getComponent("test"));
        try {
            query.getRanking().getFeatures().put("query(myTensor1)", Tensor.from(tensorString));
            fail("Expected exception");
        }
        catch (IllegalArgumentException e) {
            assertEquals("Could not set 'ranking.features.query(myTensor1)' to 'tensor(x[3]):[0.1, 0.2, 0.3]': " +
                    "Require a tensor of type tensor(a{},b{})",
                    Exceptions.toMessageString(e));
        }
        try {
            query.properties().set("ranking.features.query(myTensor1)", Tensor.from(tensorString));
            fail("Expected exception");
        }
        catch (IllegalArgumentException e) {
            assertEquals("Could not set 'ranking.features.query(myTensor1)' to 'tensor(x[3]):[0.1, 0.2, 0.3]': " +
                    "Require a tensor of type tensor(a{},b{})",
                    Exceptions.toMessageString(e));
        }
    }

    // Expected to work exactly as testTensorRankFeatureInRequest
    @Test
    void testTensorRankFeatureInRequestWithInheritedQueryProfileType() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(emptyInheritingTesttype);
        registry.register(profile);

        CompiledQueryProfileRegistry cRegistry = registry.compile();
        String tensorString = "{{a:a1, b:b1}:1.0, {a:a2, b:b1}:2.0}";
        Query query = new Query(HttpRequest.createTestRequest("?" + urlEncode("ranking.features.query(myTensor1)") +
                        "=" + urlEncode(tensorString),
                        com.yahoo.jdisc.http.HttpRequest.Method.GET),
                cRegistry.getComponent("test"));
        assertEquals(0, query.errors().size());
        assertEquals(Tensor.from(tensorString), query.properties().get("ranking.features.query(myTensor1)"));
        assertEquals(Tensor.from(tensorString), query.getRanking().getFeatures().getTensor("query(myTensor1)").get());
    }

    @Test
    void testUnembeddedTensorRankFeatureInRequest() {
        String text = "text to embed into a tensor";
        Tensor embedding1 = Tensor.from("tensor<float>(x[5]):[3,7,4,0,0]]");
        Tensor embedding2 = Tensor.from("tensor<float>(x[5]):[1,2,3,4,0]]");

        Map<String, Embedder> embedders = Map.of(
                "emb1", new MockEmbedder(text, Language.UNKNOWN, embedding1)
        );
        assertEmbedQuery("embed(" + text + ")", embedding1, embedders);
        assertEmbedQuery("embed('" + text + "')", embedding1, embedders);
        assertEmbedQuery("embed(\"" + text + "\")", embedding1, embedders);
        assertEmbedQuery("embed(emb1, '" + text + "')", embedding1, embedders);
        assertEmbedQuery("embed(emb1, \"" + text + "\")", embedding1, embedders);
        assertEmbedQueryFails("embed(emb2, \"" + text + "\")", embedding1, embedders,
                "Can't find embedder 'emb2'. Available embedder ids are 'emb1'.");

        embedders = Map.of(
                "emb1", new MockEmbedder(text, Language.UNKNOWN, embedding1),
                "emb2", new MockEmbedder(text, Language.UNKNOWN, embedding2)
        );
        assertEmbedQuery("embed(emb1, '" + text + "')", embedding1, embedders);
        assertEmbedQuery("embed(emb2, '" + text + "')", embedding2, embedders);
        assertEmbedQueryFails("embed(emb2, text)", embedding1, embedders,
                              "Multiple embedders are provided but the string to embed is not quoted. " +
                              "Usage: embed(embedder-id, 'text'). Available embedder ids are 'emb1', 'emb2'.");
        assertEmbedQueryFails("embed(emb3, \"" + text + "\")", embedding1, embedders,
                "Can't find embedder 'emb3'. Available embedder ids are 'emb1', 'emb2'.");

        // And with specified language
        embedders = Map.of(
                "emb1", new MockEmbedder(text, Language.ENGLISH, embedding1)
        );
        assertEmbedQuery("embed(" + text + ")", embedding1, embedders, Language.ENGLISH.languageCode());

        embedders = Map.of(
                "emb1", new MockEmbedder(text, Language.ENGLISH, embedding1),
                "emb2", new MockEmbedder(text, Language.UNKNOWN, embedding2)
        );
        assertEmbedQuery("embed(emb1, '" + text + "')", embedding1, embedders, Language.ENGLISH.languageCode());
        assertEmbedQuery("embed(emb2, '" + text + "')", embedding2, embedders, Language.UNKNOWN.languageCode());
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Test
    void testIllegalStrictAssignmentFromRequest() {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtypeStrict);

        QueryProfile newUser = new QueryProfile("newUser");
        newUser.setType(userStrict);

        profile.set("myUserQueryProfile", newUser, registry);

        try {
            new Query(HttpRequest.createTestRequest("?myUserQueryProfile.nondeclared=someValue",
                            com.yahoo.jdisc.http.HttpRequest.Method.GET),
                    profile.compile(null));
            fail("Above statement should throw");
        } catch (IllegalArgumentException e) {
            // As expected.
            assertTrue(Exceptions.toMessageString(e).contains(
                    "Could not set 'myUserQueryProfile.nondeclared' to 'someValue': 'nondeclared' is not declared in query profile type 'userStrict', and the type is strict"));
        }
    }

    /**
     * Tests overriding a subprofile as an id string through the query.
     * Here there exists a user profile already, and then a new one is overwritten.
     * The whole thing is accessed through a two levels of nontyped top-level profiles
     */
    @Test
    void testTypedOverridingOfQueryProfileReferencesNonStrictThroughQueryNestedInAnUntypedProfile() {
        QueryProfile topMap = new QueryProfile("topMap");

        QueryProfile subMap = new QueryProfile("topSubMap");
        topMap.set("subMap", subMap, registry);

        QueryProfile test = new QueryProfile("test");
        test.setType(testtype);
        subMap.set("typeProfile", test, registry);

        QueryProfile myUser = new QueryProfile("myUser");
        myUser.setType(user);
        myUser.set("myUserString", "userValue1", registry);
        myUser.set("myUserInteger", 442, registry);
        test.set("myUserQueryProfile", myUser, registry);

        QueryProfile newUser = new QueryProfile("newUser");
        newUser.setType(user);
        newUser.set("myUserString", "newUserValue1", registry);
        newUser.set("myUserInteger", 845, registry);

        registry.register(topMap);
        registry.register(subMap);
        registry.register(test);
        registry.register(myUser);
        registry.register(newUser);
        CompiledQueryProfileRegistry cRegistry = registry.compile();

        Query query = new Query(HttpRequest.createTestRequest("?subMap.typeProfile.myUserQueryProfile=newUser",
                        com.yahoo.jdisc.http.HttpRequest.Method.GET),
                cRegistry.getComponent("topMap"));

        assertEquals(0, query.errors().size());

        assertEquals("newUserValue1", query.properties().get("subMap.typeProfile.myUserQueryProfile.myUserString"));
        assertEquals(845, query.properties().get("subMap.typeProfile.myUserQueryProfile.myUserInteger"));
    }

    /**
     * Same as previous test but using the untyped myQueryProfile reference instead of the typed myUserQueryProfile
     */
    @Test
    void testAnonTypedOverridingOfQueryProfileReferencesNonStrictThroughQueryNestedInAnUntypedProfile() {
        QueryProfile topMap = new QueryProfile("topMap");

        QueryProfile subMap = new QueryProfile("topSubMap");
        topMap.set("subMap", subMap, registry);

        QueryProfile test = new QueryProfile("test");
        test.setType(testtype);
        subMap.set("typeProfile", test, registry);

        QueryProfile myUser = new QueryProfile("myUser");
        myUser.setType(user);
        myUser.set("myUserString", "userValue1", registry);
        myUser.set("myUserInteger", 442, registry);
        test.set("myQueryProfile", myUser, registry);

        QueryProfile newUser = new QueryProfile("newUser");
        newUser.setType(user);
        newUser.set("myUserString", "newUserValue1", registry);
        newUser.set("myUserInteger", 845, registry);

        registry.register(topMap);
        registry.register(subMap);
        registry.register(test);
        registry.register(myUser);
        registry.register(newUser);
        CompiledQueryProfileRegistry cRegistry = registry.compile();

        Query query = new Query(HttpRequest.createTestRequest("?subMap.typeProfile.myQueryProfile=newUser", com.yahoo.jdisc.http.HttpRequest.Method.GET), cRegistry.getComponent("topMap"));
        assertEquals(0, query.errors().size());

        assertEquals("newUserValue1", query.properties().get("subMap.typeProfile.myQueryProfile.myUserString"));
        assertEquals(845, query.properties().get("subMap.typeProfile.myQueryProfile.myUserInteger"));
    }

    /**
     * Tests setting a illegal value in a strict profile nested under untyped maps
     */
    @Test
    void testSettingValueInStrictTypeNestedUnderUntypedMaps() {
        QueryProfile topMap = new QueryProfile("topMap");

        QueryProfile subMap = new QueryProfile("topSubMap");
        topMap.set("subMap", subMap, registry);

        QueryProfile test = new QueryProfile("test");
        test.setType(testtypeStrict);
        subMap.set("typeProfile", test, registry);

        registry.register(topMap);
        registry.register(subMap);
        registry.register(test);
        CompiledQueryProfileRegistry cRegistry = registry.compile();

        try {
            new Query(
                    HttpRequest.createTestRequest("?subMap.typeProfile.someValue=value",
                            com.yahoo.jdisc.http.HttpRequest.Method.GET),
                    cRegistry.getComponent("topMap"));
            fail("Above statement should throw");
        } catch (IllegalArgumentException e) {
            // As expected.
            assertTrue(Exceptions.toMessageString(e).contains(
                    "Could not set 'subMap.typeProfile.someValue' to 'value': 'someValue' is not declared in query profile type 'testtypeStrict', and the type is strict"));
        }
    }

    /**
     * Tests overriding a subprofile as an id string through the query.
     * Here, no user profile is set before it is assigned in the query
     * The whole thing is accessed through a two levels of nontyped top-level profiles
     */
    @Test
    void testTypedSettingOfQueryProfileReferencesNonStrictThroughQueryNestedInAnUntypedProfile() {
        QueryProfile topMap = new QueryProfile("topMap");

        QueryProfile subMap = new QueryProfile("topSubMap");
        topMap.set("subMap", subMap, registry);

        QueryProfile test = new QueryProfile("test");
        test.setType(testtype);
        subMap.set("typeProfile", test, registry);

        QueryProfile newUser = new QueryProfile("newUser");
        newUser.setType(user);
        newUser.set("myUserString", "newUserValue1", registry);
        newUser.set("myUserInteger", 845, registry);

        registry.register(topMap);
        registry.register(subMap);
        registry.register(test);
        registry.register(newUser);
        CompiledQueryProfileRegistry cRegistry = registry.compile();

        Query query = new Query(HttpRequest.createTestRequest("?subMap.typeProfile.myUserQueryProfile=newUser",
                        com.yahoo.jdisc.http.HttpRequest.Method.GET),
                cRegistry.getComponent("topMap"));
        assertEquals(0, query.errors().size());

        assertEquals("newUserValue1", query.properties().get("subMap.typeProfile.myUserQueryProfile.myUserString"));
        assertEquals(845, query.properties().get("subMap.typeProfile.myUserQueryProfile.myUserInteger"));
    }

    @Test
    void testNestedTypeName() {
        ComponentId.resetGlobalCountersForTests();
        QueryProfileRegistry registry = new QueryProfileRegistry();
        QueryProfileType type = new QueryProfileType("testType");
        registry.getTypeRegistry().register(type);
        type.addField(new FieldDescription("ranking.features.query(embedding_profile)",
                        "tensor<float>(model{},x[128])"),
                registry.getTypeRegistry());
        QueryProfile test = new QueryProfile("test");
        registry.register(test);
        test.setType(type);
        CompiledQueryProfileRegistry cRegistry = registry.compile();
        Query query = new Query("?query=foo", cRegistry.getComponent("test"));

        // With a prefix we're not in the built-in type space
        query.properties().set("prefix.ranking.foo", 0.1);
        assertEquals(0.1, query.properties().get("prefix.ranking.foo"));
    }

    @Test
    void testNestedTypeNameUsingBuiltInTypes() {
        ComponentId.resetGlobalCountersForTests();
        QueryProfileRegistry registry = new QueryProfileRegistry();
        QueryProfileType type = new QueryProfileType("testType");
        type.inherited().add(Query.getArgumentType()); // Include native type checking
        registry.getTypeRegistry().register(type);
        type.addField(new FieldDescription("ranking.features.query(embedding_profile)",
                        "tensor<float>(model{},x[128])"),
                registry.getTypeRegistry());
        QueryProfile test = new QueryProfile("test");
        registry.register(test);
        test.setType(type);
        CompiledQueryProfileRegistry cRegistry = registry.compile();
        Query query = new Query("?query=foo", cRegistry.getComponent("test"));

        // Cannot set a property in a strict built-in type
        try {
            query.properties().set("ranking.foo", 0.1);
            fail("Expected exception");
        }
        catch (IllegalArgumentException e) {
            assertEquals("'foo' is not a valid property in 'ranking'. See the query api for valid keys starting by 'ranking'.",
                    e.getCause().getMessage());
        }

        // With a prefix we're not in the built-in type space
        query.properties().set("prefix.ranking.foo", 0.1);
        assertEquals(0.1, query.properties().get("prefix.ranking.foo"));
    }

    private void assertWrongType(QueryProfile profile,String typeName,String name,Object value) {
        try {
            profile.set(name,value, registry);
            fail("Should fail setting " + name + " to " + value);
        }
        catch (IllegalArgumentException e) {
            assertEquals("Could not set '" + name + "' to '" + value + "': '" + value + "' is not a " + typeName,
                         Exceptions.toMessageString(e));
        }
    }

    private void assertNotPermitted(QueryProfile profile,String name,Object value) {
        String localName = CompoundName.from(name).last();
        try {
            profile.set(name, value, registry);
            fail("Should fail setting " + name + " to " + value);
        }
        catch (IllegalArgumentException e) {
            assertTrue(Exceptions.toMessageString(e).startsWith("Could not set '" + name + "' to '" + value + "': '" + localName + "' is not declared"));
        }
    }

    private void assertEmbedQuery(String embed, Tensor expected, Map<String, Embedder> embedders) {
        assertEmbedQuery(embed, expected, embedders, null);
    }

    private void assertEmbedQuery(String embed, Tensor expected, Map<String, Embedder> embedders, String language) {
        QueryProfile profile = new QueryProfile("test");
        profile.setType(testtype);
        registry.register(profile);
        CompiledQueryProfileRegistry cRegistry = registry.compile();

        String languageParam = language == null ? "" : "&language=" + language;
        String destination = "query(myTensor4)";

        Query query = new Query.Builder().setRequest(HttpRequest.createTestRequest(
                                "?" + urlEncode("ranking.features." + destination) +
                                "=" + urlEncode(embed) +
                                languageParam,
                                com.yahoo.jdisc.http.HttpRequest.Method.GET))
                .setQueryProfile(cRegistry.getComponent("test"))
                .setEmbedders(embedders)
                .build();
        assertEquals(0, query.errors().size());
        assertEquals(expected, query.properties().get("ranking.features." + destination));
        assertEquals(expected, query.getRanking().getFeatures().getTensor(destination).get());
    }

    private void assertEmbedQueryFails(String embed, Tensor expected, Map<String, Embedder> embedders, String errMsg) {
        Throwable t = assertThrows(IllegalArgumentException.class, () -> assertEmbedQuery(embed, expected, embedders));
        while (t != null) {
            if (t.getMessage().equals(errMsg)) return;
            t = t.getCause();
        }
        fail("Error '" + errMsg + "' not thrown");
    }

    private static final class MockEmbedder implements Embedder {

        private final String expectedText;
        private final Language expectedLanguage;
        private final Tensor tensorToReturn;

        public MockEmbedder(String expectedText,
                            Language expectedLanguage,
                            Tensor tensorToReturn) {
            this.expectedText = expectedText;
            this.expectedLanguage = expectedLanguage;
            this.tensorToReturn = tensorToReturn;
        }

        @Override
        public List<Integer> embed(String text, Context context) {
            fail("Unexpected call");
            return null;
        }

        @Override
        public Tensor embed(String text, Context context, TensorType tensorType) {
            assertEquals(expectedText, text);
            assertEquals(expectedLanguage, context.getLanguage());
            assertEquals(tensorToReturn.type(), tensorType);
            return tensorToReturn;
        }

    }

}
