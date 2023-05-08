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

import java.lang.reflect.Field;

import org.apache.ibatis.reflection.Reflector;

/**
 * @author Clinton Begin
 */
public final class PropertyCopier {

  private PropertyCopier() {
    // Prevent Instantiation of Static Class 阻止静态类的实例化
  }

  /**
   * 完成对象的输出复制
   * @param type 对象的类型
   * @param sourceBean 提供属性值的对象
   * @param destinationBean 要被写入新属性值的对象
   */
  public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
    Class<?> parent = type; // 对象的类型
    while (parent != null) {
      final Field[] fields = parent.getDeclaredFields(); // 获取该类的所有属性，不包含继承属性
      // 循环遍历属性进行复制
      for (Field field : fields) {
        try {
          try {
            field.set(destinationBean, field.get(sourceBean));
          } catch (IllegalAccessException e) {
            // 如果无法访问
            if (!Reflector.canControlMemberAccessible()) {
              throw e; // 如果无法访问控制成员，则抛出异常
            }
            field.setAccessible(true); // 修改属性的可访问性
            field.set(destinationBean, field.get(sourceBean)); // 再次复制属性值
          }
        } catch (Exception e) {
          // Nothing useful to do, will only fail on final fields, which will be ignored.
          // 没有做任何有用的操作，只会在final字段失败，该字段将会被忽略。
        }
      }
      parent = parent.getSuperclass(); // 获取父类，继续进行复制
    }
  }

}
