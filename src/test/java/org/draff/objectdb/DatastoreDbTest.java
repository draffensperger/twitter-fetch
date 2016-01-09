package org.draff.objectdb;

import org.junit.Before;
import org.junit.Test;

import com.jayway.awaitility.Awaitility;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;

import java.util.List;

/**
 * Created by dave on 1/3/16.
 */

class User {
  long id;
  long depthGoal;
  boolean followersRetrieved;
}

class Follower {
  long userId;
  long followerId;
  public String id() {
    return userId + ":" + followerId;
  }
}

class FollowersCursor {
  long id;
  long cursor;
}

public class DatastoreDbTest {
  private DatastoreDb db;

  @Before
  public void setup() {
    db = new DatastoreDb(TestDatastore.get());
    TestDatastore.clean("User", "Follower", "FollowersCursor");
  }

  @Test
  public void testSaveFindOneAndDelete() {
    FollowersCursor cursor = new FollowersCursor();
    cursor.id = 1;
    cursor.cursor = 10;

    assertNull(db.findOne(FollowersCursor.class));

    db.save(cursor);
    waitForEventualSave(FollowersCursor.class);

    FollowersCursor retrievedCursor = db.findOne(FollowersCursor.class);
    assertNotNull(retrievedCursor);

    // Check that they are different objects, i.e. not cached or something
    assertTrue(cursor != retrievedCursor);
    assertEquals(cursor.id, retrievedCursor.id);
    assertEquals(cursor.cursor, retrievedCursor.cursor);

    // Check that the cursor is still there
    assertNotNull(db.findOne(FollowersCursor.class));

    db.delete(cursor);
    waitOnEventualConsistency(() -> db.findOne(Follower.class) == null);

    assertNull(db.findOne(FollowersCursor.class));
  }

  @Test
  public void testSaveMultipleAndConstrainedFind() {
    Follower follower1 = new Follower();
    follower1.userId = 3;
    follower1.followerId = 4;

    Follower follower2 = new Follower();
    follower2.userId = 5;
    follower2.followerId = 6;

    db.save(Arrays.asList(follower1, follower2));
    waitForEventualSave(Follower.class);

    Map<String, Object> constraints1 =
        new ImmutableMap.Builder<String, Object>()
            .put("userId", 3L).build();
    Follower found = db.findOne(Follower.class, constraints1);
    assertNotNull(found);
    assertEquals(3, found.userId);
    assertEquals(4, found.followerId);

    Map<String, Object> constraints2 =
        new ImmutableMap.Builder<String, Object>()
            .put("userId", 5L).put("followerId", 6L).build();
    assertNotNull(db.findOne(Follower.class, constraints2));

    Map<String, Object> constraints3 =
        new ImmutableMap.Builder<String, Object>()
            .put("userId", -5L).put("followerId", 6L).build();
    assertNull(db.findOne(Follower.class, constraints3));
  }

  @Test
  public void testFindByIds() {
    User user1 = new User();
    user1.id = 1;
    user1.depthGoal = 2;
    User user2 = new User();
    user2.id = 2;
    user2.depthGoal = 1;

    db.save(Arrays.asList(user1, user2));
    waitForEventualSave(User.class);

    List<User> users = db.findByIds(User.class, Arrays.asList(1, 2));
    assertEquals(2, users.size());
    User found1 = users.get(0);
    User found2 = users.get(1);
    if (found1.id > found2.id) {
      User temp = found1;
      found2 = found1;
      found1 = temp;
    }

    assertEquals(1, found1.id);
    assertEquals(2, found1.depthGoal);
    assertEquals(2, found2.id);
    assertEquals(1, found2.depthGoal);
  }

  @Test
  public void testFindById() {
    assertNull(db.findById(User.class, 8L));

    User user = new User();
    user.id = 8;
    user.depthGoal = 5;
    db.save(user);
    waitForEventualSave(User.class);

    User found = db.findById(User.class, 8L);
    assertNotNull(found);
    assertEquals(8, found.id);
    assertEquals(5, found.depthGoal);
  }

  @Test
  public void testSaveFields() {
    User user1 = new User();
    user1.id = 7;
    user1.depthGoal = 2;
    user1.followersRetrieved = false;

    User user2 = new User();
    user2.id = 7;
    user2.depthGoal = 1;
    user2.followersRetrieved = true;

    db.saveFields(user1, "depthGoal");
    db.saveFields(user2, "followersRetrieved");
    waitForEventualSave(User.class);

    User found = db.findById(User.class, 7L);
    assertNotNull(found);
    assertEquals(found.id, 7);
    assertEquals(2, found.depthGoal);
    assertEquals(true, found.followersRetrieved);
  }

  private void waitForEventualSave(Class clazz) {
    waitOnEventualConsistency(() -> db.findOne(clazz) != null);
  }

  private void waitOnEventualConsistency(Callable<Boolean> condition) {
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(condition);
  }
}
