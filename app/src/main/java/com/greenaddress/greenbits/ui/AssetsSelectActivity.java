package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.ui.components.AssetsAdapter;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.io.IOException;
import java.util.Map;

public class AssetsSelectActivity extends LoggedActivity implements AssetsAdapter.OnAssetSelected {

    private RecyclerView assetsList;

    @Override
    protected void onCreateWithService(Bundle savedInstanceState) {
        if (mService == null || mService.getModel() == null) {
            toFirst();
            return;
        }
        setTitleBackTransparent();
        setContentView(R.layout.activity_assets_selection);
        assetsList = findViewById(R.id.assetsList);
        assetsList.setLayoutManager(new LinearLayoutManager(this));
        Map<String, BalanceData> assetsBalances;
        try {
            assetsBalances = mService.getSession().getBalance(0, 0); // TODO handle subaccounts
            final AssetsAdapter adapter = new AssetsAdapter(assetsBalances, mService, this, getResources(), this);
            assetsList.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            setResult(RESULT_CANCELED);
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        if (mService == null || mService.getModel() == null)
            return;
        if (mService.isDisconnected()) {
            return;
        }
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        if (mService == null || mService.getModel() == null)
            return;
    }

    @Override
    public void onAssetSelected(final String assetSelected) {
        Log.d("ASSET", "selected " + assetSelected);
        final Intent intent = getIntent();
        intent.putExtra(PrefKeys.ASSET_SELECTED, assetSelected);
        setResult(RESULT_OK, intent);
        finish();
    }
}