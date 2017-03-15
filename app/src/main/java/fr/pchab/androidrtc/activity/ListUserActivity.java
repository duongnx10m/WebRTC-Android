package fr.pchab.androidrtc.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import fr.pchab.androidrtc.MyApplication;
import fr.pchab.androidrtc.R;
import fr.pchab.androidrtc.adapter.AdapterUser;
import fr.pchab.androidrtc.data.User;
import fr.pchab.androidrtc.data.WebService;

/**
 * Created by duongnx on 3/14/2017.
 */

public class ListUserActivity extends AppCompatActivity implements WebService.OnServerListener, AdapterUser.OnCallListener {
    private View rootView;
    private RecyclerView recyclerView;
    private AdapterUser mAdapter;
    private TextView tvMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.activity_list);
        tvMessage = (TextView) findViewById(R.id.tvMessage);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        if (mAdapter == null) {
            mAdapter = new AdapterUser(this, this);
        }
        recyclerView.setAdapter(mAdapter);

        new WebService(this).execute("https://" + getResources().getString(R.string.host) + "/streams.json");

        TextView tvUserName = (TextView) findViewById(R.id.tvUserName);
        tvUserName.setText(MyApplication.getInstance().getLoginUser());

        findViewById(R.id.btStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ListUserActivity.this, RtcActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onCall(int position) {
        User user = mAdapter.getUsers().get(position);
        if (user != null) {
            Intent intent = new Intent(this, RtcActivity.class);
            intent.putExtra(RtcActivity.KEY_CALLER_ID, user.getId());
            startActivity(intent);
            // ((ListUserActivity) getActivity()).replaceFragment(new FrgVideoChat());
        }
    }

    @Override
    public void onServerComplete(ArrayList<User> users) {
        // Log.d("duongnx","onServerComplete:"+users.size());
        if (users != null && users.size() > 0) {
            mAdapter.setUsers(users);
            mAdapter.notifyDataSetChanged();
            tvMessage.setVisibility(View.GONE);
        } else tvMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onServerFailed() {
        tvMessage.setVisibility(View.VISIBLE);
    }
}
