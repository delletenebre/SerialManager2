package kg.delletenebre.serialmanager2.widgets;

import io.realm.RealmObject;
import io.realm.annotations.Required;


public class WidgetReceiveModel extends RealmObject {
    @Required
    private Integer index;
    @Required
    private String key;
    private String value;
    private String textColor;
    private int textSize;
    private String backgroundColor;
    private String backgroundImage;
    private int layoutAlignId;
    private int textAlignId;
}
