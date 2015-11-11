/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.library.util.log;

/**
 * A callback interface, the methods will be called when the new log file opened,
 * it is obtained from the description of the log file.
 * <br>EXAMPLES:<br>
 * <code><pre>
 * public class DefaultLogFileDescription implements LogFileDescription {
 *      private final Context mContext;
 *      public DefaultLogFileDescription(Context pContext) {
 *          mContext = pContext;
 *      }
 *      public String getLogFileDescription() {
 *          HashMap<String, String> infos = new HashMap<>();
 *          try {
 *              PackageManager pm = mContext.getPackageManager();
 *              PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
 *              if (pi != null) {
 *                  String versionName = pi.versionName == null ? "null" : pi.versionName;
 *                  String versionCode = pi.versionCode + "";
 *                  infos.put("versionName", versionName);
 *                  infos.put("versionCode", versionCode);
 *              }
 *              Field[] fields = Build.class.getDeclaredFields();
 *              for (Field field : fields) {
 *                  field.setAccessible(true);
 *                  infos.put(field.getName(), field.get(null).toString());
 *              }
 *          } catch (Exception e) {
 *              e.printStackTrace();
 *          }
 *          return infos.toString();
 *      }
 * }
 * </pre></code>
 * <p>NOTICE: The implement should be thread safe.</p>
 *
 * @author John Kenrinus Lee
 * @version 2015-10-30
 * @see DefaultLogFileDescription
 */
public interface LogFileDescription {
    String getLogFileDescription();
}
