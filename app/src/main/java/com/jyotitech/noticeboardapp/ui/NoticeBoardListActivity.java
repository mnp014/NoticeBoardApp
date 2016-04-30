package com.jyotitech.noticeboardapp.ui;

import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.jyotitech.noticeboardapp.R;
import com.jyotitech.noticeboardapp.adapter.NoticeBoardListAdapter;
import com.jyotitech.noticeboardapp.adapter.UserListAdapter;
import com.jyotitech.noticeboardapp.interfaces.OutdatedResourceSubscriber;
import com.jyotitech.noticeboardapp.model.NoticeBoard;
import com.jyotitech.noticeboardapp.model.UserMember;
import com.jyotitech.noticeboardapp.sugar_models.SONotice;
import com.jyotitech.noticeboardapp.sugar_models.SONoticeBoard;
import com.jyotitech.noticeboardapp.sugar_models.SOUser;
import com.jyotitech.noticeboardapp.sugar_models.SOUserMember;
import com.jyotitech.noticeboardapp.utils.AppPreferences;
import com.jyotitech.noticeboardapp.utils.KeyConstants;
import com.jyotitech.noticeboardapp.utils.NetworkUtils;
import com.jyotitech.noticeboardapp.utils.NotificationHandler;
import com.jyotitech.noticeboardapp.utils.ToastMaker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoticeBoardListActivity extends OutdatedResourceSubscriberActivity implements OutdatedResourceSubscriber {

    private static final String TAG = NoticeBoardListActivity.class.getSimpleName();
    private NoticeBoardListAdapter noticeBoardListAdapter;
    private NoticeBoardListActivity mActivityContext;
    private Application mAppContext;
    private EditText edtTitle;
    private UserListAdapter userListAdapter;
    private Dialog dialogAddNoticeBoard;
    private long largestNoticeBoardId;
    private FloatingActionButton fab;
    private RecyclerView recList;
    private RelativeLayout rltProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_board_or_notice_list);

        mActivityContext = this;
        mAppContext = getApplication();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        recList = (RecyclerView) findViewById(R.id.recycler_view);
        rltProgress = (RelativeLayout) findViewById(R.id.rlt_progress);

        recList.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);
        noticeBoardListAdapter = new NoticeBoardListAdapter(this);
        recList.setAdapter(noticeBoardListAdapter);

        createAddNoticeBoardDialog();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edtTitle.setText("");
                userListAdapter.clearSelectedList();
                dialogAddNoticeBoard.show();
            }
        });

        updateNoticeBoards();
        updateUsersList();
    }

    private void updateNoticeBoards() {
        new AsyncTask<Void, Void, List<SONoticeBoard>>() {
            @Override
            protected void onPreExecute() {
                setProgress(true);
            }

            @Override
            protected List<SONoticeBoard> doInBackground(Void... params) {
                return SONoticeBoard.find(SONoticeBoard.class, null, null);
            }

            @Override
            protected void onPostExecute(List<SONoticeBoard> noticeBoards) {
                noticeBoardListAdapter.setDataSource(noticeBoards);
                setProgress(false);
            }
        }.execute();
    }

    private void updateUsersList() {
        new AsyncTask<Void, Void, List<SOUser>>() {
            @Override
            protected void onPreExecute() {
                setProgress(true);
            }

            @Override
            protected List<SOUser> doInBackground(Void... params) {
                return SOUser.find(SOUser.class, null, null);
            }

            @Override
            protected void onPostExecute(List<SOUser> result) {
                userListAdapter.addDataSource(result);
                setProgress(false);
            }
        }.execute();
    }

    private void createAddNoticeBoardDialog() {
        // Create custom dialog object
        dialogAddNoticeBoard = new Dialog(this);

        // Include dialog.xml file
        dialogAddNoticeBoard.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogAddNoticeBoard.setContentView(R.layout.dialog_add_notice_board);


        // set values for custom dialog components - text, image and button
        ListView lstUsers = (ListView) dialogAddNoticeBoard.findViewById(R.id.lst_names);
        Button btnAdd = (Button) dialogAddNoticeBoard.findViewById(R.id.btn_add);
        Button btnCancel = (Button) dialogAddNoticeBoard.findViewById(R.id.btn_cancel);
        edtTitle = (EditText) dialogAddNoticeBoard.findViewById(R.id.edt_notice_board_title);
        CheckBox chkSelectAll = (CheckBox) dialogAddNoticeBoard.findViewById(R.id.checkbox);

        chkSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkbox = (CheckBox) v;
                userListAdapter.setAllSelection(checkbox.isChecked());
            }
        });

        userListAdapter = new UserListAdapter(mActivityContext, chkSelectAll);
        lstUsers.setAdapter(userListAdapter);

        btnAdd.setTag(dialogAddNoticeBoard);
        // if accept button is clicked, store notice board on server
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**Validate**/
                if (edtTitle.getText().toString().trim().isEmpty()) {
                    ToastMaker.createShortToast(R.string.error_enter_title, mActivityContext);
                    return;
                }
                if (userListAdapter.getSelectedUserMembersList().isEmpty()) {
                    ToastMaker.createShortToast(R.string.error_select_user, mActivityContext);
                    return;
                }

                /**Process the request **/
                if (NetworkUtils.isConnectedToInternet(mAppContext)) {
                    processAddNoticeBoardRequest();
                } else {
                    ToastMaker.createShortToast(R.string.toast_internet_connection_error, mActivityContext);
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close dialog
                dialogAddNoticeBoard.hide();
            }
        });
    }

    private void processAddNoticeBoardRequest() {
        /** Get the largest id from server **/
        new Firebase(KeyConstants.FIREBASE_RESOURCE_NOTICEBOARD).orderByChild("id")
                .limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    NoticeBoard noticeBoard = snapshot.getValue(NoticeBoard.class);
                    largestNoticeBoardId = noticeBoard.getId();
                }

                /**Send data to server**/
                List<UserMember> userMemberList = new ArrayList<>();
                //Add appowner with write permissions
                UserMember userMember;
                SOUser soUser = SOUser.getAppOwner();
                if (soUser != null) {
                    userMember = new UserMember();
                    userMember.setId(soUser.getUserId());
                    userMember.setFullname(soUser.getFullname());
                    userMember.setPermissions(KeyConstants.PERMISSION_WRITE);
                    userMemberList.add(userMember);
                } else {
                    Log.e(TAG, "App owner not found");
                }

                //Add remaining selected users
                HashMap<SOUser, String> selectedUserHashMap = userListAdapter.getSelectedUserMembersList();
                for (Map.Entry<SOUser, String> entrySet : selectedUserHashMap.entrySet()) {
                    userMember = new UserMember();
                    userMember.setId(entrySet.getKey().getUserId());
                    userMember.setFullname(entrySet.getKey().getFullname());
                    userMember.setPermissions(entrySet.getValue());
                    userMemberList.add(userMember);
                }

                //Push notice board to firebase
                NoticeBoard noticeBoard = new NoticeBoard();
                noticeBoard.setTitle(edtTitle.getText().toString().trim());
                noticeBoard.setMembers(userMemberList);
                //noticeBoard.setNotices(new ArrayList<Notice>());
                noticeBoard.setLastModifiedAt(Calendar.getInstance().getTimeInMillis());
                noticeBoard.setId(++largestNoticeBoardId);
                new Firebase(KeyConstants.FIREBASE_RESOURCE_NOTICEBOARD).push().setValue(noticeBoard);

                /** Save data to local database **/
                SONoticeBoard soNoticeBoard = new SONoticeBoard();
                soNoticeBoard.setNoticeBoardId(largestNoticeBoardId);
                soNoticeBoard.setLastVisitedAt(noticeBoard.getLastModifiedAt());
                soNoticeBoard.setLastModifiedAt(noticeBoard.getLastModifiedAt());
                soNoticeBoard.setLastVisitedAt(System.currentTimeMillis());
                soNoticeBoard.setTitle(noticeBoard.getTitle());
                soNoticeBoard.save();
                SOUserMember soUserMember;
                for (UserMember userMemberFor : userMemberList) {
                    soUserMember = new SOUserMember();
                    soUserMember.setPermissions(userMemberFor.getPermissions());
                    soUserMember.setNoticeBoardId(largestNoticeBoardId);
                    soUserMember.setUserId(userMemberFor.getId());
                    soUserMember.save();
                }
                noticeBoardListAdapter.addDataToDataSource(soNoticeBoard);

                // Close dialog
                dialogAddNoticeBoard.hide();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, firebaseError.getMessage());
            }
        });
    }

    private void setProgress(boolean flag) {
        if (flag) {
            rltProgress.setVisibility(View.VISIBLE);
            fab.setVisibility(View.GONE);
            recList.setVisibility(View.GONE);
        } else {
            rltProgress.setVisibility(View.GONE);
            fab.setVisibility(View.VISIBLE);
            recList.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onDestroy() {
        if (dialogAddNoticeBoard != null) {
            dialogAddNoticeBoard.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_notice_board, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            //Clear preferences
            AppPreferences.getInstance().clear();

            //Clear whole database
            SONoticeBoard.deleteAll(SONoticeBoard.class);
            SONotice.deleteAll(SONotice.class);
            SOUser.deleteAll(SOUser.class);
            SOUserMember.deleteAll(SOUserMember.class);

            NotificationHandler.getInstance().clearNotifications();
            ((NoticeBoardApplication)mAppContext).removeGlobalDataListner();
            //Launch fresh login activity
            Intent intent = new Intent(NoticeBoardListActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDatasetChanged(String dataset) {
        if (dataset.equals(KeyConstants.OUTDATED_RESOURCE_NOTICE_BOARD)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateNoticeBoards();
                }
            });
        }
        else if(dataset.equals(KeyConstants.OUTDATED_RESOURCE_USER)){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUsersList();
                }
            });
        }
    }
}
