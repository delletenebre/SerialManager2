package kg.delletenebre.serialmanager2.commands;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmResults;
import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.R;


public class Adapter extends RecyclerView.Adapter {

    private Realm mRealm;
    private OrderedRealmCollection<Command> mItems;


    // Provide a suitable constructor (depends on the kind of dataset)
    public Adapter() {
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
        Adapter.ViewHolder viewHolder = (Adapter.ViewHolder) holder;
        final Command item = mItems.get(position);

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
        mItems = mRealm.where(Command.class).findAllAsync().sort("index");
        notifyDataSetChanged();
    }

    public String getItemTitle(Context context, int position) {
        Command item = mItems.get(position);
        return item.getTitleLabel(context);
    }

    public void add(int position, Command command) {
        mItems.add(position, command);
        notifyItemInserted(position);
    }

    public void updatePositions(final int oldIndex, final int newIndex) {
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Command movedCommand = realm.where(Command.class).equalTo("index", oldIndex)
                        .findFirst();
                if (movedCommand != null) {
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
                    notifyItemMoved(oldIndex, newIndex);
                }
            }
        });
    }

    public void remove(final int position) {
        Command item = mItems.get(position);
        if (mItems.contains(item)) {
            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Command removedCommand = realm.where(Command.class)
                            .equalTo("index", position)
                            .findFirst();
                    if (removedCommand != null) {
                        removedCommand.deleteFromRealm();

                        RealmResults<Command> commands = realm.where(Command.class)
                                .greaterThan("index", position)
                                .findAll();
                        for (Command command : commands) {
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
        Command mCommand;

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
            Intent intent = new Intent(view.getContext(), EditActivity.class);
            intent.putExtra("CommandIndex", mCommand.getIndex());
            context.startActivity(intent);
        }
    }


}
