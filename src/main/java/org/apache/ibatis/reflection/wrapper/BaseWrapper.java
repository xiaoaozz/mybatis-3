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

import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 基础包装器，实现ObjectWrapper接口。
 * 为子类BeanWrapper和MapWrapper提供公共的方法和属性。
 * @author Clinton Begin
 */
public abstract class BaseWrapper implements ObjectWrapper {

  protected static final Object[] NO_ARGUMENTS = {}; // 无参，主要用于执行get方法所需
  protected final MetaObject metaObject; // 被包装对象的元数据对象

  protected BaseWrapper(MetaObject metaObject) {
    this.metaObject = metaObject;
  }

  /**
   * 解析对象中的集合名
   * 根据属性表达式获取对应属性的集合(Array、List、Map)对象，调用MetaObject的getValue()方法获取。
   * @param prop PropertyTokenizer对象
   * @param object 指定Object对象
   * @return
   */
  protected Object resolveCollection(PropertyTokenizer prop, Object object) {
    if ("".equals(prop.getName())) {
      // 如果表达式不合法解析不到属性名，则直接返回默认值
      return object;
    }
    // 解析到属性名，调用metaObject.getValue()方法获取属性值并返回
    return metaObject.getValue(prop.getName());
  }

  /**
   * 根据属性表达式获取集合(Array、List、Map)的值
   * @param prop  PropertyTokenizer对象
   * @param collection 集合(Array、List、Map)
   * @return 对应下标或key的值
   */
  protected Object getCollectionValue(PropertyTokenizer prop, Object collection) {
    if (collection instanceof Map) {
      return ((Map) collection).get(prop.getIndex()); // 如果是Map类型，则index为key，例如map['key']
    }
    int i = Integer.parseInt(prop.getIndex());
    // 如果是其他类型，则index为下标，例如list[0]
    if (collection instanceof List) {
      return ((List) collection).get(i);
    } else if (collection instanceof Object[]) {
      return ((Object[]) collection)[i];
    } else if (collection instanceof char[]) {
      return ((char[]) collection)[i];
    } else if (collection instanceof boolean[]) {
      return ((boolean[]) collection)[i];
    } else if (collection instanceof byte[]) {
      return ((byte[]) collection)[i];
    } else if (collection instanceof double[]) {
      return ((double[]) collection)[i];
    } else if (collection instanceof float[]) {
      return ((float[]) collection)[i];
    } else if (collection instanceof int[]) {
      return ((int[]) collection)[i];
    } else if (collection instanceof long[]) {
      return ((long[]) collection)[i];
    } else if (collection instanceof short[]) {
      return ((short[]) collection)[i];
    } else {
      // 不是集合类型，则抛出异常
      throw new ReflectionException(
          "The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
    }
  }

  /**
   * 根据(参数prop)设置集合(Array、List、Map)的值
   * @param prop
   * @param collection
   * @param value
   */
  protected void setCollectionValue(PropertyTokenizer prop, Object collection, Object value) {
    if (collection instanceof Map) {
      ((Map) collection).put(prop.getIndex(), value); // 如果是Map类型，则index为key
    } else {
      int i = Integer.parseInt(prop.getIndex());
      // 如果是其他类型，则index为下标
      if (collection instanceof List) {
        ((List) collection).set(i, value);
      } else if (collection instanceof Object[]) {
        ((Object[]) collection)[i] = value;
      } else if (collection instanceof char[]) {
        ((char[]) collection)[i] = (Character) value;
      } else if (collection instanceof boolean[]) {
        ((boolean[]) collection)[i] = (Boolean) value;
      } else if (collection instanceof byte[]) {
        ((byte[]) collection)[i] = (Byte) value;
      } else if (collection instanceof double[]) {
        ((double[]) collection)[i] = (Double) value;
      } else if (collection instanceof float[]) {
        ((float[]) collection)[i] = (Float) value;
      } else if (collection instanceof int[]) {
        ((int[]) collection)[i] = (Integer) value;
      } else if (collection instanceof long[]) {
        ((long[]) collection)[i] = (Long) value;
      } else if (collection instanceof short[]) {
        ((short[]) collection)[i] = (Short) value;
      } else {
        // 不是集合类型，则抛出异常
        throw new ReflectionException(
            "The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
      }
    }
  }

}
