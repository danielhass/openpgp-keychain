/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;

public abstract class DecryptFragment extends Fragment {
    private static final int RESULT_CODE_LOOKUP_KEY = 0x00007006;

    protected long mSignatureKeyId = 0;

    protected LinearLayout mResultLayout;
    protected RelativeLayout mSignatureLayout;
    protected TextView mResultText;

    protected ImageView mSignatureStatusImage;
    protected TextView mUserId;
    protected TextView mUserIdRest;

    protected Button mLookupKey;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mResultLayout = (LinearLayout) getView().findViewById(R.id.result);
        mResultText = (TextView) getView().findViewById(R.id.result_text);
        mSignatureLayout = (RelativeLayout) getView().findViewById(R.id.result_signature);
        mSignatureStatusImage = (ImageView) getView().findViewById(R.id.ic_signature_status);
        mUserId = (TextView) getView().findViewById(R.id.mainUserId);
        mUserIdRest = (TextView) getView().findViewById(R.id.mainUserIdRest);
        mLookupKey = (Button) getView().findViewById(R.id.lookup_key);
        mLookupKey.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                lookupUnknownKey(mSignatureKeyId);
            }
        });
        mResultLayout.setVisibility(View.GONE);
    }

    private void lookupUnknownKey(long unknownKeyId) {
        Intent intent = new Intent(getActivity(), ImportKeysActivity.class);
        intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER);
        intent.putExtra(ImportKeysActivity.EXTRA_KEY_ID, unknownKeyId);
        startActivityForResult(intent, RESULT_CODE_LOOKUP_KEY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case RESULT_CODE_LOOKUP_KEY: {
                if (resultCode == Activity.RESULT_OK) {
                    // TODO: generate new OpenPgpSignatureResult and display it
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    protected void onResult(PgpDecryptVerifyResult decryptVerifyResult) {
        OpenPgpSignatureResult signatureResult = decryptVerifyResult.getSignatureResult();

        mSignatureKeyId = 0;
        mResultLayout.setVisibility(View.VISIBLE);
        if (signatureResult != null) {
            mSignatureStatusImage.setVisibility(View.VISIBLE);

            mSignatureKeyId = signatureResult.getKeyId();

            String userId = signatureResult.getUserId();
            String[] userIdSplit = KeyRing.splitUserId(userId);
            if (userIdSplit[0] != null) {
                mUserId.setText(userIdSplit[0]);
            } else {
                mUserId.setText(R.string.user_id_no_name);
            }
            if (userIdSplit[1] != null) {
                mUserIdRest.setText(userIdSplit[1]);
            } else {
                mUserIdRest.setText(getString(R.string.label_key_id) + ": "
                        + PgpKeyHelper.convertKeyIdToHex(mSignatureKeyId));
            }

            switch (signatureResult.getStatus()) {
                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED: {
                    if (signatureResult.isSignatureOnly()) {
                        mResultText.setText(R.string.decrypt_result_signature_certified);
                    } else {
                        mResultText.setText(R.string.decrypt_result_decrypted_and_signature_certified);
                    }

                    mResultLayout.setBackgroundColor(getResources().getColor(R.color.android_green_light));
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
                    mSignatureLayout.setVisibility(View.VISIBLE);
                    mLookupKey.setVisibility(View.GONE);
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED: {
                    if (signatureResult.isSignatureOnly()) {
                        mResultText.setText(R.string.decrypt_result_signature_uncertified);
                    } else {
                        mResultText.setText(R.string.decrypt_result_decrypted_and_signature_uncertified);
                    }

                    mResultLayout.setBackgroundColor(getResources().getColor(R.color.android_orange_light));
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
                    mSignatureLayout.setVisibility(View.VISIBLE);
                    mLookupKey.setVisibility(View.GONE);
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_UNKNOWN_PUB_KEY: {
                    if (signatureResult.isSignatureOnly()) {
                        mResultText.setText(R.string.decrypt_result_signature_unknown_pub_key);
                    } else {
                        mResultText.setText(R.string.decrypt_result_decrypted_unknown_pub_key);
                    }

                    mResultLayout.setBackgroundColor(getResources().getColor(R.color.android_orange_light));
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mSignatureLayout.setVisibility(View.VISIBLE);
                    mLookupKey.setVisibility(View.VISIBLE);
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_ERROR: {
                    mResultText.setText(R.string.decrypt_result_invalid_signature);

                    mResultLayout.setBackgroundColor(getResources().getColor(R.color.android_red_light));
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mSignatureLayout.setVisibility(View.GONE);
                    mLookupKey.setVisibility(View.GONE);
                    break;
                }

                default: {
                    mResultText.setText(R.string.error);

                    mResultLayout.setBackgroundColor(getResources().getColor(R.color.android_red_light));
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mSignatureLayout.setVisibility(View.GONE);
                    mLookupKey.setVisibility(View.GONE);
                    break;
                }
            }
        } else {
            mSignatureLayout.setVisibility(View.GONE);
            mLookupKey.setVisibility(View.GONE);

            // successful decryption-only
            mResultLayout.setBackgroundColor(getResources().getColor(R.color.android_purple_light));
            mResultText.setText(R.string.decrypt_result_decrypted);
        }
    }

    protected void showPassphraseDialog(long keyId) {
        PassphraseDialogFragment.show(getActivity(), keyId,
                new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                            String passphrase =
                                    message.getData().getString(PassphraseDialogFragment.MESSAGE_DATA_PASSPHRASE);
                            decryptStart(passphrase);
                        }
                    }
                }
        );
    }

    /**
     * Should be overridden by MessageFragment and FileFragment to start actual decryption
     *
     * @param passphrase
     */
    protected abstract void decryptStart(String passphrase);

}
