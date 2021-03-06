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

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class EditSubkeyExpiryDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_CREATION_DATE = "creation_date";
    private static final String ARG_EXPIRY_DATE = "expiry_date";

    public static final int MESSAGE_NEW_EXPIRY_DATE = 1;
    public static final int MESSAGE_CANCEL = 2;
    public static final String MESSAGE_DATA_EXPIRY_DATE = "expiry_date";

    private Messenger mMessenger;
    private Calendar mExpiryCal;

    private DatePicker mDatePicker;

    /**
     * Creates new instance of this dialog fragment
     */
    public static EditSubkeyExpiryDialogFragment newInstance(Messenger messenger,
                                                             Long creationDate, Long expiryDate) {
        EditSubkeyExpiryDialogFragment frag = new EditSubkeyExpiryDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);
        args.putSerializable(ARG_CREATION_DATE, creationDate);
        args.putSerializable(ARG_EXPIRY_DATE, expiryDate);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);
        Date creationDate = new Date(getArguments().getLong(ARG_CREATION_DATE) * 1000);
        Date expiryDate = new Date(getArguments().getLong(ARG_EXPIRY_DATE) * 1000);

        Calendar creationCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        creationCal.setTime(creationDate);
        mExpiryCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        mExpiryCal.setTime(expiryDate);

        Log.d(Constants.TAG, "onCreateDialog");

        // Explicitly not using DatePickerDialog here!
        // DatePickerDialog is difficult to customize and has many problems (see old git versions)
        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

        alert.setTitle(R.string.expiry_date_dialog_title);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.edit_subkey_expiry_dialog, null);
        alert.setView(view);

        mDatePicker = (DatePicker) view.findViewById(R.id.edit_subkey_expiry_date_picker);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            // will crash with IllegalArgumentException if we set a min date
            // that is not before expiry
            if (creationCal.before(mExpiryCal)) {
                mDatePicker.setMinDate(creationCal.getTime().getTime()
                        + DateUtils.DAY_IN_MILLIS);
            } else {
                // when creation date isn't available
                mDatePicker.setMinDate(mExpiryCal.getTime().getTime()
                        + DateUtils.DAY_IN_MILLIS);
            }
        }

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                Calendar selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                //noinspection ResourceType
                selectedCal.set(mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth());

                if (mExpiryCal != null) {
                    long numDays = (selectedCal.getTimeInMillis() / 86400000)
                            - (mExpiryCal.getTimeInMillis() / 86400000);
                    if (numDays > 0) {
                        Bundle data = new Bundle();
                        data.putSerializable(MESSAGE_DATA_EXPIRY_DATE, selectedCal.getTime().getTime() / 1000);
                        sendMessageToHandler(MESSAGE_NEW_EXPIRY_DATE, data);
                    }
                } else {
                    Bundle data = new Bundle();
                    data.putSerializable(MESSAGE_DATA_EXPIRY_DATE, selectedCal.getTime().getTime() / 1000);
                    sendMessageToHandler(MESSAGE_NEW_EXPIRY_DATE, data);
                }
            }
        });

        alert.setNeutralButton(R.string.btn_no_date, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                Bundle data = new Bundle();
                data.putSerializable(MESSAGE_DATA_EXPIRY_DATE, null);
                sendMessageToHandler(MESSAGE_NEW_EXPIRY_DATE, data);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return alert.show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        dismiss();
        sendMessageToHandler(MESSAGE_CANCEL, null);
    }

    /**
     * Send message back to handler which is initialized in a activity
     *
     * @param what Message integer you want to send
     */
    private void sendMessageToHandler(Integer what, Bundle data) {
        Message msg = Message.obtain();
        msg.what = what;
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }
}
