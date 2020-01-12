package org.adaway.ui.next;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.util.Function;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.adaway.AdAwayApplication;
import org.adaway.R;
import org.adaway.helper.NotificationHelper;
import org.adaway.helper.PreferenceHelper;
import org.adaway.helper.ThemeHelper;
import org.adaway.model.adblocking.AdBlockMethod;
import org.adaway.model.error.HostError;
import org.adaway.model.update.Manifest;
import org.adaway.ui.help.HelpActivity;
import org.adaway.ui.hosts.HostsSourcesActivity;
import org.adaway.ui.lists.ListsActivity;
import org.adaway.ui.prefs.PrefsActivity;
import org.adaway.ui.tcpdump.TcpdumpLogActivity;
import org.adaway.ui.welcome.WelcomeActivity;
import org.adaway.util.Log;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN;
import static org.adaway.model.adblocking.AdBlockMethod.UNDEFINED;
import static org.adaway.ui.lists.ListsFragment.BLACKLIST_TAB;
import static org.adaway.ui.lists.ListsFragment.REDIRECTION_TAB;
import static org.adaway.ui.lists.ListsFragment.TAB;
import static org.adaway.ui.lists.ListsFragment.WHITELIST_TAB;
import static org.adaway.util.Constants.TAG;

/**
 * This class is the application main activity.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class NextActivity extends AppCompatActivity {
    /**
     * The support link.
     */
    public static final String SUPPORT_LINK = "https://paypal.me/BruceBUJON";
    /**
     * The project link.
     */
    private static final String PROJECT_LINK = "https://github.com/AdAway/AdAway";

    //    protected CoordinatorLayout coordinatorLayout;
    private BottomAppBar appBar;
    private FloatingActionButton fab;
    private BottomSheetBehavior<View> drawerBehavior;
    private NextViewModel nextViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFirstStep();
        ThemeHelper.applyTheme(this);
        NotificationHelper.clearUpdateHostsNotification(this);
        Log.i(TAG, "Starting main activity");
        setContentView(R.layout.next_activity);

        this.nextViewModel = ViewModelProviders.of(this).get(NextViewModel.class);
        this.nextViewModel.isAdBlocked().observe(this, this::notifyAdBlocked);
        this.nextViewModel.getError().observe(this, this::notifyError);

        this.appBar = findViewById(R.id.bar);
        applyActionBar();
        bindAppVersion();
        bindHostCounter();
        bindSourceCounter();
        bindPending();
        bindState();
        bindClickListeners();
        setUpBottomDrawer();
        bindFab();

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (this.showFragment(item.getItemId())) {
                this.drawerBehavior.setState(STATE_HIDDEN);
            }
            return false; // TODO Handle selection
        });
    }

    @Override
    public void onBackPressed() {
        // Hide drawer if expanded
        if (this.drawerBehavior != null && this.drawerBehavior.getState() != STATE_HIDDEN) {
            this.drawerBehavior.setState(STATE_HIDDEN);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return showFragment(item.getItemId());
    }

    private void checkFirstStep() {
        AdBlockMethod adBlockMethod = PreferenceHelper.getAdBlockMethod(this);
        if (adBlockMethod == UNDEFINED) {
            this.startActivity(new Intent(this, WelcomeActivity.class));
            this.finish();
        }
    }

    private void applyActionBar() {
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.hide();
//        }
        setSupportActionBar(this.appBar);
    }

    private void bindAppVersion() {
        TextView versionTextView = findViewById(R.id.versionTextView);
        versionTextView.setText(this.nextViewModel.getVersionName());
        versionTextView.setOnClickListener(this::showChangelog);

        this.nextViewModel.getAppManifest().observe(
                this,
                manifest -> versionTextView.setText(R.string.update_available)
        );
    }

    private void bindHostCounter() {
        Resources resources = getResources();
        Function<Integer, CharSequence> stringMapper = count -> Integer.toString(count);

        TextView blockedHostCountTextView = findViewById(R.id.blockedHostCounterTextView);
        TextView blockedHostTextView = findViewById(R.id.blockedHostTextView);
        LiveData<Integer> blockedHostCount = this.nextViewModel.getBlockedHostCount();
        Transformations.map(blockedHostCount, stringMapper).observe(this, blockedHostCountTextView::setText);
        blockedHostCount.observe(this, count ->
                blockedHostTextView.setText(resources.getQuantityText(R.plurals.blocked_hosts_label, count))
        );

        TextView allowedHostCountTextView = findViewById(R.id.allowedHostCounterTextView);
        TextView allowedHostTextView = findViewById(R.id.allowedHostTextView);
        LiveData<Integer> allowedHostCount = this.nextViewModel.getAllowedHostCount();
        Transformations.map(allowedHostCount, stringMapper).observe(this, allowedHostCountTextView::setText);
        allowedHostCount.observe(this, count ->
                allowedHostTextView.setText(resources.getQuantityText(R.plurals.allowed_hosts_label, count))
        );

        TextView redirectHostCountTextView = findViewById(R.id.redirectHostCounterTextView);
        TextView redirectHostTextView = findViewById(R.id.redirectHostTextView);
        LiveData<Integer> redirectHostCount = this.nextViewModel.getRedirectHostCount();
        Transformations.map(redirectHostCount, stringMapper).observe(this, redirectHostCountTextView::setText);
        redirectHostCount.observe(this, count ->
                redirectHostTextView.setText(resources.getQuantityText(R.plurals.redirect_hosts_label, count))
        );
    }

    private void bindSourceCounter() {
        Resources resources = getResources();

        TextView upToDateSourcesTextView = findViewById(R.id.upToDateSourcesTextView);
        LiveData<Integer> upToDateSourceCount = this.nextViewModel.getUpToDateSourceCount();
        upToDateSourceCount.observe(this, count ->
                upToDateSourcesTextView.setText(resources.getQuantityString(R.plurals.up_to_date_source_label, count, count))
        );

        TextView outdatedSourcesTextView = findViewById(R.id.outdatedSourcesTextView);
        LiveData<Integer> outdatedSourceCount = this.nextViewModel.getOutdatedSourceCount();
        outdatedSourceCount.observe(this, count ->
                outdatedSourcesTextView.setText(resources.getQuantityString(R.plurals.outdated_source_label, count, count))
        );
    }

    private void bindPending() {
        View sourcesImageView = findViewById(R.id.sourcesImageView);
        View sourcesProgressBar = findViewById(R.id.sourcesProgressBar);
        this.nextViewModel.getPending().observe(this, pending -> {
            if (pending) {
                hideView(sourcesImageView);
                showView(sourcesProgressBar);
            } else {
                showView(sourcesImageView);
                hideView(sourcesProgressBar);
            }
        });
    }

    private void bindState() {
        TextView stateTextView = findViewById(R.id.stateTextView);
        this.nextViewModel.getState().observe(this, stateTextView::setText);
    }

    private void bindClickListeners() {
        CardView blockedHostCardView = findViewById(R.id.blockedHostCardView);
        blockedHostCardView.setOnClickListener(v -> startHostListActivity(BLACKLIST_TAB));
        CardView allowedHostCardView = findViewById(R.id.allowedHostCardView);
        allowedHostCardView.setOnClickListener(v -> startHostListActivity(WHITELIST_TAB));
        CardView redirectHostHostCardView = findViewById(R.id.redirectHostCardView);
        redirectHostHostCardView.setOnClickListener(v -> startHostListActivity(REDIRECTION_TAB));
        CardView sourcesCardView = findViewById(R.id.sourcesCardView);
        sourcesCardView.setOnClickListener(this::startHostsSourcesActivity);
        ImageView checkForUpdateImageView = findViewById(R.id.checkForUpdateImageView);
        checkForUpdateImageView.setOnClickListener(this::updateHostsList);
        ImageView updateImageView = findViewById(R.id.updateImageView);
        updateImageView.setOnClickListener(this::syncHostsList);
        CardView helpCardView = findViewById(R.id.helpCardView);
        helpCardView.setOnClickListener(this::startHelpActivity);
        CardView projectCardView = findViewById(R.id.projectCardView);
        projectCardView.setOnClickListener(this::showProjectPage);
        CardView supportCardView = findViewById(R.id.supportCardView);
        supportCardView.setOnClickListener(this::showSupportPage);
    }

    private void setUpBottomDrawer() {
        View bottomDrawer = findViewById(R.id.bottom_drawer);
        this.drawerBehavior = BottomSheetBehavior.from(bottomDrawer);
        this.drawerBehavior.setState(STATE_HIDDEN);

        this.appBar.setNavigationOnClickListener(v -> this.drawerBehavior.setState(STATE_HALF_EXPANDED));
//        bar.setNavigationIcon(R.drawable.ic_menu_24dp);
//        bar.replaceMenu(R.menu.next_actions);
    }

    private void bindFab() {
        this.fab = findViewById(R.id.fab);
        this.fab.setOnClickListener(v -> this.nextViewModel.toggleAdBlocking());
    }

    private boolean showFragment(@IdRes int actionId) {
        switch (actionId) {
            case R.id.drawer_preferences:
                startPrefsActivity();
                this.drawerBehavior.setState(STATE_HIDDEN);
                return true;
            case R.id.drawer_dns_logs:
                startDnsLogActivity();
                this.drawerBehavior.setState(STATE_HIDDEN);
                return true;
            case R.id.action_update:
                syncHostsList(null); // TODO
                return true;
            case R.id.action_show_log:
                // TODO
                break;
        }
        return false;
    }

    private void notifyUpdating(boolean updating) {
        TextView stateTextView = findViewById(R.id.stateTextView);
        if (updating) {
            showView(stateTextView);
        } else {
            hideView(stateTextView);
        }

//        Menu menu = this.appBar.getMenu();
//        MenuItem updateItemMenu = menu.findItem(R.id.action_update);
//        if (updateItemMenu != null) {
//            updateItemMenu.setIcon(updating ? R.drawable.ic_language_red : R.drawable.ic_sync_24dp);
//        }
    }

    private void showView(View view) {
        view.clearAnimation();
        view.setAlpha(0F);
        view.setVisibility(VISIBLE);
        view.animate()
                .alpha(1F)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(VISIBLE);
                    }
                });
    }

    private void hideView(View view) {
        view.clearAnimation();
        view.animate()
                .alpha(0F)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(GONE);
                    }
                });
    }

    /**
     * Start hosts lists activity.
     *
     * @param tab The tab to show.
     */
    private void startHostListActivity(int tab) {
        Intent intent = new Intent(this, ListsActivity.class);
        intent.putExtra(TAB, tab);
        startActivity(intent);
    }

    /**
     * Start hosts source activity.
     *
     * @param view The event source view.
     */
    private void startHostsSourcesActivity(@SuppressWarnings("unused") View view) {
        startActivity(new Intent(this, HostsSourcesActivity.class));
    }

    /**
     * Update the hosts list status.
     *
     * @param view The event source view.
     */
    private void updateHostsList(@SuppressWarnings("unused") View view) {
        notifyUpdating(true);
        this.nextViewModel.update();
    }

    /**
     * Synchronize the hosts list.
     *
     * @param view The event source view.
     */
    private void syncHostsList(@SuppressWarnings("unused") View view) {
        notifyUpdating(true);
        this.nextViewModel.sync();
    }


    /**
     * Start help activity.
     *
     * @param view The source event view.
     */
    private void startHelpActivity(@SuppressWarnings("unused") View view) {
        startActivity(new Intent(this, HelpActivity.class));
    }

    /**
     * Show development page.
     *
     * @param view The source event view.
     */
    private void showProjectPage(@SuppressWarnings("unused") View view) {
        // Show development page
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
        startActivity(browserIntent);
    }

    /**
     * Show support page.
     *
     * @param view The source event view.
     */
    private void showSupportPage(@SuppressWarnings("unused") View view) {
        // Show support dialog
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.baseline_favorite_24)
                .setTitle(R.string.drawer_support_dialog_title)
                .setMessage(R.string.drawer_support_dialog_text)
                .setPositiveButton(R.string.drawer_support_dialog_button, (d, which) -> {
                    // Show support page
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_LINK));
                    startActivity(browserIntent);
                })
                .create()
                .show();
    }

    /**
     * Start preferences activity.
     */
    private void startPrefsActivity() {
        startActivity(new Intent(this, PrefsActivity.class));
    }

    /**
     * Start DNS log activity.
     */
    private void startDnsLogActivity() {
        startActivity(new Intent(this, TcpdumpLogActivity.class));
    }

    private void notifyAdBlocked(boolean adBlocked) {
        FrameLayout layout = findViewById(R.id.headerFrameLayout);
        int color = adBlocked ? getResources().getColor(R.color.primary, null) : Color.GRAY;
        layout.setBackgroundColor(color);
        this.fab.setImageResource(adBlocked ? R.drawable.ic_pause_24dp : R.drawable.icon);
    }

    private void notifyError(HostError error) {
        if (error == null) {
            return;
        }

        notifyUpdating(false);

        String message = getString(error.getDetailsKey()) + "\n\n" + getString(R.string.error_dialog_help);
        new MaterialAlertDialogBuilder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(error.getMessageKey())
                .setMessage(message)
                .setPositiveButton(R.string.button_close, (dialog, id) -> dialog.dismiss())
                .setNegativeButton(R.string.button_help, (dialog, id) -> {
                    dialog.dismiss();
                    startActivity(new Intent(this, HelpActivity.class));
                })
                .create()
                .show();
    }

    private void showChangelog(@SuppressWarnings("unused") View view) {
        // Check manifest
        Manifest manifest = this.nextViewModel.getAppManifest().getValue();
        if (manifest == null) {
            return;
        }
        // Format changelog
        String message = " • " + String.join("\n • ", manifest.changelog.split("\n"));
        // Create dialog
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        if (manifest.updateAvailable) {
            message = getString(R.string.update_update_message) + message;
            dialogBuilder
                    .setMessage(message)
                    .setPositiveButton(R.string.update_update_button, (dialog, which) -> {
                        ((AdAwayApplication) this.getApplication()).getUpdateModel().update();
                        dialog.dismiss();
                    })
                    .setNeutralButton(R.string.button_close, (dialog, which) -> dialog.dismiss());
        } else {
            message = getString(R.string.update_up_to_date_message) + message;
            dialogBuilder.setTitle(R.string.update_up_to_date_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.button_close, (dialog, which) -> dialog.dismiss());
        }
        dialogBuilder.create().show();
    }
}
