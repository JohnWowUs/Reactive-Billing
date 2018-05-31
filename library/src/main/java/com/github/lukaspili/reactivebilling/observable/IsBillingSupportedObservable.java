package com.github.lukaspili.reactivebilling.observable;

import android.content.Context;
import android.os.RemoteException;

import android.support.annotation.NonNull;
import com.github.lukaspili.reactivebilling.BillingService;
import com.github.lukaspili.reactivebilling.model.PurchaseType;
import com.github.lukaspili.reactivebilling.response.Response;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;

public class IsBillingSupportedObservable extends BaseObservable<Response> {

    @NonNull public static Observable<Response> create(@NonNull Context context, @NonNull PurchaseType purchaseType) {
        return Observable.create(new IsBillingSupportedObservable(context, purchaseType));
    }

    private final PurchaseType purchaseType;

    private IsBillingSupportedObservable(Context context, PurchaseType purchaseType) {
        super(context);
        this.purchaseType = purchaseType;
    }

    @Override
    protected void onBillingServiceReady(BillingService billingService, ObservableEmitter<? super Response> observer) {
        try {
            observer.onNext(billingService.isBillingSupported(purchaseType));
            observer.onComplete();
        } catch (RemoteException e) {
            observer.onError(e);
        }
    }
}
