package com.liskovsoft.sharedutils.rx;

import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.helpers.Helpers;

import app.revanced.extension.shared.utils.Logger;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <a href="https://medium.com/android-news/rxjava-schedulers-what-when-and-how-to-use-it-6cfc27293add">Info about schedulers</a>
 */
public class RxHelper {
    private static @Nullable Scheduler sCachedScheduler;

    private static Scheduler getCachedScheduler() {
        if (sCachedScheduler == null) {
            sCachedScheduler = Schedulers.from(Executors.newCachedThreadPool());
        }

        return sCachedScheduler;
    }

    public static void disposeActions(Disposable... actions) {
        if (actions != null) {
            for (Disposable action : actions) {
                if (isActionRunning(action)) {
                    action.dispose();
                }
            }
        }
    }

    public static void disposeActions(List<Disposable> actions) {
        if (actions != null) {
            for (Disposable action : actions) {
                if (isActionRunning(action)) {
                    action.dispose();
                }
            }
            actions.clear();
        }
    }

    /**
     * NOTE: Don't use it to check that action in completed inside other action (scrollEnd bug).
     */
    public static boolean isAnyActionRunning(Disposable... actions) {
        if (actions != null) {
            for (Disposable action : actions) {
                if (isActionRunning(action)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * NOTE: Don't use it to check that action in completed inside other action (scrollEnd bug).
     */
    public static boolean isAnyActionRunning(List<Disposable> actions) {
        if (actions != null) {
            for (Disposable action : actions) {
                if (isActionRunning(action)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isActionRunning(Disposable action) {
        return action != null && !action.isDisposed();
    }

    public static <T> Disposable execute(Observable<T> observable, @Nullable OnResult<T> onResult, @Nullable OnError onError, @Nullable Runnable onFinish) {
        if (onResult == null) {
            onResult = result -> {}; // ignore result
        }

        if (onError == null) {
            onError = error -> Logger.printException(() -> "Execute error", error);
        }

        if (onFinish == null) {
            onFinish = () -> {};
        }

        return observable
                .subscribe(
                        onResult::onResult,
                        onError::onError,
                        onFinish::run
                );
    }

    public static <T> Disposable execute(Observable<T> observable) {
        return execute(observable, null, null, null);
    }

    public static <T> Disposable execute(Observable<T> observable, OnResult<T> onResult, OnError onError) {
        return execute(observable, onResult, onError, null);
    }

    public static <T> Disposable execute(Observable<T> observable, OnError onError) {
        return execute(observable, null, onError, null);
    }

    public static <T> Disposable execute(Observable<T> observable, Runnable onFinish) {
        return execute(observable, null, null, onFinish);
    }

    public static <T> Disposable execute(Observable<T> observable, OnError onError, Runnable onFinish) {
        return execute(observable, null, onError, onFinish);
    }

    public static Disposable startInterval(Runnable callback, int periodSec) {
        return interval(periodSec, TimeUnit.SECONDS)
                .subscribe(
                        period -> callback.run(),
                        error -> Logger.printException(() -> "startInterval error", error)
                );
    }

    public static Disposable runAsync(Runnable callback) {
        return Completable.fromRunnable(callback)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(() -> {}, error -> Logger.printException(() -> "runAsync failed", error));
    }

    public static Disposable runAsync(Runnable callback, long delayMs) {
        return Completable.fromRunnable(callback)
                .delaySubscription(delayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(() -> {}, error -> Logger.printException(() -> "runAsync failed", error));
    }

    public static Disposable runAsyncUser(Runnable callback) {
        return runAsyncUser(callback, null, null);
    }

    public static Disposable runAsyncUser(Runnable callback, Runnable onFinish) {
        return runAsyncUser(callback, null, onFinish);
    }

    public static Disposable runUser(Runnable callback) {
        return runAsyncUser(() -> {}, null, callback);
    }

    public static Disposable runAsyncUser(Runnable callback, @Nullable OnError onError, @Nullable Runnable onFinish) {
        return Completable.fromRunnable(callback)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            if (onFinish != null) {
                                onFinish.run();
                            }
                        },
                        error -> {
                            if (onError != null) {
                                onError.onError(error);
                            }
                        }
                );
    }

    public static <T> void runBlocking(Observable<T> observable) {
        observable.blockingSubscribe();
    }

    /**
     * <a href="https://stackoverflow.com/questions/43525052/rxjava2-observable-take-throws-undeliverableexception">More info 1</a>
     * <a href="https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling">More info 2</a>
     */
    public static void setupGlobalErrorHandler() {
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                Logger.printException(() -> "Undeliverable exception received, not sure what to do", e);
                return;
            }
            if ((e instanceof IllegalStateException) &&
                    ((e.getCause() instanceof SocketException) ||
                     (e.getCause() instanceof SocketTimeoutException) ||
                     (e.getCause() instanceof UnknownHostException))) {
                // network problems (no internet, failed to connect etc)
                Logger.printException(() -> "Network error", e);
                return;
            }
            if ((e instanceof IllegalStateException) && (e.getCause() == null)) {
                Logger.printException(() -> "Seems that the user forgot to implement error handler", e);
                return;
            }
            if (e instanceof IOException) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if ((e instanceof IllegalStateException) &&
                    (e.getCause() instanceof IOException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }
            if (e instanceof NullPointerException && Helpers.equals(e.getStackTrace()[0].getClassName(), "java.net.SocksSocketImpl")) {
                // Proxy connection error?
                // java.net.SocksSocketImpl.privilegedConnect (SocksSocketImpl.java:94)
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
                return;
            }
            if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
                return;
            }
        });
    }

    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return setup(Observable.create(wrapOnSubscribe(source)));
    }

    public static <T> Observable<T> createLong(ObservableOnSubscribe<T> source) {
        return setupLong(Observable.create(wrapOnSubscribe(source)));
    }

    public static <T> Observable<T> fromCallable(Callable<T> callback) {
        // NOTE: In stock implementation Unhandled NPE crash will happen if callable returns null
        //return setup(Observable.fromCallable(callback));
        return fromNullable(callback);
    }

    @SafeVarargs
    private static <T> Observable<T> fromMultiCallable(Callable<T>... callbacks) {
        return fromMultiNullable(callbacks);
    }

    public static <T> Observable<T> fromIterable(Iterable<T> source) {
        return setup(Observable.fromIterable(source));
    }

    public static Observable<Void> fromRunnable(Runnable callback) {
        return create(emitter -> {
            callback.run();
            emitter.onComplete();
        });
    }

    private static <T> Observable<T> fromNullable(Callable<T> callback) {
        return create(emitter -> {
            T result = callback.call();

            if (result != null) {
                emitter.onNext(result);
                emitter.onComplete();
            } else {
                // Be aware of OnErrorNotImplementedException exception if error handler not implemented!
                // Essential part to notify about problems. Don't remove!
                onError(emitter, "fromNullable result is null");
                Logger.printException(() -> "fromNullable result is null");
            }
        });
    }

    @SafeVarargs
    private static <T> Observable<T> fromMultiNullable(Callable<T>... callbacks) {
        return create(emitter -> {
            boolean success = false;
            for (Callable<T> callback : callbacks) {
                T result = callback.call();

                if (result != null) {
                    emitter.onNext(result);
                    success = true;
                }
            }

            if (success) {
                emitter.onComplete();
            } else {
                // Be aware of OnErrorNotImplementedException exception if error handler not implemented!
                // Essential part to notify about problems. Don't remove!
                onError(emitter, "fromMultiNullable result is null");
                Logger.printException(() -> "fromMultiNullable result is null");
            }
        });
    }

    public static Observable<Long> interval(long period, TimeUnit unit) {
        return setupLong(Observable.interval(period, unit));
    }

    /**
     * Fix fall back on the global error handler.
     * <a href="https://stackoverflow.com/questions/44420422/crash-when-sending-exception-through-rxjava">More info</a><br/>
     * Be aware of {@link OnErrorNotImplementedException} exception if error handler not implemented inside subscribe clause!
     */
    public static <T> void onError(ObservableEmitter<T> emitter, String msg) {
        emitter.tryOnError(new IllegalStateException(msg));
    }

    /**
     * Short running tasks <br/>
     * https://stackoverflow.com/questions/33370339/what-is-the-difference-between-schedulers-io-and-schedulers-computation
     */
    private static <T> Observable<T> setup(Observable<T> observable) {
        // NOTE: Schedulers.io() reuses blocked threads in RxJava 2
        // https://github.com/ReactiveX/RxJava/issues/6542
        return observable
                .subscribeOn(getCachedScheduler())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Long running tasks <br/>
     * https://stackoverflow.com/questions/33370339/what-is-the-difference-between-schedulers-io-and-schedulers-computation
     */
    private static <T> Observable<T> setupLong(Observable<T> observable) {
        // NOTE: Schedulers.io() reuses blocked threads in RxJava 2
        // https://github.com/ReactiveX/RxJava/issues/6542
        // fix blocking (e.g. SponsorBlock not responding)
        return observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Catch errors thrown after Observer is disposed.
     * Such errors cannot be caught anywhere else.
     */
    private static <T> ObservableOnSubscribe<T> wrapOnSubscribe(ObservableOnSubscribe<T> source) {
        return emitter -> {
            try {
                source.subscribe(emitter);
            } catch (Exception e) {
                // Catch errors thrown after Observer is disposed.
                // Such errors cannot be caught anywhere else.
                if (emitter.isDisposed()) {
                    // InterruptedIOException - Thread interrupted. Thread died!!
                    // UnknownHostException: Unable to resolve host (DNS error) Thread died?
                    // Don't rethrow!!! These exceptions cannot be caught inside RxJava!!! Thread died!!!
                    e.printStackTrace();
                } else {
                    throw e;
                }
            }
        };
    }
}
