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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 */
public final class PropertyNamer {

  private PropertyNamer() {
    // Prevent Instantiation of Static Class 阻止静态类实例化
  }

  /**
   * 通过方法名找出对应的属性
   * @param name 方法名
   * @return
   */
  public static String methodToProperty(String name) {
    if (name.startsWith("is")) {
      name = name.substring(2); // 如果方法名以“is”开头，即boolean类型的属性，则去除“is”
    } else if (name.startsWith("get") || name.startsWith("set")) {
      name = name.substring(3); // 如果方法名以“get”或者“set”开头，则去除
    } else {
      // 否则，抛出异常
      throw new ReflectionException(
          "Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
    }

    if (name.length() == 1 || name.length() > 1 && !Character.isUpperCase(name.charAt(1))) {
      // 处理get、set方法的驼峰式命名，将首字母改为小写
      name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }
    return name;
  }

  public static boolean isProperty(String name) {
    return isGetter(name) || isSetter(name);
  }

  public static boolean isGetter(String name) {
    // 判断是否是getter方法命名。条件：①以get开头并且字符串长度大于3 ②以is开头并且字符串长度大于2
    return name.startsWith("get") && name.length() > 3 || name.startsWith("is") && name.length() > 2;
  }

  public static boolean isSetter(String name) {
    // 判断是否是setter方法命名。条件：以set开头并且字符串长度大于3
    return name.startsWith("set") && name.length() > 3;
  }

}
