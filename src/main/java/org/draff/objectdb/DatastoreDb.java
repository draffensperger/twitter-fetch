package org.draff.objectdb;

import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.DatastoreV1.*;

import static com.google.api.services.datastore.client.DatastoreHelper.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.draff.objectdb.EntityMapper.*;

/**
 * Created by dave on 1/1/16.
 */
public class DatastoreDb implements ObjectDb {
  private DatastoreUtil util;
  private Datastore datastore;

  public DatastoreDb(Datastore datastore) {
    this.util = new DatastoreUtil(datastore);
    this.datastore = datastore;
  }

  public void save(Object object) {
    saveFields(object);
  }

  public void save(List<Object> objects) {
    saveFields(objects);
  }

  public void saveFields(Object object, String... fields) {
    util.saveUpsert(toEntity(object, fields));
  }

  public void saveFields(List<Object> objects, String... fields) {
    List<Entity> entities = objects.stream()
        .map(o -> toEntity(o, fields)).collect(Collectors.toList());
    util.saveUpserts(entities);
  }

  public <T> T findOne(Class<T> clazz) {
    return findOneByFilter(clazz, null);
  }

  public <T> T findOne(Class<T> clazz, Map<String, Object> fieldConstraints) {
    List<Filter> filters = new ArrayList<>();
    fieldConstraints.forEach((field, value) ->
        filters.add(makeFilter(field, PropertyFilter.Operator.EQUAL, toValue(value)).build())
    );
    return findOneByFilter(clazz, makeFilter(filters).build());
  }

  private <T> T findOneByFilter(Class<T> clazz, Filter filter) {
    return clazz.cast(fromEntity(util.findOne(entityKind(clazz), filter), clazz));
  }

  public <T> List<T> findByIds(Class<T> clazz, List<Object> ids) {
    List<Key> keys = ids.stream().map(id -> makeKey(entityKind(clazz), id).build())
        .collect(Collectors.toList());

    return util.findByIds(keys).stream()
        .map(entityResult -> fromEntity(entityResult.getEntity(), clazz))
        .collect(Collectors.toList());
  }

  public <T> T findById(Class<T> clazz, long id) {
    return findByIdObject(clazz, id);
  }

  public <T> T findById(Class<T> clazz, String id) {
    return findByIdObject(clazz, id);
  }

  private <T> T findByIdObject(Class<T> clazz, Object id) {
    return fromEntity(util.findById(makeKey(entityKind(clazz), id).build()), clazz);
  }


  public void delete(Object object) {
    util.saveDelete(makeKey(entityKind(object.getClass()), getObjectId(object)));
  }
}
