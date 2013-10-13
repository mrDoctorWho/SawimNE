package ru.sawim.view;

import DrawControls.icons.Icon;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.models.RosterAdapter;
import sawim.ExternalApi;
import sawim.FileTransfer;
import sawim.chat.ChatHistory;
import sawim.modules.DebugLog;
import sawim.roster.Roster;
import sawim.roster.TreeNode;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.08.13
 * Time: 18:58
 * To change this template use File | Settings | File Templates.
 */
public class SendToView extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {

    public static final String TAG = "SendToView";
    private RosterAdapter allRosterAdapter;
    private Roster roster;
    private HorizontalScrollView horizontalScrollView;
    private LinearLayout protocolBarLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        roster = Roster.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.roster_view, null);
        LinearLayout rosterViewLayout = (LinearLayout) v.findViewById(R.id.roster_view);
        rosterViewLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));

        horizontalScrollView = (HorizontalScrollView) rosterViewLayout.findViewById(R.id.horizontalScrollView);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {Scheme.getColor(Scheme.THEME_CAP_BACKGROUND),Scheme.getColor(Scheme.THEME_BACKGROUND)});
        gd.setCornerRadius(0f);
        horizontalScrollView.setBackgroundDrawable(gd);
        protocolBarLayout = (LinearLayout) horizontalScrollView.findViewById(R.id.protocol_bar);

        ListView allListView = new ListView(getActivity());
        allListView.setCacheColorHint(0x00000000);
        allListView.setScrollingCacheEnabled(false);
        allListView.setAnimationCacheEnabled(false);
        allListView.setDivider(null);
        allListView.setDividerHeight(0);
        allListView.setOnItemClickListener(this);

        allRosterAdapter = new RosterAdapter(getActivity(), roster, Roster.ALL_CONTACTS);
        allListView.setAdapter(allRosterAdapter);
        return allListView;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        TreeNode item = allRosterAdapter.getItem(position);
        if (item.isContact()) {
            Protocol p = roster.getCurrentProtocol();
            Contact c = ((Contact) item);
            Intent intent = getActivity().getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            String type = intent.getType();
            Uri data = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (data == null) return;
            if (type.equals("text/plain")) {
                ChatView newFragment = new ChatView();
                c.activate(p);
                newFragment.initChat(p, c);
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, ChatView.TAG);
                transaction.addToBackStack(null);
                transaction.commit();
                if (intent.getStringExtra(Intent.EXTRA_TEXT) != null)
                    newFragment.insert(intent.getStringExtra(Intent.EXTRA_TEXT));
            } else {
                try {
                    InputStream is = getActivity().getContentResolver().openInputStream(data);
                    FileTransfer fileTransfer = new FileTransfer(getActivity(), p, c);
                    fileTransfer.setFinish(true);
                    fileTransfer.onFileSelect(is, ExternalApi.getFileName(data, getActivity()));
                } catch (FileNotFoundException e) {
                    DebugLog.panic("onFileSelect", e);
                }
            }
        } else if (item.isGroup()) {
            Group group = (Group) item;
            group.setExpandFlag(!group.isExpanded());
        }
        update();
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    private void updateBarProtocols() {
        final int protCount = roster.getProtocolCount();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (protCount > 1) {
                    protocolBarLayout.removeAllViews();
                    for (int i = 0; i < protCount; ++i) {
                        Protocol protocol = roster.getProtocol(i);
                        ImageButton imageBarButtons = new ImageButton(getActivity());
                        imageBarButtons.setOnClickListener(SendToView.this);
                        imageBarButtons.setId(i);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                        lp.gravity = Gravity.CENTER;
                        imageBarButtons.setLayoutParams(lp);
                        imageBarButtons.setBackgroundDrawable(new ColorDrawable(0));
                        if (i == roster.getCurrentItemProtocol())
                            imageBarButtons.setBackgroundColor(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND));
                        imageBarButtons.setImageDrawable(protocol.getCurrentStatusIcon().getImage());
                        Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                        if (null != messageIcon)
                            imageBarButtons.setImageDrawable(messageIcon.getImage());
                        protocolBarLayout.addView(imageBarButtons, i);
                    }
                } else {
                    horizontalScrollView.setVisibility(LinearLayout.GONE);
                    protocolBarLayout.setVisibility(LinearLayout.GONE);
                }
            }
        });

    }

    @Override
    public void onClick(View view) {
        Toast toast = Toast.makeText(getActivity(), roster.getProtocol(view.getId()).getUserId(), Toast.LENGTH_LONG);
        toast.setDuration(5);
        toast.show();
        roster.setCurrentItemProtocol(view.getId());
        update();
    }

    private void update() {
        if (roster == null) return;
        if (roster.getProtocolCount() == 0) return;
        roster.updateOptions();
        updateBarProtocols();
        if (allRosterAdapter != null)
            allRosterAdapter.buildFlatItems();
    }

}
