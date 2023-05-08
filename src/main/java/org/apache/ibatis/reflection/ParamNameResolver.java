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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 参数名解析器
 */
public class ParamNameResolver {

  public static final String GENERIC_NAME_PREFIX = "param"; // 自动生成的参数名前缀
  private final boolean useActualParamName; // 是否使用实际的参数名(通过反射获取)

  /**
   * <p>
   * The key is the index and the value is the name of the parameter.<br />
   * The name is obtained from {@link Param} if specified. When {@link Param} is not specified, the parameter index is
   * used. Note that this index could be different from the actual index when the method has special parameters (i.e.
   * {@link RowBounds} or {@link ResultHandler}).
   * </p>
   * key是索引，value是参数名称。如果指定@Param，则从@Param中获取。当不指定时，将使用参数索引。
   * 请注意：当方法具有特殊参数RowBounds或者ResultHandler时，该索引可能与实际索引不同。
   * <ul>
   * <li>aMethod(@Param("M") int a, @Param("N") int b) -&gt; {{0, "M"}, {1, "N"}}</li>
   * <li>aMethod(int a, int b) -&gt; {{0, "0"}, {1, "1"}}</li>
   * <li>aMethod(int a, RowBounds rb, int b) -&gt; {{0, "0"}, {2, "1"}}</li>
   * </ul>
   */
  // 方法输入参数的参数次序表。key为参数次序，value为参数名称或者参数@Param注解的值
  private final SortedMap<Integer, String> names;
  // 该方法输入参数中是否还有@Param注解
  private boolean hasParamAnnotation;

  /**
   * 构造方法
   * 将目标方法的参数名依次列举出来。在列举的过程中，如果某个参数存在@Param注解，则会用注解的value替换参数名。
   * (1)优先获取@Param注解的value
   * (2)通过反射获取参数名
   * (3)使用参数下标
   * @param config
   * @param method
   */
  public ParamNameResolver(Configuration config, Method method) {
    // 从配置对象中获取：是否使用实际参数名称 <setting name="useActualParamName" value="true" /> 配置
    this.useActualParamName = config.isUseActualParamName();
    // 反射获取方法参数类型
    final Class<?>[] paramTypes = method.getParameterTypes();
    // 反射获取参数注解
    final Annotation[][] paramAnnotations = method.getParameterAnnotations();
    // key为参数次序，value为参数名称，存放参数的map容器
    final SortedMap<Integer, String> map = new TreeMap<>();
    int paramCount = paramAnnotations.length;
    // get names from @Param annotations
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      if (isSpecialParameter(paramTypes[paramIndex])) {
        // skip special parameters 跳过特殊参数
        /**
         * 跳过RowBounds和ResultHandler参数，这两个参数不做解析
         * RowBounds：处理分页 ResultHandler：处理结果
         */
        continue;
      }
      String name = null;
      for (Annotation annotation : paramAnnotations[paramIndex]) {
        if (annotation instanceof Param) {
          hasParamAnnotation = true;
          // 如果加了@Param注解，则使用value中指定的值
          name = ((Param) annotation).value();
          break;
        }
      }
      if (name == null) {
        // @Param was not specified. 没有使用@Param驻俄籍
        if (useActualParamName) {
          // 如果使用实际的参数名，则通过反射获取参数名
          // JDK8编译类加 -parameters参数可以保留参数名称，否则得到的是arg0,arg1这种无意义的参数名
          name = getActualParamName(method, paramIndex);
        }
        if (name == null) {
          // use the parameter index as the name ("0", "1", ...)
          // gcode issue #71
          // 如果名称还是为null，则可以使用下标来获取参数：#{param1}, #{param2}..
          name = String.valueOf(map.size());
        }
      }
      map.put(paramIndex, name);
    }
    names = Collections.unmodifiableSortedMap(map); // 使其不可变
  }

  private String getActualParamName(Method method, int paramIndex) {
    return ParamNameUtil.getParamNames(method).get(paramIndex);
  }

  private static boolean isSpecialParameter(Class<?> clazz) {
    return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
  }

  /**
   * Returns parameter names referenced by SQL providers.
   *
   * @return the names
   */
  public String[] getNames() {
    return names.values().toArray(new String[0]);
  }

  /**
   *
   * 获取参数名对应的参数值
   * 一般是Map结构，当参数只有1个时，直接返回，xml中写任意值都可以匹配到。
   *
   * <p>
   * A single non-special parameter is returned without a name. Multiple parameters are named using the naming rule. In
   * addition to the default names, this method also adds the generic names (param1, param2, ...).
   * </p>
   *
   * @param args
   *          the args
   *
   * @return the named params
   */
  public Object getNamedParams(Object[] args) {
    final int paramCount = names.size(); // 参数个数
    if (args == null || paramCount == 0) {
      return null; // 无参情况直接返回null
    }
    if (!hasParamAnnotation && paramCount == 1) {
      // 没有@Param注解且参数只有一个
      Object value = args[names.firstKey()];
      // 如果参数是集合类型，参数名会封装成 collection/list/array
      return wrapToMapIfCollection(value, useActualParamName ? names.get(names.firstKey()) : null);
    } else {
      final Map<String, Object> param = new ParamMap<>();
      int i = 0;
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        // 参数名对应实参值
        param.put(entry.getValue(), args[entry.getKey()]);
        // add generic param names (param1, param2, ...) 额外自动生成一个参数名映射：param1, param2...
        final String genericParamName = GENERIC_NAME_PREFIX + (i + 1);
        // ensure not to overwrite parameter named with @Param 确保不要覆盖以@Param命名的参数
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      return param;
    }
  }

  /**
   * Wrap to a {@link ParamMap} if object is {@link Collection} or array.
   * 如果对象是Collection或者数组，则包装为ParamMap。
   *
   * @param object
   *          a parameter object
   * @param actualParamName
   *          an actual parameter name (If specify a name, set an object to {@link ParamMap} with specified name)
   *
   * @return a {@link ParamMap}
   *
   * 如果是集合，通过Collection访问；如果是List，通过List访问，如果是数组，通过Array访问。否则，直接返回。
   *
   * @since 3.5.5
   */
  public static Object wrapToMapIfCollection(Object object, String actualParamName) {
    if (object instanceof Collection) {
      ParamMap<Object> map = new ParamMap<>();
      map.put("collection", object);
      if (object instanceof List) {
        map.put("list", object);
      }
      Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
      return map;
    }
    if (object != null && object.getClass().isArray()) {
      ParamMap<Object> map = new ParamMap<>();
      map.put("array", object);
      Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
      return map;
    }
    return object;
  }

}
