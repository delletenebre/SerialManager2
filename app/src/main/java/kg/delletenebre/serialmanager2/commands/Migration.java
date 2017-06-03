package kg.delletenebre.serialmanager2.commands;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

public class Migration implements RealmMigration {

    @Override
    public void migrate(final DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        // Migrate from version 0 to version 1
        if (oldVersion == 0) {
            RealmObjectSchema commandSchema = schema.get("Command");
            commandSchema
                    .addField("notyDuration_temp", float.class)
                    .transform(new RealmObjectSchema.Function() {
                        @Override
                        public void apply(DynamicRealmObject obj) {
                            obj.setFloat("notyDuration_temp", obj.getInt("notyDuration"));
                        }
                    })
                    .removeField("notyDuration")
                    .renameField("notyDuration_temp", "notyDuration");
            oldVersion++;
        }
    }
}