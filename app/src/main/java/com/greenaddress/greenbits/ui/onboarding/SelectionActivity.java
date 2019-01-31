package com.greenaddress.greenbits.ui.onboarding;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.blockstream.libwally.Wally;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.PinSaveActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.TabbedMainActivity;
import com.greenaddress.greenbits.ui.UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Random;
import java.util.Set;

public class SelectionActivity extends LoginActivity implements View.OnClickListener {
    private static final int PINSAVE = 1337;

    private String mMnemonic;
    private String mWordSelected;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_onboarding_selection);
        setTitleBackTransparent();
        setTitle("");

        UI.preventScreenshots(this);
        // Generate mnemonic from GDK
        try {
            mMnemonic = getIntent().getDataString();
            Wally.bip39_mnemonic_validate(Wally.bip39_get_wordlist("en"), mMnemonic);
        } catch (final Exception e) {
            finish();
            return;
        }

        // Set up the action bar.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Initialization
        mSectionsPagerAdapter.selectFragment(this, 0);
    }

    private void onMnemonicVerified() {
        startLoading();
        mService.getExecutor().execute(() -> {
            final String mnemonic = mMnemonic;
            try {
                mService.getConnectionManager().connect();
                mService.getSession().registerUser(this, null, mnemonic).resolve(null, null);
                mService.getConnectionManager().loginWithMnemonic(mnemonic, "");
            } catch (final Exception ex) {
                UI.toast(SelectionActivity.this, ex.getMessage(), Toast.LENGTH_LONG);
            }
        });
    }
    @Override
    protected void onLoginSuccess() {
        super.onLoginSuccess();
        stopLoading();
        mService.resetSignUp();
        if(mService.hasPin()) {
            final Intent intent = new Intent(this, TabbedMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            final Intent savePin = PinSaveActivity.createIntent(this, mMnemonic);
            savePin.putExtra("skip_visible", true);
            startActivityForResult(savePin, PINSAVE);
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        if (observable instanceof ConnectionManager) {
            //do not auto redirect if I am in post login, so I can finish setting two-factors
            final ConnectionManager cm = (ConnectionManager) observable;
            if (!cm.isPostLogin()) {
                super.update(observable, o);
            }
        }
    }

    @Override
    protected void onLoginFailure() {
        super.onLoginFailure();
        stopLoading();
        toast("Failed to SignUp");
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case PINSAVE:
            startActivity(new Intent(this, SuccessActivity.class));
            break;
        }
    }

    @Override
    protected void onResumeWithService() {
        //super.onResumeWithService(); not pass activity if logged
        mService.getConnectionManager().addObserver(this);
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        final int selected = mSectionsPagerAdapter.getSelected();
        if (selected == 0)
            super.onBackPressed();
        else
            mSectionsPagerAdapter.selectFragment(this, selected - 1);
    }

    @Override
    public void onClick(final View view) {
        final int selected = mSectionsPagerAdapter.getSelected();
        final String wordExpected = mSectionsPagerAdapter.getFragment(selected)
                                    .getArguments().getString("word");

        // Press string selection button
        mWordSelected = ((Button) view).getText().toString();

        final boolean youWin = wordExpected.equals(mWordSelected);
        if (youWin) {
            // if right, continue or finish (if completed)
            if (selected == mSectionsPagerAdapter.getCount() - 1)
                onMnemonicVerified();
            else
                mSectionsPagerAdapter.selectFragment(this, selected + 1);
            mWordSelected = null;
        } else {
            // if fails, go to the previous page
            UI.toast(this, R.string.id_wrong_choice_check_your, Toast.LENGTH_LONG);
            finish();
        }


    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final int FRAGMENTS_NUMBER = 4;
        private final SelectionFragment[] fragments = new SelectionFragment[4];
        private int selected = 0;
        private List<Integer> indexes;
        private List<String> words;

        public SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);

            words = Arrays.asList(mMnemonic.split(" "));
            final Random random = new Random();
            final Set<Integer> iSet = new HashSet<>();
            while (iSet.size() < 4) {
                iSet.add(random.nextInt(words.size()));
            }
            this.indexes = new ArrayList<>(iSet);

        }

        @Override
        public Fragment getItem(final int index) {
            if (index < FRAGMENTS_NUMBER) {
                // pass splitted words list to fragment
                final String random = words.get(indexes.get(index));
                fragments[index] = SelectionFragment.newInstance(words, random);
                return fragments[index];
            }
            return null;
        }

        @Override
        public int getCount() {
            return FRAGMENTS_NUMBER;
        }

        public int getSelected() {
            return selected;
        }

        public SelectionFragment getFragment(final int index) {
            return fragments[index];
        }

        public void selectFragment(final AppCompatActivity activity, final int index) {
            selected = index;
            activity.getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, getItem(index))
            .commit();
        }
    }
}
