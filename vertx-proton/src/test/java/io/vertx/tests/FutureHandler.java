/*
* Copyright 2016 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.vertx.tests;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract public class FutureHandler<T, X> implements Future<T>, Handler<X> {

  protected ExecutionException exception;
  protected T result;
  protected CountDownLatch latch = new CountDownLatch(1);

  public static <T> FutureHandler<T, T> simple() {
    return new FutureHandler<T, T>() {
      @Override
      synchronized public void handle(T t) {
        result = t;
        latch.countDown();
      }
    };
  }

  public static <T> FutureHandler<T, AsyncResult<T>> asyncResult() {
    return new FutureHandler<T, AsyncResult<T>>() {
      @Override
      synchronized public void handle(AsyncResult<T> t) {
        if (t.succeeded()) {
          result = t.result();
        } else {
          exception = new ExecutionException(t.cause());
        }
        latch.countDown();
      }
    };
  }

  @Override
  abstract public void handle(X t);

  public T get() throws InterruptedException, ExecutionException {
    latch.await();
    return result();
  }

  private T result() throws ExecutionException {
    synchronized (this) {
      if (exception != null) {
        throw exception;
      }
      return result;
    }
  }

  public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
    if (latch.await(timeout, unit)) {
      return result();
    } else {
      throw new TimeoutException();
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return false;
  }
}
