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

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 普通对象包装器
 * 继承BaseWrapper类，基于MetaClass实现Object的属性操作。
 * @author Clinton Begin
 */
public class BeanWrapper extends BaseWrapper {

  private final Object object; // 被包装的对象
  private final MetaClass metaClass; // 被包装对象所属类的元类

  public BeanWrapper(MetaObject metaObject, Object object) {
    super(metaObject);
    this.object = object;
    this.metaClass = MetaClass.forClass(object.getClass(), metaObject.getReflectorFactory());
  }

  /**
   * 获取被包装对象中对应表达式的属性值
   * @param prop 属性表达式，注意，该表达式不包含子表达式
   * @return
   *
   * 如果表达式形如：`arr[0]/list[0]/map[key]`，则先获取对应属性`arr/list/map`对象的值，再获取索引对应元素的值。
   * 如果表达式不带索引，则传入的就是个属性名，调用`getBeanProperty`方法，获取属性对应的值。
   */
  @Override
  public Object get(PropertyTokenizer prop) {
    if (prop.getIndex() != null) {
      // 若存在索引信息，则表示该属性表达式中的name部分为集合属性
      // 通过BaseWrapper中的公共方法resolveCollection获取集合对象和集合属性
      Object collection = resolveCollection(prop, object);
      return getCollectionValue(prop, collection);
    }
    // 不存在索引信息，则name部分为普通对象，查找并调用Invoker相关方法获取属性
    return getBeanProperty(prop, object);
  }

  @Override
  public void set(PropertyTokenizer prop, Object value) {
    if (prop.getIndex() != null) {
      // 若存在索引信息，则表示该属性表达式中的name部分为集合属性
      // 通过BaseWrapper中的公共方法resolveCollection获取集合对象和集合属性
      Object collection = resolveCollection(prop, object);
      setCollectionValue(prop, collection, value);
    } else {
      // 不存在索引信息，则name部分为普通对象，查找并调用Invoker相关方法设置属性
      setBeanProperty(prop, object, value);
    }
  }

  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    return metaClass.findProperty(name, useCamelCaseMapping);
  }

  @Override
  public String[] getGetterNames() {
    return metaClass.getGetterNames();
  }

  @Override
  public String[] getSetterNames() {
    return metaClass.getSetterNames();
  }

  @Override
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name); // 解析表达式
    if (!prop.hasNext()) {
      // 不存在子表达式，直接调用metaClass.getSetterType()方法获取属性类型
      // 这里之所以是使用metaClass.getSetterType(name)而不是metaValue.getSetterType(name)
      // 是因为metaValue.getSetterType也是依赖objectWrapper.getSetterType，如果还是调用
      // metaValue.getSetterType会陷入无限递归，metaClass才是递归的出口
      return metaClass.getSetterType(name);
    }
    // 创建MetaObject对象
    // 为什么优先使用MetaObject？而当且仅当封装的对象为空时，才使用MetaClass对象呢？
    // MetaClass封装的是类的元信息，MetaObject封装的是对象的元信息，可以将类的元信息看成是对象元信息的一个子集。
    // 根据类元信息得到的一些类型信息可能更加具体。
    MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
    if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
      // 如果metaValue为SystemMetaObject.NULL_META_OBJECT，表示封装的Java对象值为null
      // 通过类元信息，获取set方法对应属性类型
      return metaClass.getSetterType(name);
    } else {
      // 当对象不为空时，通过对象元信息，获取set方法对应属性类型，可以获得更具体的类型信息
      // 递归判断子表达式的children。然后返回。
      return metaValue.getSetterType(prop.getChildren());
    }
  }

  /**
   * 获取属性表达式对应的属性的get方法的返回类型
   * @param name
   * @return
   */
  @Override
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (!prop.hasNext()) {
      return metaClass.getGetterType(name);
    }
    MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
    if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
      return metaClass.getGetterType(name);
    } else {
      return metaValue.getGetterType(prop.getChildren());
    }
  }

  /**
   * 是否存在属性表达式对应的属性的set方法
   * @param name
   * @return
   */
  @Override
  public boolean hasSetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (!prop.hasNext()) {
      return metaClass.hasSetter(name);
    }
    if (metaClass.hasSetter(prop.getIndexedName())) {
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return metaClass.hasSetter(name);
      } else {
        return metaValue.hasSetter(prop.getChildren());
      }
    } else {
      return false;
    }
  }

  /**
   * 是否存在表达式对应的属性的get方法
   * @param name
   * @return
   */
  @Override
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (!prop.hasNext()) {
      return metaClass.hasGetter(name);
    }
    if (metaClass.hasGetter(prop.getIndexedName())) {
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return metaClass.hasGetter(name);
      } else {
        return metaValue.hasGetter(prop.getChildren());
      }
    } else {
      return false;
    }
  }

  /**
   * 为表达式指定的属性创建对应的MetaObject对象
   * @param name 完整的属性名
   * @param prop 属性名描述符
   * @param objectFactory 对象创建工厂
   * @return
   */
  @Override
  public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    MetaObject metaValue;
    Class<?> type = getSetterType(prop.getName()); // 获取属性表达式指定属性的类型
    try {
      Object newObject = objectFactory.create(type); // 创建对应的属性对象
      // 创建属性对应的MetaObject对象
      metaValue = MetaObject.forObject(newObject, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(),
          metaObject.getReflectorFactory());
      set(prop, newObject); // 为属性所属对象设置对应的属性值
    } catch (Exception e) {
      throw new ReflectionException("Cannot set value of property '" + name + "' because '" + name
          + "' is null and cannot be instantiated on instance of " + type.getName() + ". Cause:" + e.toString(), e);
    }
    return metaValue;
  }

  private Object getBeanProperty(PropertyTokenizer prop, Object object) {
    try {
      // 得到获取属性对应的Invoker对象
      Invoker method = metaClass.getGetInvoker(prop.getName());
      try {
        return method.invoke(object, NO_ARGUMENTS); // 通过Invoker封装的反射操作获取属性值
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable t) {
      throw new ReflectionException(
          "Could not get property '" + prop.getName() + "' from " + object.getClass() + ".  Cause: " + t.toString(), t);
    }
  }

  private void setBeanProperty(PropertyTokenizer prop, Object object, Object value) {
    try {
      Invoker method = metaClass.getSetInvoker(prop.getName());
      Object[] params = { value };
      try {
        method.invoke(object, params);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    } catch (Throwable t) {
      throw new ReflectionException("Could not set property '" + prop.getName() + "' of '" + object.getClass()
          + "' with value '" + value + "' Cause: " + t.toString(), t);
    }
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
  public <E> void addAll(List<E> list) {
    throw new UnsupportedOperationException();
  }

}
