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
package org.apache.ibatis.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 类的元数据，或者称为类型描述对象，用于保存类型元数据。
 * 通过Reflector和ReflectorFactory的组合使用，实现对复杂的属性表达式的解析。
 * @author Clinton Begin
 */
public class MetaClass {

  private final ReflectorFactory reflectorFactory; // Reflector的工厂类，具有缓存Reflector对象的功能
  private final Reflector reflector; // 反射器，用于解析和存储目标类中的元信息。创建MetaClass时，会指定一个Class reflector会记录该类的相关信息

  private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
    this.reflectorFactory = reflectorFactory;
    this.reflector = reflectorFactory.findForClass(type); // <settings>标签解析 根据类型创建Reflector
  }

  /**
   * 静态方法创建MetaClass对象
   * @param type 指定类
   * @param reflectorFactory 指定reflectorFactory
   * @return
   */
  public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
    return new MetaClass(type, reflectorFactory); // <settings>标签解析 调用构造方法
  }

  /**
   * 创建类的属性对应的MetaClass对象
   * @param name
   * @return
   */
  public MetaClass metaClassForProperty(String name) {
    Class<?> propType = reflector.getGetterType(name); // 直接从reflector对象的getTypes中获取
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 查找类是否存在名为name的属性
   * @param name 指定查找的属性名
   * @return
   */
  public String findProperty(String name) {
    StringBuilder prop = buildProperty(name, new StringBuilder());
    return prop.length() > 0 ? prop.toString() : null;
  }

  /**
   * findProperty的重载方法
   * @param name 指定查找的属性名
   * @param useCamelCaseMapping 是否使用驼峰命名
   * @return
   */
  public String findProperty(String name, boolean useCamelCaseMapping) {
    if (useCamelCaseMapping) {
      // 因为Reflector中的caseInsensitivePropertyMap属性中保存着类中所有的字段名(全大写)
      // 所以这里没有必要在去除下划线后再将其转化为驼峰，只需要去除下划线即可
      name = name.replace("_", "");
    }
    return findProperty(name);
  }

  /**
   * 获取所有可读属性的name集合
   * @return
   */
  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

  /**
   * 获取所有可写属性的name集合
   * @return
   */
  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

  /**
   * 递归获取属性set的集合
   * @param name
   * @return
   */
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name); // 利用分析器获取对象表达式所表示的属性所对应的set类型
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop.getName());
      return metaProp.getSetterType(prop.getChildren());
    }
    return reflector.getSetterType(prop.getName());
  }

  /**
   * 递归解析表达式表示的属性值的对应类型
   * @param name
   * @return
   */
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 表达式没有children元素即为递归退出条件
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop);
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    return getGetterType(prop);
  }

  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    Class<?> propType = getGetterType(prop);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * getGetterType重载方法，如果prop右下标且为集合则获取泛型类型
   *  例如：
   *      private List<String> list;
   *      List<String> getList();
   *
   *      prop list 返回 java.util.List
   *      prop list[0] 返回 java.util.String
   * @param prop
   * @return
   */
  private Class<?> getGetterType(PropertyTokenizer prop) {
    Class<?> type = reflector.getGetterType(prop.getName()); // 获取返回类型
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
      // 如果有索引，且是Collection接口的子类，比如userList[0]这种形式
      Type returnType = getGenericGetterType(prop.getName()); // 获取集合的泛型类型
      if (returnType instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          // Collection<T>最多只有一个泛型
          returnType = actualTypeArguments[0];
          if (returnType instanceof Class) {
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }
      }
    }
    return type;
  }

  /**
   * 获取属性或者字段的实际类型
   * @param propertyName
   * @return
   */
  private Type getGenericGetterType(String propertyName) {
    try {
      // 获取属性名对应的Invoker对象
      Invoker invoker = reflector.getGetInvoker(propertyName);
      if (invoker instanceof MethodInvoker) {
        // 对应属性有getter方法的情况
        Field declaredMethod = MethodInvoker.class.getDeclaredField("method");
        declaredMethod.setAccessible(true);
        Method method = (Method) declaredMethod.get(invoker);
        // 调用TypeParameterResolver工具类解析getter方法的实际类型
        return TypeParameterResolver.resolveReturnType(method, reflector.getType());
      }
      if (invoker instanceof GetFieldInvoker) {
        // 对应属性没有getter方法的情况
        Field declaredField = GetFieldInvoker.class.getDeclaredField("field");
        declaredField.setAccessible(true);
        Field field = (Field) declaredField.get(invoker);
        // 调用TypeParameterResolver工具类解析字段的实际类型
        return TypeParameterResolver.resolveFieldType(field, reflector.getType());
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      // Ignored
    }
    return null;
  }

  /**
   * 递归判断表示式表示的属性是否存在setter方法
   * @param name
   * @return
   */
  public boolean hasSetter(String name) {
    // <settings>标签解析 解析属性表达式，创建PropertyTokenizer对象
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (!prop.hasNext()) {
      // <settings>标签解析 hasNext返回true，表明name是一个复合属性，即有子表达式
      // <settings>标签解析 调用reflector方法的hasSetter方法
      return reflector.hasSetter(prop.getName());
    }
    if (reflector.hasSetter(prop.getName())) {
      // <settings>标签解析 父级属性名有set方法
      // <settings>标签解析 为属性创建MetaClass对象，递归操作
      MetaClass metaProp = metaClassForProperty(prop.getName());
      // <settings>标签解析 再次调用hasSetter
      return metaProp.hasSetter(prop.getChildren());
    } else {
      return false;
    }
  }

  /**
   * 递归判断表示式表示的属性是否存在getter方法
   * @param name
   * @return
   */
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (!prop.hasNext()) {
      return reflector.hasGetter(prop.getName());
    }
    if (reflector.hasGetter(prop.getName())) {
      MetaClass metaProp = metaClassForProperty(prop);
      return metaProp.hasGetter(prop.getChildren());
    } else {
      return false;
    }
  }

  /**
   * 获取getter方法的Invoker
   * @param name
   * @return
   */
  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

  /**
   * 获取setter方法的Invoker
   * @param name
   * @return
   */
  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  /**
   * 根据传入的表达式，递归查询表达式中的字段是否存在
   * @param name 需要查找的表达式
   * @param builder
   * @return
   */
  private StringBuilder buildProperty(String name, StringBuilder builder) {
    // 解析属性表达式
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 如果有子表达式
      String propertyName = reflector.findPropertyName(prop.getName());
      if (propertyName != null) {
        builder.append(propertyName);
        builder.append(".");
        MetaClass metaProp = metaClassForProperty(propertyName);
        metaProp.buildProperty(prop.getChildren(), builder); // 递归解析子表达式
      }
    } else {
      // 没有子表达式，忽略属性名的大小写
      String propertyName = reflector.findPropertyName(name);
      if (propertyName != null) {
        builder.append(propertyName);
      }
    }
    return builder;
  }

  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
