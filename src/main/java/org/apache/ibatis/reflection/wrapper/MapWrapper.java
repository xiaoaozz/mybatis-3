/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection.wrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * Map对象包装器
 * 继承BaseWrapper类，基于Map接口方法实现对属性的操作。
 * @author Clinton Begin
 */
public class MapWrapper extends BaseWrapper {

  private final Map<String, Object> map; // 封装的Map对象

  public MapWrapper(MetaObject metaObject, Map<String, Object> map) {
    super(metaObject);
    this.map = map;
  }

  @Override
  public Object get(PropertyTokenizer prop) {
    if (prop.getIndex() != null) {
      // 存在索引信息，则表示该属性表达式中的name部分为集合属性
      Object collection = resolveCollection(prop, map);
      return getCollectionValue(prop, collection);
    }
    // 不存在索引信息，则name部分为普通对象，直接从map中获取值
    return map.get(prop.getName());
  }

  @Override
  public void set(PropertyTokenizer prop, Object value) {
    if (prop.getIndex() != null) {
      // 存在索引信息，则表示该属性表达式中的name部分为集合属性
      Object collection = resolveCollection(prop, map);
      setCollectionValue(prop, collection, value);
    } else {
      // 不存在索引信息，则name部分为普通对象，直接从map中设置值
      map.put(prop.getName(), value);
    }
  }

  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    return name;
  }

  @Override
  public String[] getGetterNames() {
    return map.keySet().toArray(new String[0]);
  }

  @Override
  public String[] getSetterNames() {
    return map.keySet().toArray(new String[0]);
  }

  @Override
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name); // 根据属性表达式创建PropertyTokenizer对象
    if (prop.hasNext()) {
      // 如果存在子表达式，根据indexedName创建MetaObject对象
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return Object.class; // 如果对应的属性为null，直接返回Object类型
      } else {
        // 否则子表达式由MetaObject处理
        return metaValue.getSetterType(prop.getChildren());
      }
    }
    // 没有子表达式，直接map操作
    if (map.get(name) != null) {
      return map.get(name).getClass();
    } else {
      return Object.class;
    }
  }

  @Override
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return Object.class;
      } else {
        return metaValue.getGetterType(prop.getChildren());
      }
    }
    if (map.get(name) != null) {
      return map.get(name).getClass();
    } else {
      return Object.class;
    }
  }

  @Override
  public boolean hasSetter(String name) {
    return true;
  }

  @Override
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (!prop.hasNext()) {
      return map.containsKey(prop.getName());
    }
    if (map.containsKey(prop.getIndexedName())) {
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return true;
      } else {
        return metaValue.hasGetter(prop.getChildren());
      }
    } else {
      return false;
    }
  }

  /**
   * 针对嵌套属性的场景
   * @param name 完整的属性名
   * @param prop 属性名描述符
   * @param objectFactory 对象创建工厂
   * @return
   *
   * 如：person.name 首次设置person会创建一个key为person，value为new HashMap<>()
   */
  @Override
  public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    HashMap<String, Object> map = new HashMap<>();
    set(prop, map);
    return MetaObject.forObject(map, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(),
        metaObject.getReflectorFactory());
  }

  @Override
  public boolean isCollection() {
    return false;
  }

  @Override
  public void add(Object element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <E> void addAll(List<E> element) {
    throw new UnsupportedOperationException();
  }

}
