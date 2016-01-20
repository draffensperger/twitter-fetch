package org.draff;

import org.draff.FollowersGoalUpdater;
import org.draff.FollowersGoal;
import org.draff.FollowersTracker;
import org.draff.UserDetail;
import org.draff.objectdb.DatastoreDb;
import org.draff.support.TestDatastore;
import org.junit.Before;
import org.junit.Test;

import twitter4j.*;
import twitter4j.api.UsersResources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.draff.support.EventualConsistencyHelper.waitForEventualDelete;
import static org.draff.support.EventualConsistencyHelper.waitForEventualSave;
import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/**
 * Created by dave on 1/13/16.
 */
public class FollowersGoalUpdaterTest {
  private DatastoreDb db;

  @Before
  public void setup() {
    db = new DatastoreDb(TestDatastore.get());
    TestDatastore.clean();
  }

  @Test
  public void testRetrieveFollowersGoalDetails() throws TwitterException {
    FollowersGoal goal1 = new FollowersGoal();
    goal1.id = "user1";
    goal1.depthGoal = 1;
    FollowersGoal goal2 = new FollowersGoal();
    goal2.id = "user2";
    goal2.depthGoal = 2;
    db.saveAll(Arrays.asList(goal1, goal2));
    waitForEventualSave(FollowersGoal.class);

    FollowersTracker existingFollowersTracker = new FollowersTracker();
    existingFollowersTracker.id = 10;
    db.save(existingFollowersTracker);
    waitForEventualSave(FollowersTracker.class);

    new FollowersGoalUpdater(db, mockUserResources()).retrieveFollowersGoalDetails();

    waitForEventualDelete(FollowersGoal.class);
    assertNull(db.findOne(FollowersGoal.class));

    UserDetail userDetail1 = db.findById(UserDetail.class, 10);
    assertNotNull(userDetail1);
    assertEquals("user1", userDetail1.screenName);

    UserDetail userDetail2 = db.findById(UserDetail.class, 20);
    assertNotNull(userDetail2);
    assertEquals("user2", userDetail2.screenName);

    FollowersTracker followersTracker1 = db.findById(FollowersTracker.class, 10);
    assertNotNull(followersTracker1);
    assertFalse(followersTracker1.shouldRetrieveLevel2Followers);

    FollowersTracker followersTracker2 = db.findById(FollowersTracker.class, 20);
    assertNotNull(followersTracker2);
    assertTrue(followersTracker2.shouldRetrieveLevel2Followers);
  }

  private UsersResources mockUserResources() throws TwitterException {
    List<User> users = new ArrayList<>();
    String user1JSON = "{\"id\":10,\"screen_name\":\"user1\"}";
    String user2JSON = "{\"id\":20,\"screen_name\":\"user2\"}";
    users.add(TwitterObjectFactory.createUser(user1JSON));
    users.add(TwitterObjectFactory.createUser(user2JSON));

    UsersResources userResources = mock(UsersResources.class);

    ResponseList<User> usersResponse = mock(ResponseList.class, delegatesTo(users));
    when(userResources.lookupUsers(new String[] {"user1", "user2"})).thenReturn(usersResponse);
    when(userResources.lookupUsers(new long[] {10, 20})).thenReturn(usersResponse);
    when(userResources.lookupUsers(new long[] {20, 10})).thenReturn(usersResponse);

    return userResources;
  }
}