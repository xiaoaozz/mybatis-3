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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;

import org.apache.ibatis.reflection.Reflector;

/**
 * @author Clinton Begin
 */
public class GetFieldInvoker implements Invoker {
  private final Field field; // 属性对应的Field对象

  public GetFieldInvoker(Field field) {
    this.field = field;
  }

  /**
   * 代理方法，获取目标对象的属性值。
   * @param target 被代理的目标对象
   * @param args 方法的参数
   * @return 方法执行结果
   * @throws IllegalAccessException
   */
  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException {
    try {
      return field.get(target); // 直接通过反射获取目标属性值
    } catch (IllegalAccessException e) {
      if (Reflector.canControlMemberAccessible()) {
        // 如果无法访问，修改属性的访问属性
        field.setAccessible(true); // 将属性的可访问性修改为可访问
        return field.get(target); // 再次通过反射获取目标属性的值
      }
      throw e;
    }
  }
  // 获取属性类型
  @Override
  public Class<?> getType() {
    return field.getType();
  }
}
