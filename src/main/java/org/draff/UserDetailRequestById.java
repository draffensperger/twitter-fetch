package org.draff;

import org.draff.objectdb.Model;

/**
 * Created by dave on 1/10/16.
 */
public class UserDetailRequestById implements Model {
  long id;
  boolean detailRetrieved = false;
  public UserDetailRequestById() {}
  public UserDetailRequestById(long userId) {
    this.id = userId;
  }
}