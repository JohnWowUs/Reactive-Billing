package com.github.lukaspili.reactivebilling.observable;

import android.content.Context;

import android.support.annotation.NonNull;
import com.github.lukaspili.reactivebilling.BillingService;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;

public class BillingServiceObservable extends BaseObservable<BillingService> {

    @NonNull public static Observable<BillingService> create(@NonNull Context context) {
        return Observable.create(new BillingServiceObservable(context));
    }

    private BillingServiceObservable(Context context) {
        super(context);
    }

    @Override
    protected void onBillingServiceReady(BillingService billingService, ObservableEmitter<? super BillingService> observer) {
        observer.onNext(billingService);
        observer.onComplete();
    }
}
