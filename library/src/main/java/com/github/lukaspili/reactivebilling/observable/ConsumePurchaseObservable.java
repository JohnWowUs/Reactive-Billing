package com.github.lukaspili.reactivebilling.observable;

import android.content.Context;
import android.os.RemoteException;

import android.support.annotation.NonNull;
import com.github.lukaspili.reactivebilling.BillingService;
import com.github.lukaspili.reactivebilling.response.Response;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

public class ConsumePurchaseObservable extends BaseObservable<Response> {

    @NonNull public static Observable<Response> create(@NonNull Context context, @NonNull String purchaseToken) {
        return Observable.create(new ConsumePurchaseObservable(context, purchaseToken));
    }

    private final String purchaseToken;

    private ConsumePurchaseObservable(Context context, String purchaseToken) {
        super(context);
        this.purchaseToken = purchaseToken;
    }

    @Override
    protected void onBillingServiceReady(BillingService billingService,ObservableEmitter<? super Response> observer) {
        try {
            observer.onNext(billingService.consumePurchase(purchaseToken));
            observer.onComplete();
        } catch (RemoteException e) {
            observer.onError(e);
        }
    }
}
