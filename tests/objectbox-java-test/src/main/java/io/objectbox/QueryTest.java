package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import io.objectbox.query.QueryBuilder.StringOrder;


import static io.objectbox.TestEntityProperties.SimpleFloat;
import static io.objectbox.TestEntityProperties.SimpleInt;
import static io.objectbox.TestEntityProperties.SimpleLong;
import static io.objectbox.TestEntityProperties.SimpleShort;
import static io.objectbox.TestEntityProperties.SimpleString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class QueryTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void testBuild() {
        Query query = box.query().build();
        assertNotNull(query);
    }

    @Test
    public void testNullNotNull() {
        List<TestEntity> scalars = putTestEntitiesScalars();
        List<TestEntity> strings = putTestEntitiesStrings();
        assertEquals(strings.size(), box.query().notNull(SimpleString).build().count());
        assertEquals(scalars.size(), box.query().isNull(SimpleString).build().count());
    }

    @Test
    public void testScalarEqual() {
        putTestEntitiesScalars();

        Query<TestEntity> query = box.query().equal(SimpleInt, 2007).build();
        assertEquals(1, query.count());
        assertEquals(8, query.findFirst().getId());
        assertEquals(8, query.findUnique().getId());
        List<TestEntity> all = query.find();
        assertEquals(1, all.size());
        assertEquals(8, all.get(0).getId());
    }

    @Test
    public void testNoConditions() {
        List<TestEntity> entities = putTestEntitiesScalars();
        Query<TestEntity> query = box.query().build();
        List<TestEntity> all = query.find();
        assertEquals(entities.size(), all.size());
        assertEquals(entities.size(), query.count());
    }

    @Test
    public void testScalarNotEqual() {
        List<TestEntity> entities = putTestEntitiesScalars();
        Query<TestEntity> query = box.query().notEqual(SimpleInt, 2007).notEqual(SimpleInt, 2002).build();
        assertEquals(entities.size() - 2, query.count());
    }

    @Test
    public void testScalarLessAndGreater() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(SimpleInt, 2003).less(SimpleShort, 207).build();
        assertEquals(3, query.count());
    }

    @Test
    public void testScalarBetween() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().between(SimpleInt, 2003, 2006).build();
        assertEquals(4, query.count());
    }

    @Test
    public void testScalarIn() {
        putTestEntitiesScalars();

        int[] valuesInt = {1, 1, 2, 3, 2003, 2007, 2002, -1};
        Query<TestEntity> query = box.query().in(SimpleInt, valuesInt).build();
        assertEquals(3, query.count());

        long[] valuesLong = {1, 1, 2, 3, 203, 207, 202, -1};
        query = box.query().in(SimpleLong, valuesLong).build();
        assertEquals(3, query.count());
    }

    @Test
    public void testOffsetLimit() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(SimpleInt, 2002).less(SimpleShort, 208).build();
        assertEquals(5, query.count());
        assertEquals(4, query.find(1, 0).size());
        assertEquals(1, query.find(4, 0).size());
        assertEquals(2, query.find(0, 2).size());
        List<TestEntity> list = query.find(1, 2);
        assertEquals(2, list.size());
        assertEquals(2004, list.get(0).getSimpleInt());
        assertEquals(2005, list.get(1).getSimpleInt());
    }

    @Test
    public void testAggregates() {
        putTestEntitiesScalars();

        Query<TestEntity> query = box.query().less(SimpleInt, 2002).build();
        assertEquals(2000.5, query.avg(SimpleInt), 0.0001);
        assertEquals(2000, query.min(SimpleInt), 0.0001);
        assertEquals(20000, query.minDouble(SimpleFloat), 0.001);
        assertEquals(2001, query.max(SimpleInt), 0.0001);
        assertEquals(20000.1, query.maxDouble(SimpleFloat), 0.001);
        assertEquals(4001, query.sum(SimpleInt), 0.0001);
        assertEquals(40000.1, query.sumDouble(SimpleFloat), 0.001);
    }

    @Test
    public void testString() {
        List<TestEntity> entities = putTestEntitiesStrings();
        int count = entities.size();
        assertEquals(1, box.query().equal(SimpleString, "banana").build().findUnique().getId());
        assertEquals(count - 1, box.query().notEqual(SimpleString, "banana").build().count());
        assertEquals(4, box.query().startsWith(SimpleString, "ba").endsWith(SimpleString, "shake").build().findUnique()
                .getId());
        assertEquals(2, box.query().contains(SimpleString, "nana").build().count());
    }

    @Test
    public void testScalarFloatLessAndGreater() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(SimpleFloat, 20000.29f).less(SimpleFloat, 20000.51f).build();
        assertEquals(3, query.count());
    }

    @Test
    // Android JNI seems to have a limit of 512 local jobject references. Internally, we must delete those temporary
    // references when processing lists. This is the test for that.
    public void testBigResultList() {
        List<TestEntity> entities = new ArrayList<>();
        String sameValueForAll = "schrodinger";
        for (int i = 0; i < 10000; i++) {
            TestEntity entity = createTestEntity(sameValueForAll, i);
            entities.add(entity);
        }
        box.put(entities);
        int count = entities.size();
        List<TestEntity> entitiesQueried = box.query().equal(SimpleString, sameValueForAll).build().find();
        assertEquals(count, entitiesQueried.size());
    }

    @Test
    public void testEqualStringOrder() {
        putTestEntitiesStrings();
        box.put(createTestEntity("BAR", 100));
        assertEquals(2, box.query().equal(SimpleString, "bar").build().count());
        assertEquals(1, box.query().equal(SimpleString, "bar", StringOrder.CASE_SENSITIVE).build().count());
    }

    @Test
    public void testOrder() {
        putTestEntitiesStrings();
        box.put(createTestEntity("BAR", 100));
        List<TestEntity> result = box.query().order(SimpleString).build().find();
        assertEquals(6, result.size());
        assertEquals("apple", result.get(0).getSimpleString());
        assertEquals("banana", result.get(1).getSimpleString());
        assertEquals("banana milk shake", result.get(2).getSimpleString());
        assertEquals("bar", result.get(3).getSimpleString());
        assertEquals("BAR", result.get(4).getSimpleString());
        assertEquals("foo bar", result.get(5).getSimpleString());
    }

    @Test
    public void testOrderDescCaseNullLast() {
        box.put(createTestEntity(null, 1000));
        box.put(createTestEntity("BAR", 100));
        putTestEntitiesStrings();
        int flags = QueryBuilder.CASE_SENSITIVE | QueryBuilder.NULLS_LAST | QueryBuilder.DESCENDING;
        List<TestEntity> result = box.query().order(SimpleString, flags).build().find();
        assertEquals(7, result.size());
        assertEquals("foo bar", result.get(0).getSimpleString());
        assertEquals("bar", result.get(1).getSimpleString());
        assertEquals("banana milk shake", result.get(2).getSimpleString());
        assertEquals("banana", result.get(3).getSimpleString());
        assertEquals("apple", result.get(4).getSimpleString());
        assertEquals("BAR", result.get(5).getSimpleString());
        assertNull(result.get(6).getSimpleString());
    }

    @Test
    public void testRemove() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(SimpleInt, 2003).build();
        assertEquals(6, query.remove());
        assertEquals(4, box.count());
    }

    @Test
    public void testFindKeysUnordered() {
        putTestEntitiesScalars();
        assertEquals(10, box.query().build().findKeysUnordered().length);

        Query<TestEntity> query = box.query().greater(SimpleInt, 2006).build();
        long[] keys = query.findKeysUnordered();
        assertEquals(3, keys.length);
        assertEquals(8, keys[0]);
        assertEquals(9, keys[1]);
        assertEquals(10, keys[2]);
    }

    @Test
    public void testSetParameterScalar() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().equal(SimpleInt, 2007).build();
        assertEquals(8, query.findUnique().getId());
        query.setParameter(SimpleInt, 2004);
        assertEquals(5, query.findUnique().getId());
    }

    @Test
    public void testSetParameter2Scalars() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().between(SimpleInt, 2005, 2008).build();
        assertEquals(4, query.count());
        query.setParameters(SimpleInt, 2002, 2003);
        List<TestEntity> entities = query.find();
        assertEquals(2, entities.size());
        assertEquals(3, entities.get(0).getId());
        assertEquals(4, entities.get(1).getId());
    }

    @Test
    public void testSetParameterString() {
        putTestEntitiesStrings();
        Query<TestEntity> query = box.query().equal(SimpleString, "banana").build();
        assertEquals(1, query.findUnique().getId());
        query.setParameter(SimpleString, "bar");
        assertEquals(3, query.findUnique().getId());
    }

    private List<TestEntity> putTestEntitiesScalars() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestEntity entity = createTestEntity(null, i);
            entities.add(entity);
        }
        box.put(entities);
        return entities;
    }

    private TestEntity createTestEntity(String simpleString, int nr) {
        TestEntity entity = new TestEntity();
        entity.setSimpleString(simpleString);
        entity.setSimpleInt(2000 + nr);
        entity.setSimpleShort((short) (200 + nr));
        entity.setSimpleFloat(20000 + nr / 10f);
        entity.setSimpleLong(200 + nr);
        return entity;
    }

    private List<TestEntity> putTestEntitiesStrings() {
        List<TestEntity> entities = new ArrayList<>();
        entities.add(createTestEntity("banana", 1));
        entities.add(createTestEntity("apple", 2));
        entities.add(createTestEntity("bar", 3));
        entities.add(createTestEntity("banana milk shake", 4));
        entities.add(createTestEntity("foo bar", 5));
        box.put(entities);
        return entities;
    }

}
