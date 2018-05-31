package com.github.lukaspili.reactivebilling.sample.inventory;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.lukaspili.reactivebilling.ReactiveBilling;
import com.github.lukaspili.reactivebilling.model.Purchase;
import com.github.lukaspili.reactivebilling.model.PurchaseType;
import com.github.lukaspili.reactivebilling.response.GetPurchasesResponse;
import com.github.lukaspili.reactivebilling.response.Response;
import com.github.lukaspili.reactivebilling.sample.R;
import com.github.lukaspili.reactivebilling.sample.TabsAdapter;
import com.github.lukaspili.reactivebilling.sample.Utils;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by lukasz on 06/05/16.
 */
public class InventoryFragment extends Fragment implements TabsAdapter.Tab {

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter = new InventoryAdapter();

    private Dialog dialog;
    private CompositeDisposable disposables;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        refreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment, container, false);
        recyclerView = (RecyclerView) refreshLayout.findViewById(R.id.recyclerview);

        return refreshLayout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        disposables = new CompositeDisposable();
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                load();
            }
        });

        adapter.bind(new InventoryAdapter.DidClickItem() {
            @Override
            public void onClick(final Purchase purchase) {
                dialog = new AlertDialog.Builder(getContext())
                        .setTitle("Consume item")
                        .setMessage(String.format("Do you want to consume the %s?", purchase.getProductId()))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                consume(purchase);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true)
                        .show();
            }
        });

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        load();
    }

    @Override
    public void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        disposables.dispose();
        super.onDestroy();
    }

    private void load() {
        Log.d(getClass().getName(), "Load inventory");

        disposables.add(ReactiveBilling.getInstance(getContext())
                .getPurchases(PurchaseType.PRODUCT, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getPurchasesResponse ->
                {
                    if (getActivity() == null) return;
                    refreshLayout.setRefreshing(false);
                    didSucceedGetPurchases(getPurchasesResponse);
                },
                        throwable ->
                {
                    if (getActivity() == null) return;
                    refreshLayout.setRefreshing(false);
                    didFailGetPurchases();
                }));
;
    }

    private void didSucceedGetPurchases(GetPurchasesResponse getPurchasesResponse) {
        if (getPurchasesResponse.isSuccess()) {
            disposables.add(
                    Observable.fromIterable(getPurchasesResponse.getList())
                    .map(GetPurchasesResponse.PurchaseResponse::getPurchase)
                    .toList()
                    .subscribe(purchases -> adapter.bind(purchases)));
        } else {
            // error
            Log.d(getClass().getName(), "error");
        }
    }

    private void didFailGetPurchases() {

    }


    // Consume

    private void consume(Purchase purchase) {
        disposables.add(
                ReactiveBilling.getInstance(getContext())
                .consumePurchase(purchase.getPurchaseToken())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::didSucceedConsumePurchase,
                           throwable ->  didFailConsumePurchase()));
    }

    private void didSucceedConsumePurchase(Response response) {
        // reload the list once the product is consumed
        load();

        String title;
        String message;
        if (response.isSuccess()) {
            title = "Product consumed";
            message = "Hope you enjoyed it";
        } else {
            title = "Failed to consume";
            message = Utils.getMessage(response.getResponseCode());
        }

        dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .show();
    }

    private void didFailConsumePurchase() {

    }

    @Override
    public void didFocus() {
        Log.d(getClass().getName(), "Inventory did focus");
        load();
    }
}
