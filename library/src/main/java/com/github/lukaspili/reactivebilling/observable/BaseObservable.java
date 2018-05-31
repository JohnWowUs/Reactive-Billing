package com.github.lukaspili.reactivebilling.observable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;
import com.android.vending.billing.IInAppBillingService;
import com.github.lukaspili.reactivebilling.BillingService;
import com.github.lukaspili.reactivebilling.ReactiveBilling;


import java.util.concurrent.Semaphore;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposables;


public abstract class BaseObservable<T> implements ObservableOnSubscribe<T> {
    @Override
    public void subscribe(ObservableEmitter<T> emitter) throws Exception {
        final Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        intent.setPackage("com.android.vending");

        // don't bother with semaphores if the originating thread is the main thread
        final boolean useSemaphore = Looper.myLooper() != Looper.getMainLooper();
        final Connection connection = new Connection(emitter, useSemaphore);

        ReactiveBilling.log(null, "Bind service (thread %s)", Thread.currentThread().getName());
        try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        } catch (SecurityException e) {
            ReactiveBilling.log(e, "Bind service error");
            emitter.onError(e);

        }
        emitter.setDisposable(
                Disposables.fromAction(() ->
                {
                    ReactiveBilling.log(null, "Unbind service (thread %s)", Thread.currentThread().getName());
                    context.unbindService(connection);
                })
        );

        if (useSemaphore) {
            // freeze the current RX thread until service is connected
            // because bindService() will call the connection callback on the main thread
            // we want to get back on the current RX thread
            ReactiveBilling.log(null, "Acquire semaphore until service is ready (thread %s)", Thread.currentThread().getName());
            semaphore.acquireUninterruptibly();

            // once the semaphore is released
            // it means that the service is connected and available
            //TODO: what happens if the service is never connected?
            deliverBillingService(emitter);
        }
    }

    protected final Context context;
    private final Semaphore semaphore = new Semaphore(0);


    private BillingService billingService;

    BaseObservable(Context context) {
        this.context = context;
    }


    private void deliverBillingService(ObservableEmitter<T>  observer) {
        ReactiveBilling.log(null, "Billing service ready (thread %s)", Thread.currentThread().getName());
        onBillingServiceReady(billingService, observer);
    }

    protected abstract void onBillingServiceReady(BillingService billingService, ObservableEmitter<? super T> observer);

    private class Connection implements ServiceConnection {

        private final ObservableEmitter<T>  observer;
        private final boolean useSemaphore;

        Connection(ObservableEmitter<T> observer, boolean useSemaphore) {
            this.observer = observer;
            this.useSemaphore = useSemaphore;
        }

        /**
         * For some reason, that method is always called on the main thread
         * Regardless of the originating thread executing bindService()
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ReactiveBilling.log(null, "Service connected (thread %s)", Thread.currentThread().getName());

            IInAppBillingService inAppBillingService = IInAppBillingService.Stub.asInterface(service);
            billingService = new BillingService(context, inAppBillingService);

            if (useSemaphore) {
                // once the service is available, release the semaphore
                // that is blocking the originating thread
                ReactiveBilling.log(null, "Release semaphore (thread %s)", Thread.currentThread().getName());
                semaphore.release();
            } else {
                deliverBillingService(observer);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ReactiveBilling.log(null, "Service disconnected (thread %s)", Thread.currentThread().getName());
            billingService = null;
        }
    }
}
