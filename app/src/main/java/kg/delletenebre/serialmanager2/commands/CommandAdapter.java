package kg.delletenebre.serialmanager2.commands;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmResults;
import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.R;


public class CommandAdapter extends RecyclerView.Adapter {

    private Realm mRealm;
    private OrderedRealmCollection<CommandModel> mItems;


    // Provide a suitable constructor (depends on the kind of dataset)
    public CommandAdapter() {
        updateRealmInstance();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.command, parent, false);

        return new ViewHolder(layout);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        CommandAdapter.ViewHolder viewHolder = (CommandAdapter.ViewHolder) holder;
        final CommandModel item = mItems.get(position);

        viewHolder.itemView.setBackgroundColor(Color.WHITE);
        viewHolder.mTitle.setVisibility(View.VISIBLE);
        viewHolder.mSummary.setVisibility(View.VISIBLE);

        viewHolder.mTitle.setText(item.getTitleLabel(App.getInstance().getApplicationContext()));
        viewHolder.mSummary.setText(
                item.getSubtitleLabel(App.getInstance().getApplicationContext()));

        viewHolder.mUndo.setVisibility(View.GONE);
        viewHolder.mUndo.setOnClickListener(null);

        viewHolder.mCommand = item;
    }

    @Override
    public int getItemCount() {
        if (mItems == null) {
            return 0;
        }

        return mItems.size();
    }

    public void updateRealmInstance() {
        mRealm = App.getInstance().getRealm();
        mItems = mRealm.where(CommandModel.class).findAllAsync().sort("index");
        notifyDataSetChanged();
    }

    public String getItemTitle(Context context, int position) {
        CommandModel item = mItems.get(position);
        return item.getTitleLabel(context);
    }

    public void add(int position, CommandModel command) {
        mItems.add(position, command);
        notifyItemInserted(position);
    }

    public void updatePositions(final int oldIndex, final int newIndex) {
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                CommandModel movedCommand = realm.where(CommandModel.class).equalTo("index", oldIndex)
                        .findFirst();
                if (movedCommand != null) {
                    RealmResults<CommandModel> commands;
                    int way = 1;
                    if (oldIndex < newIndex) {
                        commands = realm.where(CommandModel.class)
                                .greaterThan("index", oldIndex)
                                .lessThanOrEqualTo("index", newIndex)
                                .findAll();
                        way = -1;
                    } else {
                        commands = realm.where(CommandModel.class)
                                .greaterThanOrEqualTo("index", newIndex)
                                .lessThan("index", oldIndex)
                                .findAll();
                    }
                    for (CommandModel command : commands) {
                        command.setIndex(command.getIndex() + way);
                    }
                    movedCommand.setIndex(newIndex);
                    notifyItemMoved(oldIndex, newIndex);
                }
            }
        });
    }

    public void remove(final int position) {
        CommandModel item = mItems.get(position);
        if (mItems.contains(item)) {
            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    CommandModel removedCommand = realm.where(CommandModel.class)
                            .equalTo("index", position)
                            .findFirst();
                    if (removedCommand != null) {
                        removedCommand.deleteFromRealm();

                        RealmResults<CommandModel> commands = realm.where(CommandModel.class)
                                .greaterThan("index", position)
                                .findAll();
                        for (CommandModel command : commands) {
                            command.setIndex(command.getIndex() - 1);
                        }

                        notifyItemRemoved(position);
                    }
                }
            });
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mTitle;
        TextView mSummary;
//        ImageView mIcon;
        Button mUndo;
        CommandModel mCommand;

        ViewHolder(View layout) {
            super(layout);
            mTitle = layout.findViewById(R.id.title);
            mSummary = layout.findViewById(R.id.summary);
//            mIcon = layout.findViewById(R.id.icon);
            mUndo = layout.findViewById(R.id.undo_button);

            layout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Context context = view.getContext();
            Intent intent = new Intent(view.getContext(), CommandEditActivity.class);
            intent.putExtra("CommandIndex", mCommand.getIndex());
            context.startActivity(intent);
        }
    }


}
