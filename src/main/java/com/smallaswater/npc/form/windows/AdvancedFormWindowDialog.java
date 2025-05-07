package com.smallaswater.npc.form.windows;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityDataTypes;
import cn.nukkit.network.protocol.NPCDialoguePacket;
import cn.nukkit.network.protocol.NPCRequestPacket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smallaswater.npc.form.element.ResponseElementDialogButton;
import com.smallaswater.npc.form.response.FormResponseDialog;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class AdvancedFormWindowDialog {

    protected static final Gson GSON = new Gson();

    private static long dialogId = 0;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String content;

    @Getter
    private String skinData = "{\"picker_offsets\":{\"scale\":[1.70,1.70,1.70],\"translate\":[0,20,0]},\"portrait_offsets\":{\"scale\":[1.750,1.750,1.750],\"translate\":[-7,50,0]},\"skin_list\":[{\"variant\":0},{\"variant\":1},{\"variant\":2},{\"variant\":3},{\"variant\":4},{\"variant\":5},{\"variant\":6},{\"variant\":7},{\"variant\":8},{\"variant\":9},{\"variant\":10},{\"variant\":11},{\"variant\":12},{\"variant\":13},{\"variant\":14},{\"variant\":15},{\"variant\":16},{\"variant\":17},{\"variant\":18},{\"variant\":19},{\"variant\":20},{\"variant\":21},{\"variant\":22},{\"variant\":23},{\"variant\":24},{\"variant\":25},{\"variant\":26},{\"variant\":27},{\"variant\":28},{\"variant\":29},{\"variant\":30},{\"variant\":31},{\"variant\":32},{\"variant\":33},{\"variant\":34}]}";

    @Getter
    private String sceneName = String.valueOf(dialogId++);

    @Setter
    @Getter
    private List<ResponseElementDialogButton> buttons;

    @Getter
    private final Entity bindEntity;

    protected BiConsumer<Player, FormResponseDialog> formClosedListener;

    private boolean isClosed = false;

    public AdvancedFormWindowDialog(String title, String content, Entity bindEntity) {
        this(title, content, bindEntity, new ArrayList<>());
    }

    public AdvancedFormWindowDialog(String title, String content, Entity bindEntity, List<ResponseElementDialogButton> buttons) {
        this.title = title;
        this.content = content;
        this.buttons = buttons;
        this.bindEntity = bindEntity;
        if (this.bindEntity == null) {
            throw new IllegalArgumentException("bindEntity cannot be null!");
        }
    }

    public ResponseElementDialogButton addAdvancedButton(String text) {
        return this.addButton(new ResponseElementDialogButton(text, text));
    }

    @Deprecated
    public ResponseElementDialogButton addButton(String text) {
        return this.addButton(new ResponseElementDialogButton(text, text));
    }

    public ResponseElementDialogButton addButton(ResponseElementDialogButton button) {
        this.buttons.add(button);
        return button;
    }

    public long getEntityId() {
        return this.getBindEntity().getId();
    }

    public void setSkinData(String data) {
        this.skinData = data;
    }

    public String getButtonJSONData() {
        return GSON.toJson(this.buttons);
    }

    public void setButtonJSONData(String json) {
        this.setButtons(GSON.fromJson(json, new TypeToken<List<ResponseElementDialogButton>>() {
        }.getType()));
    }

    public void updateSceneName() {
        this.sceneName = String.valueOf(dialogId++);
    }

    public AdvancedFormWindowDialog onClosed(@NotNull BiConsumer<Player, FormResponseDialog> listener) {
        this.formClosedListener = Objects.requireNonNull(listener);
        return this;
    }

    protected void callClosed(@NotNull Player player, FormResponseDialog response) {
        if (this.formClosedListener != null && !this.isClosed) {
            this.formClosedListener.accept(player, response);
        }
    }

    public static final Cache<String, AdvancedFormWindowDialog> WINDOW_DIALOG_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

    public void send(Player player) {
        if (WINDOW_DIALOG_CACHE.getIfPresent(this.getSceneName()) != null) {
            this.updateSceneName();
        }
        String actionJson = this.getButtonJSONData();

        this.getBindEntity().setDataProperty(EntityDataTypes.HAS_NPC, true);
        this.getBindEntity().setDataProperty(EntityDataTypes.NPC_DATA, this.getSkinData());
        this.getBindEntity().setDataProperty(EntityDataTypes.ACTIONS, actionJson);
        this.getBindEntity().setDataProperty(EntityDataTypes.INTERACT_TEXT, this.getContent());

        NPCDialoguePacket packet = new NPCDialoguePacket();
        packet.runtimeEntityId = this.getEntityId();
        packet.action = NPCDialoguePacket.NPCDialogAction.OPEN;
        packet.dialogue = this.getContent();
        packet.npcName = this.getTitle();
        packet.sceneName = this.getSceneName();
        packet.actionJson = actionJson;
        WINDOW_DIALOG_CACHE.put(this.getSceneName(), this);
        player.dataPacket(packet);
    }

    public void close(Player player, FormResponseDialog response) {
        NPCDialoguePacket closeWindowPacket = new NPCDialoguePacket();
        closeWindowPacket.runtimeEntityId=response.getEntityRuntimeId();
        closeWindowPacket.action = NPCDialoguePacket.NPCDialogAction.CLOSE;
        closeWindowPacket.sceneName=response.getSceneName();
        player.dataPacket(closeWindowPacket);
    }

    public static boolean onEvent(@NotNull NPCRequestPacket packet, @NotNull Player player) {
        AdvancedFormWindowDialog dialog = WINDOW_DIALOG_CACHE.getIfPresent(packet.sceneName);
        if (dialog == null) {
            return false;
        }

        if (packet.requestType == NPCRequestPacket.RequestType.EXECUTE_CLOSING_COMMANDS) {
            WINDOW_DIALOG_CACHE.invalidate(packet.sceneName);
        }

        FormResponseDialog response = new FormResponseDialog(packet, dialog);

        ResponseElementDialogButton clickedButton = response.getClickedButton();
        if (packet.requestType == NPCRequestPacket.RequestType.EXECUTE_ACTION && clickedButton != null) {
            clickedButton.callClicked(player, response);
            dialog.isClosed = true;
        }

        if (packet.requestType == NPCRequestPacket.RequestType.EXECUTE_CLOSING_COMMANDS) {
            dialog.callClosed(player, response);
        }
        return true;
    }

}
