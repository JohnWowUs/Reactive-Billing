package com.github.lukaspili.reactivebilling;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.github.lukaspili.reactivebilling.model.Purchase;
import com.github.lukaspili.reactivebilling.parser.PurchaseParser;
import com.github.lukaspili.reactivebilling.response.PurchaseResponse;
import com.jakewharton.rxrelay2.PublishRelay;

import io.reactivex.Observable;


/**
 * Created by lukasz on 06/05/16.
 */
public class PurchaseFlowService {

    private final Context context;
    private final PublishRelay<PurchaseResponse> subject = PublishRelay.create();
    private boolean hasSubscription;
    private final Observable<PurchaseResponse> observable = subject.doOnSubscribe( __ ->
    {
        if (hasSubscription) {
            throw new IllegalStateException("Already has subscription");
        }

        ReactiveBilling.log(null, "Purchase flow - subscribe (thread %s)", Thread.currentThread().getName());
        hasSubscription = true;
    });


    PurchaseFlowService(Context context) {
        this.context = context;
    }

    Observable<PurchaseResponse> getObservable() {
        return observable;
    }

    public void startFlow(PendingIntent buyIntent, Bundle extras) {
        if (!hasSubscription) {
            throw new IllegalStateException("Cannot start flow without subscribers");
        }

        ReactiveBilling.log(null, "Start flow (thread %s)", Thread.currentThread().getName());

        Intent intent = new Intent(context, ReactiveBillingShadowActivity.class);
        intent.putExtra("BUY_INTENT", buyIntent);

        if (extras != null) {
            intent.putExtra("BUY_EXTRAS", extras);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    void onActivityResult(int resultCode, Intent data, Bundle extras) {
        if (!hasSubscription) {
            throw new IllegalStateException("Subject cannot be null when receiving purchase result");
        }

        if (resultCode == Activity.RESULT_OK) {
            ReactiveBilling.log(null, "Purchase flow result - OK (thread %s)", Thread.currentThread().getName());

            int response = data.getIntExtra("RESPONSE_CODE", -1);
            ReactiveBilling.log(null, "Purchase flow result - response: %d (thread %s)", response, Thread.currentThread().getName());

            if (response == 0) {
                String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
                Purchase purchase = PurchaseParser.parse(purchaseData);
                String signature = data.getStringExtra("INAPP_DATA_SIGNATURE");
                subject.accept(new PurchaseResponse(response, purchase, purchaseData, signature, extras, false));
            } else {
                subject.accept(new PurchaseResponse(response, null, null, null, extras, false));
            }
        } else {
            ReactiveBilling.log(null, "Purchase flow result - CANCELED (thread %s)", Thread.currentThread().getName());
            subject.accept(new PurchaseResponse(-1, null, null, null, extras, true));
        }
    }
}
