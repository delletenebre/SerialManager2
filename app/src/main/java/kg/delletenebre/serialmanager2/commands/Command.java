package kg.delletenebre.serialmanager2.commands;


import android.content.Context;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Required;
import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.R;
import kg.delletenebre.serialmanager2.utils.Utils;

public class Command extends RealmObject {
    @Ignore
    public static final int ACTION_NONE = 0;
    @Ignore
    public static final int ACTION_RUN_APPLICATION = 1;
    @Ignore
    public static final int ACTION_EMULATE_KEY = 2;
    @Ignore
    public static final int ACTION_SHELL_COMMAND = 3;
    @Ignore
    public static final int ACTION_SEND_DATA = 4;


    @Required
    private Integer index;

    private boolean folder;
    private String key;
    private String value;
    private float scatter;
    private String intentValueExtra;
    private int actionId;
    private String chosenApp;
    private String chosenAppLabel;
    private int emulatedKeyId;
    private String shellCommand;
    private String sendData;
    private String notyMessage;
    private float notyDuration;
    private int notyTextSize;
    private String notyBgColor;
    private String notyTextColor;
    private int positionX;
    private int positionY;
    private int offsetX;
    private int offsetY;


    public boolean isFolder() {
        return folder;
    }
    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }

    public float getScatter() {
        return scatter;
    }
    public void setScatter(float scatter) {
        this.scatter = scatter;
    }

    public String getIntentValueExtra() {
        return intentValueExtra;
    }
    public void setIntentValueExtra(String intentValueExtra) {
        this.intentValueExtra = intentValueExtra;
    }

    public int getActionId() {
        return actionId;
    }
    public void setActionId(int actionId) {
        this.actionId = actionId;
    }

    public String getChosenAppLabel() {
        return chosenAppLabel;
    }
    public void setChosenAppLabel(String chosenAppLabel) {
        this.chosenAppLabel = chosenAppLabel;
    }

    public int getEmulatedKeyId() {
        return emulatedKeyId;
    }
    public void setEmulatedKeyId(int emulatedKeyId) {
        this.emulatedKeyId = emulatedKeyId;
    }

    public String getShellCommand() {
        return shellCommand;
    }
    public void setShellCommand(String shellCommand) {
        this.shellCommand = shellCommand;
    }

    public String getSendData() {
        return sendData;
    }
    public void setSendData(String sendData) {
        this.sendData = sendData;
    }

    public String getNotyMessage() {
        return notyMessage;
    }
    public void setNotyMessage(String notyMessage) {
        this.notyMessage = notyMessage;
    }

    public float getNotyDuration() {
        return notyDuration;
    }
    public int getNotyDurationInMillis() {
        return (int)(notyDuration * 1000);
    }
    public void setNotyDuration(float notyDuration) {
        this.notyDuration = notyDuration;
    }

    public int getNotyTextSize() {
        return notyTextSize;
    }
    public void setNotyTextSize(int notyTextSize) {
        this.notyTextSize = notyTextSize;
    }

    public String getNotyBgColor() {
        return notyBgColor;
    }
    public void setNotyBgColor(String notyBgColor) {
        this.notyBgColor = notyBgColor;
    }

    public String getNotyTextColor() {
        return notyTextColor;
    }
    public void setNotyTextColor(String notyTextColor) {
        this.notyTextColor = notyTextColor;
    }

    public int getOffsetX() {
        return offsetX;
    }
    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }
    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public String getChosenApp() {
        return chosenApp;
    }
    public void setChosenApp(String chosenApp) {
        this.chosenApp = chosenApp;
    }

    public int getPositionX() {
        return positionX;
    }
    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionY() {
        return positionY;
    }
    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public String getTitleLabel(Context context) {
        String strValue = value;
        if (value != null) {
            if (value.isEmpty()) {
                strValue = "*";
            } else if (Utils.isNumber(value) && scatter != 0.0f) {
                strValue += " ± " + scatter;
            }
        }
        return context.getString(R.string.command_title, key, strValue);

    }

    public String getSubtitleLabel(Context context) {
        String[] actions = context.getResources().getStringArray(R.array.array_command_actions);

        switch (getActionId()) {
            case ACTION_RUN_APPLICATION:
                return chosenAppLabel;

            case ACTION_EMULATE_KEY:
                return context.getString(R.string.command_subtitle,
                        actions[getActionId()],
                        App.getInstance().getVirtualKeyboard().getNameById(emulatedKeyId));

            case ACTION_SHELL_COMMAND:
                return context.getString(R.string.command_subtitle,
                        actions[getActionId()],
                        shellCommand);

            case ACTION_SEND_DATA:
                return context.getString(R.string.command_subtitle,
                        actions[getActionId()],
                        sendData);

            default:
                // ACTION_NONE
                return actions[getActionId()];
        }
    }
}
