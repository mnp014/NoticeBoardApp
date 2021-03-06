package com.noticeboardapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.noticeboardapp.sugar_models.SOUser;
import com.noticeboardapp.R;
import com.noticeboardapp.utils.KeyConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Adapter to show list of Users on the screen.
 * Created by Pinky Walve on 24/4/16.
 */
public class UserListAdapter extends BaseAdapter implements Filterable {
    List<SOUser> userList;
    List<SOUser> filteredUserList;
    HashMap<SOUser, String> selectedUserList;
    Context context;
    LayoutInflater layoutInflater;
    CheckBox mainCheckBox;
    UserFilter userFilter;

    public UserListAdapter(Context context, CheckBox mainCheckBox) {
        userList = new ArrayList<>();
        filteredUserList = new ArrayList<>();
        selectedUserList = new HashMap<>();
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.mainCheckBox = mainCheckBox;
        userFilter = new UserFilter();
    }

    public void addDataSource(List<SOUser> userList) {
        this.userList.clear();
        this.filteredUserList.clear();
        this.selectedUserList.clear();
        this.userList.addAll(userList);
        this.filteredUserList.addAll(userList);
        notifyDataSetChanged();
    }

    public void clearSelectedList() {
        this.selectedUserList.clear();
        notifyDataSetChanged();
    }

    public HashMap<SOUser, String> getSelectedUserMembersList() {
        return selectedUserList;
    }

    public boolean isAllSelected() {
        return !userList.isEmpty() && selectedUserList.size() == userList.size();
    }

    public void setAllSelection(boolean setAllSelected) {
        if (setAllSelected) {
            this.selectedUserList.clear();
            for (SOUser userSelected : userList) {
                this.selectedUserList.put(userSelected, KeyConstants.PERMISSION_READ);
            }
        }
        else {
            this.selectedUserList.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredUserList.size();
    }

    @Override
    public SOUser getItem(int position) {
        return filteredUserList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SOUser user = getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_user_list, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.txtUserName.setText(user.getFullname());
        boolean selectedListContainUser = selectedUserList.containsKey(user);
        viewHolder.chkbox.setChecked(selectedListContainUser);
        viewHolder.chkbox.setTag(user);
        viewHolder.chkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SOUser clickedUser = (SOUser) v.getTag();
                if (selectedUserList.containsKey(clickedUser)) {
                    selectedUserList.remove(clickedUser);
                }
                else {
                    selectedUserList.put(clickedUser, KeyConstants.PERMISSION_READ);
                }
                notifyDataSetChanged();
                mainCheckBox.setChecked(isAllSelected());
            }
        });

        viewHolder.relPermissions.setVisibility(selectedListContainUser ? View.VISIBLE : View.GONE);
        viewHolder.switchPermission.setChecked(selectedListContainUser
                && selectedUserList.get(user).equals(KeyConstants.PERMISSION_WRITE));
        viewHolder.switchPermission.setTag(user);
        viewHolder.switchPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Switch switchBox = (Switch)v;
                SOUser clickedUser = (SOUser) v.getTag();
                selectedUserList.put(clickedUser,
                        switchBox.isChecked() ? KeyConstants.PERMISSION_WRITE : KeyConstants.PERMISSION_READ);
                notifyDataSetChanged();
            }
        });
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    class UserFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String filterString = constraint.toString().toLowerCase();

            int count = userList.size();
            ArrayList<SOUser> newFilteredUserList = new ArrayList<>(count);

            for (SOUser soUser : userList) {
                if(soUser.getFullname().toLowerCase().contains(filterString)) {
                    newFilteredUserList.add(soUser);
                }
            }

            FilterResults results = new FilterResults();
            results.values = newFilteredUserList;
            results.count = newFilteredUserList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredUserList.clear();
            filteredUserList.addAll((ArrayList<SOUser>)results.values);
            notifyDataSetChanged();
        }
    }

    class ViewHolder {
        TextView txtUserName;
        CheckBox chkbox;
        RelativeLayout relPermissions;
        Switch switchPermission;

        public ViewHolder(View view) {
            txtUserName = (TextView) view.findViewById(R.id.txt_name);
            chkbox = (CheckBox) view.findViewById(R.id.checkbox);
            relPermissions = (RelativeLayout) view.findViewById(R.id.rel_permissions);
            switchPermission = (Switch) view.findViewById(R.id.switch_permission);
        }
    }
}
