package com.ferg.afergulator.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.google.android.gms.games.multiplayer.Invitation;

/**
 * Created by Nick on 9/29/13.
 */
public class InvitationDialog extends DialogFragment {

    public interface Callbacks {
        void onInviteAccepted(Invitation aInvite);
        void onInviteDeclined(Invitation aInvite);
    }

    private static final String TAG = InvitationDialog.class.getCanonicalName();

    private Invitation mInvite;
    private Callbacks mCallbacks;

    public InvitationDialog() {}

    public InvitationDialog(Invitation aInvite) {
        mInvite = aInvite;
    }

    public static InvitationDialog newInstance(Invitation aInvite) {
        return new InvitationDialog(aInvite);
    }

    public InvitationDialog setCallbacks(Callbacks aCallbacks) {
        mCallbacks = aCallbacks;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder bldr = new AlertDialog.Builder(getActivity());
        if (mInvite != null && mCallbacks != null) {
            bldr.setTitle("Game Invitation:");
            bldr.setMessage(mInvite.getInviter().getDisplayName() + " invites you to a game.");

            bldr.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mCallbacks.onInviteAccepted(mInvite);
                }
            });

            bldr.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mCallbacks.onInviteDeclined(mInvite);
                }
            });

        } else {
            if (mCallbacks == null)
                bldr.setTitle("No Invite Callbacks!").setMessage("mCallbacks == null");
            if (mInvite == null)
                bldr.setTitle("Empty Invitation!").setMessage("mInvite == null");
        }

        return bldr.create();
    }

    public void show(FragmentManager aMgr) {
        InvitationDialog invite = null;
        if (aMgr != null) {
            invite = (InvitationDialog) aMgr.findFragmentByTag(TAG);
            if (invite != null) invite.dismiss();
            try {
                super.show(aMgr, TAG);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void dismiss() {
        try {
            super.dismiss();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

}
