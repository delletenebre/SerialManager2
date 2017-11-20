package kg.delletenebre.serialmanager2.commands;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
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

        if (oldVersion == 1) {
            schema.create("WidgetReceiveModel")
                    .addField("index", Integer.class, FieldAttribute.REQUIRED)
                    .addField("key", String.class, FieldAttribute.REQUIRED)
                    .addField("value", String.class)
                    .addField("textColor", String.class)
                    .addField("textSize", int.class)
                    .addField("backgroundColor", String.class)
                    .addField("backgroundImage", String.class)
                    .addField("layoutAlignId", int.class)
                    .addField("textAlignId", int.class);

            oldVersion++;
        }

        if (oldVersion == 2) {
            RealmObjectSchema commandSchema = schema.get("Command");
            commandSchema
                    .addField("positionZ", int.class)
                    .transform(new RealmObjectSchema.Function() {
                        @Override
                        public void apply(DynamicRealmObject obj) {
                            obj.setInt("positionZ", 0);
                        }
                    });
            oldVersion++;
        }

        if (oldVersion == 3) {
            RealmObjectSchema widgetReceiveSchema = schema.get("WidgetReceiveModel");
            widgetReceiveSchema
                    .addField("id", int.class)
                    .transform(new RealmObjectSchema.Function() {
                        @Override
                        public void apply(DynamicRealmObject obj) {
                            obj.setFloat("id", obj.getInt("index"));
                        }
                    })
                    .removeField("index")
                    .setRequired("value", true)
                    .setRequired("textColor", true)
                    .setRequired("backgroundColor", true)
                    .setRequired("backgroundImage", true);
            oldVersion++;
        }
    }
}