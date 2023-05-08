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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ibatis.util.MapUtil;

public class DefaultReflectorFactory implements ReflectorFactory {
  private boolean classCacheEnabled = true; // 该字段决定是否开启对Reflector对象的缓存
  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>(); // 目标类和反射器映射缓存

  public DefaultReflectorFactory() {
  }

  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }

  /**
   * 生成Reflector对象
   * @param type 目标类型
   * @return 目标类型的Reflector对象
   */
  @Override
  public Reflector findForClass(Class<?> type) {
    if (classCacheEnabled) { // 允许混村，默认为true
      // 从缓存中获取Reflector对象，如果缓存为空，则创建一个新的实例，放入缓存中
      return MapUtil.computeIfAbsent(reflectorMap, type, Reflector::new);
    }
    return new Reflector(type); // 创建一个新的Reflector实例
  }

}
