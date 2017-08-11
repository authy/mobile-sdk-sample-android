package com.twilio.authsample.approvalrequests.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.twilio.auth.TwilioAuth;
import com.twilio.auth.external.ApprovalRequest;
import com.twilio.auth.external.ApprovalRequestLogo;
import com.twilio.auth.external.ApprovalRequestStatus;
import com.twilio.authsample.App;
import com.twilio.authsample.R;
import com.twilio.authsample.approvalrequests.adapters.ApprovalRequestInfoAdapter;
import com.twilio.authsample.registration.RegistrationActivity;
import com.twilio.authsample.utils.AuthyActivityListener;
import com.twilio.authsample.utils.AuthyTask;
import com.twilio.authsample.utils.ImageUtils;
import com.twilio.authsample.utils.MessageHelper;
import com.twilio.authsample.utils.TimeFormattingUtils;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;

public class ApprovalRequestDetailActivity extends AppCompatActivity {

    public static final String PARAM_APPROVAL_REQUEST = "approval_request";

    private RecyclerView approvalRequestAttributes;
    private ImageView transactionImage;
    private TextView transactionMessage;
    private TextView transactionStatusMessage;
    private Button approveButton;
    private Button denyButton;
    private View buttonBar;
    private View transactionStatusContainer;

    private ApprovalRequest approvalRequest;
    private TwilioAuth twilioAuth;
    private Picasso picasso;
    protected MessageHelper messageHelper;

    // Listeners
    private AuthyActivityListener<Void> approvalRequestApprovedListener = new AuthyActivityListener<Void>() {
        @Override
        public void onSuccess(Void result) {
            enableButtons(false);
            messageHelper.show(buttonBar, R.string.approve_success).addCallback(messageDismissedCallback);
        }

        @Override
        public void onError(Exception exception) {
            handleUpdateApprovalRequestError(exception, true);
        }
    };

    private AuthyActivityListener<Void> approvalRequestDeniedListener = new AuthyActivityListener<Void>() {
        @Override
        public void onSuccess(Void result) {
            enableButtons(false);
            messageHelper.show(buttonBar, R.string.deny_success).addCallback(messageDismissedCallback);
        }

        @Override
        public void onError(Exception exception) {
            handleUpdateApprovalRequestError(exception, false);
        }
    };

    private Snackbar.Callback messageDismissedCallback = new Snackbar.Callback() {
        @Override
        public void onDismissed(Snackbar snackbar, int event) {
            super.onDismissed(snackbar, event);
            finish();
        }
    };

    /**
     * Creates an intent to launch the ApprovalRequestDetailActivity
     *
     * @param approvalRequest
     */
    public static Intent createIntent(Context context, ApprovalRequest approvalRequest) {
        final Intent intent = new Intent(context, ApprovalRequestDetailActivity.class);
        intent.putExtra(PARAM_APPROVAL_REQUEST, approvalRequest);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approval_request_detail);

        twilioAuth = ((App) getApplicationContext()).getTwilioAuth();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        approvalRequest = (ApprovalRequest) getIntent().getSerializableExtra(PARAM_APPROVAL_REQUEST);
        picasso = Picasso.with(this);

        messageHelper = new MessageHelper();
        initViews();

        bindViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        messageHelper.getSnackbar().removeCallback(messageDismissedCallback);
        messageHelper.dismiss();
        super.onStop();
    }

    private void initViews() {
        transactionImage = (ImageView) findViewById(R.id.transactionImage);
        transactionMessage = (TextView) findViewById(R.id.transactionMessage);
        transactionStatusMessage = (TextView) findViewById(R.id.transactionStatusMessage);
        buttonBar = findViewById(R.id.buttonBar);
        approveButton = (Button) findViewById(R.id.approveButton);
        denyButton = (Button) findViewById(R.id.denyButton);
        transactionStatusContainer = findViewById(R.id.transactionStatusContainer);
        approvalRequestAttributes = (RecyclerView) findViewById(R.id.approvalRequestAttributes);
        approvalRequestAttributes.setLayoutManager(new LinearLayoutManager(this));
    }


    private void bindViews() {
        // Check status
        checkStatus(approvalRequest);

        // Message
        CharSequence message = TextUtils.isEmpty(approvalRequest.getMessage()) ? "" : Html.fromHtml(approvalRequest.getMessage());
        transactionMessage.setText(message);

        // Images
        final List<? extends ApprovalRequestLogo> images = approvalRequest.getLogos();
        if (images.size() > 0) {
            String imageUrl = ImageUtils.getMostSuitableImageUrl(images);
            picasso.load(imageUrl)
                    .noFade()
                    .placeholder(R.drawable.default_logo)
                    .error(R.drawable.default_logo)
                    .into(transactionImage);
        } else {
            transactionImage.setImageResource(R.drawable.default_logo);
        }

        // Details
        approvalRequestAttributes.setAdapter(new ApprovalRequestInfoAdapter(this, approvalRequest.getDetails()));

        // Buttons
        bindButtons();
    }

    private void checkStatus(ApprovalRequest approvalRequest) {
        // Approval request might just have expired
        if (approvalRequest.isPending()) {
            long expirationTimestamp = approvalRequest.getExpirationTimestamp() * 1000;
            long nowTimestamp = Calendar.getInstance().getTimeInMillis();
            if (nowTimestamp > expirationTimestamp) {
                approvalRequest.setStatus(ApprovalRequestStatus.expired);
            }
        }
    }

    private void bindButtons() {

        approveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                approve();
            }
        });

        denyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deny();
            }
        });

        switch (approvalRequest.getStatus()) {
            case pending:
                enableButtons(true);
                break;
            case expired:
                enableButtons(false);
                String expirationText = TimeFormattingUtils.formatExpirationTime(approvalRequest.getExpirationTimestamp());
                transactionStatusMessage.setText(getString(R.string.transaction_expired_message, expirationText));
                break;
            case approved:
                enableButtons(false);
                transactionStatusMessage.setText(R.string.transaction_approved_message);
                break;
            case denied:
                enableButtons(false);
                transactionStatusMessage.setText(R.string.transaction_denied_message);
                break;

        }
    }

    private void enableButtons(boolean enable) {
        buttonBar.setVisibility(enable ? View.VISIBLE : View.GONE);
        transactionStatusContainer.setVisibility(enable ? View.GONE : View.VISIBLE);
        approveButton.setEnabled(enable);
        denyButton.setEnabled(enable);
    }

    private void deny() {
        new AuthyTask<Void>(approvalRequestDeniedListener) {

            @Override
            public Void executeOnBackground() {
                twilioAuth.denyRequest(approvalRequest);
                return null;
            }
        }.execute();
    }

    private void approve() {
        new AuthyTask<Void>(approvalRequestApprovedListener) {

            @Override
            public Void executeOnBackground() {
                twilioAuth.approveRequest(approvalRequest);
                return null;
            }
        }.execute();
    }


    private void handleUpdateApprovalRequestError(Exception exception, final boolean approveError) {
        Snackbar snackbar = messageHelper.show(buttonBar, approveError ? R.string.approve_failed : R.string.deny_failed);
        Log.e(ApprovalRequestDetailActivity.class.getSimpleName(), approveError ? "Exception while approving request" : "Exception while denying request", exception);

        if (!twilioAuth.isDeviceRegistered()) {
            RegistrationActivity.startRegistrationActivity(this, R.string.registration_error_device_deleted);
            finish();
            return;
        }

        // Create refresh button
        snackbar.setAction(R.string.approval_request_action_refresh, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Refresh UI
                bindViews();
            }
        });
    }
}
