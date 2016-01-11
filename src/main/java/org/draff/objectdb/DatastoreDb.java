package org.draff.objectdb;

import com.google.api.services.datastore.DatastoreV1.Entity;
import com.google.api.services.datastore.DatastoreV1.Filter;
import com.google.api.services.datastore.DatastoreV1.Key;
import com.google.api.services.datastore.DatastoreV1.PropertyFilter;
import com.google.api.services.datastore.client.Datastore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.api.services.datastore.client.DatastoreHelper.makeFilter;
import static com.google.api.services.datastore.client.DatastoreHelper.makeKey;
import static org.draff.objectdb.EntityMapper.entityKind;
import static org.draff.objectdb.EntityMapper.fromEntity;
import static org.draff.objectdb.EntityMapper.getObjectId;
import static org.draff.objectdb.EntityMapper.toEntity;
import static org.draff.objectdb.EntityMapper.toValue;

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
    util.saveUpsert(toEntity(object));
  }

  public void saveAll(List<? extends Object> objects) {
    List<Entity> entities = objects.stream()
        .map(o -> toEntity(o)).collect(Collectors.toList());
    util.saveUpserts(entities);
  }

  @Override
  public <T> List<T> find(Class<T> clazz, int limit) {
    return findByFilter(clazz, null, limit);
  }

  @Override
  public <T> List<T> find(Class<T> clazz, Map<String, Object> fieldConstraints, int limit) {
    return findByConstraints(clazz, fieldConstraints, limit);
  }

  @Override
  public <T> T findOne(Class<T> clazz) {
    return firstOrNull(findByFilter(clazz, null, 1));
  }

  @Override
  public <T> T findOne(Class<T> clazz, Map<String, Object> fieldConstraints) {
    return firstOrNull(findByConstraints(clazz, fieldConstraints, 1));
  }

  private <T> T firstOrNull(List<T> list) {
    return list.isEmpty() ? null : list.get(0);
  }

  private <T> List<T> findByConstraints(Class<T> clazz, Map<String, Object> fieldConstraints, int limit) {
    List<Filter> filters = new ArrayList<>();
    fieldConstraints.forEach((field, value) ->
        filters.add(makeFilter(field, PropertyFilter.Operator.EQUAL, toValue(value)).build())
    );
    return findByFilter(clazz, makeFilter(filters).build(), limit);
  }

  private <T> List<T> findByFilter(Class<T> clazz, Filter filter, int limit) {
    return util.find(entityKind(clazz), filter, limit).stream()
        .map(entity -> clazz.cast(fromEntity(entity, clazz))).collect(Collectors.toList());
  }

  public <T> List<T> findByIds(Class<T> clazz, List<Object> ids) {
    List<Key> keys = ids.stream().map(id -> makeKey(entityKind(clazz), id).build())
        .collect(Collectors.toList());

    return util.findByIds(keys).stream()
        .map(entityResult -> fromEntity(entityResult.getEntity(), clazz))
        .collect(Collectors.toList());
  }


  @Override
  public <T> T findById(Class<T> clazz, long id) {
    return findByIdObject(clazz, id);
  }

  @Override
  public <T> T findById(Class<T> clazz, String id) {
    return findByIdObject(clazz, id);
  }

  private <T> T findByIdObject(Class<T> clazz, Object id) {
    return fromEntity(util.findById(makeKey(entityKind(clazz), id).build()), clazz);
  }

  public void delete(Object object) {
    util.saveDelete(objectKey(object));
  }

  public void deleteAll(List<?> objects) {
    util.saveDeletes(objects.stream().map(o -> objectKey(o)).collect(Collectors.toList()));
  }

  private Key.Builder objectKey(Object object) {
    return makeKey(entityKind(object.getClass()), getObjectId(object));
  }
}