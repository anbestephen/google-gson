/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import com.google.gson.TestTypes.ArrayOfArrays;
import com.google.gson.TestTypes.ArrayOfObjects;
import com.google.gson.TestTypes.BagOfPrimitives;
import com.google.gson.TestTypes.ClassOverridingEquals;
import com.google.gson.TestTypes.ClassWithCustomTypeConverter;
import com.google.gson.TestTypes.ClassWithEnumFields;
import com.google.gson.TestTypes.ClassWithNoFields;
import com.google.gson.TestTypes.ClassWithSerializedNameFields;
import com.google.gson.TestTypes.ClassWithSubInterfacesOfCollection;
import com.google.gson.TestTypes.ClassWithTransientFields;
import com.google.gson.TestTypes.ContainsReferenceToSelfType;
import com.google.gson.TestTypes.MyEnum;
import com.google.gson.TestTypes.Nested;
import com.google.gson.TestTypes.StringWrapper;
import com.google.gson.TestTypes.SubTypeOfNested;
import com.google.gson.annotations.Since;
import com.google.gson.reflect.TypeToken;

import junit.framework.TestCase;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Small test for Json Serialization
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class JsonSerializerTest extends TestCase {
  private Gson gson;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    gson = new Gson();
  }

  public void testCircular() {
    ContainsReferenceToSelfType a = new ContainsReferenceToSelfType();
    ContainsReferenceToSelfType b = new ContainsReferenceToSelfType();
    a.children.add(b);
    b.children.add(a);
    try {
      gson.toJson(a);
      fail("Circular types should not get printed!");
    } catch (IllegalStateException expected) { }
  }

  public void testSelfReference() throws Exception {
    ClassOverridingEquals objA = new ClassOverridingEquals();
    objA.ref = objA;

    try {
      gson.toJson(objA);
      fail("Circular reference to self can not be serialized!");
    } catch (IllegalStateException expected) { }
  }

  public void testObjectEqualButNotSame() throws Exception {
    ClassOverridingEquals objA = new ClassOverridingEquals();
    ClassOverridingEquals objB = new ClassOverridingEquals();
    objB.ref = objA;

    assertEquals(objB.getExpectedJson(), gson.toJson(objB));
  }

  public void testClassWithTransientFields() throws Exception {
    ClassWithTransientFields target = new ClassWithTransientFields(1L);
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testClassWithNoFields() {
    assertEquals("{}", gson.toJson(new ClassWithNoFields()));
  }

  public void testAnonymousLocalClasses() {
    assertEquals("", gson.toJson(new ClassWithNoFields() {
      // empty anonymous class
    }));

    gson = new Gson(new ObjectNavigatorFactory(new ModifierBasedExclusionStrategy(
        true, Modifier.TRANSIENT, Modifier.STATIC), Gson.DEFAULT_NAMING_POLICY));
    assertEquals("{}", gson.toJson(new ClassWithNoFields() {
      // empty anonymous class
    }));
  }

  public void testWriter() throws Exception {
    Writer writer = new StringWriter();
    BagOfPrimitives src = new BagOfPrimitives();
    gson.toJson(src, writer);
    assertEquals(src.getExpectedJson(), writer.toString());
  }

  public void testEmptyCollectionInAnObject() {
    ContainsReferenceToSelfType target = new ContainsReferenceToSelfType();
    assertEquals("{\"children\":[]}", gson.toJson(target));
  }

  public void testArrayOfObjects() {
    ArrayOfObjects target = new ArrayOfObjects();
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testArrayOfArrays() {
    ArrayOfArrays target = new ArrayOfArrays();
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testNested() {
    Nested target = new Nested(new BagOfPrimitives(10, 20, false, "stringValue"),
       new BagOfPrimitives(30, 40, true, "stringValue"));
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testInheritence() {
    SubTypeOfNested target = new SubTypeOfNested(new BagOfPrimitives(10, 20, false, "stringValue"),
        new BagOfPrimitives(30, 40, true, "stringValue"));
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testNull() {
    assertEquals("", gson.toJson(null));
  }

  public void testNullFields() {
    Nested target = new Nested(new BagOfPrimitives(10, 20, false, "stringValue"), null);
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testSubInterfacesOfCollection() {
    List<Integer> list = new LinkedList<Integer>();
    list.add(0);
    list.add(1);
    list.add(2);
    list.add(3);
    Queue<Long> queue = new LinkedList<Long>();
    queue.add(0L);
    queue.add(1L);
    queue.add(2L);
    queue.add(3L);
    Set<Float> set = new TreeSet<Float>();
    set.add(0.1F);
    set.add(0.2F);
    set.add(0.3F);
    set.add(0.4F);
    SortedSet<Character> sortedSet = new TreeSet<Character>();
    sortedSet.add('a');
    sortedSet.add('b');
    sortedSet.add('c');
    sortedSet.add('d');
    ClassWithSubInterfacesOfCollection target =
        new ClassWithSubInterfacesOfCollection(list, queue, set, sortedSet);
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testCustomSerializers() {
    Gson gson = new GsonBuilder().registerSerializer(
        ClassWithCustomTypeConverter.class, new JsonSerializer<ClassWithCustomTypeConverter>() {
      public JsonElement serialize(ClassWithCustomTypeConverter src, Type typeOfSrc,
          JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("bag", 5);
        json.addProperty("value", 25);
        return json;
      }
    }).create();
    ClassWithCustomTypeConverter target = new ClassWithCustomTypeConverter();
    assertEquals("{\"bag\":5,\"value\":25}", gson.toJson(target));
  }

  public void testNestedCustomSerializers() {
    Gson gson = new GsonBuilder().registerSerializer(
        BagOfPrimitives.class, new JsonSerializer<BagOfPrimitives>() {
      public JsonElement serialize(BagOfPrimitives src, Type typeOfSrc,
          JsonSerializationContext context) {
        return new JsonPrimitive(6);
      }
    }).create();
    ClassWithCustomTypeConverter target = new ClassWithCustomTypeConverter();
    assertEquals("{\"bag\":6,\"value\":10}", gson.toJson(target));
  }

  public void testStaticFieldsAreNotSerialized() {
    BagOfPrimitives target = new BagOfPrimitives();
    assertFalse(gson.toJson(target).contains("DEFAULT_VALUE"));
  }

  public void testTopLevelEnum() {
    MyEnum target = MyEnum.VALUE1;
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testClassWithEnumField() {
    ClassWithEnumFields target = new ClassWithEnumFields();
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  static class Version1 {
    int a = 0;
    @Since(1.0) int b = 1;
  }

  static class Version1_1 extends Version1 {
    @Since(1.1) int c = 2;
  }

  @Since(1.2)
  static class Version1_2 {
    int d = 3;
  }

  public void testVersionedClasses() {
    Gson gson = new GsonBuilder().setVersion(1.0).create();
    String json1 = gson.toJson(new Version1());
    String json2 = gson.toJson(new Version1_1());
    assertEquals(json1, json2);
  }

  public void testIgnoreLaterVersionClass() {
    Gson gson = new GsonBuilder().setVersion(1.0).create();
    assertEquals("", gson.toJson(new Version1_2()));
  }

  public void testVersionedGsonWithUnversionedClasses() {
    Gson gson = new GsonBuilder().setVersion(1.0).create();
    BagOfPrimitives target = new BagOfPrimitives(10, 20, false, "stringValue");
    assertEquals(target.getExpectedJson(), gson.toJson(target));
  }

  public void testDefaultSupportForUrl() throws Exception {
    String urlValue = "http://google.com/";
    URL url = new URL(urlValue);
    assertEquals('"' + urlValue + '"', gson.toJson(url));
  }

  public void testDefaultSupportForUri() throws Exception {
    String uriValue = "http://google.com/";
    URI uri = new URI(uriValue);
    assertEquals('"' + uriValue + '"', gson.toJson(uri));
  }

  public void testDefaultSupportForLocaleWithLanguage() throws Exception {
    Locale target = new Locale("en");
    assertEquals("\"en\"", gson.toJson(target));
  }

  public void testDefaultSupportForLocaleWithLanguageCountry() throws Exception {
    Locale target = Locale.CANADA_FRENCH;
    assertEquals("\"fr_CA\"", gson.toJson(target));
  }

  public void testDefaultSupportForLocaleWithLanguageCountryVariant() throws Exception {
    Locale target = new Locale("de", "DE", "EURO");
    String json = gson.toJson(target);
    assertEquals("\"de_DE_EURO\"", json);
  }

  public void testMap() throws Exception {
    Map<String, Integer> map = new LinkedHashMap<String, Integer>();
    map.put("a", 1);
    map.put("b", 2);
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);
    assertTrue(json.contains("\"a\":1"));
    assertTrue(json.contains("\"b\":2"));
  }

  public void testEmptyMap() throws Exception {
    Map<String, Integer> map = new LinkedHashMap<String, Integer>();
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);
    assertEquals("{}", json);
  }

  public void testSingleQuoteInStrings() throws Exception {
    String valueWithQuotes = "beforeQuote'afterQuote";
    String jsonRepresentation = gson.toJson(valueWithQuotes);
    assertEquals(valueWithQuotes, gson.fromJson(jsonRepresentation, String.class));
  }

  public void testEscapingQuotesInStrings() throws Exception {
    String valueWithQuotes = "beforeQuote\"afterQuote";
    String jsonRepresentation = gson.toJson(valueWithQuotes);
    String target = gson.fromJson(jsonRepresentation, String.class);
    assertEquals(valueWithQuotes, target);
  }

  public void testEscapingQuotesInStringArray() throws Exception {
    String[] valueWithQuotes = { "beforeQuote\"afterQuote" };
    String jsonRepresentation = gson.toJson(valueWithQuotes);
    String[] target = gson.fromJson(jsonRepresentation, String[].class);
    assertEquals(1, target.length);
    assertEquals(valueWithQuotes[0], target[0]);
  }

  public void testEscapingObjectFields() throws Exception {
    BagOfPrimitives objWithPrimitives = new BagOfPrimitives(1L, 1, true, "test with\" <script>");
    String jsonRepresentation = gson.toJson(objWithPrimitives);
    assertFalse(jsonRepresentation.contains("<"));
    assertFalse(jsonRepresentation.contains(">"));
    assertTrue(jsonRepresentation.contains("\\\""));

    BagOfPrimitives expectedObject = gson.fromJson(jsonRepresentation, BagOfPrimitives.class);
    assertEquals(objWithPrimitives.getExpectedJson(), expectedObject.getExpectedJson());
  }

  public void testGsonWithNonDefaultFieldNamingPolicy() {
    Gson gson = new GsonBuilder().setFieldNamingPolicy(
        FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    StringWrapper target = new StringWrapper("blah");
    assertEquals("{\"SomeConstantStringInstanceField\":\""
        + target.someConstantStringInstanceField + "\"}", gson.toJson(target));
  }

  public void testGsonWithSerializedNameFieldNamingPolicy() {
    ClassWithSerializedNameFields expected = new ClassWithSerializedNameFields(5);
    String actual = gson.toJson(expected);
    assertEquals(expected.getExpectedJson(), actual);
  }
}