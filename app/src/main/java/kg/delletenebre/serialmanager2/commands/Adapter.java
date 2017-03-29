package kg.delletenebre.serialmanager2.commands;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmResults;
import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.R;


public class Adapter extends RecyclerView.Adapter {

    private static final int PENDING_REMOVAL_TIMEOUT = 3000;

    private Realm mRealm;
    private OrderedRealmCollection<Command> mItems;
    private List<Command> mItemsPendingRemoval;

    private Handler handler = new Handler(); // hanlder for running delayed runnables
    private HashMap<Command, Runnable> mPendingRunnables = new HashMap<>(); // map of items to pending runnables, so we can cancel a removal if need be



    // Provide a suitable constructor (depends on the kind of dataset)
    public Adapter() {
        mRealm = App.getInstance().getRealm();
        mItems = mRealm.where(Command.class).findAllAsync().sort("index");
        mItemsPendingRemoval = new ArrayList<>();
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
        Adapter.ViewHolder viewHolder = (Adapter.ViewHolder) holder;
        final Command item = mItems.get(position);

        if (mItemsPendingRemoval.contains(item)) {
            // we need to show the "undo" state of the row
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#F44336"));

//            viewHolder.mIcon.setVisibility(View.GONE);
            viewHolder.mTitle.setVisibility(View.GONE);
            viewHolder.mSummary.setVisibility(View.GONE);

            viewHolder.mUndo.setVisibility(View.VISIBLE);
            viewHolder.mUndo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // user wants to undo the removal, let's cancel the pending task
                    Runnable pendingRemovalRunnable = mPendingRunnables.get(item);
                    mPendingRunnables.remove(item);
                    if (pendingRemovalRunnable != null) {
                        handler.removeCallbacks(pendingRemovalRunnable);
                    }
                    mItemsPendingRemoval.remove(item);
                    // this will rebind the row in "normal" state
                    notifyItemChanged(mItems.indexOf(item));
                }
            });
        } else {
            // we need to show the "normal" state
            viewHolder.itemView.setBackgroundColor(Color.WHITE);

//            viewHolder.mIcon.setVisibility(View.VISIBLE);
            viewHolder.mTitle.setVisibility(View.VISIBLE);
            viewHolder.mSummary.setVisibility(View.VISIBLE);

            viewHolder.mTitle.setText(item.getTitleLabel(App.getInstance().getApplicationContext()));
            viewHolder.mSummary.setText(
                    item.getSubtitleLabel(App.getInstance().getApplicationContext()));

            viewHolder.mUndo.setVisibility(View.GONE);
            viewHolder.mUndo.setOnClickListener(null);
        }

        viewHolder.mCommand = item;
    }

    @Override
    public int getItemCount() {
        if (mItems == null) {
            return 0;
        }

        return mItems.size();
    }

    public void add(int position, Command command) {
        mItems.add(position, command);
        notifyItemInserted(position);
    }

    public void updatePositions(final int oldIndex, final int newIndex) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Command movedCommand = realm.where(Command.class).equalTo("index", oldIndex)
                        .findFirst();
                RealmResults<Command> commands;
                int way = 1;
                if (oldIndex < newIndex) {
                    commands = realm.where(Command.class)
                            .greaterThan("index", oldIndex)
                            .lessThanOrEqualTo("index", newIndex)
                            .findAll();
                    way = -1;
                } else {
                    commands = realm.where(Command.class)
                            .greaterThanOrEqualTo("index", newIndex)
                            .lessThan("index", oldIndex)
                            .findAll();
                }
                for (Command command : commands) {
                    command.setIndex(command.getIndex() + way);
                }

                movedCommand.setIndex(newIndex);
            }
        });

        notifyItemMoved(oldIndex, newIndex);
    }

    public void pendingRemoval(int position) {
        final Command item = mItems.get(position);
        if (!mItemsPendingRemoval.contains(item)) {
            mItemsPendingRemoval.add(item);
            // this will redraw row in "undo" state
            notifyItemChanged(position);
            // let's create, store and post a runnable to remove the item
            Runnable pendingRemovalRunnable = new Runnable() {
                @Override
                public void run() {
                    remove(mItems.indexOf(item));
                }
            };
            handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
            mPendingRunnables.put(item, pendingRemovalRunnable);
        }
    }

    public boolean isPendingRemoval(int position) {
        Command item = mItems.get(position);
        return mItemsPendingRemoval.contains(item);
    }

    public void remove(final int position) {
        Command item = mItems.get(position);
        if (mItemsPendingRemoval.contains(item)) {
            mItemsPendingRemoval.remove(item);
        }
        if (mItems.contains(item)) {
            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Command removedCommand = realm.where(Command.class)
                            .equalTo("index", position)
                            .findFirst();
                    removedCommand.deleteFromRealm();

                    RealmResults<Command> commands = realm.where(Command.class)
                            .greaterThan("index", position)
                            .findAll();
                    for (Command command : commands) {
                        command.setIndex(command.getIndex() - 1);
                    }
                }
            });

            notifyItemRemoved(position);
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mTitle;
        TextView mSummary;
//        ImageView mIcon;
        Button mUndo;
        Command mCommand;

        ViewHolder(View layout) {
            super(layout);
            mTitle = (TextView) layout.findViewById(R.id.title);
            mSummary = (TextView) layout.findViewById(R.id.summary);
//            mIcon = (ImageView) layout.findViewById(R.id.icon);
            mUndo = (Button) layout.findViewById(R.id.undo_button);

            layout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Context context = view.getContext();
            Intent intent = new Intent(view.getContext(), EditActivity.class);
            intent.putExtra("CommandIndex", mCommand.getIndex());
            context.startActivity(intent);
        }
    }


}
