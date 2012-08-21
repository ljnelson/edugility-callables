/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * Copyright (c) 2012-2012 Edugility LLC.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * The original copy of this license is available at
 * http://www.opensource.org/license/mit-license.html.
 */
package com.edugility.callables;

import java.io.Serializable;

import java.lang.reflect.Method;

import java.util.Arrays;

import java.util.concurrent.Callable;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static java.util.logging.Level.FINER;

public class JNDICallable<T> implements Callable<T>, Serializable {

  private static final long serialVersionUID = 1L;

  protected transient Logger logger;

  private transient Context context;

  private boolean cache;

  private final String jndiName;

  private final Class<?> cls;

  private final String methodName;
  
  private final Class<?>[] parameterTypes;

  private transient Object target;

  private transient Method method;

  private Object[] parameterValues;

  public JNDICallable(final String jndiName, final Class<?> cls, final String methodName) {
    this(jndiName, cls, methodName, null, null);
  }

  public JNDICallable(final String jndiName, final Class<?> cls, final String methodName, final Class<?>[] parameterTypes, final Object[] parameterValues) {
    super();
    this.logger = this.createLogger();
    this.jndiName = jndiName;
    this.cls = cls;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
    this.parameterValues = parameterValues;
  }

  public JNDICallable(final Object target, final Method method, final Object... parameterValues) {
    super();
    this.logger = this.createLogger();
    this.jndiName = null;
    this.target = target;
    if (target == null) {
      this.cls = null;
    } else {
      this.cls = target.getClass();
    }
    this.method = method;
    if (method == null) {
      this.methodName = null;
      this.parameterTypes = null;
    } else {
      this.methodName = method.getName();
      this.parameterTypes = method.getParameterTypes();
    }
    this.parameterValues = parameterValues;
  }

  protected Context createContext() throws NamingException {
    return new InitialContext();
  }

  protected Logger createLogger() {
    return Logger.getLogger(this.getClass().getName());
  }

  public Object[] getParameterValues() {
    return this.parameterValues;
  }

  public void setParameterValues(final Object... parameterValues) {
    this.parameterValues = parameterValues;
  }

  public boolean getCache() {
    return this.cache;
  }

  public void setCache(final boolean cache) {
    this.cache = cache;
  }

  @Override
  public T call() throws Exception {
    if (this.logger != null && this.logger.isLoggable(FINER)) {
      this.logger.entering(this.getClass().getName(), "call");
    }
    T returnValue = null;
    
    if (this.method == null && this.methodName != null && this.cls != null) {
      if (this.logger != null && this.logger.isLoggable(FINER)) {
        this.logger.logp(FINER, this.getClass().getName(), "call", String.format("Looking up %s#%s(%s)", this.cls.getName(), this.methodName, toParameterString(this.parameterTypes)));
      }
      this.method = this.cls.getMethod(this.methodName, this.parameterTypes);
      if (this.logger != null && this.logger.isLoggable(FINER)) {
        this.logger.logp(FINER, this.getClass().getName(), "call", String.format("Method to invoke: %s", this.method));
      }
    }
    
    if (this.method != null) {
      if (this.context == null) {
        this.context = this.createContext();
      }
      
      if (this.context != null && this.jndiName != null && (this.target == null || !this.getCache())) {
        if (this.logger != null && this.logger.isLoggable(FINER)) {
          this.logger.logp(FINER, this.getClass().getName(), "call", String.format("Looking up %s in JNDI context %s", this.jndiName, this.context));
        }
        this.target = this.context.lookup(this.jndiName);
        if (this.logger != null && this.logger.isLoggable(FINER)) {
          this.logger.logp(FINER, this.getClass().getName(), "call", String.format("Found %s in JNDI context %s under name %s", this.target, this.context, this.jndiName));
        }
      }
      
      if (this.target != null) {   
        if (this.logger != null && this.logger.isLoggable(FINER)) {
          this.logger.logp(FINER, this.getClass().getName(), "call", String.format("Invoking %s on %s with %s", this.method, this.target, toParameterString(this.parameterValues)));
        }
        @SuppressWarnings("unchecked")
        final T temp = (T)this.method.invoke(this.target, this.parameterValues);
        returnValue = temp;
        if (this.logger != null && this.logger.isLoggable(FINER)) {
          this.logger.logp(FINER, this.getClass().getName(), "call", String.format("Result of invocation: %s", temp));
        }
      }
    }
    
    if (this.logger != null && this.logger.isLoggable(FINER)) {
      this.logger.exiting(this.getClass().getName(), "call", returnValue);
    }
    return returnValue;
  }

  private static final String toParameterString(final Object[] parameterValues) {
    final StringBuilder sb = new StringBuilder();
    if (parameterValues != null) {
      final int size = parameterValues.length;
      for (int i = 0; i < size; i++) {
        final Object o = parameterValues[i];
        if (o != null) {
          sb.append(o.toString());
          if (i < size - 1) {
            sb.append(", ");
          }
        }
      }
    }
    return sb.toString();
  }

  private static final String toParameterString(final Class<?>[] parameterTypes) {
    final StringBuilder sb = new StringBuilder();
    if (parameterTypes != null) {
      final int size = parameterTypes.length;
      for (int i = 0; i < size; i++) {
        final Class<?> c = parameterTypes[i];
        if (c != null) {
          sb.append(c.getName());
          if (i < size - 1) {
            sb.append(", ");
          }
        }
      }
    }
    return sb.toString();
  }

}