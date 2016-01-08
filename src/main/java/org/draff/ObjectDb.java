package org.draff;

import java.util.Map;
import java.util.List;

/**
 * Created by dave on 1/7/16.
 */
public interface ObjectDb {
  void save(Object object);
  void save(List<Object> objects);
  <T> T findOne(Class<T> clazz);
  <T> T findOne(Class<T> clazz, Map<String, Object> fieldConstraints);
  <T> List<T> findByIds(Class<T> clazz, List<Object> ids);
  void delete(Object object);
}
