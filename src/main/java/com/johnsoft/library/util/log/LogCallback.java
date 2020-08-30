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
 * A callback interface, the methods will be called when check start log level and after each print or write.
 * <br>EXAMPLES:<br>
 * <code><pre>
 * public class DefaultLogCallback implements LogCallback {
 *      public boolean handleCondition(boolean originCondition) {
 *          return originCondition;
 *      }
 *      public void handleError(int level, String tag, String msg, Throwable tr) {
 *          if (level == Log.ERROR) {
 *              MailUtil.sendToMail(msg, tr);
 *          }
 *      }
 * }
 * </pre></code>
 * <p>NOTICE: The implement should be thread safe.</p>
 *
 * @author John Kenrinus Lee
 * @version 2015-10-30
 * @see DefaultLogCallback
 */
public interface LogCallback {
    boolean handleCondition(boolean originCondition, int level, String tag, String msg, Throwable tr);
    void handleError(int level, String tag, String msg, Throwable tr);
}