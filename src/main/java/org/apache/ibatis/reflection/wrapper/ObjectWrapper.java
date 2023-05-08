/*
 *    Copyright 2009-2022 the original author or authors.
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

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 对象包装器接口定义，包装对象后提供统一的属性操作方法
 * @author Clinton Begin
 */
public interface ObjectWrapper {

  Object get(PropertyTokenizer prop); // 获取被包装对象某个属性的值

  void set(PropertyTokenizer prop, Object value); // 设置被包装对象某个属性的值

  String findProperty(String name, boolean useCamelCaseMapping); // 查找属性名称

  String[] getGetterNames(); // 获取所有的属性get方法名称

  String[] getSetterNames(); // 获取所有的属性set方法名称

  Class<?> getSetterType(String name); // 获取指定属性的set方法的参数类型

  Class<?> getGetterType(String name); // 获取指定属性的get方法的返回类型

  boolean hasSetter(String name); // 是否有指定属性的set方法

  boolean hasGetter(String name); // 是否有指定属性的get方法

  /**
   * 实例化某个属性的值，并获取对应的MetaObject对象
   * @param name 完整的属性名
   * @param prop 属性名描述符
   * @param objectFactory 对象创建工厂
   * @return 指定的属性的值对应的MetaObject对象
   */
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  boolean isCollection(); // 判断被包装的对象是否是集合

  void add(Object element); // 往被包装的集合对象中添加新的元素

  <E> void addAll(List<E> element); // 往被包装的集合对象中添加一组元素

}
