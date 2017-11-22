package kg.delletenebre.serialmanager2.widgets

import android.support.annotation.Keep
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

@Keep
open class WidgetSimpleModel : RealmObject() {
    @PrimaryKey var id: Int = 0
    var key: String = ""
    var text: String = ""
    var textColor: String = "#ffffffff"
    var textSize: Int = 14
    var backgroundColor: String = "#88000000"
    var backgroundImage: String = ""
    var textVerticalPositionId: Int = 1
    var textAlignmentId: Int = 1

    var actionId: Int = 0
    var chosenApp: String = ""
    var chosenAppLabel: String = ""
    var emulatedKeyId: Int = 0
    var shellCommand: String = ""
    var sendData: String = ""
}