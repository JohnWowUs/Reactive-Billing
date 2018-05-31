package com.github.lukaspili.reactivebilling.observable;

import android.content.Context;
import android.os.RemoteException;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.github.lukaspili.reactivebilling.BillingService;
import com.github.lukaspili.reactivebilling.model.PurchaseType;
import com.github.lukaspili.reactivebilling.response.GetPurchasesResponse;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;

public class GetPurchasesObservable extends BaseObservable<GetPurchasesResponse> {

    @NonNull public static Observable<GetPurchasesResponse> create(@NonNull Context context,
        @NonNull PurchaseType purchaseType, @Nullable String continuationToken) {
        return Observable.create(new GetPurchasesObservable(context, purchaseType, continuationToken));
    }

    private PurchaseType purchaseType;
    private String continuationToken;

    protected GetPurchasesObservable(Context context, PurchaseType purchaseType, String continuationToken) {
        super(context);
        this.purchaseType = purchaseType;
        this.continuationToken = continuationToken;
    }

    @Override
    protected void onBillingServiceReady(BillingService billingService, ObservableEmitter<? super GetPurchasesResponse> observer) {
        try {
            observer.onNext(billingService.getPurchases(purchaseType, continuationToken));
            observer.onComplete();
        } catch (RemoteException e) {
            observer.onError(e);
        }
    }

}
